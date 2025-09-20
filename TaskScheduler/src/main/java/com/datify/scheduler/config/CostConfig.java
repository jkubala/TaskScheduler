package com.datify.scheduler.config;

public record CostConfig(int taskPlacementCost, int idealTimeslotMissPenalty) {

    public static CostConfig defaultConfig() {
        return new CostConfig(10, 10);
    }
}
