package com.poc.workflow.exception;

public class DuplicateUserException extends Exception {

    public DuplicateUserException(String message) {
        super(message);
    }
}