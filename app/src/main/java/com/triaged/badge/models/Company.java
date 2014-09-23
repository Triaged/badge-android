package com.triaged.badge.models;

/**
 * Created by Sadegh Kazemy on 9/23/14.
 */
public class Company {

    String id;
    String name;
    boolean usesDepartments;
    User[] users;
    OfficeLocation[] officeLocations;
    Department[] departments;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isUsesDepartments() {
        return usesDepartments;
    }

    public User[] getUsers() {
        return users;
    }

    public OfficeLocation[] getOfficeLocations() {
        return officeLocations;
    }

    public Department[] getDepartments() {
        return departments;
    }
}
