package com.poc.workflow;

import com.poc.workflow.console.MainMenu;

public class Main {

    public static void main(String[] args) {
        System.out.println("Workflow Orchestrator & Task Scheduler");
        try {
            new MainMenu().show();
        } catch (Exception e) {

            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}