package com.cinema.model;

import java.time.LocalDateTime;

/**
 * Represents a seat in a cinema room.
 * Mirrors the `Seat` table defined in schema_room.sql.
 */
public class Seat {
    private Integer seatId;
    private Integer roomId;
    private String seatTypeName; // e.g., Standard, VIP, Couple
    private String rowLabel;     // e.g., A, B, C
    private Integer seatNumber;  // column number within the row
    private Integer gridRow;     // numeric row index (1‑based)
    private Integer gridColumn;  // numeric column index (1‑based)
    private String status;       // Active, Maintenance, Unavailable
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Seat() {}

    public Seat(Integer seatId, Integer roomId, String seatTypeName, String rowLabel, Integer seatNumber,
                Integer gridRow, Integer gridColumn, String status) {
        this.seatId = seatId;
        this.roomId = roomId;
        this.seatTypeName = seatTypeName;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.gridRow = gridRow;
        this.gridColumn = gridColumn;
        this.status = status;
    }

    // Getters & Setters
    public Integer getSeatId() { return seatId; }
    public void setSeatId(Integer seatId) { this.seatId = seatId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getSeatTypeName() { return seatTypeName; }
    public void setSeatTypeName(String seatTypeName) { this.seatTypeName = seatTypeName; }
    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
    public Integer getGridRow() { return gridRow; }
    public void setGridRow(Integer gridRow) { this.gridRow = gridRow; }
    public Integer getGridColumn() { return gridColumn; }
    public void setGridColumn(Integer gridColumn) { this.gridColumn = gridColumn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
