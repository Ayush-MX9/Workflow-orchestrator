package com.poc.workflow.console;

import java.util.List;
import com.poc.workflow.service.UserService;
import com.poc.workflow.servicesImpl.UserServiceImpl;
import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;
import com.poc.workflow.util.InputUtil;
import com.poc.workflow.util.LogUtil;

// Admin menu. Reads input, calls WorkflowService, shows results or error messages.
public class AdminMenu {

    private final AbstractUser currentUser;
    private final WorkflowService workflowService = new WorkflowServiceImpl();
    private final UserService userService = new UserServiceImpl();

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
                case 7, 8 -> System.out.println("Coming soon (needs ReportService).");
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