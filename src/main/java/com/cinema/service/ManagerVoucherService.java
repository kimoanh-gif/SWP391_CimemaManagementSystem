package com.cinema.service;

import com.cinema.dao.ManagerPromotionDAO;
import com.cinema.dao.ManagerVoucherDAO;
import com.cinema.dao.MembershipRankDAO;
import com.cinema.dto.VoucherForm;
import com.cinema.exception.PromotionAdminException;
import com.cinema.model.AdminPage;
import com.cinema.model.ManagerPromotionFilter;
import com.cinema.model.ManagerPromotionRow;
import com.cinema.model.ManagerVoucherRow;
import com.cinema.model.MembershipRank;
import com.cinema.model.PromotionLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * UC33 Promotion &amp; Voucher Management – Vouchers (M11 Voucher tab, M11.1–M11.4).
 *
 *  Code: unique, 3–50 characters A–Z 0–9 - _ (stored upper-case)
 *  PERCENT 1–100 with optional cap; FIXED &gt; 0
 *  Valid period inside the promotion period, end not before start
 *  Per-customer limit must not exceed the total limit
 *  Audit rule: once used by a booking, code / discount type / value are locked and the voucher cannot be deleted
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Service
public class ManagerVoucherService {

    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_-]{3,50}$");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ManagerVoucherDAO voucherDAO;
    private final ManagerPromotionDAO promotionDAO;
    private final MembershipRankDAO membershipRankDAO;

    public ManagerVoucherService(ManagerVoucherDAO voucherDAO, ManagerPromotionDAO promotionDAO,
                                 MembershipRankDAO membershipRankDAO) {
        this.voucherDAO = voucherDAO;
        this.promotionDAO = promotionDAO;
        this.membershipRankDAO = membershipRankDAO;
    }

    public AdminPage<ManagerVoucherRow> search(ManagerPromotionFilter filter) {
        List<ManagerVoucherRow> rows = voucherDAO.findAll().stream()
                .filter(r -> filter.matchesKeyword(r.getCode(), r.getPromotionName()))
                .filter(r -> filter.matchesStatus(r.getDisplayStatus()))
                .filter(r -> filter.matchesType(r.getDiscountType()))
                .toList();
        return AdminPage.of(rows, filter.page(), filter.size());
    }

    public int countAll() {
        return voucherDAO.findAll().size();
    }

    public ManagerVoucherRow get(int voucherId) {
        return voucherDAO.findById(voucherId)
                .orElseThrow(() -> new PromotionAdminException("The voucher could not be found."));
    }

    public List<MembershipRank> getRanks() {
        return membershipRankDAO.findActive();
    }

    @Transactional
    public int create(VoucherForm form) {
        normalize(form);
        if (!Set.of(PromotionLifecycle.ACTIVE, PromotionLifecycle.DRAFT).contains(form.getStatus())) {
            form.setStatus(PromotionLifecycle.ACTIVE);
        }
        throwIfInvalid(validate(form, null));
        return voucherDAO.insert(form);
    }

    @Transactional
    public void update(int voucherId, VoucherForm form) {
        ManagerVoucherRow current = get(voucherId);
        normalize(form);
        if (!form.getStatus().equals(current.getStatus())) {
            ManagerPromotionService.checkTransition(current.getStatus(), form.getStatus(), form.getValidTo());
        }
        throwIfInvalid(validate(form, current));
        voucherDAO.update(voucherId, form);
    }

    @Transactional
    public ManagerVoucherRow changeStatus(int voucherId, String newStatus) {
        ManagerVoucherRow current = get(voucherId);
        String target = newStatus == null ? "" : newStatus.trim().toUpperCase();
        ManagerPromotionService.checkTransition(current.getStatus(), target, current.getValidTo());
        voucherDAO.updateStatus(voucherId, target);
        return current;
    }

    @Transactional
    public ManagerVoucherRow delete(int voucherId) {
        ManagerVoucherRow current = get(voucherId);
        if (current.isLocked()) {
            throw new PromotionAdminException("Voucher " + current.getCode() + " was used in " + current.getReferenceCount()
                    + " bookings and cannot be deleted. Stop it instead to keep the accounting history.");
        }
        if (voucherDAO.deleteIfUnused(voucherId) != 1) {
            throw new PromotionAdminException("This voucher was used by a booking just now and cannot be deleted.");
        }
        return current;
    }

    // ------------------------------------------------------------------ rules

    Map<String, String> validate(VoucherForm f, ManagerVoucherRow current) {
        Map<String, String> errors = new LinkedHashMap<>();

        Optional<ManagerPromotionRow> promotion = f.getPromotionId() != null
                ? promotionDAO.findById(f.getPromotionId()) : Optional.empty();
        if (promotion.isEmpty()) {
            errors.put("promotionId", "Please choose the promotion this voucher belongs to.");
        } else if (promotion.get().isStopped()) {
            errors.put("promotionId", "This promotion is stopped. Choose another promotion.");
        }

        if (f.getCode() == null || f.getCode().isEmpty()) {
            errors.put("code", "Please enter the voucher code.");
        } else if (!CODE.matcher(f.getCode()).matches()) {
            errors.put("code", "Use 3–50 characters: letters, digits, - or _ (no spaces).");
        } else if (voucherDAO.codeExists(f.getCode(), current != null ? current.getVoucherId() : null)) {
            errors.put("code", "Voucher code " + f.getCode() + " already exists.");
        }

        boolean percent = "PERCENT".equals(f.getDiscountType());
        if (!Set.of("PERCENT", "FIXED").contains(f.getDiscountType())) {
            errors.put("discountType", "Please choose the discount type.");
        }
        if (f.getDiscountValue() == null || f.getDiscountValue().signum() <= 0) {
            errors.put("discountValue", "The discount value must be greater than 0.");
        } else if (percent && f.getDiscountValue().compareTo(HUNDRED) > 0) {
            errors.put("discountValue", "A percentage discount must be between 1 and 100.");
        }
        if (f.getMaxDiscountAmount() != null && f.getMaxDiscountAmount().signum() < 0) {
            errors.put("maxDiscountAmount", "The maximum discount cannot be negative.");
        }
        if (f.getMinBookingAmount() != null && f.getMinBookingAmount().signum() < 0) {
            errors.put("minBookingAmount", "The minimum booking amount cannot be negative.");
        }

        if (f.getValidFrom() == null) errors.put("validFrom", "Please choose the start date.");
        if (f.getValidTo() == null) errors.put("validTo", "Please choose the end date.");
        if (f.getValidFrom() != null && f.getValidTo() != null && f.getValidTo().isBefore(f.getValidFrom())) {
            errors.put("validTo", "The end date must not be earlier than the start date.");
        }
        if (promotion.isPresent() && f.getValidFrom() != null && f.getValidTo() != null) {
            ManagerPromotionRow p = promotion.get();
            if ((p.getStartDate() != null && f.getValidFrom().isBefore(p.getStartDate()))
                    || (p.getEndDate() != null && f.getValidTo().isAfter(p.getEndDate()))) {
                errors.put("validTo", "The voucher period must be inside the promotion period (" + p.getPeriodDisplay() + ").");
            }
        }

        if (f.getTotalUsageLimit() != null && f.getTotalUsageLimit() < 1) {
            errors.put("totalUsageLimit", "Leave empty for unlimited, or enter at least 1.");
        }
        if (f.getPerAccountLimit() != null && f.getPerAccountLimit() < 1) {
            errors.put("perAccountLimit", "Leave empty for unlimited, or enter at least 1.");
        }
        if (f.getTotalUsageLimit() != null && f.getPerAccountLimit() != null && f.getPerAccountLimit() > f.getTotalUsageLimit()) {
            errors.put("perAccountLimit", "The per-customer limit cannot be greater than the total limit.");
        }
        if (current != null && f.getTotalUsageLimit() != null && f.getTotalUsageLimit() < current.getUsageCount()) {
            errors.put("totalUsageLimit", "The voucher is already used " + current.getUsageCount() + " times.");
        }

        if (f.getRequiredRankId() != null && getRanks().stream().noneMatch(r -> f.getRequiredRankId().equals(r.getRankId()))) {
            errors.put("requiredRankId", "The selected membership rank does not exist.");
        }

        if (current != null && current.isLocked()) {
            if (!current.getCode().equalsIgnoreCase(f.getCode() == null ? "" : f.getCode())) {
                errors.put("code", "The code is locked because the voucher already has bookings.");
            }
            String currentType = current.isPercentage() ? "PERCENT" : "FIXED";
            if (!currentType.equals(f.getDiscountType())
                    || current.getDiscountValue() == null || f.getDiscountValue() == null
                    || current.getDiscountValue().compareTo(f.getDiscountValue()) != 0) {
                errors.put("discountValue", "The discount is locked because the voucher already has bookings.");
            }
        }
        return errors;
    }

    private static void normalize(VoucherForm f) {
        if (f.getCode() != null) f.setCode(f.getCode().trim().toUpperCase(Locale.ROOT));
        f.setDiscountType(f.getDiscountType() == null ? "" : f.getDiscountType().trim().toUpperCase(Locale.ROOT));
        if (!"PERCENT".equals(f.getDiscountType())) f.setMaxDiscountAmount(null);
        f.setStatus(f.getStatus() == null ? PromotionLifecycle.ACTIVE : f.getStatus().trim().toUpperCase(Locale.ROOT));
    }

    private static void throwIfInvalid(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new PromotionAdminException("Please fix the highlighted fields.", errors);
        }
    }
}
