package com.poc.workflow.model;

import com.poc.workflow.enums.Role;

public class AdminUser extends AbstractUser {

    public AdminUser(String username, String password) {
        super(username, password, Role.ADMIN);
    }
}