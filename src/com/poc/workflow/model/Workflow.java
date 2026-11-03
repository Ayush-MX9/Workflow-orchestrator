package com.poc.workflow.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// A named collection of tasks that run in order.
// Validation 
public class Workflow {

    private final String workflowId;   // never changes after creation
    private String name;               // must be unique across workflows (checked by the service)
    private String description;
    private boolean active;            // only active workflows can be executed
    private final List<Task> tasks;    // kept in execution order

    public Workflow(String workflowId, String name, String description) {
        this.workflowId = workflowId;
        this.name = name;
        this.description = description;
        this.active = false;           // new workflows start inactive; Admin activates them
        this.tasks = new ArrayList<>();
    }

    // ---------- getters ----------

    public String getWorkflowId() {
        return workflowId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    // read-only view: tasks can only be added or removed through the methods below
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    // ---------- setters (used by Admin "Edit Workflow" and "Activate / Deactivate") ----------

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // ---------- task helpers ----------

    public void addTask(Task task) {
        tasks.add(task);
    }

    // returns true if a task was removed
    public boolean removeTask(String taskId) {
        return tasks.removeIf(task -> task.getTaskId().equals(taskId));
    }

    public Task findTaskById(String taskId) {
        for (Task task : tasks) {
            if (task.getTaskId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    public boolean hasTask(String taskId) {
        return findTaskById(taskId) != null;
    }

    public long getTotalEstimatedTimeMs() {
        long total = 0;
        for (Task task : tasks) {
            total += task.getEstimatedTimeMs();
        }
        return total;
    }

    @Override
    public String toString() {
        return workflowId + " | " + name + " | " + (active ? "ACTIVE" : "INACTIVE")
                + " | " + tasks.size() + " task(s)";
    }
}