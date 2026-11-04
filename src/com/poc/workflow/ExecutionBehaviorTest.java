package com.poc.workflow;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.enums.TaskStatus;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ReportService;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.servicesImpl.ReportServiceImpl;
import com.poc.workflow.util.DataStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecutionBehaviorTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        DataStore.workflows.clear();
        DataStore.workflowRuns.clear();

        Workflow workflow = new Workflow("WF-ORDER", "Dependency order", "Out-of-order tasks");
        workflow.setActive(true);
        workflow.addTask(new Task("T3", "Third", TaskPriority.HIGH, 50, List.of("T2")));
        workflow.addTask(new Task("T2", "Second", TaskPriority.HIGH, 50, List.of("T1")));
        workflow.addTask(new Task("T1", "First", TaskPriority.HIGH, 50));
        DataStore.workflows.put(workflow.getWorkflowId(), workflow);

        WorkflowRun failedRun = new ExecutionServiceImpl(1.0)
                .executeWorkflow(workflow.getWorkflowId(), "operator1");
        check("First task fails and dependent tasks are skipped",
                failedRun.getTaskStatuses().get("T1") == TaskStatus.FAILED
                        && failedRun.getTaskTimesMs().get("T1") == 50
                        && failedRun.getTaskStatuses().get("T2") == TaskStatus.FAILED
                        && failedRun.getTaskTimesMs().get("T2") == 0
                        && failedRun.getTaskStatuses().get("T3") == TaskStatus.FAILED
                        && failedRun.getTaskTimesMs().get("T3") == 0);
        check("Failed dependency chain produces FAILED run", failedRun.getStatus() == RunStatus.FAILED);

        Workflow emptyWorkflow = new Workflow("WF-EMPTY", "Empty", "No tasks");
        emptyWorkflow.setActive(true);
        DataStore.workflows.put(emptyWorkflow.getWorkflowId(), emptyWorkflow);
        try {
            new ExecutionServiceImpl().executeWorkflow(emptyWorkflow.getWorkflowId(), "operator1");
            check("Empty workflow execution is rejected", false);
        } catch (IllegalArgumentException e) {
            check("Empty workflow execution is rejected", true);
        }

        ReportService reports = new ReportServiceImpl();
        check("Reports filter runs by user", reports.getRunsByUser("operator1").size() == 1);
        check("Reports filter runs by status",
                reports.getRunsByStatus(RunStatus.FAILED).size() == 1);
        check("Task failure report counts failed and skipped tasks",
                reports.getTaskFailureCounts().values().stream().mapToLong(Long::longValue).sum() == 3);
        Map<String, Double> averages = reports.getAverageTaskTimes("WF-ORDER");
        check("Average task time excludes tasks that did not execute",
                averages.size() == 1 && averages.get("T1") == 50.0);

        Workflow averageWorkflow = new Workflow("WF-AVERAGES", "Task averages", "Sort average task times");
        averageWorkflow.setActive(true);
        averageWorkflow.addTask(new Task("A-FAST", "Fast", TaskPriority.HIGH, 20));
        averageWorkflow.addTask(new Task("Z-SLOW", "Slow", TaskPriority.HIGH, 100));
        averageWorkflow.addTask(new Task("M-MEDIUM", "Medium", TaskPriority.HIGH, 50));
        DataStore.workflows.put(averageWorkflow.getWorkflowId(), averageWorkflow);
        new ExecutionServiceImpl().executeWorkflow(averageWorkflow.getWorkflowId(), "operator1");
        Map<String, Double> sortedAverages = reports.getAverageTaskTimes(averageWorkflow.getWorkflowId());
        check("Average task times sort from longest to shortest",
                new ArrayList<>(sortedAverages.keySet())
                        .equals(List.of("Z-SLOW", "M-MEDIUM", "A-FAST")));

        check("Top workflow report includes executed workflow",
                reports.getTopWorkflowsByRunCount().get("WF-ORDER | Dependency order") == 1L);
        check("All-runs report returns stored history", reports.getAllRuns().size() == 2);

        System.out.println("Passed: " + passed + "  Failed: " + failed);
        if (failed > 0) {
            throw new AssertionError("Execution behavior tests failed");
        }
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
