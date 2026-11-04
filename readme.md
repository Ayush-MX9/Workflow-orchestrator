!!initialised 
# Workflow Orchestrator & Task Scheduler

A Java console application for creating workflows, organizing tasks by dependencies, running workflows, and viewing run history and analytics.

## Requirements

| Requirement | Details |
|---|---|
| Java Development Kit | JDK 17 or newer (`java` and `javac` on `PATH`) |
| Build tool | None; compile directly with `javac` |
| Interface | Interactive terminal |

## Compile and run

Run these commands from the repository root.

| Step | Windows PowerShell |
|---|---|
| Compile all Java source files | `$sources = Get-ChildItem .\src -Recurse -Filter *.java \| ForEach-Object { $_.FullName }`<br>`New-Item -ItemType Directory -Force .\out \| Out-Null`<br>`javac -d .\out $sources` |
| Start the application | `java -cp .\out com.poc.workflow.Main` |

On macOS or Linux, run:

```sh
mkdir -p out
find src -name '*.java' -print0 | xargs -0 javac -d out
java -cp out com.poc.workflow.Main
```

Compile the source before each run. Running `java com.poc.workflow.Main` from `src` may use old `.class` files instead of the latest source.

## Quick workflow guide

| Step | Role and menu | Action |
|---|---|---|
| 1 | Either | Register an account, or log in with a sample account listed below. |
| 2 | Admin | Choose **Create Workflow**. New workflows start inactive. |
| 3 | Admin | Choose **Manage Tasks in Workflow**, select the workflow ID, then add tasks. Give each task a short unique ID (for example, `T1`) and a separate descriptive name. |
| 4 | Admin | Activate the workflow using **Activate / Deactivate Workflow**. Operators can only see and run active workflows. |
| 5 | Operator | Choose **View Active Workflows** to see workflow tasks, or **Execute Workflow** to run one. |
| 6 | Either | Review run history/details; admins can also view workflow analytics. Choose **Logout** to return to the main menu, then **Exit** to close the app. |

### Task inputs and execution

| Field or behavior | Details |
|---|---|
| Task ID | Short identifier unique within its workflow, such as `T1`; use this ID when editing, removing, or declaring dependencies. |
| Task name | Human-readable description, such as `Approve payroll`. |
| Priority | `HIGH` adds 0 ms, `MEDIUM` adds 50 ms, and `LOW` adds 100 ms to the base execution time. |
| Dependencies | Optional comma-separated IDs of tasks already in the same workflow, such as `T1, T2`. Tasks execute after their dependencies. |
| Execution | Simulated: the app reports task status and estimated time; it does not perform the real-world work described by a task. |
| Empty/inactive workflow | A workflow must contain tasks and be active before it can be executed. |

## Sample accounts

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Operator | `operator1` | `op123` |

Usernames and passwords are case-sensitive. You can also register accounts from the main menu.

## Data lifetime

Users registered during a run, workflows, tasks, and run history are held in memory. They are cleared when the application exits; the two sample accounts are loaded again on the next start.

## Project layout

| Path | Purpose |
|---|---|
| `src/com/poc/workflow/Main.java` | Application entry point |
| `src/com/poc/workflow/console/` | Main, admin, operator, and task menus |
| `src/com/poc/workflow/model/` | Users, workflows, tasks, and workflow runs |
| `src/com/poc/workflow/service/` | Service interfaces |
| `src/com/poc/workflow/servicesImpl/` | Workflow, task, execution, user, and reporting logic |
| `src/com/poc/workflow/util/` | In-memory data store, dependency ordering, input, and logging utilities |

## Running the included test programs

Compile the project first using the commands above, then run these from the repository root:

| Test program | Command |
|---|---|
| Workflow service | `java -cp .\out com.poc.workflow.WorkflowModuleTest` |
| Task service | `java -cp .\out com.poc.workflow.TaskModuleTest` |
| Workflow execution | `java -cp .\out com.poc.workflow.ExecutionModuleTest` |
| Execution behavior and reports | `java -cp .\out com.poc.workflow.ExecutionBehaviorTest` |

These are standalone test programs, not a Maven/Gradle test suite. Check their output for `FAIL` lines and the final pass/fail count where provided.