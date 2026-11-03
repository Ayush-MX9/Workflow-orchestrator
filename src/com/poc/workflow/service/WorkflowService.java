package com.poc.workflow.service;

import java.util.List;

import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.exception.CircularDependencyException;
import com.poc.workflow.exception.TaskAlreadyExistsException;
import com.poc.workflow.exception.TaskNotFoundException;
import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;

public interface WorkflowService {

    // ---------- workflows ----------

    // name must be unique (case-insensitive); the service generates the workflowId
    Workflow createWorkflow(String name, String description)
            throws WorkflowAlreadyExistsException;

    Workflow getWorkflowById(String workflowId)
            throws WorkflowNotFoundException;

    Workflow getWorkflowByName(String name)
            throws WorkflowNotFoundException;

    // a null/blank newName or newDescription means "keep the current value"
    void updateWorkflow(String workflowId, String newName, String newDescription)
            throws WorkflowNotFoundException, WorkflowAlreadyExistsException;

    void deleteWorkflow(String workflowId)
            throws WorkflowNotFoundException;

    void setWorkflowActive(String workflowId, boolean active)
            throws WorkflowNotFoundException;

    List<Workflow> getAllWorkflows();

    List<Workflow> getActiveWorkflows();

    // ---------- tasks inside a workflow ----------

    void addTask(String workflowId, Task task)
            throws WorkflowNotFoundException, TaskAlreadyExistsException, CircularDependencyException;

    void updateTask(String workflowId, String taskId, String newName,
                    TaskPriority newPriority,
                    Long newBaseTimeMs, List<String> newDependsOn)
            throws WorkflowNotFoundException, TaskNotFoundException, CircularDependencyException;

    void removeTask(String workflowId, String taskId)
            throws WorkflowNotFoundException, TaskNotFoundException;
}