package com.cinema.model;

public class Permission {
    private Integer permissionId;
    private String  name;
    private String  description;

    public Permission() {}

    public Permission(Integer permissionId, String name, String description) {
        this.permissionId = permissionId;
        this.name = name;
        this.description = description;
    }

    public Integer getPermissionId() { return permissionId; }
    public void setPermissionId(Integer permissionId) { this.permissionId = permissionId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
