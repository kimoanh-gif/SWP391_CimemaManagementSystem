package com.cinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidMovieStatusException extends RuntimeException {
    public InvalidMovieStatusException(String message) {
        super(message);
    }
}
