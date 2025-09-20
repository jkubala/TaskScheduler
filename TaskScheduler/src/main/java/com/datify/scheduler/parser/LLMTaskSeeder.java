package com.datify.scheduler.parser;

import com.datify.scheduler.model.ExtractedTask;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.model.TimeSlot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Whole class made by Claude
 */
@Slf4j
public class LLMTaskSeeder {
    private static final String GEMINI_API_KEY = System.getenv("GOOGLE_API_KEY");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_SECONDS = 5;

    public static Map<UUID, Task> seedFromLLM(String inputText) {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isBlank()) {
            log.error("GOOGLE_API_KEY environment variable not set. Falling back to hardcoded tasks.");
            return seedHardcodedTasks();
        }

        Client client = new Client();

        String prompt = """
                Extract all tasks from this description. For each task, create a structured JSON array entry with:
                - name: Task name (string)
                - durationMinutes: Duration in minutes (int)
                - idealStart: Start time like "08:00" (string)
                - idealEnd: End time like "09:00" (string)
                - days: Array of days like ["MONDAY", "TUESDAY"] (array of strings)
                - dependsOn: Array of dependency task names (array of strings, empty if none)
                Return ONLY the JSON array, no other text. Example:
                [
                  {"name": "Morning Meeting", "durationMinutes": 30, "idealStart": "08:00", "idealEnd": "09:00", "days": ["MONDAY"], "dependsOn": []}
                ]
                Description: %s
                """.formatted(inputText);

        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                GenerateContentResponse response = client.models.generateContent("gemini-1.5-flash", prompt, null);
                String content = response.text();
                log.info("Gemini Response: {}", content);

                // Clean up any unwanted formatting
                content = content.trim();
                if (content.startsWith("```json") && content.endsWith("```")) {
                    content = content.substring(7, content.length() - 3).trim();
                }

                ExtractedTask[] extractedTasks = OBJECT_MAPPER.readValue(content, ExtractedTask[].class);

                Map<UUID, Task> tasks = new HashMap<>();
                Map<String, UUID> nameToId = new HashMap<>();

                for (ExtractedTask et : extractedTasks) {
                    List<TimeSlot> idealWindows = et.days().stream()
                            .map(dayStr -> {
                                DayOfWeek day = DayOfWeek.valueOf(dayStr.toUpperCase());
                                LocalTime start = LocalTime.parse(et.idealStart());
                                LocalTime end = LocalTime.parse(et.idealEnd());
                                return new TimeSlot(start, end, day);
                            })
                            .collect(Collectors.toList());

                    Set<UUID> depIds = new HashSet<>();
                    for (String depName : et.dependsOn()) {
                        if (nameToId.containsKey(depName)) {
                            depIds.add(nameToId.get(depName));
                        } else {
                            log.warn("Dependency '{}' not found for task '{}'", depName, et.name());
                        }
                    }

                    Task task = new Task.TaskBuilder(et.name())
                            .duration(Duration.ofMinutes(et.durationMinutes()))
                            .idealTimeWindows(idealWindows)
                            .dependencyIds(depIds)
                            .build();

                    UUID id = task.getId();
                    tasks.put(id, task);
                    nameToId.put(et.name(), id);
                }

                log.info("Seeded {} tasks from Gemini", tasks.size());
                return tasks;

            } catch (Exception e) {
                if ((e.getMessage().contains("429") || e.getMessage().contains("quotaExceeded")) && retries < MAX_RETRIES - 1) {
                    long backoffSeconds = INITIAL_BACKOFF_SECONDS * (1 << retries);
                    log.warn("Rate limit/quota hit. Retrying in {} seconds... (Attempt {}/{})", backoffSeconds, retries + 1, MAX_RETRIES);
                    try {
                        TimeUnit.SECONDS.sleep(backoffSeconds);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Sleep interrupted", ie);
                    }
                    retries++;
                } else {
                    log.error("Failed to seed from Gemini after {} retries: {}", retries, e.getMessage(), e);
                    return seedHardcodedTasks();
                }
            }
        }

        log.error("Max retries reached for Gemini seeding. Falling back to hardcoded tasks.");
        return seedHardcodedTasks();
    }

    private static Map<UUID, Task> seedHardcodedTasks() {
        log.debug("Creating seed data from hardcoded fallback...");

        Map<UUID, Task> tasks = new HashMap<>();

        List<TimeSlot> twMondayMorning = List.of(
                new TimeSlot(LocalTime.of(8, 0), LocalTime.of(9, 0), DayOfWeek.MONDAY)
        );
        List<TimeSlot> twTuesdayMorning = List.of(
                new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0), DayOfWeek.TUESDAY)
        );
        List<TimeSlot> twWednesdayMidMorning = List.of(
                new TimeSlot(LocalTime.of(10, 0), LocalTime.of(11, 0), DayOfWeek.WEDNESDAY)
        );
        List<TimeSlot> twThursdayLateMorning = List.of(
                new TimeSlot(LocalTime.of(8, 0), LocalTime.of(9, 0), DayOfWeek.THURSDAY)
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

        // Create 10 hardcoded tasks
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
                .duration(Duration.ofMinutes(90))
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
                .duration(Duration.ofMinutes(120))
                .idealTimeWindows(twFridayAfternoon)
                .dependencyIds(Set.of(t2.getId()))
                .build();

        Task t6 = new Task.TaskBuilder("Frontend Implementation")
                .description("Implement UI screens")
                .duration(Duration.ofMinutes(120))
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
                .duration(Duration.ofMinutes(90))
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

        List<Task> allTasks = List.of(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
        for (Task task : allTasks) {
            tasks.put(task.getId(), task);
        }

        log.debug("Created {} tasks from hardcoded fallback", tasks.size());
        return tasks;
    }
}