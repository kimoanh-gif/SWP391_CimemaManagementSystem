package com.cinema.service;

import com.cinema.dao.MemberLookupDAO;
import com.cinema.model.MemberLookupResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberLookupServiceTest {

    @Mock
    private MemberLookupDAO memberLookupDAO;

    @Test
    void searchesByPhone() {
        MemberLookupResult expected = new MemberLookupResult(2, "Sample Customer", "0900000000",
                "sample@example.com", "STANDARD", 20, 20, List.of());
        when(memberLookupDAO.findByPhone("0900000000")).thenReturn(Optional.of(expected));

        assertEquals(expected, new MemberLookupService(memberLookupDAO).search("0900000000", null));
        verify(memberLookupDAO).findByPhone("0900000000");
    }

    @Test
    void requiresExactlyOneSearchValue() {
        MemberLookupService service = new MemberLookupService(memberLookupDAO);

        assertThrows(ResponseStatusException.class, () -> service.search(null, null));
        assertThrows(ResponseStatusException.class,
                () -> service.search("0900000000", "a@example.com"));
        verifyNoInteractions(memberLookupDAO);
    }

    @Test
    void returnsNotFoundWhenNoMemberMatches() {
        when(memberLookupDAO.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new MemberLookupService(memberLookupDAO).search(null, "missing@example.com"));

        assertEquals(404, exception.getStatusCode().value());
    }
}
