package com.svnpackager.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.svnpackager.model.SvnProject;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final String CONFIG_DIR;
    private static final String CONFIG_FILE;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            CONFIG_DIR = appData + File.separator + "SVNPackager" + File.separator + "config";
        } else {
            CONFIG_DIR = System.getProperty("user.home") + File.separator + ".svn-packager";
        }
        CONFIG_FILE = CONFIG_DIR + File.separator + "config.json";
    }

    private List<SvnProject> projects;

    public ConfigManager() {
        this.projects = new ArrayList<>();
        loadConfig();
    }

    public List<SvnProject> getProjects() {
        return projects;
    }

    public void addProject(SvnProject project) {
        projects.add(project);
        saveConfig();
    }

    public void updateProject(SvnProject project) {
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getId().equals(project.getId())) {
                projects.set(i, project);
                break;
            }
        }
        saveConfig();
    }

    public void removeProject(String projectId) {
        projects.removeIf(p -> p.getId().equals(projectId));
        saveConfig();
    }

    public SvnProject getProjectById(String projectId) {
        for (SvnProject project : projects) {
            if (project.getId().equals(projectId)) {
                return project;
            }
        }
        return null;
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            return;
        }
        try (Reader reader = new FileReader(configFile)) {
            Type listType = new TypeToken<ArrayList<SvnProject>>() {}.getType();
            List<SvnProject> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                this.projects = loaded;
            }
        } catch (Exception e) {
        }
    }

    public void saveConfig() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(projects, writer);
        } catch (Exception e) {
        }
    }
}
