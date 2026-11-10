package com.cinema.integration;

import com.cinema.CinemaManagementApplication;
import com.cinema.dao.MovieDao;
import com.cinema.dao.RevenueReportDao;
import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.ShowtimeDao;
import com.cinema.dto.ShowtimeForm;
import com.cinema.exception.InvalidSeatLayoutException;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.model.Showtime;
import com.cinema.model.report.DailyRevenueItem;
import com.cinema.model.report.MovieRevenueItem;
import com.cinema.model.report.RevenueReportSummary;
import com.cinema.model.report.TransactionRevenueItem;
import com.cinema.service.SeatService;
import com.cinema.service.ShowtimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CinemaManagementApplication.class)
@ActiveProfiles("local")
public class ManagerCriticalRisksIntegrationTest {

    @Autowired
    private RevenueReportDao revenueReportDao;

    @Autowired
    private SeatService seatService;

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private RoomDao roomDao;

    @Autowired
    private SeatDao seatDao;

    @Autowired
    private ShowtimeDao showtimeDao;

    @Autowired
    private MovieDao movieDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private com.cinema.dao.AdminDashboardDao adminDashboardDao;

    @Autowired
    private com.cinema.service.ManagerDashboardService managerDashboardService;

    @Test
    @DisplayName("Point 4 - Revenue Consistency: RevenueReportDao, AdminDashboardDao, and ManagerDashboardService agree on SQL Server")
    void testRevenueReportDao_QueriesExecuteOnSqlServer() {
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.now().plusDays(1);

        // 1. Summary
        RevenueReportSummary summary = revenueReportDao.getSummary(start, end);
        assertNotNull(summary);
        assertNotNull(summary.getTotalRevenue());
        assertNotNull(summary.getTicketRevenue());
        assertNotNull(summary.getFbRevenue());
        assertTrue(summary.getTicketGrossPercentage() >= 0.0 && summary.getTicketGrossPercentage() <= 100.0);
        assertTrue(summary.getFbGrossPercentage() >= 0.0 && summary.getFbGrossPercentage() <= 100.0);

        // 2. Daily
        List<DailyRevenueItem> daily = revenueReportDao.getDailyRevenue(start, end);
        assertNotNull(daily);

        // 3. Movie
        List<MovieRevenueItem> movies = revenueReportDao.getMovieRevenue(start, end);
        assertNotNull(movies);

        // 4. Transactions
        List<TransactionRevenueItem> txs = revenueReportDao.getTransactions(start, end);
        assertNotNull(txs);

        // 5. Cross-check AdminDashboardDao and ManagerDashboardService against RevenueReportDao
        java.util.Map<String, Object> adminSummary = adminDashboardDao.summary(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        BigDecimal adminRev = (BigDecimal) adminSummary.get("revenue");
        int adminTickets = ((Number) adminSummary.get("tickets")).intValue();

        assertEquals(0, summary.getTotalRevenue().compareTo(adminRev),
                "RevenueReportDao and AdminDashboardDao must agree on total settled revenue");
        assertEquals(summary.getTotalTickets(), adminTickets,
                "RevenueReportDao and AdminDashboardDao must agree on total booked tickets");

        com.cinema.model.ManagerDashboardStats mgrStats = managerDashboardService.getDashboardStats();
        assertEquals(0, summary.getTotalRevenue().compareTo(mgrStats.getTotalRevenue()),
                "RevenueReportDao and ManagerDashboardService must agree on total settled revenue");
        assertEquals(0, summary.getTicketRevenue().compareTo(mgrStats.getTicketRevenue()),
                "RevenueReportDao and ManagerDashboardService must agree on ticket revenue");
        assertEquals(0, summary.getFbRevenue().compareTo(mgrStats.getFbRevenue()),
                "RevenueReportDao and ManagerDashboardService must agree on F&B revenue");
        assertTrue(revenueReportDao.countAnomalousPaymentRecords() >= 0);
    }

    @Test
    @DisplayName("Point 2 - Seat Map Rollback: First update succeeds in transaction but subsequent failure rolls back all changes in SQL Server")
    void testSeatMapTransactionRollback_WhenSubsequentOperationFails_NoPartialChanges() {
        List<Room> rooms = roomDao.findAll(null, null);
        assertFalse(rooms.isEmpty(), "At least one room must exist for test");
        Room room = rooms.get(0);
        int roomId = room.getRoomId();

        List<Seat> seats = seatDao.findByRoomId(roomId);
        assertTrue(seats.size() >= 2, "Room must have at least 2 seats for rollback test");

        Seat seat1 = seats.get(0);
        int seat1Id = seat1.getSeatId();
        String originalSeat1Status = jdbcTemplate.queryForObject(
                "SELECT Status FROM dbo.Seat WHERE SeatID = ?", String.class, seat1Id);

        // Target new status for seat1
        String testStatus = "Active".equalsIgnoreCase(originalSeat1Status) ? "Maintenance" : "Active";

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // Execute transaction where seat1 is updated, but subsequent operation causes a database-level FK error
        assertThrows(Exception.class, () -> {
            txTemplate.execute(status -> {
                // Step 1: Update Seat 1 in this transaction
                jdbcTemplate.update("UPDATE dbo.Seat SET Status = ? WHERE SeatID = ?", testStatus, seat1Id);

                // Step 2: Intentionally trigger a SQL Server foreign key constraint violation (Error 547)
                // by inserting a seat with non-existent SeatTypeID = 999999
                jdbcTemplate.update("""
                        INSERT INTO dbo.Seat (RoomID, RowLabel, SeatNumber, SeatTypeID, Status, CreatedAt)
                        VALUES (?, 'ZZ', 99, 999999, 'Active', SYSUTCDATETIME())
                        """, roomId);

                return null;
            });
        });

        // Verify in SQL Server that Seat 1 was rolled back and did NOT keep testStatus
        String seat1StatusAfterRollback = jdbcTemplate.queryForObject(
                "SELECT Status FROM dbo.Seat WHERE SeatID = ?", String.class, seat1Id);

        assertEquals(originalSeat1Status, seat1StatusAfterRollback,
                "SQL Server transaction must rollback seat1 update when subsequent operation fails; no partial changes allowed");

        // Verify that invalid seat ZZ99 was never committed
        Integer zzCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.Seat WHERE RoomID = ? AND RowLabel = 'ZZ' AND SeatNumber = 99",
                Integer.class, roomId);
        assertEquals(0, zzCount, "Failed insert must not exist in SQL Server");
    }

    @Test
    @DisplayName("Point 3 - Showtime Concurrency: Two independent transactions swap rooms concurrently with zero deadlocks and consistent schedules")
    void testShowtimeConcurrency_CrossRoomSwap_NoDeadlock_SchedulesConsistent() throws Exception {
        List<Room> rooms = roomDao.findAll(null, "Ready");
        if (rooms.size() < 2) {
            rooms = roomDao.findAll(null, null);
        }
        assertTrue(rooms.size() >= 2, "At least two rooms required for cross-room concurrency test");

        Room room1 = rooms.get(0);
        Room room2 = rooms.get(1);
        int r1 = Math.min(room1.getRoomId(), room2.getRoomId());
        int r2 = Math.max(room1.getRoomId(), room2.getRoomId());

        List<Movie> movies = movieDao.findAll(null, null, null, "Now Showing");
        if (movies.isEmpty()) {
            movies = movieDao.findAll(null, null, null, null);
        }
        assertFalse(movies.isEmpty(), "At least one movie required for test");
        Movie testMovie = movies.stream()
                .filter(m -> m.getEndDate() == null)
                .findFirst()
                .orElse(movies.get(0));
        int movieId = testMovie.getMovieId();

        // Target test date safely in the future without any existing schedules
        LocalDate testDate = testMovie.getEndDate() == null 
                ? LocalDate.now().plusMonths(3)
                : testMovie.getEndDate().minusDays(2);
        LocalDateTime st1Time = testDate.atTime(10, 0);
        LocalDateTime st2Time = testDate.atTime(14, 0);

        // Pre-clean any leftover test records
        jdbcTemplate.update("DELETE FROM dbo.Showtime WHERE MovieID = ? AND StartTime IN (?, ?)", movieId, st1Time, st2Time);

        // Insert initial test showtimes: ST1 in Room 1 (10:00), ST2 in Room 2 (14:00)
        ShowtimeForm initialForm1 = new ShowtimeForm();
        initialForm1.setRoomId(r1);
        initialForm1.setMovieId(movieId);
        initialForm1.setStartTime(st1Time);
        Showtime st1 = showtimeService.createShowtime(initialForm1);

        ShowtimeForm initialForm2 = new ShowtimeForm();
        initialForm2.setRoomId(r2);
        initialForm2.setMovieId(movieId);
        initialForm2.setStartTime(st2Time);
        Showtime st2 = showtimeService.createShowtime(initialForm2);

        int st1Id = st1.getShowtimeId();
        int st2Id = st2.getShowtimeId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicReference<Exception> thread1Error = new AtomicReference<>();
        AtomicReference<Exception> thread2Error = new AtomicReference<>();

        try {
            // Thread 1: moves ST1 from Room 1 (r1) to Room 2 (r2)
            Future<?> f1 = executor.submit(() -> {
                try {
                    startGate.await();
                    ShowtimeForm updateForm1 = new ShowtimeForm();
                    updateForm1.setRoomId(r2);
                    updateForm1.setMovieId(movieId);
                    updateForm1.setStartTime(st1Time);
                    showtimeService.updateShowtime(st1Id, updateForm1);
                } catch (Exception e) {
                    thread1Error.set(e);
                }
            });

            // Thread 2: moves ST2 from Room 2 (r2) to Room 1 (r1)
            Future<?> f2 = executor.submit(() -> {
                try {
                    startGate.await();
                    ShowtimeForm updateForm2 = new ShowtimeForm();
                    updateForm2.setRoomId(r1);
                    updateForm2.setMovieId(movieId);
                    updateForm2.setStartTime(st2Time);
                    showtimeService.updateShowtime(st2Id, updateForm2);
                } catch (Exception e) {
                    thread2Error.set(e);
                }
            });

            // Fire both threads simultaneously
            startGate.countDown();
            f1.get(15, TimeUnit.SECONDS);
            f2.get(15, TimeUnit.SECONDS);

            // Verify neither thread suffered SQL Server Deadlock Error 1205
            if (thread1Error.get() != null) {
                fail("Thread 1 encountered unexpected error: " + thread1Error.get().getMessage(), thread1Error.get());
            }
            if (thread2Error.get() != null) {
                fail("Thread 2 encountered unexpected error: " + thread2Error.get().getMessage(), thread2Error.get());
            }

            // Verify in SQL Server that ST1 is now in Room 2, and ST2 is now in Room 1
            Integer finalRoomSt1 = jdbcTemplate.queryForObject(
                    "SELECT RoomID FROM dbo.Showtime WHERE ShowtimeID = ?", Integer.class, st1Id);
            Integer finalRoomSt2 = jdbcTemplate.queryForObject(
                    "SELECT RoomID FROM dbo.Showtime WHERE ShowtimeID = ?", Integer.class, st2Id);

            assertEquals(r2, finalRoomSt1, "Showtime 1 must have cleanly moved to Room 2");
            assertEquals(r1, finalRoomSt2, "Showtime 2 must have cleanly moved to Room 1");

            // Verify zero overlaps in both rooms
            List<Showtime> overlapsR1 = showtimeDao.findOverlappingShowtimes(r1, st2Time, st2Time.plusMinutes(120), st2Id);
            List<Showtime> overlapsR2 = showtimeDao.findOverlappingShowtimes(r2, st1Time, st1Time.plusMinutes(120), st1Id);
            assertTrue(overlapsR1.isEmpty(), "Room 1 must have zero overlapping schedules");
            assertTrue(overlapsR2.isEmpty(), "Room 2 must have zero overlapping schedules");

        } finally {
            executor.shutdownNow();
            // Safe cleanup of test records
            jdbcTemplate.update("DELETE FROM dbo.Showtime WHERE ShowtimeID IN (?, ?)", st1Id, st2Id);
        }
    }
}


