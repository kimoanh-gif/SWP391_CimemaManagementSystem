package com.cinema.controller;

import com.cinema.model.MembershipRank;
import com.cinema.model.SystemSetting;
import com.cinema.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for System Configuration Management.
 * All endpoints are prefixed /api/admin/config
 */
@RestController
@RequestMapping("/api/admin/config")
public class SystemConfigApiController {

    @Autowired
    private SystemConfigService service;

    // ==================== SETTINGS ====================

    /**
     * GET /api/admin/config/settings
     * Normal Flow Step 2: Display all system rules and configuration settings.
     * Returns settings grouped by category.
     */
    @GetMapping("/settings")
    public ResponseEntity<?> listSettings() {
        Map<String, List<SystemSetting>> settings = service.getAllSettingsGrouped();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", settings
        ));
    }

    /**
     * GET /api/admin/config/settings/category/{category}
     * Get settings by category (VOUCHER, MEMBERSHIP, BOOKING, SYSTEM).
     */
    @GetMapping("/settings/category/{category}")
    public ResponseEntity<?> getSettingsByCategory(@PathVariable String category) {
        List<SystemSetting> settings = service.getSettingsByCategory(category.toUpperCase());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", settings
        ));
    }

    /**
     * GET /api/admin/config/settings/{id}
     * Get a single setting by ID.
     */
    @GetMapping("/settings/{id}")
    public ResponseEntity<?> getSetting(@PathVariable int id) {
        return service.getSettingByKey(String.valueOf(id))
                .<ResponseEntity<?>>map(setting -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", setting
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "code", "NOT_FOUND",
                        "message", "Setting not found."
                )));
    }

    /**
     * PUT /api/admin/config/settings/{id}
     * Normal Flow Step 5-8: Update a setting value.
     * 
     * Body: { "value": "new value" }
     */
    @PutMapping("/settings/{id}")
    public ResponseEntity<?> updateSetting(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        
        String newValue = body.get("value");
        
        // Get current value for response
        var currentSetting = service.getSettingByKey(String.valueOf(id));
        String oldValue = currentSetting.map(SystemSetting::getValue).orElse("");
        String settingName = currentSetting.map(SystemSetting::getSettingName).orElse("");

        var result = service.updateSetting(id, newValue);
        
        if (result.success()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.message(),
                    "settingKey", result.settingKey(),
                    "oldValue", oldValue,
                    "newValue", newValue
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", result.code(),
                    "message", result.message()
            ));
        }
    }

    /**
     * POST /api/admin/config/settings/batch
     * Batch update multiple settings at once.
     * 
     * Body: { "settings": { "1": "value1", "2": "value2" } }
     */
    @PostMapping("/settings/batch")
    public ResponseEntity<?> batchUpdateSettings(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, String> settingsMap = (Map<String, String>) body.get("settings");
        
        if (settingsMap == null || settingsMap.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", "EMPTY_REQUEST",
                    "message", "No settings provided for update."
            ));
        }

        // Convert string keys to integers
        Map<Integer, String> intKeyedMap = settingsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Integer.parseInt(e.getKey()),
                        Map.Entry::getValue
                ));

        var results = service.updateSettings(intKeyedMap);
        
        int successCount = (int) results.stream().filter(SystemConfigService.OperationResult::success).count();
        int failCount = results.size() - successCount;

        return ResponseEntity.ok(Map.of(
                "success", failCount == 0,
                "message", "Updated " + successCount + " setting(s), " + failCount + " failed.",
                "results", results.stream()
                        .filter(r -> !r.success())
                        .map(r -> Map.of(
                                "code", r.code(),
                                "message", r.message(),
                                "settingKey", r.settingKey()
                        ))
                        .toList()
        ));
    }

    /**
     * POST /api/admin/config/settings/{id}/reset
     * Reset a setting to its default value.
     */
    @PostMapping("/settings/{id}/reset")
    public ResponseEntity<?> resetSetting(@PathVariable int id) {
        var result = service.resetToDefault(id);
        
        return result.success()
                ? ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.message(),
                        "newValue", result.newValue()
                ))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.code(),
                        "message", result.message()
                ));
    }

    // ==================== MEMBERSHIP RANKS ====================

    /**
     * GET /api/admin/config/membership/ranks
     * Get all membership ranks.
     */
    @GetMapping("/membership/ranks")
    public ResponseEntity<?> listRanks() {
        List<MembershipRank> ranks = service.getAllRanks();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ranks
        ));
    }

    /**
     * PUT /api/admin/config/membership/ranks/{id}/threshold
     * Alternative Flow A5: Configure Membership Rank Thresholds.
     * 
     * Body: { "minPoints": 500 }
     */
    @PutMapping("/membership/ranks/{id}/threshold")
    public ResponseEntity<?> updateRankThreshold(
            @PathVariable int id,
            @RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        
        var points = body.get("minPoints");
        if (points == null || !points.isIntegralNumber() || !points.canConvertToInt() || points.intValue() < 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", "INVALID_REQUEST",
                    "message", "minPoints must be a whole number between 0 and 2147483647."
            ));
        }

        var result = service.updateRankThreshold(id, points.intValue());
        
        return result.success()
                ? ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.message(),
                        "oldValue", result.oldValue(),
                        "newValue", result.newValue()
                ))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.code(),
                        "message", result.message()
                ));
    }

    /**
     * PUT /api/admin/config/membership/ranks/{id}/benefits
     * Alternative Flow A6: Configure Membership Benefits.
     * 
     * Body: { "benefits": "{\"pointsRate\":2,\"birthdayBonus\":true}" }
     */
    @PutMapping("/membership/ranks/{id}/benefits")
    public ResponseEntity<?> updateRankBenefits(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        
        String benefits = body.get("benefits");
        if (benefits == null || benefits.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", "INVALID_REQUEST",
                    "message", "benefits JSON is required."
            ));
        }

        var result = service.updateRankBenefits(id, benefits);
        
        return result.success()
                ? ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", result.message()
                ))
                : ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "code", result.code(),
                        "message", result.message()
                ));
    }

    // ==================== VALIDATION ====================

    /**
     * POST /api/admin/config/validate
     * Validate a setting value before saving.
     * 
     * Body: { "settingId": 1, "value": "new value" }
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateSetting(@RequestBody Map<String, Object> body) {
        int settingId = (int) body.get("settingId");
        String newValue = (String) body.get("value");

        var setting = service.getSettingByKey(String.valueOf(settingId));
        if (setting.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "code", "NOT_FOUND",
                    "message", "Setting not found."
            ));
        }

        var validation = service.validateSetting(setting.get(), newValue);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", validation.valid(),
                "errorCode", validation.errorCode() != null ? validation.errorCode() : "",
                "errorMessage", validation.errorMessage() != null ? validation.errorMessage() : ""
        ));
    }
}
