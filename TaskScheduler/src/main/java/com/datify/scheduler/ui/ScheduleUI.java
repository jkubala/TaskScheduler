package com.datify.scheduler.ui;

import com.datify.scheduler.model.Placement;
import com.datify.scheduler.model.State;
import com.datify.scheduler.model.Task;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Whole class made by Claude
 */
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

    private JTable scheduleTable;
    private DefaultTableModel tableModel;
    private Map<UUID, Color> taskColors;

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

        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Refresh Schedule");
        refreshButton.addActionListener(e -> refreshDisplay());
        controlPanel.add(refreshButton);

        JButton clearButton = new JButton("Clear Schedule");
        clearButton.addActionListener(e -> clearSchedule());
        controlPanel.add(clearButton);

        add(controlPanel, BorderLayout.SOUTH);

        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    public void displaySchedule(State state) {
        clearSchedule();
        assignTaskColors(state.placedTasks().values());

        for (Placement placement : state.placedTasks().values()) {
            placeTaskInSchedule(placement);
        }

        scheduleTable.repaint();
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
        Task task = placement.task();
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
