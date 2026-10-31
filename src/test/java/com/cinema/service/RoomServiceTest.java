package com.cinema.service;

import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.exception.RoomHasActiveShowtimesException;
import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.Room;
import com.cinema.dto.RoomForm;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class RoomServiceTest {

    @Mock
    private RoomDao roomDao;

    @Mock
    private SeatDao seatDao;

    @InjectMocks
    private RoomService roomService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void getAll_shouldReturnDaoResult() {
        when(roomDao.findAll(null, null)).thenReturn(Collections.emptyList());
        assertTrue(roomService.getAll(null, null).isEmpty());
        verify(roomDao).findAll(null, null);
    }

    @Test
    void getById_existingRoom() {
        Room room = new Room();
        room.setRoomId(1);
        when(roomDao.findById(1)).thenReturn(Optional.of(room));
        assertEquals(1, roomService.getById(1).getRoomId());
    }

    @Test
    void getById_notFound() {
        when(roomDao.findById(anyInt())).thenReturn(Optional.empty());
        assertThrows(RoomNotFoundException.class, () -> roomService.getById(99));
    }

    @Test
    void deactivate_withoutUpcomingShowtimes() {
        Room room = new Room();
        room.setRoomId(5);
        room.setRoomCode("R005");
        room.setRoomName("Sala 5");
        room.setRoomType("Standard Laser");
        room.setCapacity(150);
        room.setStatus("Ready");
        when(roomDao.findById(5)).thenReturn(Optional.of(room));
        when(roomDao.hasUpcomingShowtimes(5)).thenReturn(false);
        // no exception expected
        assertDoesNotThrow(() -> roomService.deactivate(5));
        verify(roomDao).update(any(RoomForm.class));
    }

    @Test
    void deactivate_withUpcomingShowtimes_shouldThrow() {
        Room room = new Room();
        room.setRoomId(6);
        when(roomDao.findById(6)).thenReturn(Optional.of(room));
        when(roomDao.hasUpcomingShowtimes(6)).thenReturn(true);
        assertThrows(RoomHasActiveShowtimesException.class, () -> roomService.deactivate(6));
    }

    @Test
    void delete_withoutShowtimes_success() {
        Room room = new Room();
        room.setRoomId(7);
        room.setRoomName("Test Cinema 7");
        when(roomDao.findById(7)).thenReturn(Optional.of(room));
        when(roomDao.countTotalShowtimes(7)).thenReturn(0);

        assertDoesNotThrow(() -> roomService.delete(7));
        verify(roomDao).deleteSeatsByRoomId(7);
        verify(roomDao).delete(7);
    }

    @Test
    void delete_withShowtimes_shouldThrow() {
        Room room = new Room();
        room.setRoomId(8);
        room.setRoomName("Test Cinema 8");
        when(roomDao.findById(8)).thenReturn(Optional.of(room));
        when(roomDao.countTotalShowtimes(8)).thenReturn(2);

        assertThrows(RoomHasActiveShowtimesException.class, () -> roomService.delete(8));
        verify(roomDao, never()).delete(anyInt());
    }

    @Test
    void create_validCapacityLayout_success() {
        RoomForm form = new RoomForm();
        form.setRoomCode("ROOM-TEST");
        form.setRoomName("Test Room");
        form.setRoomType("Standard 2D");
        form.setTotalRows(10);
        form.setSeatsPerRow(12);
        form.setCapacity(120);
        form.setStatus("Ready");

        assertDoesNotThrow(() -> roomService.create(form));
        verify(roomDao).insert(form);
    }

    @Test
    void create_duplicateRoomCode_shouldThrow() {
        RoomForm form = new RoomForm();
        form.setRoomCode("ROOM-01");
        form.setRoomName("Unique Room");
        form.setRoomType("IMAX Laser");
        form.setTotalRows(10);
        form.setSeatsPerRow(10);
        form.setCapacity(100);
        form.setStatus("Ready");

        when(roomDao.existsByRoomCode("ROOM-01", null)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.create(form));
        assertTrue(ex.getMessage().contains("Mã phòng chiếu đã tồn tại"));
        verify(roomDao, never()).insert(any());
    }

    @Test
    void create_mismatchCapacityLayout_shouldThrow() {
        RoomForm form = new RoomForm();
        form.setTotalRows(10);
        form.setSeatsPerRow(12);
        form.setCapacity(100); // 10 * 12 = 120 != 100

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.create(form));
        assertTrue(ex.getMessage().contains("không khớp"));
        verify(roomDao, never()).insert(any());
    }

    @Test
    void create_missingOneOfRowsOrSeats_shouldThrow() {
        RoomForm form = new RoomForm();
        form.setTotalRows(10);
        form.setSeatsPerRow(null);
        form.setCapacity(100);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.create(form));
        assertTrue(ex.getMessage().contains("nhập đầy đủ"));
        verify(roomDao, never()).insert(any());
    }

    @Test
    void update_mismatchCapacityLayout_shouldThrow() {
        Room existing = new Room();
        existing.setRoomId(15);
        when(roomDao.findById(15)).thenReturn(Optional.of(existing));

        RoomForm form = new RoomForm();
        form.setRoomId(15);
        form.setTotalRows(8);
        form.setSeatsPerRow(10);
        form.setCapacity(90); // 8 * 10 = 80 != 90

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.update(form));
        assertTrue(ex.getMessage().contains("không khớp"));
        verify(roomDao, never()).update(any());
    }

    @Test
    void update_toMaintenanceWithUpcomingShowtimes_shouldThrow() {
        Room existing = new Room();
        existing.setRoomId(15);
        existing.setStatus("Ready");
        when(roomDao.findById(15)).thenReturn(Optional.of(existing));
        when(roomDao.hasUpcomingShowtimes(15)).thenReturn(true);

        RoomForm form = new RoomForm();
        form.setRoomId(15);
        form.setRoomCode("ROOM-15");
        form.setRoomName("Room 15");
        form.setRoomType("IMAX Laser");
        form.setTotalRows(10);
        form.setSeatsPerRow(10);
        form.setCapacity(100);
        form.setStatus("Maintenance");

        assertThrows(RoomHasActiveShowtimesException.class, () -> roomService.update(form));
        verify(roomDao, never()).update(any());
    }

    @Test
    void roomForm_beanValidation_detectsInvalidCodeStatusAndMismatchCapacity() {
        RoomForm form = new RoomForm();
        form.setRoomCode("INVALID CODE!");
        form.setRoomName("Room 01");
        form.setRoomType("IMAX Laser");
        form.setTotalRows(10);
        form.setSeatsPerRow(10);
        form.setCapacity(80);
        form.setStatus("InvalidStatus");

        var violations = validator.validate(form);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("roomCode")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("capacityMatchingLayout")));
    }
}
