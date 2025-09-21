package com.datify.scheduler.planner.strategy;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.ScheduleState;
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
    public ScheduleState findSchedule(ScheduleState startScheduleState) {
        if (startScheduleState == null) {
            throw new IllegalArgumentException("Start state cannot be null");
        }

        PriorityQueue<ScheduleState> frontier = new PriorityQueue<>(Comparator.comparingInt(ScheduleState::totalCostEstimated));
        frontier.add(startScheduleState);
        ScheduleState bestSolution = null;
        int nodesExplored = 0;
        long startTime = System.currentTimeMillis();

        while (!frontier.isEmpty()) {
            ScheduleState current = frontier.poll();
            nodesExplored++;

            if (current.isComplete()) {
                if (bestSolution == null || current.costSoFar() < bestSolution.costSoFar()) {
                    bestSolution = current;
                }
                continue;
            }

            if (limitReached(nodesExplored, startTime)) {
                log.warn("A* search stopped: node or time limit reached");
                break;
            }

            for (Task task : current.unplacedTasks().values()) {
                for (Placement placement : generatePlacements(task, current)) {
                    ScheduleState next = createStateWithPlacement(current, task, placement);
                    frontier.add(next);
                }
            }
        }

        log.info("A* search explored {} nodes", nodesExplored);
        return bestSolution != null ? bestSolution : startScheduleState;
    }
}