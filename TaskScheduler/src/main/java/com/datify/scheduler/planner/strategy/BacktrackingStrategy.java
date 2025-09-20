package com.datify.scheduler.planner.strategy;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.State;
import com.datify.scheduler.model.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class BacktrackingStrategy extends AbstractPlanningStrategy {

    private State bestSolution;
    private int bestCost;
    private int nodesExplored;
    private long startTime;

    public BacktrackingStrategy(SchedulerConfig schedulerConfig, CostConfig costConfig) {
        super(schedulerConfig, costConfig);
    }

    @Override
    public State findSchedule(State startState) {
        if (startState == null) {
            throw new IllegalArgumentException("Start state cannot be null");
        }

        bestSolution = null;
        bestCost = Integer.MAX_VALUE;
        nodesExplored = 0;
        startTime = System.currentTimeMillis();

        log.info("Starting backtracking search with {} unplaced tasks", startState.unplacedTasks().size());
        backtrackSearch(startState);

        long elapsed = System.currentTimeMillis() - startTime;
        if (bestSolution != null) {
            log.info("Found best solution with cost {} after {} nodes in {}ms", bestCost, nodesExplored, elapsed);
            return bestSolution;
        } else {
            log.warn("No valid solution found after {} nodes in {}ms", nodesExplored, elapsed);
            return startState;
        }
    }

    private boolean backtrackSearch(State currentState) {
        nodesExplored++;

        if (limitReached(nodesExplored, startTime)) {
            return bestSolution != null;
        }

        if (currentState.isComplete()) {
            int cost = currentState.costSoFar();
            if (cost < bestCost) {
                bestSolution = currentState;
                bestCost = cost;
                log.info("New best solution found: {}", bestCost);
                return true;
            }
            return false;
        }

        if (currentState.totalCostEstimated() >= bestCost) {
            return false;
        }

        Task taskToPlace = selectNextTask(currentState);
        if (taskToPlace == null) {
            return false;
        }

        List<Placement> placements = generatePlacements(taskToPlace, currentState);
        placements.sort(Comparator.comparingInt(p -> calculatePlacementCost(taskToPlace, p.timeSlot())));

        boolean foundAnySolution = false;
        for (Placement placement : placements) {
            State newState = createStateWithPlacement(currentState, taskToPlace, placement);
            if (backtrackSearch(newState)) {
                foundAnySolution = true;
            }
        }

        return foundAnySolution;
    }

    private Task selectNextTask(State state) {
        Task mostConstrained = null;
        int minSlots = Integer.MAX_VALUE;

        for (Task task : state.unplacedTasks().values()) {
            int slots = generatePlacements(task, state).size();
            if (slots < minSlots && slots > 0) {
                minSlots = slots;
                mostConstrained = task;
            }
        }

        return mostConstrained;
    }
}