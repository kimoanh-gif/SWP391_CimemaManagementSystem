package com.cinema.controller;

import com.cinema.exception.InvalidFBItemException;
import com.cinema.exception.FBItemNotFoundException;
import com.cinema.model.FBItem;
import com.cinema.model.FBItemForm;
import com.cinema.service.FBItemService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for Screen 61: Food & Beverage Management (Iteration 2 / UC-32).
 */
@Controller
@RequestMapping("/manager/fb")
public class FBItemController {

    private final FBItemService fbItemService;

    public FBItemController(FBItemService fbItemService) {
        this.fbItemService = fbItemService;
    }

    @GetMapping
    public String viewFBItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String category,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<FBItem> items = fbItemService.getAllItems(keyword, category, status);
        List<FBItem> singleItems = fbItemService.getAllItems(null, null, "Active");

        long totalCount = items.size();
        long comboCount = items.stream().filter(i -> "Combo".equalsIgnoreCase(i.getCategory())).count();
        long foodCount = items.stream().filter(i -> "Food".equalsIgnoreCase(i.getCategory()) || "Snack".equalsIgnoreCase(i.getCategory())).count();
        long drinkCount = items.stream().filter(i -> "Drink".equalsIgnoreCase(i.getCategory())).count();
        long activeCount = items.stream().filter(FBItem::isActive).count();
        long inactiveCount = items.stream().filter(i -> !i.isActive()).count();

        model.addAttribute("items", items);
        model.addAttribute("singleItems", singleItems);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);

        model.addAttribute("totalCount", totalCount);
        model.addAttribute("comboCount", comboCount);
        model.addAttribute("foodCount", foodCount);
        model.addAttribute("drinkCount", drinkCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);

        if (!model.containsAttribute("fbForm")) {
            model.addAttribute("fbForm", new FBItemForm());
        }

        return "manager/fb";
    }

    @PostMapping("/save")
    public String saveFBItem(
            @Valid @ModelAttribute("fbForm") FBItemForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.fbForm", bindingResult);
            redirectAttributes.addFlashAttribute("fbForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin sản phẩm F&B không hợp lệ. Vui lòng kiểm tra lại.");
            return "redirect:/manager/fb";
        }

        try {
            if (form.getItemId() == null || form.getItemId() <= 0) {
                fbItemService.createItem(form);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm F&B mới '" + form.getItemName() + "' thành công!");
            } else {
                fbItemService.updateItem(form.getItemId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm F&B '" + form.getItemName() + "' thành công!");
            }
        } catch (InvalidFBItemException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("fbForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("fbForm", form);
        }

        return "redirect:/manager/fb";
    }

    @PostMapping("/{id}/price")
    public String updatePrice(
            @PathVariable int id,
            @RequestParam BigDecimal sellingPrice,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            FBItem item = fbItemService.updatePrice(id, sellingPrice);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật giá bán món '" + item.getItemName() + "' thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật giá: " + ex.getMessage());
        }

        return "redirect:/manager/fb";
    }

    @PostMapping("/{id}/status")
    public String toggleStatus(
            @PathVariable int id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            FBItem item = fbItemService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đổi trạng thái món '" + item.getItemName() + "' sang " + status + " thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đổi trạng thái: " + ex.getMessage());
        }

        return "redirect:/manager/fb";
    }
}
