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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        assertTrue(ex.getMessage().contains("chưa có sơ đồ ghế khả dụng"));
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
        assertTrue(exToday.getMessage().contains("Không được chọn ngày ở quá khứ hoặc hiện tại"));

        // Test with yesterday's date
        ShowtimeForm formPast = new ShowtimeForm();
        formPast.setMovieId(1);
        formPast.setRoomId(1);
        formPast.setStartTime(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0));
        formPast.setBaseTicketPrice(new BigDecimal("85000"));

        InvalidShowtimeException exPast = assertThrows(InvalidShowtimeException.class, () ->
                showtimeService.createShowtime(formPast));
        assertTrue(exPast.getMessage().contains("Không được chọn ngày ở quá khứ hoặc hiện tại"));
    }
}
