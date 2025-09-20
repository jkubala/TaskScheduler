package com.datify.scheduler.config;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class SchedulerConfig {
    private final LocalTime workStart;
    private final LocalTime workEnd;
    private final int timeSlotMinutes;
    private final int taskPlacementCost;
    private final int idealTimeslotMissPenalty;
    private final int maxNodes;
    private final long maxTimeMs;

    public static SchedulerConfig defaultConfig() {
        return SchedulerConfig.builder()
                .workStart(LocalTime.of(8, 0))
                .workEnd(LocalTime.of(17, 0))
                .timeSlotMinutes(10)
                .taskPlacementCost(10)
                .idealTimeslotMissPenalty(10)
                .maxNodes(10000)
                .maxTimeMs(30000)
                .build();
    }
}