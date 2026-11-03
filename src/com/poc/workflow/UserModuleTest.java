package com.poc.workflow;

import com.poc.workflow.enums.Role;
import com.poc.workflow.model.AbstractUser;
import com.poc.workflow.model.AdminUser;
import com.poc.workflow.model.OperatorUser;
import com.poc.workflow.service.UserService;
import com.poc.workflow.servicesImpl.UserServiceImpl;
import java.util.Scanner;

public class UserModuleTest {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        UserService userService =
                new UserServiceImpl();

        while (true) {

            System.out.println("\n===== USER MODULE TEST =====");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");

            System.out.print("Enter Choice : ");

            int choice =
                    Integer.parseInt(
                            scanner.nextLine());

            switch (choice) {

                case 1:

                    try {

                        System.out.print(
                                "Enter Username : ");

                        String username =
                                scanner.nextLine();

                        System.out.print(
                                "Enter Password : ");

                        String password =
                                scanner.nextLine();

                        System.out.print(
                                "Enter Role (ADMIN/OPERATOR) : ");

                        Role role =
                                Role.valueOf(
                                        scanner.nextLine()
                                                .toUpperCase());

                        AbstractUser user;

                        if (role == Role.ADMIN) {

                            user = new AdminUser(
                                    username,
                                    password);

                        } else {

                            user = new OperatorUser(
                                    username,
                                    password);
                        }

                        userService.registerUser(user);

                        System.out.println(
                                "Registration Successful");

                    } catch (Exception e) {

                        System.out.println(
                                e.getMessage());
                    }

                    break;

                case 2:

                    try {

                        System.out.print(
                                "Enter Username : ");

                        String username =
                                scanner.nextLine();

                        System.out.print(
                                "Enter Password : ");

                        String password =
                                scanner.nextLine();

                        AbstractUser user =
                                userService.login(
                                        username,
                                        password);

                        System.out.println(
                                "Login Successful");

                        System.out.println(
                                "Welcome "
                                        + user.getUsername());

                        System.out.println(
                                "Role : "
                                        + user.getRole());

                    } catch (Exception e) {

                        System.out.println(
                                e.getMessage());
                    }

                    break;

                case 3:

                    System.out.println(
                            "Exiting Application...");

                    scanner.close();

                    System.exit(0);

                default:

                    System.out.println(
                            "Invalid Choice");
            }
        }
    }
}