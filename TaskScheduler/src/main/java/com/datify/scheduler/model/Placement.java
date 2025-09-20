package com.datify.scheduler.model;
import java.util.UUID;

public record Placement(UUID taskId, TimeSlot timeSlot, int costSoFar) {
    public Placement {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID must not be null");
        }
        if (timeSlot == null) {
            throw new IllegalArgumentException("Time window must not be null");
        }
    }
}