package com.cinema.service;

import com.cinema.dao.StaffNewsDAO;
import com.cinema.model.NewsRequest;
import com.cinema.model.StaffNewsArticle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffNewsServiceTest {

    @Mock
    private StaffNewsDAO staffNewsDAO;

    @Test
    void createsPendingNewsWithTrimmedFields() {
        StaffNewsService service = new StaffNewsService(staffNewsDAO);
        when(staffNewsDAO.findActiveStaffId(4)).thenReturn(Optional.of(8));
        when(staffNewsDAO.insert(8, "Premiere", "Opening soon", null, "Details", "PENDING")).thenReturn(12);
        StaffNewsArticle article = article(12, 8, "PENDING");
        when(staffNewsDAO.findById(12)).thenReturn(Optional.of(article));

        assertEquals(article, service.create(4, request(" pending ")));
        verify(staffNewsDAO).insert(8, "Premiere", "Opening soon", null, "Details", "PENDING");
    }

    @Test
    void allowsManagerToUpdateRejectedNews() {
        StaffNewsService service = new StaffNewsService(staffNewsDAO);
        when(staffNewsDAO.findById(12)).thenReturn(Optional.of(article(12, 8, "REJECTED")))
                .thenReturn(Optional.of(article(12, 8, "DRAFT")));
        when(staffNewsDAO.update(12, null, true, "Premiere", "Opening soon", null,
                "Details", "DRAFT")).thenReturn(1);

        assertEquals("DRAFT", service.update(22, "MANAGER", 12, request("draft")).status());
        verify(staffNewsDAO).update(12, null, true, "Premiere", "Opening soon", null,
                "Details", "DRAFT");
    }

    @Test
    void refusesToUpdatePublishedNews() {
        StaffNewsService service = new StaffNewsService(staffNewsDAO);
        when(staffNewsDAO.findById(12)).thenReturn(Optional.of(article(12, 8, "PUBLISHED")));

        assertThrows(ResponseStatusException.class,
                () -> service.update(4, "STAFF", 12, request("DRAFT")));

        verify(staffNewsDAO, never()).update(anyInt(), any(), anyBoolean(), anyString(),
                anyString(), any(), anyString(), anyString());
    }

    private NewsRequest request(String status) {
        return new NewsRequest(" Premiere ", " Opening soon ", " ", " Details ", status);
    }

    private StaffNewsArticle article(int id, int staffId, String status) {
        return new StaffNewsArticle(id, staffId, "Premiere", "Opening soon", "Details",
                null, status, null, null, null);
    }
}
