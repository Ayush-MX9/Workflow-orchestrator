package com.poc.workflow.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.enums.TaskStatus;

// Central place for log lines and table formatting.
public final class LogUtil {

    // Phase 2: set to true to add the thread name to every log line
    private static final boolean SHOW_THREAD_NAME = true;

    // DateTimeFormatter is immutable and thread-safe, so one shared instance is fine
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LogUtil() {
    }

    // ---------- execution logs (spec wording) ----------

    public static void taskStarting(String taskId, String taskName) {
        info("Starting Task " + taskId + " (" + taskName + ")");
    }

    public static void taskCompleted(String taskId, String taskName, long timeMs) {
        info("Completed Task " + taskId + " (" + taskName + ") in " + timeMs + " ms");
    }

    public static void taskFailed(String taskId, String taskName) {
        info("Task " + taskId + " (" + taskName + ") failed");
    }

    // General log line: [14:32:05.123] message   (Phase 2: [14:32:05.123] [pool-1-thread-2] message)
    // println is synchronized internally, so lines from different threads never interleave mid-line
    public static void info(String message) {
        StringBuilder line = new StringBuilder();
        line.append('[').append(LocalTime.now().format(TIME_FORMAT)).append("] ");
        if (SHOW_THREAD_NAME) {
            line.append('[').append(Thread.currentThread().getName()).append("] ");
        }
        line.append(message);
        System.out.println(line);
    }

    // ---------- formatting helpers ----------

    // null-safe: a run that has not finished has no endTime
    public static String formatDateTime(LocalDateTime time) {
        return (time == null) ? "-" : time.format(DATE_TIME_FORMAT);
    }

    // 1500 -> "1500 ms"
    public static String formatDuration(long ms) {
        return ms + " ms";
    }

    public static void printWorkflowRunSummary(WorkflowRun run) {
        header("WORKFLOW RUN SUMMARY: " + run.getRunId());
        int[] widths = {12, 24, 16, 16};
        System.out.println(row(widths, "Task ID", "Task Name", "Status", "Time (ms)"));
        System.out.println(separator(widths));
        run.getTaskStatuses().forEach((taskId, status) ->
                System.out.println(row(widths, taskId, run.getTaskNames().get(taskId),
                        status.name(), String.valueOf(run.getTaskTimesMs().get(taskId)))));
        System.out.println("Run status: " + run.getStatus());
        System.out.println("Total simulated time: " + formatDuration(run.getTotalTimeMs()));
    }

    // ---------- console tables ----------

    // Section title:  ===== TITLE =====
    public static void header(String title) {
        System.out.println();
        System.out.println("===== " + title + " =====");
    }

    // One table row, each cell padded to its width. Longer text is cut so columns stay aligned.
    // Example: row(new int[]{10, 20, 8}, "RUN-001", "Payroll", "SUCCESS")
    public static String row(int[] widths, String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            String cell = (cells[i] == null) ? "-" : cells[i];
            int width = widths[i];
            if (cell.length() > width) {
                cell = cell.substring(0, Math.max(0, width - 1)) + "~";
            }
            sb.append(String.format("%-" + width + "s ", cell));
        }
        return sb.toString().stripTrailing();
    }

    public static String separator(int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int width : widths) {
            sb.append("-".repeat(width)).append(' ');
        }
        return sb.toString().stripTrailing();
    }
}