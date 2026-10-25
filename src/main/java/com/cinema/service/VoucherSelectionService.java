package com.cinema.service;

import com.cinema.dao.VoucherDAO;
import com.cinema.model.BookingPricing;
import com.cinema.model.VoucherCandidate;
import com.cinema.model.VoucherDiscounts;
import com.cinema.model.VoucherOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Screen: Voucher / Promotion Selection (UC11 Book Tickets Online – voucher step).
 *
 * Rules:
 *  BR-17 validity period  · BR-18 active status · BR-19 total usage limit
 *  BR-20 minimum booking amount + membership rank · BR-21 one voucher per booking
 *  BR-38 per-account usage limit · BR-16 total = subtotal − discount
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Service
public class VoucherSelectionService {

    private final VoucherDAO voucherDAO;
    private final Clock clock;

    @Autowired
    public VoucherSelectionService(VoucherDAO voucherDAO) {
        this(voucherDAO, Clock.systemDefaultZone());
    }

    /** Test constructor with a fixed clock. */
    VoucherSelectionService(VoucherDAO voucherDAO, Clock clock) {
        this.voucherDAO = voucherDAO;
        this.clock = clock;
    }

    /** Data shown on the voucher screen. */
    public record SelectionView(BookingPricing pricing, List<VoucherOption> vouchers, VoucherOption appliedVoucher) {
    }

    public static class VoucherException extends RuntimeException {
        public VoucherException(String message) {
            super(message);
        }
    }

    @Transactional
    public SelectionView getSelectionView(int accountId, int bookingId) {
        BookingPricing pricing = refreshAppliedVoucher(accountId, bookingId);

        List<VoucherOption> options = voucherDAO.findCurrentlyValid().stream()
                .map(v -> evaluate(v, pricing))
                .toList();

        VoucherOption applied = null;
        if (pricing.hasVoucher()) {
            applied = options.stream().filter(VoucherOption::isApplied).findFirst()
                    .orElseGet(() -> voucherDAO.findById(pricing.voucherId())
                            .map(v -> evaluate(v, pricing)).orElse(null));
        }
        return new SelectionView(pricing, options, applied);
    }

    /** Validates the code and stores voucher + discount on the booking. */
    @Transactional
    public VoucherOption applyVoucher(int accountId, int bookingId, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) {
            throw new VoucherException("Please enter a voucher code.");
        }
        if (code.length() > 50) {
            throw new VoucherException("The voucher code is invalid.");
        }

        BookingPricing pricing = loadEditableBooking(accountId, bookingId);
        VoucherCandidate voucher = voucherDAO.findByCode(code)
                .orElseThrow(() -> new VoucherException("Voucher code \"" + code + "\" does not exist."));

        VoucherOption option = evaluate(voucher, pricing);
        if (!option.isSelectable()) {
            throw new VoucherException(option.getReason());
        }

        voucherDAO.updateBookingVoucher(bookingId, voucher.voucherId(), option.getDiscountAmount());
        option.setStatus(VoucherOption.APPLIED);
        option.setReason(null);
        return option;
    }

    /** "Remove" and "Skip": booking continues without a voucher. */
    @Transactional
    public void removeVoucher(int accountId, int bookingId) {
        BookingPricing pricing = loadEditableBooking(accountId, bookingId);
        if (pricing.hasVoucher() || pricing.discountAmount().signum() != 0) {
            voucherDAO.updateBookingVoucher(bookingId, null, BigDecimal.ZERO);
        }
    }

    /**
     * Called on "Continue": re-checks the applied voucher (the F&amp;B subtotal or the
     * voucher itself may have changed) and returns its code, or empty when none is applied.
     */
    @Transactional
    public Optional<String> confirmForCheckout(int accountId, int bookingId) {
        BookingPricing before = loadEditableBooking(accountId, bookingId);
        if (!before.hasVoucher()) {
            return Optional.empty();
        }
        BookingPricing after = refreshAppliedVoucher(accountId, bookingId);
        if (!after.hasVoucher()) {
            throw new VoucherException("Voucher " + before.voucherCode()
                    + " can no longer be applied to this booking and was removed. Please review your total.");
        }
        return Optional.ofNullable(after.voucherCode());
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Recalculates the discount of the applied voucher against the current subtotal.
     * A voucher that is no longer valid is removed from the booking.
     */
    private BookingPricing refreshAppliedVoucher(int accountId, int bookingId) {
        BookingPricing pricing = loadBooking(accountId, bookingId);
        if (!pricing.hasVoucher() || !pricing.isEditable()) {
            return pricing;
        }
        Optional<VoucherCandidate> voucher = voucherDAO.findById(pricing.voucherId());
        VoucherOption check = voucher.map(v -> evaluate(v, pricing)).orElse(null);

        if (check == null || !check.isSelectable()) {
            voucherDAO.updateBookingVoucher(bookingId, null, BigDecimal.ZERO);
        } else if (check.getDiscountAmount().compareTo(pricing.discountAmount()) != 0) {
            voucherDAO.updateBookingVoucher(bookingId, pricing.voucherId(), check.getDiscountAmount());
        } else {
            return pricing;
        }
        return loadBooking(accountId, bookingId);
    }

    private BookingPricing loadBooking(int accountId, int bookingId) {
        return voucherDAO.findBookingPricing(bookingId, accountId)
                .orElseThrow(() -> new VoucherException("Your current booking could not be found."));
    }

    private BookingPricing loadEditableBooking(int accountId, int bookingId) {
        BookingPricing pricing = loadBooking(accountId, bookingId);
        if (!pricing.isEditable()) {
            throw new VoucherException("Payment has already started for this booking, so the voucher can no longer be changed.");
        }
        return pricing;
    }

    /** Applies BR-17..20 and BR-38 to one voucher; never throws. */
    VoucherOption evaluate(VoucherCandidate v, BookingPricing pricing) {
        VoucherOption option = new VoucherOption(v);
        LocalDateTime now = LocalDateTime.now(clock);
        BigDecimal subtotal = pricing.subtotal() != null ? pricing.subtotal() : BigDecimal.ZERO;

        if (!"ACTIVE".equalsIgnoreCase(v.status()) || !"ACTIVE".equalsIgnoreCase(v.promotionStatus())) {
            return reject(option, VoucherOption.INVALID, "This voucher is not active.");
        }
        if ((v.validTo() != null && now.isAfter(v.validTo()))
                || (v.promotionEnd() != null && now.isAfter(v.promotionEnd()))) {
            return reject(option, VoucherOption.EXPIRED, "This voucher has expired.");
        }
        if ((v.validFrom() != null && now.isBefore(v.validFrom()))
                || (v.promotionStart() != null && now.isBefore(v.promotionStart()))) {
            return reject(option, VoucherOption.INVALID, "This voucher is not valid yet.");
        }
        if (v.totalUsageLimit() != null
                && voucherDAO.countUsage(v.voucherId(), pricing.bookingId()) >= v.totalUsageLimit()) {
            return reject(option, VoucherOption.INVALID, "This voucher has reached its usage limit.");
        }
        if (v.perAccountLimit() != null
                && voucherDAO.countCustomerUsage(v.voucherId(), pricing.customerId(), pricing.bookingId())
                >= v.perAccountLimit()) {
            return reject(option, VoucherOption.INVALID, "You have already used this voucher the maximum number of times.");
        }
        if (v.minBookingAmount() != null && subtotal.compareTo(v.minBookingAmount()) < 0) {
            return reject(option, VoucherOption.INVALID,
                    "Requires a minimum booking amount of " + formatMoney(v.minBookingAmount()) + ".");
        }
        if (v.requiredRankMinPoints() != null && pricing.tierPoints() < v.requiredRankMinPoints()) {
            return reject(option, VoucherOption.INVALID,
                    "Requires membership rank " + (v.requiredRankName() != null ? v.requiredRankName() : "") + " or higher.");
        }

        BigDecimal discount = VoucherDiscounts.calculate(v.discountType(), v.discountValue(),
                v.maxDiscountAmount(), subtotal);
        if (discount.signum() <= 0) {
            return reject(option, VoucherOption.INVALID, "This voucher gives no discount for the current booking.");
        }
        option.setDiscountAmount(discount);
        option.setStatus(pricing.voucherId() != null && pricing.voucherId() == v.voucherId()
                ? VoucherOption.APPLIED : VoucherOption.AVAILABLE);
        return option;
    }

    private static VoucherOption reject(VoucherOption option, String status, String reason) {
        option.setStatus(status);
        option.setReason(reason);
        option.setDiscountAmount(BigDecimal.ZERO);
        return option;
    }

    private static String formatMoney(BigDecimal amount) {
        return new java.text.DecimalFormat("#,###").format(amount) + " ₫";
    }
}
