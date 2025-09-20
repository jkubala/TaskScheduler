package com.datify.scheduler;

import com.datify.scheduler.model.*;
import com.datify.scheduler.planner.SchedulePlanner;
import com.datify.scheduler.ui.ScheduleUI;

import javax.swing.SwingUtilities;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Map<UUID, Task> validTaskIds = seedData();

        State startState = new State(
                new ArrayList<>(),
                new HashMap<>(validTaskIds),
                validTaskIds,
                0,
                0
        );

        SchedulePlanner schedulePlanner = new SchedulePlanner();
        State bestStateFound = schedulePlanner.beginPlanning(startState);

        if (bestStateFound == null || bestStateFound.placedTasks().isEmpty()) {
            System.out.println("No valid schedule found");
            return;
        } else {
            System.out.println("Number of placed tasks: " + bestStateFound.placedTasks().size());
        }

        // Display UI (made with Claude)
        SwingUtilities.invokeLater(() -> {
            System.out.println("Creating UI on EDT...");
            try {
                ScheduleUI ui = new ScheduleUI();
                ui.setVisible(true);
                System.out.println("UI window created and set visible");
                ui.displaySchedule(bestStateFound);
                System.out.println("Schedule data loaded into UI");
            } catch (Exception e) {
                System.err.println("Error creating UI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private static Map<UUID, Task> seedData() {
        List<TimeSlot> morningMeetingIdealTW = new ArrayList<>();
        List<TimeSlot> codeReviewIdealTW = new ArrayList<>();
        List<TimeSlot> documentationIdealTW = new ArrayList<>();
        morningMeetingIdealTW.add(new TimeSlot(
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                DayOfWeek.MONDAY
        ));
        morningMeetingIdealTW.add(new TimeSlot(
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                DayOfWeek.MONDAY
        ));
        codeReviewIdealTW.add(new TimeSlot(
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                DayOfWeek.MONDAY
        ));
        codeReviewIdealTW.add(new TimeSlot(
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                DayOfWeek.MONDAY
        ));
        documentationIdealTW.add(new TimeSlot(
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                DayOfWeek.MONDAY
        ));
        documentationIdealTW.add(new TimeSlot(
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                DayOfWeek.MONDAY
        ));

        Task morningMeeting = new Task.TaskBuilder("Morning Meeting")
                .description("Team standup meeting")
                .idealTimeWindows(morningMeetingIdealTW)
                .duration(Duration.ofMinutes(30))
                .build();


        Task documentation = new Task.TaskBuilder("Documentation")
                .description("Update project documentation")
                .duration(Duration.ofMinutes(90))
                .dependencyIds(Set.of(morningMeeting.getId()))
                .idealTimeWindows(documentationIdealTW)
                .build();

        Task codeReview = new Task.TaskBuilder("Code Review")
                .description("Review pull requests")
                .duration(Duration.ofHours(1))
                .idealTimeWindows(codeReviewIdealTW)
                .dependencyIds(Set.of(documentation.getId()))
                .build();

        // TODO check for circular dependencies
        Map<UUID, Task> validTaskIds = new HashMap<>();
        validTaskIds.put(morningMeeting.getId(), morningMeeting);
        validTaskIds.put(codeReview.getId(), codeReview);
        validTaskIds.put(documentation.getId(), documentation);
        return validTaskIds;
    }
}