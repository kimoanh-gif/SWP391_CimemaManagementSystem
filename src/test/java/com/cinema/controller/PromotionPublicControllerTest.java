package com.cinema.controller;

import com.cinema.model.Promotion;
import com.cinema.service.CustomerHeaderModelService;
import com.cinema.service.PromotionPublicService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionPublicControllerTest {

    @Mock
    private PromotionPublicService promotionService;

    @Mock
    private CustomerHeaderModelService headerModelService;

    @Mock
    private HttpSession session;

    private PromotionPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new PromotionPublicController(promotionService, headerModelService);
    }

    @Test
    void listShowsActivePromotions() {
        when(promotionService.getActivePromotions()).thenReturn(List.of(promotion(1)));

        Model model = new ConcurrentModel();
        String view = controller.listPromotions(session, model);

        assertEquals("customer/promotions-news/promotions", view);
        assertEquals(1, ((List<?>) model.getAttribute("promotions")).size());
        assertEquals(false, model.getAttribute("loadError"));
        assertEquals("promotions", model.getAttribute("activeTab"));
    }

    @Test
    void listShowsRetryStateWhenDatabaseFails() {
        when(promotionService.getActivePromotions())
                .thenThrow(new DataAccessResourceFailureException("db down"));

        Model model = new ConcurrentModel();
        String view = controller.listPromotions(session, model);

        assertEquals("customer/promotions-news/promotions", view);
        assertEquals(true, model.getAttribute("loadError"));
        assertTrue(((List<?>) model.getAttribute("promotions")).isEmpty());
    }

    @Test
    void detailShowsPublicPromotionAndExcludesItFromOthers() {
        Promotion current = promotion(1);
        when(promotionService.getPublicPromotion(1)).thenReturn(Optional.of(current));
        when(promotionService.getLatestPromotions(4)).thenReturn(List.of(current, promotion(2)));

        Model model = new ConcurrentModel();
        String view = controller.promotionDetails(1, session, model, new RedirectAttributesModelMap());

        assertEquals("customer/promotions-news/promotion-details", view);
        assertEquals(current, model.getAttribute("promotion"));
        List<?> others = (List<?>) model.getAttribute("otherPromotions");
        assertEquals(1, others.size());
        assertFalse(others.contains(current));
    }

    @Test
    void detailOfExpiredOrMissingPromotionRedirectsToList() {
        when(promotionService.getPublicPromotion(99)).thenReturn(Optional.empty());

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = controller.promotionDetails(99, session, new ConcurrentModel(), redirect);

        assertEquals("redirect:/promotions", view);
        assertTrue(redirect.getFlashAttributes().containsKey("errorMessage"));
    }

    @Test
    void legacyUrlRedirectsToTheRightTab() {
        assertEquals("redirect:/promotions", controller.legacyPromotionsNews(null));
        assertEquals("redirect:/promotions", controller.legacyPromotionsNews("promotions"));
        assertEquals("redirect:/news", controller.legacyPromotionsNews("news"));
    }

    private static Promotion promotion(int id) {
        Promotion p = new Promotion();
        p.setPromotionId(id);
        p.setName("Promo " + id);
        return p;
    }
}
