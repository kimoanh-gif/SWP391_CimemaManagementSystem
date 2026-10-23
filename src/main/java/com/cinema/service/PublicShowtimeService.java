package com.cinema.service;

import com.cinema.dao.PublicShowtimeDAO;
import com.cinema.model.PublicShowtime;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for public showtime browsing (UC03 — View Showtimes).
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Service
public class PublicShowtimeService {

    /** Number of days shown in the Showtime List date selector. */
    public static final int SHOWTIME_WINDOW_DAYS = 14;

    /** Time slot filter values accepted on the Showtime List screen. */
    public static final List<String> TIME_SLOTS = List.of("ALL", "morning", "afternoon", "evening");

    private final PublicShowtimeDAO showtimeDAO;

    public PublicShowtimeService(PublicShowtimeDAO showtimeDAO) {
        this.showtimeDAO = showtimeDAO;
    }

    /** Showtimes for a date with every filter applied together (BR-ST-08). */
    public List<PublicShowtime> getShowtimes(LocalDate date, Integer movieId, Integer genreId,
                                             String ageRating, String roomType, String timeSlot) {
        if (date == null) {
            date = LocalDate.now();
        }
        return showtimeDAO.findShowtimes(date, movieId, genreId, ageRating, roomType, normalizeTimeSlot(timeSlot));
    }

    public List<PublicShowtime> getShowtimes(LocalDate date, Integer movieId, String roomType, String timeSlot) {
        return getShowtimes(date, movieId, null, null, roomType, timeSlot);
    }

    public List<PublicShowtime> getShowtimesForMovie(int movieId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return showtimeDAO.findShowtimesByMovieAndDate(movieId, date);
    }

    /** Dates on which the movie still has bookable showtimes; may be empty. */
    public List<LocalDate> getAvailableDatesForMovie(int movieId) {
        return showtimeDAO.findAvailableDatesForMovie(movieId);
    }

    public List<LocalDate> getUpcomingDates(int daysCount) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < daysCount; i++) {
            list.add(today.plusDays(i));
        }
        return list;
    }

    /** BR-ST-04: the selected date must be today or inside the showtime window. */
    public boolean isDateInWindow(LocalDate date) {
        if (date == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !date.isBefore(today) && date.isBefore(today.plusDays(SHOWTIME_WINDOW_DAYS));
    }

    public List<String> getRoomTypes() {
        return showtimeDAO.findActiveRoomTypes();
    }

    public List<PublicShowtime> getTodayShowtimes(int limit) {
        return showtimeDAO.findShowtimesForToday(limit);
    }

    public Optional<PublicShowtime> getShowtimeById(int showtimeId) {
        return showtimeDAO.findById(showtimeId);
    }

    public String normalizeTimeSlot(String timeSlot) {
        if (timeSlot == null) {
            return "ALL";
        }
        for (String slot : TIME_SLOTS) {
            if (slot.equalsIgnoreCase(timeSlot.trim())) {
                return slot;
            }
        }
        return "ALL";
    }
}
