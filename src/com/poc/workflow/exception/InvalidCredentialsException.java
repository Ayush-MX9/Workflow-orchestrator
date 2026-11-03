package com.poc.workflow.exception;

public class InvalidCredentialsException
        extends Exception {

    public InvalidCredentialsException(
            String message) {

        super(message);
    }
}