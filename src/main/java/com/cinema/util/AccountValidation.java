package com.cinema.util;

import java.util.regex.Pattern;

/** Account field rules shared by registration and staff account creation. */
public final class AccountValidation {
    public static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    public static final Pattern PHONE = Pattern.compile("^(0|\\+84)[0-9]{9,10}$");
    public static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{4,50}$");
    public static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,128}$");

    private AccountValidation() {}
}
