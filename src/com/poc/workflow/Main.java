package com.poc.workflow;

import com.poc.workflow.console.MainMenu;
import com.poc.workflow.util.SplashScreen;

public class Main {

    public static void main(String[] args) {
            SplashScreen.bootSequence();

        System.out.println("Workflow Orchestrator & Task Scheduler");
        try {
            new MainMenu().show();
        } catch (Exception e) {

            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}