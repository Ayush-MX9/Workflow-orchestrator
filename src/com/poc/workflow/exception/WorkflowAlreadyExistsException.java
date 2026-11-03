package com.poc.workflow.exception;

public class WorkflowAlreadyExistsException
        extends Exception {

    public WorkflowAlreadyExistsException(
            String message) {

        super(message);
    }
}