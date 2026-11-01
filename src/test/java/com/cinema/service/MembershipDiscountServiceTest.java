package com.cinema.service;
import com.cinema.dao.MembershipDiscountDAO;
import com.cinema.model.MembershipDiscountPolicy;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipDiscountServiceTest {
    BigDecimal money(long n) { return BigDecimal.valueOf(n); }
    @Test void checksOrderThresholdCapRoundingAndUsageBoundaries() {
        var policy = new MembershipDiscountPolicy(3, money(100000), money(50000), money(10));
        assertEquals(BigDecimal.ZERO, policy.discount(money(99999), 0));
        assertEquals(money(10000), policy.discount(money(100000), 0));
        assertEquals(money(10000), policy.discount(money(100009), 0));
        assertEquals(money(50000), policy.discount(money(900000), 2));
        assertEquals(BigDecimal.ZERO, policy.discount(money(900000), 3));
        assertEquals(BigDecimal.ZERO, new MembershipDiscountPolicy(0, money(0), money(50000), money(10)).discount(money(100000), 0));
        assertEquals(money(100), new MembershipDiscountPolicy(3, money(0), money(50000), money(100)).discount(money(100), 0));
    }
    @Test void excludesCurrentBookingAndReservesAfterCustomerLock() {
        var dao = mock(MembershipDiscountDAO.class);
        var service = new MembershipDiscountService(dao);
        var order = new MembershipDiscountDAO.Order(7, 2, "PENDING", null, money(150000), new MembershipDiscountPolicy(3, money(100000), money(50000), money(10)));
        when(dao.lockOrder(11, 8, false)).thenReturn(Optional.of(order));
        when(dao.countUsedOrReserved(7, 2, 11)).thenReturn(2);
        service.refreshOnline(11, 8);
        var sequence = inOrder(dao);
        sequence.verify(dao).lockOrder(11, 8, false);
        sequence.verify(dao).countUsedOrReserved(7, 2, 11);
        sequence.verify(dao).saveReservation(11, order, money(15000));
        when(dao.countUsedOrReserved(7, 2, 11)).thenReturn(3);
        service.refreshOnline(11, 8);
        verify(dao).saveReservation(11, order, BigDecimal.ZERO);
    }
    @Test void voucherDoesNotStackAndPaymentSnapshotsAreUntouched() {
        var dao = mock(MembershipDiscountDAO.class);
        var service = new MembershipDiscountService(dao);
        var policy = new MembershipDiscountPolicy(3, money(0), money(50000), money(10));
        var order = new MembershipDiscountDAO.Order(7, 2, "PENDING", 9, money(150000), policy);
        when(dao.lockOrder(11, 8, true)).thenReturn(Optional.of(order));
        service.refreshCounter(11, 8);
        verify(dao).saveReservation(11, order, BigDecimal.ZERO);
        verify(dao, never()).countUsedOrReserved(anyInt(), anyInt(), anyInt());
        reset(dao);
        for (String status : new String[]{"PAYMENT_PENDING", "CONFIRMED", "CANCELLED", "EXPIRED"}) {
            when(dao.lockOrder(11, 8, false)).thenReturn(Optional.of(new MembershipDiscountDAO.Order(7, 2, status, null, money(150000), policy)));
            service.refreshOnline(11, 8);
        }
        when(dao.lockOrder(12, 8, false)).thenReturn(Optional.empty());
        service.refreshOnline(12, 8);
        verify(dao, never()).saveReservation(anyInt(), any(), any());
    }
}
