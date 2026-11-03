package com.poc.workflow.console;

import com.poc.workflow.model.AbstractUser;

// TEMPORARY stub: replaced by the real Admin menu later.
public class AdminMenu {

    private final AbstractUser currentUser;

    public AdminMenu(AbstractUser currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        System.out.println("[Admin menu for " + currentUser.getUsername() + " coming soon]");
    }
}