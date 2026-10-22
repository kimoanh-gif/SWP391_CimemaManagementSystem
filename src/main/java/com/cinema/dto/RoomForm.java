package com.cinema.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating/updating a Room.
 * Validation follows SRS requirements.
 */
public class RoomForm {
    private Integer roomId; // null for create

    @NotBlank(message = "Mã phòng không được để trống")
    @Size(max = 20, message = "Mã phòng không được vượt quá 20 ký tự")
    private String roomCode;

    @NotBlank(message = "Tên phòng không được để trống")
    @Size(max = 100, message = "Tên phòng không được vượt quá 100 ký tự")
    private String roomName;

    @NotBlank(message = "Loại phòng không được để trống")
    private String roomType; // expected values defined in SRS (IMAX Laser, ...)

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    @Max(value = 5000, message = "Sức chứa không được vượt quá 5000")
    private Integer capacity;

    // Optional technical specs
    private Integer totalRows;
    private Integer seatsPerRow;
    private String projector;
    private String soundSystem;
    private String screenSize;

    @NotBlank(message = "Trạng thái phòng không được để trống")
    private String status; // Screening, Ready, Cleaning, Maintenance

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    public RoomForm() {
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
}
