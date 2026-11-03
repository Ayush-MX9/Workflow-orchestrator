package com.poc.workflow.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.poc.workflow.enums.TaskPriority;


public class Task {

    private final String taskId;              // unique within a workflow (checked by the service)
    private String taskName;
    private TaskPriority priority;
    private long baseExecutionTimeMs;         // time the task takes without any priority delay
    private final List<String> dependsOnTaskIds;  // ids of tasks that must finish before this one

    public Task(String taskId, String taskName, TaskPriority priority,
                long baseExecutionTimeMs, List<String> dependsOnTaskIds) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.priority = priority;
        this.baseExecutionTimeMs = baseExecutionTimeMs;
        this.dependsOnTaskIds = (dependsOnTaskIds == null)
                ? new ArrayList<>()
                : new ArrayList<>(dependsOnTaskIds);
    }

    // Convenience constructor for a task with no dependencies
    public Task(String taskId, String taskName, TaskPriority priority, long baseExecutionTimeMs) {
        this(taskId, taskName, priority, baseExecutionTimeMs, null);
    }

    //  getters 

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public long getBaseExecutionTimeMs() {
        return baseExecutionTimeMs;
    }

    public List<String> getDependsOnTaskIds() {
        return Collections.unmodifiableList(dependsOnTaskIds);
    }

    
    public long getEstimatedTimeMs() {
        return baseExecutionTimeMs + priority.getDelayMs();
    }

    //setters (used when an Admin edits a task; taskId stays fixed)

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public void setBaseExecutionTimeMs(long baseExecutionTimeMs) {
        this.baseExecutionTimeMs = baseExecutionTimeMs;
    }

    public void setDependsOnTaskIds(List<String> newDependencies) {
        this.dependsOnTaskIds.clear();
        if (newDependencies != null) {
            this.dependsOnTaskIds.addAll(newDependencies);
        }
    }

    @Override
    public String toString() {
        return taskId + " | " + taskName + " | " + priority
                + " | " + baseExecutionTimeMs + " ms | depends on " + dependsOnTaskIds;
    }
}