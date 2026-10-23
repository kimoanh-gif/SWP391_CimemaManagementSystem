package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
import com.cinema.model.NewsArticle;
import com.cinema.service.PromotionNewsService;
import com.cinema.service.SessionService;
import com.cinema.util.PublicPageModelHelper;
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
class NewsDetailsControllerTest {

    @Mock
    private PromotionNewsService promotionNewsService;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private HttpSession session;

    private NewsDetailsController controller;

    @BeforeEach
    void setUp() {
        controller = new NewsDetailsController(promotionNewsService,
                new PublicPageModelHelper(sessionService, customerDAO));
    }

    @Test
    void testNewsDetailsFound() {
        NewsArticle article = new NewsArticle();
        article.setNewsId(2);
        article.setTitle("Article 2");

        when(promotionNewsService.getNewsById(2)).thenReturn(Optional.of(article));
        when(promotionNewsService.getOtherNews(2, 3)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.newsDetails(2, session, model, new RedirectAttributesModelMap());

        assertEquals("customer/promotions-news/news-details", view);
        assertEquals(article, model.getAttribute("news"));
    }

    @Test
    void testUnpublishedNewsRedirectsWithMessage() {
        when(promotionNewsService.getNewsById(8)).thenReturn(Optional.empty());

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String view = controller.newsDetails(8, session, new ConcurrentModel(), ra);

        assertEquals("redirect:/promotions-news?tab=news", view);
        assertTrue(ra.getFlashAttributes().containsKey("errorMessage"));
    }
}
