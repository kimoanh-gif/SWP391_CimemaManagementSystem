package com.cinema.controller;

import com.cinema.dao.CustomerDAO;
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
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionDetailsControllerTest {

    @Mock
    private PromotionNewsService promotionNewsService;

    @Mock
    private SessionService sessionService;

    @Mock
    private CustomerDAO customerDAO;

    @Mock
    private HttpSession session;

    private PromotionDetailsController controller;

    @BeforeEach
    void setUp() {
        controller = new PromotionDetailsController(promotionNewsService,
                new PublicPageModelHelper(sessionService, customerDAO));
    }

    @Test
    void testPromotionDetailsFound() {
        Promotion promo = new Promotion();
        promo.setPromotionId(1);
        promo.setName("Promo 1");

        when(promotionNewsService.getPromotionById(1)).thenReturn(Optional.of(promo));
        when(promotionNewsService.getOtherPromotions(1, 3)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.promotionDetails(1, session, model, new RedirectAttributesModelMap());

        assertEquals("customer/promotions-news/promotion-details", view);
        assertEquals(promo, model.getAttribute("promotion"));
    }

    @Test
    void testExpiredPromotionRedirectsWithMessage() {
        when(promotionNewsService.getPromotionById(9)).thenReturn(Optional.empty());

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String view = controller.promotionDetails(9, session, new ConcurrentModel(), ra);

        assertEquals("redirect:/promotions-news?tab=promotions", view);
        assertTrue(ra.getFlashAttributes().containsKey("errorMessage"));
    }
}
