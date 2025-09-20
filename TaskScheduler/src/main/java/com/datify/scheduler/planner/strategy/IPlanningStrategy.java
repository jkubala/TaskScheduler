package com.datify.scheduler.planner.strategy;

import com.datify.scheduler.model.State;

public interface IPlanningStrategy {
    State findSchedule(State startState);
}
