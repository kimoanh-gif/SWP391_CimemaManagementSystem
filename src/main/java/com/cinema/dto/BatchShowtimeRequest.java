package com.cinema.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * DTO for batch creating multiple showtimes in one screening room (Room-First Scheduler).
 */
public class BatchShowtimeRequest {

    @NotNull(message = "Screening room ID is required.")
    @Positive(message = "Invalid screening room ID.")
    private Integer roomId;

    @NotEmpty(message = "Please add at least one showtime slot before saving.")
    @Valid
    private List<ShowtimeForm> showtimes;

    public BatchShowtimeRequest() {
    }

    public BatchShowtimeRequest(Integer roomId, List<ShowtimeForm> showtimes) {
        this.roomId = roomId;
        this.showtimes = showtimes;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public List<ShowtimeForm> getShowtimes() {
        return showtimes;
    }

    public void setShowtimes(List<ShowtimeForm> showtimes) {
        this.showtimes = showtimes;
    }
}

