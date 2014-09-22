package com.triaged.badge.models;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public class User {

    int id;
    String firstName;
    String lastName;
    String fullName;
    boolean archived;
    String avatarFaceUrl;
    String avatarUrl;
    String email;
    String managerId;
    String primaryOfficeLocationid;
    String currentOfficeLocaitonId;
    String departmentId;
    int sharingOfficeLocationStatus;
    boolean installedApp;

    EmployeeInfo employeeInfo;

    public static final int SHARING_LOCATION_UNAVAILABLE = 100;
    public static final int SHARING_LOCATION_ONE = 200;
    public static final int SHARING_LOCATION_OFF = 300;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getAvatarFaceUrl() {
        return avatarFaceUrl;
    }

    public void setAvatarFaceUrl(String avatarFaceUrl) {
        this.avatarFaceUrl = avatarFaceUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public String getPrimaryOfficeLocationid() {
        return primaryOfficeLocationid;
    }

    public void setPrimaryOfficeLocationid(String primaryOfficeLocationid) {
        this.primaryOfficeLocationid = primaryOfficeLocationid;
    }

    public String getCurrentOfficeLocaitonId() {
        return currentOfficeLocaitonId;
    }

    public void setCurrentOfficeLocaitonId(String currentOfficeLocaitonId) {
        this.currentOfficeLocaitonId = currentOfficeLocaitonId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public boolean isInstalledApp() {
        return installedApp;
    }

    public void setInstalledApp(boolean installedApp) {
        this.installedApp = installedApp;
    }

    public int getSharingOfficeLocationStatus() {
        return sharingOfficeLocationStatus;
    }

    public void setSharingOfficeLocationStatus(int sharingOfficeLocationStatus) {
        if (sharingOfficeLocationStatus != SHARING_LOCATION_ONE &&
                sharingOfficeLocationStatus != SHARING_LOCATION_OFF &&
                sharingOfficeLocationStatus != SHARING_LOCATION_UNAVAILABLE) {
            throw  new IllegalArgumentException("invalid argument for sharing office location status");
        }
        this.sharingOfficeLocationStatus = sharingOfficeLocationStatus;
    }

    public EmployeeInfo getEmployeeInfo() {
        return employeeInfo;
    }

    public void setEmployeeInfo(EmployeeInfo employeeInfo) {
        this.employeeInfo = employeeInfo;
    }
}
