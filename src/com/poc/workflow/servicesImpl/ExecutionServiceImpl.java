package com.poc.workflow.servicesImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.poc.workflow.enums.TaskStatus;
import com.poc.workflow.exception.InactiveWorkflowExecutionException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.model.WorkflowRunContext;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.util.DataStore;
import com.poc.workflow.util.DependencyUtil;
import com.poc.workflow.util.IdGenerator;
import com.poc.workflow.util.LogUtil;

public class ExecutionServiceImpl implements ExecutionService {

    private static final double FAILURE_PROBABILITY = 0.0;
    private static final int TASK_THREAD_COUNT = 10;
    private static final int RETRY_THREAD_COUNT = 3;

    private final double failureProbability;
    private final ExecutorService taskExecutor =
            Executors.newFixedThreadPool(TASK_THREAD_COUNT, namedDaemonFactory("task-worker"));
    private final ScheduledExecutorService retryScheduler =
            Executors.newScheduledThreadPool(RETRY_THREAD_COUNT, namedDaemonFactory("task-retry"));
    private final RetryHandler retryHandler = new RetryHandler(retryScheduler, taskExecutor);
    private boolean shutDown;

    public ExecutionServiceImpl() {
        this(FAILURE_PROBABILITY);
    }

    public ExecutionServiceImpl(double failureProbability) {
        if (Double.isNaN(failureProbability)
                || failureProbability < 0.0 || failureProbability > 1.0) {
            throw new IllegalArgumentException("Failure probability must be between 0 and 1");
        }
        this.failureProbability = failureProbability;
        LogUtil.info("Thread Pool Size : " + TASK_THREAD_COUNT);
        LogUtil.info("Retry Pool Size  : " + RETRY_THREAD_COUNT);
    }

    @Override
    public WorkflowRun executeWorkflow(
            String workflowId,
            String triggeredBy)
            throws WorkflowNotFoundException,
                   InactiveWorkflowExecutionException {
        WorkflowRunContext context = executeWorkflowAsync(workflowId, triggeredBy);
        try {
            return context.getCompletion().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for workflow execution", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Workflow execution failed", cause);
        }
    }

    @Override
    public synchronized WorkflowRunContext executeWorkflowAsync(
            String workflowId,
            String triggeredBy)
            throws WorkflowNotFoundException,
                   InactiveWorkflowExecutionException {
        if (shutDown) {
            throw new RejectedExecutionException("Execution service is shut down");
        }

        Workflow workflow = DataStore.workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException("Workflow not found");
        }
        if (!workflow.isActive()) {
            throw new InactiveWorkflowExecutionException("Workflow is inactive");
        }
        if (workflow.getTasks().isEmpty()) {
            throw new IllegalArgumentException("Cannot execute a workflow with no tasks");
        }

        List<Task> taskSnapshot = workflow.getTasks().stream()
                .map(task -> new Task(task.getTaskId(), task.getTaskName(), task.getPriority(),
                        task.getBaseExecutionTimeMs(), task.getDependsOnTaskIds()))
                .toList();
        List<Task> executionOrder = DependencyUtil.sortByDependencies(taskSnapshot);
        WorkflowRun run = new WorkflowRun(IdGenerator.nextRunId(), workflow, triggeredBy);
        WorkflowRunContext context = new WorkflowRunContext(run);
        DataStore.activeRuns.put(run.getRunId(), context);

        LogUtil.info("Queued workflow " + workflow.getName() + " as " + run.getRunId());

        Map<String, CompletableFuture<Void>> taskCompletions = new HashMap<>();
        try {
            for (Task task : executionOrder) {
                CompletableFuture<?>[] dependencies = task.getDependsOnTaskIds().stream()
                        .map(taskCompletions::get)
                        .toArray(CompletableFuture<?>[]::new);
                task.getDependsOnTaskIds().forEach(dependencyId ->
                        LogUtil.taskWaitingForDependency(run, task.getTaskId(), dependencyId));
                CompletableFuture<Void> completion = new CompletableFuture<>();
                CompletableFuture<Void> dispatch = CompletableFuture.allOf(dependencies)
                        .thenRunAsync(() -> {
                            new TaskExecutor(task, run, failureProbability, retryHandler, completion).run();
                        }, taskExecutor);
                dispatch.whenComplete((unused, error) -> {
                    if (error != null) {
                        completion.completeExceptionally(error);
                    }
                });
                taskCompletions.put(task.getTaskId(), completion);
            }

            CompletableFuture.allOf(taskCompletions.values().toArray(new CompletableFuture<?>[0]))
                    .whenComplete((unused, error) -> finishRun(context, error));
        } catch (RejectedExecutionException e) {
            finishRun(context, e);
        }

        return context;
    }

    private void finishRun(WorkflowRunContext context, Throwable executionError) {
        WorkflowRun run = context.getRun();
        Throwable completionError = executionError;
        try {
            run.getTaskStatuses().forEach((taskId, status) -> {
                if (status == TaskStatus.PENDING || status == TaskStatus.RUNNING) {
                    run.markFailed(taskId, 0);
                }
            });
            run.finish();
            DataStore.workflowRuns.add(run);
            LogUtil.printWorkflowRunSummary(run);
            LogUtil.info("Workflow execution finished with status " + run.getStatus());
            if (run.getStatus() == com.poc.workflow.enums.RunStatus.FAILED) {
                LogUtil.workflowFailed(run);
            }
            if (executionError != null) {
                LogUtil.info("Workflow execution encountered an error: " + executionError);
            }
        } catch (RuntimeException e) {
            completionError = e;
            LogUtil.info("Could not finalize workflow run " + run.getRunId() + ": " + e);
        } finally {
            DataStore.activeRuns.remove(run.getRunId(), context);
        }

        if (completionError == null) {
            context.complete();
        } else {
            context.completeExceptionally(completionError);
        }
    }

    @Override
    public synchronized void shutdown() {
        if (shutDown) {
            return;
        }
        shutDown = true;

        List<CompletableFuture<WorkflowRun>> runningCompletions = new ArrayList<>();
        DataStore.activeRuns.values().forEach(context ->
                runningCompletions.add(context.getCompletion()));
        try {
            CompletableFuture.allOf(runningCompletions.toArray(new CompletableFuture<?>[0])).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            taskExecutor.shutdownNow();
            retryScheduler.shutdownNow();
            return;
        } catch (ExecutionException e) {
            LogUtil.info("A workflow run failed while shutting down: " + e.getCause());
        }

        taskExecutor.shutdown();
        retryScheduler.shutdown();
        awaitTermination(taskExecutor, "task executor");
        awaitTermination(retryScheduler, "retry scheduler");
    }

    private static void awaitTermination(ExecutorService executor, String name) {
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                LogUtil.info("Timed out waiting for " + name + "; interrupting remaining work.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory namedDaemonFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
