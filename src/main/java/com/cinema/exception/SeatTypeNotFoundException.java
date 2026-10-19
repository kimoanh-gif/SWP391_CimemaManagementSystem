package com.cinema.exception;

public class SeatTypeNotFoundException extends RuntimeException {
    public SeatTypeNotFoundException(String message) {
        super(message);
    }
}
