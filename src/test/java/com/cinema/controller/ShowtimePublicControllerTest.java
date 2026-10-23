package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.Movie;
import com.cinema.model.PublicShowtime;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
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
        controller = new ShowtimePublicController(showtimeService, movieService,
                new PublicPageModelHelper(sessionService, customerDAO));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testListShowtimesReturnsListViewAndGroupsByMovie() {
        LocalDate today = LocalDate.now();
        PublicShowtime st = new PublicShowtime();
        st.setShowtimeId(1);
        st.setMovieId(10);
        st.setMovieTitle("Movie 10");

        when(showtimeService.isDateInWindow(today)).thenReturn(true);
        when(showtimeService.getUpcomingDates(14)).thenReturn(List.of(today));
        when(movieService.getAllPublicMovies()).thenReturn(List.of());
        when(movieService.getActiveGenres()).thenReturn(List.of());
        when(showtimeService.getRoomTypes()).thenReturn(List.of("2D Digital"));
        when(movieService.normalizeAgeRating(null)).thenReturn(null);
        when(showtimeService.normalizeTimeSlot("ALL")).thenReturn("ALL");
        when(movieService.getAgeRatings()).thenReturn(MovieCatalogService.AGE_RATINGS);
        when(showtimeService.getShowtimes(today, null, null, null, "ALL", "ALL")).thenReturn(List.of(st));

        Model model = new ConcurrentModel();
        String view = controller.listShowtimes(today, null, null, null, "ALL", "ALL", session, model);

        assertEquals("customer/showtimes/list", view);
        Map<Integer, List<PublicShowtime>> grouped = (Map<Integer, List<PublicShowtime>>) model.getAttribute("groupedShowtimes");
        assertEquals(1, grouped.size());
        assertEquals(List.of("2D Digital"), model.getAttribute("roomTypes"));
        assertEquals(false, model.getAttribute("hasFilters"));
    }

    @Test
    void testPastDateFallsBackToTodayWithMessage() {
        LocalDate past = LocalDate.now().minusDays(3);
        LocalDate today = LocalDate.now();

        when(showtimeService.isDateInWindow(past)).thenReturn(false);
        when(showtimeService.getUpcomingDates(14)).thenReturn(List.of(today));
        when(movieService.getAllPublicMovies()).thenReturn(List.of());
        when(movieService.getActiveGenres()).thenReturn(List.of());
        when(showtimeService.getRoomTypes()).thenReturn(List.of());
        when(movieService.normalizeAgeRating(null)).thenReturn(null);
        when(showtimeService.normalizeTimeSlot("ALL")).thenReturn("ALL");
        when(movieService.getAgeRatings()).thenReturn(MovieCatalogService.AGE_RATINGS);
        when(showtimeService.getShowtimes(today, null, null, null, "ALL", "ALL")).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.listShowtimes(past, null, null, null, "ALL", "ALL", session, model);

        assertEquals(today, model.getAttribute("selectedDate"));
        assertTrue(model.containsAttribute("errorMessage"));
    }

    @Test
    void testFiltersAreValidatedAgainstDatabaseValues() {
        LocalDate today = LocalDate.now();
        Movie movie = new Movie();
        movie.setMovieId(3);

        when(showtimeService.isDateInWindow(today)).thenReturn(true);
        when(showtimeService.getUpcomingDates(14)).thenReturn(List.of(today));
        when(movieService.getAllPublicMovies()).thenReturn(List.of(movie));
        when(movieService.getActiveGenres()).thenReturn(List.of());
        when(showtimeService.getRoomTypes()).thenReturn(List.of("Dolby Atmos"));
        when(movieService.normalizeAgeRating("T16")).thenReturn("T16");
        when(showtimeService.normalizeTimeSlot("evening")).thenReturn("evening");
        when(movieService.getAgeRatings()).thenReturn(MovieCatalogService.AGE_RATINGS);
        when(showtimeService.getShowtimes(today, 3, null, "T16", "Dolby Atmos", "evening")).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.listShowtimes(today, 3, 77, "T16", "dolby atmos", "evening", session, model);

        verify(showtimeService).getShowtimes(today, 3, null, "T16", "Dolby Atmos", "evening");
        assertEquals("Dolby Atmos", model.getAttribute("selectedRoomType"));
        assertEquals(true, model.getAttribute("hasFilters"));
    }
}
