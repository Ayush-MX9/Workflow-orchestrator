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





}