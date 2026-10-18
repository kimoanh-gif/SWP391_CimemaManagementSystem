package com.cinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateGenreException extends RuntimeException {
    public DuplicateGenreException(String message) {
        super(message);
    }
}
