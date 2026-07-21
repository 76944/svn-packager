package com.svnpackager.ui;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.Chunk;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class DiffPreviewDialog extends JDialog {
    private JTable leftTable;
    private JTable rightTable;
    private DefaultTableModel leftModel;
    private DefaultTableModel rightModel;
    private JLabel statusLabel;
    private long minRevision;
    private long maxRevision;
    private volatile boolean syncing = false;

    private static final Color CODE_BG = new Color(30, 30, 46);
    private static final Color CODE_TEXT = new Color(192, 192, 192);
    private static final Color LINE_NUM_COLOR = new Color(128, 128, 144);
    private static final Color GUTTER_BG = new Color(24, 24, 38);
    private static final Color ADDED_BG = new Color(26, 58, 42);
    private static final Color ADDED_TEXT = new Color(192, 192, 192);
    private static final Color REMOVED_BG = new Color(92, 32, 32);
    private static final Color REMOVED_TEXT = new Color(192, 192, 192);
    private static final Color ADDED_MARKER_COLOR = new Color(64, 192, 64);
    private static final Color REMOVED_MARKER_COLOR = new Color(224, 64, 64);
    private static final Color SEPARATOR_COLOR = new Color(80, 80, 100);
    private static final Color GRID_COLOR = new Color(50, 50, 70);
    private static final int CONTEXT_LINES = 3;
    private static final Font CODE_FONT = new Font("Monospaced", Font.PLAIN, 13);
    private static final int LINE_NUM_WIDTH = 60;
    private static final int MARKER_WIDTH = 24;
    private static final String[] COLUMN_NAMES = {"", "", ""};

    private boolean maximized = false;
    private Rectangle normalBounds;

    public DiffPreviewDialog(Window owner, String title, String beforeContent,
                             String afterContent, long minRevision, long maxRevision) {
        super(owner, title, Dialog.ModalityType.MODELESS);
        this.minRevision = minRevision;
        this.maxRevision = maxRevision;
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 500));
        setLocationRelativeTo(owner);
        initUI();
        showDiff(beforeContent, afterContent);
    }

    private void initUI() {
        JPanel contentPanel = new JPanel(new BorderLayout(0, 0));
        contentPanel.setBackground(CODE_BG);
        setContentPane(contentPanel);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 3));
        topBar.setBackground(CODE_BG);
        JButton btnMaximize = new JButton("最大化");
        btnMaximize.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        btnMaximize.setBackground(new Color(50, 50, 70));
        btnMaximize.setForeground(LINE_NUM_COLOR);
        btnMaximize.setFocusPainted(false);
        btnMaximize.setBorderPainted(false);
        btnMaximize.addActionListener(e -> toggleMaximize(btnMaximize));
        topBar.add(btnMaximize);
        contentPanel.add(topBar, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 1, 0));

        leftModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        leftTable = createDiffTable(leftModel);
        JScrollPane leftScroll = new JScrollPane(leftTable);
        leftScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, GRID_COLOR));
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(wrapWithPanel(leftScroll));

        rightModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        rightTable = createDiffTable(rightModel);
        JScrollPane rightScroll = new JScrollPane(rightTable);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        centerPanel.add(wrapWithPanel(rightScroll));

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(CODE_BG);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 3, 5));
        statusLabel = new JLabel("就绪");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        statusLabel.setForeground(LINE_NUM_COLOR);
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        add(bottomPanel, BorderLayout.SOUTH);

        setupSyncScroll();
    }

    private JPanel wrapWithPanel(JScrollPane scroll) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JTable createDiffTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(CODE_FONT);
        table.setRowHeight(20);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(CODE_BG);
        table.setForeground(CODE_TEXT);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(true);
        table.setSelectionBackground(new Color(50, 70, 100));
        table.setSelectionForeground(Color.WHITE);
        table.setTableHeader(null);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFocusable(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(LINE_NUM_WIDTH);
        table.getColumnModel().getColumn(0).setMaxWidth(LINE_NUM_WIDTH);
        table.getColumnModel().getColumn(0).setMinWidth(LINE_NUM_WIDTH);
        table.getColumnModel().getColumn(1).setPreferredWidth(MARKER_WIDTH);
        table.getColumnModel().getColumn(1).setMaxWidth(MARKER_WIDTH);
        table.getColumnModel().getColumn(1).setMinWidth(MARKER_WIDTH);
        table.getColumnModel().getColumn(2).setPreferredWidth(800);

        table.getColumnModel().getColumn(0).setCellRenderer(new LineNumberRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new MarkerRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new CodeRenderer());

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    copySelectedCell(table);
                }
            }
        });

        return table;
    }

    private void copySelectedCell(JTable table) {
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if (row >= 0 && col >= 0) {
            Object value = table.getValueAt(row, col);
            if (value != null) {
                StringSelection selection = new StringSelection(value.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            }
        }
    }

    private void toggleMaximize(JButton btn) {
        if (maximized) {
            setBounds(normalBounds);
            btn.setText("最大化");
            maximized = false;
        } else {
            normalBounds = getBounds();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle screenBounds = ge.getMaximumWindowBounds();
            setBounds(screenBounds);
            btn.setText("还原");
            maximized = true;
        }
    }

    private class LineNumberRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setForeground(LINE_NUM_COLOR);
            label.setBackground(GUTTER_BG);
            label.setFont(CODE_FONT);
            label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 6));
            return label;
        }
    }

    private class MarkerRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(CODE_FONT);
            String text = value != null ? value.toString() : "";
            if (text.equals("-")) {
                label.setForeground(REMOVED_MARKER_COLOR);
                label.setBackground(REMOVED_BG);
            } else if (text.equals("+")) {
                label.setForeground(ADDED_MARKER_COLOR);
                label.setBackground(ADDED_BG);
            } else {
                label.setForeground(LINE_NUM_COLOR);
                label.setBackground(GUTTER_BG);
            }
            return label;
        }
    }

    private class CodeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setFont(CODE_FONT);
            label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            String marker = (String) table.getModel().getValueAt(row, 1);
            if (marker != null && marker.equals("-")) {
                label.setBackground(REMOVED_BG);
                label.setForeground(REMOVED_TEXT);
            } else if (marker != null && marker.equals("+")) {
                label.setBackground(ADDED_BG);
                label.setForeground(ADDED_TEXT);
            } else if (marker != null && marker.equals("~")) {
                label.setForeground(SEPARATOR_COLOR);
                label.setBackground(CODE_BG);
                label.setHorizontalAlignment(CENTER);
            } else {
                label.setBackground(CODE_BG);
                label.setForeground(CODE_TEXT);
                label.setHorizontalAlignment(LEFT);
            }
            return label;
        }
    }

    private void showDiff(String beforeContent, String afterContent) {
        if (beforeContent == null && afterContent == null) {
            addMessageRow(leftModel, "【无法获取文件内容】");
            addMessageRow(rightModel, "【无法获取文件内容】");
            statusLabel.setText("无法获取文件内容");
            statusLabel.setForeground(Color.RED);
            return;
        }

        if (beforeContent == null) {
            addMessageRow(leftModel, "【新增文件 - 无变更前内容】");
            addAllLines(rightModel, afterContent, "added");
            statusLabel.setText("新增文件");
            statusLabel.setForeground(new Color(0, 128, 0));
            return;
        }

        if (afterContent == null) {
            addAllLines(leftModel, beforeContent, "removed");
            addMessageRow(rightModel, "【已删除文件 - 无变更后内容】");
            statusLabel.setText("已删除文件");
            statusLabel.setForeground(Color.RED);
            return;
        }

        if (isBinaryContent(beforeContent) || isBinaryContent(afterContent)) {
            addMessageRow(leftModel, "【二进制文件无法预览】");
            addMessageRow(rightModel, "【二进制文件无法预览】");
            statusLabel.setText("二进制文件");
            statusLabel.setForeground(Color.ORANGE);
            return;
        }

        List<String> beforeLines = splitLines(beforeContent);
        List<String> afterLines = splitLines(afterContent);

        if (beforeLines.equals(afterLines)) {
            addAllLines(leftModel, beforeContent, "default");
            addAllLines(rightModel, afterContent, "default");
            statusLabel.setText("无差异");
            statusLabel.setForeground(Color.GRAY);
            return;
        }

        try {
            Patch<String> patch = DiffUtils.diff(beforeLines, afterLines);
            displayDiff(patch, beforeLines, afterLines);

            int added = 0, removed = 0, changed = 0;
            for (AbstractDelta<String> delta : patch.getDeltas()) {
                if (delta.getType() == DeltaType.INSERT) added += delta.getTarget().getLines().size();
                else if (delta.getType() == DeltaType.DELETE) removed += delta.getSource().getLines().size();
                else if (delta.getType() == DeltaType.CHANGE) {
                    changed += Math.max(delta.getSource().getLines().size(), delta.getTarget().getLines().size());
                }
            }
            statusLabel.setText(String.format("变更: +%d 新增 / -%d 删除 / ~%d 修改", added, removed, changed));
            statusLabel.setForeground(new Color(0, 128, 0));
        } catch (Exception e) {
            addAllLines(leftModel, beforeContent, "default");
            addAllLines(rightModel, afterContent, "default");
            statusLabel.setText("差异计算失败: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
        }
    }

    private void displayDiff(Patch<String> patch, List<String> beforeLines, List<String> afterLines) {
        int leftIdx = 0;
        int rightIdx = 0;
        int leftLineNum = 0;
        int rightLineNum = 0;
        int prevSourceEnd = 0;
        int prevTargetEnd = 0;

        List<AbstractDelta<String>> deltas = patch.getDeltas();

        for (int d = 0; d < deltas.size(); d++) {
            AbstractDelta<String> delta = deltas.get(d);
            Chunk<String> source = delta.getSource();
            Chunk<String> target = delta.getTarget();
            int sourcePos = source.getPosition();
            int targetPos = target.getPosition();

            if (d == 0) {
                int gap = sourcePos;
                if (gap > CONTEXT_LINES) {
                    leftIdx = sourcePos - CONTEXT_LINES;
                    rightIdx = targetPos - CONTEXT_LINES;
                    leftLineNum = leftIdx;
                    rightLineNum = rightIdx;
                    addSeparatorRow(leftModel);
                    addSeparatorRow(rightModel);
                }
                while (leftIdx < sourcePos && rightIdx < targetPos) {
                    leftLineNum++;
                    rightLineNum++;
                    addDiffRow(leftModel, String.valueOf(leftLineNum), "", beforeLines.get(leftIdx));
                    addDiffRow(rightModel, String.valueOf(rightLineNum), "", afterLines.get(rightIdx));
                    leftIdx++;
                    rightIdx++;
                }
            } else {
                int gapSource = sourcePos - prevSourceEnd;

                if (gapSource <= 2 * CONTEXT_LINES) {
                    while (leftIdx < sourcePos) {
                        leftLineNum++;
                        addDiffRow(leftModel, String.valueOf(leftLineNum), "", beforeLines.get(leftIdx));
                        leftIdx++;
                    }
                    while (rightIdx < targetPos) {
                        rightLineNum++;
                        addDiffRow(rightModel, String.valueOf(rightLineNum), "", afterLines.get(rightIdx));
                        rightIdx++;
                    }
                } else {
                    while (leftIdx < prevSourceEnd + CONTEXT_LINES) {
                        leftLineNum++;
                        addDiffRow(leftModel, String.valueOf(leftLineNum), "", beforeLines.get(leftIdx));
                        leftIdx++;
                    }
                    while (rightIdx < prevTargetEnd + CONTEXT_LINES) {
                        rightLineNum++;
                        addDiffRow(rightModel, String.valueOf(rightLineNum), "", afterLines.get(rightIdx));
                        rightIdx++;
                    }
                    addSeparatorRow(leftModel);
                    addSeparatorRow(rightModel);

                    leftIdx = sourcePos - CONTEXT_LINES;
                    rightIdx = targetPos - CONTEXT_LINES;
                    leftLineNum = leftIdx;
                    rightLineNum = rightIdx;
                    while (leftIdx < sourcePos) {
                        leftLineNum++;
                        addDiffRow(leftModel, String.valueOf(leftLineNum), "", beforeLines.get(leftIdx));
                        leftIdx++;
                    }
                    while (rightIdx < targetPos) {
                        rightLineNum++;
                        addDiffRow(rightModel, String.valueOf(rightLineNum), "", afterLines.get(rightIdx));
                        rightIdx++;
                    }
                }
            }

            if (delta.getType() == DeltaType.DELETE) {
                for (int i = 0; i < source.getLines().size(); i++) {
                    leftLineNum++;
                    addDiffRow(leftModel, String.valueOf(leftLineNum), "-", source.getLines().get(i));
                    addDiffRow(rightModel, "", "-", "");
                }
                leftIdx += source.getLines().size();
            } else if (delta.getType() == DeltaType.INSERT) {
                for (int i = 0; i < target.getLines().size(); i++) {
                    rightLineNum++;
                    addDiffRow(leftModel, "", "+", "");
                    addDiffRow(rightModel, String.valueOf(rightLineNum), "+", target.getLines().get(i));
                }
                rightIdx += target.getLines().size();
            } else if (delta.getType() == DeltaType.CHANGE) {
                List<String> srcLines = source.getLines();
                List<String> tgtLines = target.getLines();
                int maxLen = Math.max(srcLines.size(), tgtLines.size());

                for (int i = 0; i < maxLen; i++) {
                    if (i < srcLines.size()) {
                        leftLineNum++;
                        addDiffRow(leftModel, String.valueOf(leftLineNum), "-", srcLines.get(i));
                    } else {
                        addDiffRow(leftModel, "", "+", "");
                    }

                    if (i < tgtLines.size()) {
                        rightLineNum++;
                        addDiffRow(rightModel, String.valueOf(rightLineNum), "+", tgtLines.get(i));
                    } else {
                        addDiffRow(rightModel, "", "-", "");
                    }
                }
                leftIdx += srcLines.size();
                rightIdx += tgtLines.size();
            }

            prevSourceEnd = leftIdx;
            prevTargetEnd = rightIdx;
        }

        int remainingLeft = beforeLines.size() - leftIdx;
        int remainingRight = afterLines.size() - rightIdx;
        if (remainingLeft > CONTEXT_LINES || remainingRight > CONTEXT_LINES) {
            int showLeft = Math.min(CONTEXT_LINES, remainingLeft);
            int showRight = Math.min(CONTEXT_LINES, remainingRight);
            for (int i = 0; i < showLeft; i++) {
                leftLineNum++;
                addDiffRow(leftModel, String.valueOf(leftLineNum), "", beforeLines.get(leftIdx));
                leftIdx++;
            }
            for (int i = 0; i < showRight; i++) {
                rightLineNum++;
                addDiffRow(rightModel, String.valueOf(rightLineNum), "", afterLines.get(rightIdx));
                rightIdx++;
            }
            addSeparatorRow(leftModel);
            addSeparatorRow(rightModel);
            leftIdx = beforeLines.size();
            rightIdx = afterLines.size();
        }

        while (leftIdx < beforeLines.size() && rightIdx < afterLines.size()) {
            leftLineNum++;
            rightLineNum++;
            addDiffRow(leftModel, String.valueOf(leftLineNum), "", beforeLines.get(leftIdx));
            addDiffRow(rightModel, String.valueOf(rightLineNum), "", afterLines.get(rightIdx));
            leftIdx++;
            rightIdx++;
        }

        while (leftIdx < beforeLines.size()) {
            leftLineNum++;
            addDiffRow(leftModel, String.valueOf(leftLineNum), "-", beforeLines.get(leftIdx));
            addDiffRow(rightModel, "", "-", "");
            leftIdx++;
        }
        while (rightIdx < afterLines.size()) {
            rightLineNum++;
            addDiffRow(leftModel, "", "+", "");
            addDiffRow(rightModel, String.valueOf(rightLineNum), "+", afterLines.get(rightIdx));
            rightIdx++;
        }
    }

    private void addDiffRow(DefaultTableModel model, String lineNum, String marker, String code) {
        model.addRow(new Object[]{lineNum, marker, code});
    }

    private void addSeparatorRow(DefaultTableModel model) {
        model.addRow(new Object[]{"", "~", "..."});
    }

    private void addMessageRow(DefaultTableModel model, String message) {
        model.addRow(new Object[]{"", "", message});
    }

    private void addAllLines(DefaultTableModel model, String content, String styleName) {
        String[] lines = content.split("\r\n|\r|\n", -1);
        String marker = styleName.equals("removed") ? "-" : styleName.equals("added") ? "+" : "";
        for (int i = 0; i < lines.length; i++) {
            model.addRow(new Object[]{String.valueOf(i + 1), marker, lines[i]});
        }
    }

    private void setupSyncScroll() {
        JScrollPane leftScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, leftTable);
        JScrollPane rightScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, rightTable);

        if (leftScroll == null || rightScroll == null) return;

        JScrollBar leftBar = leftScroll.getVerticalScrollBar();
        JScrollBar rightBar = rightScroll.getVerticalScrollBar();

        AdjustmentListener leftListener = e -> {
            if (syncing) return;
            syncing = true;
            rightBar.setValue(leftBar.getValue());
            syncing = false;
        };

        AdjustmentListener rightListener = e -> {
            if (syncing) return;
            syncing = true;
            leftBar.setValue(rightBar.getValue());
            syncing = false;
        };

        leftBar.addAdjustmentListener(leftListener);
        rightBar.addAdjustmentListener(rightListener);
    }

    private List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] split = content.split("\r\n|\r|\n", -1);
        for (String line : split) {
            lines.add(line);
        }
        if (content.endsWith("\n") || content.endsWith("\r")) {
            lines.add("");
        }
        return lines;
    }

    private boolean isBinaryContent(String content) {
        if (content == null || content.isEmpty()) return false;
        int nonPrintable = 0;
        int total = Math.min(content.length(), 8192);
        for (int i = 0; i < total; i++) {
            char c = content.charAt(i);
            if (c == '\0' || (c < 32 && c != '\n' && c != '\r' && c != '\t')) {
                nonPrintable++;
            }
        }
        return (nonPrintable * 100 / total) > 10;
    }
}
