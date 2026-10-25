package com.cinema.exception;

import java.util.Map;

/**
 * Business / validation error on the Manager Promotion &amp; Voucher screens (M11).
 * fieldErrors maps a form field name to its message.
 */
public class PromotionAdminException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public PromotionAdminException(String message) {
        this(message, Map.of());
    }

    public PromotionAdminException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? Map.copyOf(fieldErrors) : Map.of();
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
