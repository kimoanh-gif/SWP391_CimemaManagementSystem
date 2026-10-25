package com.cinema.service;

import com.cinema.dao.ManagerPromotionDAO;
import com.cinema.dao.ManagerVoucherDAO;
import com.cinema.dto.PromotionForm;
import com.cinema.exception.PromotionAdminException;
import com.cinema.model.AdminPage;
import com.cinema.model.ManagerPromotionFilter;
import com.cinema.model.ManagerPromotionRow;
import com.cinema.model.ManagerPromotionStats;
import com.cinema.model.ManagerVoucherRow;
import com.cinema.model.PromotionLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UC33 Promotion &amp; Voucher Management – Promotion campaigns (M11, M11.1–M11.4).
 *
 *  BR-PN-03 end date must not be earlier than start date
 *  Audit rule (M11.4): a campaign whose vouchers were used in any Booking cannot be hard-deleted;
 *  it can only be paused or stopped.
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Service
public class ManagerPromotionService {

    public static final int MAX_NAME = 150;
    public static final int MAX_DESCRIPTION = 500;

    private final ManagerPromotionDAO promotionDAO;
    private final ManagerVoucherDAO voucherDAO;

    public ManagerPromotionService(ManagerPromotionDAO promotionDAO, ManagerVoucherDAO voucherDAO) {
        this.promotionDAO = promotionDAO;
        this.voucherDAO = voucherDAO;
    }

    public AdminPage<ManagerPromotionRow> search(ManagerPromotionFilter filter) {
        List<ManagerPromotionRow> rows = promotionDAO.findAll().stream()
                .filter(r -> filter.matchesKeyword(r.getName(), r.getPrimaryCode(), r.getCode(), r.getDescription()))
                .filter(r -> filter.matchesStatus(r.getDisplayStatus()))
                .filter(r -> "ALL".equals(filter.type()) || (r.getPrimaryCode() != null && filter.matchesType(r.getPrimaryDiscountType())))
                .toList();
        return AdminPage.of(rows, filter.page(), filter.size());
    }

    public ManagerPromotionStats getStats() {
        ManagerPromotionStats stats = promotionDAO.stats();
        return stats != null ? stats : ManagerPromotionStats.empty();
    }

    public List<ManagerPromotionRow> getAll() {
        return promotionDAO.findAll();
    }

    public ManagerPromotionRow get(int promotionId) {
        return promotionDAO.findById(promotionId)
                .orElseThrow(() -> new PromotionAdminException("The promotion could not be found."));
    }

    public List<ManagerVoucherRow> getVouchers(int promotionId) {
        return voucherDAO.findByPromotion(promotionId);
    }

    @Transactional
    public int create(PromotionForm form) {
        normalize(form);
        if (!Set.of(PromotionLifecycle.ACTIVE, PromotionLifecycle.DRAFT).contains(form.getStatus())) {
            form.setStatus(PromotionLifecycle.ACTIVE);
        }
        throwIfInvalid(validate(form));
        return promotionDAO.insert(form);
    }

    @Transactional
    public void update(int promotionId, PromotionForm form) {
        ManagerPromotionRow current = get(promotionId);
        normalize(form);
        if (!form.getStatus().equals(current.getStatus())) {
            checkTransition(current.getStatus(), form.getStatus(), form.getEndDate());
        }
        throwIfInvalid(validate(form));
        promotionDAO.update(promotionId, form);
    }

    /** Pause, resume, activate a draft, or stop permanently. */
    @Transactional
    public ManagerPromotionRow changeStatus(int promotionId, String newStatus) {
        ManagerPromotionRow current = get(promotionId);
        String target = newStatus == null ? "" : newStatus.trim().toUpperCase();
        checkTransition(current.getStatus(), target, current.getEndDate());
        promotionDAO.updateStatus(promotionId, target);
        return current;
    }

    /** M11.4: hard delete only when no booking used any voucher of this campaign. */
    @Transactional
    public ManagerPromotionRow delete(int promotionId) {
        ManagerPromotionRow current = get(promotionId);
        if (current.isLocked()) {
            throw new PromotionAdminException("This promotion already has " + current.getReferenceCount()
                    + " bookings and cannot be deleted. Stop it instead to keep the accounting history.");
        }
        if (promotionDAO.deleteIfUnused(promotionId) != 1) {
            throw new PromotionAdminException("This promotion was used by a booking just now and cannot be deleted.");
        }
        return current;
    }

    // ------------------------------------------------------------------ rules

    Map<String, String> validate(PromotionForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getName() == null || form.getName().isBlank()) {
            errors.put("name", "Please enter the promotion name.");
        } else if (form.getName().length() > MAX_NAME) {
            errors.put("name", "The name must be at most " + MAX_NAME + " characters.");
        }
        if (form.getDescription() != null && form.getDescription().length() > MAX_DESCRIPTION) {
            errors.put("description", "The description must be at most " + MAX_DESCRIPTION + " characters.");
        }
        if (form.getStartDate() == null) errors.put("startDate", "Please choose the start date.");
        if (form.getEndDate() == null) errors.put("endDate", "Please choose the end date.");
        if (form.getStartDate() != null && form.getEndDate() != null && form.getEndDate().isBefore(form.getStartDate())) {
            errors.put("endDate", "The end date must not be earlier than the start date.");
        }
        return errors;
    }

    static void checkTransition(String from, String to, LocalDateTime end) {
        String f = from == null ? "" : from.toUpperCase();
        if (!Set.of(PromotionLifecycle.ACTIVE, PromotionLifecycle.PAUSED, PromotionLifecycle.INACTIVE, PromotionLifecycle.DRAFT).contains(to)) {
            throw new PromotionAdminException("The selected status is not valid.");
        }
        if (f.equals(to)) return;
        if (PromotionLifecycle.INACTIVE.equals(f)) {
            throw new PromotionAdminException("A stopped promotion cannot be reactivated. Create a new one instead.");
        }
        if (PromotionLifecycle.DRAFT.equals(to)) {
            throw new PromotionAdminException("A published promotion cannot go back to draft.");
        }
        if (PromotionLifecycle.PAUSED.equals(to) && PromotionLifecycle.DRAFT.equals(f)) {
            throw new PromotionAdminException("A draft cannot be paused. Activate it or keep it as a draft.");
        }
        if (PromotionLifecycle.ACTIVE.equals(to) && end != null && end.isBefore(LocalDateTime.now())) {
            throw new PromotionAdminException("The end date has passed. Extend the end date before activating.");
        }
    }

    private static void normalize(PromotionForm form) {
        if (form.getName() != null) form.setName(form.getName().trim());
        if (form.getDescription() != null) {
            String d = form.getDescription().trim();
            form.setDescription(d.isEmpty() ? null : d);
        }
        form.setStatus(form.getStatus() == null ? PromotionLifecycle.ACTIVE : form.getStatus().trim().toUpperCase());
    }

    private static void throwIfInvalid(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new PromotionAdminException("Please fix the highlighted fields.", errors);
        }
    }
}
