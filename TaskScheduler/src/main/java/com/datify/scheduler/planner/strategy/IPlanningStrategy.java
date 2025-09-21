package com.datify.scheduler.planner.strategy;

import com.datify.scheduler.model.ScheduleState;

public interface IPlanningStrategy {
    ScheduleState findSchedule(ScheduleState startScheduleState);
}
