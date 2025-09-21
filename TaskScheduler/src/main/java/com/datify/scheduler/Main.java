package com.datify.scheduler;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.State;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.parser.LLMTaskSeeder;
import com.datify.scheduler.planner.SchedulePlanner;
import com.datify.scheduler.planner.strategy.AStarStrategy;
import com.datify.scheduler.ui.ScheduleUI;
import com.datify.scheduler.util.TaskValidator;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.*;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("Scheduler starting...");

        Map<UUID, Task> validTasks = seedData();
        if (validTasks.isEmpty()) {
            return;
        }
        log.info("Seeded {} tasks", validTasks.size());
        State startState = new State(
                new HashMap<>(),
                new HashMap<>(validTasks),
                0,
                0
        );

        // Backtracking
        SchedulePlanner schedulePlanner = new SchedulePlanner();
        // AStar
        //AStarStrategy aStarStrategy = new AStarStrategy(SchedulerConfig.defaultConfig(), CostConfig.defaultConfig());
        //schedulePlanner = new SchedulePlanner(aStarStrategy);
        State bestStateFound = schedulePlanner.beginPlanning(startState);

        if (bestStateFound == null || bestStateFound.placedTasks().isEmpty()) {
            log.warn("No valid schedule found");
            return;
        } else {
            log.info("Successfully created schedule with {} tasks",
                    bestStateFound.placedTasks().size());
        }

        // Display UI (made with Claude)
        SwingUtilities.invokeLater(() -> {
            log.info("Creating UI on EDT...");
            try {
                ScheduleUI ui = new ScheduleUI();
                ui.setVisible(true);
                log.info("UI window created and set visible");
                ui.displaySchedule(bestStateFound);
                log.info("Schedule data loaded into UI");
            } catch (Exception e) {
                log.error("Error creating UI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static Map<UUID, Task> seedData() {
        log.debug("Creating seed data...");

        String input = """
        I need a 30-minute morning meeting at 8 AM on Monday or Tuesday.
        After that, a 60-minute documentation task ideally between 9 AM and 10 AM on Tuesday
        Schedule a 90-minute code review at 10 AM on Wednesday.
        """;
        Map<UUID, Task> tasks = LLMTaskSeeder.seedFromLLM(input);

        if (TaskValidator.hasCircularDependencies(tasks)) {
            log.error("Circular dependencies detected!");
            return Collections.emptyMap();
        }

        if (TaskValidator.hasInvalidIdealTimeWindows(tasks)) {
            log.error("Some tasks are bigger than their ideal time windows!");
        }

        log.info("Seeded and validated {} tasks from LLM", tasks.size());
        return tasks;
    }
}