package com.poc.workflow;

import java.util.List;

import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.exception.CircularDependencyException;
import com.poc.workflow.exception.TaskAlreadyExistsException;
import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;

// Manual test runner for WorkflowService. Prints PASS/FAIL per case.
public class WorkflowModuleTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        WorkflowService service = new WorkflowServiceImpl();

        // ---- workflows ----
        Workflow wf = service.createWorkflow("Payroll", "Monthly payroll");
        String id = wf.getWorkflowId();
        check("Create workflow returns generated id", id != null && id.startsWith("WF-"));
        check("New workflow starts inactive", !wf.isActive());

        try {
            service.createWorkflow("payroll", "same name, different case");
            check("Duplicate name rejected (case-insensitive)", false);
        } catch (WorkflowAlreadyExistsException e) {
            check("Duplicate name rejected (case-insensitive)", true);
        }

        check("Get by id works", service.getWorkflowById(id) == wf);
        check("Get by name works", service.getWorkflowByName("PAYROLL") == wf);

        try {
            service.getWorkflowById("WF-999");
            check("Unknown id throws", false);
        } catch (WorkflowNotFoundException e) {
            check("Unknown id throws", true);
        }

        service.setWorkflowActive(id, true);
        check("Activate works", wf.isActive());
        check("Active list contains it", service.getActiveWorkflows().contains(wf));
        service.setWorkflowActive(id, false);
        check("Deactivate works", !wf.isActive());
        check("Active list no longer contains it", !service.getActiveWorkflows().contains(wf));

        service.updateWorkflow(id, "Payroll v2", null);
        check("Update name works, blank description kept",
                wf.getName().equals("Payroll v2") && wf.getDescription().equals("Monthly payroll"));

        Workflow other = service.createWorkflow("Billing", "Invoices");
        try {
            service.updateWorkflow(other.getWorkflowId(), "payroll v2", null);
            check("Rename to an existing name rejected", false);
        } catch (WorkflowAlreadyExistsException e) {
            check("Rename to an existing name rejected", true);
        }
        service.updateWorkflow(id, "PAYROLL V2", null);
        check("Workflow may change case of its own name", wf.getName().equals("PAYROLL V2"));

        // ---- tasks ----
        service.addTask(id, new Task("T1", "Collect data", TaskPriority.HIGH, 100));
        service.addTask(id, new Task("T2", "Calculate", TaskPriority.MEDIUM, 200, List.of("T1")));
        check("Two tasks added in order", wf.getTasks().size() == 2);

        try {
            service.addTask(id, new Task("T1", "Duplicate", TaskPriority.LOW, 50));
            check("Duplicate taskId rejected", false);
        } catch (TaskAlreadyExistsException e) {
            check("Duplicate taskId rejected", true);
        }

        try {
            service.addTask(id, new Task("T3", "Bad dep", TaskPriority.LOW, 50, List.of("X")));
            check("Unknown dependency rejected", false);
        } catch (IllegalArgumentException e) {
            check("Unknown dependency rejected", true);
        }
        check("Rejected tasks were not added", wf.getTasks().size() == 2);

        // T2 already depends on T1, so making T1 depend on T2 forms T1 -> T2 -> T1
        try {
            service.updateTask(id, "T1", null, null, null, List.of("T2"));
            check("Cycle T1 -> T2 -> T1 detected", false);
        } catch (CircularDependencyException e) {
            check("Cycle T1 -> T2 -> T1 detected", true);
            System.out.println("      message: " + e.getMessage());
        }
        check("Failed update left T1 unchanged", wf.findTaskById("T1").getDependsOnTaskIds().isEmpty());

        try {
            service.updateTask(id, "T1", null, null, null, List.of("T1"));
            check("Self-dependency detected", false);
        } catch (CircularDependencyException e) {
            check("Self-dependency detected", true);
        }

        try {
            service.removeTask(id, "T1");
            check("Cannot remove a task others depend on", false);
        } catch (IllegalStateException e) {
            check("Cannot remove a task others depend on", true);
        }
        service.removeTask(id, "T2");
        service.removeTask(id, "T1");
        check("Tasks removable in reverse order", wf.getTasks().isEmpty());

        // ---- delete ----
        service.deleteWorkflow(id);
        try {
            service.getWorkflowById(id);
            check("Deleted workflow is gone", false);
        } catch (WorkflowNotFoundException e) {
            check("Deleted workflow is gone", true);
        }

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}