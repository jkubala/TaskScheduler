package com.datify.scheduler.util;

import com.datify.scheduler.model.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class DependencyValidator {
    public static boolean hasCircularDependencies(Map<UUID, Task> tasks) {
        Set<UUID> visiting = new HashSet<>();
        Set<UUID> visited = new HashSet<>();

        for (UUID taskId : tasks.keySet()) {
            if (!visited.contains(taskId)) {
                if (hasCircularDependencyDFS(taskId, tasks, visiting, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasCircularDependencyDFS(UUID taskId, Map<UUID, Task> tasks, Set<UUID> visiting, Set<UUID> visited) {
        if (visiting.contains(taskId)) {
            log.error("Circular dependency detected involving task: {}",
                    tasks.get(taskId) != null ? tasks.get(taskId).getName() : taskId);
            return true;
        }

        if (visited.contains(taskId)) {
            return false;
        }

        visiting.add(taskId);

        Task task = tasks.get(taskId);
        if (task != null) {
            for (UUID dependencyId : task.getDependencyIds()) {
                if (hasCircularDependencyDFS(dependencyId, tasks, visiting, visited)) {
                    return true;
                }
            }
        }

        visiting.remove(taskId);
        visited.add(taskId);

        return false;
    }
}