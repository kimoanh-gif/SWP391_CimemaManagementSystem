package com.cinema.exception;

public class InvalidPricingConfigurationException extends RuntimeException {
    public InvalidPricingConfigurationException(String message) {
        super(message);
    }
}
