package com.poc.workflow.servicesImpl;

import com.poc.workflow.exception.InactiveWorkflowExecutionException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.enums.TaskStatus;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.util.DataStore;
import com.poc.workflow.util.DependencyUtil;
import com.poc.workflow.util.IdGenerator;
import com.poc.workflow.util.LogUtil;
import java.util.List;

public class ExecutionServiceImpl
        implements ExecutionService {

    private static final double FAILURE_PROBABILITY = 0.0;

    private final double failureProbability;

    public ExecutionServiceImpl() {
        this(FAILURE_PROBABILITY);
    }

    public ExecutionServiceImpl(double failureProbability) {
        if (failureProbability < 0.0 || failureProbability > 1.0) {
            throw new IllegalArgumentException("Failure probability must be between 0 and 1");
        }
        this.failureProbability = failureProbability;
    }

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

        if (workflow.getTasks().isEmpty()) {
            throw new IllegalArgumentException("Cannot execute a workflow with no tasks");
        }

        // Validate and order tasks before creating a run, so invalid workflows are not recorded.
        List<Task> executionOrder = DependencyUtil.sortByDependencies(workflow.getTasks());

        // Step 3: Create Run Object
        WorkflowRun run =
                new WorkflowRun(
                        IdGenerator.nextRunId(),
                        workflow,
                        triggeredBy);

        LogUtil.header("EXECUTING WORKFLOW");

        // Step 4: Execute Tasks One By One
        for (Task task : executionOrder) {
            boolean dependenciesCompleted = task.getDependsOnTaskIds().stream()
                    .allMatch(dependencyId ->
                            run.getTaskStatuses().get(dependencyId)
                                    == TaskStatus.COMPLETED);
            if (!dependenciesCompleted) {
                run.markFailed(task.getTaskId(), 0);
                LogUtil.taskFailed(task.getTaskId(), task.getTaskName());
                continue;
            }

            LogUtil.taskStarting(task.getTaskId(), task.getTaskName());
            run.markRunning(
                    task.getTaskId());

            long executionTime =
                    task.getEstimatedTimeMs();
            if (Math.random() < failureProbability) {
                run.markFailed(task.getTaskId(), executionTime);
                LogUtil.taskFailed(task.getTaskId(), task.getTaskName());
            } else {
                run.markCompleted(task.getTaskId(), executionTime);
                LogUtil.taskCompleted(task.getTaskId(), task.getTaskName(), executionTime);
            }
        }

        // Step 5: Finish Workflow Run
        run.finish();
        LogUtil.printWorkflowRunSummary(run);

        // Step 6: Store Run History
        DataStore.workflowRuns.add(run);

        LogUtil.info("Workflow execution finished with status " + run.getStatus());

        return run;
    }
}