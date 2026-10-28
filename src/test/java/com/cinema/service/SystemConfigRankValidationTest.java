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
}
