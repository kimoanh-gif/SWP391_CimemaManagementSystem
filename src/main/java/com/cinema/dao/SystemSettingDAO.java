package com.cinema.dao;

import com.cinema.model.SystemSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class SystemSettingDAO {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<SystemSetting> rowMapper = (rs, rowNum) -> mapResultSetToModel(rs);
    private static final String SELECT_COLUMNS = """
            SELECT SettingID, SettingKey, SettingName, Category, ValueType,
                   Value, DefaultValue, [Description], ValidationRules,
                   IsActive, UpdatedAt AS CreatedAt, UpdatedAt
            """;

    @Autowired
    public SystemSettingDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private SystemSetting mapResultSetToModel(ResultSet rs) throws SQLException {
        SystemSetting s = new SystemSetting();
        s.setSettingId(rs.getInt("SettingID"));
        s.setSettingKey(rs.getString("SettingKey"));
        s.setSettingName(rs.getString("SettingName"));
        s.setCategory(rs.getString("Category"));
        s.setValueType(rs.getString("ValueType"));
        s.setValue(rs.getString("Value"));
        s.setDefaultValue(rs.getString("DefaultValue"));
        s.setDescription(rs.getString("Description"));
        s.setValidationRules(rs.getString("ValidationRules"));
        s.setIsActive(rs.getBoolean("IsActive"));
        Timestamp created = rs.getTimestamp("CreatedAt");
        if (created != null) s.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("UpdatedAt");
        if (updated != null) s.setUpdatedAt(updated.toLocalDateTime());
        return s;
    }

    // ==================== READ OPERATIONS ====================

    public Optional<SystemSetting> findByKey(String settingKey) {
        String sql = SELECT_COLUMNS + """
            FROM dbo.SystemSetting
            WHERE UPPER(LTRIM(RTRIM(SettingKey))) = UPPER(LTRIM(RTRIM(?)))
            """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, settingKey.trim()));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<SystemSetting> findById(int settingId) {
        String sql = SELECT_COLUMNS + """
            FROM dbo.SystemSetting WHERE SettingID = ?
            """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, settingId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<SystemSetting> findAll() {
        return jdbcTemplate.query(SELECT_COLUMNS + """
            FROM dbo.SystemSetting ORDER BY SettingKey
            """, rowMapper);
    }

    public List<SystemSetting> findByCategory(String category) {
        return jdbcTemplate.query(SELECT_COLUMNS + """
            FROM dbo.SystemSetting
            WHERE UPPER(Category) = UPPER(LTRIM(RTRIM(?)))
            ORDER BY SettingKey
            """, rowMapper, category);
    }

    public List<SystemSetting> findActive() {
        return jdbcTemplate.query(SELECT_COLUMNS + """
            FROM dbo.SystemSetting WHERE IsActive = 1 ORDER BY SettingKey
            """, rowMapper);
    }

    public boolean existsByKey(String settingKey) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dbo.SystemSetting WHERE UPPER(LTRIM(RTRIM(SettingKey))) = UPPER(LTRIM(RTRIM(?)))",
            Integer.class, settingKey.trim());
        return count != null && count > 0;
    }

    // ==================== WRITE OPERATIONS ====================

    public int insert(SystemSetting setting) {
        String sql = """
            INSERT INTO dbo.SystemSetting
                (SettingKey, Value, [Description], SettingName, Category, ValueType,
                 DefaultValue, ValidationRules, IsActive)
            OUTPUT inserted.SettingID
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        Integer id = jdbcTemplate.queryForObject(sql, Integer.class,
                setting.getSettingKey(),
                setting.getValue(),
                setting.getDescription(),
                setting.getSettingName(),
                setting.getCategory(),
                setting.getValueType(),
                setting.getDefaultValue(),
                setting.getValidationRules(),
                !Boolean.FALSE.equals(setting.getIsActive()));
        return id != null ? id : 0;
    }

    public int updateValue(int settingId, String newValue) {
        return jdbcTemplate.update("""
            UPDATE dbo.SystemSetting SET Value = ?, UpdatedAt = SYSDATETIME() WHERE SettingID = ?
            """, newValue, settingId);
    }

    public int update(SystemSetting setting) {
        return jdbcTemplate.update("""
            UPDATE dbo.SystemSetting
            SET Value = ?, [Description] = ?, SettingName = ?, Category = ?,
                ValueType = ?, DefaultValue = ?, ValidationRules = ?, IsActive = ?,
                UpdatedAt = SYSDATETIME()
            WHERE SettingID = ?
            """,
                setting.getValue(),
                setting.getDescription(),
                setting.getSettingName(),
                setting.getCategory(),
                setting.getValueType(),
                setting.getDefaultValue(),
                setting.getValidationRules(),
                !Boolean.FALSE.equals(setting.getIsActive()),
                setting.getSettingId());
    }

    public int delete(int settingId) {
        return jdbcTemplate.update("DELETE FROM dbo.SystemSetting WHERE SettingID = ?", settingId);
    }

    public int countByCategory(String category) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dbo.SystemSetting WHERE UPPER(Category) = UPPER(LTRIM(RTRIM(?)))",
            Integer.class, category);
        return count != null ? count : 0;
    }

    // ==================== SEED DEFAULT SETTINGS ====================

    /**
     * Seed default settings if they don't exist.
     * Called on application startup.
     */
    public void seedDefaultSettings() {
        seedVoucherSettings();
        seedMembershipSettings();
        seedBookingSettings();
        seedSystemSettings();
    }

    private void seedVoucherSettings() {
        seedIfNotExists("VOUCHER_MAX_DISCOUNT_AMOUNT", "Maximum Discount Amount", "VOUCHER", "NUMBER",
                "0", "0", "Maximum percentage-voucher discount per booking in VND. 0 means no system-wide cap.", "min:0,max:999999999999");
        seedIfNotExists("VOUCHER_VALIDITY_DAYS", "Voucher Validity Period", "VOUCHER", "NUMBER",
                "30", "30", "Number of days voucher is valid after creation", "min:1,max:365");
        seedIfNotExists("VOUCHER_MIN_BOOKING_AMOUNT", "Minimum Booking Amount", "VOUCHER", "NUMBER",
                "50000", "50000", "Minimum booking amount to use voucher (VND)", "min:0");
        seedIfNotExists("VOUCHER_MAX_DISCOUNT_PERCENT", "Maximum Discount Percentage", "VOUCHER", "NUMBER",
                "50", "50", "Maximum discount percentage for percentage-based vouchers", "min:1,max:100");
        seedIfNotExists("VOUCHER_TOTAL_LIMIT", "Voucher Total Limit", "VOUCHER", "NUMBER",
                "1000", "1000", "Maximum number of vouchers that can be issued", "min:1");
        seedIfNotExists("VOUCHER_PER_USER_LIMIT", "Voucher Per User Limit", "VOUCHER", "NUMBER",
                "5", "5", "Maximum vouchers per user", "min:1,max:100");
    }

    private void seedMembershipSettings() {
        seedIfNotExists("MEMBERSHIP_SILVER_THRESHOLD", "Silver Rank Threshold", "MEMBERSHIP", "NUMBER",
                "0", "0", "Minimum points for Silver rank", "min:0");
        seedIfNotExists("MEMBERSHIP_GOLD_THRESHOLD", "Gold Rank Threshold", "MEMBERSHIP", "NUMBER",
                "500", "500", "Minimum points for Gold rank", "min:1");
        seedIfNotExists("MEMBERSHIP_PLATINUM_THRESHOLD", "Platinum Rank Threshold", "MEMBERSHIP", "NUMBER",
                "2000", "2000", "Minimum points for Platinum rank", "min:1");
        seedIfNotExists("MEMBERSHIP_DIAMOND_THRESHOLD", "Diamond Rank Threshold", "MEMBERSHIP", "NUMBER",
                "5000", "5000", "Minimum points for Diamond rank", "min:1");
        seedIfNotExists("MEMBERSHIP_POINTS_RATE", "Points Earning Rate", "MEMBERSHIP", "NUMBER",
                "1", "1", "Points earned per 1,000 VND spent", "min:0.1,max:100");
        seedIfNotExists("MEMBERSHIP_REDEMPTION_RATE", "Points Redemption Rate", "MEMBERSHIP", "NUMBER",
                "100", "100", "Points required for 1 VND discount", "min:1");
        seedIfNotExists("MEMBERSHIP_BIRTHDAY_BONUS", "Birthday Bonus Multiplier", "MEMBERSHIP", "NUMBER",
                "2", "2", "Points multiplier on birthday (e.g., 2 = double points)", "min:1,max:10");
    }

    private void seedBookingSettings() {
        seedIfNotExists("BOOKING_SEAT_HOLD_MINUTES", "Seat Hold Time", "BOOKING", "NUMBER",
                "15", "15", "Minutes to hold seats before payment", "min:1,max:60");
        seedIfNotExists("BOOKING_MAX_TICKETS", "Max Tickets Per Order", "BOOKING", "NUMBER",
                "10", "10", "Maximum tickets per booking order", "min:1,max:20");
        seedIfNotExists("BOOKING_CANCEL_WINDOW_HOURS", "Cancellation Window", "BOOKING", "NUMBER",
                "24", "24", "Hours before showtime to allow cancellation", "min:0,max:168");
        seedIfNotExists("BOOKING_BOOKING_FEE", "Booking Fee", "BOOKING", "NUMBER",
                "0", "0", "Booking fee in VND", "min:0");
    }

    private void seedSystemSettings() {
        seedIfNotExists("SYSTEM_PASSWORD_MIN_LENGTH", "Password Minimum Length", "SYSTEM", "NUMBER",
                "8", "8", "Minimum password length", "min:6,max:32");
        seedIfNotExists("SYSTEM_OTP_EXPIRY_MINUTES", "OTP Expiry Time", "SYSTEM", "NUMBER",
                "5", "5", "OTP validity in minutes", "min:1,max:30");
        seedIfNotExists("SYSTEM_OTP_MAX_SEND_DAILY", "OTP Max Sends Per Day", "SYSTEM", "NUMBER",
                "5", "5", "Maximum OTP codes that can be sent per email per day", "min:1,max:20");
        seedIfNotExists("SYSTEM_OTP_RESEND_COOLDOWN", "OTP Resend Cooldown", "SYSTEM", "NUMBER",
                "60", "60", "Seconds to wait between OTP resends", "min:10,max:300");
        seedIfNotExists("SYSTEM_SESSION_TIMEOUT_MINUTES", "Session Timeout", "SYSTEM", "NUMBER",
                "30", "30", "Session timeout in minutes", "min:5,max:480");
        seedIfNotExists("SYSTEM_LOGIN_ATTEMPTS", "Max Login Attempts", "SYSTEM", "NUMBER",
                "5", "5", "Maximum failed login attempts before lockout", "min:1,max:10");
    }

    private void seedIfNotExists(String key, String name, String category, String valueType,
                                 String value, String defaultValue, String description, String validationRules) {
        if (!existsByKey(key)) {
            SystemSetting setting = new SystemSetting();
            setting.setSettingKey(key);
            setting.setSettingName(name);
            setting.setCategory(category);
            setting.setValueType(valueType);
            setting.setValue(value);
            setting.setDefaultValue(defaultValue);
            setting.setDescription(description);
            setting.setValidationRules(validationRules);
            setting.setIsActive(true);
            insert(setting);
        }
    }
}
