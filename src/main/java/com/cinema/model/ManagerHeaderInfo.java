package com.cinema.model;

/** Data shared by every Manager page header (room status pill, signed-in user). */
public class ManagerHeaderInfo {

    private final int activeRooms;
    private final int totalRooms;
    private final String userName;
    private final String userRole;

    public ManagerHeaderInfo(int activeRooms, int totalRooms, String userName, String userRole) {
        this.activeRooms = activeRooms;
        this.totalRooms = totalRooms;
        this.userName = userName;
        this.userRole = userRole;
    }

    public int getActiveRooms() { return activeRooms; }
    public int getTotalRooms() { return totalRooms; }
    public String getUserName() { return userName == null || userName.isBlank() ? "Manager" : userName; }

    public String getRoleLabel() {
        return "ADMIN".equalsIgnoreCase(userRole) ? "System Administrator" : "Cinema Manager";
    }

    public boolean isAllRoomsActive() { return totalRooms > 0 && activeRooms == totalRooms; }

    public String getUserInitials() {
        String[] parts = getUserName().trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
