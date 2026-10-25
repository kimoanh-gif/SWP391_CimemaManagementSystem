package com.cinema.service;

import com.cinema.dao.VoucherDAO;
import com.cinema.model.BookingPricing;
import com.cinema.model.VoucherCandidate;
import com.cinema.model.VoucherOption;
import com.cinema.service.VoucherSelectionService.VoucherException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherSelectionServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 24, 20, 0);
    private static final int ACCOUNT = 7;
    private static final int BOOKING = 100;
    private static final int CUSTOMER = 3;

    @Mock
    private VoucherDAO voucherDAO;

    private VoucherSelectionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new VoucherSelectionService(voucherDAO, clock);
        lenient().when(voucherDAO.countUsage(anyInt(), anyInt())).thenReturn(0);
        lenient().when(voucherDAO.countCustomerUsage(anyInt(), anyInt(), anyInt())).thenReturn(0);
    }

    @Test
    void percentageVoucherIsCappedByMaxDiscount() {
        // 10% of 600,000 = 60,000 but the cap is 50,000
        VoucherOption option = service.evaluate(voucher("WELCOME10", "PERCENT", 10, 50_000, 100_000), pricing(600_000, 0));

        assertEquals(VoucherOption.AVAILABLE, option.getStatus());
        assertEquals(new BigDecimal("50000"), option.getDiscountAmount());
    }

    @Test
    void fixedVoucherNeverExceedsSubtotal() {
        VoucherOption option = service.evaluate(voucher("BIG", "FIXED", 500_000, null, null), pricing(200_000, 0));

        assertEquals(new BigDecimal("200000"), option.getDiscountAmount());
    }

    @Test
    void expiredVoucherIsRejected() {
        VoucherCandidate expired = new VoucherCandidate(1, "OLD", "PERCENT", BigDecimal.TEN, null, null,
                NOW.minusDays(10), NOW.minusDays(1), null, null, "ACTIVE",
                "Promo", null, "ACTIVE", NOW.minusDays(10), NOW.plusDays(10), null, null, null);

        VoucherOption option = service.evaluate(expired, pricing(300_000, 0));

        assertEquals(VoucherOption.EXPIRED, option.getStatus());
    }

    @Test
    void minimumBookingAmountIsRequired() {
        VoucherOption option = service.evaluate(voucher("WELCOME10", "PERCENT", 10, 50_000, 100_000), pricing(80_000, 0));

        assertEquals(VoucherOption.INVALID, option.getStatus());
        assertTrue(option.getReason().contains("minimum booking amount"));
    }

    @Test
    void membershipRankIsRequired() {
        VoucherCandidate gold = new VoucherCandidate(2, "GOLD5", "PERCENT", BigDecimal.valueOf(5), null, null,
                NOW.minusDays(1), NOW.plusDays(1), null, null, "ACTIVE",
                "Gold promo", null, "ACTIVE", NOW.minusDays(1), NOW.plusDays(1), 2, "Gold Member", 500);

        assertEquals(VoucherOption.INVALID, service.evaluate(gold, pricing(300_000, 0, 499)).getStatus());
        assertEquals(VoucherOption.AVAILABLE, service.evaluate(gold, pricing(300_000, 0, 500)).getStatus());
    }

    @Test
    void perAccountLimitIsEnforced() {
        VoucherCandidate once = new VoucherCandidate(3, "ONCE", "FIXED", BigDecimal.valueOf(20_000), null, null,
                NOW.minusDays(1), NOW.plusDays(1), null, 1, "ACTIVE",
                "Promo", null, "ACTIVE", NOW.minusDays(1), NOW.plusDays(1), null, null, null);
        when(voucherDAO.countCustomerUsage(3, CUSTOMER, BOOKING)).thenReturn(1);

        assertEquals(VoucherOption.INVALID, service.evaluate(once, pricing(300_000, 0)).getStatus());
    }

    @Test
    void totalUsageLimitIsEnforced() {
        VoucherCandidate limited = new VoucherCandidate(4, "LIMIT", "FIXED", BigDecimal.valueOf(20_000), null, null,
                NOW.minusDays(1), NOW.plusDays(1), 10, null, "ACTIVE",
                "Promo", null, "ACTIVE", NOW.minusDays(1), NOW.plusDays(1), null, null, null);
        when(voucherDAO.countUsage(4, BOOKING)).thenReturn(10);

        assertEquals(VoucherOption.INVALID, service.evaluate(limited, pricing(300_000, 0)).getStatus());
    }

    @Test
    void applyValidCodeStoresDiscountOnBooking() {
        when(voucherDAO.findBookingPricing(BOOKING, ACCOUNT)).thenReturn(Optional.of(pricing(300_000, 0)));
        when(voucherDAO.findByCode("welcome10"))
                .thenReturn(Optional.of(voucher("WELCOME10", "PERCENT", 10, 50_000, 100_000)));

        VoucherOption applied = service.applyVoucher(ACCOUNT, BOOKING, " welcome10 ");

        assertEquals(VoucherOption.APPLIED, applied.getStatus());
        verify(voucherDAO).updateBookingVoucher(BOOKING, 1, new BigDecimal("30000"));
    }

    @Test
    void applyUnknownCodeThrows() {
        when(voucherDAO.findBookingPricing(BOOKING, ACCOUNT)).thenReturn(Optional.of(pricing(300_000, 0)));
        when(voucherDAO.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThrows(VoucherException.class, () -> service.applyVoucher(ACCOUNT, BOOKING, "NOPE"));
        verify(voucherDAO, never()).updateBookingVoucher(anyInt(), any(), any());
    }

    @Test
    void applyIsBlockedAfterPaymentStarted() {
        BookingPricing paying = new BookingPricing(BOOKING, CUSTOMER, 0, "PAYMENT_PENDING",
                BigDecimal.valueOf(300_000), BigDecimal.ZERO, BigDecimal.valueOf(300_000), null, null);
        when(voucherDAO.findBookingPricing(BOOKING, ACCOUNT)).thenReturn(Optional.of(paying));

        assertThrows(VoucherException.class, () -> service.applyVoucher(ACCOUNT, BOOKING, "WELCOME10"));
    }

    @Test
    void emptyCodeThrows() {
        assertThrows(VoucherException.class, () -> service.applyVoucher(ACCOUNT, BOOKING, "  "));
    }

    @Test
    void continueRemovesVoucherThatNoLongerQualifies() {
        // Subtotal dropped below the minimum after the customer changed F&B.
        BookingPricing withVoucher = new BookingPricing(BOOKING, CUSTOMER, 0, "PENDING",
                BigDecimal.valueOf(80_000), BigDecimal.valueOf(8_000), BigDecimal.valueOf(72_000), 1, "WELCOME10");
        BookingPricing withoutVoucher = new BookingPricing(BOOKING, CUSTOMER, 0, "PENDING",
                BigDecimal.valueOf(80_000), BigDecimal.ZERO, BigDecimal.valueOf(80_000), null, null);
        when(voucherDAO.findBookingPricing(BOOKING, ACCOUNT))
                .thenReturn(Optional.of(withVoucher), Optional.of(withVoucher), Optional.of(withoutVoucher));
        when(voucherDAO.findById(1)).thenReturn(Optional.of(voucher("WELCOME10", "PERCENT", 10, 50_000, 100_000)));

        assertThrows(VoucherException.class, () -> service.confirmForCheckout(ACCOUNT, BOOKING));
        verify(voucherDAO).updateBookingVoucher(eq(BOOKING), isNull(), eq(BigDecimal.ZERO));
    }

    @Test
    void continueWithoutVoucherReturnsEmpty() {
        when(voucherDAO.findBookingPricing(BOOKING, ACCOUNT)).thenReturn(Optional.of(pricing(300_000, 0)));

        assertTrue(service.confirmForCheckout(ACCOUNT, BOOKING).isEmpty());
    }

    // ------------------------------------------------------------------ helpers

    private static VoucherCandidate voucher(String code, String type, long value, Integer max, Integer min) {
        return new VoucherCandidate(1, code, type, BigDecimal.valueOf(value),
                max != null ? BigDecimal.valueOf(max) : null,
                min != null ? BigDecimal.valueOf(min) : null,
                NOW.minusDays(30), NOW.plusDays(30), 1000, 1, "ACTIVE",
                "New member offer", "First booking discount", "ACTIVE",
                NOW.minusDays(30), NOW.plusDays(30), null, null, null);
    }

    private static BookingPricing pricing(long subtotal, long discount) {
        return pricing(subtotal, discount, 0);
    }

    private static BookingPricing pricing(long subtotal, long discount, int tierPoints) {
        return new BookingPricing(BOOKING, CUSTOMER, tierPoints, "PENDING",
                BigDecimal.valueOf(subtotal), BigDecimal.valueOf(discount),
                BigDecimal.valueOf(subtotal - discount), null, null);
    }
}
