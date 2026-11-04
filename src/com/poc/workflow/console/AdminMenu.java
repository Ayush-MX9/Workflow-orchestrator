package com.poc.workflow.console;

import java.util.List;
import com.poc.workflow.service.UserService;
import com.poc.workflow.servicesImpl.UserServiceImpl;
import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.service.ReportService;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.ReportServiceImpl;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;
import com.poc.workflow.util.InputUtil;
import com.poc.workflow.util.LogUtil;

// Admin menu. Reads input, calls WorkflowService, shows results or error messages.
public class AdminMenu {

    private final AbstractUser currentUser;
    private final UserService userService = new UserServiceImpl();
    private final WorkflowService workflowService = new WorkflowServiceImpl();
    private final ReportService reportService = new ReportServiceImpl();

    public AdminMenu(AbstractUser currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=========== ADMIN MENU ==========");
            System.out.println("Logged in as: " + currentUser.getUsername());
            System.out.println("1. Create Workflow");
            System.out.println("2. Edit Workflow");
            System.out.println("3. Delete Workflow");
            System.out.println("4. Activate / Deactivate Workflow");
            System.out.println("5. Manage Tasks in Workflow");
            System.out.println("6. View All Workflows");
            System.out.println("7. View Workflow Runs");
            System.out.println("8. View Analytics");
            System.out.println("9. View Registered Users");
            System.out.println("10. Logout");

            int choice = InputUtil.readInt("Enter choice: ", 1, 10);
            switch (choice) {
                case 1 -> createWorkflow();
                case 2 -> editWorkflow();
                case 3 -> deleteWorkflow();
                case 4 -> toggleWorkflow();
                case 5 -> new TaskMenu(workflowService).show();
                case 6 -> viewAllWorkflows();
                case 7 -> viewWorkflowRuns(reportService.getAllRuns());
                case 8 -> showAnalyticsMenu();
                case 9 -> viewUsers();
                case 10 -> running = false;
            }
        }
    }

    private void viewUsers() {
        int[] widths = {16, 10, 10};
        LogUtil.header("REGISTERED USERS");
        System.out.println(LogUtil.row(widths, "Username", "Role", "Session"));
        System.out.println(LogUtil.separator(widths));
        for (AbstractUser u : userService.getAllUsers()) {
            System.out.println(LogUtil.row(widths, u.getUsername(), u.getRole().name(),
                    u.isLoggedIn() ? "ACTIVE" : "-"));
        }
    }
    private void createWorkflow() {
        String name = InputUtil.readString("Workflow name: ");
        String description = InputUtil.readString("Description: ");
        try {
            Workflow workflow = workflowService.createWorkflow(name, description);
            System.out.println("Created: " + workflow);
            System.out.println("It is INACTIVE. Add tasks (option 5), then activate it (option 4).");
        } catch (WorkflowAlreadyExistsException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void editWorkflow() {
        if (!showWorkflows()) {
            return;
        }
        String id = InputUtil.readString("Workflow id to edit: ");
        // Enter = keep current value (the service ignores blank values)
        String newName = InputUtil.readOptionalString("New name (Enter to keep): ");
        String newDescription = InputUtil.readOptionalString("New description (Enter to keep): ");
        try {
            workflowService.updateWorkflow(id, newName, newDescription);
            System.out.println("Updated: " + workflowService.getWorkflowById(id));
        } catch (WorkflowNotFoundException | WorkflowAlreadyExistsException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void deleteWorkflow() {
        if (!showWorkflows()) {
            return;
        }
        String id = InputUtil.readString("Workflow id to delete: ");
        if (!InputUtil.readYesNo("Delete " + id + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        try {
            workflowService.deleteWorkflow(id);
            System.out.println("Deleted " + id + ". Past runs stay in the history.");
        } catch (WorkflowNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void toggleWorkflow() {
        if (!showWorkflows()) {
            return;
        }
        String id = InputUtil.readString("Workflow id: ");
        System.out.println("1. Activate   2. Deactivate");
        int choice = InputUtil.readInt("Select: ", 1, 2);
        try {
            workflowService.setWorkflowActive(id, choice == 1);
            System.out.println("Updated: " + workflowService.getWorkflowById(id));
        } catch (WorkflowNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewAllWorkflows() {
        showWorkflows();
    }

    private void viewWorkflowRuns(List<WorkflowRun> runs) {
        if (runs.isEmpty()) {
            System.out.println("No workflow runs found.");
            return;
        }
        int[] widths = {12, 24, 16, 16, 22};
        LogUtil.header("WORKFLOW RUNS");
        System.out.println(LogUtil.row(widths, "Run ID", "Workflow", "Status",
                "Simulated time", "Triggered by"));
        System.out.println(LogUtil.separator(widths));
        for (WorkflowRun run : runs) {
            System.out.println(LogUtil.row(widths, run.getRunId(), run.getWorkflowName(),
                    run.getStatus() == null ? "RUNNING" : run.getStatus().name(),
                    LogUtil.formatDuration(run.getTotalTimeMs()), run.getTriggeredBy()));
        }
    }

    private void showAnalyticsMenu() {
        boolean showing = true;
        while (showing) {
            System.out.println("\n========== WORKFLOW REPORTS ==========");
            System.out.println("1. Filter runs by status");
            System.out.println("2. Top 3 workflows by run count");
            System.out.println("3. Average task time in a workflow");
            System.out.println("4. Task failure counts");
            System.out.println("5. Back");
            int choice = InputUtil.readInt("Enter choice: ", 1, 5);
            switch (choice) {
                case 1 -> filterRunsByStatus();
                case 2 -> showTopWorkflows();
                case 3 -> showAverageTaskTimes();
                case 4 -> showTaskFailureCounts();
                case 5 -> showing = false;
            }
        }
    }

    private void filterRunsByStatus() {
        System.out.println("1. SUCCESS  2. PARTIAL_SUCCESS  3. FAILED");
        int choice = InputUtil.readInt("Select status: ", 1, 3);
        RunStatus status = switch (choice) {
            case 1 -> RunStatus.SUCCESS;
            case 2 -> RunStatus.PARTIAL_SUCCESS;
            default -> RunStatus.FAILED;
        };
        viewWorkflowRuns(reportService.getRunsByStatus(status));
    }

    private void showTopWorkflows() {
        var workflows = reportService.getTopWorkflowsByRunCount();
        if (workflows.isEmpty()) {
            System.out.println("No workflow runs found.");
            return;
        }
        LogUtil.header("TOP WORKFLOWS BY RUN COUNT");
        workflows.forEach((name, count) -> System.out.println(name + " | " + count + " run(s)"));
    }

    private void showAverageTaskTimes() {
        if (!showWorkflows()) {
            return;
        }
        String workflowId = InputUtil.readString("Workflow id: ");
        try {
            Workflow workflow = workflowService.getWorkflowById(workflowId);
            var averages = reportService.getAverageTaskTimes(workflowId);
            if (averages.isEmpty()) {
                System.out.println("No completed task timings for this workflow.");
                return;
            }
            LogUtil.header("AVERAGE TASK TIMES: " + workflow.getName());
            averages.forEach((taskId, average) ->
                    System.out.println(taskId + " | " + average + " ms"));
        } catch (WorkflowNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void showTaskFailureCounts() {
        var failures = reportService.getTaskFailureCounts();
        if (failures.isEmpty()) {
            System.out.println("No failed tasks recorded.");
            return;
        }
        LogUtil.header("TASK FAILURE COUNTS");
        failures.forEach((task, count) -> System.out.println(task + " | " + count + " failure(s)"));
    }

    // prints all workflows as a table; returns false if there are none
    private boolean showWorkflows() {
        List<Workflow> workflows = workflowService.getAllWorkflows();
        if (workflows.isEmpty()) {
            System.out.println("No workflows yet. Create one first (option 1).");
            return false;
        }
        int[] widths = {8, 24, 9, 6, 12};
        LogUtil.header("WORKFLOWS");
        System.out.println(LogUtil.row(widths, "ID", "Name", "Status", "Tasks", "Est. time"));
        System.out.println(LogUtil.separator(widths));
        for (Workflow w : workflows) {
            System.out.println(LogUtil.row(widths,
                    w.getWorkflowId(),
                    w.getName(),
                    w.isActive() ? "ACTIVE" : "INACTIVE",
                    String.valueOf(w.getTasks().size()),
                    LogUtil.formatDuration(w.getTotalEstimatedTimeMs())));
        }
        return true;
    }
}