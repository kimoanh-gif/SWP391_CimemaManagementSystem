package com.cinema.controller;

import com.cinema.model.LoyaltyInfo;
import com.cinema.model.MembershipRank;
import com.cinema.service.LoyaltyService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoyaltyModelAdviceTest {

    @SuppressWarnings("unchecked")
    private LoyaltyModelAdvice advice(LoyaltyService loyaltyService, SessionService sessionService) {
        ObjectProvider<LoyaltyService> lp = mock(ObjectProvider.class);
        ObjectProvider<SessionService> sp = mock(ObjectProvider.class);
        when(lp.getIfAvailable()).thenReturn(loyaltyService);
        when(sp.getIfAvailable()).thenReturn(sessionService);
        return new LoyaltyModelAdvice(lp, sp);
    }

    private static LoyaltyInfo info(MembershipRank rank) {
        return new LoyaltyInfo("Anh", 0, 800, rank, null, 0, 100, 0, null, List.of(), List.of());
    }

    @Test
    void voucherStepShowsMemberDiscountHintWhenRankPolicyIsActive() {
        LoyaltyService loyaltyService = mock(LoyaltyService.class);
        SessionService sessionService = mock(SessionService.class);
        HttpSession session = new MockHttpSession();
        when(sessionService.hasRole(session, "CUSTOMER")).thenReturn(true);
        when(sessionService.currentAccountId(session)).thenReturn(7);
        MembershipRank gold = new MembershipRank(2, "GOLD", "Gold Member", 500, null, null, "5", true);
        gold.setDiscountUsageLimit(3);
        gold.setMinBookingAmount(new BigDecimal("100000"));
        gold.setMaxDiscountAmount(new BigDecimal("50000"));
        when(loyaltyService.getLoyaltyInfo(7)).thenReturn(info(gold));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customer/booking/promotions");
        Model model = new ConcurrentModel();
        advice(loyaltyService, sessionService).addLoyalty(request, session, model);

        assertEquals("Your Gold Member benefit: 5% member discount on bookings from 100,000 ₫, up to 50,000 ₫ per booking, "
                        + "for 3 bookings at this rank (not combined with vouchers). It is applied at checkout when you continue "
                        + "without a voucher; applying a voucher replaces it.",
                model.getAttribute("memberDiscountHint"));
    }

    @Test
    void adviceAppliesToTheLoyaltyAndTheBookingControllers() {
        org.springframework.web.bind.annotation.ControllerAdvice annotation =
                LoyaltyModelAdvice.class.getAnnotation(org.springframework.web.bind.annotation.ControllerAdvice.class);
        assertEquals(List.of(CustomerProfileController.class, CustomerBookingController.class),
                List.of(annotation.assignableTypes()));
    }

    @Test
    void voucherStepShowsNoHintWhenRankHasNoActiveDiscount() {
        LoyaltyService loyaltyService = mock(LoyaltyService.class);
        SessionService sessionService = mock(SessionService.class);
        HttpSession session = new MockHttpSession();
        when(sessionService.hasRole(session, "CUSTOMER")).thenReturn(true);
        when(sessionService.currentAccountId(session)).thenReturn(7);
        MembershipRank standard = new MembershipRank(1, "STANDARD", "Standard", 0, null, null, "5", true); // no limit/cap
        when(loyaltyService.getLoyaltyInfo(7)).thenReturn(info(standard));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/customer/booking/promotions");
        Model model = new ConcurrentModel();
        advice(loyaltyService, sessionService).addLoyalty(request, session, model);

        assertFalse(model.containsAttribute("memberDiscountHint"));
    }
}
