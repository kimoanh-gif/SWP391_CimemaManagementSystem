package com.cinema.exception;

/**
 * Thrown when attempting to deactivate a room that still has upcoming showtimes.
 * Enforces Business Rule BR-52.
 */
public class RoomHasActiveShowtimesException extends RuntimeException {
    public RoomHasActiveShowtimesException(int roomId) {
        super("Room ID " + roomId + " has upcoming showtimes and cannot be deactivated.");
    }
}
