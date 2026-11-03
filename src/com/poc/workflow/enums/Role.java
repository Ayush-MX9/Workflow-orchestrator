package com.poc.workflow.enums;

// Type of user in the system. A user's role decides which menu they see.
public enum Role {
    ADMIN,      // manages workflows, tasks and views all runs/analytics
    OPERATOR    // views active workflows and triggers executions
}