package com.cinema.dao;

import com.cinema.model.AvailableVoucher;
import com.cinema.model.MemberLookupResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MemberLookupDAO {

    private final JdbcTemplate jdbcTemplate;

    public MemberLookupDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<MemberLookupResult> findByPhone(String phone) {
        return findMember("LTRIM(RTRIM(a.Phone)) = ?", phone.trim());
    }

    public Optional<MemberLookupResult> findByEmail(String email) {
        return findMember("LOWER(LTRIM(RTRIM(a.Email))) = LOWER(?)", email.trim());
    }

    private Optional<MemberLookupResult> findMember(String predicate, String value) {
        String sql = """
                SELECT c.CustomerID, a.FullName, a.Phone, a.Email, mr.[Name] AS MembershipRank,
                       c.AvailablePoints, c.TierPoints
                FROM dbo.Customer c
                JOIN dbo.Account a ON a.AccountID = c.AccountID
                LEFT JOIN dbo.MembershipRank mr ON mr.RankID = c.RankID
                WHERE %s AND UPPER(a.[Status]) = 'ACTIVE'
                """.formatted(predicate);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new MemberLookupResult(
                rs.getInt("CustomerID"), rs.getString("FullName"), rs.getString("Phone"),
                rs.getString("Email"), rs.getString("MembershipRank"),
                rs.getInt("AvailablePoints"), rs.getInt("TierPoints"), List.of()
        ), value).stream().findFirst().map(member ->
                withVouchers(member, findAvailableVouchers(member.customerId())));
    }

    public Optional<Integer> findCustomerIdByPhoneOrEmail(String value) {
        String column = value.contains("@") ? "Email" : "Phone";
        String predicate = "Email".equals(column)
                ? "LOWER(LTRIM(RTRIM(a.Email))) = LOWER(?)"
                : "LTRIM(RTRIM(a.Phone)) = ?";
        return jdbcTemplate.query("""
                SELECT c.CustomerID
                FROM dbo.Customer c
                JOIN dbo.Account a ON a.AccountID = c.AccountID
                WHERE %s AND UPPER(a.[Status]) = 'ACTIVE'
                """.formatted(predicate), (rs, rowNum) -> rs.getInt("CustomerID"), value.trim())
                .stream().findFirst();
    }

    private List<AvailableVoucher> findAvailableVouchers(int customerId) {
        return jdbcTemplate.query("""
                SELECT v.VoucherID, v.Code, p.[Name] AS PromotionName, p.[Description],
                       v.DiscountType, v.DiscountValue, v.MaxDiscountAmount, v.MinBookingAmount,
                       v.ValidTo
                FROM dbo.Voucher v
                JOIN dbo.Promotion p ON p.PromotionID = v.PromotionID
                JOIN dbo.Customer c ON c.CustomerID = ?
                WHERE UPPER(v.[Status]) = 'ACTIVE' AND UPPER(p.[Status]) = 'ACTIVE'
                  AND v.ValidFrom <= SYSDATETIME() AND v.ValidTo >= SYSDATETIME()
                  AND p.StartDate <= SYSDATETIME() AND p.EndDate >= SYSDATETIME()
                  AND (v.RequiredRankID IS NULL OR v.RequiredRankID = c.RankID)
                  AND (v.TotalUsageLimit IS NULL OR (
                      SELECT COUNT(*) FROM dbo.Booking b
                      WHERE b.VoucherID = v.VoucherID
                        AND UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')
                  ) < v.TotalUsageLimit)
                  AND (v.PerAccountLimit IS NULL OR (
                      SELECT COUNT(*) FROM dbo.Booking b
                      WHERE b.VoucherID = v.VoucherID AND b.CustomerID = c.CustomerID
                        AND UPPER(b.[Status]) NOT IN ('CANCELLED', 'EXPIRED')
                  ) < v.PerAccountLimit)
                ORDER BY v.ValidTo, v.VoucherID
                """, (rs, rowNum) -> new AvailableVoucher(
                rs.getInt("VoucherID"), rs.getString("Code"), rs.getString("PromotionName"),
                rs.getString("Description"), rs.getString("DiscountType"),
                rs.getBigDecimal("DiscountValue"), rs.getBigDecimal("MaxDiscountAmount"),
                rs.getBigDecimal("MinBookingAmount"), rs.getTimestamp("ValidTo").toLocalDateTime()
        ), customerId);
    }

    private MemberLookupResult withVouchers(MemberLookupResult member, List<AvailableVoucher> vouchers) {
        return new MemberLookupResult(member.customerId(), member.fullName(), member.phone(), member.email(),
                member.membershipRank(), member.availablePoints(), member.tierPoints(), vouchers);
    }
}
