package com.poc.workflow.exception;

public class InactiveWorkflowExecutionException
        extends Exception {

    public InactiveWorkflowExecutionException(
            String message) {

        super(message);
    }
}