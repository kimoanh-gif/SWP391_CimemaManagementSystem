package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.model.Movie;
import com.cinema.model.MovieReview;
import com.cinema.model.PublicShowtime;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.util.PublicPageModelHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Screen: Movie Details ( /movies/{id} )
 * Use cases: UC02 — View Movie Information, UC03 — View Showtimes (for this movie)
 *
 * - BR-MI-01: only public movies (Now Showing / Coming Soon) can be opened.
 * - BR-MI-05: a missing trailer does not block the page.
 * - BR-MI-06: only bookable showtimes of this movie are listed.
 * - A2: when the movie has no showtimes a "no showtimes yet" message is shown.
 *
 * Review submission is handled by {@link MovieReviewController}.
 *
 * Assignee: Nguyễn Đức Anh (Iter2)
 */
@Controller
public class MovieDetailsController {

    static final int RELATED_MOVIE_LIMIT = 4;
    static final String NOT_FOUND_MESSAGE = "The requested movie could not be found or is no longer available.";

    private final MovieCatalogService movieService;
    private final PublicShowtimeService showtimeService;
    private final PublicPageModelHelper pageHelper;

    public MovieDetailsController(MovieCatalogService movieService,
                                  PublicShowtimeService showtimeService,
                                  PublicPageModelHelper pageHelper) {
        this.movieService = movieService;
        this.showtimeService = showtimeService;
        this.pageHelper = pageHelper;
    }

    @GetMapping("/movies/{id}")
    public String movieDetails(
            @PathVariable("id") int movieId,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<Movie> movieOpt;
        try {
            movieOpt = movieService.getPublicMovieById(movieId);
        } catch (DataAccessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Movie information could not be loaded right now. Please try again.");
            return "redirect:/movies";
        }
        if (movieOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", NOT_FOUND_MESSAGE);
            return "redirect:/movies";
        }

        AuthenticatedUser user = pageHelper.populateHeader(session, model, "movies");
        Movie movie = movieOpt.get();
        model.addAttribute("movie", movie);

        // Showtimes: pick the requested date only if the movie really has showtimes on it.
        List<LocalDate> availableDates = pageHelper.loadSafely(model,
                () -> showtimeService.getAvailableDatesForMovie(movieId), List.of());
        LocalDate selectedDate = null;
        if (!availableDates.isEmpty()) {
            selectedDate = (showDate != null && availableDates.contains(showDate)) ? showDate : availableDates.get(0);
            if (showDate != null && !availableDates.contains(showDate)) {
                model.addAttribute("infoMessage", "There are no showtimes on the selected date. Showing the nearest available date instead.");
            }
        }
        final LocalDate dateForQuery = selectedDate;
        List<PublicShowtime> showtimes = dateForQuery == null
                ? List.of()
                : pageHelper.loadSafely(model, () -> showtimeService.getShowtimesForMovie(movieId, dateForQuery), List.of());

        model.addAttribute("availableDates", availableDates);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("showtimes", showtimes);

        // Reviews and related movies are optional; missing data never fails the page (BR-MI-07).
        List<MovieReview> reviews = pageHelper.loadSafely(model, () -> movieService.getMovieReviews(movieId), List.of());
        model.addAttribute("reviews", reviews);
        model.addAttribute("canReview", pageHelper.isCustomer(user));
        model.addAttribute("relatedMovies", pageHelper.loadSafely(model,
                () -> movieService.getRelatedMovies(movieId, RELATED_MOVIE_LIMIT), List.of()));

        return "customer/movies/details";
    }
}
