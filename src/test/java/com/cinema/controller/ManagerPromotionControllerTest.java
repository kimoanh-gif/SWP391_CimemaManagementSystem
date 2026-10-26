package com.cinema.controller;

import com.cinema.dto.PromotionForm;
import com.cinema.exception.PromotionAdminException;
import com.cinema.model.AdminPage;
import com.cinema.model.ManagerPromotionFilter;
import com.cinema.model.ManagerPromotionRow;
import com.cinema.model.ManagerPromotionStats;
import com.cinema.service.ManagerPromotionService;
import com.cinema.service.ManagerVoucherService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** M11 controller. Assignee: Nguyễn Đức Anh */
@ExtendWith(MockitoExtension.class)
class ManagerPromotionControllerTest {

    @Mock
    private ManagerPromotionService promotionService;
    @Mock
    private ManagerVoucherService voucherService;
    @Mock
    private SessionService sessionService;
    @Mock
    private HttpSession session;

    private ManagerPromotionController controller;

    @BeforeEach
    void setUp() {
        controller = new ManagerPromotionController(promotionService, voucherService, sessionService);
    }

    @Test
    void nonManagerIsSentToLogin() {
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(false);

        assertEquals("redirect:/login", controller.list(null, null, null, null, null, session, new ConcurrentModel()));
    }

    @Test
    void listShowsPromotionTab() {
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(true);
        when(promotionService.search(any(ManagerPromotionFilter.class)))
                .thenReturn(AdminPage.of(List.<ManagerPromotionRow>of(), 1, 6));
        when(promotionService.getStats()).thenReturn(ManagerPromotionStats.empty());
        when(promotionService.getAll()).thenReturn(List.of());
        when(voucherService.countAll()).thenReturn(0);

        Model model = new ConcurrentModel();
        assertEquals("manager/promotions/list", controller.list(null, "bogus", null, null, 999, session, model));
        assertEquals("promotions", model.getAttribute("tab"));
        assertEquals("ALL", ((ManagerPromotionFilter) model.getAttribute("filter")).status());
        assertEquals(6, ((ManagerPromotionFilter) model.getAttribute("filter")).size());
    }

    @Test
    void invalidCreateReturnsToFormWithErrors() {
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(true);
        PromotionForm form = new PromotionForm();
        when(promotionService.create(form)).thenThrow(new PromotionAdminException("Please fix", Map.of("name", "Required")));

        Model model = new ConcurrentModel();
        assertEquals("manager/promotions/form", controller.create(form, session, model, new RedirectAttributesModelMap()));
        assertEquals(Map.of("name", "Required"), model.getAttribute("fieldErrors"));
        assertEquals("/manager/promotions", model.getAttribute("formAction"));
    }

    @Test
    void deleteOfUsedPromotionStaysOnDetail() {
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(true);
        when(promotionService.delete(4)).thenThrow(new PromotionAdminException("locked"));

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/manager/promotions/4", controller.delete(4, session, redirect));
        assertEquals("locked", redirect.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void statusChangeFromListReturnsToList() {
        when(sessionService.hasRole(session, "MANAGER")).thenReturn(true);
        ManagerPromotionRow row = new ManagerPromotionRow();
        row.setName("Deal");
        when(promotionService.changeStatus(3, "PAUSED")).thenReturn(row);

        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        assertEquals("redirect:/manager/promotions", controller.changeStatus(3, "PAUSED", "list", session, redirect));
        assertEquals("Promotion \"Deal\" has been paused.", redirect.getFlashAttributes().get("successMessage"));
    }
}
