package com.cinema.service;

import com.cinema.config.VnpayProperties;
import com.cinema.dao.CustomerBookingDAO;
import com.cinema.model.BookingPaidEvent;
import com.cinema.model.PaymentView;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentEmailEventTest {
    final CustomerBookingDAO dao = mock(CustomerBookingDAO.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final LocalDateTime future = LocalDateTime.now().plusMinutes(10);

    PaymentView payment(String status) {
        return new PaymentView(10, 42, "BK42", "REF42", "VNPAY", new BigDecimal("200000"), status, future, future);
    }

    @Test void simulatedPaymentPublishesOnlyAfterDaoConfirmation() {
        when(dao.findPayment(10, 7)).thenReturn(Optional.of(payment("PENDING")));
        var service = new CustomerBookingService(dao, events);
        assertEquals(42, service.confirmSimulatedPayment(10, 7));
        var order = inOrder(dao, events);
        order.verify(dao).confirmSimulatedPayment(10, 7);
        order.verify(events).publishEvent(new BookingPaidEvent(42));
    }

    @Test void failedSimulatedPaymentDoesNotPublish() {
        when(dao.findPayment(10, 7)).thenReturn(Optional.of(payment("PENDING")));
        doThrow(new IllegalStateException("expired")).when(dao).confirmSimulatedPayment(10, 7);
        assertThrows(CustomerBookingService.BookingException.class,
                () -> new CustomerBookingService(dao, events).confirmSimulatedPayment(10, 7));
        verifyNoInteractions(events);
    }

    VnpayService vnpay() {
        var service = spy(new VnpayService(new VnpayProperties(), dao, events));
        doReturn(true).when(service).hasValidSignature(anyMap());
        when(dao.findPaymentByTransactionRef("REF42")).thenReturn(Optional.of(payment("PENDING")));
        return service;
    }

    Map<String, String> callback(String response) {
        return Map.of("vnp_TxnRef", "REF42", "vnp_Amount", "20000000",
                "vnp_ResponseCode", response, "vnp_TransactionStatus", response);
    }

    @Test void successfulIpnPublishesAndRepeatedIpnDoesNot() {
        var service = vnpay();
        assertEquals("00", service.processIpn(callback("00")));
        verify(events).publishEvent(new BookingPaidEvent(42));
        when(dao.findPaymentByTransactionRef("REF42")).thenReturn(Optional.of(payment("PAID")));
        assertEquals("02", service.processIpn(callback("00")));
        verifyNoMoreInteractions(events);
    }

    @Test void unsuccessfulIpnDoesNotPublish() {
        assertEquals("00", vnpay().processIpn(callback("24")));
        verifyNoInteractions(events);
    }

    @Test void invalidSignatureOrAmountDoesNotPublish() {
        var service = vnpay();
        doReturn(false).when(service).hasValidSignature(anyMap());
        assertEquals("97", service.processIpn(callback("00")));
        doReturn(true).when(service).hasValidSignature(anyMap());
        var invalid = new java.util.HashMap<>(callback("00"));
        invalid.put("vnp_Amount", "100");
        assertEquals("04", service.processIpn(invalid));
        verifyNoInteractions(events);
    }
}
