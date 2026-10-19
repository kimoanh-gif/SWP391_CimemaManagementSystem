package com.cinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedManagerException extends RuntimeException {
    public UnauthorizedManagerException(String message) {
        super(message);
    }
}
