package com.poc.workflow.exception;

public class TaskAlreadyExistsException
        extends Exception {

    public TaskAlreadyExistsException(
            String message) {

        super(message);
    }
}