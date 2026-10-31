package com.cinema.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for updating a single seat's type and/or status (Screen 58 / Function 38).
 */
public class SeatUpdateForm {

    @Positive(message = "Mã ghế không hợp lệ.")
    private Integer seatId;

    @Positive(message = "Mã phòng chiếu không hợp lệ.")
    private Integer roomId;

    @Size(max = 50, message = "Tên loại ghế không được vượt quá 50 ký tự.")
    private String seatTypeName;

    @Pattern(
            regexp = "^(|Active|Maintenance|Unavailable)$",
            message = "Trạng thái ghế không hợp lệ. Cho phép: Active, Maintenance, Unavailable."
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

    @AssertTrue(message = "Phải cung cấp ít nhất loại ghế hoặc trạng thái ghế để cập nhật.")
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

