package com.cinema.controller.api;

import com.cinema.dto.ApiResponse;
import com.cinema.model.ManagerDashboardStats;
import com.cinema.service.ManagerDashboardService;
import com.cinema.util.ManagerSecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API Controller for Screen 53: Manager Dashboard (UC-25).
 * Exposes live aggregated operational metrics.
 */
@RestController
@RequestMapping("/api/manager/dashboard")
public class ManagerDashboardApiController {

    private final ManagerDashboardService dashboardService;

    public ManagerDashboardApiController(ManagerDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ManagerDashboardStats>> getStats(HttpServletRequest request) {
        ManagerSecurityUtils.verifyManagerAccess(request);
        ManagerDashboardStats stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok("Lấy dữ liệu thống kê tổng quan thành công", stats));
    }
}
