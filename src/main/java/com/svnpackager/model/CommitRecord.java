package com.svnpackager.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommitRecord {
    private long revision;
    private String author;
    private Date date;
    private String message;
    private List<String> changedPaths;

    public CommitRecord() {
        this.changedPaths = new ArrayList<>();
    }

    public CommitRecord(long revision, String author, Date date, String message) {
        this();
        this.revision = revision;
        this.author = author;
        this.date = date;
        this.message = message;
    }

    public long getRevision() { return revision; }
    public void setRevision(long revision) { this.revision = revision; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getChangedPaths() { return changedPaths; }
    public void setChangedPaths(List<String> changedPaths) { this.changedPaths = changedPaths; }
}
