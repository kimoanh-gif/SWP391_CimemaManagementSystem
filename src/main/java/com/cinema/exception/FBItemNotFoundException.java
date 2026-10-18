package com.cinema.exception;

public class FBItemNotFoundException extends RuntimeException {
    public FBItemNotFoundException(String message) {
        super(message);
    }
}
