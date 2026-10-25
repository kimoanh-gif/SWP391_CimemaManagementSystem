package com.cinema.service;

import com.cinema.dao.ManagerPromotionDAO;
import com.cinema.dao.ManagerVoucherDAO;
import com.cinema.dao.MembershipRankDAO;
import com.cinema.dto.VoucherForm;
import com.cinema.exception.PromotionAdminException;
import com.cinema.model.ManagerVoucherRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UC33 M11 – vouchers. Assignee: Nguyễn Đức Anh */
@ExtendWith(MockitoExtension.class)
class ManagerVoucherServiceTest {

    @Mock
    private ManagerVoucherDAO voucherDAO;
    @Mock
    private ManagerPromotionDAO promotionDAO;
    @Mock
    private MembershipRankDAO rankDAO;

    private ManagerVoucherService service;

    @BeforeEach
    void setUp() {
        service = new ManagerVoucherService(voucherDAO, promotionDAO, rankDAO);
    }

    @Test
    void validVoucherIsCreatedWithUpperCaseCode() {
        when(promotionDAO.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        when(voucherDAO.codeExists("SUMMER-15", null)).thenReturn(false);
        when(voucherDAO.insert(any(VoucherForm.class))).thenReturn(9);
        VoucherForm f = form(" summer-15 ", "PERCENT", "15");
        f.setMaxDiscountAmount(new BigDecimal("50000"));

        assertEquals(9, service.create(f));
        assertEquals("SUMMER-15", f.getCode());
    }

    @Test
    void fixedVoucherDropsMaxDiscount() {
        when(promotionDAO.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        when(voucherDAO.codeExists("FIX20K", null)).thenReturn(false);
        when(voucherDAO.insert(any(VoucherForm.class))).thenReturn(2);
        VoucherForm f = form("FIX20K", "FIXED", "20000");
        f.setMaxDiscountAmount(new BigDecimal("1"));

        service.create(f);
        assertNull(f.getMaxDiscountAmount());
    }

    @Test
    void duplicateCodeAndBadPercentAreRejected() {
        when(promotionDAO.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        when(voucherDAO.codeExists("WELCOME10", null)).thenReturn(true);

        PromotionAdminException ex = assertThrows(PromotionAdminException.class,
                () -> service.create(form("WELCOME10", "PERCENT", "150")));
        assertTrue(ex.getFieldErrors().containsKey("code"));
        assertTrue(ex.getFieldErrors().containsKey("discountValue"));
        verify(voucherDAO, never()).insert(any());
    }

    @Test
    void invalidCodeFormatIsRejectedWithoutDbLookup() {
        when(promotionDAO.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));

        PromotionAdminException ex = assertThrows(PromotionAdminException.class,
                () -> service.create(form("A B", "FIXED", "1000")));
        assertTrue(ex.getFieldErrors().containsKey("code"));
        verify(voucherDAO, never()).codeExists(any(), any());
    }

    @Test
    void periodOutsidePromotionAndBadLimitsAreRejected() {
        when(promotionDAO.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        when(voucherDAO.codeExists("LONG", null)).thenReturn(false);
        VoucherForm f = form("LONG", "FIXED", "1000");
        f.setValidTo(LocalDateTime.now().plusDays(400));
        f.setTotalUsageLimit(5);
        f.setPerAccountLimit(6);

        Map<String, String> errors = assertThrows(PromotionAdminException.class, () -> service.create(f)).getFieldErrors();
        assertTrue(errors.containsKey("validTo"));
        assertTrue(errors.containsKey("perAccountLimit"));
    }

    @Test
    void usedVoucherKeepsCodeAndDiscount() {
        ManagerVoucherRow current = row(4, "WED45", 3);
        when(voucherDAO.findById(4)).thenReturn(Optional.of(current));
        when(promotionDAO.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        when(voucherDAO.codeExists(eq("WED46"), eq(4))).thenReturn(false);
        VoucherForm f = form("WED46", "PERCENT", "20");

        Map<String, String> errors = assertThrows(PromotionAdminException.class, () -> service.update(4, f)).getFieldErrors();
        assertTrue(errors.containsKey("code"));
        assertTrue(errors.containsKey("discountValue"));
    }

    @Test
    void usedVoucherCanStillChangeLimits() {
        ManagerVoucherRow current = row(4, "WED45", 3);
        when(voucherDAO.findById(4)).thenReturn(Optional.of(current));
        when(promotionDAO.findById(1)).thenReturn(Optional.of(ManagerPromotionServiceTest.row(1, "ACTIVE", 0)));
        when(voucherDAO.codeExists(eq("WED45"), eq(4))).thenReturn(false);
        VoucherForm f = form("WED45", "PERCENT", "10.00");
        f.setTotalUsageLimit(500);

        service.update(4, f);
        verify(voucherDAO).update(4, f);
    }

    @Test
    void usedVoucherCannotBeDeleted() {
        when(voucherDAO.findById(4)).thenReturn(Optional.of(row(4, "WED45", 3)));

        assertThrows(PromotionAdminException.class, () -> service.delete(4));
        verify(voucherDAO, never()).deleteIfUnused(anyInt());
    }

    @Test
    void unusedVoucherIsDeleted() {
        when(voucherDAO.findById(6)).thenReturn(Optional.of(row(6, "NEW", 0)));
        when(voucherDAO.deleteIfUnused(6)).thenReturn(1);

        service.delete(6);
        verify(voucherDAO).deleteIfUnused(6);
        assertFalse(row(6, "NEW", 0).isLocked());
    }

    private static VoucherForm form(String code, String type, String value) {
        VoucherForm f = new VoucherForm();
        f.setPromotionId(1);
        f.setCode(code);
        f.setDiscountType(type);
        f.setDiscountValue(new BigDecimal(value));
        f.setValidFrom(LocalDateTime.now());
        f.setValidTo(LocalDateTime.now().plusDays(7));
        return f;
    }

    private static ManagerVoucherRow row(int id, String code, int references) {
        ManagerVoucherRow r = new ManagerVoucherRow();
        r.setVoucherId(id);
        r.setPromotionId(1);
        r.setCode(code);
        r.setDiscountType("PERCENT");
        r.setDiscountValue(new BigDecimal("10"));
        r.setStatus("ACTIVE");
        r.setValidFrom(LocalDateTime.now().minusDays(1));
        r.setValidTo(LocalDateTime.now().plusDays(7));
        r.setReferenceCount(references);
        r.setUsageCount(references);
        return r;
    }
}
