package com.poc.workflow.service;

import java.util.List;

import com.poc.workflow.exception.CircularDependencyException;
import com.poc.workflow.exception.TaskAlreadyExistsException;
import com.poc.workflow.exception.TaskNotFoundException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;

public interface TaskService {

    void addTask(
            String workflowId,
            Task task)
            throws WorkflowNotFoundException,
                   TaskAlreadyExistsException,
                   CircularDependencyException;

    void deleteTask(
            String workflowId,
            String taskId)
            throws WorkflowNotFoundException,
                   TaskNotFoundException;

    Task getTaskById(
            String workflowId,
            String taskId)
            throws WorkflowNotFoundException,
                   TaskNotFoundException;

    List<Task> getTasksByWorkflow(
            String workflowId)
            throws WorkflowNotFoundException;
}