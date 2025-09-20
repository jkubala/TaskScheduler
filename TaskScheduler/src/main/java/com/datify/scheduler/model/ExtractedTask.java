package com.datify.scheduler.model;

import java.util.List;

public record ExtractedTask(
        String name,
        int durationMinutes,
        String idealStart,
        String idealEnd,
        List<String> days,
        List<String> dependsOn
) {
}