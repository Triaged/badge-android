package com.triaged.badge.models;

import android.text.TextUtils;

import java.util.concurrent.TimeoutException;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public class User {

    long id;
    String firstName;
    String lastName;
    String fullName;
    transient String initials;
    boolean archived;
    String avatarFaceUrl;
    String avatarUrl;
    String email;
    long managerId;
    long primaryOfficeLocationId;
    long currentOfficeLocationId;
    long departmentId;
    boolean isSharingOfficeLocation;
    boolean installedApp;

    EmployeeInfo employeeInfo;

    public static final int SHARING_LOCATION_UNAVAILABLE = 100;
    public static final int SHARING_LOCATION_ONE = 200;
    public static final int SHARING_LOCATION_OFF = 300;


    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public long getManagerId() {
        return managerId;
    }

    public void setManagerId(long managerId) {
        this.managerId = managerId;
    }

    public long getPrimaryOfficeLocationId() {
        return primaryOfficeLocationId;
    }

    public void setPrimaryOfficeLocationId(long primaryOfficeLocationId) {
        this.primaryOfficeLocationId = primaryOfficeLocationId;
    }

    public long currentOfficeLocationId() {
        return currentOfficeLocationId;
    }

    public void setCurrentOfficeLocationId(long currentOfficeLocationId) {
        this.currentOfficeLocationId = currentOfficeLocationId;
    }

    public long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(long departmentId) {
        this.departmentId = departmentId;
    }

    public boolean isInstalledApp() {
        return installedApp;
    }

    public void setInstalledApp(boolean installedApp) {
        this.installedApp = installedApp;
    }

    public boolean isSharingLocation() {
        return isSharingOfficeLocation;
    }

    public void setSharingOfficeLocationStatus(boolean sharingOfficeLocationStatus) {
        this.isSharingOfficeLocation = sharingOfficeLocationStatus;
    }

    public EmployeeInfo getEmployeeInfo() {
        return employeeInfo;
    }

    public void setEmployeeInfo(EmployeeInfo employeeInfo) {
        this.employeeInfo = employeeInfo;
    }

    public String initials() {
        if (initials == null) {
            if (TextUtils.isEmpty(firstName)) {
                if (TextUtils.isEmpty(lastName)) {
                    initials = "";
                }
                initials = lastName.substring(0, 1).toLowerCase();
            } else {
                if (TextUtils.isEmpty(lastName)) {
                    initials = firstName.substring(0, 1).toUpperCase();
                } else {
                    initials = firstName.substring(0, 1) + lastName.substring(0, 1).toUpperCase();
                }
            }
        }
        return initials;
    }
}
