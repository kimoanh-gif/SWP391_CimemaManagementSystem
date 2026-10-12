package com.cinema.service;

import com.cinema.dao.RoomDao;
import com.cinema.exception.RoomHasActiveShowtimesException;
import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.Room;
import com.cinema.model.RoomForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for Room Management (Screen 56).
 * Enforces BR-08 (active rooms for showtimes) and BR-52 (cannot deactivate rooms with upcoming showtimes).
 */
@Service
public class RoomService {

    private final RoomDao roomDao;

    public RoomService(RoomDao roomDao) {
        this.roomDao = roomDao;
    }

    public List<Room> getAll(String keyword, String status) {
        return roomDao.findAll(keyword, status);
    }

    public Room getById(int id) {
        return roomDao.findById(id).orElseThrow(() -> new RoomNotFoundException(id));
    }

    @Transactional
    public void create(RoomForm form) {
        // Additional business checks could be added here (e.g., unique code/name)
        roomDao.insert(form);
    }

    @Transactional
    public void update(RoomForm form) {
        // Ensure room exists
        getById(form.getRoomId());
        roomDao.update(form);
    }

    @Transactional
    public void deactivate(int roomId) {
        Room room = getById(roomId);
        // BR-52: cannot deactivate if upcoming showtimes exist
        if (roomDao.hasUpcomingShowtimes(roomId)) {
            throw new RoomHasActiveShowtimesException(roomId);
        }
        // Change status to Maintenance (or a dedicated Deactivated status if defined)
        room.setStatus("Maintenance");
        RoomForm form = new RoomForm();
        form.setRoomId(roomId);
        form.setRoomCode(room.getRoomCode());
        form.setRoomName(room.getRoomName());
        form.setRoomType(room.getRoomType());
        form.setCapacity(room.getCapacity());
        form.setTotalRows(room.getTotalRows());
        form.setSeatsPerRow(room.getSeatsPerRow());
        form.setProjector(room.getProjector());
        form.setSoundSystem(room.getSoundSystem());
        form.setScreenSize(room.getScreenSize());
        form.setStatus("Maintenance");
        form.setDescription(room.getDescription());
        roomDao.update(form);
    }
}
