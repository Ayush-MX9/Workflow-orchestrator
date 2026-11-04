package com.poc.workflow.servicesImpl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import com.poc.workflow.enums.TaskStatus;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.util.IdGenerator;
import com.poc.workflow.util.LogUtil;

final class TaskExecutor implements Runnable {

    private static final int MAX_RETRIES = 2;

    private final Task task;
    private final WorkflowRun run;
    private final double failureProbability;
    private final RetryHandler retryHandler;
    private final CompletableFuture<Void> completion;

    TaskExecutor(Task task, WorkflowRun run, double failureProbability,
                 RetryHandler retryHandler, CompletableFuture<Void> completion) {
        this.task = task;
        this.run = run;
        this.failureProbability = failureProbability;
        this.retryHandler = retryHandler;
        this.completion = completion;
    }

    @Override
    public void run() {
        executeAttempt(0);
    }

    private void executeAttempt(int retryNumber) {
        boolean dependenciesCompleted = task.getDependsOnTaskIds().stream()
                .allMatch(dependencyId ->
                        run.getTaskStatus(dependencyId) == TaskStatus.COMPLETED);
        if (!dependenciesCompleted) {
            run.markFailed(task.getTaskId(), 0);
            LogUtil.taskFailed(run, task.getTaskId());
            completion.complete(null);
            return;
        }

        if (!task.getDependsOnTaskIds().isEmpty()) {
            LogUtil.dependenciesSatisfied(run, task.getTaskId());
        }

        if (!run.tryMarkRunning(task.getTaskId())) {
            completion.completeExceptionally(
                    new IllegalStateException("Task is not ready to run: " + task.getTaskId()));
            return;
        }

        run.recordTaskWorker(task.getTaskId(), Thread.currentThread().getName());
        IdGenerator.recordTaskStarted();
        LogUtil.taskStarting(run, task.getTaskId());
        long executionTime = task.getEstimatedTimeMs();

        try {
            Thread.sleep(executionTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            run.markFailed(task.getTaskId(), executionTime);
            IdGenerator.recordTaskFailed();
            LogUtil.taskFailed(run, task.getTaskId());
            completion.complete(null);
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() < failureProbability) {
            run.markFailed(task.getTaskId(), executionTime);
            IdGenerator.recordTaskFailed();
            LogUtil.taskFailed(run, task.getTaskId());

            if (retryNumber < MAX_RETRIES) {
                int retry = run.incrementRetryCount(task.getTaskId());
                LogUtil.retryScheduled(run, retry, MAX_RETRIES);
                try {
                    retryHandler.scheduleRetry(() -> executeAttempt(retryNumber + 1));
                } catch (RejectedExecutionException e) {
                    LogUtil.info("Retry could not be scheduled for Task " + task.getTaskId()
                            + ": " + e.getMessage());
                    completion.complete(null);
                }
                return;
            }

            LogUtil.taskFailedAfterRetries(run, task.getTaskId());
            completion.complete(null);
            return;
        }

        run.markCompleted(task.getTaskId(), executionTime);
        IdGenerator.recordTaskCompleted();
        LogUtil.taskCompleted(run, task.getTaskId(), executionTime);
        completion.complete(null);
    }
}
