package com.datify.scheduler;

import com.datify.scheduler.model.State;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.model.TimeSlot;
import com.datify.scheduler.planner.SchedulePlanner;
import com.datify.scheduler.ui.ScheduleUI;
import com.datify.scheduler.util.TaskValidator;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
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

        SchedulePlanner schedulePlanner = new SchedulePlanner();
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

        Map<UUID, Task> tasks = new HashMap<>();

        List<TimeSlot> twMondayMorning = List.of(
                new TimeSlot(LocalTime.of(8, 0), LocalTime.of(9, 0), DayOfWeek.MONDAY)
        );
        List<TimeSlot> twTuesdayMorning = List.of(
                new TimeSlot(LocalTime.of(8, 0), LocalTime.of(9, 0), DayOfWeek.TUESDAY)
        );
        List<TimeSlot> twWednesdayMidMorning = List.of(
                new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0), DayOfWeek.WEDNESDAY)
        );
        List<TimeSlot> twThursdayLateMorning = List.of(
                new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0), DayOfWeek.THURSDAY)
        );
        List<TimeSlot> twFridayAfternoon = List.of(
                new TimeSlot(LocalTime.of(14, 0), LocalTime.of(15, 0), DayOfWeek.FRIDAY)
        );
        List<TimeSlot> twMondayAfternoon = List.of(
                new TimeSlot(LocalTime.of(14, 0), LocalTime.of(15, 0), DayOfWeek.MONDAY)
        );
        List<TimeSlot> twTuesdayLateAfternoon = List.of(
                new TimeSlot(LocalTime.of(16, 0), LocalTime.of(17, 0), DayOfWeek.TUESDAY)
        );
        List<TimeSlot> twWednesdayAfternoon = List.of(
                new TimeSlot(LocalTime.of(15, 0), LocalTime.of(16, 0), DayOfWeek.WEDNESDAY)
        );
        List<TimeSlot> twThursdayAfternoon = List.of(
                new TimeSlot(LocalTime.of(15, 0), LocalTime.of(16, 0), DayOfWeek.THURSDAY)
        );
        List<TimeSlot> twFridayLateAfternoon = List.of(
                new TimeSlot(LocalTime.of(16, 0), LocalTime.of(17, 0), DayOfWeek.FRIDAY)
        );

        // Create 10 tasks spread across the week
        Task t1 = new Task.TaskBuilder("Morning Meeting")
                .description("Team standup meeting")
                .duration(Duration.ofMinutes(30))
                .idealTimeWindows(twMondayMorning)
                .build();

        Task t2 = new Task.TaskBuilder("Documentation")
                .description("Update project documentation")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twTuesdayMorning)
                .dependencyIds(Set.of(t1.getId()))
                .build();

        Task t3 = new Task.TaskBuilder("Code Review")
                .description("Review pull requests")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twWednesdayMidMorning)
                .build();

        Task t4 = new Task.TaskBuilder("Design Meeting")
                .description("Discuss UI/UX design")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twThursdayLateMorning)
                .dependencyIds(Set.of(t1.getId()))
                .build();

        Task t5 = new Task.TaskBuilder("Backend Implementation")
                .description("Implement API endpoints")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twFridayAfternoon)
                .dependencyIds(Set.of(t2.getId()))
                .build();

        Task t6 = new Task.TaskBuilder("Frontend Implementation")
                .description("Implement UI screens")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twMondayAfternoon)
                .dependencyIds(Set.of(t2.getId()))
                .build();

        Task t7 = new Task.TaskBuilder("Unit Testing")
                .description("Write unit tests")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twTuesdayLateAfternoon)
                .dependencyIds(Set.of(t5.getId(), t6.getId()))
                .build();

        Task t8 = new Task.TaskBuilder("Integration Testing")
                .description("Integration tests for API and frontend")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twWednesdayAfternoon)
                .dependencyIds(Set.of(t7.getId()))
                .build();

        Task t9 = new Task.TaskBuilder("Deployment to Staging")
                .description("Deploy application to staging environment")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twThursdayAfternoon)
                .dependencyIds(Set.of(t8.getId()))
                .build();

        Task t10 = new Task.TaskBuilder("Client Demo")
                .description("Demo to client")
                .duration(Duration.ofMinutes(60))
                .idealTimeWindows(twFridayLateAfternoon)
                .dependencyIds(Set.of(t9.getId()))
                .build();

        // Add all tasks to map
        List<Task> allTasks = List.of(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);

        for (Task task : allTasks) {
            tasks.put(task.getId(), task);
        }

        // Check for circular dependencies
        if (TaskValidator.hasCircularDependencies(tasks)) {
            log.error("Circular dependencies detected!");
            return Collections.emptyMap();
        }

        if (TaskValidator.hasInvalidIdealTimeWindows(tasks)) {
            log.error("Some tasks are bigger than their ideal time windows!");
            return Collections.emptyMap();
        }

        log.debug("Created {} tasks", tasks.size());
        return tasks;
    }
}