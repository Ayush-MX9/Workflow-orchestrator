package com.poc.workflow.console;

import com.poc.workflow.model.AbstractUser;

// TEMPORARY stub: Pratik writes the real Operator menu.
public class OperatorMenu {

    private final AbstractUser currentUser;

    public OperatorMenu(AbstractUser currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        System.out.println("[Operator menu for " + currentUser.getUsername() + " coming soon]");
    }
}