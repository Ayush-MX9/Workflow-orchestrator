package com.poc.workflow.console;

import java.util.List;
import java.util.Map;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.service.ReportService;
import com.poc.workflow.service.UserService;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.servicesImpl.ReportServiceImpl;
import com.poc.workflow.servicesImpl.UserServiceImpl;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;
import com.poc.workflow.util.InputUtil;
import com.poc.workflow.util.LogUtil;

// Admin menu. Reads input, calls the services, shows results or error messages.
public class AdminMenu {

    private final AbstractUser currentUser;
    private final ExecutionService executionService;   // shared with the other menus (one thread pool)
    private final WorkflowService workflowService = new WorkflowServiceImpl();
    private final UserService userService = new UserServiceImpl();
    private final ReportService reportService = new ReportServiceImpl();

    // used by MainMenu so every menu shares one executor
    public AdminMenu(AbstractUser currentUser, ExecutionService executionService) {
        this.currentUser = currentUser;
        this.executionService = executionService;
    }

    // kept so older code and tests still compile
    public AdminMenu(AbstractUser currentUser) {
        this(currentUser, new ExecutionServiceImpl());
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
            System.out.println("10. Run Workflow Multiple Times (parallel)");
            System.out.println("11. View Live Progress");
            System.out.println("12. Execute Multiple Active Workflows Concurrently");
            System.out.println("13. Logout");

            int choice = InputUtil.readInt("Enter choice: ", 1, 13);

            switch (choice) {
                case 1 -> createWorkflow();
                case 2 -> editWorkflow();
                case 3 -> deleteWorkflow();
                case 4 -> toggleWorkflow();
                case 5 -> new TaskMenu(workflowService).show();
                case 6 -> showWorkflows();
                case 7 -> viewRuns();
                case 8 -> viewAnalytics();
                case 9 -> viewUsers();
                case 10 -> RunActions.runMultiple(executionService, workflowService, currentUser.getUsername());
                case 11 -> RunActions.showProgress();
                case 12 -> RunActions.executeAllActiveWorkflowsConcurrently(
                        executionService, workflowService, currentUser.getUsername());
                case 13 -> running = false;
            }
        }
    }

    // ---------- workflows ----------

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
        // Enter = keep the current value (the service ignores blank values)
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

    // ---------- runs and analytics ----------

    // Option 7: all runs, optional status filter, optional run details
    private void viewRuns() {
        System.out.println("1. All runs  2. SUCCESS  3. PARTIAL_SUCCESS  4. FAILED");
        int choice = InputUtil.readInt("Filter: ", 1, 4);

        List<WorkflowRun> runs = switch (choice) {
            case 2 -> reportService.getRunsByStatus(RunStatus.SUCCESS);
            case 3 -> reportService.getRunsByStatus(RunStatus.PARTIAL_SUCCESS);
            case 4 -> reportService.getRunsByStatus(RunStatus.FAILED);
            default -> reportService.getAllRuns();
        };
        if (runs.isEmpty()) {
            System.out.println("No runs found.");
            return;
        }

        int[] widths = {10, 22, 12, 16, 20, 10};
        LogUtil.header("WORKFLOW RUNS");
        System.out.println(LogUtil.row(widths, "Run ID", "Workflow", "By", "Status", "Started", "Time"));
        System.out.println(LogUtil.separator(widths));
        for (WorkflowRun run : runs) {
            System.out.println(LogUtil.row(widths, run.getRunId(), run.getWorkflowName(),
                    run.getTriggeredBy(), String.valueOf(run.getStatus()),
                    LogUtil.formatDateTime(run.getStartTime()),
                    LogUtil.formatDuration(run.getTotalTimeMs())));
        }

        // Enter skips; otherwise show the task-wise summary of that run
        String runId = InputUtil.readOptionalString("Run id for details (Enter to skip): ");
        if (runId.isEmpty()) {
            return;
        }
        for (WorkflowRun run : runs) {
            if (run.getRunId().equalsIgnoreCase(runId)) {
                LogUtil.printWorkflowRunSummary(run);
                return;
            }
        }
        System.out.println("No run with id " + runId + " in this list.");
    }

    // Option 8: analytics sub-menu
    private void viewAnalytics() {
        System.out.println("1. Top 3 workflows by runs");
        System.out.println("2. Tasks with highest failure count");
        System.out.println("3. Task average execution time (one workflow)");
        int choice = InputUtil.readInt("Select: ", 1, 3);

        switch (choice) {
            case 1 -> {
                Map<String, Long> top = reportService.getTopWorkflowsByRunCount();
                if (top.isEmpty()) {
                    System.out.println("No runs yet.");
                    return;
                }
                LogUtil.header("TOP 3 WORKFLOWS BY RUNS");
                top.forEach((workflow, count) -> System.out.println(workflow + "  ->  " + count + " run(s)"));
            }
            case 2 -> {
                Map<String, Long> failures = reportService.getTaskFailureCounts();
                if (failures.isEmpty()) {
                    System.out.println("No task failures recorded.");
                    return;
                }
                LogUtil.header("TASK FAILURE COUNTS (highest first)");
                failures.forEach((task, count) -> System.out.println(task + "  ->  " + count));
            }
            case 3 -> {
                if (!showWorkflows()) {
                    return;
                }
                String id = InputUtil.readString("Workflow id: ");
                try {
                    workflowService.getWorkflowById(id);   // validates the id
                } catch (WorkflowNotFoundException e) {
                    System.out.println("Error: " + e.getMessage());
                    return;
                }
                Map<String, Double> averages = reportService.getAverageTaskTimes(id);
                if (averages.isEmpty()) {
                    System.out.println("No executed tasks for this workflow yet.");
                    return;
                }
                LogUtil.header("AVERAGE TASK TIME (slowest first)");
                averages.forEach((task, avg) -> System.out.printf("%-8s %.1f ms%n", task, avg));
            }
        }
    }

    // ---------- users ----------

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
}