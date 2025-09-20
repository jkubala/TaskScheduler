package com.datify.scheduler.config;

public record CostConfig(int taskPlacementCost, int idealTimeslotMissPenalty) {

    public CostConfig {
        if (taskPlacementCost < 0) throw new IllegalArgumentException("taskPlacementCost must be >= 0");
        if (idealTimeslotMissPenalty < 0) throw new IllegalArgumentException("idealTimeslotMissPenalty must be >= 0");
    }

    public static CostConfig defaultConfig() {
        return new CostConfig(10, 10);
    }
}
