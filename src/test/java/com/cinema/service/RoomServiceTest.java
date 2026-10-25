package com.cinema.service;

import com.cinema.dao.RoomDao;
import com.cinema.exception.RoomHasActiveShowtimesException;
import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.Room;
import com.cinema.dto.RoomForm;
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

    @InjectMocks
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
    void create_validCapacityAndLayout_success() {
        RoomForm form = new RoomForm();
        form.setRoomCode("ROOM-TEST");
        form.setRoomName("Test Room");
        form.setRoomType("2D Standard");
        form.setTotalRows(10);
        form.setSeatsPerRow(12);
        form.setCapacity(120); // 10 * 12 = 120
        form.setStatus("Ready");

        when(roomDao.existsByRoomCode("ROOM-TEST", null)).thenReturn(false);
        when(roomDao.existsByRoomName("Test Room", null)).thenReturn(false);
        when(roomDao.insert(form)).thenReturn(1);

        assertDoesNotThrow(() -> roomService.create(form));
        verify(roomDao).insert(form);
    }

    @Test
    void create_mismatchedCapacity_throwsIllegalArgumentException() {
        RoomForm form = new RoomForm();
        form.setRoomCode("ROOM-TEST");
        form.setRoomName("Test Room");
        form.setTotalRows(10);
        form.setSeatsPerRow(12);
        form.setCapacity(100); // 10 * 12 = 120 != 100

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> roomService.create(form));
        assertTrue(ex.getMessage().contains("phải bằng"));
    }

    @Test
    void create_missingRowsOrSeats_throwsIllegalArgumentException() {
        RoomForm form = new RoomForm();
        form.setRoomCode("ROOM-TEST");
        form.setRoomName("Test Room");
        form.setCapacity(100);

        assertThrows(IllegalArgumentException.class, () -> roomService.create(form));
    }
}
