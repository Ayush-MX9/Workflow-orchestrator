package com.poc.workflow.model;

import com.poc.workflow.enums.Role;

public class OperatorUser extends AbstractUser {

    public OperatorUser(String username, String password) {
        super(username, password, Role.OPERATOR);
    }
}