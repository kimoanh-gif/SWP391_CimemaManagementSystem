package com.cinema.controller;

import com.cinema.model.ManagerNewsFilter;
import com.cinema.model.ManagerNewsItem;
import com.cinema.model.ManagerNewsStats;
import com.cinema.service.ManagerNewsService;
import com.cinema.service.ManagerNewsService.NewsReviewException;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerNewsControllerTest {

    @Mock
    private ManagerNewsService newsService;

    @Mock
    private SessionService sessionService;

    @Mock
    private HttpSession session;

    private ManagerNewsController controller;

    @BeforeEach
    void setUp() {
        controller = new ManagerNewsController(newsService, sessionService);
    }

    @Test
    void nonManagerIsSentToLogin() {
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(false);

        assertEquals("redirect:/login",
                controller.viewNewsManagement(null, null, null, null, null, null, session, new ConcurrentModel()));
    }

    @Test
    void listDefaultsToPendingFilter() {
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(true);
        when(newsService.getStats()).thenReturn(ManagerNewsStats.empty());
        when(newsService.search(any(ManagerNewsFilter.class)))
                .thenReturn(new ManagerNewsService.NewsPage(List.of(), 0, 1, 10));
        when(newsService.getReviewHistory()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.viewNewsManagement(null, null, null, null, null, null, session, model);

        assertEquals("manager/news", view);
        assertEquals("PENDING", ((ManagerNewsFilter) model.getAttribute("filter")).status());
    }

    @Test
    void rejectErrorIsShownAsFlashMessage() {
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(true);
        when(sessionService.currentAccountId(session)).thenReturn(2);
        when(newsService.reject(5, 2, "")).thenThrow(new NewsReviewException("Please enter a rejection reason before confirming."));

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.reject(5, "", "status=PENDING", session, redirect);

        assertEquals("redirect:/manager/news?status=PENDING", view);
        assertTrue(redirect.getFlashAttributes().containsKey("errorMessage"));
    }

    @Test
    void approveSuccessKeepsFilters() {
        ManagerNewsItem item = new ManagerNewsItem();
        item.setNewsId(5);
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(true);
        when(sessionService.currentAccountId(session)).thenReturn(2);
        when(newsService.approve(5, 2, null)).thenReturn(item);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.approve(5, "NOW", null, "status=PENDING&view=5&page=2", session, redirect);

        assertEquals("redirect:/manager/news?status=PENDING&page=2", view);
        assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    void unsafeReturnQueryIsIgnored() {
        assertEquals("redirect:/manager/news", ManagerNewsController.backToList("x=<script>"));
        assertEquals("redirect:/manager/news", ManagerNewsController.backToList(null));
    }
}
