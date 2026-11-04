package com.poc.workflow.servicesImpl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

final class RetryHandler {

    private static final long RETRY_DELAY_MS = 100;

    private final ScheduledExecutorService scheduler;
    private final ExecutorService taskExecutor;

    RetryHandler(ScheduledExecutorService scheduler, ExecutorService taskExecutor) {
        this.scheduler = scheduler;
        this.taskExecutor = taskExecutor;
    }

    void scheduleRetry(Runnable retry) {
        scheduler.schedule(() -> taskExecutor.execute(retry), RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
    }
}
