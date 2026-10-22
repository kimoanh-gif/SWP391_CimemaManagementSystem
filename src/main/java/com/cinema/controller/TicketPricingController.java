package com.cinema.controller;

import com.cinema.exception.InvalidPricingConfigurationException;
import com.cinema.exception.SeatTypeNotFoundException;
import com.cinema.exception.TicketPricingNotFoundException;
import com.cinema.model.SeatType;
import com.cinema.dto.SeatTypeForm;
import com.cinema.model.TicketPricing;
import com.cinema.dto.TicketPricingForm;
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
import java.util.Map;

/**
 * Controller for Screen 60: Ticket Pricing Management (Iteration 2 / UC-31).
 * Handles the manager Thymeleaf template and form submissions.
 */
@Controller
@RequestMapping("/manager/pricing")
public class TicketPricingController {

    private final TicketPricingService pricingService;

    public TicketPricingController(TicketPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping
    public String viewPricingScreen(
            @RequestParam(required = false, defaultValue = "ALL") String dayType,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<TicketPricing> rules = pricingService.getAllPricingRules(dayType, status);
        List<SeatType> seatTypes = pricingService.getAllSeatTypes("Active");
        Map<Integer, Map<Integer, BigDecimal>> matrix = pricingService.generatePricingMatrix();
        BigDecimal atp = pricingService.getAverageTicketPrice();

        // Base 2D price (standard weekday 12:00-17:00)
        BigDecimal base2D = rules.stream()
                .filter(r -> "WEEKDAY".equalsIgnoreCase(r.getDayType()) && "STANDARD".equalsIgnoreCase(r.getTimeSlot()))
                .map(TicketPricing::getBasePrice)
                .findFirst()
                .orElse(new BigDecimal("85000"));

        long activeRulesCount = rules.stream().filter(TicketPricing::isActive).count();

        model.addAttribute("rules", rules);
        model.addAttribute("seatTypes", seatTypes);
        model.addAttribute("matrix", matrix);
        model.addAttribute("atp", atp);
        model.addAttribute("base2D", base2D);
        model.addAttribute("activeRulesCount", activeRulesCount);
        model.addAttribute("selectedDayType", dayType);
        model.addAttribute("selectedStatus", status);

        if (!model.containsAttribute("pricingForm")) {
            model.addAttribute("pricingForm", new TicketPricingForm());
        }
        if (!model.containsAttribute("seatTypeForm")) {
            model.addAttribute("seatTypeForm", new SeatTypeForm());
        }

        return "manager/pricing";
    }

    @PostMapping("/save")
    public String savePricingRule(
            @Valid @ModelAttribute("pricingForm") TicketPricingForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.pricingForm", bindingResult);
            redirectAttributes.addFlashAttribute("pricingForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin quy tắc giá không hợp lệ. Vui lòng kiểm tra lại.");
            return "redirect:/manager/pricing";
        }

        try {
            if (form.getPricingId() == null || form.getPricingId() <= 0) {
                pricingService.createPricing(form);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm quy tắc định giá mới '" + form.getRuleName() + "' thành công!");
            } else {
                pricingService.updatePricing(form.getPricingId(), form);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật quy tắc định giá '" + form.getRuleName() + "' thành công!");
            }
        } catch (InvalidPricingConfigurationException | com.cinema.exception.DuplicatePricingRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("pricingForm", form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu quy tắc: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("pricingForm", form);
        }

        return "redirect:/manager/pricing";
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
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật hệ số giá cho loại ghế '" + st.getTypeName() + "' thành " + multiplier + " thành công!");
        } catch (InvalidPricingConfigurationException | SeatTypeNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật hệ số giá: " + ex.getMessage());
        }

        return "redirect:/manager/pricing";
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
            TicketPricing tp = pricingService.updatePricingStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái quy tắc '" + tp.getRuleName() + "' thành " + status + " thành công!");
        } catch (com.cinema.exception.DuplicatePricingRuleException | InvalidPricingConfigurationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đổi trạng thái: " + ex.getMessage());
        }

        return "redirect:/manager/pricing";
    }
}
