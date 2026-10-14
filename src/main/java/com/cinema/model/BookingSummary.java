package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingSummary {
    private int bookingId;
    private String bookingCode;
    private String movieTitle;
    private String roomName;
    private LocalDateTime startTime;
    private String seatLabels;
    private int seatCount;
    private BigDecimal seatTotal;
    private BigDecimal foodTotal;
    private BigDecimal totalAmount;
    private LocalDateTime heldUntil;
    private String status;

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public String getSeatLabels() { return seatLabels; }
    public void setSeatLabels(String seatLabels) { this.seatLabels = seatLabels; }
    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }
    public BigDecimal getSeatTotal() { return seatTotal; }
    public void setSeatTotal(BigDecimal seatTotal) { this.seatTotal = seatTotal; }
    public BigDecimal getFoodTotal() { return foodTotal; }
    public void setFoodTotal(BigDecimal foodTotal) { this.foodTotal = foodTotal; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getHeldUntil() { return heldUntil; }
    public void setHeldUntil(LocalDateTime heldUntil) { this.heldUntil = heldUntil; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
