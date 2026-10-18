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
        String view = controller.listShowtimes(today, null, "ALL", "ALL", session, model);

        assertEquals("customer/showtimes/list", view);
        assertTrue(model.containsAttribute("groupedShowtimes"));
        assertTrue(model.containsAttribute("upcomingDates"));
    }
}
