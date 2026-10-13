package com.cinema.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a Showtime entity (Screen 59 / UC-30 / SWP391 Iteration 2).
 * Associated with Movie and Room, includes scheduling time intervals and status.
 */
public class Showtime {

    private Integer showtimeId;
    private Integer movieId;
    private Integer roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal baseTicketPrice;
    private String status; // 'Scheduled', 'Showing', 'Completed', 'Cancelled', 'Stopped'
    private LocalDateTime createdAt;

    // Associated join fields for UI display
    private String movieTitle;
    private String moviePoster;
    private Integer movieDuration;
    private String movieAgeRating;
    private String roomName;
    private String roomCode;
    private String roomType;
    private Integer roomCapacity;

    public Showtime() {
    }

    public Showtime(Integer showtimeId, Integer movieId, Integer roomId,
                    LocalDateTime startTime, LocalDateTime endTime,
                    BigDecimal baseTicketPrice, String status) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.roomId = roomId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.baseTicketPrice = baseTicketPrice;
        this.status = status;
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

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
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

    public BigDecimal getBaseTicketPrice() {
        return baseTicketPrice;
    }

    public void setBaseTicketPrice(BigDecimal baseTicketPrice) {
        this.baseTicketPrice = baseTicketPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMoviePoster() {
        return moviePoster;
    }

    public void setMoviePoster(String moviePoster) {
        this.moviePoster = moviePoster;
    }

    public Integer getMovieDuration() {
        return movieDuration;
    }

    public void setMovieDuration(Integer movieDuration) {
        this.movieDuration = movieDuration;
    }

    public String getMovieAgeRating() {
        return movieAgeRating;
    }

    public void setMovieAgeRating(String movieAgeRating) {
        this.movieAgeRating = movieAgeRating;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public Integer getRoomCapacity() {
        return roomCapacity;
    }

    public void setRoomCapacity(Integer roomCapacity) {
        this.roomCapacity = roomCapacity;
    }

    public boolean isActive() {
        return "Scheduled".equalsIgnoreCase(this.status) || "Showing".equalsIgnoreCase(this.status);
    }
}
