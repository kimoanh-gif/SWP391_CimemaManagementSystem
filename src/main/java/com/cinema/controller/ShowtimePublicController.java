package com.cinema.controller;

import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.model.PublicShowtime;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.util.PublicPageModelHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Screen: Showtime List ( /showtimes )
 * Use case: UC03 — View Showtimes
 *
 * Filters: date, movie, genre, age rating, room type and time slot.
 * - BR-ST-04 / E1: a date outside [today, today + 13] is rejected and today is shown instead.
 * - BR-ST-07: filter values must exist in the system; unknown values are ignored.
 * - BR-ST-08: all selected filters are applied together.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class ShowtimePublicController {

    private final PublicShowtimeService showtimeService;
    private final MovieCatalogService movieService;
    private final PublicPageModelHelper pageHelper;

    public ShowtimePublicController(PublicShowtimeService showtimeService,
                                    MovieCatalogService movieService,
                                    PublicPageModelHelper pageHelper) {
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.pageHelper = pageHelper;
    }

    @GetMapping("/showtimes")
    public String listShowtimes(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "movieId", required = false) Integer movieId,
            @RequestParam(name = "genreId", required = false) Integer genreId,
            @RequestParam(name = "ageRating", required = false) String ageRating,
            @RequestParam(name = "roomType", required = false, defaultValue = "ALL") String roomType,
            @RequestParam(name = "timeSlot", required = false, defaultValue = "ALL") String timeSlot,
            HttpSession session,
            Model model
    ) {
        pageHelper.populateHeader(session, model, "showtimes");

        // 1. Date (BR-ST-04)
        LocalDate selectedDate = LocalDate.now();
        if (date != null) {
            if (showtimeService.isDateInWindow(date)) {
                selectedDate = date;
            } else {
                model.addAttribute("errorMessage",
                        "The selected date is not available. Please choose a date from the list below.");
            }
        }
        model.addAttribute("upcomingDates", showtimeService.getUpcomingDates(PublicShowtimeService.SHOWTIME_WINDOW_DAYS));
        model.addAttribute("selectedDate", selectedDate);

        // 2. Filter options loaded from the database (BR-ST-07)
        List<Movie> allMovies = pageHelper.loadSafely(model, movieService::getAllPublicMovies, List.of());
        List<Genre> genres = pageHelper.loadSafely(model, movieService::getActiveGenres, List.of());
        List<String> roomTypes = pageHelper.loadSafely(model, showtimeService::getRoomTypes, List.of());

        Integer selectedMovieId = allMovies.stream().map(Movie::getMovieId)
                .filter(id -> Objects.equals(id, movieId)).findFirst().orElse(null);
        Integer selectedGenreId = genres.stream().map(Genre::getGenreId)
                .filter(id -> Objects.equals(id, genreId)).findFirst().orElse(null);
        String selectedAgeRating = movieService.normalizeAgeRating(ageRating);
        String selectedRoomType = roomTypes.stream()
                .filter(rt -> rt.equalsIgnoreCase(roomType == null ? "" : roomType.trim()))
                .findFirst().orElse("ALL");
        String selectedTimeSlot = showtimeService.normalizeTimeSlot(timeSlot);

        // 3. Showtimes, grouped by movie for the schedule layout
        final LocalDate queryDate = selectedDate;
        List<PublicShowtime> showtimes = pageHelper.loadSafely(model,
                () -> showtimeService.getShowtimes(queryDate, selectedMovieId, selectedGenreId,
                        selectedAgeRating, selectedRoomType, selectedTimeSlot),
                List.of());
        Map<Integer, List<PublicShowtime>> groupedByMovieId = showtimes.stream()
                .collect(Collectors.groupingBy(PublicShowtime::getMovieId, LinkedHashMap::new, Collectors.toList()));

        model.addAttribute("showtimes", showtimes);
        model.addAttribute("groupedShowtimes", groupedByMovieId);
        model.addAttribute("allMovies", allMovies);
        model.addAttribute("genres", genres);
        model.addAttribute("ageRatings", movieService.getAgeRatings());
        model.addAttribute("roomTypes", roomTypes);

        model.addAttribute("selectedMovieId", selectedMovieId);
        model.addAttribute("selectedGenreId", selectedGenreId);
        model.addAttribute("selectedAgeRating", selectedAgeRating);
        model.addAttribute("selectedRoomType", selectedRoomType);
        model.addAttribute("selectedTimeSlot", selectedTimeSlot);
        model.addAttribute("hasFilters", selectedMovieId != null || selectedGenreId != null
                || selectedAgeRating != null || !"ALL".equals(selectedRoomType) || !"ALL".equals(selectedTimeSlot));

        return "customer/showtimes/list";
    }
}
