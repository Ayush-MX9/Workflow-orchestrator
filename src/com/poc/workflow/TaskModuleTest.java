package com.poc.workflow;

import com.poc.workflow.enums.TaskPriority;
import com.poc.workflow.model.Task;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.service.TaskService;
import com.poc.workflow.service.WorkflowService;
import com.poc.workflow.servicesImpl.TaskServiceImpl;
import com.poc.workflow.servicesImpl.WorkflowServiceImpl;
import java.util.List;

public class TaskModuleTest {

    public static void main(String[] args) {

        WorkflowService workflowService =
                new WorkflowServiceImpl();

        TaskService taskService =
                new TaskServiceImpl();

        try {

            System.out.println(
                    "===== CREATE WORKFLOW =====");

            Workflow workflow =
                    new Workflow(
                            "WF001",
                            "Daily Report",
                            "Report Generation Workflow");

            workflowService.createWorkflow(
                    workflow.getWorkflowId(),
                    workflow.getName());

            System.out.println(
                    "Workflow Created Successfully");

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== ADD TASK 1 =====");

            Task task1 =
                    new Task(
                            "TASK001",
                            "Extract Data",
                            TaskPriority.HIGH,
                            2000);

            taskService.addTask(
                    "WF001",
                    task1);

            System.out.println(
                    "TASK001 Added Successfully");

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== ADD TASK 2 =====");

            Task task2 =
                    new Task(
                            "TASK002",
                            "Transform Data",
                            TaskPriority.MEDIUM,
                            1500,
                            List.of("TASK001"));

            taskService.addTask(
                    "WF001",
                    task2);

            System.out.println(
                    "TASK002 Added Successfully");

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== ADD TASK 3 =====");

            Task task3 =
                    new Task(
                            "TASK003",
                            "Load Data",
                            TaskPriority.LOW,
                            1000,
                            List.of("TASK002"));

            taskService.addTask(
                    "WF001",
                    task3);

            System.out.println(
                    "TASK003 Added Successfully");

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== DUPLICATE TASK TEST =====");

            Task duplicateTask =
                    new Task(
                            "TASK001",
                            "Duplicate",
                            TaskPriority.HIGH,
                            500);

            taskService.addTask(
                    "WF001",
                    duplicateTask);

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== GET TASK =====");

            Task task =
                    taskService.getTaskById(
                            "WF001",
                            "TASK002");

            System.out.println(task);

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== VIEW ALL TASKS =====");

            for (Task task :
                    taskService.getTasksByWorkflow(
                            "WF001")) {

                System.out.println(task);
            }

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== DELETE TASK 3 =====");

            taskService.deleteTask(
                    "WF001",
                    "TASK003");

            System.out.println(
                    "TASK003 Deleted Successfully");

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }

        try {

            System.out.println(
                    "\n===== FETCH DELETED TASK =====");

            taskService.getTaskById(
                    "WF001",
                    "TASK003");

        } catch (Exception e) {

            System.out.println(
                    e.getMessage());
        }



        try {

    System.out.println(
            "\n===== CIRCULAR DEPENDENCY TEST =====");

    Workflow cyclicWorkflow =
            new Workflow(
                    "WF002",
                    "Cycle Workflow",
                    "DFS Test");

        workflowService.createWorkflow(
                cyclicWorkflow.getWorkflowId(),
                cyclicWorkflow.getName());

    Task taskA =
            new Task(
                    "A",
                    "Task A",
                    TaskPriority.HIGH,
                    1000,
                    List.of("C"));

    Task taskB =
            new Task(
                    "B",
                    "Task B",
                    TaskPriority.MEDIUM,
                    1000,
                    List.of("A"));

    Task taskC =
            new Task(
                    "C",
                    "Task C",
                    TaskPriority.LOW,
                    1000,
                    List.of("B"));

    taskService.addTask("WF002", taskA);

    taskService.addTask("WF002", taskB);

    taskService.addTask("WF002", taskC);

}
catch (Exception e) {

    System.out.println(
            e.getMessage());
}
    }
}