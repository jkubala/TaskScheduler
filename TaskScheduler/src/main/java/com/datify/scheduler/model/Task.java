package com.datify.scheduler.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@ToString
public class Task {
    private final UUID id;
    private String name;
    private String description;
    private Duration duration;
    private final Set<UUID> dependencyIds;
    private final List<TimeSlot> idealTimeWindows;

    private Task(TaskBuilder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.duration = builder.duration;
        this.dependencyIds = Set.copyOf(builder.dependencyIds);
        this.idealTimeWindows = List.copyOf(builder.idealTimeWindows);
    }

    public static class TaskBuilder {
        private UUID id;
        private final String name;
        private String description;
        private Duration duration;
        private Set<UUID> dependencyIds = Set.of();
        private List<TimeSlot> idealTimeWindows = List.of();

        public TaskBuilder(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Task name must not be null or blank");
            }
            this.name = name;
        }

        public TaskBuilder description(String description)
        {
            this.description = description;
            return this;
        }

        public TaskBuilder duration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public TaskBuilder dependencyIds(Set<UUID> ids) {
            this.dependencyIds = ids;
            return this;
        }

        public TaskBuilder idealTimeWindows(List<TimeSlot> tw) {
            this.idealTimeWindows = tw;
            return this;
        }

        public Task build() {
            id = UUID.randomUUID();
            return new Task(this);
        }
    }
}
