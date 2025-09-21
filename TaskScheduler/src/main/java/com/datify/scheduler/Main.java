package com.datify.scheduler;

import com.datify.scheduler.model.ScheduleState;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.parser.LLMTaskSeeder;
import com.datify.scheduler.planner.SchedulePlanner;
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

        ScheduleState startScheduleState = new ScheduleState(
                new HashMap<>(),
                new HashMap<>(validTasks),
                0,
                0
        );

        // Default to Backtracking strategy for initial computation
        SchedulePlanner schedulePlanner = new SchedulePlanner();
        ScheduleState bestScheduleStateFound = schedulePlanner.beginPlanning(startScheduleState);

        if (bestScheduleStateFound == null || bestScheduleStateFound.placedTasks().isEmpty()) {
            log.warn("No valid schedule found with default strategy");
        } else {
            log.info("Successfully created initial schedule with {} tasks",
                    bestScheduleStateFound.placedTasks().size());
        }

        // Display UI with strategy selection capability
        SwingUtilities.invokeLater(() -> {
            log.info("Creating UI on EDT...");
            try {
                ScheduleUI ui = new ScheduleUI();
                ui.setVisible(true);
                log.info("UI window created and set visible");

                // Set initial data for recomputation functionality
                ui.setInitialData(startScheduleState, validTasks);

                // Display initial schedule (even if empty)
                if (bestScheduleStateFound != null) {
                    ui.displaySchedule(bestScheduleStateFound);
                    log.info("Initial schedule data loaded into UI");
                } else {
                    ui.displaySchedule(startScheduleState); // Display empty schedule
                    log.info("Empty schedule displayed - use recompute to try different strategies");
                }
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