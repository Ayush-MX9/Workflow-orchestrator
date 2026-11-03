package com.poc.workflow.service;

import java.util.*;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;

public interface WorkflowService {

    void createWorkFlow(Workflow workFlow) throws WorkflowAlreadyExistsException;

    void deleteWorkFlow(String workFlowID) throws WorkflowNotFoundException;

    Workflow getWorkFlowbyID(String workFlowID) throws WorkflowNotFoundException;

    List<Workflow> getAllWorkflows();

    void activateWorkflow(String workFlowID) throws WorkflowNotFoundException;

    void deactivateWorkflow(String workflowId)
            throws WorkflowNotFoundException;

}
