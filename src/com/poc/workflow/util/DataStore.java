package com.poc.workflow.util;

import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.Workflow;
import com.poc.workflow.model.WorkflowRun;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataStore {

    /*
     * Key   -> Username
     * Value -> User Object
     *
     * Example:
     *
     * admin      -> AdminUser Object
     * operator1  -> OperatorUser Object
     */
    public static Map<String, AbstractUser> users =
            new HashMap<>();

    public static Map<String, Workflow> workflows =
        new HashMap<>();

    public static List<WorkflowRun> workflowRuns =
        new ArrayList<>();

    


    
  
}