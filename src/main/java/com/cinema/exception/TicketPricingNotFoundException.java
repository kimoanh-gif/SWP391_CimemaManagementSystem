package com.cinema.exception;

public class TicketPricingNotFoundException extends RuntimeException {
    public TicketPricingNotFoundException(String message) {
        super(message);
    }
}
