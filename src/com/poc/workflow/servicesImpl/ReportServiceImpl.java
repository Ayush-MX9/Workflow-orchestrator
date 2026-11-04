package com.poc.workflow.servicesImpl;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.enums.TaskStatus;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ReportService;
import com.poc.workflow.util.DataStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportServiceImpl implements ReportService {

    @Override
    public List<WorkflowRun> getAllRuns() {
        return new ArrayList<>(DataStore.workflowRuns);
    }

    @Override
    public List<WorkflowRun> getRunsByUser(String username) {
        return DataStore.workflowRuns.stream()
                .filter(run -> run.getTriggeredBy().equals(username))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkflowRun> getRunsByStatus(RunStatus status) {
        return DataStore.workflowRuns.stream()
                .filter(run -> run.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getTopWorkflowsByRunCount() {
        Map<String, Long> countsById = DataStore.workflowRuns.stream()
                .collect(Collectors.groupingBy(
                        WorkflowRun::getWorkflowId,
                        Collectors.counting()));
        Map<String, String> workflowNames = new LinkedHashMap<>();
        for (WorkflowRun run : DataStore.workflowRuns) {
            workflowNames.put(run.getWorkflowId(), run.getWorkflowName());
        }
        Map<String, Long> topWorkflows = new LinkedHashMap<>();
        countsById.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(3)
                .forEach(entry -> topWorkflows.put(
                        entry.getKey() + " | " + workflowNames.get(entry.getKey()), entry.getValue()));
        return topWorkflows;
    }

    @Override
    public Map<String, Double> getAverageTaskTimes(String workflowId) {
        Map<String, long[]> totals = new TreeMap<>();
        for (WorkflowRun run : DataStore.workflowRuns) {
            if (!run.getWorkflowId().equals(workflowId)) {
                continue;
            }
            for (Map.Entry<String, TaskStatus> task : run.getTaskStatuses().entrySet()) {
                long taskTimeMs = run.getTaskTimesMs().getOrDefault(task.getKey(), 0L);
                if ((task.getValue() == TaskStatus.COMPLETED || task.getValue() == TaskStatus.FAILED)
                        && taskTimeMs > 0) {
                    long[] aggregate = totals.computeIfAbsent(task.getKey(), ignored -> new long[2]);
                    aggregate[0] += taskTimeMs;
                    aggregate[1]++;
                }
            }
        }

        Map<String, Double> averages = new LinkedHashMap<>();
        totals.forEach((taskId, aggregate) ->
                averages.put(taskId, (double) aggregate[0] / aggregate[1]));
        return averages.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first,
                        LinkedHashMap::new));
    }

    @Override
    public Map<String, Long> getTaskFailureCounts() {
        Map<String, Long> counts = new TreeMap<>();
        for (WorkflowRun run : DataStore.workflowRuns) {
            for (Map.Entry<String, TaskStatus> task : run.getTaskStatuses().entrySet()) {
                if (task.getValue() == TaskStatus.FAILED) {
                    String taskKey = run.getWorkflowId() + " | " + task.getKey();
                    counts.merge(taskKey, 1L, Long::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first,
                        LinkedHashMap::new));
    }
}
