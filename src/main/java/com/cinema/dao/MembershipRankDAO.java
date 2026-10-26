package com.cinema.dao;

import com.cinema.model.MembershipRank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class MembershipRankDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MembershipRank> rowMapper = (rs, rowNum) -> mapResultSetToModel(rs);
    private static final String SELECT_COLUMNS = """
            SELECT RankID, [Name], DisplayName, MinPoints, MaxPoints,
                   Benefits, DiscountPercent, IsActive
            """;

    @Autowired
    public MembershipRankDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private MembershipRank mapResultSetToModel(ResultSet rs) throws SQLException {
        MembershipRank r = new MembershipRank();
        r.setRankId(rs.getInt("RankID"));
        r.setName(rs.getString("Name"));
        r.setDisplayName(rs.getString("DisplayName"));
        r.setMinPoints(rs.getInt("MinPoints"));
        Integer maxPoints = rs.getInt("MaxPoints");
        r.setMaxPoints(rs.wasNull() ? null : maxPoints);
        r.setBenefits(rs.getString("Benefits"));
        r.setDiscountPercent(rs.getString("DiscountPercent"));
        r.setIsActive(rs.getBoolean("IsActive"));
        return r;
    }

    public Optional<MembershipRank> findById(int rankId) {
        String sql = SELECT_COLUMNS + """
            FROM dbo.MembershipRank WHERE RankID = ?
            """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, rankId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<MembershipRank> findByName(String name) {
        String sql = SELECT_COLUMNS + """
            FROM dbo.MembershipRank WHERE UPPER(LTRIM(RTRIM(Name))) = UPPER(LTRIM(RTRIM(?)))
            """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, name.trim()));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<MembershipRank> findAll() {
        return jdbcTemplate.query(SELECT_COLUMNS + """
            FROM dbo.MembershipRank ORDER BY MinPoints
            """, rowMapper);
    }

    public List<MembershipRank> findActive() {
        return jdbcTemplate.query(SELECT_COLUMNS + """
            FROM dbo.MembershipRank WHERE IsActive = 1 ORDER BY MinPoints
            """, rowMapper);
    }

    public int insert(MembershipRank rank) {
        String sql = """
            INSERT INTO dbo.MembershipRank
                ([Name], DisplayName, MinPoints, MaxPoints, Benefits, DiscountPercent, IsActive)
            OUTPUT inserted.RankID
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        Integer id = jdbcTemplate.queryForObject(sql, Integer.class,
                rank.getName(),
                rank.getDisplayName(),
                rank.getMinPoints(),
                rank.getMaxPoints(),
                rank.getBenefits(),
                rank.getDiscountPercent(),
                !Boolean.FALSE.equals(rank.getIsActive()));
        return id != null ? id : 0;
    }

    public int update(MembershipRank rank) {
        return jdbcTemplate.update("""
            UPDATE dbo.MembershipRank
            SET [Name] = ?, DisplayName = ?, MinPoints = ?, MaxPoints = ?,
                Benefits = ?, DiscountPercent = ?, IsActive = ?
            WHERE RankID = ?
            """,
                rank.getName(),
                rank.getDisplayName(),
                rank.getMinPoints(),
                rank.getMaxPoints(),
                rank.getBenefits(),
                rank.getDiscountPercent(),
                !Boolean.FALSE.equals(rank.getIsActive()),
                rank.getRankId());
    }

    public int updateThreshold(int rankId, int minPoints) {
        return jdbcTemplate.update(
            "UPDATE dbo.MembershipRank SET MinPoints = ? WHERE RankID = ?",
            minPoints, rankId);
    }

    public void lockThresholds() {
        jdbcTemplate.queryForList("SELECT RankID FROM dbo.MembershipRank WITH (UPDLOCK, HOLDLOCK) ORDER BY RankID");
    }

    public int delete(int rankId) {
        return jdbcTemplate.update("DELETE FROM dbo.MembershipRank WHERE RankID = ?", rankId);
    }

    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dbo.MembershipRank WHERE UPPER(LTRIM(RTRIM(Name))) = UPPER(LTRIM(RTRIM(?)))",
            Integer.class, name.trim());
        return count != null && count > 0;
    }

    /**
     * Seed default membership ranks.
     */
    public void seedDefaultRanks() {
        if (!existsByName("SILVER")) {
            MembershipRank silver = new MembershipRank();
            silver.setName("SILVER");
            silver.setDisplayName("Silver Member");
            silver.setMinPoints(0);
            silver.setMaxPoints(499);
            silver.setDiscountPercent("0");
            silver.setBenefits("{\"pointsRate\":1,\"birthdayBonus\":true,\"priorityBooking\":false}");
            silver.setIsActive(true);
            insert(silver);
        }
        if (!existsByName("GOLD")) {
            MembershipRank gold = new MembershipRank();
            gold.setName("GOLD");
            gold.setDisplayName("Gold Member");
            gold.setMinPoints(500);
            gold.setMaxPoints(1999);
            gold.setDiscountPercent("5");
            gold.setBenefits("{\"pointsRate\":1.5,\"birthdayBonus\":true,\"priorityBooking\":true}");
            gold.setIsActive(true);
            insert(gold);
        }
        if (!existsByName("PLATINUM")) {
            MembershipRank platinum = new MembershipRank();
            platinum.setName("PLATINUM");
            platinum.setDisplayName("Platinum Member");
            platinum.setMinPoints(2000);
            platinum.setMaxPoints(4999);
            platinum.setDiscountPercent("10");
            platinum.setBenefits("{\"pointsRate\":2,\"birthdayBonus\":true,\"priorityBooking\":true,\"freeSnacks\":false}");
            platinum.setIsActive(true);
            insert(platinum);
        }
        if (!existsByName("DIAMOND")) {
            MembershipRank diamond = new MembershipRank();
            diamond.setName("DIAMOND");
            diamond.setDisplayName("Diamond Member");
            diamond.setMinPoints(5000);
            diamond.setMaxPoints(null);
            diamond.setDiscountPercent("15");
            diamond.setBenefits("{\"pointsRate\":3,\"birthdayBonus\":true,\"priorityBooking\":true,\"freeSnacks\":true}");
            diamond.setIsActive(true);
            insert(diamond);
        }
    }
}
