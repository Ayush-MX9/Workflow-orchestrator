package com.poc.workflow.console;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.RejectedExecutionException;

import com.poc.workflow.exception.InactiveWorkflowExecutionException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.model.WorkflowRunContext;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.util.InputUtil;
import com.poc.workflow.util.LogUtil;
import com.poc.workflow.util.ProgressLogger;

// Phase 2 menu actions shared by the Admin and Operator menus.
public final class RunActions {

    private RunActions() {
    }

    // Triggers the same workflow several times, one after another. The runs execute
    // in parallel on the executor, so this method returns without waiting for them.
    public static void runMultiple(ExecutionService executionService,
                                   WorkflowService workflowService, String username) {
        List<Workflow> active = workflowService.getActiveWorkflows();
        if (active.isEmpty()) {
            System.out.println("No active workflows available.");
            return;
        }
        int[] widths = {10, 28, 8, 14};
        LogUtil.header("ACTIVE WORKFLOWS");
        System.out.println(LogUtil.row(widths, "ID", "Name", "Tasks", "Est. time"));
        System.out.println(LogUtil.separator(widths));
        for (Workflow w : active) {
            System.out.println(LogUtil.row(widths, w.getWorkflowId(), w.getName(),
                    String.valueOf(w.getTasks().size()),
                    LogUtil.formatDuration(w.getTotalEstimatedTimeMs())));
        }

        String workflowId = InputUtil.readString("Workflow id to run: ");
        int count = InputUtil.readInt("How many runs (5-20): ", 5, 20);   // spec: 5+ parallel runs

        int triggered = 0;
        for (int i = 1; i <= count; i++) {
            try {
                WorkflowRunContext context =
                        executionService.executeWorkflowAsync(workflowId, username);
                triggered++;
                System.out.println("Triggered " + context.getRun().getRunId()
                        + " (" + i + "/" + count + ")");
            } catch (WorkflowNotFoundException | InactiveWorkflowExecutionException e) {
                System.out.println("Error: " + e.getMessage());
                break;   // same workflow every time, so the next attempt would fail too
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Cannot execute workflow: " + e.getMessage());
                break;
            } catch (RejectedExecutionException e) {
                System.out.println("Executor is shut down; no more runs can start.");
                break;
            }
        }
        System.out.println(triggered + " run(s) triggered. Use \"View live progress\" to watch them,"
                + " or check run history when they finish.");
    }

    public static void showProgress() {
        ProgressLogger.printSnapshot();
    }

    public static void executeAllActiveWorkflowsConcurrently(
            ExecutionService executionService, WorkflowService workflowService, String username) {
        List<Workflow> active = workflowService.getActiveWorkflows();
        if (active.isEmpty()) {
            System.out.println("No active workflows available.");
            return;
        }

        ExecutorService dispatcher = Executors.newFixedThreadPool(
                Math.min(active.size(), 10), namedDispatcherFactory());
        List<Future<?>> dispatches = new ArrayList<>();
        try {
            for (Workflow workflow : active) {
                dispatches.add(dispatcher.submit(() -> {
                    try {
                        WorkflowRunContext context =
                                executionService.executeWorkflowAsync(workflow.getWorkflowId(), username);
                        System.out.println(Thread.currentThread().getName() + " -> "
                                + workflow.getWorkflowId() + " (" + context.getRun().getRunId() + ")");
                    } catch (WorkflowNotFoundException | InactiveWorkflowExecutionException
                             | IllegalArgumentException | IllegalStateException
                             | RejectedExecutionException e) {
                        System.out.println("Could not start " + workflow.getWorkflowId()
                                + ": " + e.getMessage());
                    }
                }));
            }
            for (Future<?> dispatch : dispatches) {
                try {
                    dispatch.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    dispatcher.shutdownNow();
                    System.out.println("Interrupted while starting active workflows.");
                    return;
                } catch (ExecutionException e) {
                    System.out.println("Unexpected error while starting active workflows: "
                            + e.getCause());
                }
            }
        } finally {
            dispatcher.shutdown();
        }
        System.out.println("All active workflows have been submitted for concurrent execution.");
    }

    private static ThreadFactory namedDispatcherFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> new Thread(runnable, "pool-thread-" + counter.incrementAndGet());
    }
}