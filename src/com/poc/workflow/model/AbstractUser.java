package com.poc.workflow.model;
//It’s the shared parent for admins and operators and holds the login data. It depends only on Role.
import com.poc.workflow.enums.Role;

public abstract class AbstractUser {

    private final String username;
    private String password;
    private final Role role;
    private boolean loggedIn;   // supports the "no second login" rule

    protected AbstractUser(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.loggedIn = false;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    // The password is never exposed through a getter; services compare it here.
    public boolean checkPassword(String input) {
        return password.equals(input);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}