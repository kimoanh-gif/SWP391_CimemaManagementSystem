package com.cinema.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for updating a single seat's type and/or status (Screen 58 / Function 38).
 */
public class SeatUpdateForm {

    @Positive(message = "Invalid seat ID.")
    private Integer seatId;

    @Positive(message = "Invalid room ID.")
    private Integer roomId;

    @Size(max = 50, message = "Seat type name cannot exceed 50 characters.")
    private String seatTypeName;

    @Pattern(
            regexp = "^(|Active|Maintenance|Unavailable)$",
            message = "Invalid seat status. Allowed: Active, Maintenance, Unavailable."
    )
    private String status;

    public SeatUpdateForm() {
    }

    public SeatUpdateForm(Integer seatId, Integer roomId, String seatTypeName, String status) {
        this.seatId = seatId;
        this.roomId = roomId;
        this.seatTypeName = seatTypeName;
        this.status = status;
    }

    @AssertTrue(message = "Must provide at least seat type or seat status to update.")
    public boolean isUpdateTargetProvided() {
        boolean hasType = seatTypeName != null && !seatTypeName.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty();
        return hasType || hasStatus;
    }

    public Integer getSeatId() {
        return seatId;
    }

    public void setSeatId(Integer seatId) {
        this.seatId = seatId;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getSeatTypeName() {
        return seatTypeName;
    }

    public void setSeatTypeName(String seatTypeName) {
        this.seatTypeName = seatTypeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

