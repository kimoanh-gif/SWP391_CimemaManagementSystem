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

    @GetMapping
    public String listSeatTypes(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<SeatType> seatTypes = pricingService.getAllSeatTypes(status);
        long totalCount = seatTypes.size();
        long activeCount = seatTypes.stream().filter(SeatType::isActive).count();

        model.addAttribute("seatTypes", seatTypes);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("selectedStatus", status);

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
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.seatTypeForm", bindingResult);
            redirectAttributes.addFlashAttribute("seatTypeForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu loại ghế không hợp lệ.");
            return "redirect:/manager/seat-types";
        }

        try {
            pricingService.createSeatType(form);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm loại ghế '" + form.getTypeName() + "' thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
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
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hệ số giá cho loại ghế '" + st.getTypeName() + "'!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
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
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu loại ghế không hợp lệ.");
            return "redirect:/manager/seat-types";
        }

        try {
            pricingService.updateSeatType(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật loại ghế '" + form.getTypeName() + "' thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
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
            redirectAttributes.addFlashAttribute("successMessage", "Đã đổi trạng thái loại ghế '" + st.getTypeName() + "' sang " + status + "!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + ex.getMessage());
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
            pricingService.deleteSeatType(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa loại ghế thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa loại ghế: " + ex.getMessage());
        }

        return "redirect:/manager/seat-types";
    }
}
