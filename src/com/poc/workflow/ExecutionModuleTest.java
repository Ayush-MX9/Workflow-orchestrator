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

public class ExecutionModuleTest {

    public static void main(String[] args) {
        try {
            WorkflowService workflowService = new WorkflowServiceImpl();
            ExecutionService executionService = new ExecutionServiceImpl();

            // the service generates the id, so always use the returned workflow's id
            Workflow wf = workflowService.createWorkflow("ETL Pipeline", "Extract, transform, load");
            String id = wf.getWorkflowId();

            // tasks go through WorkflowService, so duplicate/cycle rules apply
            workflowService.addTask(id, new Task("T1", "Extract", TaskPriority.HIGH, 1000));
            workflowService.addTask(id, new Task("T2", "Transform", TaskPriority.MEDIUM, 1200, List.of("T1")));
            workflowService.addTask(id, new Task("T3", "Load", TaskPriority.LOW, 800, List.of("T2")));

            // only active workflows can run
            workflowService.setWorkflowActive(id, true);

            WorkflowRun run = executionService.executeWorkflow(id, "admin");

            System.out.println("\n===== RUN SUMMARY =====");
            System.out.println(run);
            System.out.println("Status       : " + run.getStatus());
            System.out.println("Completed    : " + run.getCompletedCount());
            System.out.println("Failed       : " + run.getFailedCount());
            System.out.println("Total time   : " + run.getTotalTimeMs() + " ms");
            run.getTaskStatuses().forEach((taskId, status) ->
                    System.out.println("  " + taskId + " -> " + status));

        } catch (Exception e) {
            System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}