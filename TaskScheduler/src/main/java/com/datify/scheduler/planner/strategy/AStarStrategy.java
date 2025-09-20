package com.datify.scheduler.planner.strategy;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.State;
import com.datify.scheduler.model.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.PriorityQueue;

@Slf4j
public class AStarStrategy extends AbstractPlanningStrategy {

    public AStarStrategy(SchedulerConfig schedulerConfig, CostConfig costConfig) {
        super(schedulerConfig, costConfig);
    }

    @Override
    public State findSchedule(State startState) {
        if (startState == null) {
            throw new IllegalArgumentException("Start state cannot be null");
        }

        PriorityQueue<State> frontier = new PriorityQueue<>(Comparator.comparingInt(State::totalCostEstimated));
        frontier.add(startState);
        State bestSolution = null;
        int nodesExplored = 0;
        long startTime = System.currentTimeMillis();

        while (!frontier.isEmpty()) {
            State current = frontier.poll();
            nodesExplored++;

            if (current.isComplete()) {
                if (bestSolution == null || current.costSoFar() < bestSolution.costSoFar()) {
                    bestSolution = current;
                }
                continue;
            }

            if (nodesExplored > schedulerConfig.maxNodes() ||
                    System.currentTimeMillis() - startTime > schedulerConfig.maxTimeMs()) {
                log.warn("A* search stopped: node or time limit reached");
                break;
            }

            for (Task task : current.unplacedTasks().values()) {
                if (!areDependenciesSatisfied(task, current)) {
                    continue;
                }

                for (Placement placement : generatePlacements(task, current)) {
                    State next = createStateWithPlacement(current, task, placement);
                    frontier.add(next);
                }
            }
        }

        log.info("A* search explored {} nodes", nodesExplored);
        return bestSolution != null ? bestSolution : startState;
    }
}