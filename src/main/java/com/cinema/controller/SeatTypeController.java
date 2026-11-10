package com.cinema.controller;

import com.cinema.model.SeatType;
import com.cinema.dto.SeatTypeForm;
import com.cinema.service.TicketPricingService;
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
 * Controller for Screen 57: Seat Type Management (UC-28 / SWP391 Iteration 2).
 */
@Controller
@RequestMapping("/manager/seat-types")
public class SeatTypeController {

    private final TicketPricingService pricingService;

    public SeatTypeController(TicketPricingService pricingService) {
        this.pricingService = pricingService;
    }

    public String listSeatTypes(
            String status,
            Model model,
            HttpServletRequest request
    ) {
        return listSeatTypes(status, 1, 10, model, request);
    }

    @GetMapping
    public String listSeatTypes(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<SeatType> seatTypes = pricingService.getAllSeatTypes(status);
        long totalCount = seatTypes.size();
        long activeCount = seatTypes.stream().filter(SeatType::isActive).count();

        if (size <= 0) size = 10;
        int totalFiltered = seatTypes.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalFiltered / size));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * size;
        int end = Math.min(start + size, totalFiltered);
        List<SeatType> pagedSeatTypes = totalFiltered > 0 ? seatTypes.subList(start, end) : java.util.Collections.emptyList();

        model.addAttribute("seatTypes", pagedSeatTypes);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("selectedStatus", status);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalFilteredCount", totalFiltered);
        model.addAttribute("startItem", totalFiltered == 0 ? 0 : start + 1);
        model.addAttribute("endItem", end);

        if (!model.containsAttribute("seatTypeForm")) {
            model.addAttribute("seatTypeForm", new SeatTypeForm());
        }

        return "manager/seat-types";
    }

    @PostMapping("/save")
    public String saveSeatType(
            @Valid @ModelAttribute("seatTypeForm") SeatTypeForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(org.springframework.validation.FieldError::getDefaultMessage)
                    .filter(msg -> msg != null && !msg.isBlank())
                    .findFirst()
                    .orElse("Invalid seat type data.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.seatTypeForm", bindingResult);
            redirectAttributes.addFlashAttribute("seatTypeForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/manager/seat-types";
        }

        try {
            pricingService.createSeatType(form);
            redirectAttributes.addFlashAttribute("successMessage", "Seat type '" + form.getTypeName() + "' created successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("seatTypeForm", form);
        }

        return "redirect:/manager/seat-types";
    }

    @PostMapping("/multiplier")
    public String updateMultiplier(
            @RequestParam int seatTypeId,
            @RequestParam BigDecimal multiplier,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            SeatType st = pricingService.updateSeatTypeMultiplier(seatTypeId, multiplier);
            redirectAttributes.addFlashAttribute("successMessage", "Price multiplier updated for seat type '" + st.getTypeName() + "'!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return "redirect:/manager/seat-types";
    }

    @PostMapping("/update/{id}")
    public String updateSeatType(
            @PathVariable int id,
            @Valid @ModelAttribute("seatTypeForm") SeatTypeForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(org.springframework.validation.FieldError::getDefaultMessage)
                    .filter(msg -> msg != null && !msg.isBlank())
                    .findFirst()
                    .orElse("Invalid seat type data.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.seatTypeForm", bindingResult);
            redirectAttributes.addFlashAttribute("seatTypeForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/manager/seat-types";
        }

        try {
            pricingService.updateSeatType(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Seat type '" + form.getTypeName() + "' updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return "redirect:/manager/seat-types";
    }

    @PostMapping("/status/{id}")
    public String toggleStatus(
            @PathVariable int id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            SeatType st = pricingService.updateSeatTypeStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Changed status of seat type '" + st.getTypeName() + "' to " + status + "!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return "redirect:/manager/seat-types";
    }

    @PostMapping("/delete/{id}")
    public String deleteSeatType(
            @PathVariable int id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        try {
            SeatType st = pricingService.getSeatTypeById(id);
            String name = st.getTypeName();
            pricingService.deleteSeatType(id);
            redirectAttributes.addFlashAttribute("successMessage", "Seat type '" + name + "' deleted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return "redirect:/manager/seat-types";
    }
}
