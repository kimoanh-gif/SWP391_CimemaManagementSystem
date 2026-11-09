package com.cinema.controller;

import com.cinema.exception.InvalidFBItemException;
import com.cinema.model.FBItem;
import com.cinema.dto.FBItemForm;
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
import java.util.Collections;
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
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "8") int size,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<FBItem> allCatalog = fbItemService.getAllItems(null, null, null);
        List<FBItem> items = fbItemService.getAllItems(keyword, category, status);
        List<FBItem> singleItems = fbItemService.getAllItems(null, null, "Active");

        long totalCount = allCatalog.size();
        long comboCount = allCatalog.stream().filter(i -> "Combo".equalsIgnoreCase(i.getCategory())).count();
        long foodCount = allCatalog.stream().filter(i -> "Food".equalsIgnoreCase(i.getCategory()) || "Snack".equalsIgnoreCase(i.getCategory())).count();
        long drinkCount = allCatalog.stream().filter(i -> "Drink".equalsIgnoreCase(i.getCategory())).count();
        long activeCount = allCatalog.stream().filter(FBItem::isActive).count();
        long inactiveCount = allCatalog.stream().filter(i -> !i.isActive()).count();

        // Pagination calculation
        if (size <= 0) size = 8;
        int totalFiltered = items.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalFiltered / size));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * size;
        int end = Math.min(start + size, totalFiltered);
        List<FBItem> pagedItems = totalFiltered > 0 ? items.subList(start, end) : Collections.emptyList();

        model.addAttribute("items", pagedItems);
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

        // Pagination attributes
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalFilteredCount", totalFiltered);
        model.addAttribute("startItem", totalFiltered == 0 ? 0 : start + 1);
        model.addAttribute("endItem", end);

        if (!model.containsAttribute("fbForm")) {
            model.addAttribute("fbForm", new FBItemForm());
        }

        return "manager/fb";
    }

    @PostMapping("/save")
    public String saveFBItem(
            @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
            @Valid @ModelAttribute("fbForm") FBItemForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (imageFile != null && !imageFile.isEmpty()) {
            if (imageFile.getSize() > 5 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("errorMessage", "Image size exceeds the 5 MB limit.");
                redirectAttributes.addFlashAttribute("fbForm", form);
                return "redirect:/manager/fb";
            }
            try {
                java.nio.file.Path directory = java.nio.file.Paths.get("uploads", "fb").toAbsolutePath().normalize();
                java.nio.file.Files.createDirectories(directory);
                String contentType = imageFile.getContentType();
                java.util.Map<String, String> allowedExtensions = java.util.Map.of(
                        "image/jpeg", ".jpg",
                        "image/png", ".png",
                        "image/webp", ".webp",
                        "image/gif", ".gif"
                );
                String ext = contentType != null ? allowedExtensions.get(contentType.toLowerCase()) : ".jpg";
                if (ext == null) ext = ".jpg";
                String fileName = "fb-" + java.util.UUID.randomUUID() + ext;
                java.nio.file.Path targetPath = directory.resolve(fileName).normalize();
                if (targetPath.startsWith(directory)) {
                    java.nio.file.Files.copy(imageFile.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    form.setImage("/uploads/fb/" + fileName);
                }
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cannot save uploaded image file: " + ex.getMessage());
                redirectAttributes.addFlashAttribute("fbForm", form);
                return "redirect:/manager/fb";
            }
        }

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().stream()
                    .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                    .filter(msg -> msg != null && !msg.isBlank())
                    .findFirst()
                    .orElse("Invalid F&B product information. Please check again.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.fbForm", bindingResult);
            redirectAttributes.addFlashAttribute("fbForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/manager/fb";
        }

        try {
            if (form.getItemId() == null || form.getItemId() <= 0) {
                fbItemService.createItem(form);
                redirectAttributes.addFlashAttribute("successMessage", "F&B product '" + form.getItemName() + "' created successfully!");
            } else {
                fbItemService.updateItem(form.getItemId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "F&B product '" + form.getItemName() + "' updated successfully!");
            }
        } catch (InvalidFBItemException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("fbForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
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
                    "Selling price for '" + item.getItemName() + "' updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating price: " + ex.getMessage());
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
                    "Status of '" + item.getItemName() + "' changed to " + status + " successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error changing status: " + ex.getMessage());
        }

        return "redirect:/manager/fb";
    }

    @PostMapping("/{id}/delete")
    public String deleteFBItem(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            FBItem item = fbItemService.getItemById(id);
            fbItemService.deleteItem(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + item.getItemName() + "' deleted successfully!");
        } catch (InvalidFBItemException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + ex.getMessage());
        }

        return "redirect:/manager/fb";
    }
}
