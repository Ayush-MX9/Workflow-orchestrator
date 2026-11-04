package com.poc.workflow.util;

import java.util.Map;

import com.poc.workflow.enums.TaskStatus;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.model.WorkflowRunContext;   // match Pratik's package

// Background thread that prints the progress of all active runs every few seconds.
// Read-only: uses DataStore.activeRuns and WorkflowRun.getTaskStatuses() (a snapshot copy).
public class ProgressLogger implements Runnable {

    private static final long INTERVAL_MS = 2000;

    // volatile: the write in stop() is immediately visible to the logger thread
    private volatile boolean running = false;
    private Thread thread;

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this, "progress-logger");
        thread.setDaemon(true);   // never keeps the JVM alive
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();   // wakes it from sleep at once
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(INTERVAL_MS);
            } catch (InterruptedException e) {
                return;   // stop() was called
            }
            if (!DataStore.activeRuns.isEmpty()) {   // periodic output stays quiet when idle
                printSnapshot();
            }
        }
    }

    // Also used by the menus ("View live progress"). Reads only; safe from any thread.
    public static void printSnapshot() {
        if (DataStore.activeRuns.isEmpty()) {
            System.out.println("No workflow runs are executing right now.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("PROGRESS: ").append(DataStore.activeRuns.size()).append(" active run(s)");
        for (WorkflowRunContext context : DataStore.activeRuns.values()) {
            sb.append("\n   ").append(describe(context.getRun()));
        }
        LogUtil.info(sb.toString());   // one println, so the block is never interleaved
    }

    private static String describe(WorkflowRun run) {
        Map<String, TaskStatus> statuses = run.getTaskStatuses();   // snapshot copy
        int done = 0, active = 0, failed = 0;
        for (TaskStatus status : statuses.values()) {
            switch (status) {
                case COMPLETED -> done++;
                case RUNNING -> active++;
                case FAILED -> failed++;
                default -> { }
            }
        }
        return run.getRunId() + " | " + run.getWorkflowName() + " | "
                + done + "/" + statuses.size() + " done, " + active + " running, " + failed + " failed";
    }
}