package com.svnpackager.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class FilePreviewDialog extends JDialog {
    private JTextArea contentArea;
    private JLabel statusLabel;

    private static final Set<String> BINARY_EXTENSIONS = new HashSet<>();

    static {
        BINARY_EXTENSIONS.add(".class");
        BINARY_EXTENSIONS.add(".jar");
        BINARY_EXTENSIONS.add(".zip");
        BINARY_EXTENSIONS.add(".war");
        BINARY_EXTENSIONS.add(".ear");
        BINARY_EXTENSIONS.add(".png");
        BINARY_EXTENSIONS.add(".jpg");
        BINARY_EXTENSIONS.add(".jpeg");
        BINARY_EXTENSIONS.add(".gif");
        BINARY_EXTENSIONS.add(".bmp");
        BINARY_EXTENSIONS.add(".ico");
        BINARY_EXTENSIONS.add(".pdf");
        BINARY_EXTENSIONS.add(".doc");
        BINARY_EXTENSIONS.add(".docx");
        BINARY_EXTENSIONS.add(".xls");
        BINARY_EXTENSIONS.add(".xlsx");
        BINARY_EXTENSIONS.add(".ppt");
        BINARY_EXTENSIONS.add(".pptx");
        BINARY_EXTENSIONS.add(".exe");
        BINARY_EXTENSIONS.add(".dll");
        BINARY_EXTENSIONS.add(".so");
        BINARY_EXTENSIONS.add(".dylib");
    }

    public FilePreviewDialog(Window owner, String title, String localFilePath) {
        super(owner, title, Dialog.ModalityType.MODELESS);
        setSize(900, 600);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(owner);
        initUI();
        loadFileContent(localFilePath);
    }

    private void initUI() {
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(contentPanel);

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        JLabel titleLabel = new JLabel("文件预览");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("关闭");
        btnClose.addActionListener(e -> dispose());
        buttonPanel.add(btnClose);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        contentArea = new JTextArea();
        contentArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        contentArea.setEditable(true);
        contentArea.setLineWrap(false);
        contentArea.setWrapStyleWord(false);
        contentArea.setBackground(new Color(40, 40, 40));
        contentArea.setForeground(new Color(200, 200, 200));
        contentArea.setCaretColor(Color.WHITE);
        contentArea.setSelectionColor(new Color(60, 120, 180));

        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("文件内容"));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("就绪");
        statusLabel.setForeground(Color.GRAY);
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadFileContent(String localFilePath) {
        if (localFilePath == null || localFilePath.trim().isEmpty()) {
            contentArea.setText("文件路径为空");
            statusLabel.setText("错误：文件路径为空");
            statusLabel.setForeground(Color.RED);
            return;
        }

        Path filePath = Paths.get(localFilePath);

        if (!Files.exists(filePath)) {
            contentArea.setText("文件不存在：\n" + localFilePath + "\n\n提示：请确保已执行 Build Artifacts 编译项目");
            statusLabel.setText("文件不存在");
            statusLabel.setForeground(Color.RED);
            return;
        }

        if (!Files.isRegularFile(filePath)) {
            contentArea.setText("不是有效的文件：\n" + localFilePath);
            statusLabel.setText("不是有效的文件");
            statusLabel.setForeground(Color.RED);
            return;
        }

        String fileName = filePath.getFileName().toString().toLowerCase();
        for (String ext : BINARY_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                contentArea.setText("【二进制文件】\n\n文件：" + fileName + "\n大小：" + formatFileSize(filePath) + "\n\n无法预览二进制文件内容");
                statusLabel.setText("二进制文件");
                statusLabel.setForeground(Color.ORANGE);
                contentArea.setEditable(false);
                return;
            }
        }

        try {
            long fileSize = Files.size(filePath);
            if (fileSize > 5 * 1024 * 1024) {
                contentArea.setText("【文件过大】\n\n文件：" + fileName + "\n大小：" + formatFileSize(filePath) + "\n\n文件超过 5MB，无法预览");
                statusLabel.setText("文件过大");
                statusLabel.setForeground(Color.ORANGE);
                contentArea.setEditable(false);
                return;
            }

            String content = readFileContent(filePath);
            contentArea.setText(content);
            contentArea.setCaretPosition(0);

            int lineCount = content.split("\n").length;
            statusLabel.setText("文件：" + fileName + " | 大小：" + formatFileSize(filePath) + " | 行数：" + lineCount);
            statusLabel.setForeground(new Color(0, 128, 0));
        } catch (IOException e) {
            contentArea.setText("读取文件失败：\n" + e.getMessage());
            statusLabel.setText("读取失败");
            statusLabel.setForeground(Color.RED);
        }
    }

    private String readFileContent(Path filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);

        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return new String(bytes, 3, bytes.length - 3, "UTF-8");
        }

        if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return new String(bytes, 2, bytes.length - 2, "UTF-16LE");
        }

        if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return new String(bytes, 2, bytes.length - 2, "UTF-16BE");
        }

        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(bytes, "GBK");
        }
    }

    private String formatFileSize(Path filePath) {
        try {
            long bytes = Files.size(filePath);
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return String.format("%.2f KB", bytes / 1024.0);
            } else {
                return String.format("%.2f MB", bytes / (1024.0 * 1024));
            }
        } catch (IOException e) {
            return "未知";
        }
    }
}