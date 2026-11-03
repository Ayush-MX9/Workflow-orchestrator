package com.poc.workflow.servicesImpl;

import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.util.DataStore;
import java.util.ArrayList;
import java.util.List;

public class WorkFlowImpl implements WorkflowService  {

    @Override 
public  void createWorkFlow(Workflow workFlow) throws WorkflowAlreadyExistsException{
    
// steps to check , 1. check similar exist or not 
    for(Workflow existing : DataStore.workflows.values()){

        if (existing.getName().equalsIgnoreCase(workFlow.getName()) || existing.getWorkflowId().equalsIgnoreCase(workFlow.getWorkflowId())){
        throw new WorkflowAlreadyExistsException("WorkFlow Name or ID is Already Existing");

        }


    }

    // if not stored in the hash maps

    DataStore.workflows.put(workFlow.getWorkflowId(), workFlow);


}


@Override 

public void deleteWorkFlow(String workFlowID)throws WorkflowNotFoundException

{

    Workflow workflow = DataStore.workflows.get(workFlowID);
    if (workflow==null){

        throw new WorkflowNotFoundException ("No workflow with this id found");


    }

    DataStore.workflows.remove(workFlowID);




}


@Override
public Workflow  getWorkFlowbyID(
        String workflowId)
        throws WorkflowNotFoundException {

    Workflow workflow =
            DataStore.workflows.get(
                    workflowId);

    if (workflow == null) {

        throw new WorkflowNotFoundException(
                "Workflow not found");
    }

    return workflow;
}


@Override
public List<Workflow> getAllWorkflows() {

    return new ArrayList<>(
            DataStore.workflows.values());
}



@Override
public void activateWorkflow(
        String workflowId)
        throws WorkflowNotFoundException {

    Workflow workflow =
             getWorkFlowbyID(workflowId);

    workflow.setActive(true);
}



@Override
public void deactivateWorkflow(
        String workflowId)
        throws WorkflowNotFoundException {

    Workflow workflow =
             getWorkFlowbyID(workflowId);

    workflow.setActive(false);
}






    
    
}
