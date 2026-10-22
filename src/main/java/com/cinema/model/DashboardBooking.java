package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Row of the "Recent bookings and transactions" table on the Manager dashboard. */
public class DashboardBooking {

    private static final DateTimeFormatter SHOWTIME = DateTimeFormatter.ofPattern("HH:mm dd/MM");

    private String bookingCode;
    private String bookingType;
    private String customerName;
    private String customerPhone;
    private String movieTitle;
    private String roomName;
    private LocalDateTime showtimeStart;
    private String status;
    private boolean checkedIn;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public LocalDateTime getShowtimeStart() { return showtimeStart; }
    public void setShowtimeStart(LocalDateTime showtimeStart) { this.showtimeStart = showtimeStart; }
    public String getStatus() { return status == null ? "" : status.toUpperCase(Locale.ROOT); }
    public void setStatus(String status) { this.status = status; }
    public boolean isCheckedIn() { return checkedIn; }
    public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount == null ? BigDecimal.ZERO : totalAmount; }

    public String getDisplayName() {
        if (customerName != null && !customerName.isBlank()) return customerName;
        return "Walk-in customer";
    }

    /** Masked phone (0912***456) or the sales channel when no phone is known. */
    public String getSubtitle() {
        if (customerPhone != null && customerPhone.length() >= 7) {
            return customerPhone.substring(0, 4) + "***" + customerPhone.substring(customerPhone.length() - 3);
        }
        String type = bookingType == null ? "" : bookingType.toUpperCase(Locale.ROOT);
        return type.contains("COUNTER") || type.contains("OFFLINE") ? "At the counter" : "Online booking";
    }

    public String getShowtimeText() {
        String time = showtimeStart == null ? "" : showtimeStart.format(SHOWTIME);
        return roomName == null ? time : time + " • " + roomName;
    }

    public String getStatusLabel() {
        if (checkedIn) return "Checked in";
        return switch (getStatus()) {
            case "CONFIRMED" -> "Paid";
            case "PENDING", "PAYMENT_PENDING" -> "Awaiting payment";
            case "CANCELLED" -> "Cancelled";
            case "EXPIRED" -> "Expired";
            default -> status == null ? "Unknown" : status;
        };
    }

    public String getStatusClass() {
        if (checkedIn) return "bg-cinema-gold/20 text-[#745100]";
        return switch (getStatus()) {
            case "CONFIRMED" -> "bg-[#EBF6EE] text-[#1E6B37]";
            case "CANCELLED", "EXPIRED" -> "bg-[#FFE8E8] text-[#C2202E]";
            default -> "bg-surface-peach text-on-surface-variant";
        };
    }

    public String getDotClass() {
        if (checkedIn) return "bg-cinema-gold";
        return switch (getStatus()) {
            case "CONFIRMED" -> "bg-[#2E9B4E]";
            case "CANCELLED", "EXPIRED" -> "bg-[#C2202E]";
            default -> "bg-outline";
        };
    }
}
