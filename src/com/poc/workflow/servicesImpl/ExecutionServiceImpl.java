package com.poc.workflow.servicesImpl;

import com.poc.workflow.exception.InactiveWorkflowExecutionException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.util.DataStore;

public class ExecutionServiceImpl
        implements ExecutionService {

    @Override
    public WorkflowRun executeWorkflow(
            String workflowId,
            String triggeredBy)
            throws WorkflowNotFoundException,
                   InactiveWorkflowExecutionException {

        // Step 1: Find Workflow
        Workflow workflow =
                DataStore.workflows.get(workflowId);

        if (workflow == null) {

            throw new WorkflowNotFoundException(
                    "Workflow not found");
        }

        // Step 2: Workflow must be active
        if (!workflow.isActive()) {

            throw new InactiveWorkflowExecutionException(
                    "Workflow is inactive");
        }

        // Step 3: Create Run Object
        WorkflowRun run =
                new WorkflowRun(
                        "RUN-" +
                        (DataStore.workflowRuns.size() + 1),
                        workflow,
                        triggeredBy);

        System.out.println(
                "\n===== EXECUTING WORKFLOW =====");

        // Step 4: Execute Tasks One By One
        for (Task task : workflow.getTasks()) {

            System.out.println(
                    "Starting Task : "
                            + task.getTaskName());

            run.markRunning(
                    task.getTaskId());

            // Simulate execution
            long executionTime =
                    task.getEstimatedTimeMs();

            run.markCompleted(
                    task.getTaskId(),
                    executionTime);

            System.out.println(
                    "Completed Task : "
                            + task.getTaskName()
                            + " ("
                            + executionTime
                            + " ms)");
        }

        // Step 5: Finish Workflow Run
        run.finish();

        // Step 6: Store Run History
        DataStore.workflowRuns.add(run);

        System.out.println(
                "\nWorkflow Execution Completed");

        return run;
    }
}