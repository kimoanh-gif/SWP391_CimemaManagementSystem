package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.NewsArticle;
import com.cinema.model.Promotion;
import com.cinema.service.PromotionNewsService;
import com.cinema.service.SessionService;
import com.cinema.util.PublicPageModelHelper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionNewsListControllerTest {

    @Mock
    private PromotionNewsService promotionNewsService;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private HttpSession session;

    private PromotionNewsListController controller;

    @BeforeEach
    void setUp() {
        controller = new PromotionNewsListController(promotionNewsService,
                new PublicPageModelHelper(sessionService, customerDAO));
    }

    @Test
    void testAllTabShowsPromotionsAndNews() {
        when(promotionNewsService.normalizeTab("all")).thenReturn("all");
        when(promotionNewsService.getActivePromotions()).thenReturn(List.of(new Promotion()));
        when(promotionNewsService.getPublishedNews()).thenReturn(List.of(new NewsArticle()));

        Model model = new ConcurrentModel();
        String view = controller.listPromotionsAndNews("all", session, model);

        assertEquals("customer/promotions-news/list", view);
        assertEquals(1, ((List<?>) model.getAttribute("promotions")).size());
        assertEquals(1, ((List<?>) model.getAttribute("newsList")).size());
    }

    @Test
    void testNewsTabDoesNotLoadPromotions() {
        when(promotionNewsService.normalizeTab("news")).thenReturn("news");
        when(promotionNewsService.getPublishedNews()).thenReturn(List.of());

        Model model = new ConcurrentModel();
        controller.listPromotionsAndNews("news", session, model);

        verify(promotionNewsService, never()).getActivePromotions();
        assertEquals("news", model.getAttribute("activeTab"));
    }

    @Test
    void testNewsStillShownWhenPromotionsFail() {
        when(promotionNewsService.normalizeTab("all")).thenReturn("all");
        when(promotionNewsService.getActivePromotions()).thenThrow(new DataAccessResourceFailureException("down"));
        when(promotionNewsService.getPublishedNews()).thenReturn(List.of(new NewsArticle()));

        Model model = new ConcurrentModel();
        controller.listPromotionsAndNews("all", session, model);

        assertEquals(true, model.getAttribute("loadError"));
        assertEquals(1, ((List<?>) model.getAttribute("newsList")).size());
    }

    @Test
    void testShortLinksRedirectToTabs() {
        assertEquals("redirect:/promotions-news?tab=promotions", controller.promotionsTab());
        assertEquals("redirect:/promotions-news?tab=news", controller.newsTab());
    }
}
