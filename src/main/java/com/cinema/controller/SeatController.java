package com.cinema.controller;

import com.cinema.dto.SeatUpdateForm;
import com.cinema.model.Room;
import com.cinema.model.Seat;
import com.cinema.model.SeatType;
import com.cinema.service.RoomService;
import com.cinema.service.SeatService;
import com.cinema.service.TicketPricingService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Screen 58: Seat Management / Seat Layout Management (UC-29).
 */
@Controller
@RequestMapping("/manager/seats")
public class SeatController {

    private final SeatService seatService;
    private final RoomService roomService;
    private final TicketPricingService pricingService;

    public SeatController(SeatService seatService, RoomService roomService, TicketPricingService pricingService) {
        this.seatService = seatService;
        this.roomService = roomService;
        this.pricingService = pricingService;
    }

    @GetMapping
    public String viewSeatLayout(
            @RequestParam(required = false) Integer roomId,
            Model model,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        List<Room> rooms = roomService.getAll(null, null);
        model.addAttribute("rooms", rooms);

        Room selectedRoom = null;
        List<Seat> seats = Collections.emptyList();

        if (roomId != null) {
            selectedRoom = rooms.stream().filter(r -> r.getRoomId().equals(roomId)).findFirst().orElse(null);
        } else if (!rooms.isEmpty()) {
            selectedRoom = rooms.get(0);
        }

        if (selectedRoom != null) {
            seats = seatService.getSeatsByRoomId(selectedRoom.getRoomId());
            model.addAttribute("selectedRoom", selectedRoom);
        }

        List<SeatType> seatTypes = pricingService.getAllSeatTypes("Active");
        model.addAttribute("seatTypes", seatTypes);
        model.addAttribute("seats", seats);

        // Stats
        long totalSeats = seats.size();
        long activeSeats = seats.stream().filter(s -> "Active".equalsIgnoreCase(s.getStatus())).count();
        long maintenanceSeats = seats.stream().filter(s -> "Maintenance".equalsIgnoreCase(s.getStatus())).count();

        model.addAttribute("totalSeats", totalSeats);
        model.addAttribute("activeSeats", activeSeats);
        model.addAttribute("maintenanceSeats", maintenanceSeats);

        Map<String, List<Seat>> seatsByRow = new LinkedHashMap<>();
        for (Seat seat : seats) {
            seatsByRow.computeIfAbsent(seat.getRowLabel(), k -> new ArrayList<>()).add(seat);
        }
        model.addAttribute("seatsByRow", seatsByRow);

        int seatsPerRow = (selectedRoom != null && selectedRoom.getSeatsPerRow() != null && selectedRoom.getSeatsPerRow() > 0)
                ? selectedRoom.getSeatsPerRow()
                : 8;
        int midPoint = seatsPerRow / 2;
        model.addAttribute("midPoint", midPoint);

        return "manager/seats";
    }

    @PostMapping("/generate-default")
    public String generateDefaultLayout(
            @RequestParam int roomId,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        if (roomId <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Invalid room ID.");
            return "redirect:/manager/seats";
        }

        try {
            List<Seat> generated = seatService.generateDefaultLayout(roomId);
            redirectAttributes.addFlashAttribute("successMessage", "Standard seat layout initialized successfully (" + generated.size() + " seats)!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return "redirect:/manager/seats?roomId=" + roomId;
    }

    @PostMapping("/update-seat")
    public String updateSeat(
            @Valid @ModelAttribute("seatUpdateForm") SeatUpdateForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        ManagerSecurityUtils.verifyManagerAccess(request);

        String redirectTarget = (form.getRoomId() != null && form.getRoomId() > 0)
                ? "redirect:/manager/seats?roomId=" + form.getRoomId()
                : "redirect:/manager/seats";

        if (form.getSeatId() == null || form.getSeatId() <= 0 || form.getRoomId() == null || form.getRoomId() <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Seat ID and room ID cannot be empty.");
            return redirectTarget;
        }

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .findFirst()
                    .orElse("Invalid seat update data.");
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + errorMsg);
            return redirectTarget;
        }

        try {
            List<Integer> targetSeatIds = new java.util.ArrayList<>();
            if (form.getSeatIds() != null && !form.getSeatIds().trim().isEmpty()) {
                String[] parts = form.getSeatIds().split(",");
                for (String p : parts) {
                    try {
                        int sid = Integer.parseInt(p.trim());
                        if (sid > 0) targetSeatIds.add(sid);
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (targetSeatIds.isEmpty() && form.getSeatId() != null && form.getSeatId() > 0) {
                targetSeatIds.add(form.getSeatId());
            }

            for (Integer sid : targetSeatIds) {
                seatService.updateSeat(sid, form.getSeatTypeName(), form.getStatus());
            }

            if (targetSeatIds.size() > 1) {
                redirectAttributes.addFlashAttribute("successMessage", "Successfully updated " + targetSeatIds.size() + " seats!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Seat updated successfully!");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return redirectTarget;
    }
}
