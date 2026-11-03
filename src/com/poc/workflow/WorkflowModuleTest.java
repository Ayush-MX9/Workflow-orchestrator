package com.poc.workflow;

import com.poc.workflow.exception.WorkflowAlreadyExistsException;
import com.poc.workflow.exception.WorkflowNotFoundException;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.WorkFlowImpl;

public class WorkflowModuleTest {

    public static void main(String[] args) {

        WorkflowService workflowService =
                new WorkFlowImpl();

        try {

            System.out.println(
                    "===== CREATE WORKFLOW TEST =====");

            Workflow workflow1 =
                    new Workflow(
                            "WF001",
                            "Daily Report",
                            "Generates Daily Report");

            workflowService.createWorkFlow(
                    workflow1);

            System.out.println(
                    "Workflow Created Successfully");

            System.out.println(
                    workflow1);



            System.out.println(
                    "\n===== DUPLICATE NAME TEST =====");

            Workflow workflow2 =
                    new Workflow(
                            "WF002",
                            "Daily Report",
                            "Another Workflow");

            workflowService.createWorkFlow(
                    workflow2);

        }

        catch (WorkflowAlreadyExistsException e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== GET WORKFLOW TEST =====");

            Workflow workflow =
                    workflowService
                            . getWorkFlowbyID(
                                    "WF001");

            System.out.println(
                    workflow);

        }

        catch (WorkflowNotFoundException e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== DEACTIVATE TEST =====");

            workflowService
                    .deactivateWorkflow(
                            "WF001");

            Workflow workflow =
                    workflowService
                            . getWorkFlowbyID(
                                    "WF001");

            System.out.println(
                    "Workflow Active Status : "
                            + workflow.isActive());

        }

        catch (WorkflowNotFoundException e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== ACTIVATE TEST =====");

            workflowService
                    .activateWorkflow(
                            "WF001");

            Workflow workflow =
                    workflowService
                            . getWorkFlowbyID(
                                    "WF001");

            System.out.println(
                    "Workflow Active Status : "
                            + workflow.isActive());

        }

        catch (WorkflowNotFoundException e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== DELETE TEST =====");

            workflowService
                    .deleteWorkFlow(
                            "WF001");

            System.out.println(
                    "Workflow Deleted Successfully");

        }

        catch (WorkflowNotFoundException e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== GET DELETED WORKFLOW TEST =====");

            workflowService
                    . getWorkFlowbyID(
                            "WF001");

        }

        catch (WorkflowNotFoundException e) {

            System.out.println(
                    e.getMessage());
        }

    }
}