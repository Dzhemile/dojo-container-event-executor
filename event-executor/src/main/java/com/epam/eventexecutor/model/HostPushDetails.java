package com.epam.eventexecutor.model;

public class HostPushDetails {
    private String hostGitHubUserName;
    private String hostGitHubRepoName;

    public String getHostGitHubUserName() {
        return hostGitHubUserName;
    }

    public void setHostGitHubUserName(String hostGitHubUserName) {
        this.hostGitHubUserName = hostGitHubUserName;
    }

    public String getHostGitHubRepoName() {
        return hostGitHubRepoName;
    }

    public void setHostGitHubRepoName(String hostGitHubRepoName) {
        this.hostGitHubRepoName = hostGitHubRepoName;
    }
}
