package com.poc.workflow.util;

import java.util.concurrent.atomic.AtomicInteger;

// Generates unique, readable IDs. AtomicInteger is used from the start because
// Phase 2 needs thread-safe run IDs; it costs nothing in Phase 1.
public final class IdGenerator {

    private static final AtomicInteger WORKFLOW_COUNTER = new AtomicInteger(0);
    private static final AtomicInteger RUN_COUNTER = new AtomicInteger(0);

    private IdGenerator() {
    }

    // WF-001, WF-002, ...
    public static String nextWorkflowId() {
        return String.format("WF-%03d", WORKFLOW_COUNTER.incrementAndGet());
    }

    // RUN-001, RUN-002, ...
    public static String nextRunId() {
        return String.format("RUN-%03d", RUN_COUNTER.incrementAndGet());
    }
}