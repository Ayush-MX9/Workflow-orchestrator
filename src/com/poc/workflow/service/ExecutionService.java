package com.poc.workflow.service;

import com.poc.workflow.exception.InactiveWorkflowExecutionException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.model.WorkflowRunContext;

public interface ExecutionService {

    WorkflowRun executeWorkflow(
            String workflowId,
            String triggeredBy)
            throws WorkflowNotFoundException,
                   InactiveWorkflowExecutionException;

    WorkflowRunContext executeWorkflowAsync(
            String workflowId,
            String triggeredBy)
            throws WorkflowNotFoundException,
                   InactiveWorkflowExecutionException;

    void shutdown();
}