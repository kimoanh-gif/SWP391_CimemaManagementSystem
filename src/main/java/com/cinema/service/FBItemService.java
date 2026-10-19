package com.cinema.service;

import com.cinema.dao.FBItemDao;
import com.cinema.exception.FBItemNotFoundException;
import com.cinema.exception.InvalidFBItemException;
import com.cinema.model.FBItem;
import com.cinema.model.FBItemForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for Food & Beverage Management (Screen 61 / UC-32).
 * Enforces business rules:
 * - BR-15: Optional F&B in bookings.
 * - BR-51: Historical booking price snapshots remain unaffected by subsequent price adjustments.
 * - Validates prices (non-negative), SKU uniqueness, and protects against physical deletion when referenced.
 */
@Service
public class FBItemService {

    private final FBItemDao fbItemDao;

    public FBItemService(FBItemDao fbItemDao) {
        this.fbItemDao = fbItemDao;
    }

    public List<FBItem> getAllItems(String keyword, String category, String status) {
        return fbItemDao.findAll(keyword, category, status);
    }

    public FBItem getItemById(int id) {
        return fbItemDao.findById(id)
                .orElseThrow(() -> new FBItemNotFoundException("Không tìm thấy sản phẩm F&B với ID: " + id));
    }

    @Transactional
    public FBItem createItem(FBItemForm form) {
        validateForm(form, null);
        validateComboComponents(form, null);

        fbItemDao.insert(form);
        FBItem created = fbItemDao.findByCode(form.getItemCode().trim())
                .orElseThrow(() -> new IllegalStateException("Lỗi lưu trữ sản phẩm F&B"));

        if ("Combo".equalsIgnoreCase(form.getCategory())
                && form.getComponentItemIds() != null && !form.getComponentItemIds().isEmpty()) {
            fbItemDao.replaceComboComponents(created.getItemId(), form.getComponentItemIds(), form.getComponentQuantities());
        }

        return getItemById(created.getItemId());
    }

    @Transactional
    public FBItem updateItem(int id, FBItemForm form) {
        FBItem existing = getItemById(id);
        validateForm(form, id);
        validateComboComponents(form, id);

        fbItemDao.update(id, form);

        if ("Combo".equalsIgnoreCase(form.getCategory())) {
            fbItemDao.replaceComboComponents(id, form.getComponentItemIds(), form.getComponentQuantities());
        } else {
            // If changed from Combo to Single item, clear any combo components
            fbItemDao.replaceComboComponents(id, null, null);
        }

        return getItemById(id);
    }

    @Transactional
    public FBItem updatePrice(int id, BigDecimal sellingPrice) {
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidFBItemException("Giá bán sản phẩm không được để trống hoặc là số âm");
        }
        getItemById(id);
        fbItemDao.updatePrice(id, sellingPrice);
        return getItemById(id);
    }

    @Transactional
    public FBItem updateStatus(int id, String status) {
        getItemById(id);
        fbItemDao.updateStatus(id, status);
        return getItemById(id);
    }

    @Transactional
    public FBItem deactivateItem(int id) {
        getItemById(id);
        fbItemDao.updateStatus(id, "Inactive");
        return getItemById(id);
    }

    @Transactional
    public void deleteItem(int id) {
        FBItem item = getItemById(id);

        // 1. Check if item has been ordered previously
        if (fbItemDao.isReferencedInOrders(id)) {
            // Preserve historical transaction records (BR-51) by soft-deactivating instead of hard delete
            fbItemDao.updateStatus(id, "Inactive");
            throw new InvalidFBItemException("Món '" + item.getItemName() + "' đã phát sinh trong đơn hàng của khách, không thể xóa hoàn toàn khỏi cơ sở dữ liệu. Hệ thống đã chuyển món sang trạng thái 'Ngừng Bán' (Inactive).");
        }

        // 2. Check if item is currently part of any other combo
        if (fbItemDao.isComponentOfAnyCombo(id)) {
            throw new InvalidFBItemException("Món '" + item.getItemName() + "' đang là thành phần của một gói Combo khác. Vui lòng gỡ món khỏi Combo trước khi xóa.");
        }

        // 3. Delete item
        fbItemDao.delete(id);
    }

    public boolean isItemAvailableForSale(int id) {
        FBItem item = getItemById(id);
        return item.isAvailableForSale();
    }

    private void validateForm(FBItemForm form, Integer currentId) {
        if (form.getItemCode() == null || form.getItemCode().isBlank()) {
            throw new InvalidFBItemException("Mã sản phẩm / SKU không được để trống");
        }
        if (form.getItemName() == null || form.getItemName().isBlank()) {
            throw new InvalidFBItemException("Tên sản phẩm không được để trống");
        }
        if (form.getSellingPrice() == null || form.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidFBItemException("Giá bán không được để trống hoặc là số âm");
        }
        if (form.getCostPrice() == null || form.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidFBItemException("Giá vốn không được để trống hoặc là số âm");
        }

        fbItemDao.findByCode(form.getItemCode().trim()).ifPresent(item -> {
            if (currentId == null || !item.getItemId().equals(currentId)) {
                throw new InvalidFBItemException("Mã sản phẩm / SKU '" + form.getItemCode() + "' đã tồn tại trong hệ thống");
            }
        });
    }

    private void validateComboComponents(FBItemForm form, Integer currentId) {
        if (!"Combo".equalsIgnoreCase(form.getCategory())) {
            return;
        }

        List<Integer> componentIds = form.getComponentItemIds();
        List<Integer> quantities = form.getComponentQuantities();

        if (componentIds == null || componentIds.isEmpty()) {
            // If updating an existing combo and form did not supply component list, preserve existing or enforce minimum
            if (currentId != null) {
                List<FBItem.ComboComponent> existingComponents = fbItemDao.findComboComponents(currentId);
                if (existingComponents != null && !existingComponents.isEmpty()) {
                    return; // Existing components retained
                }
            }
            throw new InvalidFBItemException("Gói Combo phải có ít nhất một mặt hàng thành phần hợp lệ.");
        }

        boolean hasValidComponent = false;
        for (int i = 0; i < componentIds.size(); i++) {
            Integer compId = componentIds.get(i);
            if (compId == null) {
                continue;
            }

            if (currentId != null && compId.equals(currentId)) {
                throw new InvalidFBItemException("Gói Combo không thể chứa chính nó.");
            }

            Integer qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : 1;
            if (qty == null || qty <= 0) {
                throw new InvalidFBItemException("Số lượng thành phần trong Combo phải lớn hơn 0.");
            }

            FBItem comp = fbItemDao.findById(compId)
                    .orElseThrow(() -> new InvalidFBItemException("Mặt hàng thành phần ID " + compId + " không tồn tại trong hệ thống."));

            if ("Combo".equalsIgnoreCase(comp.getCategory())) {
                throw new InvalidFBItemException("Combo chỉ có thể chứa các mặt hàng đơn lẻ (Food, Drink, Snack), không thể lồng Combo vào Combo.");
            }

            if (!comp.isActive()) {
                throw new InvalidFBItemException("Không thể đưa mặt hàng đang ngừng bán '" + comp.getItemName() + "' vào Combo.");
            }

            hasValidComponent = true;
        }

        if (!hasValidComponent) {
            throw new InvalidFBItemException("Gói Combo phải có ít nhất một mặt hàng thành phần hợp lệ.");
        }
    }
}
