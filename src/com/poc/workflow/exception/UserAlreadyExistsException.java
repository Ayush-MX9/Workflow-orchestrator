package com.poc.workflow.exception;

public class UserAlreadyExistsException extends Exception{


    // make constructor 
    public UserAlreadyExistsException(
        String message
    )

    {
        super(message);
    }

    
}