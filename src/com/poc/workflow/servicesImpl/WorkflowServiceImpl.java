package com.poc.workflow.servicesImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.exception.CircularDependencyException;
import com.poc.workflow.exception.TaskAlreadyExistsException;
import com.poc.workflow.exception.TaskNotFoundException;
import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.util.DataStore;
import com.poc.workflow.util.IdGenerator;

// Business rules for workflows and tasks. Throws exceptions only; never prints.
public class WorkflowServiceImpl implements WorkflowService {
    @Override
    public Workflow createWorkflow(String name, String description)
            throws WorkflowAlreadyExistsException {
        ensureNameIsFree(name, null);
        Workflow workflow = new Workflow(IdGenerator.nextWorkflowId(), name, description);
        DataStore.workflows.put(workflow.getWorkflowId(), workflow);
        return workflow;
    }

    @Override
    public Workflow getWorkflowById(String workflowId) throws WorkflowNotFoundException {
        Workflow workflow = DataStore.workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException("Workflow not found with id: " + workflowId);
        }
        return workflow;
    }

    @Override
    public Workflow getWorkflowByName(String name) throws WorkflowNotFoundException {
        for (Workflow workflow : DataStore.workflows.values()) {
            if (workflow.getName().equalsIgnoreCase(name)) {
                return workflow;
            }
        }
        throw new WorkflowNotFoundException("Workflow not found with name: " + name);
    }

    @Override
    public void updateWorkflow(String workflowId, String newName, String newDescription)
            throws WorkflowNotFoundException, WorkflowAlreadyExistsException {
        Workflow workflow = getWorkflowById(workflowId);

        // blank/null means "keep the current value"
        if (newName != null && !newName.isBlank()) {
            ensureNameIsFree(newName, workflowId);   // ignore the workflow's own current name
            workflow.setName(newName);
        }
        if (newDescription != null && !newDescription.isBlank()) {
            workflow.setDescription(newDescription);
        }
    }

    @Override
    public void deleteWorkflow(String workflowId) throws WorkflowNotFoundException {
        getWorkflowById(workflowId);              // throws if missing
        DataStore.workflows.remove(workflowId);   // old runs keep their own copies, so history stays valid
    }

    @Override
    public void setWorkflowActive(String workflowId, boolean active)
            throws WorkflowNotFoundException {
        getWorkflowById(workflowId).setActive(active);
    }

    @Override
    public List<Workflow> getAllWorkflows() {
        return new ArrayList<>(DataStore.workflows.values());
    }

    @Override
    public List<Workflow> getActiveWorkflows() {
        List<Workflow> active = new ArrayList<>();
        for (Workflow workflow : DataStore.workflows.values()) {
            if (workflow.isActive()) {
                active.add(workflow);
            }
        }
        return active;
    }

    // ---------- tasks ----------

    @Override
    public void addTask(String workflowId, Task task)
            throws WorkflowNotFoundException, TaskAlreadyExistsException, CircularDependencyException {
        Workflow workflow = getWorkflowById(workflowId);

        if (workflow.hasTask(task.getTaskId())) {
            throw new TaskAlreadyExistsException("Task already exists in workflow: " + task.getTaskId());
        }

        // check the dependency graph as it WOULD be after adding, before really adding
        Map<String, List<String>> graph = buildGraph(workflow);
        graph.put(task.getTaskId(), new ArrayList<>(task.getDependsOnTaskIds()));
        checkDependencies(graph, task.getTaskId());

        workflow.addTask(task);
    }

    @Override
    public void updateTask(String workflowId, String taskId, String newName,
                           TaskPriority newPriority, Long newBaseTimeMs, List<String> newDependsOn)
            throws WorkflowNotFoundException, TaskNotFoundException, CircularDependencyException {
        Workflow workflow = getWorkflowById(workflowId);
        Task task = workflow.findTaskById(taskId);
        if (task == null) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }

        // validate new dependencies first, so a failed check leaves the task unchanged
        if (newDependsOn != null) {
            Map<String, List<String>> graph = buildGraph(workflow);
            graph.put(taskId, new ArrayList<>(newDependsOn));
            checkDependencies(graph, taskId);
            task.setDependsOnTaskIds(newDependsOn);
        }
        if (newName != null && !newName.isBlank()) {
            task.setTaskName(newName);
        }
        if (newPriority != null) {
            task.setPriority(newPriority);
        }
        if (newBaseTimeMs != null) {
            task.setBaseExecutionTimeMs(newBaseTimeMs);
        }
    }

    @Override
    public void removeTask(String workflowId, String taskId)
            throws WorkflowNotFoundException, TaskNotFoundException {
        Workflow workflow = getWorkflowById(workflowId);
        if (!workflow.hasTask(taskId)) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }

        // refuse to leave other tasks depending on a task that no longer exists
        for (Task other : workflow.getTasks()) {
            if (other.getDependsOnTaskIds().contains(taskId)) {
                throw new IllegalStateException("Cannot remove " + taskId
                        + ": task " + other.getTaskId() + " depends on it");
            }
        }
        workflow.removeTask(taskId);
    }

    // ---------- helpers ----------

    // throws if another workflow already uses this name; excludeId lets a workflow keep its own name
    private void ensureNameIsFree(String name, String excludeId) throws WorkflowAlreadyExistsException {
        for (Workflow existing : DataStore.workflows.values()) {
            boolean sameName = existing.getName().equalsIgnoreCase(name);
            boolean isSelf = existing.getWorkflowId().equals(excludeId);
            if (sameName && !isSelf) {
                throw new WorkflowAlreadyExistsException("Workflow name already exists: " + name);
            }
        }
    }

    // taskId -> its dependsOn list, for every task currently in the workflow
    private Map<String, List<String>> buildGraph(Workflow workflow) {
        Map<String, List<String>> graph = new HashMap<>();
        for (Task task : workflow.getTasks()) {
            graph.put(task.getTaskId(), new ArrayList<>(task.getDependsOnTaskIds()));
        }
        return graph;
    }

    // Rejects dependencies on unknown tasks, then runs cycle detection from the changed task
    private void checkDependencies(Map<String, List<String>> graph, String startTaskId)
            throws CircularDependencyException {
        for (String dep : graph.get(startTaskId)) {
            if (!graph.containsKey(dep)) {
                throw new IllegalArgumentException("Dependency refers to unknown task: " + dep);
            }
        }
        // a cycle can only pass through the changed task, so DFS from it is enough
        detectCycle(graph, startTaskId, new HashSet<>(), new ArrayList<>());
    }

    // DFS: "path" holds the tasks on the current route; meeting one again means a cycle
    private void detectCycle(Map<String, List<String>> graph, String current,
                             Set<String> done, List<String> path)
            throws CircularDependencyException {
        if (path.contains(current)) {
            path.add(current);
            throw new CircularDependencyException("Circular dependency: " + String.join(" -> ", path));
        }
        if (done.contains(current)) {
            return;   // already fully explored, no cycle through it
        }
        path.add(current);
        for (String dep : graph.getOrDefault(current, List.of())) {
            detectCycle(graph, dep, done, path);
        }
        path.remove(path.size() - 1);
        done.add(current);
    }
}