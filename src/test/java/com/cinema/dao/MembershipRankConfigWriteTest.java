package com.cinema.dao;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
class MembershipRankConfigWriteTest {
    @Test void updatesOnlyPointBoundsWithCorrectParameterOrder() {
        var jdbc = mock(JdbcTemplate.class);
        var dao = new MembershipRankDAO(jdbc);
        dao.updateThreshold(2, 500, 1800);
        verify(jdbc).update("UPDATE dbo.MembershipRank SET MinPoints = ?, MaxPoints = ? WHERE RankID = ?", 500, 1800, 2);
        verifyNoMoreInteractions(jdbc);
    }
    @Test void highestRankCanSaveUnlimitedMaximum() {
        var jdbc = mock(JdbcTemplate.class);
        var dao = new MembershipRankDAO(jdbc);
        dao.updateThreshold(3, 2000, null);
        verify(jdbc).update("UPDATE dbo.MembershipRank SET MinPoints = ?, MaxPoints = ? WHERE RankID = ?", 2000, null, 3);
    }
    @Test void writesDiscountAndAllLimitsInTheSameStatement() {
        var jdbc = mock(JdbcTemplate.class);
        var dao = new MembershipRankDAO(jdbc);
        dao.updatePolicy(2, 500, 1800, new java.math.BigDecimal("10.00"), 3,
                new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000"));
        verify(jdbc).update("""
                UPDATE dbo.MembershipRank SET MinPoints=?,MaxPoints=?,DiscountPercent=?,
                    DiscountUsageLimit=?,MinBookingAmount=?,MaxDiscountAmount=? WHERE RankID=?
                """, 500, 1800, new java.math.BigDecimal("10.00"), 3,
                new java.math.BigDecimal("100000"), new java.math.BigDecimal("50000"), 2);
    }
}
