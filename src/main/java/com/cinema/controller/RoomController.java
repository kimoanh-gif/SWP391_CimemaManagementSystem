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

    public String list(String keyword,
                       String status,
                       Model model,
                       jakarta.servlet.http.HttpServletRequest request) {
        return list(keyword, status, 1, 9, model, request);
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false, defaultValue = "1") int page,
                       @RequestParam(required = false, defaultValue = "9") int size,
                       Model model,
                       jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        List<Room> rooms = roomService.getAll(keyword, status);
        populatePaginatedRoomModel(rooms, keyword, status, page, size, model);
        return "manager/rooms"; // Thymeleaf template
    }

    private void populatePaginatedRoomModel(List<Room> rooms, String keyword, String status, int page, int size, Model model) {
        if (size <= 0) size = 9;
        int totalFiltered = rooms.size();
        long activeRoomCount = rooms.stream()
                .filter(r -> "Ready".equalsIgnoreCase(r.getStatus()) || "Screening".equalsIgnoreCase(r.getStatus()))
                .count();
        long maintenanceRoomCount = rooms.stream()
                .filter(r -> "Maintenance".equalsIgnoreCase(r.getStatus()) || "Cleaning".equalsIgnoreCase(r.getStatus()))
                .count();
        long totalRoomCapacity = rooms.stream()
                .mapToLong(r -> r.getCapacity() != null ? r.getCapacity() : 0)
                .sum();

        int totalPages = Math.max(1, (int) Math.ceil((double) totalFiltered / size));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * size;
        int end = Math.min(start + size, totalFiltered);
        List<Room> pagedRooms = totalFiltered > 0 ? rooms.subList(start, end) : java.util.Collections.emptyList();

        model.addAttribute("rooms", pagedRooms);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("status", status != null ? status : "");

        model.addAttribute("activeRoomCount", activeRoomCount);
        model.addAttribute("maintenanceRoomCount", maintenanceRoomCount);
        model.addAttribute("totalRoomCapacity", totalRoomCapacity);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalFilteredCount", totalFiltered);
        model.addAttribute("startItem", totalFiltered == 0 ? 0 : start + 1);
        model.addAttribute("endItem", end);
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
            populatePaginatedRoomModel(roomService.getAll(null, null), null, null, 1, 9, model);
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
