package com.poc.workflow.enums;

public enum TaskPriority {

    HIGH(300), // number given so,  that it will be used for sorting the task
    
    // internally :public static final TaskPriority HIGH = new TaskPriority(300);
 
    MEDIUM(200),
    LOW(100);

    // encapsulated so that it can not be changed anythime 
    private final int priorityFactor;


// constructor for initialisation 
    TaskPriority(int priorityFactor) {
        this.priorityFactor = priorityFactor;
    }


// setting a getter method to access the priority factor
    public int getPriorityFactor() {
        return priorityFactor;
    }
}
