package com.datify.scheduler.ui;

import com.datify.scheduler.config.CostConfig;
import com.datify.scheduler.config.SchedulerConfig;
import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.ScheduleState;
import com.datify.scheduler.model.Task;
import com.datify.scheduler.parser.LLMTaskSeeder;
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

    private static final String BACKTRACKING_STRATEGY = "Backtracking";
    private static final String ASTAR_STRATEGY = "A*";

    private static final String SOURCE_GEMINI = "Gemini API";
    private static final String SOURCE_HARDCODED = "Hardcoded";

    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private final Map<UUID, Color> taskColors;

    private JComboBox<String> strategyComboBox;
    private JComboBox<String> taskSourceComboBox;
    private JButton recomputeButton;
    private JLabel statusLabel;

    private ScheduleState initialScheduleState;
    private Map<UUID, Task> originalTasks;

    public ScheduleUI() {
        this.taskColors = new HashMap<>();
        initializeUI();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ScheduleUI ui = new ScheduleUI();
            ui.setVisible(true);
        });
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

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);

        setSize(900, 700); // Slightly taller to fit new dropdown
        setLocationRelativeTo(null);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Strategy selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        controlPanel.add(new JLabel("Planning Strategy:"), gbc);

        gbc.gridx = 1;
        strategyComboBox = new JComboBox<>(new String[]{BACKTRACKING_STRATEGY, ASTAR_STRATEGY});
        strategyComboBox.setSelectedItem(BACKTRACKING_STRATEGY);
        strategyComboBox.setToolTipText("Select planning algorithm");
        controlPanel.add(strategyComboBox, gbc);

        // Recompute button
        gbc.gridx = 2;
        recomputeButton = new JButton("Recompute Schedule");
        recomputeButton.setToolTipText("Recalculate schedule using selected strategy");
        recomputeButton.addActionListener(new RecomputeActionListener());
        recomputeButton.setEnabled(false);
        controlPanel.add(recomputeButton, gbc);

        // Status label
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.BLUE);
        controlPanel.add(statusLabel, gbc);

        // Refresh and Clear buttons
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

        // Task source selection
        gbc.gridx = 0;
        gbc.gridy = 2;
        controlPanel.add(new JLabel("Task Source:"), gbc);

        gbc.gridx = 1;
        taskSourceComboBox = new JComboBox<>(new String[]{SOURCE_GEMINI, SOURCE_HARDCODED});
        taskSourceComboBox.setSelectedItem(SOURCE_GEMINI);
        taskSourceComboBox.setToolTipText("Select task source");
        controlPanel.add(taskSourceComboBox, gbc);

        return controlPanel;
    }

    public void displaySchedule(ScheduleState scheduleState) {
        clearSchedule();
        assignTaskColors(scheduleState.placedTasks().values());

        for (Placement placement : scheduleState.placedTasks().values()) {
            placeTaskInSchedule(placement);
        }

        scheduleTable.repaint();
        statusLabel.setText(String.format("Schedule loaded: %d tasks placed",
                scheduleState.placedTasks().size()));
    }

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
        return Math.min(baseRow, tableModel.getRowCount() - 1);
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

    private class RecomputeActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (originalTasks == null) {
                JOptionPane.showMessageDialog(ScheduleUI.this,
                        "No initial data available for recomputation.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            recomputeButton.setEnabled(false);
            statusLabel.setText("Computing schedule...");
            statusLabel.setForeground(Color.ORANGE);

            SwingWorker<ScheduleState, Void> worker = new SwingWorker<>() {
                @Override
                protected ScheduleState doInBackground() {
                    String selectedStrategy = (String) strategyComboBox.getSelectedItem();
                    IPlanningStrategy strategy = createStrategy(selectedStrategy);

                    SchedulePlanner planner = new SchedulePlanner(strategy);

                    // Task source selection
                    String source = (String) taskSourceComboBox.getSelectedItem();
                    Map<UUID, Task> tasks;
                    if (SOURCE_HARDCODED.equals(source)) {
                        tasks = LLMTaskSeeder.seedHardcodedTasks();
                    } else {
                        String input = """
                            I need a 30-minute morning meeting at 8 AM on Monday or Tuesday.
                            After that, a 60-minute documentation task ideally between 9 AM and 10 AM on Tuesday.
                            Schedule a 90-minute code review at 10 AM on Wednesday.
                            """;
                        tasks = LLMTaskSeeder.seedFromLLM(input);
                    }

                    return planner.beginPlanning(new ScheduleState(
                            new HashMap<>(),
                            new HashMap<>(tasks),
                            0,
                            0
                    ));
                }

                @Override
                protected void done() {
                    try {
                        ScheduleState result = get();
                        if (result != null && !result.placedTasks().isEmpty()) {
                            displaySchedule(result);
                            statusLabel.setText(String.format("Recomputed with %s strategy: %d tasks placed",
                                    strategyComboBox.getSelectedItem(), result.placedTasks().size()));
                            statusLabel.setForeground(Color.BLUE);
                        } else {
                            statusLabel.setText("Recomputation failed - no valid schedule found");
                            statusLabel.setForeground(Color.RED);
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
                case ASTAR_STRATEGY -> new AStarStrategy(schedulerConfig, costConfig);
                case BACKTRACKING_STRATEGY -> new BacktrackingStrategy(schedulerConfig, costConfig);
                default -> new BacktrackingStrategy(schedulerConfig, costConfig);
            };
        }
    }

    private class TaskCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 0) {
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
