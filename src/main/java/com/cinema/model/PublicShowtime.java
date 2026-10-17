package com.cinema.model;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model representing a Showtime for customer viewing and booking.
 * Screens 21 (Home), 23 (Movie Details), 24 (Showtime List).
 */
public class PublicShowtime {

    private Integer showtimeId;
    private Integer movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private String movieAgeRating;
    private Integer movieDuration;
    private String movieGenres;
    private Integer roomId;
    private String roomName;
    private String roomType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private Integer availableSeats;
    private Integer totalSeats;
    private String status;

    public PublicShowtime() {
    }

    public Integer getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Integer showtimeId) {
        this.showtimeId = showtimeId;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMoviePosterUrl() {
        if (moviePosterUrl != null && !moviePosterUrl.isBlank()) {
            return moviePosterUrl;
        }
        return "/images/cinema-login-hero.png";
    }

    public void setMoviePosterUrl(String moviePosterUrl) {
        this.moviePosterUrl = moviePosterUrl;
    }

    public String getMovieAgeRating() {
        return movieAgeRating != null ? movieAgeRating : "P";
    }

    public void setMovieAgeRating(String movieAgeRating) {
        this.movieAgeRating = movieAgeRating;
    }

    public Integer getMovieDuration() {
        return movieDuration != null ? movieDuration : 0;
    }

    public void setMovieDuration(Integer movieDuration) {
        this.movieDuration = movieDuration;
    }

    public String getMovieGenres() {
        return movieGenres != null ? movieGenres : "";
    }

    public void setMovieGenres(String movieGenres) {
        this.movieGenres = movieGenres;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomType() {
        return roomType != null ? roomType : "2D";
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getBasePrice() {
        return basePrice != null ? basePrice : BigDecimal.ZERO;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getAvailableSeats() {
        return availableSeats != null ? availableSeats : 0;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public Integer getTotalSeats() {
        return totalSeats != null ? totalSeats : 0;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Helper methods
    public LocalDate getShowDate() {
        return startTime != null ? startTime.toLocalDate() : LocalDate.now();
    }

    public String getFormattedStartTime() {
        if (startTime == null) return "--:--";
        return startTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getFormattedEndTime() {
        if (endTime == null) return "--:--";
        return endTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getFormattedShowDate() {
        if (startTime == null) return "";
        return startTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getFormattedPrice() {
        if (basePrice == null) return "0 đ";
        DecimalFormat df = new DecimalFormat("###,###");
        return df.format(basePrice) + " đ";
    }

    public boolean isSoldOut() {
        return availableSeats != null && availableSeats <= 0;
    }

    public String getSeatStatusBadge() {
        if (availableSeats == null || availableSeats <= 0) return "Hết ghế";
        if (availableSeats < 10) return "Còn " + availableSeats + " ghế";
        return "Còn " + availableSeats + " ghế";
    }

    public String getSeatStatusClass() {
        if (availableSeats == null || availableSeats <= 0) return "seat-sold-out";
        if (availableSeats < 10) return "seat-warning";
        return "seat-available";
    }

    public String getTimeSlotCategory() {
        if (startTime == null) return "afternoon";
        int hour = startTime.getHour();
        if (hour < 12) return "morning";
        if (hour < 18) return "afternoon";
        return "evening";
    }
}
