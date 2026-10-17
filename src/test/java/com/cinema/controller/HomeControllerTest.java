package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.Movie;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.PromotionNewsService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
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
        homeController = new HomeController(movieService, showtimeService, promotionNewsService, sessionService, customerDAO);
    }

    @Test
    void testHomeReturnsIndexViewAndPopulatesModel() {
        Movie movie = new Movie();
        movie.setMovieId(1);
        movie.setTitle("Featured Movie");

        when(movieService.getFeaturedMovies(5)).thenReturn(List.of(movie));
        when(movieService.getNowShowingMovies(8)).thenReturn(List.of(movie));
        when(movieService.getComingSoonMovies(6)).thenReturn(List.of());
        when(showtimeService.getTodayShowtimes(8)).thenReturn(List.of());
        when(promotionNewsService.getLatestPromotions(4)).thenReturn(List.of());
        when(promotionNewsService.getLatestNews(3)).thenReturn(List.of());
        when(showtimeService.getUpcomingDates(7)).thenReturn(List.of());
        when(movieService.searchMovies(null, null, "ALL", "newest")).thenReturn(List.of(movie));

        Model model = new ConcurrentModel();
        String view = homeController.home(session, model);

        assertEquals("index", view);
        assertTrue(model.containsAttribute("featuredMovies"));
        assertTrue(model.containsAttribute("nowShowingMovies"));
        assertTrue(model.containsAttribute("todayShowtimes"));
        assertTrue(model.containsAttribute("promotions"));
        assertTrue(model.containsAttribute("newsList"));
    }
}
