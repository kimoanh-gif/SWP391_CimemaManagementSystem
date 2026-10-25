package com.cinema.service;

import com.cinema.dao.MembershipRankDAO;
import com.cinema.dao.VoucherDAO;
import com.cinema.model.CustomerProfile;
import com.cinema.model.LoyaltyInfo;
import com.cinema.model.MembershipRank;
import com.cinema.model.VoucherCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock
    private CustomerProfileService customerProfileService;

    @Mock
    private MembershipRankDAO membershipRankDAO;

    @Mock
    private VoucherDAO voucherDAO;

    private LoyaltyService service;

    @BeforeEach
    void setUp() {
        service = new LoyaltyService(customerProfileService, membershipRankDAO, voucherDAO, new MembershipRankCalculator());
    }

    @Test
    void rankProgressAndNextRankAreCalculatedFromTierPoints() {
        when(customerProfileService.getProfile(7)).thenReturn(profile(800));
        when(membershipRankDAO.findActive()).thenReturn(defaultRanks());
        when(voucherDAO.findCurrentlyValid()).thenReturn(List.of());

        LoyaltyInfo info = service.getLoyaltyInfo(7);

        assertEquals("Gold Member", info.getCurrentRankName());
        assertEquals("Platinum Member", info.getNextRankName());
        assertEquals(1200, info.pointsToNextRank());
        assertEquals(20, info.progressPercent()); // (800-500)/(2000-500)
        assertTrue(info.rankBenefits().contains("5% member discount"));
        assertTrue(info.rankBenefits().contains("Priority booking"));
    }

    @Test
    void topRankHasNoNextRank() {
        when(customerProfileService.getProfile(7)).thenReturn(profile(9000));
        when(membershipRankDAO.findActive()).thenReturn(defaultRanks());
        when(voucherDAO.findCurrentlyValid()).thenReturn(List.of());

        LoyaltyInfo info = service.getLoyaltyInfo(7);

        assertEquals("Diamond Member", info.getCurrentRankName());
        assertNull(info.getNextRankName());
        assertTrue(info.isTopRank());
        assertEquals(100, info.progressPercent());
    }

    @Test
    void vouchersForHigherRankAreHidden() {
        LocalDateTime now = LocalDateTime.now();
        VoucherCandidate open = candidate(1, "ALL10", null, null, now);
        VoucherCandidate platinum = candidate(2, "PLAT20", "Platinum Member", 2000, now);
        when(voucherDAO.findCurrentlyValid()).thenReturn(List.of(open, platinum));

        List<LoyaltyInfo.LoyaltyVoucher> vouchers = service.findRankVouchers(3, 800);

        assertEquals(1, vouchers.size());
        assertEquals("ALL10", vouchers.get(0).code());
    }

    @Test
    void plainTextBenefitsAreSplitIntoLines() {
        MembershipRank rank = rank("SILVER", "Silver Member", 0, "Birthday gift; Free upgrade", "0");

        assertEquals(List.of("Birthday gift", "Free upgrade"), service.describeBenefits(rank));
    }

    // ------------------------------------------------------------------ helpers

    private static CustomerProfile profile(int tierPoints) {
        CustomerProfile p = new CustomerProfile();
        p.setAccountId(7);
        p.setCustomerId(3);
        p.setFullName("Nguyen Van A");
        p.setAvailablePoints(350);
        p.setTierPoints(tierPoints);
        return p;
    }

    private static List<MembershipRank> defaultRanks() {
        return List.of(
                rank("SILVER", "Silver Member", 0, "{\"pointsRate\":1,\"birthdayBonus\":true,\"priorityBooking\":false}", "0"),
                rank("GOLD", "Gold Member", 500, "{\"pointsRate\":1.5,\"birthdayBonus\":true,\"priorityBooking\":true}", "5"),
                rank("PLATINUM", "Platinum Member", 2000, "{\"pointsRate\":2}", "10"),
                rank("DIAMOND", "Diamond Member", 5000, "{\"pointsRate\":3,\"freeSnacks\":true}", "15"));
    }

    private static MembershipRank rank(String name, String display, int min, String benefits, String discount) {
        return new MembershipRank(null, name, display, min, null, benefits, discount, true);
    }

    private static VoucherCandidate candidate(int id, String code, String rankName, Integer rankMin, LocalDateTime now) {
        return new VoucherCandidate(id, code, "PERCENT", BigDecimal.TEN, null, null,
                now.minusDays(1), now.plusDays(1), null, null, "ACTIVE",
                "Promo", null, "ACTIVE", now.minusDays(1), now.plusDays(1),
                rankMin != null ? 9 : null, rankName, rankMin);
    }
}
