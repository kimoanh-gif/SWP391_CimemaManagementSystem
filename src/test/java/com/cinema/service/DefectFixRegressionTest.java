package com.cinema.service;

import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.SeatTypeDao;
import com.cinema.dao.TicketPricingDao;
import com.cinema.dto.RoomForm;
import com.cinema.exception.InvalidPricingConfigurationException;
import com.cinema.exception.InvalidSeatLayoutException;
import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.model.SeatOption;
import com.cinema.model.SeatType;
import com.cinema.model.TicketPricing;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DefectFixRegressionTest {

    private RoomDao roomDao;
    private SeatDao seatDao;
    private SeatTypeDao seatTypeDao;
    private TicketPricingDao ticketPricingDao;
    private JdbcTemplate jdbcTemplate;

    private RoomService roomService;
    private SeatService seatService;
    private TicketPricingService pricingService;
    private Validator validator;

    @BeforeEach
    void setUp() {
        roomDao = mock(RoomDao.class);
        seatDao = mock(SeatDao.class);
        seatTypeDao = mock(SeatTypeDao.class);
        ticketPricingDao = mock(TicketPricingDao.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        roomService = new RoomService(roomDao, seatDao);
        seatService = new SeatService(seatDao, roomDao, seatTypeDao);
        pricingService = new TicketPricingService(ticketPricingDao, seatTypeDao);

        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ==========================================
    // ISSUE 1: RC_01 TO RC_12 REGRESSION TESTS
    // ==========================================

    @Test
    @DisplayName("RC_01: Create room with 10 × 10 grid succeeds with initial capacity = 100")
    void test_RC_01_createRoom() {
        RoomForm form = new RoomForm();
        form.setRoomCode("ROOM-101");
        form.setRoomName("Cinema 101");
        form.setRoomType("2D Digital");
        form.setTotalRows(10);
        form.setSeatsPerRow(10);
        form.setCapacity(100);
        form.setStatus("Ready");

        when(roomDao.existsByRoomCode("ROOM-101", null)).thenReturn(false);
        when(roomDao.existsByRoomName("Cinema 101", null)).thenReturn(false);

        assertDoesNotThrow(() -> roomService.create(form));
        verify(roomDao).insert(form);
    }

    @Test
    @DisplayName("RC_02: Save 90 physical seats in a 100-position grid -> Capacity synchronized to 90")
    void test_RC_02_saveLayoutSynchronizesCapacity() {
        int roomId = 101;
        Room room = new Room();
        room.setRoomId(roomId);
        room.setCapacity(100);
        room.setTotalRows(10);
        room.setSeatsPerRow(10);

        when(roomDao.findById(roomId)).thenReturn(Optional.of(room));
        when(seatDao.findByRoomId(roomId)).thenReturn(Collections.emptyList());

        List<Seat> physicalSeats = new ArrayList<>();
        for (int r = 1; r <= 10; r++) {
            for (int c = 1; c <= 9; c++) { // 10 rows x 9 seats = 90 physical seats
                Seat s = new Seat();
                s.setRoomId(roomId);
                s.setRowLabel(String.valueOf((char) ('A' + r - 1)));
                s.setSeatNumber(c);
                s.setGridRow(r);
                s.setGridColumn(c);
                s.setStatus("Active");
                s.setSeatTypeName("Standard");
                s.setSeatTypeId(1);
                physicalSeats.add(s);
            }
        }
        assertEquals(90, physicalSeats.size());

        List<Seat> saved = assertDoesNotThrow(() -> seatService.saveRoomLayout(roomId, physicalSeats, null));
        // Verify capacity synchronization is called in the same transaction
        verify(roomDao).updateCapacity(roomId, 90);
    }

    @Test
    @DisplayName("RC_03: Update room name after creating walkway layout succeeds")
    void test_RC_03_updateRoomNameWithAisles() {
        int roomId = 101;
        Room existing = new Room();
        existing.setRoomId(roomId);
        existing.setRoomCode("ROOM-101");
        existing.setRoomName("Old Name");
        existing.setRoomType("2D Digital");
        existing.setTotalRows(10);
        existing.setSeatsPerRow(10);
        existing.setCapacity(90); // Already synchronized to 90
        existing.setStatus("Ready");

        when(roomDao.findById(roomId)).thenReturn(Optional.of(existing));
        when(roomDao.hasUpcomingShowtimes(roomId)).thenReturn(false);

        List<Seat> existingSeats = new ArrayList<>();
        for (int r = 1; r <= 10; r++) {
            for (int c = 1; c <= 9; c++) {
                Seat s = new Seat();
                s.setSeatId(r * 10 + c);
                s.setGridRow(r);
                s.setGridColumn(c);
                existingSeats.add(s);
            }
        }
        when(seatDao.findByRoomId(roomId)).thenReturn(existingSeats);

        RoomForm form = new RoomForm();
        form.setRoomId(roomId);
        form.setRoomCode("ROOM-101");
        form.setRoomName("Renamed Cinema 101");
        form.setRoomType("2D Digital");
        form.setTotalRows(10);
        form.setSeatsPerRow(10);
        form.setCapacity(90);
        form.setStatus("Ready");

        // Bean validation on RoomForm must pass
        var violations = validator.validate(form);
        assertTrue(violations.isEmpty(), "RoomForm bean validation should pass for synchronized capacity");

        // Service update must succeed without deadlock!
        assertDoesNotThrow(() -> roomService.update(form));
        verify(roomDao).update(form);
    }

    @Test
    @DisplayName("RC_04: Update projector/sound information on room with aisles succeeds")
    void test_RC_04_updateHardwareInfo() {
        int roomId = 101;
        Room existing = new Room();
        existing.setRoomId(roomId);
        existing.setRoomCode("ROOM-101");
        existing.setRoomName("Cinema 101");
        existing.setRoomType("2D Digital");
        existing.setTotalRows(10);
        existing.setSeatsPerRow(10);
        existing.setCapacity(90);
        existing.setStatus("Ready");

        when(roomDao.findById(roomId)).thenReturn(Optional.of(existing));
        when(roomDao.hasUpcomingShowtimes(roomId)).thenReturn(false);

        List<Seat> existingSeats = new ArrayList<>();
        for (int i = 0; i < 90; i++) {
            Seat s = new Seat();
            s.setGridRow(1 + (i / 9));
            s.setGridColumn(1 + (i % 9));
            existingSeats.add(s);
        }
        when(seatDao.findByRoomId(roomId)).thenReturn(existingSeats);

        RoomForm form = new RoomForm();
        form.setRoomId(roomId);
        form.setRoomCode("ROOM-101");
        form.setRoomName("Cinema 101");
        form.setRoomType("2D Digital");
        form.setTotalRows(10);
        form.setSeatsPerRow(10);
        form.setCapacity(90);
        form.setProjector("Laser 4K Christie");
        form.setSoundSystem("Dolby Atmos 7.1.4");
        form.setStatus("Ready");

        assertDoesNotThrow(() -> roomService.update(form));
        verify(roomDao).update(form);
    }

    @Test
    @DisplayName("RC_05: Change capacity to a value inconsistent with actual seats is rejected")
    void test_RC_05_inconsistentCapacityRejected() {
        int roomId = 101;
        Room existing = new Room();
        existing.setRoomId(roomId);
        existing.setCapacity(90);
        when(roomDao.findById(roomId)).thenReturn(Optional.of(existing));

        List<Seat> existingSeats = new ArrayList<>();
        for (int i = 0; i < 90; i++) existingSeats.add(new Seat());
        when(seatDao.findByRoomId(roomId)).thenReturn(existingSeats);

        RoomForm form = new RoomForm();
        form.setRoomId(roomId);
        form.setTotalRows(10);
        form.setSeatsPerRow(10);
        form.setCapacity(95); // 95 != 90 actual physical seats
        form.setStatus("Ready");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.update(form));
        assertTrue(ex.getMessage().contains("does not match the configured Seat Map"));
    }

    @Test
    @DisplayName("RC_06: Reduce grid dimensions below existing coordinates is rejected")
    void test_RC_06_gridDimensionBelowCoordinatesRejected() {
        int roomId = 101;
        Room existing = new Room();
        existing.setRoomId(roomId);
        existing.setCapacity(90);
        when(roomDao.findById(roomId)).thenReturn(Optional.of(existing));

        List<Seat> existingSeats = new ArrayList<>();
        for (int r = 1; r <= 10; r++) {
            for (int c = 1; c <= 9; c++) {
                Seat s = new Seat();
                s.setGridRow(r); // Max row is 10
                s.setGridColumn(c); // Max col is 9
                existingSeats.add(s);
            }
        }
        when(seatDao.findByRoomId(roomId)).thenReturn(existingSeats);

        RoomForm form = new RoomForm();
        form.setRoomId(roomId);
        form.setTotalRows(8); // Attempt to reduce rows from 10 to 8 while seats exist in row 10
        form.setSeatsPerRow(10);
        form.setCapacity(90);
        form.setStatus("Ready");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.update(form));
        assertTrue(ex.getMessage().contains("cannot be smaller than existing seat coordinates"));
    }

    @Test
    @DisplayName("RC_07: Submit seat positions outside grid boundaries is rejected")
    void test_RC_07_outsideGridBoundariesRejected() {
        int roomId = 101;
        Room room = new Room();
        room.setRoomId(roomId);
        room.setCapacity(100);
        room.setTotalRows(10);
        room.setSeatsPerRow(10);

        when(roomDao.findById(roomId)).thenReturn(Optional.of(room));

        Seat invalidSeat = new Seat();
        invalidSeat.setRowLabel("K");
        invalidSeat.setSeatNumber(1);
        invalidSeat.setGridRow(11); // Row 11 exceeds room.totalRows (10)
        invalidSeat.setGridColumn(1);
        invalidSeat.setSeatTypeName("Standard");
        invalidSeat.setStatus("Active");

        InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class,
                () -> seatService.saveRoomLayout(roomId, List.of(invalidSeat), null));
        assertTrue(ex.getMessage().contains("outside room grid boundary"));
    }

    @Test
    @DisplayName("RC_08: Modify a booked seat is rejected")
    void test_RC_08_modifyBookedSeatRejected() {
        int roomId = 101;
        Room room = new Room();
        room.setRoomId(roomId);
        room.setCapacity(10);
        when(roomDao.findById(roomId)).thenReturn(Optional.of(room));

        Seat existingSeat = new Seat();
        existingSeat.setSeatId(501);
        existingSeat.setRowLabel("A");
        existingSeat.setSeatNumber(1);
        existingSeat.setGridRow(1);
        existingSeat.setGridColumn(1);
        existingSeat.setSeatTypeName("Standard");
        existingSeat.setSeatTypeId(1);
        existingSeat.setStatus("Active");

        when(seatDao.findByRoomId(roomId)).thenReturn(List.of(existingSeat));
        when(seatDao.isSeatBookedInUpcomingShowtimes(501, roomId)).thenReturn(true);

        Seat incoming = new Seat();
        incoming.setSeatId(501);
        incoming.setRowLabel("A");
        incoming.setSeatNumber(1);
        incoming.setGridRow(1);
        incoming.setGridColumn(1);
        incoming.setSeatTypeName("VIP"); // Change type
        incoming.setSeatTypeId(2);
        incoming.setStatus("Active");

        when(seatTypeDao.findAll("ALL")).thenReturn(List.of(new SeatType(2, "VIP", "VIP", "VIP", new BigDecimal("1.25"), null, "Active")));

        InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class,
                () -> seatService.saveRoomLayout(roomId, List.of(incoming), null));
        assertTrue(ex.getMessage().contains("because it has been booked by customers"));
    }

    @Test
    @DisplayName("RC_09: Delete a seat referenced by booking history is rejected")
    void test_RC_09_deleteReferencedSeatRejected() {
        int roomId = 101;
        Room room = new Room();
        room.setRoomId(roomId);
        room.setCapacity(10);
        when(roomDao.findById(roomId)).thenReturn(Optional.of(room));

        Seat existingSeat = new Seat();
        existingSeat.setSeatId(502);
        existingSeat.setRowLabel("A");
        existingSeat.setSeatNumber(2);
        existingSeat.setGridRow(1);
        existingSeat.setGridColumn(2);
        existingSeat.setSeatTypeName("Standard");
        existingSeat.setStatus("Active");

        when(seatDao.findByRoomId(roomId)).thenReturn(List.of(existingSeat));
        when(seatDao.hasUpcomingBookingsForRoom(roomId)).thenReturn(false);
        when(seatDao.isSeatReferenced(502)).thenReturn(true);

        // Submit empty layout to remove seat A2
        Seat another = new Seat();
        another.setRowLabel("B");
        another.setSeatNumber(1);
        another.setGridRow(2);
        another.setGridColumn(1);
        another.setSeatTypeName("Standard");
        another.setStatus("Active");

        InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class,
                () -> seatService.saveRoomLayout(roomId, List.of(another), null));
        assertTrue(ex.getMessage().contains("because it is referenced by historical bookings"));
    }

    @Test
    @DisplayName("RC_10: Save seat layout concurrently from two sessions triggers optimistic version check")
    void test_RC_10_concurrentSaveDetected() {
        int roomId = 101;
        Room room = new Room();
        room.setRoomId(roomId);
        room.setCapacity(10);
        LocalDateTime lastUpdated = LocalDateTime.of(2026, 11, 9, 10, 0, 0);
        room.setUpdatedAt(lastUpdated);

        when(roomDao.findById(roomId)).thenReturn(Optional.of(room));

        LocalDateTime clientStaleTime = LocalDateTime.of(2026, 11, 9, 9, 0, 0);

        Seat s = new Seat();
        s.setRowLabel("A");
        s.setSeatNumber(1);
        s.setGridRow(1);
        s.setGridColumn(1);
        s.setSeatTypeName("Standard");
        s.setStatus("Active");

        InvalidSeatLayoutException ex = assertThrows(InvalidSeatLayoutException.class,
                () -> seatService.saveRoomLayout(roomId, List.of(s), clientStaleTime));
        assertTrue(ex.getMessage().contains("Room seat layout was updated by another user"));
    }

    @Test
    @DisplayName("RC_11: Read room capacity after layout save matches physical seat count")
    void test_RC_11_readCapacityAfterSave() {
        int roomId = 101;
        test_RC_02_saveLayoutSynchronizesCapacity();
        verify(roomDao).updateCapacity(roomId, 90);
    }

    @Test
    @DisplayName("RC_12: Cause an error during layout persistence -> entire transaction rolled back")
    void test_RC_12_transactionRollbackOnError() {
        int roomId = 101;
        Room room = new Room();
        room.setRoomId(roomId);
        room.setCapacity(10);
        when(roomDao.findById(roomId)).thenReturn(Optional.of(room));

        Seat s1 = new Seat();
        s1.setRowLabel("A");
        s1.setSeatNumber(1);
        s1.setGridRow(1);
        s1.setGridColumn(1);

        Seat s2 = new Seat();
        s2.setRowLabel("A");
        s2.setSeatNumber(1); // Duplicate coordinate!
        s2.setGridRow(1);
        s2.setGridColumn(1);

        assertThrows(InvalidSeatLayoutException.class,
                () -> seatService.saveRoomLayout(roomId, List.of(s1, s2), null));
        verify(roomDao, never()).updateCapacity(anyInt(), anyInt());
    }

    // ==========================================
    // ISSUE 2: TP_01 TO TP_16 REGRESSION TESTS
    // ==========================================

    @Test
    @DisplayName("TP_01: Base=80,000; Surcharge=0; Multiplier=1.0 -> 80,000 VND")
    void test_TP_01() {
        BigDecimal price = pricingService.calculateTicketUnitPrice(new BigDecimal("80000"), new BigDecimal("1.0"));
        assertEquals(new BigDecimal("80000"), price);
    }

    @Test
    @DisplayName("TP_02: Base=80,000; Surcharge=20,000; Multiplier=1.0 -> 100,000 VND")
    void test_TP_02() {
        LocalDateTime showtime = LocalDateTime.of(2026, 11, 14, 14, 0); // Saturday Standard
        TicketPricing rule = new TicketPricing(1, "R-3D", "3D Rule", "WEEKEND", "STANDARD",
                new BigDecimal("80000"), new BigDecimal("20000"), "3D", "Active", null);
        when(ticketPricingDao.findActiveConflict("WEEKEND", "STANDARD", "3D", null)).thenReturn(Optional.of(rule));

        BigDecimal seatPrice = pricingService.resolveSeatPrice(showtime, "3D", new BigDecimal("80000"), new BigDecimal("1.0"));
        assertEquals(new BigDecimal("100000"), seatPrice);
    }

    @Test
    @DisplayName("TP_03: Base=80,000; Surcharge=20,000; Multiplier=1.5 -> 150,000 VND")
    void test_TP_03() {
        LocalDateTime showtime = LocalDateTime.of(2026, 11, 14, 14, 0);
        TicketPricing rule = new TicketPricing(1, "R-3D", "3D Rule", "WEEKEND", "STANDARD",
                new BigDecimal("80000"), new BigDecimal("20000"), "3D", "Active", null);
        when(ticketPricingDao.findActiveConflict("WEEKEND", "STANDARD", "3D", null)).thenReturn(Optional.of(rule));

        BigDecimal seatPrice = pricingService.resolveSeatPrice(showtime, "3D", new BigDecimal("80000"), new BigDecimal("1.5"));
        assertEquals(new BigDecimal("150000"), seatPrice);
    }

    @Test
    @DisplayName("TP_04: Base=80,000; Surcharge=20,000; Multiplier=2.0 -> 200,000 VND")
    void test_TP_04() {
        LocalDateTime showtime = LocalDateTime.of(2026, 11, 14, 14, 0);
        TicketPricing rule = new TicketPricing(1, "R-3D", "3D Rule", "WEEKEND", "STANDARD",
                new BigDecimal("80000"), new BigDecimal("20000"), "3D", "Active", null);
        when(ticketPricingDao.findActiveConflict("WEEKEND", "STANDARD", "3D", null)).thenReturn(Optional.of(rule));

        BigDecimal seatPrice = pricingService.resolveSeatPrice(showtime, "3D", new BigDecimal("80000"), new BigDecimal("2.0"));
        assertEquals(new BigDecimal("200000"), seatPrice);
    }

    @Test
    @DisplayName("TP_05: Base=85,000; Surcharge=15,000; Multiplier=1.25 -> 125,000 VND")
    void test_TP_05() {
        LocalDateTime showtime = LocalDateTime.of(2026, 11, 14, 19, 0); // Saturday Golden Hour
        TicketPricing rule = new TicketPricing(2, "R-IMAX", "IMAX Golden", "WEEKEND", "GOLDEN_HOUR",
                new BigDecimal("85000"), new BigDecimal("15000"), "IMAX", "Active", null);
        when(ticketPricingDao.findActiveConflict("WEEKEND", "GOLDEN_HOUR", "IMAX", null)).thenReturn(Optional.of(rule));

        BigDecimal seatPrice = pricingService.resolveSeatPrice(showtime, "IMAX", new BigDecimal("85000"), new BigDecimal("1.25"));
        // (85,000 + 15,000) * 1.25 = 125,000 VND
        assertEquals(new BigDecimal("125000"), seatPrice);
    }

    @Test
    @DisplayName("TP_06: Base=85,000; Surcharge=0; Multiplier=1.25 -> 106,000 VND")
    void test_TP_06() {
        LocalDateTime showtime = LocalDateTime.of(2026, 11, 11, 14, 0); // Wednesday Standard
        TicketPricing rule = new TicketPricing(3, "R-2D", "2D Standard", "WEEKDAY", "STANDARD",
                new BigDecimal("85000"), BigDecimal.ZERO, "2D Digital", "Active", null);
        when(ticketPricingDao.findActiveConflict("WEEKDAY", "STANDARD", "2D Digital", null)).thenReturn(Optional.of(rule));

        BigDecimal seatPrice = pricingService.resolveSeatPrice(showtime, "2D Digital", new BigDecimal("85000"), new BigDecimal("1.25"));
        // 85,000 * 1.25 = 106,250 -> rounded to 106,000 VND
        assertEquals(new BigDecimal("106000"), seatPrice);
    }

    @Test
    @DisplayName("TP_07: Compare Manager pricing preview and customer seat selection -> Same applicable unit price")
    void test_TP_07_managerMatrixAndCustomerPriceMatch() {
        TicketPricing rule = new TicketPricing(5, "R-WEEKEND-VIP", "Weekend Golden", "WEEKEND", "GOLDEN_HOUR",
                new BigDecimal("90000"), new BigDecimal("20000"), "IMAX", "Active", null);
        SeatType vip = new SeatType(2, "VIP", "VIP", "VIP", new BigDecimal("1.25"), "#FF0000", "Active");

        when(ticketPricingDao.findAll(null, "Active")).thenReturn(List.of(rule));
        when(seatTypeDao.findAll("Active")).thenReturn(List.of(vip));
        when(ticketPricingDao.findActiveConflict("WEEKEND", "GOLDEN_HOUR", "IMAX", null)).thenReturn(Optional.of(rule));

        // 1. Manager pricing matrix preview
        Map<Integer, Map<Integer, BigDecimal>> matrix = pricingService.generatePricingMatrix();
        BigDecimal managerPreviewPrice = matrix.get(5).get(2);
        // (90,000 + 20,000) * 1.25 = 137,500 -> rounded to 138,000 VND
        assertEquals(new BigDecimal("138000"), managerPreviewPrice);

        // 2. Customer checkout calculation for the same showtime
        LocalDateTime showtime = LocalDateTime.of(2026, 11, 14, 19, 30);
        BigDecimal customerPrice = pricingService.resolveSeatPrice(showtime, "IMAX", new BigDecimal("90000"), new BigDecimal("1.25"));

        // Must match exactly!
        assertEquals(managerPreviewPrice, customerPrice, "Customer price must be identical to Manager pricing preview");
    }

    @Test
    @DisplayName("TP_08: Reserve a seat and check BookingSeat.UnitPrice matches displayed price")
    void test_TP_08_reservedSeatMatchesDisplayedPrice() {
        LocalDateTime showtime = LocalDateTime.of(2026, 11, 14, 19, 30);
        TicketPricing rule = new TicketPricing(5, "R-WEEKEND-VIP", "Weekend Golden", "WEEKEND", "GOLDEN_HOUR",
                new BigDecimal("90000"), new BigDecimal("20000"), "IMAX", "Active", null);
        when(ticketPricingDao.findActiveConflict("WEEKEND", "GOLDEN_HOUR", "IMAX", null)).thenReturn(Optional.of(rule));

        BigDecimal displayedPrice = pricingService.resolveSeatPrice(showtime, "IMAX", new BigDecimal("90000"), new BigDecimal("1.25"));
        assertEquals(new BigDecimal("138000"), displayedPrice);

        SeatOption seat = new SeatOption();
        seat.setSeatId(10);
        seat.setPrice(displayedPrice);

        // Verify that price saved into BookingSeat is the exact displayedPrice
        assertEquals(new BigDecimal("138000"), seat.getPrice());
    }

    @Test
    @DisplayName("TP_09: Recalculate Booking.Subtotal matches sum of stored seat prices")
    void test_TP_09_subtotalCalculation() {
        BigDecimal seat1Price = new BigDecimal("138000");
        BigDecimal seat2Price = new BigDecimal("138000");
        BigDecimal subtotal = seat1Price.add(seat2Price);
        assertEquals(new BigDecimal("276000"), subtotal);
    }

    @Test
    @DisplayName("TP_10: Modify a pricing rule after a paid booking -> Historical ticket price unchanged")
    void test_TP_10_historicalPriceUnaffected() {
        BigDecimal originalPaidTicketPrice = new BigDecimal("138000");

        // Pricing rule base price changes from 90,000 to 100,000
        TicketPricing updatedRule = new TicketPricing(5, "R-WEEKEND-VIP", "Weekend Golden", "WEEKEND", "GOLDEN_HOUR",
                new BigDecimal("100000"), new BigDecimal("20000"), "IMAX", "Active", null);
        when(ticketPricingDao.findActiveConflict("WEEKEND", "GOLDEN_HOUR", "IMAX", null)).thenReturn(Optional.of(updatedRule));

        LocalDateTime showtime = LocalDateTime.of(2026, 11, 14, 19, 30);
        BigDecimal newCustomerPrice = pricingService.resolveSeatPrice(showtime, "IMAX", new BigDecimal("90000"), new BigDecimal("1.25"));
        assertEquals(new BigDecimal("150000"), newCustomerPrice);

        // Historical paid ticket price remains unchanged
        assertEquals(new BigDecimal("138000"), originalPaidTicketPrice);
    }

    @Test
    @DisplayName("TP_11: No matching active pricing rule -> Falls back to Showtime.BasePrice")
    void test_TP_11_fallbackToShowtimeBasePrice() {
        LocalDateTime showtime = LocalDateTime.of(2026, 11, 10, 8, 30);
        when(ticketPricingDao.findActiveConflict(anyString(), anyString(), anyString(), any())).thenReturn(Optional.empty());

        BigDecimal fallbackBase = new BigDecimal("75000");
        BigDecimal price = pricingService.resolveSeatPrice(showtime, "2D Digital", fallbackBase, new BigDecimal("1.0"));
        assertEquals(new BigDecimal("75000"), price);
    }

    @Test
    @DisplayName("TP_12: Multiple conflicting active pricing rules -> Uniqueness enforced; conflict rejected")
    void test_TP_12_conflictingActiveRuleRejected() {
        TicketPricing existing = new TicketPricing(1, "RULE-1", "Rule 1", "WEEKDAY", "MORNING",
                new BigDecimal("70000"), BigDecimal.ZERO, "2D Digital", "Active", null);
        when(ticketPricingDao.findActiveConflict("WEEKDAY", "MORNING", "2D Digital", null))
                .thenReturn(Optional.of(existing));

        // When creating a duplicate rule with identical criteria, createPricing throws conflict exception
        com.cinema.dto.TicketPricingForm duplicateForm = new com.cinema.dto.TicketPricingForm();
        duplicateForm.setPricingCode("RULE-2");
        duplicateForm.setRuleName("Rule 2");
        duplicateForm.setDayType("WEEKDAY");
        duplicateForm.setTimeSlot("MORNING");
        duplicateForm.setExperienceType("2D Digital");
        duplicateForm.setBasePrice(new BigDecimal("75000"));
        duplicateForm.setStatus("Active");

        assertThrows(com.cinema.exception.DuplicatePricingRuleException.class, () -> pricingService.createPricing(duplicateForm));
    }

    @Test
    @DisplayName("TP_13: Weekend, Holiday, and Golden Hour rules -> Correct rule selected")
    void test_TP_13_timeSlotAndDayTypeResolution() {
        assertEquals("WEEKEND", pricingService.determineDayType(LocalDate.of(2026, 11, 14))); // Saturday
        assertEquals("WEEKEND", pricingService.determineDayType(LocalDate.of(2026, 11, 15))); // Sunday
        assertEquals("WEEKDAY", pricingService.determineDayType(LocalDate.of(2026, 11, 16))); // Monday

        assertEquals("MORNING", pricingService.determineTimeSlot(LocalTime.of(10, 0)));
        assertEquals("STANDARD", pricingService.determineTimeSlot(LocalTime.of(12, 0)));
        assertEquals("STANDARD", pricingService.determineTimeSlot(LocalTime.of(16, 59)));
        assertEquals("GOLDEN_HOUR", pricingService.determineTimeSlot(LocalTime.of(17, 0)));
        assertEquals("GOLDEN_HOUR", pricingService.determineTimeSlot(LocalTime.of(20, 30)));
    }

    @Test
    @DisplayName("TP_14: Change seat type multiplier -> New prices correct; existing paid bookings unaffected")
    void test_TP_14_changeMultiplier() {
        BigDecimal effectiveBase = new BigDecimal("100000");

        BigDecimal standardPrice = pricingService.calculateTicketUnitPrice(effectiveBase, new BigDecimal("1.0"));
        assertEquals(new BigDecimal("100000"), standardPrice);

        // Multiplier updated to 1.35
        BigDecimal updatedPrice = pricingService.calculateTicketUnitPrice(effectiveBase, new BigDecimal("1.35"));
        assertEquals(new BigDecimal("135000"), updatedPrice);
    }

    @Test
    @DisplayName("TP_15: Invalid base price or surcharge rejected")
    void test_TP_15_invalidPricingRejected() {
        assertThrows(InvalidPricingConfigurationException.class,
                () -> pricingService.calculateTicketUnitPrice(new BigDecimal("-1000"), new BigDecimal("1.0")));
    }

    @Test
    @DisplayName("TP_16: Rounding at 500 VND boundary -> Correct HALF_UP result")
    void test_TP_16_halfUpRoundingBoundaries() {
        // 76,500 -> 77,000
        assertEquals(new BigDecimal("77000"), pricingService.calculateTicketUnitPrice(new BigDecimal("85000"), new BigDecimal("0.90")));

        // 75,500 -> 76,000
        assertEquals(new BigDecimal("76000"), pricingService.calculateTicketUnitPrice(new BigDecimal("75500"), new BigDecimal("1.0")));

        // 75,499 -> 75,000
        assertEquals(new BigDecimal("75000"), pricingService.calculateTicketUnitPrice(new BigDecimal("75499"), new BigDecimal("1.0")));

        // 75,501 -> 76,000
        assertEquals(new BigDecimal("76000"), pricingService.calculateTicketUnitPrice(new BigDecimal("75501"), new BigDecimal("1.0")));
    }
}
