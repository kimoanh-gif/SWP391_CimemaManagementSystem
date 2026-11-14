package com.cinema.service;

import com.cinema.dao.*;
import com.cinema.dto.VoucherForm;
import com.cinema.model.SystemSetting;
import com.cinema.model.ManagerVoucherRow;
import com.cinema.exception.PromotionAdminException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoucherPolicyServiceTest {
    SystemSettingDAO settings = mock(SystemSettingDAO.class);
    VoucherPolicyService policy = new VoucherPolicyService(settings);
    ManagerVoucherDAO vouchers = mock(ManagerVoucherDAO.class);
    ManagerPromotionDAO promotions = mock(ManagerPromotionDAO.class);
    ManagerVoucherService manager = new ManagerVoucherService(vouchers, promotions, mock(MembershipRankDAO.class), policy);

    List<SystemSetting> rules(String percent, String cap) {
        List<SystemSetting> rows = new ArrayList<>();
        for (var rule : VoucherPolicyService.RULES) {
            var row = new SystemSetting(); row.setSettingKey(rule.key()); row.setCategory("VOUCHER");
            row.setIsActive(true); row.setValue(Long.toString(rule.fallback()));
            if (rule.key().equals("VOUCHER_MAX_DISCOUNT_PERCENT")) row.setValue(percent);
            if (rule.key().equals("VOUCHER_MAX_DISCOUNT_AMOUNT")) row.setValue(cap);
            row.setUpdatedAt(LocalDateTime.of(2026, 11, 14, 12, 0));
            rows.add(row);
        }
        return rows;
    }
    VoucherForm form() {
        VoucherForm f = new VoucherForm(); f.setCode("TEST20"); f.setPromotionId(1);
        f.setDiscountValue(new BigDecimal("20"));
        f.setValidFrom(LocalDateTime.of(2026, 11, 14, 12, 0)); f.setValidTo(f.getValidFrom().plusDays(30));
        manager.applyDefaults(f);
        return f;
    }
    @Test void adminChangesAreReadAgainAndExistingVoucherGetsWarningWithoutBeingModified() {
        when(settings.findByCategory("VOUCHER")).thenReturn(rules("30", "50000"), rules("10", "20000"));
        var first = policy.current();
        var row = new ManagerVoucherRow(); row.setDiscountType("PERCENT"); row.setDiscountValue(new BigDecimal("20"));
        row.setMaxDiscountAmount(new BigDecimal("50000")); row.setMinBookingAmount(new BigDecimal("50000"));
        row.setTotalUsageLimit(1000); row.setPerAccountLimit(5);
        assertEquals(new BigDecimal("30"), first.maxPercent());
        assertTrue(manager.policyWarnings(row).containsKey("discountValue"));
        assertEquals(new BigDecimal("50000"), row.getMaxDiscountAmount());
        verifyNoInteractions(vouchers);
    }
    @Test void allLimitsAreCheckedAndBoundaryValuesAreAccepted() {
        when(settings.findByCategory("VOUCHER")).thenReturn(rules("20", "50000"));
        var f = form();
        assertTrue(policy.current().violations(f).isEmpty());
        f.setDiscountValue(new BigDecimal("21")); f.setMaxDiscountAmount(new BigDecimal("50001"));
        f.setMinBookingAmount(new BigDecimal("49999")); f.setValidTo(f.getValidTo().plusSeconds(1));
        f.setTotalUsageLimit(1001); f.setPerAccountLimit(6);
        assertEquals(Set.of("discountValue", "maxDiscountAmount", "minBookingAmount", "validTo", "totalUsageLimit", "perAccountLimit"),
                policy.current().violations(f).keySet());
    }
    @Test void craftedCreateIsRejectedBeforeInsert() {
        when(settings.findByCategory("VOUCHER")).thenReturn(rules("20", "50000"));
        when(promotions.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        var f = form(); f.setDiscountValue(new BigDecimal("40"));
        var ex = assertThrows(PromotionAdminException.class, () -> manager.create(f));
        assertTrue(ex.getFieldErrors().containsKey("discountValue"));
        verify(vouchers, never()).insert(any());
    }
    @Test void validCreationPersistsOwnCapAndLaterEditMustUseNewPolicy() {
        when(settings.findByCategory("VOUCHER")).thenReturn(rules("30", "50000"));
        when(promotions.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        var f = form();
        // Keep dates inside the promotion fixture.
        f.setValidFrom(LocalDateTime.now()); f.setValidTo(f.getValidFrom().plusDays(7));
        when(vouchers.insert(any())).thenReturn(7);
        assertEquals(7, manager.create(f));
        assertEquals(new BigDecimal("50000"), f.getMaxDiscountAmount());
        var row = new ManagerVoucherRow(); row.setVoucherId(7); row.setStatus("ACTIVE"); row.setCode("TEST20");
        when(vouchers.findById(7)).thenReturn(Optional.of(row));
        when(settings.findByCategory("VOUCHER")).thenReturn(rules("10", "20000"));
        var ex = assertThrows(PromotionAdminException.class, () -> manager.update(7, f));
        assertTrue(ex.getFieldErrors().containsKey("discountValue"));
        assertTrue(ex.getFieldErrors().containsKey("maxDiscountAmount"));
        verify(vouchers, never()).update(anyInt(), any());
    }
    @Test void firstDraftActivationMustMeetCurrentPolicyButIssuedVoucherCanBePausedAndResumed() {
        when(settings.findByCategory("VOUCHER")).thenReturn(rules("10", "50000"));
        var row = new ManagerVoucherRow(); row.setVoucherId(7); row.setCode("TEST20"); row.setPromotionId(1);
        row.setStatus("DRAFT"); row.setDiscountType("PERCENT"); row.setDiscountValue(new BigDecimal("20"));
        row.setValidTo(LocalDateTime.now().plusDays(1));
        when(vouchers.findById(7)).thenReturn(Optional.of(row));
        when(promotions.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        assertThrows(PromotionAdminException.class, () -> manager.changeStatus(7, "ACTIVE"));
        verify(vouchers, never()).updateStatus(anyInt(), anyString());
        row.setStatus("PAUSED");
        manager.changeStatus(7, "ACTIVE");
        verify(vouchers).updateStatus(7, "ACTIVE");
    }
    @Test void invalidAdminNumbersCannotBeSaved() {
        var admin = new SystemConfigService(settings, mock(MembershipRankDAO.class));
        for (var rule : VoucherPolicyService.RULES) {
            var row = new SystemSetting(); row.setCategory("VOUCHER"); row.setSettingKey(rule.key());
            for (String bad : List.of("NaN", "Infinity", "1.5", "-1", "", "9999999999999"))
                assertFalse(admin.validateSetting(row, bad).valid(), rule.key() + "=" + bad);
            assertTrue(admin.validateSetting(row, Long.toString(rule.min())).valid());
            assertTrue(admin.validateSetting(row, Long.toString(rule.max())).valid());
        }
    }
    @Test void policyFragmentRendersLimitsAndWarnings() {
        when(settings.findByCategory("VOUCHER")).thenReturn(rules("30", "50000"));
        var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        var context = new org.thymeleaf.context.Context(); context.setVariable("voucherPolicy", policy.current());
        context.setVariable("policyWarnings", Map.of("discountValue", "Above current limit"));
        String html = engine.process("manager/promotions/voucher-policy", context);
        assertTrue(html.contains("30%")); assertTrue(html.contains("50,000"));
        assertTrue(html.contains("Above current limit")); assertTrue(html.contains("data-max-percent=\"30\""));
    }
}
