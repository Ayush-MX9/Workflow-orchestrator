package com.poc.workflow.util;

import com.poc.workflow.model.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DependencyUtil {

    private DependencyUtil() {
    }

    public static List<Task> sortByDependencies(List<Task> tasks) {
        Map<String, Task> tasksById = new HashMap<>();
        for (Task task : tasks) {
            if (tasksById.put(task.getTaskId(), task) != null) {
                throw new IllegalArgumentException("Duplicate task id: " + task.getTaskId());
            }
        }

        List<Task> sorted = new ArrayList<>();
        Set<String> completed = new HashSet<>();
        while (sorted.size() < tasks.size()) {
            boolean madeProgress = false;
            for (Task task : tasks) {
                if (completed.contains(task.getTaskId())) {
                    continue;
                }

                for (String dependencyId : task.getDependsOnTaskIds()) {
                    if (!tasksById.containsKey(dependencyId)) {
                        throw new IllegalArgumentException("Task " + task.getTaskId()
                                + " depends on unknown task: " + dependencyId);
                    }
                }

                if (completed.containsAll(task.getDependsOnTaskIds())) {
                    sorted.add(task);
                    completed.add(task.getTaskId());
                    madeProgress = true;
                }
            }
            if (!madeProgress) {
                throw new IllegalArgumentException("Workflow contains a circular dependency");
            }
        }
        return sorted;
    }
}
