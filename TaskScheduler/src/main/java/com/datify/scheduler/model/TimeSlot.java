package com.datify.scheduler.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimeSlot(LocalTime start, LocalTime end, DayOfWeek dayOfWeek) {
    public TimeSlot {
        if (start == null || end == null || dayOfWeek == null) {
            throw new IllegalArgumentException("Start, end and dayOfWeek must not be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start must be before or equal to end");
        }
    }

    public boolean intersectsWith(TimeSlot other) {
        return dayOfWeek == other.dayOfWeek
                && this.start.isBefore(other.end)
                && other.start.isBefore(this.end);
    }

    public boolean envelops(TimeSlot other) {
        return this.dayOfWeek == other.dayOfWeek
                && (other.start.equals(this.start) || other.start.isAfter(this.start))
                && (other.end.equals(this.end) || other.end.isBefore(this.end));
    }
}
