package com.svnpackager.service;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.*;

public class PackagerService {

    public interface ProgressCallback {
        void onProgress(String message);
    }

    public void buildProject(String projectPath, ProgressCallback callback) throws Exception {
        callback.onProgress("开始Maven编译...");
        ProcessBuilder pb = new ProcessBuilder(
                "mvn", "clean", "package", "-DskipTests", "-f", projectPath);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                callback.onProgress(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Maven编译失败，退出码: " + exitCode);
        }
        callback.onProgress("Maven编译完成");
    }

    public String packageForTomcat(String projectPath, String outputDir, String appName,
                                    ProgressCallback callback) throws Exception {
        callback.onProgress("查找编译产物...");

        Path projectRoot = Paths.get(projectPath);
        Path warDir = findWarExplodedDirAnywhere(projectRoot, appName);
        if (warDir == null) {
            throw new RuntimeException("未找到编译产物（war展开目录），请先编译项目");
        }

        callback.onProgress("打包ZIP文件...");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        String zipFileName = appName + "_" + timestamp + ".zip";
        Path zipPath = Paths.get(outputDir, zipFileName);

        if (!Files.exists(Paths.get(outputDir))) {
            Files.createDirectories(Paths.get(outputDir));
        }

        zipDirectory(warDir, zipPath, callback);

        callback.onProgress("打包完成: " + zipPath.toAbsolutePath());
        return zipPath.toAbsolutePath().toString();
    }

    public String packageForTomcatIncremental(String projectPath, String outputDir,
                                               String appName, String fromRevision,
                                               List<String> changedFiles,
                                               ProgressCallback callback) throws Exception {
        callback.onProgress("增量打包模式...");

        Path projectRoot = Paths.get(projectPath);
        Path warDir = findWarExplodedDirAnywhere(projectRoot, appName);
        if (warDir == null) {
            throw new RuntimeException("未找到编译产物（war展开目录），请先编译项目");
        }

        Path webInfClasses = warDir.resolve("WEB-INF").resolve("classes");

        callback.onProgress("筛选变更文件...");
        Set<String> excludedFiles = new HashSet<>(Arrays.asList(
                "license.xml", "web.xml", "config.properties", "db.properties",
                "email.properties", "logback.xml", "redis.properties",
                "wechat-config.properties", "whitelist.xml", "sn.txt"));
        List<Path> filesToPackage = new ArrayList<>();
        for (String changedFile : changedFiles) {
            String fileName = Paths.get(changedFile).getFileName().toString();
            if (excludedFiles.contains(fileName)) {
                callback.onProgress("跳过排除文件: " + fileName);
                continue;
            }
            List<Path> resolved = mapSourcePathToWarOutput(
                    changedFile, projectRoot, warDir, webInfClasses);
            for (Path p : resolved) {
                if (Files.exists(p) && !filesToPackage.contains(p)
                        && !excludedFiles.contains(p.getFileName().toString())) {
                    filesToPackage.add(p);
                }
            }
        }

        if (filesToPackage.isEmpty()) {
            callback.onProgress("警告：未在编译产物中找到任何变更文件，尝试使用源文件相对路径匹配...");
            for (String changedFile : changedFiles) {
                if (changedFile.startsWith("/")) {
                    changedFile = changedFile.substring(1);
                }
                Path filePath = warDir.resolve(changedFile);
                if (Files.exists(filePath) && !filesToPackage.contains(filePath)
                        && !excludedFiles.contains(filePath.getFileName().toString())) {
                    filesToPackage.add(filePath);
                }
            }
        }

        if (filesToPackage.isEmpty()) {
            callback.onProgress("没有找到需要打包的变更文件");
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        String zipFileName = appName + "_incremental_" + timestamp + ".zip";
        Path zipPath = Paths.get(outputDir, zipFileName);

        if (!Files.exists(Paths.get(outputDir))) {
            Files.createDirectories(Paths.get(outputDir));
        }

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Set<String> addedEntries = new HashSet<>();
            for (Path file : filesToPackage) {
                String entryName = warDir.relativize(file).toString().replace('\\', '/');
                callback.onProgress("添加: " + entryName);
                Files.walk(file).filter(Files::isRegularFile).forEach(path -> {
                    try {
                        String relativePath = warDir.relativize(path).toString().replace('\\', '/');
                        if (addedEntries.add(relativePath)) {
                            zos.putNextEntry(new ZipEntry(relativePath));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        callback.onProgress("增量打包完成: " + zipPath.toAbsolutePath());
        return zipPath.toAbsolutePath().toString();
    }

    private Path findWarExplodedDirAnywhere(Path projectRoot, String appName) throws IOException {
        Path targetDir = projectRoot.resolve("target");
        if (Files.exists(targetDir)) {
            Path warDir = findWarExplodedDir(targetDir, appName);
            if (warDir != null) return warDir;
        }

        Path outArtifacts = projectRoot.resolve("out").resolve("artifacts");
        if (Files.exists(outArtifacts)) {
            Path warDir = findWarExplodedDir(outArtifacts, appName);
            if (warDir != null) return warDir;
        }

        Path outDir = projectRoot.resolve("out");
        if (Files.exists(outDir)) {
            Path warDir = findWarExplodedDirRecursive(outDir, 3);
            if (warDir != null) return warDir;
        }

        Path buildDir = projectRoot.resolve("build");
        if (Files.exists(buildDir)) {
            Path warDir = findWarExplodedDir(buildDir, appName);
            if (warDir != null) return warDir;
        }

        return null;
    }

    private Path findWarExplodedDir(Path searchDir, String appName) throws IOException {
        Path[] candidates = {
                searchDir.resolve(appName),
                searchDir.resolve(appName + "-exploded"),
                searchDir.resolve(appName + "_war_exploded"),
                searchDir.resolve("exploded-war")
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate) && Files.isDirectory(candidate.resolve("WEB-INF"))) {
                return candidate;
            }
        }

        String normalized = appName.replace('.', '_').replace('-', '_');
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(searchDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    String name = entry.getFileName().toString();
                    if (name.contains(appName) || name.contains(normalized)) {
                        if (Files.isDirectory(entry.resolve("WEB-INF"))) {
                            return entry;
                        }
                    }
                }
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(searchDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry) && Files.isDirectory(entry.resolve("WEB-INF"))) {
                    return entry;
                }
            }
        }

        return null;
    }

    private Path findWarExplodedDirRecursive(Path dir, int maxDepth) throws IOException {
        if (maxDepth <= 0) return null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    if (Files.isDirectory(entry.resolve("WEB-INF"))) {
                        return entry;
                    }
                }
            }
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    Path found = findWarExplodedDirRecursive(entry, maxDepth - 1);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private List<Path> mapSourcePathToWarOutput(String svnPath, Path projectRoot,
                                                 Path warDir, Path webInfClasses) {
        List<Path> results = new ArrayList<>();

        String localPath = toLocalPath(svnPath);

        if (localPath.contains("src/main/java/") || localPath.contains("src\\main\\java\\")) {
            String relativeToSrc = localPath.replaceFirst(".*src/main/java/", "")
                                             .replaceFirst(".*src\\\\main\\\\java\\\\", "");
            if (relativeToSrc.endsWith(".java")) {
                String classBase = relativeToSrc.substring(0, relativeToSrc.length() - 5);
                Path classFile = webInfClasses.resolve(classBase + ".class");
                if (Files.exists(classFile)) {
                    results.add(classFile);
                }
                Path parentDir = classFile.getParent();
                if (parentDir != null && Files.exists(parentDir)) {
                    String simpleName = classBase.contains("/")
                            ? classBase.substring(classBase.lastIndexOf('/') + 1)
                            : classBase;
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir,
                            simpleName + "$*.class")) {
                        for (Path inner : stream) {
                            if (!results.contains(inner)) {
                                results.add(inner);
                            }
                        }
                    } catch (IOException ignored) {}
                    Path javaFile = projectRoot.resolve("src/main/java").resolve(relativeToSrc);
                    for (String cn : extractClassNames(javaFile)) {
                        Path clsFile = parentDir.resolve(cn + ".class");
                        if (Files.exists(clsFile) && !results.contains(clsFile)) {
                            results.add(clsFile);
                        }
                    }
                }
            }
        } else if (localPath.contains("src/main/resources/") || localPath.contains("src\\main\\resources\\")) {
            String relativeToResources = localPath.replaceFirst(".*src/main/resources/", "")
                                                   .replaceFirst(".*src\\\\main\\\\resources\\\\", "");
            Path resourceFile = webInfClasses.resolve(relativeToResources);
            if (Files.exists(resourceFile)) {
                results.add(resourceFile);
            }
        } else if (localPath.contains("src/main/webapp/") || localPath.contains("src\\main\\webapp\\")) {
            String relativeToWebapp = localPath.replaceFirst(".*src/main/webapp/", "")
                                                .replaceFirst(".*src\\\\main\\\\webapp\\\\", "");
            Path webappFile = warDir.resolve(relativeToWebapp);
            if (Files.exists(webappFile)) {
                results.add(webappFile);
            }
        } else if (localPath.contains("WebRoot/") || localPath.contains("WebRoot\\")) {
            String relativeToWebRoot = localPath.replaceFirst(".*WebRoot/", "")
                                                 .replaceFirst(".*WebRoot\\\\", "");
            Path webappFile = warDir.resolve(relativeToWebRoot);
            if (Files.exists(webappFile)) {
                results.add(webappFile);
            }
        } else if (localPath.startsWith("src/") || localPath.startsWith("src\\")) {
            String relativeToSrc = localPath.substring(4);
            if (relativeToSrc.startsWith("/") || relativeToSrc.startsWith("\\")) {
                relativeToSrc = relativeToSrc.substring(1);
            }
            if (relativeToSrc.endsWith(".java")) {
                String classBase = relativeToSrc.substring(0, relativeToSrc.length() - 5);
                Path classFile = webInfClasses.resolve(classBase + ".class");
                if (Files.exists(classFile)) {
                    results.add(classFile);
                }
                Path parentDir = classFile.getParent();
                if (parentDir != null && Files.exists(parentDir)) {
                    String simpleName = classBase.contains("/")
                            ? classBase.substring(classBase.lastIndexOf('/') + 1)
                            : classBase;
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir,
                            simpleName + "$*.class")) {
                        for (Path inner : stream) {
                            if (!results.contains(inner)) {
                                results.add(inner);
                            }
                        }
                    } catch (IOException ignored) {}
                    Path javaFile = projectRoot.resolve("src").resolve(relativeToSrc);
                    for (String cn : extractClassNames(javaFile)) {
                        Path clsFile = parentDir.resolve(cn + ".class");
                        if (Files.exists(clsFile) && !results.contains(clsFile)) {
                            results.add(clsFile);
                        }
                    }
                }
            } else {
                Path resourceFile = webInfClasses.resolve(relativeToSrc);
                if (Files.exists(resourceFile)) {
                    results.add(resourceFile);
                }
            }
        }

        if (results.isEmpty()) {
            String fileName = Paths.get(localPath).getFileName().toString();
            if (fileName.contains(".") && Files.exists(webInfClasses)) {
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                try {
                    Files.walk(webInfClasses).filter(Files::isRegularFile).forEach(path -> {
                        String name = path.getFileName().toString();
                        if (name.equals(fileName) || name.equals(baseName + ".class")
                                || name.startsWith(baseName + "$")) {
                            if (!results.contains(path)) {
                                results.add(path);
                            }
                        }
                    });
                } catch (IOException ignored) {}
            }
        }

        if (results.isEmpty()) {
            String cleanPath = localPath.startsWith("/") ? localPath.substring(1) : localPath;
            Path directInWar = warDir.resolve(cleanPath);
            if (Files.exists(directInWar)) {
                results.add(directInWar);
            }
        }

        if (results.isEmpty()) {
            String cleanPath = localPath.startsWith("/") ? localPath.substring(1) : localPath;
            String[] parts = cleanPath.split("/");
            if (parts.length >= 2 && !parts[0].equals("src") && !parts[0].equals("WEB-INF")
                    && !parts[0].equals("WebRoot")) {
                StringBuilder stripped = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (stripped.length() > 0) stripped.append("/");
                    stripped.append(parts[i]);
                }
                Path retryInWar = warDir.resolve(stripped.toString());
                if (Files.exists(retryInWar)) {
                    results.add(retryInWar);
                }
            }
        }

        return results;
    }

    private String toLocalPath(String svnPath) {
        if (svnPath.startsWith("/")) {
            svnPath = svnPath.substring(1);
        }
        if (svnPath.startsWith("trunk/")) {
            svnPath = svnPath.substring(6);
        } else if (svnPath.startsWith("branches/")) {
            svnPath = svnPath.substring(9);
            int slashIdx = svnPath.indexOf('/');
            if (slashIdx >= 0) {
                svnPath = svnPath.substring(slashIdx + 1);
            }
        } else if (svnPath.startsWith("tags/")) {
            svnPath = svnPath.substring(5);
            int slashIdx = svnPath.indexOf('/');
            if (slashIdx >= 0) {
                svnPath = svnPath.substring(slashIdx + 1);
            }
        }
        return svnPath;
    }

    private List<String> extractClassNames(Path javaFile) {
        List<String> classNames = new ArrayList<>();
        if (!Files.exists(javaFile)) return classNames;
        try {
            String content = new String(Files.readAllBytes(javaFile), "UTF-8");
            Pattern pattern = Pattern.compile(
                    "(?:class|interface|enum|@interface)\\s+(\\w+)");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                classNames.add(matcher.group(1));
            }
        } catch (IOException ignored) {}
        return classNames;
    }

    private void zipDirectory(Path sourceDir, Path zipPath, ProgressCallback callback) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Files.walk(sourceDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String entryName = sourceDir.relativize(path).toString().replace('\\', '/');
                            callback.onProgress("添加: " + entryName);
                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
