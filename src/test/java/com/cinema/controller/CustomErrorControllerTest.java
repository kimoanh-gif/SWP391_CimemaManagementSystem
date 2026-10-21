package com.cinema.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;

class CustomErrorControllerTest {
    private final CustomErrorController controller = new CustomErrorController();

    @Test
    void internalServerErrorUsesCloudflareStylePage() {
        Model model = new ConcurrentModel();
        assertEquals("error-500", controller.handleError(requestWithStatus(500), model));
        assertEquals(500, model.getAttribute("status"));
        assertNotNull(model.getAttribute("rayId"));
        assertEquals("localhost", model.getAttribute("host"));
        assertNull(model.getAttribute("trace"));
    }

    @Test
    void notFoundUsesCinemaErrorPage() {
        Model model = new ConcurrentModel();
        assertEquals("error", controller.handleError(requestWithStatus(404), model));
        assertEquals("Không tìm thấy trang", model.getAttribute("headline"));
        assertFalse(model.containsAttribute("rayId"));
    }

    @Test
    void otherServerErrorsDoNotUseThe500Page() {
        Model model = new ConcurrentModel();
        assertEquals("error", controller.handleError(requestWithStatus(503), model));
        assertEquals("Hệ thống đang bảo trì", model.getAttribute("headline"));
    }

    private MockHttpServletRequest requestWithStatus(int status) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/broken-page");
        return request;
    }
}
