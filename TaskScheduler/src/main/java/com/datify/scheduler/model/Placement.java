package com.datify.scheduler.model;

import java.util.Objects;

public record Placement(Task task, TimeSlot timeSlot) {
    public Placement {
        Objects.requireNonNull(task, "Task must not be null");
        Objects.requireNonNull(timeSlot, "TimeSlot must not be null");
    }
}