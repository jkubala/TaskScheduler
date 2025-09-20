package com.datify.scheduler.model;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;

public class Task {
    private final UUID id;
    private String name;
    private String description;
    private Duration duration;
    private final Set<UUID> dependencyIds;
    private final List<TimeSlot> idealTimeWindows;

    public Task(TaskBuilder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.duration = builder.duration;
        this.dependencyIds = Set.copyOf(builder.dependencyIds);
        this.idealTimeWindows = List.copyOf(builder.idealTimeWindows);
    }

    public String getName() { return name; }
    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be null or blank");
        this.name = name;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description != null ? description : ""; }

    public Duration getDuration() { return duration; }
    public void setDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) throw new IllegalArgumentException("Duration must be positive and non-null");
        this.duration = duration;
    }

    public UUID getId() { return id; }
    public Set<UUID> getDependencyIds() { return dependencyIds; }
    public List<TimeSlot> getIdealTimeWindows() { return idealTimeWindows; }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", duration=" + duration +
                ", dependencyIds=" + dependencyIds +
                ", idealTimeWindows=" + idealTimeWindows +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class TaskBuilder {
        private UUID id;
        private final String name;
        private String description = "";
        private Duration duration;
        private Set<UUID> dependencyIds = Set.of();
        private List<TimeSlot> idealTimeWindows = List.of();

        public TaskBuilder(String name) {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Task name must not be null or blank");
            this.name = name;
        }

        public TaskBuilder description(String description) { this.description = description != null ? description : ""; return this; }
        public TaskBuilder duration(Duration duration) { this.duration = duration; return this; }
        public TaskBuilder dependencyIds(Set<UUID> ids) { this.dependencyIds = ids != null ? Set.copyOf(ids) : Set.of(); return this; }
        public TaskBuilder idealTimeWindows(List<TimeSlot> tw) { this.idealTimeWindows = tw != null ? List.copyOf(tw) : List.of(); return this; }

        public Task build() {
            this.id = UUID.randomUUID();
            if (duration == null) throw new IllegalStateException("Task duration must be set");
            return new Task(this);
        }
    }
}