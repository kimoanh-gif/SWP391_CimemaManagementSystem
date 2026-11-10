package com.cinema.service;

import com.cinema.dao.MovieDao;
import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.SeatTypeDao;
import com.cinema.dao.ShowtimeDao;
import com.cinema.dto.ShowtimeForm;
import com.cinema.exception.InvalidSeatLayoutException;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
import com.cinema.model.ManagerDashboardStats;
import com.cinema.model.report.RevenueReportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ManagerCriticalRisksFixesTest {

    // --- Task 1: Revenue Report Mocks ---
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private MovieService movieService;
    @Mock
    private GenreService genreService;
    @Mock
    private RoomService roomService;
    @Mock
    private TicketPricingService ticketPricingService;
    @Mock
    private FBItemService fbItemService;

    // --- Task 2: Seat Map Mocks ---
    @Mock
    private SeatDao seatDao;
    @Mock
    private RoomDao roomDao;
    @Mock
    private SeatTypeDao seatTypeDao;

    // --- Task 3: Showtime Mocks ---
    @Mock
    private ShowtimeDao showtimeDao;
    @Mock
    private MovieDao movieDao;

    private SeatService seatService;
    private ShowtimeService showtimeService;

    @BeforeEach
    void setUp() {
        seatService = new SeatService(seatDao, roomDao, seatTypeDao);
        showtimeService = new ShowtimeService(showtimeDao, movieDao, roomDao, seatDao);
        showtimeService.setTicketPricingService(ticketPricingService);
    }

    // =========================================================================
    // TASK 1: REVENUE REPORT & DASHBOARD TESTS
    // =========================================================================

    @Test
    @DisplayName("Task 1.1: Pure F&B orders do not inflate ticket revenue to total revenue")
    void testPureFbOrder_DoesNotInflateTicketRevenue() {
        when(movieService.getAllMovies(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(genreService.getAllGenres(any(), any())).thenReturn(Collections.emptyList());
        when(roomService.getAll(any(), any())).thenReturn(Collections.emptyList());
        when(ticketPricingService.getAllSeatTypes(any())).thenReturn(Collections.emptyList());
        when(ticketPricingService.getAllPricingRules(any(), any())).thenReturn(Collections.emptyList());
        when(fbItemService.getAllItems(any(), any(), any())).thenReturn(Collections.emptyList());

        // Table exists
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'Booking'"), eq(Integer.class))).thenReturn(1);
        // Paid bookings count = 1
        when(jdbcTemplate.queryForObject(contains("SELECT COUNT(*) FROM dbo.Booking WHERE UPPER([Status])"), eq(Integer.class))).thenReturn(1);
        // Total revenue = 120,000 VND
        when(jdbcTemplate.queryForObject(contains("COALESCE(SUM(TotalAmount), 0)"), eq(BigDecimal.class))).thenReturn(new BigDecimal("120000"));
        // Ticket revenue = 0 VND (no tickets in this order)
        when(jdbcTemplate.queryForObject(contains("COALESCE(SUM(bs.UnitPrice), 0)"), eq(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
        // F&B revenue = 120,000 VND
        when(jdbcTemplate.queryForObject(contains("COALESCE(SUM(bfb.Quantity * bfb.UnitPrice), 0)"), eq(BigDecimal.class))).thenReturn(new BigDecimal("120000"));
        // Booked seats count = 0
        when(jdbcTemplate.queryForObject(contains("COUNT(bs.BookingSeatID)"), eq(Integer.class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("COUNT(t.TicketID)"), eq(Integer.class))).thenReturn(0);
        // Capacity = 100
        when(jdbcTemplate.queryForObject(contains("COALESCE(SUM(r.Capacity), 0)"), eq(Integer.class))).thenReturn(100);

        ManagerDashboardService dashboardService = new ManagerDashboardService(
                movieService, genreService, roomService, showtimeService, ticketPricingService, fbItemService, jdbcTemplate
        );

        ManagerDashboardStats stats = dashboardService.getDashboardStats();

        assertEquals(new BigDecimal("120000"), stats.getTotalRevenue());
        assertEquals(new BigDecimal("120000"), stats.getFbRevenue());
        // CRITICAL CHECK: Ticket revenue MUST be 0, NOT fallbacked to 120,000!
        assertEquals(BigDecimal.ZERO, stats.getTicketRevenue(),
                "Ticket revenue must remain ZERO when no tickets are sold; must not copy totalRevenue.");
    }

    @Test
    @DisplayName("Task 1.2: Ticket & F&B percentages are mathematically bounded [0, 100%] even with heavy voucher discounts")
    void testRevenueReportSummary_PercentagesWithLargeVoucherDiscount() {
        RevenueReportSummary summary = new RevenueReportSummary();
        summary.setTicketRevenue(new BigDecimal("200000")); // Gross Ticket
        summary.setFbRevenue(new BigDecimal("50000"));     // Gross F&B
        summary.setDiscountAmount(new BigDecimal("150000"));// Large voucher discount
        summary.setTotalRevenue(new BigDecimal("100000"));  // Net Revenue collected

        // Gross total = 200,000 + 50,000 = 250,000
        assertEquals(new BigDecimal("250000"), summary.getGrossRevenue());

        // Ticket % of gross = 200,000 / 250,000 = 80.0%
        assertEquals(80.0, summary.getTicketGrossPercentage(), 0.01);

        // F&B % of gross = 50,000 / 250,000 = 20.0%
        assertEquals(20.0, summary.getFbGrossPercentage(), 0.01);

        // Sum of percentages must equal 100%
        assertEquals(100.0, summary.getTicketGrossPercentage() + summary.getFbGrossPercentage(), 0.01);

        // Test with 0 revenue
        RevenueReportSummary emptySummary = new RevenueReportSummary();
        assertEquals(0.0, emptySummary.getTicketGrossPercentage());
        assertEquals(0.0, emptySummary.getFbGrossPercentage());
    }

    // =========================================================================
    // TASK 2: SEAT MAP CONCURRENCY & BATCH ROLLBACK TESTS
    // =========================================================================

    @Test
    @DisplayName("Task 2.1 & 2.2: Batch update seats successfully updates all seats, locks room and bumps timestamp")
    void testBatchUpdateSeats_Success() {
        Room room = new Room();
        room.setRoomId(1);
        room.setUpdatedAt(LocalDateTime.of(2026, 11, 10, 10, 0, 0));
        when(roomDao.findById(1)).thenReturn(Optional.of(room));

        Seat s1 = new Seat();
        s1.setSeatId(101);
        s1.setRoomId(1);
        s1.setRowLabel("A");
        s1.setSeatNumber(1);
        s1.setSeatTypeName("Standard");
        s1.setStatus("Active");

        Seat s2 = new Seat();
        s2.setSeatId(102);
        s2.setRoomId(1);
        s2.setRowLabel("A");
        s2.setSeatNumber(2);
        s2.setSeatTypeName("Standard");
        s2.setStatus("Active");

        when(seatDao.findByRoomId(1)).thenReturn(List.of(s1, s2));

        SeatType vip = new SeatType();
        vip.setSeatTypeId(2);
        vip.setTypeName("VIP");
        when(seatTypeDao.findAll(anyString())).thenReturn(List.of(vip));

        when(seatDao.isSeatBookedInUpcomingShowtimes(anyInt(), eq(1))).thenReturn(false);

        LocalDateTime expected = LocalDateTime.of(2026, 11, 10, 10, 0, 0);
        List<Seat> result = seatService.batchUpdateSeats(1, List.of(101, 102), "VIP", "Active", expected);

        assertEquals(2, result.size());
        assertEquals("VIP", s1.getSeatTypeName());
        assertEquals("VIP", s2.getSeatTypeName());

        // Verify room was locked for update
        verify(roomDao).lockRoomForUpdate(1);
        // Verify both seats updated in DB
        verify(seatDao).update(s1);
        verify(seatDao).update(s2);
        // Verify room timestamp bumped
        verify(roomDao).updateRoomTimestamp(1);
    }

    @Test
    @DisplayName("Task 2.4 & 2.5: Atomicity - If one seat fails booking validation, entire batch fails before any update")
    void testBatchUpdateSeats_AtomicityRollbackOnConstraintFailure() {
        Room room = new Room();
        room.setRoomId(1);
        room.setUpdatedAt(LocalDateTime.of(2026, 11, 10, 10, 0, 0));
        when(roomDao.findById(1)).thenReturn(Optional.of(room));

        Seat s1 = new Seat();
        s1.setSeatId(101);
        s1.setRoomId(1);
        s1.setRowLabel("A");
        s1.setSeatNumber(1);
        s1.setSeatTypeName("Standard");
        s1.setStatus("Active");

        Seat s2 = new Seat();
        s2.setSeatId(102);
        s2.setRoomId(1);
        s2.setRowLabel("A");
        s2.setSeatNumber(2);
        s2.setSeatTypeName("Standard");
        s2.setStatus("Active");

        when(seatDao.findByRoomId(1)).thenReturn(List.of(s1, s2));

        SeatType vip = new SeatType();
        vip.setSeatTypeId(2);
        vip.setTypeName("VIP");
        when(seatTypeDao.findAll(anyString())).thenReturn(List.of(vip));

        // Seat 101 has NO bookings, but Seat 102 HAS BOOKINGS
        when(seatDao.isSeatBookedInUpcomingShowtimes(101, 1)).thenReturn(false);
        when(seatDao.isSeatBookedInUpcomingShowtimes(102, 1)).thenReturn(true);

        LocalDateTime expected = LocalDateTime.of(2026, 11, 10, 10, 0, 0);
        InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class, () ->
                seatService.batchUpdateSeats(1, List.of(101, 102), "VIP", "Active", expected));

        assertTrue(ex.getMessage().contains("A2"));
        assertTrue(ex.getMessage().contains("booked"));

        // CRITICAL CHECK: seatDao.update must NEVER be called for seat 101 or 102!
        verify(seatDao, never()).update(any());
        verify(roomDao, never()).updateRoomTimestamp(anyInt());
    }

    @Test
    @DisplayName("Task 2.6 & 2.7: Stale expectedUpdatedAt timestamp is strictly rejected (AC06)")
    void testBatchUpdateSeats_StaleVersionRejected() {
        Room room = new Room();
        room.setRoomId(1);
        // Room was updated at 10:05 by another manager
        room.setUpdatedAt(LocalDateTime.of(2026, 11, 10, 10, 5, 0));
        when(roomDao.findById(1)).thenReturn(Optional.of(room));

        // Current manager loaded room at 10:00 (stale by 5 minutes)
        LocalDateTime staleExpected = LocalDateTime.of(2026, 11, 10, 10, 0, 0);

        InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class, () ->
                seatService.batchUpdateSeats(1, List.of(101), "VIP", "Active", staleExpected));

        assertTrue(ex.getMessage().contains("updated by another user"));
        verify(seatDao, never()).update(any());
    }

    @Test
    @DisplayName("Task 2.1: Room.UpdatedAt = NULL allows initial save; subsequent save with NULL when Room.UpdatedAt is set is rejected")
    void testBatchUpdateSeats_NullVersionHandling() {
        // Case 1: Room.updatedAt is NULL (initial unversioned room). Initial save with null expectedUpdatedAt must succeed.
        Room roomInitial = new Room();
        roomInitial.setRoomId(1);
        roomInitial.setUpdatedAt(null);
        when(roomDao.findById(1)).thenReturn(Optional.of(roomInitial));

        Seat s1 = new Seat();
        s1.setSeatId(101);
        s1.setRoomId(1);
        s1.setRowLabel("A");
        s1.setSeatNumber(1);
        s1.setSeatTypeName("Standard");
        s1.setStatus("Active");
        when(seatDao.findByRoomId(1)).thenReturn(List.of(s1));

        SeatType vip = new SeatType();
        vip.setSeatTypeId(2);
        vip.setTypeName("VIP");
        when(seatTypeDao.findAll(anyString())).thenReturn(List.of(vip));
        when(seatDao.isSeatBookedInUpcomingShowtimes(101, 1)).thenReturn(false);

        assertDoesNotThrow(() -> seatService.batchUpdateSeats(1, List.of(101), "VIP", "Active", null));
        verify(roomDao).updateRoomTimestamp(1);

        // Case 2: Room.updatedAt is now NOT NULL (set to 10:00). Client submits with expectedUpdatedAt = NULL.
        // Must be rejected as stale/unversioned attempt.
        Room roomUpdated = new Room();
        roomUpdated.setRoomId(1);
        roomUpdated.setUpdatedAt(LocalDateTime.of(2026, 11, 10, 10, 0, 0));
        when(roomDao.findById(1)).thenReturn(Optional.of(roomUpdated));

        InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class, () ->
                seatService.batchUpdateSeats(1, List.of(101), "VIP", "Active", null));
        assertTrue(ex.getMessage().contains("updated by another user"));
    }

    // =========================================================================
    // TASK 3: SHOWTIME SCHEDULER DEADLOCK & CONCURRENCY TESTS
    // =========================================================================

    @Test
    @DisplayName("Task 3.1: updateShowtime locks rooms in deterministic ascending RoomID order to eliminate AB-BA deadlocks")
    void testUpdateShowtime_OrderedRoomLocking_PreventsDeadlock() {
        // Case A: Moving showtime from Room 2 to Room 1 (new = 1, old = 2)
        Showtime st1 = new Showtime();
        st1.setShowtimeId(501);
        st1.setRoomId(2);
        st1.setMovieId(10);
        st1.setStartTime(LocalDate.now().plusDays(2).atTime(14, 0));
        st1.setEndTime(LocalDate.now().plusDays(2).atTime(16, 0));
        when(showtimeDao.findById(501)).thenReturn(Optional.of(st1));

        ShowtimeForm formA = new ShowtimeForm();
        formA.setRoomId(1);
        formA.setMovieId(10);
        formA.setStartTime(st1.getStartTime());

        Movie movie = new Movie();
        movie.setMovieId(10);
        movie.setTitle("Avatar");
        movie.setStatus("Now Showing");
        movie.setDuration(120);
        when(movieDao.findById(10)).thenReturn(Optional.of(movie));

        Room room1 = new Room();
        room1.setRoomId(1);
        room1.setRoomName("Cinema 01");
        room1.setStatus("Ready");
        when(roomDao.findById(1)).thenReturn(Optional.of(room1));

        when(seatDao.countActiveSeatsByRoomId(1)).thenReturn(50);
        when(showtimeDao.findOverlappingShowtimes(eq(1), any(), any(), eq(501))).thenReturn(Collections.emptyList());

        showtimeService.updateShowtime(501, formA);

        InOrder inOrderA = inOrder(roomDao);
        // Room 1 (min) must be locked BEFORE Room 2 (max)
        inOrderA.verify(roomDao).lockRoomForUpdate(1);
        inOrderA.verify(roomDao).lockRoomForUpdate(2);

        // Case B: Moving showtime from Room 1 to Room 2 (new = 2, old = 1)
        reset(roomDao);
        Showtime st2 = new Showtime();
        st2.setShowtimeId(502);
        st2.setRoomId(1);
        st2.setMovieId(10);
        st2.setStartTime(LocalDate.now().plusDays(2).atTime(14, 0));
        st2.setEndTime(LocalDate.now().plusDays(2).atTime(16, 0));
        when(showtimeDao.findById(502)).thenReturn(Optional.of(st2));

        ShowtimeForm formB = new ShowtimeForm();
        formB.setRoomId(2);
        formB.setMovieId(10);
        formB.setStartTime(st2.getStartTime());

        Room room2 = new Room();
        room2.setRoomId(2);
        room2.setRoomName("Cinema 02");
        room2.setStatus("Ready");
        when(roomDao.findById(2)).thenReturn(Optional.of(room2));

        when(seatDao.countActiveSeatsByRoomId(2)).thenReturn(50);
        when(showtimeDao.findOverlappingShowtimes(eq(2), any(), any(), eq(502))).thenReturn(Collections.emptyList());

        showtimeService.updateShowtime(502, formB);

        InOrder inOrderB = inOrder(roomDao);
        // Room 1 (min) must STILL be locked BEFORE Room 2 (max)!
        inOrderB.verify(roomDao).lockRoomForUpdate(1);
        inOrderB.verify(roomDao).lockRoomForUpdate(2);
    }
}
