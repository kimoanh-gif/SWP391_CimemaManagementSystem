package com.cinema.exception;

/**
 * Thrown when attempting to deactivate a room that still has upcoming showtimes.
 * Enforces Business Rule BR-52.
 */
public class RoomHasActiveShowtimesException extends RuntimeException {
    public RoomHasActiveShowtimesException(int roomId) {
        super("Phòng chiếu (ID: " + roomId + ") đang có các suất chiếu sắp diễn ra hoặc chưa kết thúc nên không thể xóa.");
    }
}
