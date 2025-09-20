package com.datify.scheduler.planner;

import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.State;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.model.TimeSlot;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

@Slf4j
public class SchedulePlanner {
    private final SchedulerConfig config;

    private State bestSolution;
    private int bestCost = Integer.MAX_VALUE;
    private int nodesExplored = 0;
    private long startTime;

    public SchedulePlanner() {
        this(SchedulerConfig.defaultConfig());
    }

    public SchedulePlanner(SchedulerConfig config) {
        this.config = config;
    }

    public State beginPlanning(State startState) {
        if (startState == null) {
            throw new IllegalArgumentException("Start state cannot be null");
        }

        log.info("Starting planning with {} unplaced tasks", startState.unplacedTasks().size());
        log.debug("Using config: work hours {}-{}, max nodes: {}", config.getWorkStart(), config.getWorkEnd(), config.getMaxNodes());

        bestSolution = null;
        bestCost = Integer.MAX_VALUE;
        nodesExplored = 0;
        startTime = System.currentTimeMillis();

        backtrackSearch(startState);
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (bestSolution != null) {
            log.info("Found solution with cost: {} (explored {} nodes in {}ms)", bestCost, nodesExplored, elapsedTime);
            return bestSolution;
        } else {
            log.warn("No valid solution found after exploring {} nodes in {}ms", nodesExplored, elapsedTime);
            return startState;
        }
    }

    private boolean backtrackSearch(State currentState) {
        nodesExplored++;

        if (log.isTraceEnabled() && nodesExplored % 1000 == 0) {
            log.trace("Explored {} nodes, current cost: {}", nodesExplored, currentState.costSoFar());
        }

        if (nodesExplored > config.getMaxNodes()) {
            log.warn("Search terminated: exceeded maximum nodes ({})", config.getMaxNodes());
            return bestSolution != null;
        }

        if ((System.currentTimeMillis() - startTime) > config.getMaxTimeMs()) {
            log.warn("Search terminated: exceeded time limit ({}ms)", config.getMaxTimeMs());
            return bestSolution != null;
        }

        if (currentState.isComplete()) {
            if (currentState.costSoFar() < bestCost) {
                bestSolution = currentState;
                bestCost = currentState.costSoFar();
                log.info("Found new best solution with cost: {} (was: {})", currentState.costSoFar(), bestCost);
                return true;
            }
            log.debug("Complete state found but not better (cost: {}, best: {})", currentState.costSoFar(), bestCost);
            return false;
        }

        if (currentState.totalCostEstimated() >= bestCost) {
            log.trace("Pruning branch: estimated cost {} >= best cost {}", currentState.totalCostEstimated(), bestCost);
            return false;
        }

        Task taskToPlace = selectNextTask(currentState);
        if (taskToPlace == null) {
            log.debug("No task available to place (dependencies not satisfied)");
            return false;
        }

        log.debug("Selected task to place: {} (duration: {})", taskToPlace.getName(), taskToPlace.getDuration());

        List<Placement> possiblePlacements = generatePossiblePlacements(taskToPlace, currentState);
        possiblePlacements.sort(Comparator.comparingInt(Placement::costSoFar));

        log.debug("Generated {} possible placements for task {}", possiblePlacements.size(), taskToPlace.getName());

        boolean foundAnySolution = false;
        for (Placement placement : possiblePlacements) {
            log.trace("Trying placement: {} on {} from {} to {}",
                    taskToPlace.getName(),
                    placement.timeSlot().dayOfWeek(),
                    placement.timeSlot().start(),
                    placement.timeSlot().end());
            State newState = createStateWithPlacement(currentState, taskToPlace, placement);
            if (newState != null) {
                if (backtrackSearch(newState)) {
                    foundAnySolution = true;
                }
            } else {
                log.trace("Failed to create valid state for placement");
            }
        }

        return foundAnySolution;
    }

    private Task selectNextTask(State currentState) {
        Task mostConstrained = null;
        int minPossibleSlots = Integer.MAX_VALUE;

        for (Task task : currentState.unplacedTasks().values()) {
             if (!areDependenciesSatisfied(task, currentState)) {
                 log.trace("Task {} skipped due to unsatisfied dependencies", task.getName());
                 continue;
            }

            int possibleSlots = generatePossiblePlacements(task, currentState).size();
            log.trace("Task {} has {} possible placement slots", task.getName(), possibleSlots);
            if (possibleSlots < minPossibleSlots && possibleSlots > 0) {
                minPossibleSlots = possibleSlots;
                mostConstrained = task;
            }
        }

        if (mostConstrained != null) {
            log.debug("Most constrained task selected: {} ({} possible slots)",
                    mostConstrained.getName(), minPossibleSlots);
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
            LocalTime currentSlot = config.getWorkStart();
            while (true) {
                LocalTime endSlot = currentSlot.plus(task.getDuration());
                if (endSlot.isAfter(config.getWorkEnd())) {
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
                currentSlot = currentSlot.plusMinutes(config.getTimeSlotMinutes());
            }
        }

        return placements;
    }

    private int calculatePlacementCost(Task task, TimeSlot timeWindow, DayOfWeek day) {
        int cost = config.getTaskPlacementCost();

        if (!task.getIdealTimeWindows().isEmpty()) {
            boolean inIdealWindow = task.getIdealTimeWindows().stream()
                    .anyMatch(idealWindow -> idealWindow.envelops(timeWindow));
            if (!inIdealWindow) {
                cost += config.getIdealTimeslotMissPenalty();
                log.trace("Task {} placed outside ideal window, adding penalty", task.getName());
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
            log.error("Error creating state with placement for task {}: {}", task.getName(), e.getMessage(), e);
            return null;
        }
    }

    private int estimateRemainingCost(Map<UUID, Task> unplacedTasks) {
        return unplacedTasks.size() * config.getTaskPlacementCost();
    }
}