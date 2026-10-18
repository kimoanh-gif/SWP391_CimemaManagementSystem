package com.cinema.service;

import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.SeatTypeDao;
import com.cinema.exception.InvalidSeatLayoutException;
import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.model.SeatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatDao seatDao;

    @Mock
    private RoomDao roomDao;

    @Mock
    private SeatTypeDao seatTypeDao;

    @InjectMocks
    private SeatService seatService;

    private Room mockRoom;

    @BeforeEach
    void setUp() {
        mockRoom = new Room();
        mockRoom.setRoomId(1);
        mockRoom.setRoomCode("ROOM-01");
        mockRoom.setRoomName("Phòng 01");
        mockRoom.setCapacity(100);
        mockRoom.setTotalRows(8);
        mockRoom.setSeatsPerRow(12);
    }

    @Test
    @DisplayName("getSeatsByRoomId: Should return seat list when room exists")
    void testGetSeatsByRoomId_Success() {
        when(roomDao.findById(1)).thenReturn(Optional.of(mockRoom));
        when(seatDao.findByRoomId(1)).thenReturn(List.of(new Seat(1, 1, "Standard", "A", 1, 1, 1, "Active")));

        List<Seat> result = seatService.getSeatsByRoomId(1);
        assertEquals(1, result.size());
        assertEquals("Standard", result.get(0).getSeatTypeName());
    }

    @Test
    @DisplayName("saveRoomLayout: Should reject when duplicate position exists")
    void testSaveRoomLayout_DuplicatePosition_ThrowsException() {
        when(roomDao.findById(1)).thenReturn(Optional.of(mockRoom));

        Seat s1 = new Seat(null, 1, "Standard", "A", 1, 1, 1, "Active");
        Seat s2 = new Seat(null, 1, "Standard", "A", 1, 1, 1, "Active");

        assertThrows(InvalidSeatLayoutException.class, () ->
                seatService.saveRoomLayout(1, List.of(s1, s2))
        );
    }

    @Test
    @DisplayName("saveRoomLayout: Should reject when count exceeds room capacity")
    void testSaveRoomLayout_ExceedsCapacity_ThrowsException() {
        mockRoom.setCapacity(2);
        when(roomDao.findById(1)).thenReturn(Optional.of(mockRoom));

        List<Seat> list = List.of(
                new Seat(null, 1, "Standard", "A", 1, 1, 1, "Active"),
                new Seat(null, 1, "Standard", "A", 2, 1, 2, "Active"),
                new Seat(null, 1, "Standard", "A", 3, 1, 3, "Active")
        );

        assertThrows(InvalidSeatLayoutException.class, () ->
                seatService.saveRoomLayout(1, list)
        );
    }

    @Test
    @DisplayName("generateDefaultLayout: Should create seats according to room dimensions")
    void testGenerateDefaultLayout_Success() {
        when(roomDao.findById(1)).thenReturn(Optional.of(mockRoom));
        when(seatTypeDao.findAll(anyString())).thenReturn(List.of(
                new SeatType(1, "STD", "Standard", null, BigDecimal.ONE, null, "Active"),
                new SeatType(2, "VIP", "VIP", null, new BigDecimal("1.25"), null, "Active"),
                new SeatType(3, "COUPLE", "Couple", null, new BigDecimal("1.50"), null, "Active"),
                new SeatType(4, "FRONT", "Front", null, new BigDecimal("0.90"), null, "Active")
        ));
        when(seatDao.findByRoomId(1)).thenReturn(new ArrayList<>());

        List<Seat> result = seatService.generateDefaultLayout(1);
        assertNotNull(result);
        verify(seatDao, atLeastOnce()).insert(any(Seat.class));
    }
}
