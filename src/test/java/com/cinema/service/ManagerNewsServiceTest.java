package com.cinema.service;

import com.cinema.dao.ManagerNewsDAO;
import com.cinema.model.ManagerNewsFilter;
import com.cinema.model.ManagerNewsItem;
import com.cinema.service.ManagerNewsService.NewsReviewException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerNewsServiceTest {

    private static final int MANAGER = 2;

    @Mock
    private ManagerNewsDAO newsDAO;

    private ManagerNewsService service;

    @BeforeEach
    void setUp() {
        service = new ManagerNewsService(newsDAO);
    }

    @Test
    void approvePendingNewsPublishesImmediately() {
        when(newsDAO.findById(5)).thenReturn(Optional.of(news(5, "PENDING", null)));
        when(newsDAO.approve(5, MANAGER, null)).thenReturn(1);

        service.approve(5, MANAGER, null);

        verify(newsDAO).approve(5, MANAGER, null);
    }

    @Test
    void approveWithPastScheduleIsRejected() {
        when(newsDAO.findById(5)).thenReturn(Optional.of(news(5, "PENDING", null)));

        assertThrows(NewsReviewException.class,
                () -> service.approve(5, MANAGER, LocalDateTime.now().minusMinutes(1)));
        verify(newsDAO, never()).approve(anyInt(), anyInt(), any());
    }

    @Test
    void onlyPendingNewsCanBeApproved() {
        when(newsDAO.findById(6)).thenReturn(Optional.of(news(6, "DRAFT", null)));

        assertThrows(NewsReviewException.class, () -> service.approve(6, MANAGER, null));
        verify(newsDAO, never()).approve(anyInt(), anyInt(), isNull());
    }

    @Test
    void rejectRequiresReason() {
        assertThrows(NewsReviewException.class, () -> service.reject(5, MANAGER, "   "));
        verify(newsDAO, never()).reject(anyInt(), anyInt(), anyString());
    }

    @Test
    void rejectReasonIsLimitedTo300Characters() {
        assertThrows(NewsReviewException.class, () -> service.reject(5, MANAGER, "x".repeat(301)));
    }

    @Test
    void rejectPendingNewsStoresTrimmedReason() {
        when(newsDAO.findById(5)).thenReturn(Optional.of(news(5, "PENDING", null)));
        when(newsDAO.reject(5, MANAGER, "Missing detailed terms")).thenReturn(1);

        service.reject(5, MANAGER, "  Missing detailed terms ");

        verify(newsDAO).reject(5, MANAGER, "Missing detailed terms");
    }

    @Test
    void concurrentChangeIsReported() {
        when(newsDAO.findById(5)).thenReturn(Optional.of(news(5, "PENDING", null)));
        when(newsDAO.approve(5, MANAGER, null)).thenReturn(0);

        assertThrows(NewsReviewException.class, () -> service.approve(5, MANAGER, null));
    }

    @Test
    void archiveOnlyForPublishedNews() {
        when(newsDAO.findById(7)).thenReturn(Optional.of(news(7, "REJECTED", null)));

        assertThrows(NewsReviewException.class, () -> service.archive(7, MANAGER));
    }

    @Test
    void publishNowOnlyForScheduledNews() {
        when(newsDAO.findById(8)).thenReturn(Optional.of(news(8, "PUBLISHED", LocalDateTime.now().minusDays(1))));

        assertThrows(NewsReviewException.class, () -> service.publishNow(8, MANAGER));
    }

    @Test
    void searchClampsPageToLastPage() {
        ManagerNewsFilter filter = ManagerNewsFilter.of(null, "ALL", null, 9, 10);
        when(newsDAO.count(filter)).thenReturn(25);
        when(newsDAO.search(any())).thenReturn(List.of());

        ManagerNewsService.NewsPage page = service.search(filter);

        assertEquals(3, page.page());
        assertEquals(3, page.totalPages());
    }

    private static ManagerNewsItem news(int id, String status, LocalDateTime publishedAt) {
        ManagerNewsItem item = new ManagerNewsItem();
        item.setNewsId(id);
        item.setTitle("News " + id);
        item.setStatus(status);
        item.setPublishedAt(publishedAt);
        return item;
    }
}
