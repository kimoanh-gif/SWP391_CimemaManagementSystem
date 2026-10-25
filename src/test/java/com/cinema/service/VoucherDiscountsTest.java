package com.cinema.service;

import com.cinema.model.Voucher;
import com.cinema.model.VoucherDiscounts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoucherDiscountsTest {

    @Test
    void percentTypesAreRecognised() {
        assertTrue(VoucherDiscounts.isPercentage("PERCENT"));
        assertTrue(VoucherDiscounts.isPercentage("percentage"));
        assertTrue(VoucherDiscounts.isPercentage("%"));
        assertFalse(VoucherDiscounts.isPercentage("FIXED"));
        assertFalse(VoucherDiscounts.isPercentage(null));
    }

    @Test
    void percentageIsRoundedToWholeVnd() {
        // 15% of 123,333 = 18,499.95 -> 18,500
        assertEquals(new BigDecimal("18500"),
                VoucherDiscounts.calculate("PERCENT", BigDecimal.valueOf(15), null, BigDecimal.valueOf(123_333)));
    }

    @Test
    void zeroSubtotalGivesNoDiscount() {
        assertEquals(BigDecimal.ZERO,
                VoucherDiscounts.calculate("FIXED", BigDecimal.valueOf(10_000), null, BigDecimal.ZERO));
    }

    @Test
    void seedVoucherWithPercentTypeIsDisplayedAsPercent() {
        Voucher v = new Voucher();
        v.setDiscountType("PERCENT");
        v.setDiscountValue(BigDecimal.TEN);

        assertEquals("Giảm 10%", v.getDiscountDisplay());
    }
}
