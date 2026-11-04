package com.poc.workflow;

import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.service.TaskService;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.servicesImpl.TaskServiceImpl;
import com.poc.workflow.servicesImpl.WorkflowImpl;
import java.util.List;

public class ExecutionModuleTest {

    public static void main(String[] args) {

        try {

            WorkflowService workflowService =
                    new WorkFlowImpl();

            TaskService taskService =
                    new TaskServiceImpl();

            ExecutionService executionService =
                    new ExecutionServiceImpl();

            Workflow workflow =
                    new Workflow(
                            "WF100",
                            "ETL Pipeline",
                            "Execution Testing");

            workflowService.createWorkFlow(
                    workflow);

            taskService.addTask(
                    "WF100",
                    new Task(
                            "T1",
                            "Extract",
                            TaskPriority.HIGH,
                            1000));

            taskService.addTask(
                    "WF100",
                    new Task(
                            "T2",
                            "Transform",
                            TaskPriority.MEDIUM,
                            1200,
                            List.of("T1")));

            taskService.addTask(
                    "WF100",
                    new Task(
                            "T3",
                            "Load",
                            TaskPriority.LOW,
                            800,
                            List.of("T2")));

            workflowService.activateWorkflow(
                    "WF100");

            WorkflowRun run =
                    executionService.executeWorkflow(
                            "WF100",
                            "admin");

            System.out.println(
                    "\n===== RUN SUMMARY =====");

            System.out.println(run);

            System.out.println(
                    "Completed Tasks : "
                            + run.getCompletedCount());

            System.out.println(
                    "Failed Tasks : "
                            + run.getFailedCount());

            System.out.println(
                    "Total Time : "
                            + run.getTotalTimeMs());

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }
    }
}