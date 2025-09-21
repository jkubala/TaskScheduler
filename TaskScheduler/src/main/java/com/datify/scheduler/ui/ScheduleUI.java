package com.datify.scheduler.ui;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.ScheduleState;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.planner.SchedulePlanner;
import com.datify.scheduler.planner.strategy.AStarStrategy;
import com.datify.scheduler.planner.strategy.BacktrackingStrategy;
import com.datify.scheduler.planner.strategy.IPlanningStrategy;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Whole class made by Claude
 */
@Slf4j
public class ScheduleUI extends JFrame {
    private static final String[] COLUMN_NAMES = {
            "Time", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private static final Color[] TASK_COLORS = {
            new Color(173, 216, 230),
            new Color(144, 238, 144),
            new Color(255, 182, 193),
            new Color(255, 218, 185),
            new Color(221, 160, 221),
            new Color(255, 255, 224),
            new Color(175, 238, 238)
    };

    // Strategy selection options
    private static final String BACKTRACKING_STRATEGY = "Backtracking";
    private static final String ASTAR_STRATEGY = "A*";

    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private final Map<UUID, Color> taskColors;

    // New components for strategy selection
    private JComboBox<String> strategyComboBox;
    private JButton recomputeButton;
    private JLabel statusLabel;

    // Store initial state and tasks for recomputation
    private ScheduleState initialScheduleState;
    private Map<UUID, Task> originalTasks;

    public ScheduleUI() {
        this.taskColors = new HashMap<>();
        initializeUI();
    }

    public static void main(String[] args) {
        ScheduleUI ui = new ScheduleUI();
        ui.setVisible(true);
    }

    private void initializeUI() {
        setTitle("DATIFY Schedule Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (int hour = 8; hour <= 20; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String timeSlot = String.format("%02d:%02d", hour, minute);
                tableModel.addRow(new Object[]{timeSlot, "", "", "", "", "", "", ""});
            }
        }

        scheduleTable = new JTable(tableModel);
        scheduleTable.setRowHeight(25);
        scheduleTable.setGridColor(Color.LIGHT_GRAY);
        scheduleTable.setDefaultRenderer(Object.class, new TaskCellRenderer());

        scheduleTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        for (int i = 1; i < COLUMN_NAMES.length; i++) {
            scheduleTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        JScrollPane scrollPane = new JScrollPane(scheduleTable);
        add(scrollPane, BorderLayout.CENTER);

        // Create control panel with strategy selection
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);

        setSize(900, 650); // Slightly taller to accommodate new controls
        setLocationRelativeTo(null);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Strategy selection row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel strategyLabel = new JLabel("Planning Strategy:");
        controlPanel.add(strategyLabel, gbc);

        gbc.gridx = 1;
        strategyComboBox = new JComboBox<>(new String[]{BACKTRACKING_STRATEGY, ASTAR_STRATEGY});
        strategyComboBox.setSelectedItem(BACKTRACKING_STRATEGY); // Default to backtracking
        strategyComboBox.setToolTipText("Select the planning algorithm to use");
        controlPanel.add(strategyComboBox, gbc);

        // Recompute button
        gbc.gridx = 2;
        recomputeButton = new JButton("Recompute Schedule");
        recomputeButton.setToolTipText("Recalculate schedule using selected strategy");
        recomputeButton.addActionListener(new RecomputeActionListener());
        recomputeButton.setEnabled(false); // Initially disabled until we have data
        controlPanel.add(recomputeButton, gbc);

        // Status label
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.BLUE);
        controlPanel.add(statusLabel, gbc);

        // Second row with existing buttons
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JButton refreshButton = new JButton("Refresh Display");
        refreshButton.addActionListener(e -> refreshDisplay());
        controlPanel.add(refreshButton, gbc);

        gbc.gridx = 1;
        JButton clearButton = new JButton("Clear Schedule");
        clearButton.addActionListener(e -> clearSchedule());
        controlPanel.add(clearButton, gbc);

        return controlPanel;
    }

    public void displaySchedule(ScheduleState scheduleState) {
        clearSchedule();
        assignTaskColors(scheduleState.placedTasks().values());

        for (Placement placement : scheduleState.placedTasks().values()) {
            placeTaskInSchedule(placement);
        }

        scheduleTable.repaint();

        // Update status
        statusLabel.setText(String.format("Schedule loaded: %d tasks placed",
                scheduleState.placedTasks().size()));
    }

    /**
     * Set the initial state and tasks to enable recomputation
     */
    public void setInitialData(ScheduleState initialScheduleState, Map<UUID, Task> originalTasks) {
        this.initialScheduleState = initialScheduleState;
        this.originalTasks = new HashMap<>(originalTasks);
        recomputeButton.setEnabled(true);
        log.info("Initial data set for recomputation: {} tasks", originalTasks.size());
    }

    private void assignTaskColors(Iterable<Placement> placements) {
        int colorIndex = 0;
        for (Placement placement : placements) {
            UUID id = placement.task().getId();
            if (!taskColors.containsKey(id)) {
                taskColors.put(id, TASK_COLORS[colorIndex % TASK_COLORS.length]);
                colorIndex++;
            }
        }
    }

    private void placeTaskInSchedule(Placement placement) {
        int dayColumn = getDayColumn(placement.timeSlot().dayOfWeek());
        if (dayColumn == -1) return;

        LocalTime startTime = placement.timeSlot().start();
        LocalTime endTime = placement.timeSlot().end();

        int startRow = getRowForTime(startTime);
        int endRow = getRowForTime(endTime);

        if (startRow == -1 || endRow == -1) return;

        for (int row = startRow; row < endRow; row++) {
            tableModel.setValueAt(placement, row, dayColumn);
        }
    }

    private int getDayColumn(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
            case SUNDAY -> 7;
        };
    }

    private int getRowForTime(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();

        if (hour < 8 || hour > 20) return -1;

        int baseRow = (hour - 8) * 2;
        if (minute >= 30) baseRow++;

        if (baseRow >= tableModel.getRowCount()) return tableModel.getRowCount() - 1;

        return baseRow;
    }

    private void clearSchedule() {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            for (int col = 1; col < tableModel.getColumnCount(); col++) {
                tableModel.setValueAt("", row, col);
            }
        }
        taskColors.clear();
    }

    private void refreshDisplay() {
        scheduleTable.repaint();
    }

    /**
     * Action listener for the recompute button
     */
    private class RecomputeActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (initialScheduleState == null || originalTasks == null) {
                JOptionPane.showMessageDialog(ScheduleUI.this,
                        "No initial data available for recomputation.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Show progress and disable button
            recomputeButton.setEnabled(false);
            statusLabel.setText("Computing schedule...");
            statusLabel.setForeground(Color.ORANGE);

            // Create worker thread to avoid blocking UI
            SwingWorker<ScheduleState, Void> worker = new SwingWorker<>() {
                @Override
                protected ScheduleState doInBackground() {
                    String selectedStrategy = (String) strategyComboBox.getSelectedItem();
                    IPlanningStrategy strategy = createStrategy(selectedStrategy);

                    SchedulePlanner planner = new SchedulePlanner(strategy);

                    // Create fresh initial state
                    ScheduleState freshInitialState = new ScheduleState(
                            new HashMap<>(),
                            new HashMap<>(originalTasks),
                            0,
                            0
                    );

                    log.info("Starting recomputation with {} strategy", selectedStrategy);
                    return planner.beginPlanning(freshInitialState);
                }

                @Override
                protected void done() {
                    try {
                        ScheduleState result = get();
                        if (result != null && !result.placedTasks().isEmpty()) {
                            displaySchedule(result);
                            statusLabel.setText(String.format("Recomputed with %s: %d tasks placed",
                                    strategyComboBox.getSelectedItem(),
                                    result.placedTasks().size()));
                            statusLabel.setForeground(Color.BLUE);
                            log.info("Recomputation successful: {} tasks placed", result.placedTasks().size());
                        } else {
                            statusLabel.setText("Recomputation failed - no valid schedule found");
                            statusLabel.setForeground(Color.RED);
                            log.warn("Recomputation failed");
                        }
                    } catch (Exception ex) {
                        statusLabel.setText("Error during recomputation: " + ex.getMessage());
                        statusLabel.setForeground(Color.RED);
                        log.error("Error during recomputation", ex);
                    } finally {
                        recomputeButton.setEnabled(true);
                    }
                }
            };

            worker.execute();
        }

        private IPlanningStrategy createStrategy(String strategyName) {
            SchedulerConfig schedulerConfig = SchedulerConfig.defaultConfig();
            CostConfig costConfig = CostConfig.defaultConfig();

            return switch (strategyName) {
                case ASTAR_STRATEGY -> {
                    log.info("Creating A* strategy");
                    yield new AStarStrategy(schedulerConfig, costConfig);
                }
                case BACKTRACKING_STRATEGY -> {
                    log.info("Creating Backtracking strategy");
                    yield new BacktrackingStrategy(schedulerConfig, costConfig);
                }
                default -> {
                    log.warn("Unknown strategy: {}, defaulting to Backtracking", strategyName);
                    yield new BacktrackingStrategy(schedulerConfig, costConfig);
                }
            };
        }
    }

    private class TaskCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 0) {
                // Time column
                component.setBackground(Color.WHITE);
                component.setForeground(Color.BLACK);
                setHorizontalAlignment(SwingConstants.CENTER);
            } else if (value instanceof Placement placement) {
                UUID id = placement.task().getId();
                component.setBackground(taskColors.getOrDefault(id, Color.LIGHT_GRAY));
                component.setForeground(Color.BLACK);
                setText(placement.task().getName());
                setHorizontalAlignment(SwingConstants.LEFT);
            } else {
                component.setBackground(Color.WHITE);
                component.setForeground(Color.BLACK);
                setText("");
            }

            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            return component;
        }
    }
}