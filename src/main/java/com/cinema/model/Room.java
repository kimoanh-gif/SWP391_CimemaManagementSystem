package com.cinema.model;

import java.time.LocalDateTime;

/**
 * Represents a Cinema Room managed by the system.
 * Screen 56 - Cinema Room Management (UC-27 / SWP391)
 */
public class Room {

    private Integer roomId;
    private String roomCode;
    private String roomName;
    private String roomType;
    private Integer capacity;
    private Integer totalRows;
    private Integer seatsPerRow;
    private String projector;
    private String soundSystem;
    private String screenSize;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int seatCount;
    private int activeSeatCount; // Usable / Active seats in Seat table (P1-01)
    private int showtimeCount;

    public Room() {
    }

    public Room(Integer roomId, String roomCode, String roomName, String roomType,
                Integer capacity, String status) {
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.roomName = roomName;
        this.roomType = roomType;
        this.capacity = capacity;
        this.status = status;
    }

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public Integer getSeatsPerRow() { return seatsPerRow; }
    public void setSeatsPerRow(Integer seatsPerRow) { this.seatsPerRow = seatsPerRow; }

    public String getProjector() { return projector; }
    public void setProjector(String projector) { this.projector = projector; }

    public String getSoundSystem() { return soundSystem; }
    public void setSoundSystem(String soundSystem) { this.soundSystem = soundSystem; }

    public String getScreenSize() { return screenSize; }
    public void setScreenSize(String screenSize) { this.screenSize = screenSize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public int getActiveSeatCount() { return activeSeatCount; }
    public void setActiveSeatCount(int activeSeatCount) { this.activeSeatCount = activeSeatCount; }

    public int getShowtimeCount() { return showtimeCount; }
    public void setShowtimeCount(int showtimeCount) { this.showtimeCount = showtimeCount; }

    /**
     * BR-08: Only rooms with active operational status can receive new showtimes.
     */
    public boolean isActiveForShowtimes() {
        return "Ready".equalsIgnoreCase(this.status) || "Screening".equalsIgnoreCase(this.status);
    }

    /**
     * P1-01: Room is ready for scheduling if active status AND has at least 1 active usable seat.
     */
    public boolean isReadyForScheduling() {
        return isActiveForShowtimes() && this.activeSeatCount > 0;
    }

    public boolean isUnderMaintenance() {
        return "Maintenance".equalsIgnoreCase(this.status);
    }
}
