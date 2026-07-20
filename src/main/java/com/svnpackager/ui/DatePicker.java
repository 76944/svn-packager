package com.svnpackager.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class DatePicker extends JPanel {
    private Date selectedDate;
    private JTextField dateField;
    private JButton calendarButton;
    private List<ActionListener> listeners = new ArrayList<>();

    public DatePicker() {
        this(new Date());
    }

    public DatePicker(Date initialDate) {
        this.selectedDate = initialDate;
        setLayout(new BorderLayout(0, 0));
        setPreferredSize(new Dimension(140, 26));

        dateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(initialDate));
        dateField.setEditable(false);
        dateField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dateField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        dateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 6, 2, 2)));
        dateField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCalendarPopup();
            }
        });
        add(dateField, BorderLayout.CENTER);

        calendarButton = new JButton("\u25BC");
        calendarButton.setFont(new Font("Dialog", Font.PLAIN, 10));
        calendarButton.setMargin(new Insets(0, 0, 0, 0));
        calendarButton.setPreferredSize(new Dimension(22, 26));
        calendarButton.setFocusPainted(false);
        calendarButton.addActionListener(e -> showCalendarPopup());
        add(calendarButton, BorderLayout.EAST);
    }

    public Date getDate() {
        return selectedDate;
    }

    public void setDate(Date date) {
        this.selectedDate = date;
        dateField.setText(new SimpleDateFormat("yyyy-MM-dd").format(date));
    }

    public void addDateChangeListener(ActionListener listener) {
        listeners.add(listener);
    }

    private void fireDateChanged() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dateChanged");
        for (ActionListener l : listeners) {
            l.actionPerformed(event);
        }
    }

    private void showCalendarPopup() {
        JDialog popup = new JDialog((Window) SwingUtilities.getWindowAncestor(this), Dialog.ModalityType.MODELESS);
        popup.setUndecorated(true);
        popup.setFocusableWindowState(true);

        CalendarPanel calendarPanel = new CalendarPanel(selectedDate, date -> {
            selectedDate = date;
            dateField.setText(new SimpleDateFormat("yyyy-MM-dd").format(date));
            popup.dispose();
            fireDateChanged();
        });

        popup.setContentPane(calendarPanel);
        popup.pack();

        Point location = dateField.getLocationOnScreen();
        popup.setLocation(location.x, location.y + dateField.getHeight());

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (popup.getX() + popup.getWidth() > screenSize.width) {
            popup.setLocation(screenSize.width - popup.getWidth(), popup.getY());
        }
        if (popup.getY() + popup.getHeight() > screenSize.height) {
            popup.setLocation(popup.getX(), location.y - popup.getHeight());
        }

        // 等弹窗真正显示并获得焦点后再挂接“失焦即关闭”监听，
        // 避免弹窗刚出现时焦点切换导致它立刻被关闭（modeless 弹窗经典坑）。
        popup.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                popup.addWindowFocusListener(new WindowAdapter() {
                    @Override
                    public void windowLostFocus(WindowEvent e2) {
                        popup.dispose();
                    }
                });
            }
        });

        popup.setVisible(true);
    }

    private static class CalendarPanel extends JPanel {
        private Calendar calendar;
        private Date initialDate;
        private JLabel monthLabel;
        private JPanel daysPanel;
        private java.util.function.Consumer<Date> onDateSelected;

        CalendarPanel(Date date, java.util.function.Consumer<Date> onDateSelected) {
            this.onDateSelected = onDateSelected;
            this.initialDate = date;
            this.calendar = Calendar.getInstance();
            this.calendar.setTime(date);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            setBackground(Color.WHITE);
            buildUI();
        }

        private void buildUI() {
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(51, 122, 183));
            header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

            JButton prevBtn = createNavButton("\u25C0");
            prevBtn.addActionListener(e -> {
                calendar.add(Calendar.MONTH, -1);
                refreshDays();
            });

            JButton nextBtn = createNavButton("\u25B6");
            nextBtn.addActionListener(e -> {
                calendar.add(Calendar.MONTH, 1);
                refreshDays();
            });

            monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setForeground(Color.WHITE);
            monthLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));

            header.add(prevBtn, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(nextBtn, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            JPanel calendarGrid = new JPanel(new BorderLayout(0, 1));
            calendarGrid.setBackground(new Color(220, 220, 220));

            JPanel weekdayPanel = new JPanel(new GridLayout(1, 7, 1, 1));
            weekdayPanel.setBackground(new Color(220, 220, 220));
            weekdayPanel.setPreferredSize(new Dimension(250, 24));
            String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
            for (String day : weekDays) {
                JLabel lbl = new JLabel(day, SwingConstants.CENTER);
                lbl.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
                lbl.setForeground(new Color(51, 122, 183));
                lbl.setBackground(new Color(240, 240, 240));
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
                weekdayPanel.add(lbl);
            }
            calendarGrid.add(weekdayPanel, BorderLayout.NORTH);

            daysPanel = new JPanel(new GridLayout(0, 7, 1, 1));
            daysPanel.setBackground(new Color(220, 220, 220));
            calendarGrid.add(daysPanel, BorderLayout.CENTER);

            add(calendarGrid, BorderLayout.CENTER);

            refreshDays();
        }

        private JButton createNavButton(String text) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Dialog", Font.PLAIN, 12));
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(51, 122, 183));
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(28, 24));
            return btn;
        }

        private void refreshDays() {
            daysPanel.removeAll();

            SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy年MM月");
            monthLabel.setText(monthFmt.format(calendar.getTime()));

            Calendar cal = (Calendar) calendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            Calendar today = Calendar.getInstance();
            Calendar initCal = Calendar.getInstance();
            initCal.setTime(initialDate);

            for (int i = 0; i < firstDayOfWeek; i++) {
                daysPanel.add(new JLabel(""));
            }

            for (int day = 1; day <= maxDay; day++) {
                final int d = day;
                JLabel dayLabel = new JLabel(String.valueOf(day), SwingConstants.CENTER);
                dayLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
                dayLabel.setOpaque(true);
                dayLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                dayLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

                boolean isToday = (today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                        && today.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                        && today.get(Calendar.DAY_OF_MONTH) == day);
                boolean isSelected = (initCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                        && initCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                        && initCal.get(Calendar.DAY_OF_MONTH) == day);

                if (isSelected) {
                    dayLabel.setBackground(new Color(51, 122, 183));
                    dayLabel.setForeground(Color.WHITE);
                } else if (isToday) {
                    dayLabel.setBackground(new Color(217, 237, 247));
                    dayLabel.setForeground(Color.BLACK);
                } else {
                    dayLabel.setBackground(Color.WHITE);
                    dayLabel.setForeground(Color.BLACK);
                }

                dayLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        Calendar selected = (Calendar) calendar.clone();
                        selected.set(Calendar.DAY_OF_MONTH, d);
                        onDateSelected.accept(selected.getTime());
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (!isSelected) {
                            dayLabel.setBackground(new Color(230, 240, 250));
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (isSelected) {
                            dayLabel.setBackground(new Color(51, 122, 183));
                        } else if (isToday) {
                            dayLabel.setBackground(new Color(217, 237, 247));
                        } else {
                            dayLabel.setBackground(Color.WHITE);
                        }
                    }
                });

                daysPanel.add(dayLabel);
            }

            int totalCells = firstDayOfWeek + maxDay;
            int remaining = (7 - (totalCells % 7)) % 7;
            for (int i = 0; i < remaining; i++) {
                daysPanel.add(new JLabel(""));
            }

            daysPanel.revalidate();
            daysPanel.repaint();

            int dayRows = (int) Math.ceil(totalCells / 7.0);
            daysPanel.setPreferredSize(new Dimension(250, dayRows * 30));
        }
    }
}
