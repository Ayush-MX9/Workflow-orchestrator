package com.poc.workflow;

import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.service.TaskService;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.servicesImpl.TaskServiceImpl;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;
import java.util.List;

public class ExecutionModuleTest {

    public static void main(String[] args) {

        try {

            WorkflowServiceImpl workflowService =
                    new WorkflowServiceImpl();

            TaskService taskService =
                    new TaskServiceImpl();

            ExecutionService executionService =
                    new ExecutionServiceImpl();

            workflowService.createWorkflow(
                    "WF100",
                    "ETL Pipeline");

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