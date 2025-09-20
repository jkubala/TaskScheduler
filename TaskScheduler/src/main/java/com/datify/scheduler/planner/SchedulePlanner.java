package com.datify.scheduler.planner;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.State;
import com.datify.scheduler.planner.strategy.AStarStrategy;
import com.datify.scheduler.planner.strategy.BacktrackingStrategy;
import com.datify.scheduler.planner.strategy.IPlanningStrategy;

public class SchedulePlanner {
    private final IPlanningStrategy strategy;

    public SchedulePlanner() {
        this(new BacktrackingStrategy(SchedulerConfig.defaultConfig(), CostConfig.defaultConfig()));
    }

    public SchedulePlanner(IPlanningStrategy strategy) {
        this.strategy = strategy;
    }

    public State beginPlanning(State startState) {
        if (startState == null) {
            throw new IllegalArgumentException("Start state cannot be null");
        }
        return strategy.findSchedule(startState);
    }
}
