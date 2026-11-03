package com.cinema.controller;

import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.Room;
import com.cinema.dto.RoomForm;
import com.cinema.service.RoomService;
import com.cinema.util.ManagerSecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;

/**
 * MVC controller for Manager UI – Screen 56 – Cinema Room Management.
 */
@Controller
@RequestMapping("/manager/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       Model model,
                       jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Room> rooms = roomService.getAll(keyword, status);
        model.addAttribute("rooms", rooms);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "manager/rooms"; // Thymeleaf template
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("roomForm") RoomForm form,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirectAttributes,
                       jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        if (result.hasErrors()) {
            String errorMsg = result.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Invalid input data");
            model.addAttribute("rooms", roomService.getAll(null, null));
            model.addAttribute("errorMessage", errorMsg);
            return "manager/rooms"; // redisplay list with error and required model attributes
        }
        try {
            if (form.getRoomId() == null) {
                roomService.create(form);
                redirectAttributes.addFlashAttribute("successMessage", "Screening room created successfully");
            } else {
                roomService.update(form);
                redirectAttributes.addFlashAttribute("successMessage", "Screening room updated successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/rooms"; // PRG pattern
    }

    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable int id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes, jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        try {
            roomService.deactivate(id);
            redirectAttributes.addFlashAttribute("successMessage", "Screening room has been set to maintenance");
        } catch (RoomNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/rooms";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable int id, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes, jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        try {
            Room room = roomService.getById(id);
            String name = room.getRoomName();
            roomService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Screening room '" + name + "' deleted successfully.");
        } catch (RoomNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/rooms";
    }
}
