package com.poc.workflow.console;

import com.poc.workflow.exception.InactiveWorkflowExecutionException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.service.ReportService;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.servicesImpl.ReportServiceImpl;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;
import com.poc.workflow.util.InputUtil;
import com.poc.workflow.util.LogUtil;
import java.util.List;

public class OperatorMenu {

private final AbstractUser currentUser;
private final WorkflowService workflowService = new WorkflowServiceImpl();
private final ExecutionService executionService;
private final ReportService reportService = new ReportServiceImpl();

// used by MainMenu: all menus share one executor, so a single shutdown() stops everything
public OperatorMenu(AbstractUser currentUser, ExecutionService executionService) {
    this.currentUser = currentUser;
    this.executionService = executionService;
}

// kept so older code and tests still compile
public OperatorMenu(AbstractUser currentUser) {
    this(currentUser, new ExecutionServiceImpl());
}

public void show() {
    boolean running = true;
    while (running) {
        System.out.println("\n========= OPERATOR MENU =========");
        System.out.println("Logged in as: " + currentUser.getUsername());
        System.out.println("1. View Active Workflows");
        System.out.println("2. Execute Workflow");
        System.out.println("3. View My Workflow Runs");
        System.out.println("4. View My Run Details");
        System.out.println("5. Run Workflow Multiple Times (parallel)");
        System.out.println("6. View Live Progress");
        System.out.println("7. Execute Multiple Active Workflows Concurrently");
        System.out.println("8. Logout");
        int choice = InputUtil.readInt("Enter choice: ", 1, 8);
        switch (choice) {
            case 1 -> showActiveWorkflows();
            case 2 -> executeWorkflow();
            case 3 -> showMyRuns();
            case 4 -> showRunDetails();
            case 5 -> RunActions.runMultiple(executionService, workflowService, currentUser.getUsername());
            case 6 -> RunActions.showProgress();
            case 7 -> RunActions.executeAllActiveWorkflowsConcurrently(
                    executionService, workflowService, currentUser.getUsername());
            case 8 -> running = false;
        }
    }
}

    private void showActiveWorkflows() {
        List<Workflow> workflows = workflowService.getActiveWorkflows();
        if (workflows.isEmpty()) {
            System.out.println("No active workflows available.");
            return;
        }
        int[] widths = {10, 28, 8, 14};
        LogUtil.header("ACTIVE WORKFLOWS");
        System.out.println(LogUtil.row(widths, "ID", "Name", "Tasks", "Est. time"));
        System.out.println(LogUtil.separator(widths));
        for (Workflow workflow : workflows) {
            System.out.println(LogUtil.row(widths, workflow.getWorkflowId(), workflow.getName(),
                    String.valueOf(workflow.getTasks().size()),
                    LogUtil.formatDuration(workflow.getTotalEstimatedTimeMs())));
            showWorkflowTasks(workflow);
        }
    }

    private void showWorkflowTasks(Workflow workflow) {
        if (workflow.getTasks().isEmpty()) {
            System.out.println("  No tasks configured.");
            return;
        }

        int[] widths = {10, 30, 10, 14, 20};
        System.out.println("  Tasks in " + workflow.getName() + ":");
        System.out.println("  " + LogUtil.row(widths, "Task ID", "Task Name", "Priority",
                "Est. time", "Depends on"));
        System.out.println("  " + LogUtil.separator(widths));
        for (Task task : workflow.getTasks()) {
            System.out.println("  " + LogUtil.row(widths, task.getTaskId(), task.getTaskName(),
                    task.getPriority().name(), LogUtil.formatDuration(task.getEstimatedTimeMs()),
                    task.getDependsOnTaskIds().isEmpty()
                            ? "-"
                            : String.join(", ", task.getDependsOnTaskIds())));
        }
    }

    private void executeWorkflow() {
        if (workflowService.getActiveWorkflows().isEmpty()) {
            System.out.println("No active workflows available.");
            return;
        }
        showActiveWorkflows();
        String workflowId = InputUtil.readString("Workflow id to execute: ");
        try {
            WorkflowRun run = executionService.executeWorkflow(workflowId, currentUser.getUsername());
            System.out.println("Run saved: " + run.getRunId() + " (" + run.getStatus() + ")");
        } catch (WorkflowNotFoundException | InactiveWorkflowExecutionException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Cannot execute workflow: " + e.getMessage());
        }
    }

    private void showMyRuns() {
        List<WorkflowRun> runs = reportService.getRunsByUser(currentUser.getUsername());
        if (runs.isEmpty()) {
            System.out.println("You have not triggered any workflow runs.");
            return;
        }
        int[] widths = {12, 24, 18, 16};
        LogUtil.header("MY WORKFLOW RUNS");
        System.out.println(LogUtil.row(widths, "Run ID", "Workflow", "Status", "Simulated time"));
        System.out.println(LogUtil.separator(widths));
        for (WorkflowRun run : runs) {
            System.out.println(LogUtil.row(widths, run.getRunId(), run.getWorkflowName(),
                    String.valueOf(run.getStatus()), LogUtil.formatDuration(run.getTotalTimeMs())));
        }
    }

    private void showRunDetails() {
        List<WorkflowRun> runs = reportService.getRunsByUser(currentUser.getUsername());
        if (runs.isEmpty()) {
            System.out.println("You have not triggered any workflow runs.");
            return;
        }
        showMyRuns();
        String runId = InputUtil.readString("Run id: ");
        for (WorkflowRun run : runs) {
            if (run.getRunId().equals(runId)) {
                LogUtil.printWorkflowRunSummary(run);
                return;
            }
        }
        System.out.println("No run found with id " + runId + " for your account.");
    }
}