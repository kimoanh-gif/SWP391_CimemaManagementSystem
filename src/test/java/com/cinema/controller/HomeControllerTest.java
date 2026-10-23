package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.Movie;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PromotionNewsService;
import com.cinema.service.PublicShowtimeService;
import com.cinema.service.SessionService;
import com.cinema.util.PublicPageModelHelper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private MovieCatalogService movieService;

    @Mock
    private PublicShowtimeService showtimeService;

    @Mock
    private PromotionNewsService promotionNewsService;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private HttpSession session;

    private HomeController homeController;

    @BeforeEach
    void setUp() {
        homeController = new HomeController(movieService, showtimeService, promotionNewsService,
                new PublicPageModelHelper(sessionService, customerDAO));
    }

    private Movie movie(int id, String title) {
        Movie movie = new Movie();
        movie.setMovieId(id);
        movie.setTitle(title);
        return movie;
    }

    @Test
    void testHomeReturnsIndexViewAndPopulatesModel() {
        Movie movie = movie(1, "Featured Movie");

        when(movieService.getNowShowingMovies(8)).thenReturn(List.of(movie));
        when(movieService.getComingSoonMovies(6)).thenReturn(List.of());
        when(movieService.getFeaturedMovies(5)).thenReturn(List.of(movie));
        when(showtimeService.getTodayShowtimes(8)).thenReturn(List.of());
        when(promotionNewsService.getLatestPromotions(4)).thenReturn(List.of());
        when(promotionNewsService.getLatestNews(3)).thenReturn(List.of());
        when(movieService.getAllPublicMovies()).thenReturn(List.of(movie));
        when(showtimeService.getUpcomingDates(7)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = homeController.home(session, model);

        assertEquals("index", view);
        assertEquals("home", model.getAttribute("activeNav"));
        assertEquals(List.of(movie), model.getAttribute("featuredMovies"));
        assertTrue(model.containsAttribute("nowShowingMovies"));
        assertTrue(model.containsAttribute("comingSoonMovies"));
        assertTrue(model.containsAttribute("todayShowtimes"));
        assertTrue(model.containsAttribute("promotions"));
        assertTrue(model.containsAttribute("newsList"));
        assertTrue(!model.containsAttribute("loadError"));
    }

    @Test
    void testHomeUsesComingSoonAsFeaturedWhenNothingIsShowing() {
        Movie upcoming = movie(2, "Upcoming Movie");

        when(movieService.getNowShowingMovies(8)).thenReturn(List.of());
        when(movieService.getComingSoonMovies(6)).thenReturn(List.of(upcoming));
        when(movieService.getFeaturedMovies(5)).thenReturn(List.of());
        when(showtimeService.getTodayShowtimes(8)).thenReturn(List.of());
        when(promotionNewsService.getLatestPromotions(4)).thenReturn(List.of());
        when(promotionNewsService.getLatestNews(3)).thenReturn(List.of());
        when(movieService.getAllPublicMovies()).thenReturn(List.of(upcoming));
        when(showtimeService.getUpcomingDates(7)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        homeController.home(session, model);

        assertEquals(List.of(upcoming), model.getAttribute("featuredMovies"));
    }

    @Test
    void testHomeStillRendersWhenDatabaseFails() {
        when(movieService.getNowShowingMovies(8)).thenThrow(new DataAccessResourceFailureException("down"));
        when(movieService.getComingSoonMovies(6)).thenReturn(List.of());
        when(movieService.getFeaturedMovies(5)).thenReturn(List.of());
        when(showtimeService.getTodayShowtimes(8)).thenReturn(List.of());
        when(promotionNewsService.getLatestPromotions(4)).thenReturn(List.of());
        when(promotionNewsService.getLatestNews(3)).thenReturn(List.of());
        when(movieService.getAllPublicMovies()).thenReturn(List.of());
        when(showtimeService.getUpcomingDates(7)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = homeController.home(session, model);

        assertEquals("index", view);
        assertEquals(true, model.getAttribute("loadError"));
        assertEquals(List.of(), model.getAttribute("nowShowingMovies"));
    }
}
