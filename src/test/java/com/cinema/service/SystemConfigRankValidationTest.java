package com.cinema.service;
import com.cinema.dao.*;
import com.cinema.model.MembershipRank;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SystemConfigRankValidationTest {
    MembershipRankDAO ranks = mock(MembershipRankDAO.class);
    SystemConfigService service = new SystemConfigService(mock(SystemSettingDAO.class), ranks);
    MembershipRank rank(int id, int min, Integer max) {
        var rank = new MembershipRank(); rank.setRankId(id);rank.setName("RANK"+id);rank.setDisplayName("Rank "+id);
        rank.setMinPoints(min);rank.setMaxPoints(max);return rank;
    }
    @BeforeEach void setup() {
        when(ranks.findAll()).thenReturn(List.of(rank(1,0,499),rank(2,500,1999),rank(3,2000,null)));
        when(ranks.findById(2)).thenReturn(Optional.of(rank(2,500,1999)));
        when(ranks.updateThreshold(2,600)).thenReturn(1);
    }
    @Test void maximumDiscountAmountMustBeWholeNonNegativeVnd() {
        var setting = new com.cinema.model.SystemSetting();
        setting.setSettingKey("VOUCHER_MAX_DISCOUNT_AMOUNT"); setting.setValueType("NUMBER");
        setting.setValidationRules("min:0,max:999999999999");
        for (String value : new String[]{"", "-1", "1.5", "1e5", "NaN", "Infinity", "1000000000000"}) {
            assertFalse(service.validateSetting(setting, value).valid(), value);
        }
        assertTrue(service.validateSetting(setting, "0").valid());
        assertTrue(service.validateSetting(setting, "100000").valid());
    }
    @Test void rejectsOverlapPreviousMaximum() {
        assertFalse(service.updateRankThreshold(2,499).success());
        verify(ranks,never()).updateThreshold(anyInt(),anyInt());
    }
    @Test void rejectsOwnInvertedRangeAndNextThreshold() {
        assertFalse(service.validateMembershipThreshold(2,2000).valid());
        assertFalse(service.validateMembershipThreshold(2,3000).valid());
    }
    @Test void rejectsNegativeAndMissingRank() {
        assertFalse(service.validateMembershipThreshold(1,-1).valid());
        assertFalse(service.validateMembershipThreshold(99,100).valid());
    }
    @Test void savesValidThresholdWhileHoldingRankLock() {
        assertTrue(service.updateRankThreshold(2,600).success());
        var ordered=inOrder(ranks);ordered.verify(ranks).lockThresholds();ordered.verify(ranks).findById(2);
        verify(ranks).updateThreshold(2,600);
    }

    @Test void validatesBothBoundsAndOnlyAllowsUnlimitedOnHighestRank() {
        for (Integer max : new Integer[]{-1, 499, 2000, null}) {
            assertFalse(service.updateRankThreshold(2, 500, max).success());
        }
        assertFalse(service.updateRankThreshold(2, 499, 1800).success());
        verify(ranks, never()).updateThreshold(anyInt(), anyInt(), any());
        assertTrue(service.validateMembershipThreshold(2, 500, 500).valid());
        assertTrue(service.validateMembershipThreshold(3, 2000, null).valid());
        assertTrue(service.validateMembershipThreshold(3, 2000, Integer.MAX_VALUE).valid());
    }

    @Test void savesBothBoundsAtomicallyWhileHoldingRankLock() {
        when(ranks.updateThreshold(2, 600, 1800)).thenReturn(1);
        assertTrue(service.updateRankThreshold(2, 600, 1800).success());
        var ordered = inOrder(ranks);
        ordered.verify(ranks).lockThresholds();
        ordered.verify(ranks).findById(2);
        ordered.verify(ranks).findAll();
        ordered.verify(ranks).updateThreshold(2, 600, 1800);
        assertFalse(service.updateRankThreshold(99, 0, null).success());
    }

    @Test void validatesPolicyAndPersistsAllFieldsUnderTheRankLock() {
        var zero = java.math.BigDecimal.ZERO;
        var ten = java.math.BigDecimal.TEN;
        for (String bad : List.of("-1", "1.5", "1000000000000")) {
            assertFalse(service.updateRankPolicy(2, 500, 1800, ten, 3, new java.math.BigDecimal(bad), ten).success());
            assertFalse(service.updateRankPolicy(2, 500, 1800, ten, 3, zero, new java.math.BigDecimal(bad)).success());
        }
        assertFalse(service.updateRankPolicy(2, 500, 1800, ten, -1, zero, ten).success());
        assertFalse(service.updateRankPolicy(2, 500, 1800, ten, 3, zero, zero).success());
        assertFalse(service.updateRankPolicy(2, 500, 1800, zero, 3, zero, ten).success());
        assertFalse(service.updateRankPolicy(2, 500, 1800, new java.math.BigDecimal("100.01"), 3, zero, ten).success());
        assertFalse(service.updateRankPolicy(2, 500, 1800, new java.math.BigDecimal("5.555"), 3, zero, ten).success());
        verify(ranks, never()).updatePolicy(anyInt(), anyInt(), any(), any(), anyInt(), any(), any());
        when(ranks.updatePolicy(2, 500, 1800, new java.math.BigDecimal("10.00"), 3,
                new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000"))).thenReturn(1);
        assertTrue(service.updateRankPolicy(2, 500, 1800, ten, 3, new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000")).success());
        var sequence = inOrder(ranks);
        sequence.verify(ranks).lockThresholds();
        sequence.verify(ranks).findById(2);
        sequence.verify(ranks).findAll();
        sequence.verify(ranks).updatePolicy(2, 500, 1800, new java.math.BigDecimal("10.00"), 3,
                new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000"));
    }

}
