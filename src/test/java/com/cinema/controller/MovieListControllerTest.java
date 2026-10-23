package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.Genre;
import com.cinema.model.Movie;
import com.cinema.service.MovieCatalogService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieListControllerTest {

    @Mock
    private MovieCatalogService movieService;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private HttpSession session;

    private MovieListController controller;

    @BeforeEach
    void setUp() {
        controller = new MovieListController(movieService, new PublicPageModelHelper(sessionService, customerDAO));
    }

    private Movie movie(int id, String status) {
        Movie m = new Movie();
        m.setMovieId(id);
        m.setTitle("Movie " + id);
        m.setStatus(status);
        return m;
    }

    @Test
    void testListMoviesGroupsByCategory() {
        Movie showing = movie(1, "Now Showing");
        Movie upcoming = movie(2, "Upcoming");
        when(movieService.normalizeType("all")).thenReturn("all");
        when(movieService.normalizeSort("newest")).thenReturn("newest");
        when(movieService.normalizeAgeRating(null)).thenReturn(null);
        when(movieService.getActiveGenres()).thenReturn(List.of());
        when(movieService.getAgeRatings()).thenReturn(MovieCatalogService.AGE_RATINGS);
        when(movieService.searchMovies("", null, null, "all", "newest")).thenReturn(List.of(showing, upcoming));

        Model model = new ConcurrentModel();
        String view = controller.listMovies("all", null, null, null, "newest", session, model);

        assertEquals("customer/movies/list", view);
        assertEquals(List.of(showing), model.getAttribute("nowShowingMovies"));
        assertEquals(List.of(upcoming), model.getAttribute("comingSoonMovies"));
        assertEquals(false, model.getAttribute("hasFilters"));
        assertEquals("movies", model.getAttribute("activeNav"));
    }

    @Test
    void testListMoviesIgnoresUnknownGenreAndAppliesAgeRating() {
        Genre action = new Genre();
        action.setGenreId(1);
        action.setName("Action");
        when(movieService.normalizeType("now-showing")).thenReturn("now-showing");
        when(movieService.normalizeSort("rating")).thenReturn("rating");
        when(movieService.normalizeAgeRating("t13")).thenReturn("T13");
        when(movieService.getActiveGenres()).thenReturn(List.of(action));
        when(movieService.getAgeRatings()).thenReturn(MovieCatalogService.AGE_RATINGS);
        when(movieService.searchMovies("space", null, "T13", "now-showing", "rating")).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.listMovies("now-showing", 999, "t13", "  space ", "rating", session, model);

        verify(movieService).searchMovies("space", null, "T13", "now-showing", "rating");
        assertNull(model.getAttribute("selectedGenreId"));
        assertEquals("T13", model.getAttribute("selectedAgeRating"));
        assertEquals("space", model.getAttribute("keyword"));
        assertTrue((Boolean) model.getAttribute("hasFilters"));
    }
}
