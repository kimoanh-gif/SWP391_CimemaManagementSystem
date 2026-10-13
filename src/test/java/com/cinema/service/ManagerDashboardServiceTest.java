package com.cinema.service;

import com.cinema.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerDashboardServiceTest {

    @Mock
    private MovieService movieService;

    @Mock
    private GenreService genreService;

    @Mock
    private RoomService roomService;

    @Mock
    private ShowtimeService showtimeService;

    @Mock
    private TicketPricingService pricingService;

    @Mock
    private FBItemService fbItemService;

    @InjectMocks
    private ManagerDashboardService dashboardService;

    @Test
    @DisplayName("getDashboardStats: Should aggregate real operational metrics correctly")
    void testGetDashboardStats_Success() {
        Movie m1 = new Movie();
        m1.setMovieId(1);
        m1.setTitle("Avatar 2");
        m1.setStatus("Now Showing");

        Movie m2 = new Movie();
        m2.setMovieId(2);
        m2.setTitle("Dune 3");
        m2.setStatus("Upcoming");

        when(movieService.getAllMovies(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(m1, m2));

        Genre g1 = new Genre(1, "Action", "Action movies", "Active");
        when(genreService.getAllGenres(isNull(), eq("ALL"))).thenReturn(List.of(g1));

        Room r1 = new Room();
        r1.setRoomId(1);
        r1.setStatus("Ready");
        when(roomService.getAll(isNull(), isNull())).thenReturn(List.of(r1));

        when(showtimeService.getAllShowtimes(any(LocalDate.class), isNull(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        SeatType st1 = new SeatType(1, "STD", "Standard", null, BigDecimal.ONE, null, "Active");
        when(pricingService.getAllSeatTypes("ALL")).thenReturn(List.of(st1));

        TicketPricing tp = new TicketPricing();
        tp.setPricingCode("PRICE-WD-STD");
        tp.setBasePrice(new BigDecimal("85000"));
        when(pricingService.getAllPricingRules(isNull(), eq("ALL"))).thenReturn(List.of(tp));
        when(pricingService.getAverageTicketPrice()).thenReturn(new BigDecimal("87500"));

        FBItem item1 = new FBItem();
        item1.setItemId(1);
        item1.setStatus("Active");
        when(fbItemService.getAllItems(isNull(), isNull(), eq("ALL"))).thenReturn(List.of(item1));

        ManagerDashboardStats stats = dashboardService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(2, stats.getTotalMovies());
        assertEquals(1, stats.getNowShowingMovies());
        assertEquals(1, stats.getUpcomingMovies());
        assertEquals(1, stats.getTotalGenres());
        assertEquals(1, stats.getTotalRooms());
        assertEquals(1, stats.getActiveRooms());
        assertEquals(0, stats.getTodayShowtimesCount());
        assertEquals(1, stats.getTotalSeatTypes());
        assertEquals(1, stats.getTotalPricingRules());
        assertEquals(new BigDecimal("85000"), stats.getBaseTicketPrice());
        assertEquals(new BigDecimal("87500"), stats.getAverageTicketPrice());
        assertEquals(1, stats.getTotalFBItems());
        assertEquals(1, stats.getActiveFBItems());
    }
}
