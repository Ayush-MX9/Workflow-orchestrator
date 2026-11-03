package com.poc.workflow.exception;

public class WorkflowNotFoundException
        extends Exception {

    public WorkflowNotFoundException(
            String message) {

        super(message);
    }
}