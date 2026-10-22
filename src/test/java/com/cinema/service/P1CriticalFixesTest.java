package com.cinema.service;

import com.cinema.dao.MovieDao;
import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.SeatTypeDao;
import com.cinema.dao.ShowtimeDao;
import com.cinema.dao.TicketPricingDao;
import com.cinema.exception.DuplicatePricingRuleException;
import com.cinema.exception.InvalidPricingConfigurationException;
import com.cinema.exception.InvalidShowtimeException;
import com.cinema.model.FBItem;
import com.cinema.model.Genre;
import com.cinema.model.ManagerDashboardStats;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.model.SeatType;
import com.cinema.model.Showtime;
import com.cinema.dto.ShowtimeForm;
import com.cinema.model.TicketPricing;
import com.cinema.dto.TicketPricingForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite verifying all P1 Critical Business Requirements and Acceptance Criteria:
 * - P1-01: Room Readiness & Capacity Consistency
 * - P1-02: Normalize Seat Type Relationship (SeatTypeID FK)
 * - P1-03: Ticket Pricing Rule Consistency (Conflict detection, TimeSlot boundaries)
 * - P1-04: Manager Dashboard Real Data Aggregation & Empty Handling
 */
@ExtendWith(MockitoExtension.class)
public class P1CriticalFixesTest {

    // =========================================================================
    // TASK P1-01: ROOM READINESS & CAPACITY CONSISTENCY
    // =========================================================================
    @Nested
    @DisplayName("P1-01: Room Readiness & Capacity Consistency")
    class RoomReadinessTests {

        @Mock private ShowtimeDao showtimeDao;
        @Mock private MovieDao movieDao;
        @Mock private RoomDao roomDao;
        @Mock private SeatDao seatDao;

        @InjectMocks
        private ShowtimeService showtimeService;

        private Movie activeMovie;
        private Room roomWithoutSeats;
        private Room roomWithSeats;

        @BeforeEach
        void setUp() {
            activeMovie = new Movie(1, "Dune: Part Two", 165, "Now Showing");
            roomWithoutSeats = new Room(2, "ROOM-02", "Phòng 2 (Chưa ghế)", "Standard Laser", 120, "Ready");
            roomWithSeats = new Room(1, "ROOM-01", "Phòng 1 (Đã có ghế)", "Standard Laser", 96, "Ready");
            roomWithSeats.setActiveSeatCount(96);
        }

        @Test
        @DisplayName("AC01: Không cho tạo Showtime nếu phòng chiếu có 0 ghế khả dụng")
        void testCreateShowtime_RejectRoomWithoutSeats() {
            ShowtimeForm form = new ShowtimeForm();
            form.setMovieId(1);
            form.setRoomId(2);
            form.setStartTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0));
            form.setBaseTicketPrice(new BigDecimal("85000"));

            when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie));
            when(roomDao.findById(2)).thenReturn(Optional.of(roomWithoutSeats));
            when(seatDao.countActiveSeatsByRoomId(2)).thenReturn(0); // 0 usable seats

            InvalidShowtimeException ex = assertThrows(InvalidShowtimeException.class, () ->
                    showtimeService.createShowtime(form));

            assertTrue(ex.getMessage().contains("chưa có sơ đồ ghế khả dụng"));
            verify(showtimeDao, never()).insert(any());
        }

        @Test
        @DisplayName("AC02: Cho phép tạo Showtime nếu phòng có ghế khả dụng và hợp lệ")
        void testCreateShowtime_AllowRoomWithSeats() {
            LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
            ShowtimeForm form = new ShowtimeForm();
            form.setMovieId(1);
            form.setRoomId(1);
            form.setStartTime(start);
            form.setBaseTicketPrice(new BigDecimal("85000"));

            when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie));
            when(roomDao.findById(1)).thenReturn(Optional.of(roomWithSeats));
            when(seatDao.countActiveSeatsByRoomId(1)).thenReturn(96);
            when(showtimeDao.findOverlappingShowtimes(eq(1), any(), any(), isNull())).thenReturn(Collections.emptyList());
            when(showtimeDao.insert(any())).thenReturn(1);

            Showtime created = new Showtime(1, 1, 1, start, start.plusMinutes(165), new BigDecimal("85000"), "Scheduled");
            when(showtimeDao.findAll(any(), any(), any(), any())).thenReturn(List.of(created));

            Showtime result = showtimeService.createShowtime(form);
            assertNotNull(result);
            verify(showtimeDao).insert(form);
        }

        @Test
        @DisplayName("AC03: Room readiness method reflects both operational status and usable seat count")
        void testRoomReadinessDefinition() {
            Room readyRoomWithSeats = new Room(1, "R1", "Ready Room", "Standard", 100, "Ready");
            readyRoomWithSeats.setActiveSeatCount(100);
            assertTrue(readyRoomWithSeats.isReadyForScheduling());

            Room readyRoomNoSeats = new Room(2, "R2", "Empty Room", "Standard", 100, "Ready");
            readyRoomNoSeats.setActiveSeatCount(0);
            assertFalse(readyRoomNoSeats.isReadyForScheduling());

            Room maintRoomWithSeats = new Room(3, "R3", "Maint Room", "Standard", 100, "Maintenance");
            maintRoomWithSeats.setActiveSeatCount(100);
            assertFalse(maintRoomWithSeats.isReadyForScheduling());
        }
    }

    // =========================================================================
    // TASK P1-02: NORMALIZE SEAT TYPE RELATIONSHIP
    // =========================================================================
    @Nested
    @DisplayName("P1-02: Normalize Seat Type Relationship")
    class NormalizeSeatTypeTests {

        @Mock private TicketPricingDao ticketPricingDao;
        @Mock private SeatTypeDao seatTypeDao;

        @InjectMocks
        private TicketPricingService ticketPricingService;

        @Test
        @DisplayName("AC01 & AC03: Calculate seat price using SeatTypeID with fallback")
        void testCalculatePriceForSeat_WithSeatTypeId() {
            SeatType vip = new SeatType(2, "VIP", "Ghế VIP Royal Amber", "VIP Seat", new BigDecimal("1.25"), "#F59E0B", "Active");
            when(seatTypeDao.findById(2)).thenReturn(Optional.of(vip));

            BigDecimal price = ticketPricingService.calculatePriceForSeat(new BigDecimal("100000"), 2, "VIP");
            // 100,000 * 1.25 = 125,000
            assertEquals(new BigDecimal("125000"), price);
        }

        @Test
        @DisplayName("AC04: Calculate seat price fallback to SeatTypeName when SeatTypeID is null")
        void testCalculatePriceForSeat_FallbackToTypeName() {
            SeatType couple = new SeatType(3, "COUPLE", "Couple", "Couple Seat", new BigDecimal("1.80"), "#EC4899", "Active");
            when(seatTypeDao.findByName("Couple")).thenReturn(Optional.of(couple));

            BigDecimal price = ticketPricingService.calculatePriceForSeat(new BigDecimal("100000"), null, "Couple");
            // 100,000 * 1.80 = 180,000
            assertEquals(new BigDecimal("180000"), price);
        }

        @Test
        @DisplayName("Seat model preserves both SeatTypeID (FK) and SeatTypeName (compatibility)")
        void testSeatModelCompatibility() {
            Seat seat = new Seat(10, 1, 2, "VIP", "B", 5, 2, 5, "Active");
            assertEquals(10, seat.getSeatId());
            assertEquals(1, seat.getRoomId());
            assertEquals(2, seat.getSeatTypeId());
            assertEquals("VIP", seat.getSeatTypeName());
            assertEquals("B", seat.getRowLabel());
            assertEquals(5, seat.getSeatNumber());
        }
    }

    // =========================================================================
    // TASK P1-03: TICKET PRICING RULE CONSISTENCY
    // =========================================================================
    @Nested
    @DisplayName("P1-03: Ticket Pricing Rule Consistency")
    class TicketPricingConsistencyTests {

        @Mock private TicketPricingDao ticketPricingDao;
        @Mock private SeatTypeDao seatTypeDao;

        @InjectMocks
        private TicketPricingService ticketPricingService;

        @Test
        @DisplayName("AC01: Rejects duplicate Active pricing rule for (DayType, TimeSlot, ExperienceType)")
        void testCreatePricing_RejectDuplicateActiveRule() {
            TicketPricingForm form = new TicketPricingForm();
            form.setPricingCode("PRICE-WD-STD-2");
            form.setRuleName("Giá Tiêu Chuẩn Trùng Lặp");
            form.setDayType("WEEKDAY");
            form.setTimeSlot("STANDARD");
            form.setExperienceType("2D Digital");
            form.setBasePrice(new BigDecimal("85000"));
            form.setFormatSurcharge(BigDecimal.ZERO);
            form.setStatus("Active");

            when(ticketPricingDao.findByCode("PRICE-WD-STD-2")).thenReturn(Optional.empty());

            TicketPricing existingActive = new TicketPricing();
            existingActive.setPricingId(1);
            existingActive.setPricingCode("PRICE-WD-STD");
            existingActive.setRuleName("Giá Tiêu Chuẩn Ngày Thường");
            existingActive.setDayType("WEEKDAY");
            existingActive.setTimeSlot("STANDARD");
            existingActive.setExperienceType("2D Digital");
            existingActive.setStatus("Active");

            when(ticketPricingDao.findActiveConflict("WEEKDAY", "STANDARD", "2D Digital", null))
                    .thenReturn(Optional.of(existingActive));

            DuplicatePricingRuleException ex = assertThrows(DuplicatePricingRuleException.class, () ->
                    ticketPricingService.createPricing(form));

            assertTrue(ex.getMessage().contains("Đã tồn tại quy tắc giá đang kích hoạt"));
            assertTrue(ex.getMessage().contains("PRICE-WD-STD"));
        }

        @Test
        @DisplayName("AC02: Rejects activating an Inactive rule if an Active conflict exists")
        void testUpdatePricingStatus_RejectActivatingConflictRule() {
            TicketPricing inactiveRule = new TicketPricing();
            inactiveRule.setPricingId(5);
            inactiveRule.setPricingCode("PRICE-WD-STD-INACTIVE");
            inactiveRule.setRuleName("Quy tắc cũ tạm dừng");
            inactiveRule.setDayType("WEEKDAY");
            inactiveRule.setTimeSlot("STANDARD");
            inactiveRule.setExperienceType("2D Digital");
            inactiveRule.setStatus("Inactive");

            when(ticketPricingDao.findById(5)).thenReturn(Optional.of(inactiveRule));

            TicketPricing activeRule = new TicketPricing();
            activeRule.setPricingId(1);
            activeRule.setPricingCode("PRICE-WD-STD");
            activeRule.setRuleName("Giá Tiêu Chuẩn Đang Dùng");

            when(ticketPricingDao.findActiveConflict("WEEKDAY", "STANDARD", "2D Digital", 5))
                    .thenReturn(Optional.of(activeRule));

            DuplicatePricingRuleException ex = assertThrows(DuplicatePricingRuleException.class, () ->
                    ticketPricingService.updatePricingStatus(5, "Active"));

            assertTrue(ex.getMessage().contains("Vui lòng vô hiệu hóa quy tắc cũ trước khi kích hoạt quy tắc này"));
        }

        @Test
        @DisplayName("AC03: Time slot boundaries verification (< 12h MORNING, 12h-17h STANDARD, >= 17h GOLDEN_HOUR)")
        void testDetermineTimeSlotBoundaries() {
            // Before 12:00 -> MORNING
            assertEquals("MORNING", ticketPricingService.determineTimeSlot(LocalTime.of(8, 0)));
            assertEquals("MORNING", ticketPricingService.determineTimeSlot(LocalTime.of(11, 59, 59)));

            // 12:00 to 16:59:59 -> STANDARD
            assertEquals("STANDARD", ticketPricingService.determineTimeSlot(LocalTime.of(12, 0, 0)));
            assertEquals("STANDARD", ticketPricingService.determineTimeSlot(LocalTime.of(14, 30)));
            assertEquals("STANDARD", ticketPricingService.determineTimeSlot(LocalTime.of(16, 59, 59)));

            // 17:00 onwards -> GOLDEN_HOUR
            assertEquals("GOLDEN_HOUR", ticketPricingService.determineTimeSlot(LocalTime.of(17, 0, 0)));
            assertEquals("GOLDEN_HOUR", ticketPricingService.determineTimeSlot(LocalTime.of(20, 15)));
            assertEquals("GOLDEN_HOUR", ticketPricingService.determineTimeSlot(LocalTime.of(23, 59)));
        }
    }

    // =========================================================================
    // TASK P1-04: MANAGER DASHBOARD REAL DATA
    // =========================================================================
    @Nested
    @DisplayName("P1-04: Manager Dashboard Real Data Aggregation")
    class ManagerDashboardRealDataTests {

        @Mock private MovieService movieService;
        @Mock private GenreService genreService;
        @Mock private RoomService roomService;
        @Mock private ShowtimeService showtimeService;
        @Mock private TicketPricingService pricingService;
        @Mock private FBItemService fbItemService;
        @Mock private JdbcTemplate jdbcTemplate;

        @InjectMocks
        private ManagerDashboardService dashboardService;

        @Test
        @DisplayName("AC01 & AC04: Dashboard handles empty/missing transaction records safely without fake numbers")
        void testGetDashboardStats_SafeEmptyTransactions() {
            when(movieService.getAllMovies(isNull(), isNull(), isNull(), isNull())).thenReturn(Collections.emptyList());
            when(genreService.getAllGenres(isNull(), eq("ALL"))).thenReturn(Collections.emptyList());
            when(roomService.getAll(isNull(), isNull())).thenReturn(Collections.emptyList());
            when(showtimeService.getAllShowtimes(any(), isNull(), isNull(), isNull())).thenReturn(Collections.emptyList());
            when(pricingService.getAllSeatTypes(eq("ALL"))).thenReturn(Collections.emptyList());
            when(pricingService.getAllPricingRules(isNull(), eq("ALL"))).thenReturn(Collections.emptyList());
            when(pricingService.getAverageTicketPrice()).thenReturn(BigDecimal.ZERO);
            when(fbItemService.getAllItems(isNull(), isNull(), eq("ALL"))).thenReturn(Collections.emptyList());

            // Booking table does not exist or has 0 paid records
            when(jdbcTemplate.queryForObject(contains("INFORMATION_SCHEMA.TABLES"), eq(Integer.class))).thenReturn(0);

            ManagerDashboardStats stats = dashboardService.getDashboardStats();

            assertNotNull(stats);
            assertFalse(stats.isHasTransactionData());
            assertEquals(BigDecimal.ZERO, stats.getTotalRevenue());
            assertEquals(BigDecimal.ZERO, stats.getTicketRevenue());
            assertNull(stats.getSeatOccupancyRate()); // Renders as "Chưa có dữ liệu"
        }

        @Test
        @DisplayName("AC02 & AC03: When transactions exist, aggregate real DB revenue and occupancy rate")
        void testGetDashboardStats_WithRealTransactions() {
            when(movieService.getAllMovies(isNull(), isNull(), isNull(), isNull())).thenReturn(Collections.emptyList());
            when(genreService.getAllGenres(isNull(), eq("ALL"))).thenReturn(Collections.emptyList());
            when(roomService.getAll(isNull(), isNull())).thenReturn(Collections.emptyList());
            when(showtimeService.getAllShowtimes(any(), isNull(), isNull(), isNull())).thenReturn(Collections.emptyList());
            when(pricingService.getAllSeatTypes(eq("ALL"))).thenReturn(Collections.emptyList());
            when(pricingService.getAllPricingRules(isNull(), eq("ALL"))).thenReturn(Collections.emptyList());
            when(pricingService.getAverageTicketPrice()).thenReturn(new BigDecimal("100833"));
            when(fbItemService.getAllItems(isNull(), isNull(), eq("ALL"))).thenReturn(Collections.emptyList());

            when(jdbcTemplate.queryForObject(contains("INFORMATION_SCHEMA.TABLES"), eq(Integer.class))).thenReturn(1);
            when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM Booking WHERE Status"), eq(Integer.class))).thenReturn(10);
            when(jdbcTemplate.queryForObject(contains("SUM(TotalAmount)"), eq(BigDecimal.class))).thenReturn(new BigDecimal("1250000"));
            when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM Ticket t"), eq(Integer.class))).thenReturn(15);
            when(jdbcTemplate.queryForObject(contains("SUM(r.Capacity)"), eq(Integer.class))).thenReturn(100);

            ManagerDashboardStats stats = dashboardService.getDashboardStats();

            assertNotNull(stats);
            assertTrue(stats.isHasTransactionData());
            assertEquals(new BigDecimal("1250000"), stats.getTotalRevenue());
            assertEquals(15, stats.getTotalBookedTickets());
            // (15 booked / 100 capacity) * 100 = 15.0%
            assertEquals(15.0, stats.getSeatOccupancyRate());
            assertEquals(new BigDecimal("100833"), stats.getAverageTicketPrice());
        }
    }
}
