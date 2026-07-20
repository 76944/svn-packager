package com.svnpackager.ui;

import com.svnpackager.model.CommitRecord;
import com.svnpackager.model.SvnProject;
import com.svnpackager.service.SvnService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogPanel extends JPanel {
    private MainFrame mainFrame;
    private SvnProject currentProject;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private JTable logTable;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;
    private JTextField txtSearch;
    private List<CommitRecord> commitRecords;

    public LogPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.commitRecords = new ArrayList<>();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JLabel titleLabel = new JLabel("SVN提交日志");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.add(new JLabel("开始日期:"));
        startDatePicker = new DatePicker();
        startDatePicker.addDateChangeListener(e -> {
            // 仅当结束日期早于开始日期时才把结束日期对齐到开始日期，
            // 避免一改开始日期就把用户已选的结束日期覆盖掉（否则无法选到起止区间）。
            Date start = startDatePicker.getDate();
            Date end = endDatePicker.getDate();
            if (start != null && end != null && end.before(start)) {
                endDatePicker.setDate(start);
            }
        });
        filterPanel.add(startDatePicker);

        filterPanel.add(new JLabel("结束日期:"));
        endDatePicker = new DatePicker();
        filterPanel.add(endDatePicker);

        JButton btnFetch = new JButton("获取日志");
        btnFetch.addActionListener(e -> fetchLogs());
        filterPanel.add(btnFetch);

        filterPanel.add(new JLabel("搜索:"));
        txtSearch = new JTextField(15);
        txtSearch.setToolTipText("按作者或提交说明过滤");
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });
        filterPanel.add(txtSearch);

        topPanel.add(filterPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"版本号", "作者", "日期", "提交说明"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(tableModel);
        logTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        logTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(400);

        logTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    startPackaging();
                }
            }
        });

        logTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    startPackaging();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(logTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        lblStatus = new JLabel("请选择项目并获取日志");
        lblStatus.setForeground(Color.GRAY);
        bottomPanel.add(lblStatus, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPack = new JButton("打包");
        btnPack.addActionListener(e -> startPackaging());
        buttonPanel.add(btnPack);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setProject(SvnProject project) {
        this.currentProject = project;
        lblStatus.setText("项目: " + project.getName());
        tableModel.setRowCount(0);
        txtSearch.setText("");
    }

    private void fetchLogs() {
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "请先选择项目", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date startDate = startDatePicker.getDate();
        Date endDate = endDatePicker.getDate();

        lblStatus.setText("正在获取日志...");
        lblStatus.setForeground(Color.BLUE);

        new Thread(() -> {
            try {
                SvnService svnService = new SvnService();
                List<CommitRecord> records = svnService.getLog(
                        currentProject.getSvnUrl(),
                        currentProject.getUsername(),
                        currentProject.getPassword(),
                        currentProject.getSvnPath(),
                        startDate, endDate
                );

                SwingUtilities.invokeLater(() -> {
                    commitRecords.clear();
                    commitRecords.addAll(records);
                    tableModel.setRowCount(0);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    for (CommitRecord record : records) {
                        tableModel.addRow(new Object[]{
                                record.getRevision(),
                                record.getAuthor(),
                                sdf.format(record.getDate()),
                                record.getMessage()
                        });
                    }
                    lblStatus.setText("共获取 " + records.size() + " 条提交记录");
                    lblStatus.setForeground(new Color(0, 128, 0));
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("获取日志失败: " + e.getMessage());
                    lblStatus.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(this,
                            "获取日志失败:\n" + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void applyFilter() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        logTable.setRowSorter(sorter);
        if (keyword.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword, 1, 3));
        }
    }

    private List<CommitRecord> getSelectedRecords() {
        List<CommitRecord> selected = new ArrayList<>();
        int[] viewRows = logTable.getSelectedRows();
        for (int viewRow : viewRows) {
            int modelRow = logTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0) {
                Object value = tableModel.getValueAt(modelRow, 0);
                if (value instanceof Long) {
                    long revision = (Long) value;
                    for (CommitRecord record : commitRecords) {
                        if (record.getRevision() == revision) {
                            selected.add(record);
                            break;
                        }
                    }
                }
            }
        }
        return selected;
    }

    private void startPackaging() {
        List<CommitRecord> selected = getSelectedRecords();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择要打包的提交记录",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        mainFrame.getPackagerPanel().setSelectedRecords(selected, currentProject);
        mainFrame.showCardPanel("packager");
    }
}
