package com.poc.workflow.console;

import com.poc.workflow.enums.Role;
import com.poc.workflow.exception.DuplicateUserException;
import com.poc.workflow.exception.InvalidCredentialsException;
import com.poc.workflow.exception.UserAlreadyLoggedInException;
import com.poc.workflow.exception.UserNotFoundException;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.AdminUser;
import com.poc.workflow.model.OperatorUser;
import com.poc.workflow.service.UserService;
import com.poc.workflow.servicesImpl.UserServiceImpl;
import com.poc.workflow.util.InputUtil;

// Entry menu: Register / Login / Exit.
// Takes input, passes it to UserService, and shows the result or the error message.
public class MainMenu {

    // depends on the interface, not the implementation
    private final UserService userService = new UserServiceImpl();

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=========== MAIN MENU ===========");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");

            int choice = InputUtil.readInt("Enter choice: ", 1, 3);

            switch (choice) {
                case 1 -> register();
                case 2 -> login();
                case 3 -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
            }
        }
    }

    private void register() {
        String username = InputUtil.readString("Username: ");
        String password = InputUtil.readString("Password: ");

        System.out.println("Role:  1. ADMIN   2. OPERATOR");
        int roleChoice = InputUtil.readInt("Select role: ", 1, 2);
        Role role = (roleChoice == 1) ? Role.ADMIN : Role.OPERATOR;

        // the service takes a ready-made user object, so the menu builds the right subclass
        AbstractUser user = (role == Role.ADMIN)
                ? new AdminUser(username, password)
                : new OperatorUser(username, password);

        try {
            userService.registerUser(user);
            System.out.println("User registered successfully: " + user);
        } catch (DuplicateUserException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void login() {
        String username = InputUtil.readString("Username: ");
        String password = InputUtil.readString("Password: ");

        try {
            AbstractUser user = userService.login(username, password);
            System.out.println("Login successful. Welcome, " + user.getUsername() + "!");

            // route by role; the sub-menu returns when the user picks Logout
            if (user.getRole() == Role.ADMIN) {
                new AdminMenu(user).show();
            } else {
                new OperatorMenu(user).show();
            }

            logout(user);

        } catch (InvalidCredentialsException | UserAlreadyLoggedInException e) {
            // two different business errors, same handling: show the message
            System.out.println("Error: " + e.getMessage());
        }
    }

    // frees the session so the user can log in again later
    private void logout(AbstractUser user) {
        try {
            userService.logout(user.getUsername());
            System.out.println("Logged out.");
        } catch (UserNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}