package com.triaged.badge.models;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public class Account {

    int id;
    boolean installedApp;
    String authenticationToken;
    String companyId;
    User currentUser;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isInstalledApp() {
        return installedApp;
    }

    public void setInstalledApp(boolean installedApp) {
        this.installedApp = installedApp;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
}
