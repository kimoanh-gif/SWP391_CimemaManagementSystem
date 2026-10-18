package com.cinema.dao;

import com.cinema.model.FBItem;
import com.cinema.model.FBItemForm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for FBItem and ComboItem tables (Screen 61 / UC-32).
 */
@Repository
public class FBItemDao {

    private final JdbcTemplate jdbcTemplate;

    public FBItemDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        StringBuilder sql = new StringBuilder("SELECT * FROM FBItem WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (ItemName LIKE ? OR ItemCode LIKE ? OR Description LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND Category = ? ");
            params.add(category.trim());
        }

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND Status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY ItemID DESC");

        List<FBItem> items = jdbcTemplate.query(sql.toString(), params.toArray(), rowMapper);
        for (FBItem item : items) {
            if (item.isCombo()) {
                item.setComboComponents(findComboComponents(item.getItemId()));
            }
        }
        return items;
    }

    public Optional<FBItem> findById(int id) {
        String sql = "SELECT * FROM FBItem WHERE ItemID = ?";
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
        String sql = "SELECT * FROM FBItem WHERE ItemCode = ?";
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
        String sql = "INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status, CreatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, SYSUTCDATETIME())";
        return jdbcTemplate.update(sql,
                form.getItemCode().trim(),
                form.getItemName().trim(),
                form.getCategory(),
                form.getDescription(),
                form.getSellingPrice(),
                form.getCostPrice() != null ? form.getCostPrice() : BigDecimal.ZERO,
                form.getImage(),
                form.getStatus() != null ? form.getStatus() : "Active"
        );
    }

    public int update(int id, FBItemForm form) {
        String sql = "UPDATE FBItem SET ItemCode = ?, ItemName = ?, Category = ?, Description = ?, " +
                "SellingPrice = ?, CostPrice = ?, Image = ?, Status = ?, UpdatedAt = SYSUTCDATETIME() " +
                "WHERE ItemID = ?";
        return jdbcTemplate.update(sql,
                form.getItemCode().trim(),
                form.getItemName().trim(),
                form.getCategory(),
                form.getDescription(),
                form.getSellingPrice(),
                form.getCostPrice() != null ? form.getCostPrice() : BigDecimal.ZERO,
                form.getImage(),
                form.getStatus(),
                id
        );
    }

    public int updatePrice(int id, BigDecimal sellingPrice) {
        String sql = "UPDATE FBItem SET SellingPrice = ?, UpdatedAt = SYSUTCDATETIME() WHERE ItemID = ?";
        return jdbcTemplate.update(sql, sellingPrice, id);
    }

    public int updateStatus(int id, String status) {
        String sql = "UPDATE FBItem SET Status = ?, UpdatedAt = SYSUTCDATETIME() WHERE ItemID = ?";
        return jdbcTemplate.update(sql, status, id);
    }

    public List<FBItem.ComboComponent> findComboComponents(int comboId) {
        String sql = "SELECT ci.ComponentItemID, f.ItemName, ci.Quantity, f.Status " +
                "FROM ComboItem ci " +
                "JOIN FBItem f ON ci.ComponentItemID = f.ItemID " +
                "WHERE ci.ComboID = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new FBItem.ComboComponent(
                rs.getInt("ComponentItemID"),
                rs.getString("ItemName"),
                rs.getInt("Quantity"),
                rs.getString("Status")
        ), comboId);
    }

    public void replaceComboComponents(int comboId, List<Integer> componentIds, List<Integer> quantities) {
        jdbcTemplate.update("DELETE FROM ComboItem WHERE ComboID = ?", comboId);
        if (componentIds != null && quantities != null) {
            String insertSql = "INSERT INTO ComboItem (ComboID, ComponentItemID, Quantity, CreatedAt) VALUES (?, ?, ?, SYSUTCDATETIME())";
            for (int i = 0; i < componentIds.size(); i++) {
                if (i < quantities.size() && componentIds.get(i) != null && quantities.get(i) != null && quantities.get(i) > 0) {
                    jdbcTemplate.update(insertSql, comboId, componentIds.get(i), quantities.get(i));
                }
            }
        }
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
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ComboItem WHERE ComponentItemID = ?", Integer.class, itemId);
            return count != null && count > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public int delete(int id) {
        jdbcTemplate.update("DELETE FROM ComboItem WHERE ComboID = ?", id);
        return jdbcTemplate.update("DELETE FROM FBItem WHERE ItemID = ?", id);
    }
}
