package com.poc.workflow.exception;

public class CircularDependencyException
        extends Exception {

    public CircularDependencyException(
            String message) {

        super(message);
    }
}