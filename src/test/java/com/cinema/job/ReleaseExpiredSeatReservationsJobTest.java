package com.cinema.job;

import com.cinema.dao.CustomerBookingDAO;
import com.cinema.dao.LoyaltyPointDAO;
import com.cinema.service.CustomerBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseExpiredSeatReservationsJobTest {

    @Mock
    private CustomerBookingDAO bookingDAO;

    @Mock
    private CustomerBookingService bookingService;

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

    @Test
    @DisplayName("P0-03: Job delegates to transactional CustomerBookingService when injected")
    void testExecute_DelegatesToTransactionalService() {
        ReleaseExpiredSeatReservationsJob serviceBackedJob =
                new ReleaseExpiredSeatReservationsJob(bookingDAO, bookingService);
        when(bookingService.releaseExpiredSeatReservations()).thenReturn(3);

        int released = serviceBackedJob.execute();

        assertEquals(3, released);
        verify(bookingService, times(1)).releaseExpiredSeatReservations();
        verifyNoInteractions(bookingDAO);
    }

    @Test
    @DisplayName("P0-03: Mid-execution SQL failure escapes @Transactional boundary to trigger rollback before job catches it")
    void testMidExecutionSqlFailure_PropagatesForRollbackAndJobHandlesCleanly() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CustomerBookingDAO realDao = new CustomerBookingDAO(jdbcTemplate, mock(LoyaltyPointDAO.class));
        reset(jdbcTemplate); // clear constructor schema check invocations

        // Statement 1 (Payment) succeeds, Statement 2 (Booking) succeeds, Statement 3 (F&B) fails
        when(jdbcTemplate.update(anyString()))
                .thenReturn(1)
                .thenReturn(1)
                .thenThrow(new DataAccessResourceFailureException("Deadlock on BookingFoodBeverage"));

        // Exception MUST escape CustomerBookingDAO.releaseExpiredSeatReservations() so Spring @Transactional rolls back
        assertThrows(DataAccessResourceFailureException.class, realDao::releaseExpiredSeatReservations);
        // Statement 4 (DELETE BookingSeat) must never be reached
        verify(jdbcTemplate, times(3)).update(anyString());

        // Verify @Transactional(rollbackFor = Exception.class) is on DAO and Service methods, not swallowed in Job
        Method daoMethod = CustomerBookingDAO.class.getMethod("releaseExpiredSeatReservations");
        Transactional daoTx = daoMethod.getAnnotation(Transactional.class);
        assertNotNull(daoTx, "CustomerBookingDAO.releaseExpiredSeatReservations must be @Transactional");

        Method serviceMethod = CustomerBookingService.class.getMethod("releaseExpiredSeatReservations");
        Transactional serviceTx = serviceMethod.getAnnotation(Transactional.class);
        assertNotNull(serviceTx, "CustomerBookingService.releaseExpiredSeatReservations must be @Transactional");

        Method jobMethod = ReleaseExpiredSeatReservationsJob.class.getMethod("execute");
        assertNull(jobMethod.getAnnotation(Transactional.class),
                "Job.execute() catches exceptions so @Transactional must be on the inner Service/DAO boundary");

        // Verify Job catches the escaping exception from the transactional layer and returns 0
        reset(jdbcTemplate);
        ReleaseExpiredSeatReservationsJob jobWithRealDao = new ReleaseExpiredSeatReservationsJob(realDao);
        when(jdbcTemplate.update(anyString()))
                .thenReturn(1)
                .thenThrow(new DataAccessResourceFailureException("Step 2 failed"));
        assertEquals(0, jobWithRealDao.execute());
    }

    @Test
    @DisplayName("P0-03: Confirmed, paid, and ticketed reservations remain protected in cleanup SQL")
    void testReleaseSqlProtectsConfirmedPaidAndTicketedBookings() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CustomerBookingDAO realDao = new CustomerBookingDAO(jdbcTemplate, mock(LoyaltyPointDAO.class));
        reset(jdbcTemplate);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(jdbcTemplate.update(sqlCaptor.capture())).thenReturn(1, 2, 1, 2);

        int count = realDao.releaseExpiredSeatReservations();
        assertEquals(2, count);

        List<String> sqlStatements = sqlCaptor.getAllValues();
        assertEquals(4, sqlStatements.size());
        String expireBookingSql = sqlStatements.get(1);
        assertTrue(expireBookingSql.contains("IN ('PENDING', 'PAYMENT_PENDING')"));
        assertTrue(expireBookingSql.contains("NOT EXISTS"));
        assertTrue(expireBookingSql.contains("'PAID'"));
        assertTrue(expireBookingSql.contains("dbo.Ticket"));
    }
}
