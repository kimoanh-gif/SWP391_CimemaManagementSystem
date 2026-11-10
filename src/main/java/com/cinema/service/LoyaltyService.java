package com.cinema.service;

import com.cinema.dao.MembershipRankDAO;
import com.cinema.dao.LoyaltyPointDAO;
import com.cinema.dao.VoucherDAO;
import com.cinema.model.CustomerProfile;
import com.cinema.model.LoyaltyInfo;
import com.cinema.model.LoyaltyInfo.LoyaltyVoucher;
import com.cinema.model.MembershipRank;
import com.cinema.model.VoucherCandidate;
import com.cinema.model.VoucherOption;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * UC18 – View Loyalty Information. Read-only: the Customer can never change points or rank (BR-34).
 *
 *  BR-32 rank is determined from TierPoints and MembershipRank.MinPoints
 *  BR-33 benefits / vouchers depend on the current rank
 *
 * Assignee: Nguyễn Đức Anh (Iter3)
 */
@Service
public class LoyaltyService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final CustomerProfileService customerProfileService;
    private final MembershipRankDAO membershipRankDAO;
    private final VoucherDAO voucherDAO;
    private final MembershipRankCalculator rankCalculator;
    private final LoyaltyPointDAO loyaltyPointDAO;

    public LoyaltyService(CustomerProfileService customerProfileService,
                          MembershipRankDAO membershipRankDAO,
                          VoucherDAO voucherDAO,
                          MembershipRankCalculator rankCalculator,
                          LoyaltyPointDAO loyaltyPointDAO) {
        this.customerProfileService = customerProfileService;
        this.membershipRankDAO = membershipRankDAO;
        this.voucherDAO = voucherDAO;
        this.rankCalculator = rankCalculator;
        this.loyaltyPointDAO = loyaltyPointDAO;
    }

    public LoyaltyInfo getLoyaltyInfo(int accountId) {
        CustomerProfile profile = customerProfileService.getProfile(accountId);
        int tierPoints = Math.max(0, profile.getTierPoints());

        List<MembershipRank> ranks = membershipRankDAO.findActive();
        MembershipRank current = rankCalculator.currentRank(ranks, tierPoints).orElse(null);
        MembershipRank next = rankCalculator.nextRank(ranks, tierPoints).orElse(null);
        int pointsToNext = next != null && next.getMinPoints() != null
                ? Math.max(0, next.getMinPoints() - tierPoints) : 0;
        LoyaltyPointDAO.ExpiringPoints expiry = loyaltyPointDAO.findNextExpiry(profile.getCustomerId()).orElse(null);

        return new LoyaltyInfo(
                profile.getFullName(),
                profile.getAvailablePoints(),
                tierPoints,
                current,
                next,
                pointsToNext,
                rankCalculator.progressPercent(current, next, tierPoints),
                expiry != null ? expiry.points() : 0,
                expiry != null ? expiry.expiresAt().toLocalDate() : null,
                describeBenefits(current),
                findRankVouchers(profile.getCustomerId(), tierPoints)
        );
    }

    /** Vouchers that are valid now, allowed for this rank and not used up by this Customer. */
    List<LoyaltyVoucher> findRankVouchers(int customerId, int tierPoints) {
        List<LoyaltyVoucher> result = new ArrayList<>();
        DecimalFormat money = new DecimalFormat("#,###");
        for (VoucherCandidate v : voucherDAO.findCurrentlyValid()) {
            if (v.requiredRankMinPoints() != null && tierPoints < v.requiredRankMinPoints()) continue;
            if (v.totalUsageLimit() != null && voucherDAO.countUsage(v.voucherId(), 0) >= v.totalUsageLimit()) continue;
            if (v.perAccountLimit() != null
                    && voucherDAO.countCustomerUsage(v.voucherId(), customerId, 0) >= v.perAccountLimit()) continue;

            VoucherOption display = new VoucherOption(v);
            String minBooking = v.minBookingAmount() != null && v.minBookingAmount().signum() > 0
                    ? money.format(v.minBookingAmount()) + " ₫" : null;
            result.add(new LoyaltyVoucher(v.code(), v.promotionName(), display.getDiscountValueDisplay(),
                    minBooking, v.requiredRankName(), display.getValidityDisplay()));
        }
        return result;
    }

    /**
     * Turns MembershipRank.Benefits into readable lines.
     * The column holds JSON such as {"pointsRate":1.5,"birthdayBonus":true}
     * or plain text from older data.
     */
    List<String> describeBenefits(MembershipRank rank) {
        List<String> lines = new ArrayList<>();
        if (rank == null) return lines;

        String memberDiscount = describeMemberDiscount(rank);
        if (memberDiscount != null) lines.add(memberDiscount);

        String raw = rank.getBenefits();
        if (raw == null || raw.isBlank()) return lines;

        String text = raw.trim();
        if (text.startsWith("{")) {
            try {
                JsonNode node = JSON.readTree(text);
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String line = describeBenefit(field.getKey(), field.getValue());
                    if (line != null) lines.add(line);
                }
                return lines;
            } catch (Exception ignored) {
                // Not valid JSON – fall back to plain text below.
            }
        }
        Arrays.stream(text.split("[\\r\\n;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(lines::add);
        return lines;
    }

    /**
     * The rank discount applied at checkout (MembershipDiscountPolicy). It only exists when the
     * percentage, the per-booking cap and the usage limit are all set; otherwise no line is shown,
     * so the screen never promises a discount that checkout will not give.
     */
    public static String describeMemberDiscount(MembershipRank rank) {
        if (rank == null) return null;
        BigDecimal percent = parseDecimal(rank.getDiscountPercent());
        BigDecimal cap = rank.getMaxDiscountAmount();
        Integer limit = rank.getDiscountUsageLimit();
        if (percent == null || percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) > 0
                || cap == null || cap.signum() <= 0 || limit == null || limit <= 0) {
            return null;
        }
        BigDecimal min = rank.getMinBookingAmount();
        String minText = min == null || min.signum() <= 0 ? "any booking" : "bookings from " + money(min);
        return percent.stripTrailingZeros().toPlainString() + "% member discount on " + minText
                + ", up to " + money(cap) + " per booking, for " + limit + (limit == 1 ? " booking" : " bookings")
                + " at this rank (not combined with vouchers)";
    }

    private static String money(BigDecimal amount) {
        return String.format(java.util.Locale.US, "%,d ₫", amount.setScale(0, java.math.RoundingMode.DOWN).longValue());
    }

    private static String describeBenefit(String key, JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (value.isBoolean() && !value.asBoolean()) return null;
        return switch (key) {
            case "pointsRate" -> "Earn points ×" + value.asText();
            case "birthdayBonus" -> "Birthday bonus";
            case "priorityBooking" -> "Priority booking";
            case "freeSnacks" -> "Free snacks";
            case "cycleMinOrders" -> null;
            default -> value.isBoolean() ? humanize(key) : humanize(key) + ": " + value.asText();
        };
    }

    private static String humanize(String key) {
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2");
        return spaced.isEmpty() ? key : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
