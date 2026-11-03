package com.poc.workflow.util;

import java.util.Scanner;

// The only class that reads from the keyboard. Every method loops until the input is valid,
// so menus never have to handle bad input themselves.
public final class InputUtil {

    // ONE shared Scanner for the whole app. Never create a second Scanner on System.in.
    private static final Scanner SCANNER = new Scanner(System.in);

    private InputUtil() {
    }

    // Non-empty text (names, descriptions, usernames, passwords)
    public static String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Input cannot be empty. Try again.");
        }
    }

    // Whole number inside [min, max]. Used for menu choices.
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }

    // Positive long, used for baseExecutionTimeMs
    public static long readPositiveLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                long value = Long.parseLong(input);
                if (value > 0) {
                    return value;
                }
                System.out.println("Value must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Try again.");
            }
        }
    }

    // y/n confirmation (e.g. "Delete this workflow?")
    public static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = SCANNER.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            }
            if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("Please type y or n.");
        }
    }

    // Optional text: returns "" if the user just presses Enter
    // (used when editing, where Enter means "keep the current value")
    public static String readOptionalString(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }
}