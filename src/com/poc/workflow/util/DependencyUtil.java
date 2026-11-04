package com.poc.workflow.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.poc.workflow.model.Task;

// Orders tasks so every task comes after the tasks it depends on.
// Tasks keep their list order wherever dependencies allow.
public final class DependencyUtil {

    private DependencyUtil() {
    }

    public static List<Task> sortByDependencies(List<Task> tasks) {
        List<Task> ordered = new ArrayList<>();
        Set<String> placed = new HashSet<>();

        while (ordered.size() < tasks.size()) {
            boolean progress = false;
            for (Task task : tasks) {
                if (!placed.contains(task.getTaskId())
                        && placed.containsAll(task.getDependsOnTaskIds())) {
                    ordered.add(task);
                    placed.add(task.getTaskId());
                    progress = true;
                }
            }
            if (!progress) {
                // can only happen on a cycle or unknown dependency, which the service already blocks
                throw new IllegalStateException("Task dependencies cannot be ordered");
            }
        }
        return ordered;
    }
}