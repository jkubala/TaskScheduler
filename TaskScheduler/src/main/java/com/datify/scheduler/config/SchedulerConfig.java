package com.datify.scheduler.config;

import java.time.LocalTime;

public record SchedulerConfig(
        LocalTime workStart,
        LocalTime workEnd,
        int timeSlotMinutes,
        int taskPlacementCost,
        int idealTimeslotMissPenalty,
        int maxNodes,
        long maxTimeMs
) {
    public SchedulerConfig {
        if (workStart == null) throw new IllegalArgumentException("workStart must not be null");
        if (workEnd == null) throw new IllegalArgumentException("workEnd must not be null");
        if (!workStart.isBefore(workEnd)) throw new IllegalArgumentException("workStart must be before workEnd");
        if (timeSlotMinutes <= 0) throw new IllegalArgumentException("timeSlotMinutes must be > 0");
        if (taskPlacementCost < 0) throw new IllegalArgumentException("taskPlacementCost must be >= 0");
        if (idealTimeslotMissPenalty < 0) throw new IllegalArgumentException("idealTimeslotMissPenalty must be >= 0");
        if (maxNodes <= 0) throw new IllegalArgumentException("maxNodes must be > 0");
        if (maxTimeMs < 0) throw new IllegalArgumentException("maxTimeMs must be >= 0");
    }

    public static SchedulerConfig defaultConfig() {
        return new SchedulerConfig(
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                10,
                10,
                10,
                10000,
                30000
        );
    }
}
