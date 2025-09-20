package com.datify.scheduler.planner.strategy;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.State;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.model.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public abstract class AbstractPlanningStrategy implements IPlanningStrategy {
    protected final SchedulerConfig schedulerConfig;
    protected final CostConfig costConfig;

    protected AbstractPlanningStrategy(SchedulerConfig schedulerConfig, CostConfig costConfig) {
        this.schedulerConfig = schedulerConfig;
        this.costConfig = costConfig;
    }

    protected boolean areDependenciesSatisfied(Task task, State state) {
        // TODO
        return true;
    }

    protected List<Placement> generatePlacements(Task task, State state) {
        List<Placement> placements = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            LocalTime slot = schedulerConfig.workStart();
            while (true) {
                LocalTime end = slot.plus(task.getDuration());
                if (end.isAfter(schedulerConfig.workEnd())) {
                    break;
                }

                TimeSlot ts = new TimeSlot(slot, end, day);
                if (state.hasSpaceForTaskIn(ts)) {
                    int cost = calculatePlacementCost(task, ts);
                    placements.add(new Placement(task.getId(), ts, cost));
                }
                slot = slot.plusMinutes(schedulerConfig.timeSlotMinutes());
            }
        }
        return placements;
    }

    protected int calculatePlacementCost(Task task, TimeSlot timeSlot) {
        int cost = costConfig.taskPlacementCost();
        if (!task.getIdealTimeWindows().isEmpty()) {
            boolean inWindow = task.getIdealTimeWindows().stream()
                    .anyMatch(ideal -> ideal.envelops(timeSlot));
            if (!inWindow) {
                cost += costConfig.idealTimeslotMissPenalty();
            }
        }
        return cost;
    }

    protected State createStateWithPlacement(State current, Task task, Placement placement) {
        List<Placement> newPlaced = new ArrayList<>(current.placedTasks());
        newPlaced.add(placement);

        Map<UUID, Task> newUnplaced = new HashMap<>(current.unplacedTasks());
        newUnplaced.remove(task.getId());

        int newCost = current.costSoFar() + placement.costSoFar();
        int estimatedRemaining = estimateRemainingCost(newUnplaced);

        return new State(newPlaced, newUnplaced, current.validTaskIds(), newCost, newCost + estimatedRemaining);
    }

    protected int estimateRemainingCost(Map<UUID, Task> unplacedTasks) {
        return unplacedTasks.size() * costConfig.taskPlacementCost();
    }

    @Override
    public abstract State findSchedule(State startState);
}