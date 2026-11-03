import java.util.Map;
import java.util.stream.Collectors;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.AnalyticsService;
import com.poc.workflow.util.DataStore;

public class AnalyticsServiceImpl implements  AnalyticsService{

    public AnalyticsServiceImpl() {
    }


public void showRunHistory() {

    System.out.println(
            "\n===== RUN HISTORY =====");

    for (WorkflowRun run :
            DataStore.workflowRuns) {

        System.out.println(run);
    }
}

public void showExecutionSummary() {

    int totalRuns =
            DataStore.workflowRuns.size();

    long successRuns =
            DataStore.workflowRuns
                    .stream()
                    .filter(run ->
                            run.getStatus()
                                    == RunStatus.SUCCESS)
                    .count();

    long failedRuns =
            DataStore.workflowRuns
                    .stream()
                    .filter(run ->
                            run.getStatus()
                                    == RunStatus.FAILED)
                    .count();

    System.out.println(
            "\n===== EXECUTION SUMMARY =====");

    System.out.println(
            "Total Runs : "
                    + totalRuns);

    System.out.println(
            "Success Runs : "
                    + successRuns);

    System.out.println(
            "Failed Runs : "
                    + failedRuns);
}


public void showTopWorkflows() {

    System.out.println(
            "\n===== TOP WORKFLOWS =====");

    Map<String, Long> workflowCounts =
            DataStore.workflowRuns
                    .stream()
                    .collect(
                            Collectors.groupingBy(
                                    WorkflowRun::getWorkflowName,
                                    Collectors.counting()));

    workflowCounts.entrySet()
            .stream()
            .sorted(
                    Map.Entry.<String, Long>
                            comparingByValue()
                            .reversed())
            .limit(3)
            .forEach(System.out::println);
}
}