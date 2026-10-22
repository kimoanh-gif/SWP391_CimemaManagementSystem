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
}
