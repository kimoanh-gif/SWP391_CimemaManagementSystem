package com.cinema.controller;

import com.cinema.service.CustomerHeaderModelService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class PolicyControllerTest {
    private final CustomerHeaderModelService header = mock(CustomerHeaderModelService.class);
    private final PolicyController controller = new PolicyController(header);
    private final HttpSession session = mock(HttpSession.class);

    @Test
    void policiesIndexHasAWorkingViewAndPolicyList() {
        ConcurrentModel model = new ConcurrentModel();
        assertEquals("customer/policies/index", controller.index(session, model));
        assertNotNull(model.getAttribute("policies"));
    }

    @Test
    void rewardsPolicyHasAWorkingDetailView() {
        ConcurrentModel model = new ConcurrentModel();
        assertEquals("customer/policies/detail", controller.detail("rewards", session, model));
        assertNotNull(model.getAttribute("policy"));
    }
}
