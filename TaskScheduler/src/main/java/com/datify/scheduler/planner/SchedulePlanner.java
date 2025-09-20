package com.datify.scheduler.planner;

import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.State;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.model.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

public class SchedulePlanner {
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final int TIME_SLOT_MINUTES = 10;
    private static final int TASK_PLACEMENT_COST = 10;
    private static final int IDEAL_TIMESLOT_MISS_PENALTY = 10;
    private static final int MAX_NODES = 10000;
    private static final long MAX_TIME_MS = 30000;

    private State bestSolution;
    private int bestCost = Integer.MAX_VALUE;
    private int nodesExplored = 0;
    private long startTime;

    public State beginPlanning(State startState) {
        System.out.println("Starting planning with " + startState.unplacedTasks().size() + " unplaced tasks");

        bestSolution = null;
        bestCost = Integer.MAX_VALUE;
        nodesExplored = 0;
        startTime = System.currentTimeMillis();

        backtrackSearch(startState);

        if (bestSolution != null) {
            System.out.println("Found solution with cost: " + bestCost + " (explored " + nodesExplored + " nodes)");
            return bestSolution;
        } else {
            System.out.println("No valid solution found after exploring " + nodesExplored + " nodes");
            return startState;
        }
    }

    private boolean backtrackSearch(State currentState) {
        nodesExplored++;

        if (nodesExplored > MAX_NODES ||
                (System.currentTimeMillis() - startTime) > MAX_TIME_MS) {
            System.out.println("Search terminated due to limits");
            return bestSolution != null;
        }

        if (currentState.isComplete()) {
            if (currentState.costSoFar() < bestCost) {
                bestSolution = currentState;
                bestCost = currentState.costSoFar();
                System.out.println("Found new best solution with cost: " + bestCost);
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

        List<Placement> possiblePlacements = generatePossiblePlacements(taskToPlace, currentState);
        possiblePlacements.sort(Comparator.comparingInt(Placement::costSoFar));

        boolean foundAnySolution = false;
        for (Placement placement : possiblePlacements) {
            State newState = createStateWithPlacement(currentState, taskToPlace, placement);
            if (newState != null) {
                if (backtrackSearch(newState)) {
                    foundAnySolution = true;
                }
            }
        }

        return foundAnySolution;
    }

    private Task selectNextTask(State currentState) {
        Task mostConstrained = null;
        int minPossibleSlots = Integer.MAX_VALUE;

        for (Task task : currentState.unplacedTasks().values()) {
             if (!areDependenciesSatisfied(task, currentState)) {
                continue;
            }

            int possibleSlots = generatePossiblePlacements(task, currentState).size();
            if (possibleSlots < minPossibleSlots && possibleSlots > 0) {
                minPossibleSlots = possibleSlots;
                mostConstrained = task;
            }
        }

        return mostConstrained;
    }

    private boolean areDependenciesSatisfied(Task task, State currentState) {
        // TODO
        return true;
    }

    private List<Placement> generatePossiblePlacements(Task task, State currentState) {
        List<Placement> placements = new ArrayList<>();

        for (DayOfWeek day : DayOfWeek.values()) {
            LocalTime currentSlot = WORK_START;
            while (true) {
                LocalTime endSlot = currentSlot.plus(task.getDuration());
                if (endSlot.isAfter(WORK_END)) {
                    break;
                }
                TimeSlot timeSlot = new TimeSlot(currentSlot, endSlot, day);
                if (currentState.hasSpaceForTaskIn(timeSlot)) {
                    int cost = calculatePlacementCost(task, timeSlot, day);
                    Placement candidatePlacement = new Placement(
                            task.getId(),
                            timeSlot,
                            cost
                    );
                    placements.add(candidatePlacement);
                }
                currentSlot = currentSlot.plusMinutes(TIME_SLOT_MINUTES);
            }
        }

        return placements;
    }

    private int calculatePlacementCost(Task task, TimeSlot timeWindow, DayOfWeek day) {
        int cost = TASK_PLACEMENT_COST;

        if (!task.getIdealTimeWindows().isEmpty()) {
            boolean inIdealWindow = task.getIdealTimeWindows().stream()
                    .anyMatch(idealWindow -> idealWindow.envelops(timeWindow));
            if (!inIdealWindow) {
                cost += IDEAL_TIMESLOT_MISS_PENALTY;
            }
        }

        return cost;
    }


    private State createStateWithPlacement(State currentState, Task task, Placement placement) {
        try {
            // TODO check for dependencies and discard if they cannot be met
            List<Placement> newPlacedTasks = new ArrayList<>(currentState.placedTasks());
            newPlacedTasks.add(placement);

            Map<UUID, Task> newUnplacedTasks = new HashMap<>(currentState.unplacedTasks());
            newUnplacedTasks.remove(task.getId());

            int newCostSoFar = currentState.costSoFar() + placement.costSoFar();
            int estimatedRemainingCost = estimateRemainingCost(newUnplacedTasks);
            int newTotalEstimated = newCostSoFar + estimatedRemainingCost;

            return new State(
                    newPlacedTasks,
                    newUnplacedTasks,
                    currentState.validTaskIds(),
                    newCostSoFar,
                    newTotalEstimated
            );
        } catch (Exception e) {
            System.err.println("Error creating state: " + e.getMessage());
            return null;
        }
    }

    private int estimateRemainingCost(Map<UUID, Task> unplacedTasks) {
        return unplacedTasks.size() * TASK_PLACEMENT_COST;
    }
}