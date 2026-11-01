package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.PublicShowtime;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimePublicControllerTest {

    @Mock
    private PublicShowtimeService showtimeService;

    @Mock
    private MovieCatalogService movieService;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private HttpSession session;

    private ShowtimePublicController controller;

    @BeforeEach
    void setUp() {
        controller = new ShowtimePublicController(showtimeService, movieService, sessionService, customerDAO);
    }

    @Test
    void testListShowtimesReturnsListViewAndGroupsByMovie() {
        LocalDate today = LocalDate.now();
        PublicShowtime st = new PublicShowtime();
        st.setShowtimeId(1);
        st.setMovieId(10);
        st.setMovieTitle("Movie 10");

        when(showtimeService.getUpcomingDates(14)).thenReturn(List.of(today));
        when(showtimeService.getShowtimes(any(LocalDate.class), any(), any(), any())).thenReturn(List.of(st));
        when(movieService.searchMovies(null, null, "ALL", "title")).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.listShowtimes(today, null, "ALL", "ALL", null, null, session, model);

        assertEquals("customer/showtimes/list", view);
        assertTrue(model.containsAttribute("groupedShowtimes"));
        assertTrue(model.containsAttribute("upcomingDates"));
    }

    @Test
    void showtimesCanBeFilteredByGenreAndAgeRating() {
        LocalDate today = LocalDate.now();
        PublicShowtime horrorT18 = new PublicShowtime();
        horrorT18.setMovieId(10);
        horrorT18.setMovieAgeRating("T18");
        PublicShowtime horrorT16 = new PublicShowtime();
        horrorT16.setMovieId(11);
        horrorT16.setMovieAgeRating("T16");
        PublicShowtime comedyT18 = new PublicShowtime();
        comedyT18.setMovieId(12);
        comedyT18.setMovieAgeRating("T18");
        com.cinema.model.Movie m10 = new com.cinema.model.Movie();
        m10.setMovieId(10);
        com.cinema.model.Movie m11 = new com.cinema.model.Movie();
        m11.setMovieId(11);

        when(showtimeService.getUpcomingDates(14)).thenReturn(List.of(today));
        when(showtimeService.getShowtimes(any(LocalDate.class), any(), any(), any()))
                .thenReturn(List.of(horrorT18, horrorT16, comedyT18));
        when(movieService.searchMovies(null, 6, "ALL", "title")).thenReturn(List.of(m10, m11));
        when(movieService.searchMovies(null, null, "ALL", "title")).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.listShowtimes(today, null, "ALL", "ALL", 6, "T18", session, model);

        assertEquals(List.of(horrorT18), model.getAttribute("showtimes"));
    }
}
