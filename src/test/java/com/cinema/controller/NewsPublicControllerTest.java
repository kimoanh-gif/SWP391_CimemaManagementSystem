package com.cinema.controller;

import com.cinema.model.NewsArticle;
import com.cinema.service.CustomerHeaderModelService;
import com.cinema.service.NewsPublicService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsPublicControllerTest {

    @Mock
    private NewsPublicService newsService;

    @Mock
    private CustomerHeaderModelService headerModelService;

    @Mock
    private HttpSession session;

    private NewsPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new NewsPublicController(newsService, headerModelService);
    }

    @Test
    void listShowsPublishedNews() {
        when(newsService.getPublishedNews()).thenReturn(List.of(article(1), article(2)));

        Model model = new ConcurrentModel();
        String view = controller.listNews(session, model);

        assertEquals("customer/promotions-news/news", view);
        assertEquals(2, ((List<?>) model.getAttribute("newsList")).size());
        assertEquals(false, model.getAttribute("loadError"));
        assertEquals("news", model.getAttribute("activeTab"));
    }

    @Test
    void listShowsRetryStateWhenDatabaseFails() {
        when(newsService.getPublishedNews()).thenThrow(new DataAccessResourceFailureException("db down"));

        Model model = new ConcurrentModel();
        controller.listNews(session, model);

        assertEquals(true, model.getAttribute("loadError"));
    }

    @Test
    void detailShowsPublishedArticle() {
        NewsArticle current = article(5);
        when(newsService.getPublishedArticle(5)).thenReturn(Optional.of(current));
        when(newsService.getLatestNews(4)).thenReturn(List.of(current, article(6), article(7)));

        Model model = new ConcurrentModel();
        String view = controller.newsDetails(5, session, model, new RedirectAttributesModelMap());

        assertEquals("customer/promotions-news/news-details", view);
        assertEquals(current, model.getAttribute("news"));
        assertEquals(2, ((List<?>) model.getAttribute("otherNews")).size());
    }

    @Test
    void unpublishedArticleCannotBeOpenedById() {
        when(newsService.getPublishedArticle(8)).thenReturn(Optional.empty());

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.newsDetails(8, session, new ConcurrentModel(), redirect);

        assertEquals("redirect:/news", view);
        assertTrue(redirect.getFlashAttributes().containsKey("errorMessage"));
    }

    private static NewsArticle article(int id) {
        NewsArticle a = new NewsArticle();
        a.setNewsId(id);
        a.setTitle("News " + id);
        return a;
    }
}
