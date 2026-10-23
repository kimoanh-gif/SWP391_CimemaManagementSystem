package com.cinema.service;

import com.cinema.dao.PublicShowtimeDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicShowtimeServiceTest {

    @Mock
    private PublicShowtimeDAO showtimeDAO;

    private PublicShowtimeService service;

    @BeforeEach
    void setUp() {
        service = new PublicShowtimeService(showtimeDAO);
    }

    @Test
    void testDateWindow() {
        LocalDate today = LocalDate.now();
        assertTrue(service.isDateInWindow(today));
        assertTrue(service.isDateInWindow(today.plusDays(13)));
        assertFalse(service.isDateInWindow(today.plusDays(14)));
        assertFalse(service.isDateInWindow(today.minusDays(1)));
        assertFalse(service.isDateInWindow(null));
    }

    @Test
    void testUnknownTimeSlotBecomesAll() {
        assertEquals("ALL", service.normalizeTimeSlot("midnight"));
        assertEquals("evening", service.normalizeTimeSlot("EVENING"));
    }

    @Test
    void testGetShowtimesPassesAllFilters() {
        LocalDate date = LocalDate.now();
        when(showtimeDAO.findShowtimes(date, 1, 2, "T13", "Dolby Atmos", "morning")).thenReturn(List.of());

        service.getShowtimes(date, 1, 2, "T13", "Dolby Atmos", "Morning");

        verify(showtimeDAO).findShowtimes(date, 1, 2, "T13", "Dolby Atmos", "morning");
    }
}
