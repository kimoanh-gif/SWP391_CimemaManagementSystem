package com.cinema.job;

import com.cinema.dao.CustomerBookingDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseExpiredSeatReservationsJobTest {

    @Mock
    private CustomerBookingDAO bookingDAO;

    private ReleaseExpiredSeatReservationsJob job;

    @BeforeEach
    void setUp() {
        job = new ReleaseExpiredSeatReservationsJob(bookingDAO);
    }

    @Test
    @DisplayName("execute: Expired holds are successfully released")
    void testExecute_SuccessWithExpiredHolds() {
        when(bookingDAO.releaseExpiredSeatReservations()).thenReturn(4);

        int released = job.execute();

        assertEquals(4, released);
        verify(bookingDAO, times(1)).releaseExpiredSeatReservations();
    }

    @Test
    @DisplayName("execute: No expired holds completes cleanly with 0 released")
    void testExecute_NoExpiredHolds() {
        when(bookingDAO.releaseExpiredSeatReservations()).thenReturn(0);

        int released = job.execute();

        assertEquals(0, released);
        verify(bookingDAO, times(1)).releaseExpiredSeatReservations();
    }

    @Test
    @DisplayName("execute: Database access failure is handled safely without crashing (SRS 6.1)")
    void testExecute_DatabaseFailureHandledSafely() {
        when(bookingDAO.releaseExpiredSeatReservations())
                .thenThrow(new DataAccessResourceFailureException("Database unreachable"));

        int released = job.execute();

        assertEquals(0, released);
        verify(bookingDAO, times(1)).releaseExpiredSeatReservations();
    }

    @Test
    @DisplayName("execute: Repeated execution is idempotent and safe")
    void testExecute_Idempotent() {
        when(bookingDAO.releaseExpiredSeatReservations()).thenReturn(2, 0);

        int firstRun = job.execute();
        int secondRun = job.execute();

        assertEquals(2, firstRun);
        assertEquals(0, secondRun);
        verify(bookingDAO, times(2)).releaseExpiredSeatReservations();
    }
}
