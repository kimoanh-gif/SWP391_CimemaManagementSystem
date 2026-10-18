package com.cinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class GenreAssociationException extends RuntimeException {
    public GenreAssociationException(String message) {
        super(message);
    }
}
