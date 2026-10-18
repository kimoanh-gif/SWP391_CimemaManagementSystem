package com.cinema.service;

import com.cinema.dao.MemberLookupDAO;
import com.cinema.model.MemberLookupResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberLookupService {

    private final MemberLookupDAO memberLookupDAO;

    public MemberLookupService(MemberLookupDAO memberLookupDAO) {
        this.memberLookupDAO = memberLookupDAO;
    }

    public MemberLookupResult search(String phone, String email) {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (hasPhone == hasEmail) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide exactly one phone number or email address.");
        }
        return (hasPhone ? memberLookupDAO.findByPhone(phone) : memberLookupDAO.findByEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active member matched the search."));
    }
}
