package com.poc.workflow.console;

import com.poc.workflow.enums.Role;
import com.poc.workflow.exception.DuplicateUserException;
import com.poc.workflow.exception.InvalidCredentialsException;
import com.poc.workflow.exception.UserAlreadyLoggedInException;
import com.poc.workflow.exception.UserNotFoundException;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.AdminUser;
import com.poc.workflow.model.OperatorUser;
import com.poc.workflow.service.ExecutionService;
import com.poc.workflow.service.UserService;
import com.poc.workflow.servicesImpl.ExecutionServiceImpl;
import com.poc.workflow.servicesImpl.UserServiceImpl;
import com.poc.workflow.util.InputUtil;
import com.poc.workflow.util.ProgressLogger;

// Entry menu. Also owns the application's background resources:
// the progress logger thread and the shared executor.
public class MainMenu {

    private final UserService userService = new UserServiceImpl();
    private final ExecutionService executionService = new ExecutionServiceImpl(0.1);
    private final ProgressLogger progressLogger = new ProgressLogger();

    public void show() {
        progressLogger.start();
        try {
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
                        System.out.println("Shutting down...");
                        running = false;
                    }
                }
            }
        } finally {
            // runs on a normal Exit AND if something throws, so no thread is left behind
            progressLogger.stop();
            executionService.shutdown();   // waits for running tasks (Pratik's awaitTermination)
            System.out.println("Goodbye!");
        }
    }

    private void register() {
        String username = InputUtil.readString("Username: ");
        String password = InputUtil.readString("Password: ");

        System.out.println("Role:  1. ADMIN   2. OPERATOR");
        int roleChoice = InputUtil.readInt("Select role: ", 1, 2);
        Role role = (roleChoice == 1) ? Role.ADMIN : Role.OPERATOR;

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
            try {
                if (user.getRole() == Role.ADMIN) {
                    new AdminMenu(user, executionService).show();
                } else {
                    new OperatorMenu(user, executionService).show();
                }
            } finally {
                logout(user);   // session is freed even if the sub-menu crashes
            }
        } catch (InvalidCredentialsException | UserAlreadyLoggedInException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void logout(AbstractUser user) {
        try {
            userService.logout(user.getUsername());
            System.out.println("Logged out.");
        } catch (UserNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}