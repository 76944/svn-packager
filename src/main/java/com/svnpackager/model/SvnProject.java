package com.svnpackager.model;

public class SvnProject {
    private String id;
    private String name;
    private String svnUrl;
    private String svnPath;
    private String localPath;
    private String username;
    private String password;
    private String lastPackedRevision;
    private String outputDir;
    private long createdTime;

    public SvnProject() {
        this.id = java.util.UUID.randomUUID().toString();
        this.createdTime = System.currentTimeMillis();
    }

    public SvnProject(String name, String svnUrl, String localPath) {
        this();
        this.name = name;
        this.svnUrl = svnUrl;
        this.localPath = localPath;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSvnUrl() { return svnUrl; }
    public void setSvnUrl(String svnUrl) { this.svnUrl = svnUrl; }
    public String getSvnPath() { return svnPath; }
    public void setSvnPath(String svnPath) { this.svnPath = svnPath; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getLastPackedRevision() { return lastPackedRevision; }
    public void setLastPackedRevision(String lastPackedRevision) { this.lastPackedRevision = lastPackedRevision; }
    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
