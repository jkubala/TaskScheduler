package com.datify.scheduler.planner.strategy;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.ScheduleState;
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

    protected List<Placement> generatePlacements(Task task, ScheduleState scheduleState) {
        List<Placement> placements = new ArrayList<>();

        if (!scheduleState.dependenciesPlaced(task)) {
            return Collections.emptyList();
        }

        for (var day : DayOfWeek.values()) {
            LocalTime slot = schedulerConfig.workStart();
            while (true) {
                LocalTime end = slot.plus(task.getDuration());
                if (end.isAfter(schedulerConfig.workEnd())) break;

                TimeSlot ts = new TimeSlot(slot, end, day);

                if (scheduleState.canPlaceTask(task, ts)) {
                    placements.add(new Placement(task, ts));
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

    protected ScheduleState createStateWithPlacement(ScheduleState current, Task task, Placement placement) {
        Map<UUID, Placement> newPlaced = new HashMap<>(current.placedTasks());
        newPlaced.put(task.getId(), placement);

        Map<UUID, Task> newUnplaced = new HashMap<>(current.unplacedTasks());
        newUnplaced.remove(task.getId());

        int newCost = current.costSoFar() + calculatePlacementCost(task, placement.timeSlot());
        int totalCostEstimated = newCost + estimateRemainingCost(current, newUnplaced);

        return new ScheduleState(newPlaced, newUnplaced, newCost, totalCostEstimated);
    }

    protected int estimateRemainingCost(ScheduleState currentScheduleState, Map<UUID, Task> unplacedTasks) {
        int baseCost = unplacedTasks.size() * costConfig.taskPlacementCost();
        int potentialPenalty = 0;

        for (Task task : unplacedTasks.values()) {
            if (!task.getIdealTimeWindows().isEmpty()) {
                boolean hasAvailableIdeal = task.getIdealTimeWindows().stream()
                        .anyMatch(ideal -> currentScheduleState.canPlaceTask(task, ideal));
                if (!hasAvailableIdeal) {
                    potentialPenalty += costConfig.idealTimeslotMissPenalty();
                }
            }
        }

        return baseCost + potentialPenalty;
    }

    @Override
    public abstract ScheduleState findSchedule(ScheduleState startScheduleState);

    protected boolean limitReached(int nodesExplored, long startTime) {
        return nodesExplored > schedulerConfig.maxNodes() ||
                System.currentTimeMillis() - startTime > schedulerConfig.maxTimeMs();
    }
}