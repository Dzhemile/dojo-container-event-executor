package com.epam.eventexecutor.model;

public class ParticipantPushDetails {

    private String participantGitHubUsername;
    private String participantGitHubRepoName;
    private String hostGitHubUsername;
    private String hostGitHubAccessToken;
    private String hostGitHubRepoName;

    public String getParticipantGitHubUsername() {
        return participantGitHubUsername;
    }

    public void setParticipantGitHubUsername(String participantGitHubUsername) {
        this.participantGitHubUsername = participantGitHubUsername;
    }

    public String getParticipantGitHubRepoName() {
        return participantGitHubRepoName;
    }

    public void setParticipantGitHubRepoName(String participantGitHubRepoName) {
        this.participantGitHubRepoName = participantGitHubRepoName;
    }

    public String getHostGitHubUsername() {
        return hostGitHubUsername;
    }

    public void setHostGitHubUsername(String hostGitHubUsername) {
        this.hostGitHubUsername = hostGitHubUsername;
    }

    public String getHostGitHubAccessToken() {
        return hostGitHubAccessToken;
    }

    public void setHostGitHubAccessToken(String hostGitHubAccessToken) {
        this.hostGitHubAccessToken = hostGitHubAccessToken;
    }

    public String getHostGitHubRepoName() {
        return hostGitHubRepoName;
    }

    public void setHostGitHubRepoName(String hostGitHubRepoName) {
        this.hostGitHubRepoName = hostGitHubRepoName;
    }
}
