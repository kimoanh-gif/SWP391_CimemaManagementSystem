package com.cinema.controller;

import com.cinema.exception.RoomNotFoundException;
import com.cinema.model.Room;
import com.cinema.dto.RoomForm;
import com.cinema.service.RoomService;
import com.cinema.util.ManagerSecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
                       jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", "Dữ liệu nhập không hợp lệ");
            return "manager/rooms"; // redisplay list with error
        }
        try {
            if (form.getRoomId() == null) {
                roomService.create(form);
                model.addAttribute("successMessage", "Tạo phòng thành công");
            } else {
                roomService.update(form);
                model.addAttribute("successMessage", "Cập nhật phòng thành công");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/rooms"; // PRG pattern
    }

    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable int id, Model model, jakarta.servlet.http.HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        try {
            roomService.deactivate(id);
            model.addAttribute("successMessage", "Đã đưa phòng vào bảo trì");
        } catch (RoomNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/rooms";
    }
}
