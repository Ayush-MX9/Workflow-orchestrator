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

    public static final Map<String, AbstractUser> users = new LinkedHashMap<>();
    public static final Map<String, Workflow> workflows = new LinkedHashMap<>();
    public static final List<WorkflowRun> workflowRuns = new ArrayList<>();

    // sample accounts required by the spec
    static {
        users.put("admin", new AdminUser("admin", "admin123"));
        users.put("operator1", new OperatorUser("operator1", "op123"));
    }

    private DataStore() {
    }
}

    


    
  
