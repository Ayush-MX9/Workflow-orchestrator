package com.poc.workflow.servicesImpl;

import java.util.List;

import com.poc.workflow.exception.CircularDependencyException;
import com.poc.workflow.exception.TaskAlreadyExistsException;
import com.poc.workflow.exception.TaskNotFoundException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.service.TaskService;
import com.poc.workflow.util.DataStore;
import java.util.HashSet;
import java.util.Set;


public class TaskServiceImpl implements TaskService {


        



    @Override
public void addTask(
        String workflowId,
        Task task)
        throws WorkflowNotFoundException,
               TaskAlreadyExistsException,
               CircularDependencyException {

    Workflow workflow =
            DataStore.workflows.get(workflowId);

    if (workflow == null) {

        throw new WorkflowNotFoundException(
                "Workflow not found");
    }

    if (workflow.hasTask(
            task.getTaskId())) {

        throw new TaskAlreadyExistsException(
                "Task already exists");
    }

    workflow.addTask(task);

if (hasCycle(workflow)) {

    workflow.removeTask(
            task.getTaskId());

    throw new CircularDependencyException(
            "Circular dependency detected");
}
}


@Override
public void deleteTask(
        String workflowId,
        String taskId)
        throws WorkflowNotFoundException,
               TaskNotFoundException {

    Workflow workflow =
            DataStore.workflows.get(workflowId);

    if (workflow == null) {

        throw new WorkflowNotFoundException(
                "Workflow not found");
    }

    if (!workflow.removeTask(taskId)) {

        throw new TaskNotFoundException(
                "Task not found");
    }
}





@Override
public Task getTaskById(
        String workflowId,
        String taskId)
        throws WorkflowNotFoundException,
               TaskNotFoundException {

    Workflow workflow =
            DataStore.workflows.get(workflowId);

    if (workflow == null) {

        throw new WorkflowNotFoundException(
                "Workflow not found");
    }

    Task task =
            workflow.findTaskById(taskId);

    if (task == null) {

        throw new TaskNotFoundException(
                "Task not found");
    }

    return task;
}




@Override
public List<Task> getTasksByWorkflow(
        String workflowId)
        throws WorkflowNotFoundException {

    Workflow workflow =
            DataStore.workflows.get(workflowId);

    if (workflow == null) {

        throw new WorkflowNotFoundException(
                "Workflow not found");
    }

    return workflow.getTasks();
}


private boolean hasCycle(
        Workflow workflow) {

    Set<String> visited =
            new HashSet<>();

    Set<String> recursionStack =
            new HashSet<>();

    for (Task task : workflow.getTasks()) {

        if (dfs(task.getTaskId(),
                workflow,
                visited,
                recursionStack)) {

            return true;
        }
    }

    return false;
}

private boolean dfs(
        String taskId,
        Workflow workflow,
        Set<String> visited,
        Set<String> recursionStack) {

    if (recursionStack.contains(taskId)) {
        return true;
    }

    if (visited.contains(taskId)) {
        return false;
    }

    visited.add(taskId);

    recursionStack.add(taskId);

    Task task =
            workflow.findTaskById(taskId);

    if (task != null) {

        for (String dependency :
                task.getDependsOnTaskIds()) {

            if (dfs(
                    dependency,
                    workflow,
                    visited,
                    recursionStack)) {

                return true;
            }
        }
    }

    recursionStack.remove(taskId);

    return false;
}







}