package com.poc.workflow.util;

import java.util.concurrent.atomic.AtomicInteger;

// Thread-safe IDs and counters. AtomicInteger uses compare-and-set, so no two threads
// get the same number and no lock is needed.
public final class IdGenerator {

    private static final AtomicInteger WORKFLOW_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger RUN_COUNTER = new AtomicInteger(0);

    private static final AtomicInteger TASKS_STARTED = new AtomicInteger(0);
    private static final AtomicInteger TASKS_COMPLETED = new AtomicInteger(0);
    private static final AtomicInteger TASKS_FAILED = new AtomicInteger(0);
    private static final AtomicInteger ACTIVE_TASKS = new AtomicInteger(0);
    private static final AtomicInteger MAX_CONCURRENT_TASKS = new AtomicInteger(0);

    private IdGenerator() {
    }

    public static String nextWorkflowId() {
        return String.format("WF-%03d", WORKFLOW_COUNTER.incrementAndGet());
    }

    public static String nextRunId() {
        return String.format("RUN-%03d", RUN_COUNTER.incrementAndGet());
    }

    // called by TaskExecutor once per task attempt
    public static void recordTaskStarted() {
        TASKS_STARTED.incrementAndGet();
        int active = ACTIVE_TASKS.incrementAndGet();
        MAX_CONCURRENT_TASKS.accumulateAndGet(active, Math::max);
    }

    public static void recordTaskCompleted() {
        TASKS_COMPLETED.incrementAndGet();
        ACTIVE_TASKS.decrementAndGet();
    }

    public static void recordTaskFailed() {
        TASKS_FAILED.incrementAndGet();
        ACTIVE_TASKS.decrementAndGet();
    }

    public static int getTasksStarted()   { return TASKS_STARTED.get(); }
    public static int getTasksCompleted() { return TASKS_COMPLETED.get(); }
    public static int getTasksFailed()    { return TASKS_FAILED.get(); }
    public static int getActiveTasks()    { return ACTIVE_TASKS.get(); }
    public static int getMaxConcurrentTasks() { return MAX_CONCURRENT_TASKS.get(); }
}