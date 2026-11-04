package com.poc.workflow.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.AdminUser;
import com.poc.workflow.model.OperatorUser;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import com.poc.workflow.model.WorkflowRunContext;   // match Pratik's package

// Shared in-memory store. Every collection is thread-safe.
public final class DataStore {

    // sorted + thread-safe, so tables list in a stable order
    public static final Map<String, AbstractUser> users = new ConcurrentSkipListMap<>();
    public static final Map<String, Workflow> workflows = new ConcurrentSkipListMap<>();

    // finished runs: many readers (reports), one add per finished run
    public static final List<WorkflowRun> workflowRuns = new CopyOnWriteArrayList<>();

    // runs executing right now: runId -> context (added at start, removed on finish)
    public static final ConcurrentHashMap<String, WorkflowRunContext> activeRuns =
            new ConcurrentHashMap<>();

    static {
        users.put("admin", new AdminUser("admin", "admin123"));
        users.put("operator1", new OperatorUser("operator1", "op123"));
    }

    private DataStore() {
    }
}