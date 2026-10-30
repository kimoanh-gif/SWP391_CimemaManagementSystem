package com.cinema.service;

import com.cinema.dao.StaffNewsDAO;
import com.cinema.dto.NewsRequest;
import com.cinema.model.StaffNewsArticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffNewsServiceTest {

    @Mock
    private StaffNewsDAO staffNewsDAO;

    private StaffNewsService service;

    @BeforeEach
    void setUp() {
        service = new StaffNewsService(staffNewsDAO);
    }

    @Test
    void savesDraftWithoutThumbnail() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));
        when(staffNewsDAO.insert(6, "Title", null, "Body", "DRAFT")).thenReturn(30);
        when(staffNewsDAO.findById(30)).thenReturn(Optional.of(article(30, "DRAFT", null)));

        StaffNewsArticle saved = service.create(15, new NewsRequest("Title", " ", "Body", "draft"));

        assertEquals("DRAFT", saved.status());
    }

    @Test
    void rejectsSubmissionWithoutThumbnail() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(15, new NewsRequest("Title", null, "Body", "PENDING")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Thumbnail image is required to submit for approval.", exception.getReason());
        verify(staffNewsDAO, never()).insert(anyInt(), any(), any(), any(), any());
    }

    @Test
    void submitsWithThumbnail() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));
        when(staffNewsDAO.insert(6, "Title", "https://img/x.jpg", "Body", "PENDING")).thenReturn(31);
        when(staffNewsDAO.findById(31)).thenReturn(Optional.of(article(31, "PENDING", "https://img/x.jpg")));

        StaffNewsArticle saved = service.create(15,
                new NewsRequest("Title", "https://img/x.jpg", "Body", "PENDING"));

        assertEquals("PENDING", saved.status());
    }

    @Test
    void rejectsResubmittingRejectedNewsWithoutThumbnail() {
        when(staffNewsDAO.findById(32)).thenReturn(Optional.of(article(32, "REJECTED", null)));
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(15, "STAFF", 32, new NewsRequest("Title", "", "Body", "PENDING")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(staffNewsDAO, never()).update(anyInt(), any(), anyBoolean(), any(), any(), any(), any());
    }

    private static StaffNewsArticle article(int id, String status, String thumbnailUrl) {
        return new StaffNewsArticle(id, 6, "Title", "Body", thumbnailUrl, status, null, null, null);
    }
}
