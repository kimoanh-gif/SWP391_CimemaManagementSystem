package com.cinema.dto;

import com.cinema.model.Seat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Form DTO representing a single seat item in a room layout batch save request (Screen 58 / Function 38).
 * Preserves exact JSON property names of {@link Seat} for API compatibility.
 */
public class SeatLayoutForm {

    @Positive(message = "Mã ghế phải lớn hơn 0.")
    private Integer seatId;

    @Positive(message = "Mã phòng chiếu phải lớn hơn 0.")
    private Integer roomId;

    @Positive(message = "Mã loại ghế phải lớn hơn 0.")
    private Integer seatTypeId;

    @Size(max = 50, message = "Tên loại ghế không được vượt quá 50 ký tự.")
    private String seatTypeName;

    @NotBlank(message = "Mã hàng ghế không được để trống.")
    @Size(max = 5, message = "Mã hàng ghế tối đa 5 ký tự.")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Mã hàng ghế chỉ được chứa chữ cái và chữ số.")
    private String rowLabel;

    @NotNull(message = "Số ghế không được để trống.")
    @Min(value = 1, message = "Số ghế phải từ 1 trở lên.")
    @Max(value = 100, message = "Số ghế không được vượt quá 100.")
    private Integer seatNumber;

    @Min(value = 1, message = "Tọa độ hàng (gridRow) phải từ 1 trở lên.")
    @Max(value = 100, message = "Tọa độ hàng (gridRow) không được vượt quá 100.")
    private Integer gridRow;

    @Min(value = 1, message = "Tọa độ cột (gridColumn) phải từ 1 trở lên.")
    @Max(value = 100, message = "Tọa độ cột (gridColumn) không được vượt quá 100.")
    private Integer gridColumn;

    @Pattern(
            regexp = "^(|Active|Maintenance|Unavailable)$",
            message = "Trạng thái ghế không hợp lệ. Cho phép: Active, Maintenance, Unavailable."
    )
    private String status;

    public SeatLayoutForm() {
    }

    public Seat toSeat(int targetRoomId) {
        Seat seat = new Seat();
        seat.setSeatId(this.seatId);
        seat.setRoomId(targetRoomId);
        seat.setSeatTypeId(this.seatTypeId);
        seat.setSeatTypeName(this.seatTypeName);
        seat.setRowLabel(this.rowLabel != null ? this.rowLabel.trim() : null);
        seat.setSeatNumber(this.seatNumber);
        seat.setGridRow(this.gridRow);
        seat.setGridColumn(this.gridColumn);
        seat.setStatus(this.status);
        return seat;
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

    public Integer getSeatTypeId() {
        return seatTypeId;
    }

    public void setSeatTypeId(Integer seatTypeId) {
        this.seatTypeId = seatTypeId;
    }

    public String getSeatTypeName() {
        return seatTypeName;
    }

    public void setSeatTypeName(String seatTypeName) {
        this.seatTypeName = seatTypeName;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getGridRow() {
        return gridRow;
    }

    public void setGridRow(Integer gridRow) {
        this.gridRow = gridRow;
    }

    public Integer getGridColumn() {
        return gridColumn;
    }

    public void setGridColumn(Integer gridColumn) {
        this.gridColumn = gridColumn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

