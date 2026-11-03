package com.poc.workflow.exception;

public class TaskNotFoundException extends Exception {

    public TaskNotFoundException(String message) {
        super(message);
    }
}