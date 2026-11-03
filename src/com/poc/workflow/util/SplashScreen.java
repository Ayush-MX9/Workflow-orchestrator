package com.poc.workflow.util;

public class SplashScreen {

    public static void bootSequence() {

        String[] messages = {
                "Loading User Registry",
                "Loading Workflow Registry",
                "Loading Task Graphs",
                "Activating Execution Engine",
                "Starting Analytics Service",
                "Initializing Security Modules"
        };

        System.out.println();

        System.out.println(
                "=================================================");
        System.out.println(
                "          WORKFLOW ORCHESTRATOR v1.0");
        System.out.println(
                "=================================================\n");

        try {

            for (String message : messages) {

                System.out.print("> ");

                for (char ch : message.toCharArray()) {

                    System.out.print(ch);

                    Thread.sleep(30);
                }

                System.out.println();

                Thread.sleep(300);
            }

            Thread.sleep(500);

            System.out.println(
                    "\n[SYSTEM ONLINE]");
            System.out.println(
                    "[READY TO ACCEPT REQUESTS]\n");

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }
}