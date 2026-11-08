package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.Movie;
import com.cinema.model.MovieReview;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoviePublicControllerTest {

    @Mock
    private MovieCatalogService movieService;

    @Mock
    private PublicShowtimeService showtimeService;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private HttpSession session;

    private MoviePublicController controller;

    @BeforeEach
    void setUp() {
        controller = new MoviePublicController(movieService, showtimeService, sessionService, customerDAO);
    }

    @Test
    void testListMoviesReturnsListView() {
        when(movieService.searchMovies(null, null, "all", "newest", null)).thenReturn(List.of());
        when(movieService.getAllGenres()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.listMovies("all", null, null, "newest", null, session, model);

        assertEquals("customer/movies/list", view);
        assertTrue(model.containsAttribute("ageRatings"));
        assertTrue(model.containsAttribute("movies"));
        assertTrue(model.containsAttribute("genres"));
    }

    @Test
    void testMovieDetailsFound() {
        Movie movie = new Movie();
        movie.setMovieId(5);
        movie.setTitle("Speedway");

        when(movieService.getMovieById(5)).thenReturn(Optional.of(movie));
        when(showtimeService.getAvailableDatesForMovie(5)).thenReturn(List.of(LocalDate.now()));
        when(showtimeService.getShowtimesForMovie(eq(5), any(LocalDate.class))).thenReturn(List.of());
        when(movieService.getMovieReviews(5)).thenReturn(List.of());
        when(movieService.getRelatedMovies(5, 4)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.movieDetails(5, null, null, session, model, new RedirectAttributesModelMap());

        assertEquals("customer/movies/details", view);
        assertEquals(movie, model.getAttribute("movie"));
        assertEquals(false, model.getAttribute("canBookTickets"));
        @SuppressWarnings("unchecked")
        List<LocalDate> calendarDates = (List<LocalDate>) model.getAttribute("showtimeCalendarDates");
        assertEquals(30, calendarDates.size());
        assertEquals(LocalDate.now(), calendarDates.get(0));
        assertEquals(LocalDate.now().plusDays(29), calendarDates.get(29));
    }

    @Test
    void movieDetailsAlwaysOpensTheThirtyDayCalendarOnToday() {
        Movie movie = new Movie();
        movie.setMovieId(5);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        when(movieService.getMovieById(5)).thenReturn(Optional.of(movie));
        when(showtimeService.getAvailableDatesForMovie(5)).thenReturn(List.of(tomorrow, tomorrow.plusDays(1)));
        when(showtimeService.getShowtimesForMovie(5, LocalDate.now())).thenReturn(List.of());
        when(movieService.getMovieReviews(5)).thenReturn(List.of());
        when(movieService.getRelatedMovies(5, 4)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.movieDetails(5, null, null, session, model, new RedirectAttributesModelMap());

        assertEquals(LocalDate.now(), model.getAttribute("selectedDate"));
    }

    @Test
    void movieDetailsUsesDisplayedReviewsForRatingSummary() {
        Movie movie = new Movie();
        movie.setMovieId(5);
        MovieReview first = new MovieReview();
        first.setRating(4);
        MovieReview second = new MovieReview();
        second.setRating(5);

        when(movieService.getMovieById(5)).thenReturn(Optional.of(movie));
        when(showtimeService.getAvailableDatesForMovie(5)).thenReturn(List.of());
        when(showtimeService.getShowtimesForMovie(eq(5), any(LocalDate.class))).thenReturn(List.of());
        when(movieService.getMovieReviews(5)).thenReturn(List.of(first, second));
        when(movieService.getRelatedMovies(5, 4)).thenReturn(List.of());

        controller.movieDetails(5, null, null, session, new ConcurrentModel(), new RedirectAttributesModelMap());

        assertEquals(2, movie.getReviewCount());
        assertEquals(4.5, movie.getAverageRating());
    }

    @Test
    void archivedMovieIsNotPubliclyViewable() {
        Movie movie = new Movie();
        movie.setMovieId(9);
        movie.setStatus("Archived");
        when(movieService.getMovieById(9)).thenReturn(Optional.of(movie));

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String view = controller.movieDetails(9, null, null, session, new ConcurrentModel(), ra);

        assertEquals("redirect:/movies", view);
        assertEquals("This movie is no longer available.", ra.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void testMovieDetailsNotFoundRedirects() {
        when(movieService.getMovieById(999)).thenReturn(Optional.empty());

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String view = controller.movieDetails(999, null, null, session, new ConcurrentModel(), ra);

        assertEquals("redirect:/movies", view);
        assertTrue(ra.getFlashAttributes().containsKey("errorMessage"));
    }
}
