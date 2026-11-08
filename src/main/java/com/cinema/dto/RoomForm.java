package com.cinema.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for creating/updating a Room.
 * Validation follows SRS requirements.
 */
public class RoomForm {
    private Integer roomId; // null for create

    @NotBlank(message = "Room code cannot be empty")
    @Size(max = 20, message = "Room code cannot exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Room code can only contain letters, numbers, hyphens or underscores")
    private String roomCode;

    @NotBlank(message = "Room name cannot be empty")
    @Size(max = 100, message = "Room name cannot exceed 100 characters")
    private String roomName;

    @NotBlank(message = "Room type cannot be empty")
    @Size(max = 50, message = "Room type cannot exceed 50 characters")
    private String roomType; // expected values defined in SRS (IMAX Laser, ...)

    @NotNull(message = "Capacity cannot be empty")
    @Min(value = 1, message = "Capacity must be greater than 0")
    @Max(value = 5000, message = "Capacity cannot exceed 5000")
    private Integer capacity;

    @NotNull(message = "Total rows cannot be empty")
    @Min(value = 1, message = "Total rows must be greater than 0")
    @Max(value = 100, message = "Total rows cannot exceed 100")
    private Integer totalRows;

    @NotNull(message = "Seats per row cannot be empty")
    @Min(value = 1, message = "Seats per row must be greater than 0")
    @Max(value = 100, message = "Seats per row cannot exceed 100")
    private Integer seatsPerRow;

    @Size(max = 100, message = "Projector info cannot exceed 100 characters")
    private String projector;

    @Size(max = 100, message = "Sound system info cannot exceed 100 characters")
    private String soundSystem;

    @Size(max = 100, message = "Screen size info cannot exceed 100 characters")
    private String screenSize;

    @NotBlank(message = "Room status cannot be empty")
    @Pattern(regexp = "^(Ready|Screening|Cleaning|Maintenance)$", message = "Invalid room status. Allowed: Ready, Screening, Cleaning, Maintenance")
    private String status; // Screening, Ready, Cleaning, Maintenance

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @AssertTrue(message = "Total room capacity cannot exceed vertical rows × horizontal seats")
    public boolean isCapacityMatchingLayout() {
        if (capacity == null || totalRows == null || seatsPerRow == null) {
            return true;
        }
        if (capacity <= 0 || totalRows <= 0 || seatsPerRow <= 0) {
            return true;
        }
        if (roomId == null) {
            return capacity.equals(totalRows * seatsPerRow);
        }
        return capacity <= (totalRows * seatsPerRow);
    }

    // Getters & Setters
    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getSeatsPerRow() {
        return seatsPerRow;
    }

    public void setSeatsPerRow(Integer seatsPerRow) {
        this.seatsPerRow = seatsPerRow;
    }

    public String getProjector() {
        return projector;
    }

    public void setProjector(String projector) {
        this.projector = projector;
    }

    public String getSoundSystem() {
        return soundSystem;
    }

    public void setSoundSystem(String soundSystem) {
        this.soundSystem = soundSystem;
    }

    public String getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(String screenSize) {
        this.screenSize = screenSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
