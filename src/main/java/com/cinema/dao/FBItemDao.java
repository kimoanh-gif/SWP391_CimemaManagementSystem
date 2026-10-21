package com.cinema.dao;

import com.cinema.model.FBItem;
import com.cinema.model.FBItemForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for FBItem and ComboItem tables (Screen 61 / UC-32).
 * Designed with dynamic column compatibility for legacy database schemas.
 */
@Repository
public class FBItemDao {

    private final JdbcTemplate jdbcTemplate;

    private volatile boolean columnsChecked = false;
    private boolean hasItemCode = true;
    private boolean hasCostPrice = true;
    private boolean hasCreatedAt = true;
    private boolean hasUpdatedAt = true;
    private boolean hasComboTable = true;

    public FBItemDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private synchronized void checkColumns() {
        if (columnsChecked) {
            return;
        }
        try {
            hasItemCode = checkColumnExists("ItemCode");
            hasCostPrice = checkColumnExists("CostPrice");
            hasCreatedAt = checkColumnExists("CreatedAt");
            hasUpdatedAt = checkColumnExists("UpdatedAt");
            hasComboTable = checkTableExists("dbo.ComboItem");
        } catch (Exception ex) {
            // Keep default values if metadata check fails
        }
        columnsChecked = true;
    }

    private boolean checkColumnExists(String columnName) {
        try {
            Integer len = jdbcTemplate.queryForObject(
                    "SELECT COL_LENGTH('dbo.FoodBeverageItem', '" + columnName + "')", Integer.class);
            return len != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkTableExists(String tableName) {
        try {
            Integer id = jdbcTemplate.queryForObject(
                    "SELECT OBJECT_ID('" + tableName + "', 'U')", Integer.class);
            return id != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String getSelectSql() {
        checkColumns();
        String itemCodeCol = hasItemCode ? "ItemCode" : "CONCAT('FB-', FoodBeverageItemID) AS ItemCode";
        String costPriceCol = hasCostPrice ? "CostPrice" : "CAST(0 AS DECIMAL(18,2)) AS CostPrice";
        String createdAtCol = hasCreatedAt ? "CreatedAt" : "CAST(NULL AS DATETIME2) AS CreatedAt";
        String updatedAtCol = hasUpdatedAt ? "UpdatedAt" : "CAST(NULL AS DATETIME2) AS UpdatedAt";

        return "SELECT FoodBeverageItemID AS ItemID, " + itemCodeCol + ", " +
                "[Name] AS ItemName, ItemType AS Category, [Description], " +
                "Price AS SellingPrice, " + costPriceCol + ", ImageURL AS Image, " +
                "[Status], " + createdAtCol + ", " + updatedAtCol + " " +
                "FROM dbo.FoodBeverageItem";
    }

    private final RowMapper<FBItem> rowMapper = (rs, rowNum) -> {
        FBItem item = new FBItem();
        item.setItemId(rs.getInt("ItemID"));
        item.setItemCode(rs.getString("ItemCode"));
        item.setItemName(rs.getString("ItemName"));
        item.setCategory(rs.getString("Category"));
        item.setDescription(rs.getString("Description"));
        item.setSellingPrice(rs.getBigDecimal("SellingPrice"));
        item.setCostPrice(rs.getBigDecimal("CostPrice"));
        item.setImage(rs.getString("Image"));
        item.setStatus(rs.getString("Status"));
        item.setCreatedAt(rs.getTimestamp("CreatedAt") != null ? rs.getTimestamp("CreatedAt").toLocalDateTime() : null);
        item.setUpdatedAt(rs.getTimestamp("UpdatedAt") != null ? rs.getTimestamp("UpdatedAt").toLocalDateTime() : null);
        return item;
    };

    public List<FBItem> findAll(String keyword, String category, String status) {
        checkColumns();
        StringBuilder sql = new StringBuilder(getSelectSql() + " WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            if (hasItemCode) {
                sql.append("AND ([Name] LIKE ? OR ItemCode LIKE ? OR [Description] LIKE ?) ");
                String kw = "%" + keyword.trim() + "%";
                params.add(kw);
                params.add(kw);
                params.add(kw);
            } else {
                sql.append("AND ([Name] LIKE ? OR [Description] LIKE ? OR CAST(FoodBeverageItemID AS VARCHAR) LIKE ?) ");
                String kw = "%" + keyword.trim() + "%";
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }
        }

        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND ItemType = ? ");
            params.add(category.trim());
        }

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND Status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY FoodBeverageItemID DESC");

        List<FBItem> items = jdbcTemplate.query(sql.toString(), params.toArray(), rowMapper);
        for (FBItem item : items) {
            if (item.isCombo()) {
                item.setComboComponents(findComboComponents(item.getItemId()));
            }
        }
        return items;
    }

    public Optional<FBItem> findById(int id) {
        String sql = getSelectSql() + " WHERE FoodBeverageItemID = ?";
        List<FBItem> list = jdbcTemplate.query(sql, ps -> ps.setInt(1, id), rowMapper);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        FBItem item = list.get(0);
        if (item.isCombo()) {
            item.setComboComponents(findComboComponents(item.getItemId()));
        }
        return Optional.of(item);
    }

    public Optional<FBItem> findByCode(String code) {
        checkColumns();
        if (!hasItemCode) {
            if (code != null && code.trim().toUpperCase().startsWith("FB-")) {
                try {
                    int id = Integer.parseInt(code.trim().substring(3));
                    return findById(id);
                } catch (NumberFormatException ignored) {}
            }
            String sql = getSelectSql() + " WHERE [Name] = ?";
            List<FBItem> list = jdbcTemplate.query(sql, ps -> ps.setString(1, code), rowMapper);
            if (!list.isEmpty()) {
                FBItem item = list.get(0);
                if (item.isCombo()) {
                    item.setComboComponents(findComboComponents(item.getItemId()));
                }
                return Optional.of(item);
            }
            return Optional.empty();
        }

        String sql = getSelectSql() + " WHERE ItemCode = ?";
        List<FBItem> list = jdbcTemplate.query(sql, ps -> ps.setString(1, code), rowMapper);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        FBItem item = list.get(0);
        if (item.isCombo()) {
            item.setComboComponents(findComboComponents(item.getItemId()));
        }
        return Optional.of(item);
    }

    public int insert(FBItemForm form) {
        checkColumns();
        List<String> cols = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (hasItemCode) {
            cols.add("ItemCode");
            placeholders.add("?");
            values.add(form.getItemCode() != null ? form.getItemCode().trim() : "FB-001");
        }
        cols.add("[Name]");
        placeholders.add("?");
        values.add(form.getItemName() != null ? form.getItemName().trim() : "");

        cols.add("ItemType");
        placeholders.add("?");
        values.add(form.getCategory());

        cols.add("[Description]");
        placeholders.add("?");
        values.add(form.getDescription());

        cols.add("Price");
        placeholders.add("?");
        values.add(form.getSellingPrice());

        if (hasCostPrice) {
            cols.add("CostPrice");
            placeholders.add("?");
            values.add(form.getCostPrice() != null ? form.getCostPrice() : BigDecimal.ZERO);
        }

        cols.add("ImageURL");
        placeholders.add("?");
        values.add(form.getImage());

        cols.add("[Status]");
        placeholders.add("?");
        values.add(form.getStatus() != null ? form.getStatus() : "Active");

        if (hasCreatedAt) {
            cols.add("CreatedAt");
            placeholders.add("SYSUTCDATETIME()");
        }

        String sql = "INSERT INTO dbo.FoodBeverageItem (" + String.join(", ", cols) + ") VALUES (" + String.join(", ", placeholders) + ")";
        return jdbcTemplate.update(sql, values.toArray());
    }

    public int update(int id, FBItemForm form) {
        checkColumns();
        List<String> sets = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (hasItemCode) {
            sets.add("ItemCode = ?");
            values.add(form.getItemCode() != null ? form.getItemCode().trim() : "");
        }
        sets.add("[Name] = ?");
        values.add(form.getItemName() != null ? form.getItemName().trim() : "");

        sets.add("ItemType = ?");
        values.add(form.getCategory());

        sets.add("[Description] = ?");
        values.add(form.getDescription());

        sets.add("Price = ?");
        values.add(form.getSellingPrice());

        if (hasCostPrice) {
            sets.add("CostPrice = ?");
            values.add(form.getCostPrice() != null ? form.getCostPrice() : BigDecimal.ZERO);
        }

        sets.add("ImageURL = ?");
        values.add(form.getImage());

        sets.add("[Status] = ?");
        values.add(form.getStatus());

        if (hasUpdatedAt) {
            sets.add("UpdatedAt = SYSUTCDATETIME()");
        }

        values.add(id);
        String sql = "UPDATE dbo.FoodBeverageItem SET " + String.join(", ", sets) + " WHERE FoodBeverageItemID = ?";
        return jdbcTemplate.update(sql, values.toArray());
    }

    public int updatePrice(int id, BigDecimal sellingPrice) {
        checkColumns();
        String sql = hasUpdatedAt
                ? "UPDATE dbo.FoodBeverageItem SET Price = ?, UpdatedAt = SYSUTCDATETIME() WHERE FoodBeverageItemID = ?"
                : "UPDATE dbo.FoodBeverageItem SET Price = ? WHERE FoodBeverageItemID = ?";
        return jdbcTemplate.update(sql, sellingPrice, id);
    }

    public int updateStatus(int id, String status) {
        checkColumns();
        String sql = hasUpdatedAt
                ? "UPDATE dbo.FoodBeverageItem SET [Status] = ?, UpdatedAt = SYSUTCDATETIME() WHERE FoodBeverageItemID = ?"
                : "UPDATE dbo.FoodBeverageItem SET [Status] = ? WHERE FoodBeverageItemID = ?";
        return jdbcTemplate.update(sql, status, id);
    }

    public List<FBItem.ComboComponent> findComboComponents(int comboId) {
        checkColumns();
        if (!hasComboTable) {
            return Collections.emptyList();
        }
        try {
            String sql = "SELECT ci.ComponentItemID, f.[Name] AS ItemName, ci.Quantity, f.[Status] " +
                    "FROM dbo.ComboItem ci " +
                    "JOIN dbo.FoodBeverageItem f ON ci.ComponentItemID = f.FoodBeverageItemID " +
                    "WHERE ci.ComboID = ?";
            return jdbcTemplate.query(sql, (rs, rowNum) -> new FBItem.ComboComponent(
                    rs.getInt("ComponentItemID"),
                    rs.getString("ItemName"),
                    rs.getInt("Quantity"),
                    rs.getString("Status")
            ), comboId);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public void replaceComboComponents(int comboId, List<Integer> componentIds, List<Integer> quantities) {
        checkColumns();
        if (!hasComboTable) {
            return;
        }
        try {
            jdbcTemplate.update("DELETE FROM dbo.ComboItem WHERE ComboID = ?", comboId);
            if (componentIds != null && quantities != null) {
                String insertSql = "INSERT INTO dbo.ComboItem (ComboID, ComponentItemID, Quantity, CreatedAt) VALUES (?, ?, ?, SYSUTCDATETIME())";
                for (int i = 0; i < componentIds.size(); i++) {
                    if (i < quantities.size() && componentIds.get(i) != null && quantities.get(i) != null && quantities.get(i) > 0) {
                        jdbcTemplate.update(insertSql, comboId, componentIds.get(i), quantities.get(i));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public boolean isReferencedInOrders(int itemId) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dbo.BookingFoodBeverage WHERE FoodBeverageItemID = ?", Integer.class, itemId);
            if (count != null && count > 0) return true;
        } catch (Exception ignored) {}
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM BookingFB WHERE ItemID = ?", Integer.class, itemId);
            if (count != null && count > 0) return true;
        } catch (Exception ignored) {}
        return false;
    }

    public boolean isComponentOfAnyCombo(int itemId) {
        checkColumns();
        if (!hasComboTable) {
            return false;
        }
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dbo.ComboItem WHERE ComponentItemID = ?", Integer.class, itemId);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public int delete(int id) {
        checkColumns();
        if (hasComboTable) {
            try {
                jdbcTemplate.update("DELETE FROM dbo.ComboItem WHERE ComboID = ?", id);
            } catch (Exception ignored) {}
        }
        return jdbcTemplate.update("DELETE FROM dbo.FoodBeverageItem WHERE FoodBeverageItemID = ?", id);
    }
}
