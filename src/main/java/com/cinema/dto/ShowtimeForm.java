package com.cinema.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Form DTO for creating and updating a Showtime (Screen 59 / UC-30).
 */
public class ShowtimeForm {

    private Integer showtimeId;

    @NotNull(message = "Please select a movie")
    @Positive(message = "Movie ID must be greater than 0")
    private Integer movieId;

    @NotNull(message = "Please select a screening room")
    @Positive(message = "Room ID must be greater than 0")
    private Integer roomId;

    @NotNull(message = "Please select start time")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    private Integer trailerDuration = 0;

    @DecimalMin(value = "0.0", message = "Base ticket price cannot be negative")
    @DecimalMax(value = "10000000.0", message = "Base ticket price cannot exceed 10,000,000 VND")
    private BigDecimal baseTicketPrice;

    @Pattern(
            regexp = "^(Scheduled|Showing|Ended|Cancelled|Stopped)$",
            message = "Invalid showtime status"
    )
    private String status = "Scheduled";

    public ShowtimeForm() {
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

    public Integer getTrailerDuration() {
        return trailerDuration != null ? trailerDuration : 0;
    }

    public void setTrailerDuration(Integer trailerDuration) {
        this.trailerDuration = trailerDuration;
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
}
