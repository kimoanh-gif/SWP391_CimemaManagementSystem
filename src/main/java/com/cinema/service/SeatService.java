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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Transactional(rollbackFor = Exception.class)
    public Seat updateSeat(int seatId, String seatTypeName, String status) {
        Seat seat = getSeatById(seatId);

        if (seatTypeName != null && !seatTypeName.isBlank()) {
            validateSeatType(seatTypeName.trim());
            if (!seat.getSeatTypeName().equalsIgnoreCase(seatTypeName.trim())
                    && seatDao.isSeatBookedInUpcomingShowtimes(seatId, seat.getRoomId())) {
                throw new InvalidSeatLayoutException("Không thể đổi loại ghế vì ghế đã có vé đặt trong suất chiếu sắp diễn ra.");
            }
            seat.setSeatTypeName(seatTypeName.trim());
            seat.setSeatTypeId(resolveSeatTypeId(seatTypeName.trim()));
        }

        if (status != null && !status.isBlank()) {
            validateSeatStatus(status.trim());
            if (!"Active".equalsIgnoreCase(status.trim())
                    && seatDao.isSeatBookedInUpcomingShowtimes(seatId, seat.getRoomId())) {
                throw new InvalidSeatLayoutException("Không thể khóa bảo trì ghế vì ghế đã có vé đặt trong suất chiếu sắp diễn ra.");
            }
            seat.setStatus(status.trim());
        }

        seatDao.update(seat);
        roomDao.updateRoomTimestamp(seat.getRoomId());
        return getSeatById(seatId);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Seat> saveRoomLayout(int roomId, List<Seat> seats) {
        return saveRoomLayout(roomId, seats, null);
    }

    /**
     * TO-BE P0-01: Diff-based seat layout update.
     * Enforces:
     * - AC01: Modifying a seat does NOT alter SeatID of unchanged seats.
     * - AC02: Physically deleting a seat referenced by historical tickets is prohibited.
     * - AC03: Atomic execution in one transaction; failure in middle rolls back entire batch.
     * - AC04: Unique constraint validation (RoomID, RowLabel, SeatNumber).
     * - AC05: Clear business conflict exceptions.
     * - AC06: Optimistic versioning / room lock prevents concurrent overwrite.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Seat> saveRoomLayout(int roomId, List<Seat> seats, LocalDateTime expectedUpdatedAt) {
        // Concurrency protection: lock room row for update within this transaction
        roomDao.lockRoomForUpdate(roomId);
        Room room = ensureRoomExists(roomId);

        // AC06: Optimistic versioning check if expectedUpdatedAt provided
        if (expectedUpdatedAt != null && room.getUpdatedAt() != null) {
            if (room.getUpdatedAt().isAfter(expectedUpdatedAt)) {
                throw new InvalidSeatLayoutException("Sơ đồ phòng chiếu đã được cập nhật bởi một người dùng khác lúc "
                        + room.getUpdatedAt() + ". Vui lòng tải lại trang trước khi lưu (AC06).");
            }
        }

        if (seats == null || seats.isEmpty()) {
            throw new InvalidSeatLayoutException("Sơ đồ ghế không được để trống.");
        }

        if (seats.size() > room.getCapacity()) {
            throw new InvalidSeatLayoutException("Số lượng ghế (" + seats.size() + ") vượt quá sức chứa phòng chiếu (" + room.getCapacity() + ").");
        }

        // AC04: Validate uniqueness of positions and valid attributes
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
                throw new InvalidSeatLayoutException("Trùng lặp vị trí ghế: " + posKey + " (AC04).");
            }

            if (seat.getSeatTypeName() == null || seat.getSeatTypeName().isBlank()) {
                seat.setSeatTypeName("Standard");
            } else {
                validateSeatType(seat.getSeatTypeName().trim());
            }

            if (seat.getSeatTypeId() == null) {
                seat.setSeatTypeId(resolveSeatTypeId(seat.getSeatTypeName()));
            }

            if (seat.getStatus() == null || seat.getStatus().isBlank()) {
                seat.setStatus("Active");
            } else {
                validateSeatStatus(seat.getStatus().trim());
            }
        }

        // Fetch current layout from DB
        List<Seat> currentSeats = seatDao.findByRoomId(roomId);
        Map<String, Seat> currentSeatMap = new HashMap<>();
        for (Seat cs : currentSeats) {
            String key = cs.getRowLabel().trim().toUpperCase() + "-" + cs.getSeatNumber();
            currentSeatMap.put(key, cs);
        }

        Set<String> remainingExistingKeys = new HashSet<>(currentSeatMap.keySet());

        // Process incoming seats (Existing -> UPDATE, New -> INSERT)
        for (Seat incomingSeat : seats) {
            String posKey = incomingSeat.getRowLabel().trim().toUpperCase() + "-" + incomingSeat.getSeatNumber();

            if (currentSeatMap.containsKey(posKey)) {
                // AC01: Existing seat matches position key -> Preserve existing SeatID
                Seat existing = currentSeatMap.get(posKey);
                remainingExistingKeys.remove(posKey);

                boolean typeChanged = !Objects.equals(existing.getSeatTypeName(), incomingSeat.getSeatTypeName())
                        || !Objects.equals(existing.getSeatTypeId(), incomingSeat.getSeatTypeId());
                boolean statusChanged = !Objects.equals(existing.getStatus(), incomingSeat.getStatus());
                boolean gridRowChanged = !Objects.equals(existing.getGridRow(), incomingSeat.getGridRow());
                boolean gridColChanged = !Objects.equals(existing.getGridColumn(), incomingSeat.getGridColumn());

                if (typeChanged || statusChanged || gridRowChanged || gridColChanged) {
                    if (typeChanged && seatDao.isSeatBookedInUpcomingShowtimes(existing.getSeatId(), roomId)) {
                        throw new InvalidSeatLayoutException("Không thể thay đổi loại ghế của ghế "
                                + posKey + " vì ghế đã được khách đặt trong suất chiếu sắp diễn ra.");
                    }

                    if (statusChanged && !"Active".equalsIgnoreCase(incomingSeat.getStatus())
                            && seatDao.isSeatBookedInUpcomingShowtimes(existing.getSeatId(), roomId)) {
                        throw new InvalidSeatLayoutException("Không thể khóa bảo trì ghế "
                                + posKey + " vì ghế đã có vé đặt trong suất chiếu sắp diễn ra.");
                    }

                    existing.setSeatTypeId(incomingSeat.getSeatTypeId() != null ? incomingSeat.getSeatTypeId() : resolveSeatTypeId(incomingSeat.getSeatTypeName()));
                    existing.setSeatTypeName(incomingSeat.getSeatTypeName());
                    existing.setStatus(incomingSeat.getStatus());
                    existing.setGridRow(incomingSeat.getGridRow());
                    existing.setGridColumn(incomingSeat.getGridColumn());

                    seatDao.update(existing);
                }
            } else {
                // New seat position -> INSERT
                incomingSeat.setRoomId(roomId);
                if (incomingSeat.getSeatTypeId() == null) {
                    incomingSeat.setSeatTypeId(resolveSeatTypeId(incomingSeat.getSeatTypeName()));
                }
                seatDao.insert(incomingSeat);
            }
        }

        // Process removed seats (seats previously in DB but missing from new layout)
        if (!remainingExistingKeys.isEmpty()) {
            boolean hasUpcomingBookings = seatDao.hasUpcomingBookingsForRoom(roomId);
            if (hasUpcomingBookings) {
                throw new InvalidSeatLayoutException("Không thể xóa ghế khỏi sơ đồ vì phòng chiếu đang có suất chiếu tương lai đã phát sinh đặt vé.");
            }

            for (String removedKey : remainingExistingKeys) {
                Seat removedSeat = currentSeatMap.get(removedKey);
                // AC02: Protect against physical deletion if referenced in historical tickets/bookings
                if (seatDao.isSeatReferenced(removedSeat.getSeatId())) {
                    throw new InvalidSeatLayoutException("Không thể xóa vật lý ghế "
                            + removedSeat.getRowLabel() + removedSeat.getSeatNumber()
                            + " (SeatID: " + removedSeat.getSeatId() + ") vì ghế đã được vé lịch sử tham chiếu (AC02).");
                }
                seatDao.deleteById(removedSeat.getSeatId());
            }
        }

        // Update room timestamp for optimistic concurrency tracking
        roomDao.updateRoomTimestamp(roomId);

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
                s.setSeatTypeId(resolveSeatTypeId(defaultType));
                s.setStatus("Active");
                generated.add(s);
            }
        }

        return saveRoomLayout(roomId, generated);
    }

    private Integer resolveSeatTypeId(String typeName) {
        if (typeName == null || typeName.isBlank()) return 1;
        try {
            List<SeatType> all = seatTypeDao.findAll("ALL");
            for (SeatType st : all) {
                if (st.getTypeName() != null && st.getTypeName().equalsIgnoreCase(typeName)) {
                    return st.getSeatTypeId();
                }
                if (st.getTypeCode() != null && st.getTypeCode().equalsIgnoreCase(typeName)) {
                    return st.getSeatTypeId();
                }
                if (st.getTypeName() != null && st.getTypeName().toLowerCase().contains(typeName.toLowerCase())) {
                    return st.getSeatTypeId();
                }
            }
        } catch (Exception ignored) {}
        return 1;
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
