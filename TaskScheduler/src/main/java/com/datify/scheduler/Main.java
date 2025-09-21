package com.datify.scheduler;

import com.datify.scheduler.ui.ScheduleUI;

import javax.swing.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.info("Starting Scheduler GUI...");
        SwingUtilities.invokeLater(() -> {
            try {
                ScheduleUI ui = new ScheduleUI();
                ui.setVisible(true);
                log.info("Scheduler GUI is now visible");
            } catch (Exception e) {
                log.error("Failed to start Scheduler GUI", e);
            }
        });
    }
}
