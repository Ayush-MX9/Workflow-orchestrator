package com.poc.workflow;

import java.util.List;

import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;
import com.poc.workflow.model.Workflow;
import java.util.List;

public class ExecutionModuleTest {

    public static void main(String[] args) {
        try {

            WorkflowServiceImpl workflowService =
                    new WorkflowServiceImpl();

            TaskService taskService =
                    new TaskServiceImpl();

            ExecutionService executionService =
                    new ExecutionServiceImpl();

            Workflow workflow = workflowService.createWorkflow(
                    "ETL Pipeline",
                    "Workflow used to validate execution");
            String workflowId = workflow.getWorkflowId();
            workflowService.setWorkflowActive(workflowId, true);

            taskService.addTask(
                    workflowId,
                    new Task(
                            "T1",
                            "Extract",
                            TaskPriority.HIGH,
                            1000));

            taskService.addTask(
                    workflowId,
                    new Task(
                            "T2",
                            "Transform",
                            TaskPriority.MEDIUM,
                            1200,
                            List.of("T1")));

            taskService.addTask(
                    workflowId,
                    new Task(
                            "T3",
                            "Load",
                            TaskPriority.LOW,
                            800,
                            List.of("T2")));

            WorkflowRun run =
                    executionService.executeWorkflow(
                            workflowId,
                            "admin");

            System.out.println("\n===== RUN SUMMARY =====");
            System.out.println(run);

            System.out.println(
                    "Completed Tasks : "
                            + run.getCompletedCount());

            System.out.println(
                    "Failed Tasks : "
                            + run.getFailedCount());

            System.out.println(
                    "Total Time : "
                            + run.getTotalTimeMs());
            if (run.getCompletedCount() != 3 || run.getFailedCount() != 0) {
                throw new AssertionError("Expected all three tasks to complete successfully");
            }

        } catch (Exception e) {
            // print the type too, so "Workflow not found" style messages are easier to trace
            System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}