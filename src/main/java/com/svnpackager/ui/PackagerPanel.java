package com.svnpackager.ui;

import com.svnpackager.model.CommitRecord;
import com.svnpackager.model.SvnProject;
import com.svnpackager.service.PackagerService;
import com.svnpackager.service.SvnService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class PackagerPanel extends JPanel {
    private MainFrame mainFrame;
    private SvnProject currentProject;
    private List<CommitRecord> selectedRecords;
    private JTextArea logArea;
    private DefaultListModel<String> fileListModel;
    private JList<String> fileList;
    private JLabel lblFileCount;
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JLabel lblProjectName;
    private JLabel lblLocalPath;
    private JLabel lblVersion;

    public PackagerPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.selectedRecords = new ArrayList<>();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JLabel titleLabel = new JLabel("增量打包");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        infoPanel.add(new JLabel("项目名称:"), gbc);
        gbc.gridx = 1;
        lblProjectName = new JLabel("未选择项目");
        infoPanel.add(lblProjectName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        infoPanel.add(new JLabel("本地路径:"), gbc);
        gbc.gridx = 1;
        lblLocalPath = new JLabel("-");
        infoPanel.add(lblLocalPath, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        infoPanel.add(new JLabel("打包版本:"), gbc);
        gbc.gridx = 1;
        lblVersion = new JLabel("-");
        infoPanel.add(lblVersion, gbc);

        centerPanel.add(infoPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);

        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        JPanel fileHeader = new JPanel(new BorderLayout());
        lblFileCount = new JLabel("待打包文件");
        lblFileCount.setFont(new Font("微软雅黑", Font.BOLD, 12));
        fileHeader.add(lblFileCount, BorderLayout.WEST);
        filePanel.add(fileHeader, BorderLayout.NORTH);

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("Consolas", Font.PLAIN, 12));
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = fileList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String svnPath = fileListModel.getElementAt(index);
                        openFilePreview(svnPath);
                    }
                }
            }
        });
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setBorder(BorderFactory.createTitledBorder("变更文件列表（双击预览）"));
        filePanel.add(fileScrollPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(filePanel);

        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        logArea.setBackground(new Color(40, 40, 40));
        logArea.setForeground(new Color(200, 200, 200));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("构建日志"));
        logPanel.add(logScrollPane, BorderLayout.CENTER);

        splitPane.setRightComponent(logPanel);

        centerPanel.add(splitPane, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("就绪");
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        lblStatus = new JLabel("就绪");
        lblStatus.setForeground(Color.GRAY);
        bottomPanel.add(lblStatus, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnBack = new JButton("返回日志");
        JButton btnPackageIncremental = new JButton("增量打包");

        btnBack.addActionListener(e -> mainFrame.showCardPanel("log"));
        btnPackageIncremental.addActionListener(e -> incrementalPackage());

        buttonPanel.add(btnBack);
        buttonPanel.add(btnPackageIncremental);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    incrementalPackage();
                }
            }
        });
        setFocusable(true);
    }

    public void setProject(SvnProject project) {
        this.currentProject = project;
        if (project != null) {
            lblProjectName.setText(project.getName() != null ? project.getName() : "未命名");
            lblLocalPath.setText(project.getLocalPath() != null ? project.getLocalPath() : "-");
            lblVersion.setText(project.getLastPackedRevision() != null ? project.getLastPackedRevision() : "未打包过");
        } else {
            lblProjectName.setText("未选择项目");
            lblLocalPath.setText("-");
            lblVersion.setText("-");
        }
    }

    public void setSelectedRecords(List<CommitRecord> records, SvnProject project) {
        this.selectedRecords = records != null ? records : new ArrayList<>();
        setProject(project);
        loadFilePreview();
        logArea.setText("");
        if (!this.selectedRecords.isEmpty()) {
            lblVersion.setText("已选 " + this.selectedRecords.size() + " 条提交");
        }
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void loadFilePreview() {
        fileListModel.clear();
        Set<String> uniquePaths = new LinkedHashSet<>();
        for (CommitRecord record : selectedRecords) {
            for (String path : record.getChangedPaths()) {
                String cleanPath = path.startsWith("/") ? path.substring(1) : path;
                uniquePaths.add(cleanPath);
            }
        }
        for (String path : uniquePaths) {
            fileListModel.addElement(path);
        }
        lblFileCount.setText("待打包文件 (" + uniquePaths.size() + ")");
    }

    private String chooseOutputDir() {
        String defaultDir = null;
        if (currentProject != null && currentProject.getOutputDir() != null
                && !currentProject.getOutputDir().trim().isEmpty()) {
            defaultDir = currentProject.getOutputDir();
        } else if (currentProject != null && currentProject.getLocalPath() != null) {
            defaultDir = currentProject.getLocalPath() + File.separator + "output";
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择打包输出目录");
        if (defaultDir != null) {
            chooser.setCurrentDirectory(new File(defaultDir));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private void incrementalPackage() {
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "请先选择项目", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedRecords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先在日志页面选择要打包的提交记录",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String outputDir = chooseOutputDir();
        if (outputDir == null) {
            return;
        }

        List<String> allChangedPaths = new ArrayList<>();
        long minRev = Long.MAX_VALUE;
        for (CommitRecord record : selectedRecords) {
            allChangedPaths.addAll(record.getChangedPaths());
            if (record.getRevision() < minRev) {
                minRev = record.getRevision();
            }
        }
        final long minRevision = minRev;

        List<String> uniquePaths = new ArrayList<>(new LinkedHashSet<>(allChangedPaths));

        logArea.setText("");
        setBuilding(true);
        appendLog("开始增量打包: " + currentProject.getName());
        appendLog("已选提交记录: " + selectedRecords.size() + " 条");
        appendLog("变更文件数: " + uniquePaths.size());
        appendLog("输出目录: " + outputDir);
        appendLog("--------------------------------------");

        new Thread(() -> {
            try {
                PackagerService packagerService = new PackagerService();
                String zipPath = packagerService.packageForTomcatIncremental(
                        currentProject.getLocalPath(),
                        outputDir,
                        currentProject.getName(),
                        String.valueOf(minRevision),
                        uniquePaths,
                        new PackagerService.ProgressCallback() {
                            @Override
                            public void onProgress(String message) {
                                SwingUtilities.invokeLater(() -> appendLog(message));
                            }
                        });

                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("打包完成");
                    appendLog("打包完成: " + zipPath);
                    setBuilding(false);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    progressBar.setString("打包失败");
                    appendLog("打包失败: " + e.getMessage());
                    setBuilding(false);
                });
            }
        }).start();
    }

    private void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setBuilding(boolean building) {
        if (building) {
            lblStatus.setText("正在处理...");
            lblStatus.setForeground(Color.BLUE);
        } else {
            lblStatus.setText("就绪");
            lblStatus.setForeground(Color.GRAY);
        }
    }

    private void openFilePreview(String svnPath) {
        if (currentProject == null || currentProject.getSvnUrl() == null) {
            JOptionPane.showMessageDialog(this, "请先选择项目", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FileRevisionRange range = findRevisionRangeForFile(svnPath);
        if (range == null) {
            JOptionPane.showMessageDialog(this, "未找到该文件的提交版本", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String targetPath = svnPath.startsWith("/") ? svnPath : "/" + svnPath;
        final long minRev = range.minRevision;
        final long maxRev = range.maxRevision;

        lblStatus.setText("正在获取变更内容...");
        lblStatus.setForeground(Color.BLUE);

        new Thread(() -> {
            try {
                SvnService svnService = new SvnService();
                String afterContent = svnService.getFileContent(
                        currentProject.getSvnUrl(),
                        currentProject.getUsername(),
                        currentProject.getPassword(),
                        targetPath,
                        maxRev);

                String beforeContent = svnService.getFileContent(
                        currentProject.getSvnUrl(),
                        currentProject.getUsername(),
                        currentProject.getPassword(),
                        targetPath,
                        minRev - 1);

                SwingUtilities.invokeLater(() -> {
                    String title = "变更预览 - " + svnPath + " (Rev " + minRev + " → " + maxRev + ")";
                    DiffPreviewDialog dialog = new DiffPreviewDialog(
                            SwingUtilities.getWindowAncestor(this), title, beforeContent, afterContent, minRev, maxRev);
                    dialog.setVisible(true);
                    lblStatus.setText("就绪");
                    lblStatus.setForeground(Color.GRAY);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "获取变更内容失败:\n" + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                    lblStatus.setText("就绪");
                    lblStatus.setForeground(Color.GRAY);
                });
            }
        }).start();
    }

    private static class FileRevisionRange {
        final long minRevision;
        final long maxRevision;

        FileRevisionRange(long minRevision, long maxRevision) {
            this.minRevision = minRevision;
            this.maxRevision = maxRevision;
        }
    }

    private FileRevisionRange findRevisionRangeForFile(String svnPath) {
        String normalizedPath = svnPath.startsWith("/") ? svnPath : "/" + svnPath;
        long minRev = Long.MAX_VALUE;
        long maxRev = Long.MIN_VALUE;

        for (CommitRecord record : selectedRecords) {
            for (String changedPath : record.getChangedPaths()) {
                String normalizedChangedPath = changedPath.startsWith("/") ? changedPath : "/" + changedPath;
                boolean match = normalizedChangedPath.equals(normalizedPath)
                        || normalizedChangedPath.endsWith(normalizedPath)
                        || normalizedPath.endsWith(normalizedChangedPath);
                if (match) {
                    if (record.getRevision() < minRev) minRev = record.getRevision();
                    if (record.getRevision() > maxRev) maxRev = record.getRevision();
                }
            }
        }

        if (minRev == Long.MAX_VALUE) {
            return null;
        }
        return new FileRevisionRange(minRev, maxRev);
    }
}
