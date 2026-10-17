package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.NewsArticle;
import com.cinema.model.Promotion;
import com.cinema.service.PromotionNewsService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionNewsPublicControllerTest {

    @Mock
    private PromotionNewsService promotionNewsService;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private HttpSession session;

    private PromotionNewsPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new PromotionNewsPublicController(promotionNewsService, sessionService, customerDAO);
    }

    @Test
    void testListPromotionsAndNews() {
        when(promotionNewsService.getActivePromotions()).thenReturn(List.of(new Promotion()));
        when(promotionNewsService.getPublishedNews()).thenReturn(List.of(new NewsArticle()));

        Model model = new ConcurrentModel();
        String view = controller.listPromotionsAndNews("all", session, model);

        assertEquals("customer/promotions-news/list", view);
        assertTrue(model.containsAttribute("promotions"));
        assertTrue(model.containsAttribute("newsList"));
    }

    @Test
    void testPromotionDetailsFound() {
        Promotion promo = new Promotion();
        promo.setPromotionId(1);
        promo.setName("Promo 1");

        when(promotionNewsService.getPromotionById(1)).thenReturn(Optional.of(promo));
        when(promotionNewsService.getLatestPromotions(3)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.promotionDetails(1, session, model, new RedirectAttributesModelMap());

        assertEquals("customer/promotions-news/promotion-details", view);
        assertEquals(promo, model.getAttribute("promotion"));
    }

    @Test
    void testNewsDetailsFound() {
        NewsArticle article = new NewsArticle();
        article.setNewsId(2);
        article.setTitle("Article 2");

        when(promotionNewsService.getNewsById(2)).thenReturn(Optional.of(article));
        when(promotionNewsService.getLatestNews(3)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.newsDetails(2, session, model, new RedirectAttributesModelMap());

        assertEquals("customer/promotions-news/news-details", view);
        assertEquals(article, model.getAttribute("news"));
    }
}
