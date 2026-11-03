package com.poc.workflow.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.enums.TaskStatus;

// One execution of a workflow. Holds its OWN copy of every task's status and time,
// so the original Task objects are never modified during execution.
public class WorkflowRun {

    private final String runId;
    private final String workflowId;
    private final String workflowName;     // snapshot, stays correct even if the workflow is renamed later
    private final String triggeredBy;      // username of the operator/admin who started the run
    private final LocalDateTime startTime;
    private LocalDateTime endTime;         // null until the run finishes
    private RunStatus status;              // null until finish() is called

    // LinkedHashMap keeps insertion order = the workflow's execution order
    private final Map<String, TaskStatus> taskStatuses = new LinkedHashMap<>();  // taskId -> status
    private final Map<String, Long> taskTimesMs = new LinkedHashMap<>();        // taskId -> simulated time
    private final Map<String, String> taskNames = new LinkedHashMap<>();         // taskId -> name (for display)

    private long totalTimeMs;              

    public WorkflowRun(String runId, Workflow workflow, String triggeredBy) {
        this.runId = runId;
        this.workflowId = workflow.getWorkflowId();
        this.workflowName = workflow.getName();
        this.triggeredBy = triggeredBy;
        this.startTime = LocalDateTime.now();

        // copy every task as PENDING: this is the per-run status copy
        for (Task task : workflow.getTasks()) {
            taskStatuses.put(task.getTaskId(), TaskStatus.PENDING);
            taskTimesMs.put(task.getTaskId(), 0L);
            taskNames.put(task.getTaskId(), task.getTaskName());
        }
    }

    // ---------- updates used by ExecutionService while the run is in progress ----------

    public void markRunning(String taskId) {
        taskStatuses.put(taskId, TaskStatus.RUNNING);
    }

    public void markCompleted(String taskId, long timeTakenMs) {
        taskStatuses.put(taskId, TaskStatus.COMPLETED);
        taskTimesMs.put(taskId, timeTakenMs);
    }

    public void markFailed(String taskId, long timeTakenMs) {
        taskStatuses.put(taskId, TaskStatus.FAILED);
        taskTimesMs.put(taskId, timeTakenMs);
    }


    public void finish() {
        this.endTime = LocalDateTime.now();

        long completed = getCompletedCount();
        if (completed == taskStatuses.size()) {
            this.status = RunStatus.SUCCESS;
        } else if (completed == 0) {
            this.status = RunStatus.FAILED;
        } else {
            this.status = RunStatus.PARTIAL_SUCCESS;
        }

        // total simulated time = sum of the individual task times
        this.totalTimeMs = taskTimesMs.values().stream().mapToLong(Long::longValue).sum();
    }

    // ---------- counts ----------

    public long getCompletedCount() {
        return taskStatuses.values().stream().filter(s -> s == TaskStatus.COMPLETED).count();
    }

    public long getFailedCount() {
        return taskStatuses.values().stream().filter(s -> s == TaskStatus.FAILED).count();
    }

    // ---------- getters ----------

    public String getRunId() { return runId; }

    public String getWorkflowId() { return workflowId; }

    public String getWorkflowName() { return workflowName; }

    public String getTriggeredBy() { return triggeredBy; }

    public LocalDateTime getStartTime() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public RunStatus getStatus() { return status; }

    public long getTotalTimeMs() { return totalTimeMs; }

    public Map<String, TaskStatus> getTaskStatuses() {
        return Collections.unmodifiableMap(taskStatuses);
    }

    public Map<String, Long> getTaskTimesMs() {
        return Collections.unmodifiableMap(taskTimesMs);
    }

    public Map<String, String> getTaskNames() {
        return Collections.unmodifiableMap(taskNames);
    }

    @Override
    public String toString() {
        return runId + " | " + workflowName + " | " + status
                + " | by " + triggeredBy + " | " + totalTimeMs + " ms";
    }
}