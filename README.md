# Workflow Orchestrator & Task Scheduler (POC-6, Phase 1) 

A console-based Java application where **Admins** build workflows made of dependent tasks, and **Operators** run them and review the results. All data is held in memory and resets when the app exits.

## Features

- **Users:** register as ADMIN or OPERATOR, login with username/password, one active session per user.
- **Workflows (Admin):** create, edit, delete, view, activate/deactivate. Names are unique (case-insensitive).
- **Tasks (Admin):** add, edit, remove, view. Each task has an ID, name, priority, base time and dependencies. Duplicate task IDs and circular dependencies are rejected.
- **Execution (Operator):** run an active workflow sequentially, in dependency order, with live console logs.
- **Tracking:** per-task status (PENDING, RUNNING, COMPLETED, FAILED), per-run summary and in-memory run history.
- **Reports:** all runs, filter by status, top 3 workflows by run count, average task time per workflow, task failure counts.

## Requirements

- JDK 17 or newer (`javac -version` to check). No external libraries.

## Setup and run

From the project root:

```bash
# Linux / macOS
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out com.poc.workflow.Main
```

```powershell
# Windows (PowerShell)
mkdir out -Force
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out com.poc.workflow.Main
```

An IDE works too: open the folder, mark `src` as the source root and run `com.poc.workflow.Main`.

## Sample users

| Username | Password | Role |
| admin | admin123 | ADMIN |
| operator1 | op123 | OPERATOR |

You can also register new users from the main menu.

## Menu navigation

```
MAIN MENU       1 Register   2 Login   3 Exit

ADMIN MENU      1 Create Workflow        6 View All Workflows
                2 Edit Workflow          7 View Workflow Runs
                3 Delete Workflow        8 View Analytics
                4 Activate/Deactivate    9 View Registered Users
                5 Manage Tasks           10 Logout

TASK MENU       1 Add   2 Edit   3 Remove   4 View   5 Back

OPERATOR MENU   1 View Active Workflows   4 View My Run Details
                2 Execute Workflow        5 Logout
                3 View My Workflow Runs
```

## Quick walkthrough

1. Login as `admin` / `admin123`.
2. **1** Create a workflow, for example `ETL Pipeline`. It starts **inactive**.
3. **5** Manage Tasks, choose the workflow, then **1** Add Task. Add `T1` (no dependency), then `T2` depending on `T1`. A dependency must already exist when you add a task.
4. **4** Activate the workflow, then **10** Logout.
5. Login as `operator1` / `op123`.
6. **2** Execute Workflow and enter the workflow ID (for example `WF-001`). The log prints `Starting Task`, `Completed Task` and a run summary.
7. **3** / **4** view your runs. Log back in as admin and use **7** / **8** for reports.

Example output:

```
Starting Task T1 - Extract
Completed Task T1 - Extract (1000 ms)
Starting Task T2 - Transform
Completed Task T2 - Transform (1250 ms)
Workflow execution finished with status SUCCESS
```

## Rules worth knowing

- **Estimated task time** = base time + priority factor (HIGH +0 ms, MEDIUM +50 ms, LOW +100 ms). Total workflow time is the sum over its tasks.
- **A task runs only after all its dependencies are COMPLETED.** Otherwise it is marked FAILED.
- **Run status:** SUCCESS (all tasks completed), PARTIAL_SUCCESS (some completed), FAILED (none completed).
- **Failures** are simulated through `ExecutionServiceImpl(double failureProbability)`. The menus use the default of `0.0`, so runs succeed unless you change this value.
- Deleting a workflow keeps its past runs in the history.

## Project structure

```
src/com/poc/workflow/
├── Main.java
├── console/       MainMenu, AdminMenu, OperatorMenu, TaskMenu
├── model/         AbstractUser, AdminUser, OperatorUser, Workflow, Task, WorkflowRun
├── enums/         Role, TaskPriority, TaskStatus, RunStatus
├── service/       service interfaces
├── servicesImpl/  service implementations
├── exception/     custom exceptions
└── util/          DataStore, InputUtil, IdGenerator, DependencyUtil, LogUtil, SplashScreen
```

## Custom exceptions

`DuplicateUserException`, `InvalidCredentialsException`, `UserAlreadyLoggedInException`, `UserNotFoundException`, `WorkflowAlreadyExistsException`, `WorkflowNotFoundException`, `TaskAlreadyExistsException`, `TaskNotFoundException`, `CircularDependencyException`, `InactiveWorkflowExecutionException`.

## Tests

Plain Java programs with a `main` method, run after compiling:

```bash
java -cp out com.poc.workflow.WorkflowModuleTest
java -cp out com.poc.workflow.ExecutionModuleTest
```

`UserModuleTest` reads from the keyboard and must be run interactively.



## AI assistance

Parts of this project (the reporting service, dependency ordering, execution service tweaks, and portions of the Admin and Operator menus) were produced with an AI coding assistant It was previously done but due to time constraints we run agents for failing test cases, then reviewed and tested by the team.

## Team

- Pratik Sah -> Role , Services Design , Custom Exceptions , Unit Testing ,Ovearll Testing.
- Ayushman Pati -> Model Design , Utils , Console , Main Menu , Service Changes.
