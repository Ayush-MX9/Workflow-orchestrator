package com.poc.workflow.exception;

public class UserAlreadyLoggedInException
        extends Exception {

    public UserAlreadyLoggedInException(
            String message) {

        super(message);
    }
}