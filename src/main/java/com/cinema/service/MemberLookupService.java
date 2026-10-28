package com.cinema.service;

import com.cinema.dao.MemberLookupDAO;
import com.cinema.model.MemberLookupResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class MemberLookupService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{9,15}$");

    private final MemberLookupDAO memberLookupDAO;

    public MemberLookupService(MemberLookupDAO memberLookupDAO) {
        this.memberLookupDAO = memberLookupDAO;
    }

    /** Searches by a single identifier: values containing "@" are treated as email, otherwise phone. */
    public MemberLookupResult search(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a phone number or email address.");
        }
        String value = identifier.trim();
        return value.contains("@") ? search(null, value) : search(value, null);
    }

    public MemberLookupResult search(String phone, String email) {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (hasPhone == hasEmail) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one phone number or email address.");
        }
        Optional<MemberLookupResult> member;
        if (hasEmail) {
            String normalizedEmail = email.trim();
            if (!EMAIL.matcher(normalizedEmail).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Enter a valid email address.");
            }
            member = memberLookupDAO.findByEmail(normalizedEmail);
        } else {
            String normalizedPhone = normalizePhone(phone);
            if (!PHONE.matcher(normalizedPhone).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Enter a valid phone number (9-15 digits).");
            }
            member = memberLookupDAO.findByPhone(normalizedPhone);
        }
        return member.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                hasEmail ? "No active member is registered with this email."
                         : "No active member is registered with this phone number."));
    }

    /** Removes spaces, dots and dashes that staff often type when reading a phone number aloud. */
    static String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim().replaceAll("[\\s.\\-]", "");
    }
}
