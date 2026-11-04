package com.poc.workflow;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;
import com.poc.workflow.util.DataStore;
import com.poc.workflow.util.IdGenerator;

// Triggers 6 workflow runs one after another and checks that the final state is consistent.
public class Phase2ExecutionTest {

    private static final int RUN_COUNT = 6;
    private static final long TIMEOUT_MS = 60_000;

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        WorkflowService workflowService = new WorkflowServiceImpl();
        ExecutionService executionService = new ExecutionServiceImpl();

        try {
            // small times so the test stays quick; T3 depends on T1 and T2, T4 on T3
            Workflow wf = workflowService.createWorkflow("Parallel Test", "Phase 2 test");
            String id = wf.getWorkflowId();
            workflowService.addTask(id, new Task("T1", "Extract", TaskPriority.HIGH, 100));
            workflowService.addTask(id, new Task("T2", "Validate", TaskPriority.MEDIUM, 150));
            workflowService.addTask(id, new Task("T3", "Transform", TaskPriority.LOW, 120, List.of("T1", "T2")));
            workflowService.addTask(id, new Task("T4", "Load", TaskPriority.HIGH, 80, List.of("T3")));
            workflowService.setWorkflowActive(id, true);
            int tasksPerRun = wf.getTasks().size();

            // trigger the runs one after another (they execute in parallel on the executor)
            Set<String> triggeredIds = new HashSet<>();
            for (int i = 1; i <= RUN_COUNT; i++) {
                WorkflowRun run = executionService.executeWorkflow(id, "tester" + i);
                triggeredIds.add(run.getRunId());
            }
            check("6 runs triggered with 6 distinct run ids", triggeredIds.size() == RUN_COUNT);

            // wait until every run has been stored in the history (or time out)
            long deadline = System.currentTimeMillis() + TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline
                    && !(DataStore.workflowRuns.size() >= RUN_COUNT && DataStore.activeRuns.isEmpty())) {
                Thread.sleep(100);
            }

            check("All runs finished and were stored in history", DataStore.workflowRuns.size() == RUN_COUNT);
            check("No run is left in the active map", DataStore.activeRuns.isEmpty());

            Set<String> historyIds = new HashSet<>();
            boolean allSettled = true;
            boolean allStatusesMatchRule = true;
            boolean allHaveEndTime = true;

            for (WorkflowRun run : DataStore.workflowRuns) {
                historyIds.add(run.getRunId());
                long completed = run.getCompletedCount();
                long failedTasks = run.getFailedCount();

                if (!run.isAllTasksSettled() || completed + failedTasks != tasksPerRun) {
                    allSettled = false;
                }
                RunStatus expected = (completed == tasksPerRun) ? RunStatus.SUCCESS
                        : (completed == 0) ? RunStatus.FAILED : RunStatus.PARTIAL_SUCCESS;
                if (run.getStatus() != expected) {
                    allStatusesMatchRule = false;
                }
                if (run.getEndTime() == null) {
                    allHaveEndTime = false;
                }
            }
            check("History has no duplicate run ids", historyIds.size() == RUN_COUNT);
            check("Every task of every run is COMPLETED or FAILED", allSettled);
            check("Every run status follows the SUCCESS/PARTIAL/FAILED rule", allStatusesMatchRule);
            check("Every run has an end time", allHaveEndTime);

            // dependency rule: T4 can only be COMPLETED if T3 was
            boolean dependenciesRespected = true;
            for (WorkflowRun run : DataStore.workflowRuns) {
                var statuses = run.getTaskStatuses();
                if (statuses.get("T4").name().equals("COMPLETED")
                        && !statuses.get("T3").name().equals("COMPLETED")) {
                    dependenciesRespected = false;
                }
            }
            check("No task completed before its dependency", dependenciesRespected);

            // counters (only meaningful if the executor calls IdGenerator.recordTask...)
            int started = IdGenerator.getTasksStarted();
            if (started > 0) {
                check("Task counters: started == completed + failed",
                        started == IdGenerator.getTasksCompleted() + IdGenerator.getTasksFailed());
            } else {
                System.out.println("INFO  Task counters not used by the executor yet (started = 0)");
            }

        } finally {
            executionService.shutdown();   // must stop the pool, otherwise the JVM keeps running
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