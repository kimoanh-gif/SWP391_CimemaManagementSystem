package com.cinema.service;

import com.cinema.dao.ManagerNewsDAO;
import com.cinema.model.ManagerNewsItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerNewsServiceTest {

    private static final int MANAGER_ID = 2;

    @Mock
    private ManagerNewsDAO newsDAO;

    private ManagerNewsService service;

    @BeforeEach
    void setUp() {
        service = new ManagerNewsService(newsDAO);
    }

    @Test
    void approveAndPublishNowMovesPendingArticleToPublished() {
        stubNews(7, "PENDING");
        when(newsDAO.approve(7, MANAGER_ID, true)).thenReturn(true);

        String message = service.approve(7, MANAGER_ID, true);

        assertTrue(message.contains("#NEWS-007"));
        verify(newsDAO).insertLog(7, MANAGER_ID, "APPROVE", "PENDING", "PUBLISHED", "Title");
    }

    @Test
    void approveOnlyKeepsArticleApproved() {
        stubNews(7, "PENDING");
        when(newsDAO.approve(7, MANAGER_ID, false)).thenReturn(true);

        service.approve(7, MANAGER_ID, false);

        verify(newsDAO).insertLog(7, MANAGER_ID, "APPROVE", "PENDING", "APPROVED", "Title");
    }

    @Test
    void cannotApproveAnArticleThatIsNotPending() {
        stubNews(7, "PUBLISHED");

        assertThrows(ManagerNewsService.NewsActionException.class, () -> service.approve(7, MANAGER_ID, true));
        verify(newsDAO, never()).approve(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void rejectRequiresAReason() {
        assertThrows(ManagerNewsService.NewsActionException.class, () -> service.reject(7, MANAGER_ID, "   "));
        verify(newsDAO, never()).reject(anyInt(), anyInt(), anyString());
    }

    @Test
    void rejectRefusesReasonsLongerThanTheLimit() {
        String longReason = "x".repeat(ManagerNewsService.REJECT_REASON_MAX + 1);
        assertThrows(ManagerNewsService.NewsActionException.class, () -> service.reject(7, MANAGER_ID, longReason));
        verify(newsDAO, never()).reject(anyInt(), anyInt(), anyString());
    }

    @Test
    void rejectStoresTheTrimmedReason() {
        stubNews(7, "PENDING");
        when(newsDAO.reject(7, MANAGER_ID, "Missing terms")).thenReturn(true);

        service.reject(7, MANAGER_ID, "  Missing terms  ");

        verify(newsDAO).insertLog(7, MANAGER_ID, "REJECT", "PENDING", "REJECTED", "Missing terms");
    }

    @Test
    void concurrentChangeIsReportedWhenTheConditionalUpdateMissesTheRow() {
        stubNews(7, "APPROVED");
        when(newsDAO.publish(7, MANAGER_ID)).thenReturn(false);

        assertThrows(ManagerNewsService.NewsActionException.class, () -> service.publish(7, MANAGER_ID));
        verify(newsDAO, never()).insertLog(anyInt(), anyInt(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void unpublishOnlyWorksForPublishedArticles() {
        stubNews(7, "APPROVED");
        assertThrows(ManagerNewsService.NewsActionException.class, () -> service.unpublish(7, MANAGER_ID));
    }

    @Test
    void reopenSendsRejectedArticleBackToPending() {
        stubNews(7, "REJECTED");
        when(newsDAO.reopen(7)).thenReturn(true);

        service.reopen(7, MANAGER_ID);

        verify(newsDAO).insertLog(7, MANAGER_ID, "REOPEN", "REJECTED", "PENDING", "Title");
    }

    @Test
    void missingArticleGivesAFriendlyError() {
        when(newsDAO.findById(99)).thenReturn(Optional.empty());
        assertThrows(ManagerNewsService.NewsActionException.class, () -> service.publish(99, MANAGER_ID));
    }

    @Test
    void unknownStatusFilterMeansAllStatuses() {
        assertNull(ManagerNewsService.normalizeStatus("ALL"));
        assertNull(ManagerNewsService.normalizeStatus("hacked'--"));
        assertEquals("PENDING", ManagerNewsService.normalizeStatus("pending"));
    }

    @Test
    void periodStartUsesMondayForThisWeek() {
        LocalDate thursday = LocalDate.of(2026, 10, 22);
        assertEquals(LocalDate.of(2026, 10, 19).atStartOfDay(), ManagerNewsService.periodStart("week", thursday));
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(), ManagerNewsService.periodStart("month", thursday));
        assertNull(ManagerNewsService.periodStart("all", thursday));
    }

    private void stubNews(int id, String status) {
        ManagerNewsItem item = new ManagerNewsItem();
        item.setNewsId(id);
        item.setTitle("Title");
        item.setStatus(status);
        when(newsDAO.findById(id)).thenReturn(Optional.of(item));
    }
}
