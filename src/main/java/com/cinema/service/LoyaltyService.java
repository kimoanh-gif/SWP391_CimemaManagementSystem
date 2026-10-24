package com.cinema.service;

import com.cinema.dao.MembershipRankDAO;
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

    public LoyaltyService(CustomerProfileService customerProfileService,
                          MembershipRankDAO membershipRankDAO,
                          VoucherDAO voucherDAO,
                          MembershipRankCalculator rankCalculator) {
        this.customerProfileService = customerProfileService;
        this.membershipRankDAO = membershipRankDAO;
        this.voucherDAO = voucherDAO;
        this.rankCalculator = rankCalculator;
    }

    public LoyaltyInfo getLoyaltyInfo(int accountId) {
        CustomerProfile profile = customerProfileService.getProfile(accountId);
        int tierPoints = Math.max(0, profile.getTierPoints());

        List<MembershipRank> ranks = membershipRankDAO.findActive();
        MembershipRank current = rankCalculator.currentRank(ranks, tierPoints).orElse(null);
        MembershipRank next = rankCalculator.nextRank(ranks, tierPoints).orElse(null);
        int pointsToNext = next != null && next.getMinPoints() != null
                ? Math.max(0, next.getMinPoints() - tierPoints) : 0;

        return new LoyaltyInfo(
                profile.getFullName(),
                profile.getAvailablePoints(),
                tierPoints,
                current,
                next,
                pointsToNext,
                rankCalculator.progressPercent(current, next, tierPoints),
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

        BigDecimal discount = parseDecimal(rank.getDiscountPercent());
        if (discount != null && discount.signum() > 0) {
            lines.add(discount.stripTrailingZeros().toPlainString() + "% member discount");
        }

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

    private static String describeBenefit(String key, JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (value.isBoolean() && !value.asBoolean()) return null;
        return switch (key) {
            case "pointsRate" -> "Earn points ×" + value.asText();
            case "birthdayBonus" -> "Birthday bonus";
            case "priorityBooking" -> "Priority booking";
            case "freeSnacks" -> "Free snacks";
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
