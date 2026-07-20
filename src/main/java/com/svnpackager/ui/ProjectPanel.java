package com.svnpackager.ui;

import com.svnpackager.model.SvnProject;
import com.svnpackager.service.SvnService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ProjectPanel extends JPanel {
    private MainFrame mainFrame;
    private JTextField txtName;
    private JTextField txtSvnUrl;
    private JTextField txtSvnPath;
    private JTextField txtLocalPath;
    private JTextField txtOutputDir;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblStatus;
    private SvnProject currentProject;
    private boolean editing;

    public ProjectPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("项目配置");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("项目名称:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtName = new JTextField(30);
        formPanel.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("SVN地址:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtSvnUrl = new JTextField(30);
        formPanel.add(txtSvnUrl, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("SVN路径:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtSvnPath = new JTextField(30);
        txtSvnPath.setToolTipText("如 trunk、branches/xxx，留空则读取整个仓库日志");
        formPanel.add(txtSvnPath, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("本地路径:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        txtLocalPath = new JTextField(30);
        JButton btnBrowse = new JButton("浏览");
        btnBrowse.addActionListener(e -> browseLocalPath());
        pathPanel.add(txtLocalPath, BorderLayout.CENTER);
        pathPanel.add(btnBrowse, BorderLayout.EAST);
        formPanel.add(pathPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("输出目录:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        JPanel outputPanel = new JPanel(new BorderLayout(5, 0));
        txtOutputDir = new JTextField(30);
        JButton btnBrowseOutput = new JButton("浏览");
        btnBrowseOutput.addActionListener(e -> browseOutputDir());
        outputPanel.add(txtOutputDir, BorderLayout.CENTER);
        outputPanel.add(btnBrowseOutput, BorderLayout.EAST);
        formPanel.add(outputPanel, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtUsername = new JTextField(30);
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtPassword = new JPasswordField(30);
        formPanel.add(txtPassword, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.GRAY);
        bottomPanel.add(lblStatus, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnTest = new JButton("测试连接");
        JButton btnSave = new JButton("保存");
        JButton btnCancel = new JButton("取消");

        btnTest.addActionListener(e -> testConnection());
        btnSave.addActionListener(e -> saveProject());
        btnCancel.addActionListener(e -> cancelEdit());

        buttonPanel.add(btnTest);
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void setProject(SvnProject project) {
        this.currentProject = project;
        txtName.setText(project.getName());
        txtSvnUrl.setText(project.getSvnUrl());
        txtSvnPath.setText(project.getSvnPath());
        txtLocalPath.setText(project.getLocalPath());
        txtOutputDir.setText(project.getOutputDir());
        txtUsername.setText(project.getUsername());
        txtPassword.setText(project.getPassword());
        lblStatus.setText(" ");
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    private void browseLocalPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            txtLocalPath.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseOutputDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            txtOutputDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void testConnection() {
        String url = txtSvnUrl.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入SVN地址", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        lblStatus.setText("正在测试连接...");
        lblStatus.setForeground(Color.BLUE);

        new Thread(() -> {
            SvnService svnService = new SvnService();
            boolean connected = svnService.testConnection(url, username, password);
            SwingUtilities.invokeLater(() -> {
                if (connected) {
                    lblStatus.setText("连接成功");
                    lblStatus.setForeground(new Color(0, 128, 0));
                } else {
                    lblStatus.setText("连接失败");
                    lblStatus.setForeground(Color.RED);
                }
            });
        }).start();
    }

    private void saveProject() {
        String name = txtName.getText().trim();
        String svnUrl = txtSvnUrl.getText().trim();
        String localPath = txtLocalPath.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (name.isEmpty() || svnUrl.isEmpty() || localPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "项目名称、SVN地址和本地路径不能为空",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentProject == null) {
            currentProject = new SvnProject();
            editing = false;
        }

        currentProject.setName(name);
        currentProject.setSvnUrl(svnUrl);
        currentProject.setSvnPath(txtSvnPath.getText().trim());
        currentProject.setLocalPath(localPath);
        currentProject.setOutputDir(txtOutputDir.getText().trim());
        currentProject.setUsername(username);
        currentProject.setPassword(password);

        if (editing) {
            mainFrame.getConfigManager().updateProject(currentProject);
        } else {
            mainFrame.getConfigManager().addProject(currentProject);
        }

        mainFrame.refreshProjectList();
        mainFrame.showCardPanel("log");

        JOptionPane.showMessageDialog(this, "项目保存成功", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelEdit() {
        mainFrame.showCardPanel("log");
    }
}
