package com.datify.scheduler.model;

import java.util.Map;
import java.util.UUID;

public record State(
        Map<UUID, Placement> placedTasks,
        Map<UUID, Task> unplacedTasks,
        int costSoFar,
        int totalCostEstimated
) {

    public boolean isComplete() {
        return unplacedTasks.isEmpty();
    }

    public boolean dependenciesPlaced(Task task) {
        return task.getDependencyIds().stream()
                .allMatch(placedTasks::containsKey);
    }

    public boolean canPlaceTask(Task task, TimeSlot candidateSlot) {
        return hasSpaceForTaskIn(candidateSlot) && isAfterDependencies(task, candidateSlot);
    }

    private boolean isAfterDependencies(Task task, TimeSlot candidateSlot) {
        for (UUID depId : task.getDependencyIds()) {
            Placement depPlacement = placedTasks.get(depId);
            if (depPlacement != null && depPlacement.timeSlot().startsAtOrAfter(candidateSlot)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSpaceForTaskIn(TimeSlot timeSlot) {
        return placedTasks.values().stream()
                .noneMatch(p -> p.timeSlot().intersectsWith(timeSlot));
    }
}
