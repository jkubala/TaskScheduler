package com.datify.scheduler.planner.strategy;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.ScheduleState;
import com.datify.scheduler.model.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class BacktrackingStrategy extends AbstractPlanningStrategy {

    private ScheduleState bestSolution;
    private int bestCost;
    private int nodesExplored;
    private long startTime;

    public BacktrackingStrategy(SchedulerConfig schedulerConfig, CostConfig costConfig) {
        super(schedulerConfig, costConfig);
    }

    @Override
    public ScheduleState findSchedule(ScheduleState startScheduleState) {
        if (startScheduleState == null) {
            throw new IllegalArgumentException("Start state cannot be null");
        }

        bestSolution = null;
        bestCost = Integer.MAX_VALUE;
        nodesExplored = 0;
        startTime = System.currentTimeMillis();

        log.info("Starting backtracking search with {} unplaced tasks", startScheduleState.unplacedTasks().size());
        backtrackSearch(startScheduleState);

        long elapsed = System.currentTimeMillis() - startTime;
        if (bestSolution != null) {
            log.info("Found best solution with cost {} after {} nodes in {}ms", bestCost, nodesExplored, elapsed);
            return bestSolution;
        } else {
            log.warn("No valid solution found after {} nodes in {}ms", nodesExplored, elapsed);
            return startScheduleState;
        }
    }

    private boolean backtrackSearch(ScheduleState currentScheduleState) {
        nodesExplored++;

        if (limitReached(nodesExplored, startTime)) {
            return bestSolution != null;
        }

        if (currentScheduleState.isComplete()) {
            int cost = currentScheduleState.costSoFar();
            if (cost < bestCost) {
                bestSolution = currentScheduleState;
                bestCost = cost;
                log.info("New best solution found: {}", bestCost);
                return true;
            }
            return false;
        }

        if (currentScheduleState.totalCostEstimated() >= bestCost) {
            return false;
        }

        Task taskToPlace = selectNextTask(currentScheduleState);
        if (taskToPlace == null) {
            return false;
        }

        List<Placement> placements = generatePlacements(taskToPlace, currentScheduleState);
        placements.sort(Comparator.comparingInt(p -> calculatePlacementCost(taskToPlace, p.timeSlot())));

        boolean foundAnySolution = false;
        for (Placement placement : placements) {
            ScheduleState newScheduleState = createStateWithPlacement(currentScheduleState, taskToPlace, placement);
            if (backtrackSearch(newScheduleState)) {
                foundAnySolution = true;
            }
        }

        return foundAnySolution;
    }

    private Task selectNextTask(ScheduleState scheduleState) {
        Task mostConstrained = null;
        int minSlots = Integer.MAX_VALUE;

        for (Task task : scheduleState.unplacedTasks().values()) {
            int slots = generatePlacements(task, scheduleState).size();
            if (slots < minSlots && slots > 0) {
                minSlots = slots;
                mostConstrained = task;
            }
        }

        return mostConstrained;
    }
}