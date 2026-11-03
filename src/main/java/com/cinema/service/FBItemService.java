package com.cinema.service;

import com.cinema.dao.FBItemDao;
import com.cinema.exception.FBItemNotFoundException;
import com.cinema.exception.InvalidFBItemException;
import com.cinema.model.FBItem;
import com.cinema.dto.FBItemForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for Food & Beverage Management (Screen 61 / UC-32).
 * Enforces business rules:
 * - BR-15: Optional F&B in bookings.
 * - BR-51: Historical booking price snapshots remain unaffected by subsequent
 * price adjustments.
 * - Validates prices (non-negative), SKU uniqueness, and protects against
 * physical deletion when referenced.
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
                .orElseThrow(() -> new FBItemNotFoundException("F&B product not found with ID: " + id));
    }

    @Transactional
    public FBItem createItem(FBItemForm form) {
        validateForm(form, null);
        validateComboComponents(form, null);

        fbItemDao.insert(form);
        FBItem created = fbItemDao.findByCode(form.getItemCode().trim())
                .orElseThrow(() -> new IllegalStateException("Error saving F&B product"));

        if ("Combo".equalsIgnoreCase(form.getCategory())
                && form.getComponentItemIds() != null && !form.getComponentItemIds().isEmpty()) {
            fbItemDao.replaceComboComponents(created.getItemId(), form.getComponentItemIds(),
                    form.getComponentQuantities());
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
            throw new InvalidFBItemException("Selling price cannot be empty or negative");
        }
        getItemById(id);
        fbItemDao.updatePrice(id, sellingPrice);
        return getItemById(id);
    }

    @Transactional
    public FBItem updateStatus(int id, String status) {
        if (status == null
                || (!"Active".equalsIgnoreCase(status.trim()) && !"Inactive".equalsIgnoreCase(status.trim()))) {
            throw new InvalidFBItemException("Status must be Active or Inactive");
        }
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
            // Preserve historical transaction records (BR-51) by soft-deactivating instead
            // of hard delete
            fbItemDao.updateStatus(id, "Inactive");
            throw new InvalidFBItemException("Product '" + item.getItemName()
                    + "' has been ordered in customer bookings and cannot be permanently deleted. The product status has been set to Inactive.");
        }

        // 2. Check if item is currently part of any other combo
        if (fbItemDao.isComponentOfAnyCombo(id)) {
            throw new InvalidFBItemException("Product '" + item.getItemName()
                    + "' is a component of an existing Combo. Please remove it from the Combo before deleting.");
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
            throw new InvalidFBItemException("SKU / Product code cannot be empty");
        }
        if (form.getItemName() == null || form.getItemName().isBlank()) {
            throw new InvalidFBItemException("Product name cannot be empty");
        }
        if (form.getSellingPrice() == null || form.getSellingPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidFBItemException("Selling price cannot be empty or negative");
        }
        if (form.getCostPrice() == null || form.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidFBItemException("Cost price cannot be empty or negative");
        }
        if (form.getCostPrice().compareTo(form.getSellingPrice()) > 0) {
            throw new InvalidFBItemException("Cost price cannot exceed selling price");
        }
        if (form.getCategory() == null || !form.getCategory().trim().matches("(?i)^(Combo|Food|Drink|Snack)$")) {
            throw new InvalidFBItemException("Category must be Combo, Food, Drink or Snack");
        }
        if (form.getStatus() != null && !form.getStatus().isBlank()
                && !"Active".equalsIgnoreCase(form.getStatus().trim())
                && !"Inactive".equalsIgnoreCase(form.getStatus().trim())) {
            throw new InvalidFBItemException("Status must be Active or Inactive");
        }
        if (Boolean.FALSE) {
        }

        fbItemDao.findByCode(form.getItemCode().trim()).ifPresent(item -> {
            if (currentId == null || !item.getItemId().equals(currentId)) {
                throw new InvalidFBItemException(
                        "SKU '" + form.getItemCode() + "' already exists in the system");
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
            // If updating an existing combo and form did not supply component list,
            // preserve existing or enforce minimum
            if (currentId != null) {
                List<FBItem.ComboComponent> existingComponents = fbItemDao.findComboComponents(currentId);
                if (existingComponents != null && !existingComponents.isEmpty()) {
                    return; // Existing components retained
                }
            }
            throw new InvalidFBItemException("Combo must have at least one valid component item.");
        }

        boolean hasValidComponent = false;
        for (int i = 0; i < componentIds.size(); i++) {
            Integer compId = componentIds.get(i);
            if (compId == null) {
                continue;
            }

            if (currentId != null && compId.equals(currentId)) {
                throw new InvalidFBItemException("Combo cannot contain itself.");
            }

            Integer qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : 1;
            if (qty == null || qty <= 0) {
                throw new InvalidFBItemException("Component quantity in combo must be greater than 0.");
            }

            FBItem comp = fbItemDao.findById(compId)
                    .orElseThrow(() -> new InvalidFBItemException(
                            "Component item ID " + compId + " does not exist in the system."));

            if ("Combo".equalsIgnoreCase(comp.getCategory())) {
                throw new InvalidFBItemException(
                        "Combo can only contain single items (Food, Drink, Snack) and cannot nest combos.");
            }

            if (!comp.isActive()) {
                throw new InvalidFBItemException(
                        "Cannot add inactive item '" + comp.getItemName() + "' into Combo.");
            }

            hasValidComponent = true;
        }

        if (!hasValidComponent) {
            throw new InvalidFBItemException("Combo must have at least one valid component item.");
        }
    }
}
