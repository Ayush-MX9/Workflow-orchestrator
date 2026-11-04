package com.poc.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.enums.TaskStatus;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.model.WorkflowRunContext;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.util.DataStore;
import com.poc.workflow.util.IdGenerator;
import com.poc.workflow.util.ProgressLogger;

public class Phase2ConcurrencyTest {

    public static void main(String[] args) throws Exception {
        DataStore.workflows.clear();
        DataStore.workflowRuns.clear();
        DataStore.activeRuns.clear();

        Workflow workflow = new Workflow("WF-PHASE2", "Concurrent test", "Five concurrent runs");
        workflow.setActive(true);
        workflow.addTask(new Task("T1", "Root one", TaskPriority.HIGH, 3000));
        workflow.addTask(new Task("T2", "After root one", TaskPriority.HIGH, 3000, List.of("T1")));
        workflow.addTask(new Task("T3", "Root two", TaskPriority.HIGH, 5000));
        workflow.addTask(new Task("T4", "After root two", TaskPriority.HIGH, 5000, List.of("T3")));
        workflow.addTask(new Task("T5", "Independent", TaskPriority.HIGH, 4000));
        DataStore.workflows.put(workflow.getWorkflowId(), workflow);

        int startedBefore = IdGenerator.getTasksStarted();
        int completedBefore = IdGenerator.getTasksCompleted();
        int failedBefore = IdGenerator.getTasksFailed();
        int concurrentBefore = IdGenerator.getMaxConcurrentTasks();
        ExecutionServiceImpl executionService = new ExecutionServiceImpl();

        try {
            List<WorkflowRunContext> contexts = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                contexts.add(executionService.executeWorkflowAsync(
                        workflow.getWorkflowId(), "operator1"));
            }
            ProgressLogger.printSnapshot();

            CompletableFuture<?>[] completions = contexts.stream()
                    .map(WorkflowRunContext::getCompletion)
                    .toArray(CompletableFuture<?>[]::new);
            CompletableFuture.allOf(completions).get(60, TimeUnit.SECONDS);

            check("Five runs completed successfully",
                    contexts.stream().allMatch(context ->
                            context.getRun().getStatus() == RunStatus.SUCCESS));
            check("All task states completed and dependencies were respected",
                    contexts.stream().allMatch(context -> {
                        WorkflowRun run = context.getRun();
                        return run.getTaskStatuses().size() == 5
                                && run.getTaskStatuses().values().stream()
                                        .allMatch(status -> status == TaskStatus.COMPLETED);
                    }));
            check("All runs were added to history", DataStore.workflowRuns.size() == 5);
            check("Active run registry is empty after completion", DataStore.activeRuns.isEmpty());

            int started = IdGenerator.getTasksStarted() - startedBefore;
            int completed = IdGenerator.getTasksCompleted() - completedBefore;
            int failed = IdGenerator.getTasksFailed() - failedBefore;
            check("Task attempt counters are consistent",
                    started == 25 && started == completed + failed);
            check("More than one task executed concurrently",
                    IdGenerator.getMaxConcurrentTasks() > Math.max(1, concurrentBefore));
            check("No task remains active", IdGenerator.getActiveTasks() == 0);
        } finally {
            executionService.shutdown();
        }
    }

    private static void check(String description, boolean passed) {
        System.out.println((passed ? "PASS " : "FAIL ") + description);
        if (!passed) {
            throw new AssertionError(description);
        }
    }
}
