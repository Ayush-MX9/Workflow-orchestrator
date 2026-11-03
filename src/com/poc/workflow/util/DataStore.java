package com.poc.workflow.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.AdminUser;
import com.poc.workflow.model.OperatorUser;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;

public final class DataStore {

    // key = username
    public static final Map<String, AbstractUser> USERS = new LinkedHashMap<>();

    // key = workflowId (LinkedHashMap keeps creation order for display)
    public static final Map<String, Workflow> WORKFLOWS = new LinkedHashMap<>();

    // every run ever triggered, oldest first
    public static final List<WorkflowRun> RUN_HISTORY = new ArrayList<>();

    // static block runs once when the class is first used: seeds the sample users
    static {
        USERS.put("admin", new AdminUser("admin", "admin123"));
        USERS.put("operator1", new OperatorUser("operator1", "op123"));
    }

    // utility class: no objects should be created
    private DataStore() {
    }
}