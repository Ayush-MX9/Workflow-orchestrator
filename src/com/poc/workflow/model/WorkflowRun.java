package com.poc.workflow.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.poc.workflow.enums.RunStatus;
import com.poc.workflow.enums.TaskStatus;

// One execution of a workflow. Holds its OWN copy of every task's status and time.
// Thread-safe: every read and write of mutable state goes through one ReentrantLock,
// and getters return snapshots, so callers never see a map being modified.
public class WorkflowRun {

    private final String runId;
    private final String workflowId;
    private final String workflowName;
    private final String triggeredBy;
    private final LocalDateTime startTime;

    // ---- mutable state: only touched while holding the lock ----
    private final ReentrantLock lock = new ReentrantLock();
    private LocalDateTime endTime;
    private RunStatus status;                 // null until finish() is called
    private long totalTimeMs;
    private final Map<String, TaskStatus> taskStatuses = new LinkedHashMap<>();
    private final Map<String, Long> taskTimesMs = new LinkedHashMap<>();
    private final Map<String, String> taskNames = new LinkedHashMap<>();
    private final Map<String, Integer> retryCounts = new LinkedHashMap<>();
    private final Map<String, String> taskWorkerThreads = new LinkedHashMap<>();

    public WorkflowRun(String runId, Workflow workflow, String triggeredBy) {
        this.runId = runId;
        this.workflowId = workflow.getWorkflowId();
        this.workflowName = workflow.getName();
        this.triggeredBy = triggeredBy;
        this.startTime = LocalDateTime.now();

        for (Task task : workflow.getTasks()) {
            taskStatuses.put(task.getTaskId(), TaskStatus.PENDING);
            taskTimesMs.put(task.getTaskId(), 0L);
            taskNames.put(task.getTaskId(), task.getTaskName());
            retryCounts.put(task.getTaskId(), 0);
        }
    }

    // ---------- updates (called by worker threads) ----------

    // Atomic check-and-set. Returns true only for the ONE thread that wins the task.
    // PENDING -> RUNNING for a first run, FAILED -> RUNNING for a retry.
    public boolean tryMarkRunning(String taskId) {
        lock.lock();
        try {
            TaskStatus current = taskStatuses.get(taskId);
            if (current == TaskStatus.PENDING || current == TaskStatus.FAILED) {
                taskStatuses.put(taskId, TaskStatus.RUNNING);
                return true;
            }
            return false;   // already running or completed: another thread owns it
        } finally {
            lock.unlock();
        }
    }

    // kept so existing code compiles; prefer tryMarkRunning in threaded code
    public void markRunning(String taskId) {
        lock.lock();
        try {
            taskStatuses.put(taskId, TaskStatus.RUNNING);
        } finally {
            lock.unlock();
        }
    }

    public void markCompleted(String taskId, long timeTakenMs) {
        lock.lock();
        try {
            taskStatuses.put(taskId, TaskStatus.COMPLETED);
            taskTimesMs.put(taskId, timeTakenMs);
        } finally {
            lock.unlock();
        }
    }

    public void recordTaskWorker(String taskId, String workerThread) {
        lock.lock();
        try {
            taskWorkerThreads.put(taskId, workerThread);
        } finally {
            lock.unlock();
        }
    }

    public void markFailed(String taskId, long timeTakenMs) {
        lock.lock();
        try {
            taskStatuses.put(taskId, TaskStatus.FAILED);
            taskTimesMs.put(taskId, timeTakenMs);
        } finally {
            lock.unlock();
        }
    }

    // used by the retry handler; returns the new attempt number
    public int incrementRetryCount(String taskId) {
        lock.lock();
        try {
            int next = retryCounts.getOrDefault(taskId, 0) + 1;
            retryCounts.put(taskId, next);
            return next;
        } finally {
            lock.unlock();
        }
    }

    // Rules: all COMPLETED = SUCCESS | none COMPLETED = FAILED | otherwise PARTIAL_SUCCESS.
    // May be called again after retries; it simply recomputes from the current statuses.
    public void finish() {
        lock.lock();
        try {
            long completed = countLocked(TaskStatus.COMPLETED);
            if (completed == taskStatuses.size()) {
                status = RunStatus.SUCCESS;
            } else if (completed == 0) {
                status = RunStatus.FAILED;
            } else {
                status = RunStatus.PARTIAL_SUCCESS;
            }
            endTime = LocalDateTime.now();
            totalTimeMs = taskTimesMs.values().stream().mapToLong(Long::longValue).sum();
        } finally {
            lock.unlock();
        }
    }

    // ---------- reads (snapshots, safe while other threads write) ----------

    public TaskStatus getTaskStatus(String taskId) {
        lock.lock();
        try {
            return taskStatuses.get(taskId);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, TaskStatus> getTaskStatuses() {
        lock.lock();
        try {
            return Collections.unmodifiableMap(new LinkedHashMap<>(taskStatuses));
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Long> getTaskTimesMs() {
        lock.lock();
        try {
            return Collections.unmodifiableMap(new LinkedHashMap<>(taskTimesMs));
        } finally {
            lock.unlock();
        }
    }

    public Map<String, String> getTaskNames() {
        // written only in the constructor and never changed, so no lock is needed
        return Collections.unmodifiableMap(taskNames);
    }

    public Map<String, String> getTaskWorkerThreads() {
        lock.lock();
        try {
            return Collections.unmodifiableMap(new LinkedHashMap<>(taskWorkerThreads));
        } finally {
            lock.unlock();
        }
    }

    public int getRetryCount(String taskId) {
        lock.lock();
        try {
            return retryCounts.getOrDefault(taskId, 0);
        } finally {
            lock.unlock();
        }
    }

    public long getCompletedCount() {
        lock.lock();
        try {
            return countLocked(TaskStatus.COMPLETED);
        } finally {
            lock.unlock();
        }
    }

    public long getFailedCount() {
        lock.lock();
        try {
            return countLocked(TaskStatus.FAILED);
        } finally {
            lock.unlock();
        }
    }

    // true when no task is PENDING or RUNNING any more (retries aside)
    public boolean isAllTasksSettled() {
        lock.lock();
        try {
            return taskStatuses.values().stream()
                    .noneMatch(s -> s == TaskStatus.PENDING || s == TaskStatus.RUNNING);
        } finally {
            lock.unlock();
        }
    }

    public RunStatus getStatus() {
        lock.lock();
        try {
            return status;
        } finally {
            lock.unlock();
        }
    }

    public LocalDateTime getEndTime() {
        lock.lock();
        try {
            return endTime;
        } finally {
            lock.unlock();
        }
    }

    public long getTotalTimeMs() {
        lock.lock();
        try {
            return totalTimeMs;
        } finally {
            lock.unlock();
        }
    }

    // immutable fields: safe to read without the lock
    public String getRunId() { return runId; }
    public String getWorkflowId() { return workflowId; }
    public String getWorkflowName() { return workflowName; }
    public String getTriggeredBy() { return triggeredBy; }
    public LocalDateTime getStartTime() { return startTime; }

    // caller must hold the lock
    private long countLocked(TaskStatus wanted) {
        return taskStatuses.values().stream().filter(s -> s == wanted).count();
    }

    @Override
    public String toString() {
        return runId + " | " + workflowName + " | " + getStatus()
                + " | by " + triggeredBy + " | " + getTotalTimeMs() + " ms";
    }
}