package com.cinema.service;

import com.cinema.dao.RoomDao;
import com.cinema.dao.SeatDao;
import com.cinema.dao.SeatTypeDao;
import com.cinema.exception.InvalidSeatLayoutException;
import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.model.SeatType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for Screen 58: Seat Management / Seat Layout Management (UC-29).
 */
@Service
public class SeatService {

    private final SeatDao seatDao;
    private final RoomDao roomDao;
    private final SeatTypeDao seatTypeDao;

    public SeatService(SeatDao seatDao, RoomDao roomDao, SeatTypeDao seatTypeDao) {
        this.seatDao = seatDao;
        this.roomDao = roomDao;
        this.seatTypeDao = seatTypeDao;
    }

    public List<Seat> getSeatsByRoomId(int roomId) {
        ensureRoomExists(roomId);
        return seatDao.findByRoomId(roomId);
    }

    public Seat getSeatById(int seatId) {
        return seatDao.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghế với ID: " + seatId));
    }

    @Transactional
    public Seat updateSeat(int seatId, String seatTypeName, String status) {
        Seat seat = getSeatById(seatId);

        if (seatTypeName != null && !seatTypeName.isBlank()) {
            validateSeatType(seatTypeName.trim());
            seat.setSeatTypeName(seatTypeName.trim());
        }

        if (status != null && !status.isBlank()) {
            validateSeatStatus(status.trim());
            seat.setStatus(status.trim());
        }

        seatDao.update(seat);
        return getSeatById(seatId);
    }

    @Transactional
    public List<Seat> saveRoomLayout(int roomId, List<Seat> seats) {
        Room room = ensureRoomExists(roomId);

        if (seats == null || seats.isEmpty()) {
            throw new InvalidSeatLayoutException("Sơ đồ ghế không được để trống.");
        }

        if (seats.size() > room.getCapacity()) {
            throw new InvalidSeatLayoutException("Số lượng ghế (" + seats.size() + ") vượt quá sức chứa phòng chiếu (" + room.getCapacity() + ").");
        }

        // Validate uniqueness of positions
        Set<String> positions = new HashSet<>();
        for (Seat seat : seats) {
            seat.setRoomId(roomId);

            if (seat.getRowLabel() == null || seat.getRowLabel().trim().isEmpty()) {
                throw new InvalidSeatLayoutException("Mã hàng ghế không được để trống.");
            }
            if (seat.getSeatNumber() == null || seat.getSeatNumber() <= 0) {
                throw new InvalidSeatLayoutException("Số ghế phải lớn hơn 0.");
            }

            String posKey = seat.getRowLabel().trim().toUpperCase() + "-" + seat.getSeatNumber();
            if (!positions.add(posKey)) {
                throw new InvalidSeatLayoutException("Trùng lặp vị trí ghế: " + posKey);
            }

            if (seat.getSeatTypeName() == null || seat.getSeatTypeName().isBlank()) {
                seat.setSeatTypeName("Standard");
            } else {
                validateSeatType(seat.getSeatTypeName().trim());
            }

            if (seat.getStatus() == null || seat.getStatus().isBlank()) {
                seat.setStatus("Active");
            } else {
                validateSeatStatus(seat.getStatus().trim());
            }
        }

        // Clear existing seats and re-insert new layout
        seatDao.deleteByRoomId(roomId);
        for (Seat seat : seats) {
            seatDao.insert(seat);
        }

        return seatDao.findByRoomId(roomId);
    }

    /**
     * Helper to auto-generate default grid layout based on Room's TotalRows and SeatsPerRow.
     */
    @Transactional
    public List<Seat> generateDefaultLayout(int roomId) {
        Room room = ensureRoomExists(roomId);
        int rows = room.getTotalRows() != null && room.getTotalRows() > 0 ? room.getTotalRows() : 8;
        int cols = room.getSeatsPerRow() != null && room.getSeatsPerRow() > 0 ? room.getSeatsPerRow() : 12;

        List<Seat> generated = new ArrayList<>();
        char startRowChar = 'A';

        for (int r = 1; r <= rows; r++) {
            String rowLabel = String.valueOf((char) (startRowChar + (r - 1)));

            // Infer default seat type according to standard theater zoning
            String defaultType = "Standard";
            if (r == 1) {
                defaultType = "Front";
            } else if (r >= (rows / 2) && r <= (rows / 2 + 2)) {
                defaultType = "VIP";
            } else if (r == rows) {
                defaultType = "Couple";
            }

            for (int c = 1; c <= cols; c++) {
                Seat s = new Seat();
                s.setRoomId(roomId);
                s.setRowLabel(rowLabel);
                s.setSeatNumber(c);
                s.setGridRow(r);
                s.setGridColumn(c);
                s.setSeatTypeName(defaultType);
                s.setStatus("Active");
                generated.add(s);
            }
        }

        return saveRoomLayout(roomId, generated);
    }

    private Room ensureRoomExists(int roomId) {
        return roomDao.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    private void validateSeatType(String typeName) {
        List<SeatType> validTypes = seatTypeDao.findAll("Active");
        boolean match = validTypes.stream().anyMatch(st ->
                st.getTypeName().equalsIgnoreCase(typeName) || st.getTypeCode().equalsIgnoreCase(typeName)
        );
        if (!match && !"Standard".equalsIgnoreCase(typeName) && !"VIP".equalsIgnoreCase(typeName) && !"Couple".equalsIgnoreCase(typeName) && !"Front".equalsIgnoreCase(typeName)) {
            throw new InvalidSeatLayoutException("Loại ghế không hợp lệ: '" + typeName + "'. Phải là loại ghế đã được kích hoạt.");
        }
    }

    private void validateSeatStatus(String status) {
        if (!"Active".equalsIgnoreCase(status) && !"Maintenance".equalsIgnoreCase(status) && !"Unavailable".equalsIgnoreCase(status)) {
            throw new InvalidSeatLayoutException("Trạng thái ghế không hợp lệ: '" + status + "'. Cho phép: Active, Maintenance, Unavailable.");
        }
    }
}
