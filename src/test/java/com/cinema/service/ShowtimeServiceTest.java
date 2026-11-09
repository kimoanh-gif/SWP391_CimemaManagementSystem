package com.cinema.service;

import com.cinema.dao.MovieDao;
import com.cinema.dao.RoomDao;
import com.cinema.dao.ShowtimeDao;
import com.cinema.exception.InvalidShowtimeException;
import com.cinema.exception.ShowtimeConflictException;
import com.cinema.model.Movie;
import com.cinema.model.Room;
import com.cinema.model.Showtime;
import com.cinema.dto.ShowtimeForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShowtimeServiceTest {

    @Mock
    private ShowtimeDao showtimeDao;

    @Mock
    private MovieDao movieDao;

    @Mock
    private RoomDao roomDao;

    @Mock
    private com.cinema.dao.SeatDao seatDao;

    @InjectMocks
    private ShowtimeService showtimeService;

    private Movie activeMovie;
    private Movie archivedMovie;
    private Room activeRoom;
    private Room maintenanceRoom;

    @BeforeEach
    void setUp() {
        activeMovie = new Movie(1, "Avatar 2", 180, "Now Showing");
        archivedMovie = new Movie(2, "Old Movie", 90, "Archived");

        activeRoom = new Room(1, "ROOM-01", "Phòng 1", "Standard Laser", 100, "Ready");
        maintenanceRoom = new Room(2, "ROOM-02", "Phòng 2", "Standard Laser", 80, "Maintenance");
    }

    @Test
    void testCreateShowtime_Success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(1);
        form.setRoomId(1);
        form.setStartTime(start);
        form.setBaseTicketPrice(new BigDecimal("85000"));

        when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie));
        when(roomDao.findById(1)).thenReturn(Optional.of(activeRoom));
        when(seatDao.countActiveSeatsByRoomId(1)).thenReturn(96);
        when(showtimeDao.findOverlappingShowtimes(eq(1), any(), any(), isNull()))
                .thenReturn(Collections.emptyList());
        when(showtimeDao.insert(any())).thenReturn(1);

        Showtime mockCreated = new Showtime(10, 1, 1, start, start.plusMinutes(180), new BigDecimal("85000"), "Scheduled");
        when(showtimeDao.findAll(any(), any(), any(), any())).thenReturn(List.of(mockCreated));

        Showtime created = showtimeService.createShowtime(form);
        assertNotNull(created);
        // Under simplified business rule: Film End = Start + 180m movie duration = start + 180m (no trailer)
        assertEquals(start.plusMinutes(180), form.getEndTime());
        verify(showtimeDao).insert(form);
    }

    @Test
    void testCreateShowtime_RejectArchivedMovie_BR07() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(2);
        form.setRoomId(1);
        form.setStartTime(start);
        form.setBaseTicketPrice(new BigDecimal("85000"));

        when(movieDao.findById(2)).thenReturn(Optional.of(archivedMovie));

        InvalidShowtimeException ex = assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.createShowtime(form));
        assertTrue(ex.getMessage().contains("BR-07"));
    }

    @Test
    void testCreateShowtime_RejectMaintenanceRoom_BR08() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(1);
        form.setRoomId(2);
        form.setStartTime(start);
        form.setBaseTicketPrice(new BigDecimal("85000"));

        when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie));
        when(roomDao.findById(2)).thenReturn(Optional.of(maintenanceRoom));

        InvalidShowtimeException ex = assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.createShowtime(form));
        assertTrue(ex.getMessage().contains("BR-08"));
    }

    @Test
    void testCreateShowtime_OverlapConflict_BR09() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(1);
        form.setRoomId(1);
        form.setStartTime(start);
        form.setBaseTicketPrice(new BigDecimal("85000"));

        when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie));
        when(roomDao.findById(1)).thenReturn(Optional.of(activeRoom));
        when(seatDao.countActiveSeatsByRoomId(1)).thenReturn(96);

        Showtime existing = new Showtime(99, 1, 1, start.plusMinutes(60), start.plusMinutes(200), new BigDecimal("85000"), "Scheduled");
        existing.setMovieTitle("Avatar 2");

        when(showtimeDao.findOverlappingShowtimes(eq(1), any(), any(), isNull()))
                .thenReturn(List.of(existing));

        ShowtimeConflictException ex = assertThrows(ShowtimeConflictException.class, () ->
                showtimeService.createShowtime(form));
        assertTrue(ex.getMessage().contains("BR-09"));
    }

    @Test
    void testCreateShowtime_RejectRoomWithoutSeats_P1_01() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(1);
        form.setRoomId(1);
        form.setStartTime(start);
        form.setBaseTicketPrice(new BigDecimal("85000"));

        when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie));
        when(roomDao.findById(1)).thenReturn(Optional.of(activeRoom));
        when(seatDao.countActiveSeatsByRoomId(1)).thenReturn(0); // 0 usable seats!

        InvalidShowtimeException ex = assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.createShowtime(form));
        assertTrue(ex.getMessage().contains("does not have a valid seat layout"));
    }

    @Test
    void testUpdateShowtime_RejectModificationWithBookings_BR54() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        ShowtimeForm form = new ShowtimeForm();
        form.setShowtimeId(10);
        form.setMovieId(1);
        form.setRoomId(2); // Changed room
        form.setStartTime(start);
        form.setBaseTicketPrice(new BigDecimal("90000"));

        Showtime existing = new Showtime(10, 1, 1, start, start.plusMinutes(120), new BigDecimal("85000"), "Scheduled");

        when(showtimeDao.findById(10)).thenReturn(Optional.of(existing));
        when(showtimeDao.hasActiveBookings(10)).thenReturn(true);

        InvalidShowtimeException ex = assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.updateShowtime(10, form));
        assertTrue(ex.getMessage().contains("BR-54"));
    }

    @Test
    void testDeleteShowtime_Success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Showtime existing = new Showtime(10, 1, 1, start, start.plusMinutes(120), new BigDecimal("85000"), "Scheduled");
        when(showtimeDao.findById(10)).thenReturn(Optional.of(existing));
        when(showtimeDao.hasAnyBookings(10)).thenReturn(false);

        assertDoesNotThrow(() -> showtimeService.deleteShowtime(10));
        verify(showtimeDao).delete(10);
    }

    @Test
    void testDeleteShowtime_WithBookings_ThrowsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Showtime existing = new Showtime(10, 1, 1, start, start.plusMinutes(120), new BigDecimal("85000"), "Scheduled");
        when(showtimeDao.findById(10)).thenReturn(Optional.of(existing));
        when(showtimeDao.hasAnyBookings(10)).thenReturn(true);

        assertThrows(InvalidShowtimeException.class, () -> showtimeService.deleteShowtime(10));
        verify(showtimeDao, never()).delete(anyInt());
    }

    @Test
    void testCreateShowtime_RejectTodayAndPastDates_BR55() {
        // Test with today's date
        ShowtimeForm formToday = new ShowtimeForm();
        formToday.setMovieId(1);
        formToday.setRoomId(1);
        formToday.setStartTime(LocalDateTime.now().withHour(23).withMinute(59));
        formToday.setBaseTicketPrice(new BigDecimal("85000"));

        InvalidShowtimeException exToday = assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.createShowtime(formToday));
        assertTrue(exToday.getMessage().contains("Cannot select a past or today date"));

        // Test with yesterday's date
        ShowtimeForm formPast = new ShowtimeForm();
        formPast.setMovieId(1);
        formPast.setRoomId(1);
        formPast.setStartTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0));
        formPast.setBaseTicketPrice(new BigDecimal("85000"));

        InvalidShowtimeException exPast = assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.createShowtime(formPast));
        assertTrue(exPast.getMessage().contains("Cannot select a past or today date"));
    }

    @Test
    void testCreateShowtime_RejectBeforeMovieReleaseDate_ST025() {
        Movie movie = new Movie();
        movie.setMovieId(5);
        movie.setTitle("Future Blockbuster");
        movie.setStatus("Upcoming");
        movie.setDuration(120);
        movie.setReleaseDate(LocalDate.now().plusDays(10)); // Release is in 10 days

        when(movieDao.findById(5)).thenReturn(Optional.of(movie));

        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(5);
        form.setRoomId(1);
        form.setStartTime(LocalDateTime.now().plusDays(5).withHour(10).withMinute(0)); // 5 days before release
        form.setBaseTicketPrice(new BigDecimal("85000"));

        InvalidShowtimeException ex = assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.createShowtime(form));
        assertTrue(ex.getMessage().contains("movie release date"));
    }

    @Test
    @DisplayName("P0-02: ShowtimeDao.insert executes SQL with BasePrice column, not BaseTicketPrice")
    void testShowtimeDaoInsert_UsesBasePriceColumn() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ShowtimeDao realDao = new ShowtimeDao(jdbcTemplate);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        ShowtimeForm form = new ShowtimeForm();
        form.setMovieId(1);
        form.setRoomId(1);
        form.setStartTime(start);
        form.setEndTime(start.plusMinutes(120));
        form.setBaseTicketPrice(new BigDecimal("90000"));
        form.setStatus("Scheduled");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(jdbcTemplate.update(sqlCaptor.capture(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        int rows = realDao.insert(form);
        assertEquals(1, rows);
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("BasePrice"));
        assertFalse(sql.contains("BaseTicketPrice"));
    }

    @Test
    @DisplayName("P0-02: ShowtimeDao.update executes SQL with BasePrice column, not BaseTicketPrice")
    void testShowtimeDaoUpdate_UsesBasePriceColumn() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ShowtimeDao realDao = new ShowtimeDao(jdbcTemplate);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        ShowtimeForm form = new ShowtimeForm();
        form.setShowtimeId(10);
        form.setMovieId(1);
        form.setRoomId(1);
        form.setStartTime(start);
        form.setEndTime(start.plusMinutes(120));
        form.setBaseTicketPrice(new BigDecimal("95000"));
        form.setStatus("Scheduled");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        when(jdbcTemplate.update(sqlCaptor.capture(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        int rows = realDao.update(10, form);
        assertEquals(1, rows);
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("BasePrice = ?"));
        assertFalse(sql.contains("BaseTicketPrice"));
    }

    @Test
    @DisplayName("P0-02: Reading showtime maps BasePrice AS BaseTicketPrice into Showtime.baseTicketPrice")
    @SuppressWarnings("unchecked")
    void testShowtimeDaoRead_MapsBaseTicketPriceCorrectly() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ShowtimeDao realDao = new ShowtimeDao(jdbcTemplate);

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0);
        ResultSet rs = mock(ResultSet.class);
        lenient().when(rs.getString(anyString())).thenReturn("");
        lenient().when(rs.getInt(anyString())).thenReturn(1);
        lenient().when(rs.getTimestamp(anyString())).thenReturn(Timestamp.valueOf(start));
        when(rs.getBigDecimal("BaseTicketPrice")).thenReturn(new BigDecimal("110000.00"));

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenAnswer(inv -> {
                    RowMapper<Showtime> mapper = inv.getArgument(2);
                    return List.of(mapper.mapRow(rs, 0));
                });

        Optional<Showtime> found = realDao.findById(25);
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("110000.00"), found.get().getBaseTicketPrice());
    }

    @Test
    @DisplayName("Batch Save: Successfully creates multiple showtimes in one room on the same day")
    void testCreateShowtimeBatch_Success_MultipleSlots() {
        when(roomDao.findById(1)).thenReturn(Optional.of(activeRoom));
        when(seatDao.countActiveSeatsByRoomId(1)).thenReturn(100);
        when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie)); // Duration 180m (09:00 - 12:00 + 15m buf -> 12:15)

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(2);
        ShowtimeForm slot1 = new ShowtimeForm();
        slot1.setMovieId(1);
        slot1.setRoomId(1);
        slot1.setStartTime(tomorrow.withHour(9).withMinute(0).withSecond(0).withNano(0));
        slot1.setBaseTicketPrice(new BigDecimal("85000"));

        // Slot 2: starts at 12:30 (after 12:15)
        ShowtimeForm slot2 = new ShowtimeForm();
        slot2.setMovieId(1);
        slot2.setRoomId(1);
        slot2.setStartTime(tomorrow.withHour(12).withMinute(30).withSecond(0).withNano(0));
        slot2.setBaseTicketPrice(new BigDecimal("90000"));

        Showtime created1 = new Showtime();
        created1.setShowtimeId(101);
        created1.setMovieTitle("Avatar 2");
        Showtime created2 = new Showtime();
        created2.setShowtimeId(102);
        created2.setMovieTitle("Avatar 2");

        when(showtimeDao.findOverlappingShowtimes(eq(1), any(), any(), isNull()))
                .thenReturn(Collections.emptyList(), List.of(created1), Collections.emptyList(), List.of(created2));

        List<Showtime> result = showtimeService.createShowtimeBatch(1, List.of(slot1, slot2));

        assertEquals(2, result.size());
        verify(roomDao).lockRoomForUpdate(1);
        verify(showtimeDao, times(2)).insert(any());
    }

    @Test
    @DisplayName("Batch Save: Detects and rejects Draft vs Draft conflict including 15-minute cleaning buffer")
    void testCreateShowtimeBatch_Fails_DraftVsDraftConflict() {
        when(roomDao.findById(1)).thenReturn(Optional.of(activeRoom));
        when(seatDao.countActiveSeatsByRoomId(1)).thenReturn(100);
        when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie)); // Duration 180m (09:00 - 12:00 + 15m buf -> 12:15)

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(2);
        // Slot 1: 09:00 - 12:00 (Cleaning buffer until 12:15)
        ShowtimeForm slot1 = new ShowtimeForm();
        slot1.setMovieId(1);
        slot1.setRoomId(1);
        slot1.setStartTime(tomorrow.withHour(9).withMinute(0).withSecond(0).withNano(0));
        slot1.setBaseTicketPrice(new BigDecimal("85000"));

        // Slot 2: 12:10 (starts during 15m cleaning buffer of slot 1!)
        ShowtimeForm slot2 = new ShowtimeForm();
        slot2.setMovieId(1);
        slot2.setRoomId(1);
        slot2.setStartTime(tomorrow.withHour(12).withMinute(10).withSecond(0).withNano(0));
        slot2.setBaseTicketPrice(new BigDecimal("90000"));

        ShowtimeConflictException ex = assertThrows(ShowtimeConflictException.class, () ->
                showtimeService.createShowtimeBatch(1, List.of(slot1, slot2)));

        assertTrue(ex.getMessage().contains("Schedule conflict between draft slots"));
        assertTrue(ex.getMessage().contains("Slot #1"));
        assertTrue(ex.getMessage().contains("Slot #2"));
        verify(showtimeDao, never()).insert(any());
    }

    @Test
    @DisplayName("Batch Save: Exact 15-minute cleaning boundary is allowed without conflict")
    void testCreateShowtimeBatch_Boundary15MinuteCleaning_Success() {
        when(roomDao.findById(1)).thenReturn(Optional.of(activeRoom));
        when(seatDao.countActiveSeatsByRoomId(1)).thenReturn(100);
        when(movieDao.findById(1)).thenReturn(Optional.of(activeMovie)); // Duration 180m (09:00 - 12:00)

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(2);
        // Slot 1: 09:00 - 12:00. Room available at 12:15.
        ShowtimeForm slot1 = new ShowtimeForm();
        slot1.setMovieId(1);
        slot1.setRoomId(1);
        slot1.setStartTime(tomorrow.withHour(9).withMinute(0).withSecond(0).withNano(0));
        slot1.setBaseTicketPrice(new BigDecimal("85000"));

        // Slot 1: 09:00 to 12:00 (180m movie, no trailer), buffer until 12:15
        // Slot 2: exactly starts at 12:15!
        ShowtimeForm slot2 = new ShowtimeForm();
        slot2.setMovieId(1);
        slot2.setRoomId(1);
        slot2.setStartTime(tomorrow.withHour(12).withMinute(15).withSecond(0).withNano(0));
        slot2.setBaseTicketPrice(new BigDecimal("90000"));

        Showtime created1 = new Showtime();
        created1.setShowtimeId(201);
        Showtime created2 = new Showtime();
        created2.setShowtimeId(202);

        when(showtimeDao.findOverlappingShowtimes(eq(1), any(), any(), isNull()))
                .thenReturn(Collections.emptyList(), List.of(created1), Collections.emptyList(), List.of(created2));

        List<Showtime> result = showtimeService.createShowtimeBatch(1, List.of(slot1, slot2));
        assertEquals(2, result.size());
        verify(showtimeDao, times(2)).insert(any());
    }

    @Test
    @DisplayName("Batch Save: Empty slot list throws InvalidShowtimeException")
    void testCreateShowtimeBatch_EmptyList_ThrowsException() {
        assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.createShowtimeBatch(1, Collections.emptyList()));
    }

    @Test
    @DisplayName("Task Requirements: Batman 140m at 17:00 -> EndTime 19:30, Room Avail 19:45; 19:44 conflicts, 19:45 succeeds")
    void testBatmanCalculationAndConflict_PromptScenario() {
        Movie batman = new Movie();
        batman.setMovieId(99);
        batman.setTitle("Batman");
        batman.setDuration(140);
        batman.setStatus("Now Showing");

        when(movieDao.findById(99)).thenReturn(Optional.of(batman));
        when(roomDao.findById(2)).thenReturn(Optional.of(activeRoom));
        when(seatDao.countActiveSeatsByRoomId(2)).thenReturn(120);

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime start1700 = tomorrow.withHour(17).withMinute(0).withSecond(0).withNano(0);

        // 1. Slot 1 at 17:00 (140m duration -> Film End: 19:20, Room Available: 19:35)
        ShowtimeForm slot1 = new ShowtimeForm();
        slot1.setMovieId(99);
        slot1.setRoomId(2);
        slot1.setStartTime(start1700);
        slot1.setBaseTicketPrice(new BigDecimal("90000"));

        // Slot 2 starting at 19:34 (should conflict because room occupied until 19:35)
        ShowtimeForm slotConflict = new ShowtimeForm();
        slotConflict.setMovieId(99);
        slotConflict.setRoomId(2);
        slotConflict.setStartTime(tomorrow.withHour(19).withMinute(34).withSecond(0).withNano(0));
        slotConflict.setBaseTicketPrice(new BigDecimal("90000"));

        // Test conflict between slot1 and slotConflict
        assertThrows(ShowtimeConflictException.class, () ->
                showtimeService.createShowtimeBatch(2, List.of(slot1, slotConflict)));

        // Slot 2 starting at exactly 19:35 (should be valid!)
        ShowtimeForm slotValid = new ShowtimeForm();
        slotValid.setMovieId(99);
        slotValid.setRoomId(2);
        slotValid.setStartTime(tomorrow.withHour(19).withMinute(35).withSecond(0).withNano(0));
        slotValid.setBaseTicketPrice(new BigDecimal("90000"));

        when(showtimeDao.findOverlappingShowtimes(eq(2), any(), any(), isNull()))
                .thenReturn(Collections.emptyList(), List.of(new Showtime()), Collections.emptyList(), List.of(new Showtime()));

        List<Showtime> batch = showtimeService.createShowtimeBatch(2, List.of(slot1, slotValid));
        assertEquals(2, batch.size());
        assertEquals(start1700.plusMinutes(140), slot1.getEndTime()); // 19:20 (no trailer)
    }
}
