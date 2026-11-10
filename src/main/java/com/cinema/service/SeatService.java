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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new IllegalArgumentException("Seat not found with ID: " + seatId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Seat updateSeat(int seatId, String seatTypeName, String status) {
        if ((seatTypeName == null || seatTypeName.isBlank()) && (status == null || status.isBlank())) {
            throw new IllegalArgumentException("Must provide at least seat type or seat status to update.");
        }

        Seat seat = getSeatById(seatId);

        if (seatTypeName != null && !seatTypeName.isBlank()) {
            SeatType matchedType = validateSeatType(seatTypeName.trim());
            String targetTypeName = matchedType != null && matchedType.getTypeName() != null ? matchedType.getTypeName() : seatTypeName.trim();
            Integer targetTypeId = matchedType != null && matchedType.getSeatTypeId() != null ? matchedType.getSeatTypeId() : resolveSeatTypeId(seatTypeName.trim());

            if (!seat.getSeatTypeName().equalsIgnoreCase(targetTypeName)
                    && seatDao.isSeatBookedInUpcomingShowtimes(seatId, seat.getRoomId())) {
                throw new InvalidSeatLayoutException("Cannot change seat type because the seat has booked tickets in an upcoming showtime.");
            }
            seat.setSeatTypeName(targetTypeName);
            seat.setSeatTypeId(targetTypeId);
        }

        if (status != null && !status.isBlank()) {
            validateSeatStatus(status.trim());
            String normalizedStatus = status.trim();
            if ("Lock".equalsIgnoreCase(normalizedStatus) || "Locked".equalsIgnoreCase(normalizedStatus)) {
                normalizedStatus = "Maintenance";
            }
            if (!"Active".equalsIgnoreCase(normalizedStatus)
                    && seatDao.isSeatBookedInUpcomingShowtimes(seatId, seat.getRoomId())) {
                throw new InvalidSeatLayoutException("Cannot set seat to maintenance because the seat has booked tickets in an upcoming showtime.");
            }
            seat.setStatus(normalizedStatus);
        }

        seatDao.update(seat);
        roomDao.updateRoomTimestamp(seat.getRoomId());
        return getSeatById(seatId);
    }

    /**
     * Batch update multiple seats in a room atomically within a single transaction.
     * Enforces:
     * - Pessimistic room lock (lockRoomForUpdate)
     * - Optimistic version check against Room.updatedAt
     * - Full pre-validation of all target seats & booking constraints before any modification
     * - Atomic rollback if any seat update fails (AC03)
     * - Bumps Room.updatedAt timestamp on successful commit
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Seat> batchUpdateSeats(int roomId, List<Integer> seatIds, String seatTypeName, String status, LocalDateTime expectedUpdatedAt) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Seat IDs list cannot be empty for batch update.");
        }
        if ((seatTypeName == null || seatTypeName.isBlank()) && (status == null || status.isBlank())) {
            throw new IllegalArgumentException("Must provide at least seat type or seat status to update.");
        }

        // 1. Lock room row to serialize concurrent seat operations on this room
        roomDao.lockRoomForUpdate(roomId);
        Room room = ensureRoomExists(roomId);

        // 2. Optimistic versioning check: reject stale updates
        validateRoomVersion(room, expectedUpdatedAt);

        // 3. Resolve seat type & status to be applied
        String targetTypeName = null;
        Integer targetTypeId = null;
        if (seatTypeName != null && !seatTypeName.isBlank()) {
            SeatType matchedType = validateSeatType(seatTypeName.trim());
            targetTypeName = matchedType != null && matchedType.getTypeName() != null ? matchedType.getTypeName() : seatTypeName.trim();
            targetTypeId = matchedType != null && matchedType.getSeatTypeId() != null ? matchedType.getSeatTypeId() : resolveSeatTypeId(seatTypeName.trim());
        }

        String targetStatus = null;
        if (status != null && !status.isBlank()) {
            validateSeatStatus(status.trim());
            targetStatus = status.trim();
            if ("Lock".equalsIgnoreCase(targetStatus) || "Locked".equalsIgnoreCase(targetStatus)) {
                targetStatus = "Maintenance";
            }
        }

        // 4. Fetch all room seats and validate all target seats belong to this room
        List<Seat> roomSeats = seatDao.findByRoomId(roomId);
        Map<Integer, Seat> seatMap = roomSeats.stream()
                .collect(Collectors.toMap(Seat::getSeatId, s -> s));

        List<Seat> targetSeats = new ArrayList<>();
        Set<Integer> uniqueSeatIds = new LinkedHashSet<>(seatIds);

        for (Integer sid : uniqueSeatIds) {
            Seat seat = seatMap.get(sid);
            if (seat == null) {
                throw new InvalidSeatLayoutException("Seat with ID " + sid + " does not belong to room ID " + roomId + ".");
            }
            targetSeats.add(seat);
        }

        // 5. Pre-validate booking constraints for ALL target seats BEFORE any DB modification
        for (Seat seat : targetSeats) {
            String seatLabel = seat.getRowLabel() + seat.getSeatNumber();
            boolean typeChanged = targetTypeName != null && !targetTypeName.equalsIgnoreCase(seat.getSeatTypeName());
            boolean statusChanged = targetStatus != null && !targetStatus.equalsIgnoreCase(seat.getStatus());

            if (typeChanged && seatDao.isSeatBookedInUpcomingShowtimes(seat.getSeatId(), roomId)) {
                throw new InvalidSeatLayoutException("Cannot change seat type of seat " + seatLabel
                        + " because it has been booked by customers in an upcoming showtime.");
            }

            if (statusChanged && !"Active".equalsIgnoreCase(targetStatus)
                    && seatDao.isSeatBookedInUpcomingShowtimes(seat.getSeatId(), roomId)) {
                throw new InvalidSeatLayoutException("Cannot set seat to maintenance for seat " + seatLabel
                        + " because it has booked tickets in an upcoming showtime.");
            }
        }

        // 6. Apply updates to all target seats
        for (Seat seat : targetSeats) {
            if (targetTypeName != null) {
                seat.setSeatTypeName(targetTypeName);
                seat.setSeatTypeId(targetTypeId);
            }
            if (targetStatus != null) {
                seat.setStatus(targetStatus);
            }
            seatDao.update(seat);
        }

        // 7. Bump room timestamp so concurrent clients become aware
        roomDao.updateRoomTimestamp(roomId);

        return targetSeats;
    }

    private void validateRoomVersion(Room room, LocalDateTime expectedUpdatedAt) {
        if (room.getUpdatedAt() != null) {
            if (expectedUpdatedAt == null) {
                throw new InvalidSeatLayoutException("Room seat layout was updated by another user at "
                        + room.getUpdatedAt() + ". Please refresh the page before saving.");
            }
            if (room.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS).isAfter(expectedUpdatedAt.truncatedTo(ChronoUnit.SECONDS))) {
                throw new InvalidSeatLayoutException("Room seat layout was updated by another user at "
                        + room.getUpdatedAt() + ". Please refresh the page before saving.");
            }
        }
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
        if (expectedUpdatedAt != null) {
            validateRoomVersion(room, expectedUpdatedAt);
        }

        if (seats == null || seats.isEmpty()) {
            throw new InvalidSeatLayoutException("Seat layout cannot be empty.");
        }

        int maxRows = room.getTotalRows() != null && room.getTotalRows() > 0 ? room.getTotalRows() : 26;
        int maxCols = room.getSeatsPerRow() != null && room.getSeatsPerRow() > 0 ? room.getSeatsPerRow() : 50;
        int maxGridPositions = maxRows * maxCols;

        if (room.getCapacity() != null && room.getCapacity() > 0 && seats.size() > room.getCapacity()) {
            throw new InvalidSeatLayoutException("Total seats (" + seats.size() + ") exceeds room capacity (" + room.getCapacity() + ").");
        }

        if (seats.size() > maxGridPositions) {
            throw new InvalidSeatLayoutException("Total seats (" + seats.size() + ") exceeds maximum grid positions (" + maxGridPositions + ").");
        }

        // AC04: Validate uniqueness of positions and valid attributes
        Set<String> positions = new HashSet<>();
        for (Seat seat : seats) {
            seat.setRoomId(roomId);

            if (seat.getRowLabel() == null || seat.getRowLabel().trim().isEmpty()) {
                throw new InvalidSeatLayoutException("Row code cannot be empty.");
            }
            if (seat.getSeatNumber() == null || seat.getSeatNumber() <= 0) {
                throw new InvalidSeatLayoutException("Seat number must be greater than 0.");
            }

            String posKey = seat.getRowLabel().trim().toUpperCase() + "-" + seat.getSeatNumber();
            if (!positions.add(posKey)) {
                throw new InvalidSeatLayoutException("Duplicate seat position: " + posKey + " (AC04).");
            }

            if (seat.getGridRow() != null && (seat.getGridRow() < 1 || seat.getGridRow() > maxRows)) {
                throw new InvalidSeatLayoutException("Seat " + posKey + " grid row (" + seat.getGridRow() + ") is outside room grid boundary (1-" + maxRows + ").");
            }
            if (seat.getGridColumn() != null && (seat.getGridColumn() < 1 || seat.getGridColumn() > maxCols)) {
                throw new InvalidSeatLayoutException("Seat " + posKey + " grid column (" + seat.getGridColumn() + ") is outside room grid boundary (1-" + maxCols + ").");
            }

            String reqTypeName = (seat.getSeatTypeName() == null || seat.getSeatTypeName().isBlank()) ? "Standard" : seat.getSeatTypeName().trim();
            SeatType matchedType = validateSeatType(reqTypeName);
            if (matchedType != null) {
                seat.setSeatTypeName(matchedType.getTypeName());
                seat.setSeatTypeId(matchedType.getSeatTypeId());
            } else {
                seat.setSeatTypeName(reqTypeName);
                if (seat.getSeatTypeId() == null) {
                    seat.setSeatTypeId(resolveSeatTypeId(reqTypeName));
                }
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
                        throw new InvalidSeatLayoutException("Cannot change seat type of seat "
                                + posKey + " because it has been booked by customers in an upcoming showtime.");
                    }

                    if (statusChanged && !"Active".equalsIgnoreCase(incomingSeat.getStatus())
                            && seatDao.isSeatBookedInUpcomingShowtimes(existing.getSeatId(), roomId)) {
                        throw new InvalidSeatLayoutException("Cannot set seat to maintenance for seat "
                                + posKey + " because it has booked tickets in an upcoming showtime.");
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
                throw new InvalidSeatLayoutException("Cannot remove seats from layout because the room has upcoming showtimes with booked tickets.");
            }

            for (String removedKey : remainingExistingKeys) {
                Seat removedSeat = currentSeatMap.get(removedKey);
                // AC02: Protect against physical deletion if referenced in historical tickets/bookings
                if (seatDao.isSeatReferenced(removedSeat.getSeatId())) {
                    throw new InvalidSeatLayoutException("Cannot physically delete seat "
                            + removedSeat.getRowLabel() + removedSeat.getSeatNumber()
                            + " (SeatID: " + removedSeat.getSeatId() + ") because it is referenced by historical bookings.");
                }
                seatDao.deleteById(removedSeat.getSeatId());
            }
        }

        // Synchronize Room.Capacity with actual configured physical seat count within the same transaction
        roomDao.updateCapacity(roomId, seats.size());
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

            SeatType matched = resolveSeatType(defaultType);
            String assignedTypeName = matched != null && matched.getTypeName() != null ? matched.getTypeName() : defaultType;
            Integer assignedTypeId = matched != null && matched.getSeatTypeId() != null ? matched.getSeatTypeId() : 1;
            String assignedColor = matched != null ? matched.getColorCode() : null;

            for (int c = 1; c <= cols; c++) {
                Seat s = new Seat();
                s.setRoomId(roomId);
                s.setRowLabel(rowLabel);
                s.setSeatNumber(c);
                s.setGridRow(r);
                s.setGridColumn(c);
                s.setSeatTypeName(assignedTypeName);
                s.setSeatTypeId(assignedTypeId);
                s.setColorCode(assignedColor);
                s.setStatus("Active");
                generated.add(s);
            }
        }

        return saveRoomLayout(roomId, generated);
    }

    public SeatType resolveSeatType(String typeName) {
        if (typeName == null || typeName.isBlank()) return null;
        try {
            List<SeatType> all = seatTypeDao.findAll("ALL");
            for (SeatType st : all) {
                if (isSeatTypeMatching(st, typeName)) {
                    return st;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Integer resolveSeatTypeId(String typeName) {
        SeatType st = resolveSeatType(typeName);
        return st != null && st.getSeatTypeId() != null ? st.getSeatTypeId() : 1;
    }

    private Room ensureRoomExists(int roomId) {
        return roomDao.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    private SeatType validateSeatType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            throw new InvalidSeatLayoutException("Invalid seat type: Seat type name cannot be empty.");
        }
        List<SeatType> validTypes = seatTypeDao.findAll("Active");
        if (validTypes != null && !validTypes.isEmpty()) {
            for (SeatType st : validTypes) {
                if (isSeatTypeMatching(st, typeName)) {
                    return st;
                }
            }
            throw new InvalidSeatLayoutException("Invalid or inactive seat type: '" + typeName + "'. Must be an active seat type.");
        }
        if (!"Standard".equalsIgnoreCase(typeName) && !"VIP".equalsIgnoreCase(typeName) && !"Couple".equalsIgnoreCase(typeName) && !"Front".equalsIgnoreCase(typeName)) {
            throw new InvalidSeatLayoutException("Invalid seat type: '" + typeName + "'. Must be an active seat type.");
        }
        return null;
    }

    private boolean isSeatTypeMatching(SeatType st, String input) {
        if (st == null || input == null) return false;
        String raw = input.trim();
        String low = raw.toLowerCase();
        String stName = st.getTypeName() != null ? st.getTypeName().trim() : "";
        String stCode = st.getTypeCode() != null ? st.getTypeCode().trim() : "";

        if (stName.equalsIgnoreCase(raw) || stCode.equalsIgnoreCase(raw)) {
            return true;
        }
        if (!stName.isEmpty() && stName.toLowerCase().contains(low)) {
            return true;
        }
        // Common Aliases
        if (low.equals("standard") && (stCode.equalsIgnoreCase("STD") || stName.toLowerCase().contains("standard"))) {
            return true;
        }
        if ((low.equals("couple") || low.equals("sweetbox")) && (stCode.equalsIgnoreCase("COUPLE") || stName.toLowerCase().contains("couple") || stName.toLowerCase().contains("sweetbox"))) {
            return true;
        }
        if (low.equals("vip") && (stCode.equalsIgnoreCase("VIP") || stName.toLowerCase().contains("vip"))) {
            return true;
        }
        if (low.equals("front")) {
            if (stCode.equalsIgnoreCase("FRONT") || stName.toLowerCase().contains("front")) {
                return true;
            }
            if (stCode.equalsIgnoreCase("STD") || stName.toLowerCase().contains("standard")) {
                return true;
            }
        }
        return false;
    }

    private void validateSeatStatus(String status) {
        if (!"Active".equalsIgnoreCase(status) && !"Maintenance".equalsIgnoreCase(status) && !"Lock".equalsIgnoreCase(status) && !"Locked".equalsIgnoreCase(status) && !"Unavailable".equalsIgnoreCase(status)) {
            throw new InvalidSeatLayoutException("Invalid seat status: '" + status + "'. Allowed: Active, Lock.");
        }
    }
}
