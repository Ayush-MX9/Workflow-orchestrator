package com.poc.workflow.enums;

// Priority of a task. Each level adds a fixed delay (in ms) to its execution time:
// estimated time = baseExecutionTimeMs + delayMs
public enum TaskPriority {

    HIGH(0),      // most urgent, no extra delay
    MEDIUM(50),
    LOW(100);     // least urgent, longest delay

    // final so the delay can never change after the constant is created
    private final long delayMs;

    // enum constructors are always private; each constant above calls this
    TaskPriority(long delayMs) {
        this.delayMs = delayMs;
    }

    // getter used by Task.getEstimatedTimeMs() and the execution logic
    public long getDelayMs() {
        return delayMs;
    }
}