package com.cinema.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Row of the "Upcoming showtimes" card on the Manager dashboard. */
public class DashboardShowtime {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");

    private int showtimeId;
    private String movieTitle;
    private String ageRating;
    private String roomName;
    private LocalDateTime startTime;
    private int bookedSeats;
    private int capacity;

    public int getShowtimeId() { return showtimeId; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public int getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(int bookedSeats) { this.bookedSeats = bookedSeats; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getStartTimeText() {
        if (startTime == null) return "";
        return startTime.toLocalDate().equals(LocalDate.now())
                ? startTime.format(TIME)
                : startTime.format(TIME) + " " + startTime.format(DAY);
    }

    public boolean isGeneralAudience() {
        return ageRating == null || ageRating.isBlank() || "P".equalsIgnoreCase(ageRating.trim());
    }

    /** "Checking tickets" within 30 minutes of the start, "Sold out" when full, otherwise "Selling tickets". */
    public String getStatusLabel() {
        if (capacity > 0 && bookedSeats >= capacity) return "Sold out";
        if (startTime != null && Duration.between(LocalDateTime.now(), startTime).toMinutes() <= 30) {
            return "Checking tickets";
        }
        return "Selling tickets";
    }

    public String getStatusClass() {
        return switch (getStatusLabel()) {
            case "Checking tickets" -> "bg-[#EBF6EE] text-[#1E6B37]";
            case "Sold out" -> "bg-cinema-gold/20 text-[#745100]";
            default -> "bg-surface-peach text-on-surface-variant";
        };
    }
}
