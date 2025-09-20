package com.datify.scheduler.config;

import java.time.LocalTime;

public record SchedulerConfig(LocalTime workStart, LocalTime workEnd, int timeSlotMinutes, int taskPlacementCost,
                              int idealTimeslotMissPenalty, int maxNodes, long maxTimeMs) {
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
