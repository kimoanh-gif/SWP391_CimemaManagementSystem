package com.cinema.model.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Detailed booking transaction record for revenue reconciliation and export.
 */
public class TransactionRevenueItem {

    private int bookingId;
    private String bookingCode;
    private String customerName;
    private String phone;
    private String movieTitle;
    private String roomName;
    private LocalDateTime showtimeStartTime;
    private String seatLabels;
    private BigDecimal ticketAmount = BigDecimal.ZERO;
    private BigDecimal fbAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private String paymentMethod;
    private String bookingStatus;
    private LocalDateTime bookingTime;

    public TransactionRevenueItem() {
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public void setBookingCode(String bookingCode) {
        this.bookingCode = bookingCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public LocalDateTime getShowtimeStartTime() {
        return showtimeStartTime;
    }

    public void setShowtimeStartTime(LocalDateTime showtimeStartTime) {
        this.showtimeStartTime = showtimeStartTime;
    }

    public String getSeatLabels() {
        return seatLabels;
    }

    public void setSeatLabels(String seatLabels) {
        this.seatLabels = seatLabels;
    }

    public BigDecimal getTicketAmount() {
        return ticketAmount;
    }

    public void setTicketAmount(BigDecimal ticketAmount) {
        this.ticketAmount = ticketAmount;
    }

    public BigDecimal getFbAmount() {
        return fbAmount;
    }

    public void setFbAmount(BigDecimal fbAmount) {
        this.fbAmount = fbAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }
}
