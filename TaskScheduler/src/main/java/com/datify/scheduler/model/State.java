package com.datify.scheduler.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record State(List<Placement> placedTasks, Map<UUID, Task> unplacedTasks, Map<UUID, Task> validTaskIds,
                    int costSoFar, int totalCostEstimated) {
    public State(List<Placement> placedTasks,
                 Map<UUID, Task> unplacedTasks,
                 Map<UUID, Task> validTaskIds,
                 int costSoFar,
                 int totalCostEstimated) {
        this.placedTasks = List.copyOf(placedTasks);
        this.unplacedTasks = new HashMap<>(unplacedTasks);
        this.validTaskIds = new HashMap<>(validTaskIds);
        this.costSoFar = costSoFar;
        this.totalCostEstimated = totalCostEstimated;
    }

    public boolean isComplete() {
        return unplacedTasks.isEmpty();
    }

    public boolean hasSpaceForTaskIn(TimeSlot timeSlot) {
        for (Placement placedTask : placedTasks) {
            if (placedTask.timeSlot().intersectsWith(timeSlot)) {
                return false;
            }
        }
        return true;
    }
}