package com.svnpackager.ui;

import com.svnpackager.model.SvnProject;
import com.svnpackager.service.ConfigManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class MainFrame extends JFrame {
    private ConfigManager configManager;
    private JList<SvnProject> projectList;
    private DefaultListModel<SvnProject> projectListModel;
    private JPanel rightPanel;
    private CardLayout cardLayout;

    private ProjectPanel projectPanel;
    private LogPanel logPanel;
    private PackagerPanel packagerPanel;

    public MainFrame() {
        this.configManager = new ConfigManager();
        initUI();
        loadProjects();
    }

    private void initUI() {
        setTitle("SVN增量打包工具 v1.0.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1200, 700));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(220);

        splitPane.setLeftComponent(createProjectPanel());
        splitPane.setRightComponent(createRightPanel());

        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createProjectPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("项目列表"));

        projectListModel = new DefaultListModel<>();
        projectList = new JList<>(projectListModel);
        projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectList.setCellRenderer(new ProjectCellRenderer());
        projectList.addListSelectionListener(e -> onProjectSelected());
        projectList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    SvnProject project = projectList.getSelectedValue();
                    if (project != null) {
                        showCardPanel("packager");
                        packagerPanel.setProject(project);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(projectList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JButton btnAdd = new JButton("新增");
        JButton btnEdit = new JButton("编辑");
        JButton btnDelete = new JButton("删除");
        JButton btnRefresh = new JButton("刷新");

        btnAdd.addActionListener(e -> addProject());
        btnEdit.addActionListener(e -> editProject());
        btnDelete.addActionListener(e -> deleteProject());
        btnRefresh.addActionListener(e -> loadProjects());

        buttonPanel.add(btnAdd);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnRefresh);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRightPanel() {
        rightPanel = new JPanel();
        cardLayout = new CardLayout();
        rightPanel.setLayout(cardLayout);

        projectPanel = new ProjectPanel(this);
        logPanel = new LogPanel(this);
        packagerPanel = new PackagerPanel(this);

        rightPanel.add(projectPanel, "project");
        rightPanel.add(logPanel, "log");
        rightPanel.add(packagerPanel, "packager");

        return rightPanel;
    }

    private void loadProjects() {
        projectListModel.clear();
        List<SvnProject> projects = configManager.getProjects();
        for (SvnProject project : projects) {
            projectListModel.addElement(project);
        }
    }

    private void onProjectSelected() {
        SvnProject project = projectList.getSelectedValue();
        if (project != null) {
            showPanel("log");
            logPanel.setProject(project);
        } else {
            showPanel("project");
        }
    }

    public void showPanel(String name) {
        showCardPanel(name);
    }

    private void addProject() {
        SvnProject project = new SvnProject();
        projectPanel.setProject(project);
        projectPanel.setEditing(false);
        showCardPanel("project");
    }

    private void editProject() {
        SvnProject project = projectList.getSelectedValue();
        if (project == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个项目", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        projectPanel.setProject(project);
        projectPanel.setEditing(true);
        showCardPanel("project");
    }

    private void deleteProject() {
        SvnProject project = projectList.getSelectedValue();
        if (project == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个项目", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int result = JOptionPane.showConfirmDialog(this,
                "确定删除项目 \"" + project.getName() + "\" 吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            configManager.removeProject(project.getId());
            loadProjects();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public JList<SvnProject> getProjectList() {
        return projectList;
    }

    public void refreshProjectList() {
        loadProjects();
    }

    public PackagerPanel getPackagerPanel() {
        return packagerPanel;
    }

    public LogPanel getLogPanel() {
        return logPanel;
    }

    public void showCardPanel(String cardName) {
        cardLayout.show(rightPanel, cardName);
    }

    class ProjectCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SvnProject) {
                SvnProject project = (SvnProject) value;
                setText(project.getName());
                setToolTipText(project.getSvnUrl());
            }
            return this;
        }
    }
}
