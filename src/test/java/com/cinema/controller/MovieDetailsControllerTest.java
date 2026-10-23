package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.Movie;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.service.SessionService;
import com.cinema.util.PublicPageModelHelper;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MovieDetailsControllerTest {

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

    private MovieDetailsController controller;

    @BeforeEach
    void setUp() {
        controller = new MovieDetailsController(movieService, showtimeService,
                new PublicPageModelHelper(sessionService, customerDAO));
    }

    private Movie movie() {
        Movie movie = new Movie();
        movie.setMovieId(5);
        movie.setTitle("Speedway");
        movie.setStatus("Now Showing");
        return movie;
    }

    @Test
    void testMovieDetailsUsesRequestedDateWhenAvailable() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(movieService.getPublicMovieById(5)).thenReturn(Optional.of(movie()));
        when(showtimeService.getAvailableDatesForMovie(5)).thenReturn(List.of(LocalDate.now(), tomorrow));
        when(showtimeService.getShowtimesForMovie(5, tomorrow)).thenReturn(List.of());
        when(movieService.getMovieReviews(5)).thenReturn(List.of());
        when(movieService.getRelatedMovies(5, 4)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.movieDetails(5, tomorrow, session, model, new RedirectAttributesModelMap());

        assertEquals("customer/movies/details", view);
        assertEquals(tomorrow, model.getAttribute("selectedDate"));
        assertEquals(false, model.getAttribute("canReview"));
    }

    @Test
    void testMovieDetailsFallsBackToFirstAvailableDate() {
        LocalDate first = LocalDate.now().plusDays(2);
        when(movieService.getPublicMovieById(5)).thenReturn(Optional.of(movie()));
        when(showtimeService.getAvailableDatesForMovie(5)).thenReturn(List.of(first));
        when(showtimeService.getShowtimesForMovie(5, first)).thenReturn(List.of());
        when(movieService.getMovieReviews(5)).thenReturn(List.of());
        when(movieService.getRelatedMovies(5, 4)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.movieDetails(5, LocalDate.now(), session, model, new RedirectAttributesModelMap());

        assertEquals(first, model.getAttribute("selectedDate"));
        assertTrue(model.containsAttribute("infoMessage"));
    }

    @Test
    void testMovieWithoutShowtimesShowsEmptyState() {
        when(movieService.getPublicMovieById(5)).thenReturn(Optional.of(movie()));
        when(showtimeService.getAvailableDatesForMovie(5)).thenReturn(List.of());
        when(movieService.getMovieReviews(5)).thenReturn(List.of());
        when(movieService.getRelatedMovies(5, 4)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.movieDetails(5, null, session, model, new RedirectAttributesModelMap());

        assertNull(model.getAttribute("selectedDate"));
        assertEquals(List.of(), model.getAttribute("showtimes"));
        verify(showtimeService, never()).getShowtimesForMovie(anyInt(), any(LocalDate.class));
    }

    @Test
    void testNonPublicMovieRedirectsToList() {
        when(movieService.getPublicMovieById(999)).thenReturn(Optional.empty());

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String view = controller.movieDetails(999, null, session, new ConcurrentModel(), ra);

        assertEquals("redirect:/movies", view);
        assertTrue(ra.getFlashAttributes().containsKey("errorMessage"));
    }
}
