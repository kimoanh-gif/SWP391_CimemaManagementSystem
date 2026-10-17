package com.cinema.service;

import com.cinema.dao.PublicShowtimeDAO;
import com.cinema.model.PublicShowtime;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PublicShowtimeService {

    private final PublicShowtimeDAO showtimeDAO;

    public PublicShowtimeService(PublicShowtimeDAO showtimeDAO) {
        this.showtimeDAO = showtimeDAO;
    }

    public List<PublicShowtime> getShowtimes(LocalDate date, Integer movieId, String roomType, String timeSlot) {
        if (date == null) date = LocalDate.now();
        return showtimeDAO.findShowtimes(date, movieId, roomType, timeSlot);
    }

    public List<PublicShowtime> getShowtimesForMovie(int movieId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        return showtimeDAO.findShowtimesByMovieAndDate(movieId, date);
    }

    public List<LocalDate> getAvailableDatesForMovie(int movieId) {
        List<LocalDate> dates = showtimeDAO.findAvailableDatesForMovie(movieId);
        if (dates.isEmpty()) {
            // Default to today and tomorrow
            dates = new ArrayList<>();
            dates.add(LocalDate.now());
            dates.add(LocalDate.now().plusDays(1));
        }
        return dates;
    }

    public List<LocalDate> getUpcomingDates(int daysCount) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < daysCount; i++) {
            list.add(today.plusDays(i));
        }
        return list;
    }

    public List<PublicShowtime> getTodayShowtimes(int limit) {
        return showtimeDAO.findShowtimesForToday(limit);
    }

    public Optional<PublicShowtime> getShowtimeById(int showtimeId) {
        return showtimeDAO.findById(showtimeId);
    }
}
