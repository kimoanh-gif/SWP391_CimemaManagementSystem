package com.cinema.model;

/** Published inside the payment transaction; email is dispatched only after commit. */
public record BookingPaidEvent(int bookingId) {}
