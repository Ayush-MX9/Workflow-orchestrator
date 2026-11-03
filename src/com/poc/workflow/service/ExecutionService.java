package com.poc.workflow.service;

import com.poc.workflow.exception.InactiveWorkflowExecutionException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.WorkflowRun;

public interface ExecutionService {

    WorkflowRun executeWorkflow(
            String workflowId,
            String triggeredBy)
            throws WorkflowNotFoundException,
                   InactiveWorkflowExecutionException;
}