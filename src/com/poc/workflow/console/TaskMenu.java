package com.poc.workflow.console;

import java.util.ArrayList;
import java.util.List;

import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.exception.CircularDependencyException;
import com.poc.workflow.exception.TaskAlreadyExistsException;
import com.poc.workflow.exception.TaskNotFoundException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.util.InputUtil;
import com.poc.workflow.util.LogUtil;

// Sub-menu for managing the tasks of one workflow (Admin menu option 5).
public class TaskMenu {

    private final WorkflowService workflowService;

    public TaskMenu(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void show() {
        // 1. choose the workflow to work on
        List<Workflow> workflows = workflowService.getAllWorkflows();
        if (workflows.isEmpty()) {
            System.out.println("No workflows yet. Create one first (option 1).");
            return;
        }
        printWorkflowList(workflows);
        String workflowId = InputUtil.readString("Workflow id: ");

        Workflow workflow;
        try {
            workflow = workflowService.getWorkflowById(workflowId);
        } catch (WorkflowNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // 2. task menu loop for that workflow
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("====== TASKS: " + workflow.getName() + " ======");
            System.out.println("1. Add Task");
            System.out.println("2. Edit Task");
            System.out.println("3. Remove Task");
            System.out.println("4. View Tasks");
            System.out.println("5. Back");

            int choice = InputUtil.readInt("Enter choice: ", 1, 5);
            switch (choice) {
                case 1 -> addTask(workflow);
                case 2 -> editTask(workflow);
                case 3 -> removeTask(workflow);
                case 4 -> viewTasks(workflow);
                case 5 -> running = false;
            }
        }
    }

    private void addTask(Workflow workflow) {
        String taskId = InputUtil.readString("Task id (e.g. T1): ");
        String taskName = InputUtil.readString("Task name: ");
        TaskPriority priority = readPriority();
        long baseTimeMs = InputUtil.readPositiveLong("Base execution time (ms): ");
        List<String> dependsOn = readDependencies("Depends on task ids (comma separated, Enter for none): ");

        try {
            workflowService.addTask(workflow.getWorkflowId(),
                    new Task(taskId, taskName, priority, baseTimeMs, dependsOn));
            System.out.println("Task added: " + taskId);
        } catch (TaskAlreadyExistsException | CircularDependencyException | WorkflowNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // dependency points to a task that does not exist in this workflow
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void editTask(Workflow workflow) {
        if (!viewTasks(workflow)) {
            return;
        }
        String taskId = InputUtil.readString("Task id to edit: ");

        // Enter = keep current value; the service ignores null values
        String newName = InputUtil.readOptionalString("New name (Enter to keep): ");

        TaskPriority newPriority = null;
        if (InputUtil.readYesNo("Change priority?")) {
            newPriority = readPriority();
        }

        Long newBaseTimeMs = null;
        if (InputUtil.readYesNo("Change base execution time?")) {
            newBaseTimeMs = InputUtil.readPositiveLong("New base execution time (ms): ");
        }

        List<String> newDependsOn = null;
        if (InputUtil.readYesNo("Change dependencies?")) {
            newDependsOn = readDependencies("New dependencies (comma separated, Enter for none): ");
        }

        try {
            workflowService.updateTask(workflow.getWorkflowId(), taskId,
                    newName, newPriority, newBaseTimeMs, newDependsOn);
            System.out.println("Task updated: " + taskId);
        } catch (TaskNotFoundException | CircularDependencyException | WorkflowNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void removeTask(Workflow workflow) {
        if (!viewTasks(workflow)) {
            return;
        }
        String taskId = InputUtil.readString("Task id to remove: ");
        if (!InputUtil.readYesNo("Remove " + taskId + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        try {
            workflowService.removeTask(workflow.getWorkflowId(), taskId);
            System.out.println("Task removed: " + taskId);
        } catch (TaskNotFoundException | WorkflowNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            // another task still depends on this one
            System.out.println("Error: " + e.getMessage());
        }
    }

    // prints the tasks in execution order; returns false if there are none
    private boolean viewTasks(Workflow workflow) {
        List<Task> tasks = workflow.getTasks();
        if (tasks.isEmpty()) {
            System.out.println("This workflow has no tasks yet.");
            return false;
        }
        int[] widths = {6, 20, 8, 10, 12, 14};
        LogUtil.header("TASKS (execution order)");
        System.out.println(LogUtil.row(widths, "ID", "Name", "Priority", "Base (ms)", "Est. (ms)", "Depends on"));
        System.out.println(LogUtil.separator(widths));
        for (Task t : tasks) {
            System.out.println(LogUtil.row(widths,
                    t.getTaskId(),
                    t.getTaskName(),
                    t.getPriority().name(),
                    String.valueOf(t.getBaseExecutionTimeMs()),
                    String.valueOf(t.getEstimatedTimeMs()),
                    t.getDependsOnTaskIds().isEmpty() ? "-" : String.join(",", t.getDependsOnTaskIds())));
        }
        System.out.println("Total estimated time: " + LogUtil.formatDuration(workflow.getTotalEstimatedTimeMs()));
        return true;
    }

    // ---------- input helpers ----------

    // maps 1/2/3 to the enum, so the user never types an enum name
    private TaskPriority readPriority() {
        System.out.println("Priority: 1. HIGH (+0 ms)  2. MEDIUM (+50 ms)  3. LOW (+100 ms)");
        int choice = InputUtil.readInt("Select priority: ", 1, 3);
        return switch (choice) {
            case 1 -> TaskPriority.HIGH;
            case 2 -> TaskPriority.MEDIUM;
            default -> TaskPriority.LOW;
        };
    }

    // "T1, T2" -> ["T1", "T2"]; empty input -> empty list
    private List<String> readDependencies(String prompt) {
        String input = InputUtil.readOptionalString(prompt);
        List<String> ids = new ArrayList<>();
        if (input.isEmpty()) {
            return ids;
        }
        for (String part : input.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    private void printWorkflowList(List<Workflow> workflows) {
        int[] widths = {8, 24, 9, 6};
        LogUtil.header("WORKFLOWS");
        System.out.println(LogUtil.row(widths, "ID", "Name", "Status", "Tasks"));
        System.out.println(LogUtil.separator(widths));
        for (Workflow w : workflows) {
            System.out.println(LogUtil.row(widths, w.getWorkflowId(), w.getName(),
                    w.isActive() ? "ACTIVE" : "INACTIVE", String.valueOf(w.getTasks().size())));
        }
    }
}