package com.cinema.service;

import com.cinema.dao.MemberLookupDAO;
import com.cinema.model.MemberLookupResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberLookupServiceTest {

    @Mock
    private MemberLookupDAO memberLookupDAO;

    private MemberLookupService service;

    @BeforeEach
    void setUp() {
        service = new MemberLookupService(memberLookupDAO);
    }

    @Test
    void searchesByEmailWhenIdentifierContainsAtSign() {
        when(memberLookupDAO.findByEmail("Member@Example.com")).thenReturn(Optional.of(member()));

        MemberLookupResult result = service.search("  Member@Example.com ");

        assertEquals(7, result.customerId());
        verify(memberLookupDAO, never()).findByPhone(anyString());
    }

    @Test
    void normalizesPhoneBeforeSearching() {
        when(memberLookupDAO.findByPhone("0900000004")).thenReturn(Optional.of(member()));

        MemberLookupResult result = service.search("0900 000-004");

        assertEquals(7, result.customerId());
    }

    @Test
    void rejectsMalformedEmailWithoutQueryingDatabase() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.search("member@"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(memberLookupDAO, never()).findByEmail(anyString());
    }

    @Test
    void returnsNotFoundWhenNoActiveMemberHasEmail() {
        when(memberLookupDAO.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.search("nobody@example.com"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private MemberLookupResult member() {
        return new MemberLookupResult(7, "Member", "0900000004", "member@example.com",
                "STANDARD", 0, 0, List.of());
    }
}
