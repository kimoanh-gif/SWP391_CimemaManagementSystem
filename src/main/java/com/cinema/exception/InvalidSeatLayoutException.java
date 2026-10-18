package com.cinema.exception;

public class InvalidSeatLayoutException extends RuntimeException {
    public InvalidSeatLayoutException(String message) {
        super(message);
    }
}
