package com.cinema.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ReviewableMovie(int movieId, String title, String posterUrl, String bookingCode,
                              LocalDateTime showtime, Integer rating, String reviewContent) {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    public boolean isReviewed() { return rating != null; }
    public String getFormattedShowtime() { return showtime == null ? "" : showtime.format(DISPLAY); }
}
