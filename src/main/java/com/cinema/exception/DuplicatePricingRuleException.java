package com.cinema.exception;

/**
 * Thrown when an active ticket pricing rule conflicts with an existing active rule
 * for the same combination of (DayType, TimeSlot, ExperienceType) per P1-03.
 * Mapped to HTTP 409 Conflict.
 */
public class DuplicatePricingRuleException extends RuntimeException {

    public DuplicatePricingRuleException(String message) {
        super(message);
    }
}
