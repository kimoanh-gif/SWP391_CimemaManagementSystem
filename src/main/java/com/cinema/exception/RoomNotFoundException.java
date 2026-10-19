package com.cinema.exception;

/**
 * Thrown when a Room with the given ID cannot be found.
 */
public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(int roomId) {
        super("Room not found with ID: " + roomId);
    }
}
