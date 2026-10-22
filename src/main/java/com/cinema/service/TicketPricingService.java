package com.cinema.service;

import com.cinema.dao.SeatTypeDao;
import com.cinema.dao.TicketPricingDao;
import com.cinema.exception.DuplicatePricingRuleException;
import com.cinema.exception.InvalidPricingConfigurationException;
import com.cinema.exception.SeatTypeNotFoundException;
import com.cinema.exception.TicketPricingNotFoundException;
import com.cinema.model.SeatType;
import com.cinema.dto.SeatTypeForm;
import com.cinema.model.TicketPricing;
import com.cinema.dto.TicketPricingForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Ticket Pricing Management (Screen 60 / UC-31)
 * Enforces BR-50 (Formula: Unit Price = (BasePrice + Surcharge) * Multiplier, rounded to nearest 1,000 VND)
 * and BR-51 (Snapshot pricing integrity).
 */
@Service
public class TicketPricingService {

    private final TicketPricingDao ticketPricingDao;
    private final SeatTypeDao seatTypeDao;

    public TicketPricingService(TicketPricingDao ticketPricingDao, SeatTypeDao seatTypeDao) {
        this.ticketPricingDao = ticketPricingDao;
        this.seatTypeDao = seatTypeDao;
    }

    public List<TicketPricing> getAllPricingRules(String dayType, String status) {
        return ticketPricingDao.findAll(dayType, status);
    }

    public TicketPricing getPricingById(int id) {
        return ticketPricingDao.findById(id)
                .orElseThrow(() -> new TicketPricingNotFoundException("Không tìm thấy quy tắc giá vé với ID: " + id));
    }

    public List<SeatType> getAllSeatTypes(String status) {
        return seatTypeDao.findAll(status);
    }

    public SeatType getSeatTypeById(int id) {
        return seatTypeDao.findById(id)
                .orElseThrow(() -> new SeatTypeNotFoundException("Không tìm thấy loại ghế với ID: " + id));
    }

    @Transactional
    public TicketPricing createPricing(TicketPricingForm form) {
        validatePricingForm(form, null);
        ticketPricingDao.insert(form);
        return ticketPricingDao.findByCode(form.getPricingCode().trim())
                .orElseThrow(() -> new IllegalStateException("Lỗi lưu trữ cấu hình giá vé"));
    }

    @Transactional
    public TicketPricing updatePricing(int id, TicketPricingForm form) {
        TicketPricing existing = getPricingById(id);
        validatePricingForm(form, id);
        ticketPricingDao.update(id, form);
        return getPricingById(id);
    }

    @Transactional
    public TicketPricing updateBasePrice(int id, BigDecimal basePrice) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPricingConfigurationException("Giá vé cơ sở không được để trống hoặc là số âm");
        }
        TicketPricing existing = getPricingById(id);
        ticketPricingDao.updateBasePrice(id, basePrice);
        return getPricingById(id);
    }

    @Transactional
    public TicketPricing updatePricingStatus(int id, String status) {
        TicketPricing existing = getPricingById(id);
        if ("Active".equalsIgnoreCase(status)) {
            ticketPricingDao.findActiveConflict(existing.getDayType(), existing.getTimeSlot(), existing.getExperienceType(), id)
                    .ifPresent(conflict -> {
                        throw new DuplicatePricingRuleException(
                                "Đã tồn tại quy tắc giá đang kích hoạt '" + conflict.getRuleName() +
                                "' (Mã: " + conflict.getPricingCode() + ") cho tổ hợp DayType=" + existing.getDayType() +
                                ", TimeSlot=" + existing.getTimeSlot() + ", ExperienceType=" + existing.getExperienceType() +
                                ". Vui lòng vô hiệu hóa quy tắc cũ trước khi kích hoạt quy tắc này."
                        );
                    });
        }
        ticketPricingDao.updateStatus(id, status);
        return getPricingById(id);
    }

    @Transactional
    public SeatType updateSeatTypeMultiplier(int seatTypeId, BigDecimal multiplier) {
        if (multiplier == null || multiplier.compareTo(new BigDecimal("0.50")) < 0 || multiplier.compareTo(new BigDecimal("3.00")) > 0) {
            throw new InvalidPricingConfigurationException("Hệ số giá ghế phải nằm trong khoảng từ 0.50 đến 3.00");
        }
        SeatType existing = getSeatTypeById(seatTypeId);
        seatTypeDao.updateMultiplier(seatTypeId, multiplier);
        return getSeatTypeById(seatTypeId);
    }

    @Transactional
    public SeatType createSeatType(SeatTypeForm form) {
        seatTypeDao.findByCode(form.getTypeCode().trim()).ifPresent(s -> {
            throw new InvalidPricingConfigurationException("Mã loại ghế '" + form.getTypeCode() + "' đã tồn tại trong hệ thống");
        });
        seatTypeDao.findByName(form.getTypeName().trim()).ifPresent(s -> {
            throw new InvalidPricingConfigurationException("Tên loại ghế '" + form.getTypeName() + "' đã tồn tại trong hệ thống");
        });
        seatTypeDao.insert(form);
        return seatTypeDao.findByCode(form.getTypeCode().trim())
                .orElseThrow(() -> new IllegalStateException("Lỗi tạo loại ghế mới"));
    }

    @Transactional
    public SeatType updateSeatType(int id, SeatTypeForm form) {
        SeatType existing = getSeatTypeById(id);
        if (form.getPriceMultiplier() == null || form.getPriceMultiplier().compareTo(new BigDecimal("0.50")) < 0 || form.getPriceMultiplier().compareTo(new BigDecimal("3.00")) > 0) {
            throw new InvalidPricingConfigurationException("Hệ số giá ghế phải nằm trong khoảng từ 0.50 đến 3.00");
        }
        seatTypeDao.findByCode(form.getTypeCode().trim()).ifPresent(s -> {
            if (!s.getSeatTypeId().equals(id)) {
                throw new InvalidPricingConfigurationException("Mã loại ghế '" + form.getTypeCode() + "' đã được sử dụng");
            }
        });
        seatTypeDao.findByName(form.getTypeName().trim()).ifPresent(s -> {
            if (!s.getSeatTypeId().equals(id)) {
                throw new InvalidPricingConfigurationException("Tên loại ghế '" + form.getTypeName() + "' đã được sử dụng");
            }
        });
        seatTypeDao.update(id, form);
        return getSeatTypeById(id);
    }

    @Transactional
    public SeatType updateSeatTypeStatus(int id, String status) {
        SeatType existing = getSeatTypeById(id);
        seatTypeDao.updateStatus(id, status);
        return getSeatTypeById(id);
    }

    /**
     * Calculates the calculated ticket unit price according to BR-50:
     * Ticket unit price = Showtime base ticket price x SeatType price multiplier,
     * rounded to the nearest 1,000 VND.
     */
    public BigDecimal calculateTicketUnitPrice(BigDecimal basePrice, BigDecimal multiplier) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPricingConfigurationException("Giá vé cơ sở không hợp lệ: " + basePrice);
        }
        if (multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            multiplier = BigDecimal.ONE;
        }
        BigDecimal rawPrice = basePrice.multiply(multiplier);
        BigDecimal thousand = new BigDecimal("1000");
        return rawPrice.divide(thousand, 0, RoundingMode.HALF_UP).multiply(thousand);
    }

    /**
     * Generates a matrix map of pricing calculations for all active pricing rules against all active seat types.
     * Matrix: Map<PricingID, Map<SeatTypeID, CalculatedPrice>>
     */
    public Map<Integer, Map<Integer, BigDecimal>> generatePricingMatrix() {
        List<TicketPricing> rules = ticketPricingDao.findAll(null, "Active");
        List<SeatType> seatTypes = seatTypeDao.findAll("Active");
        Map<Integer, Map<Integer, BigDecimal>> matrix = new HashMap<>();

        for (TicketPricing rule : rules) {
            Map<Integer, BigDecimal> row = new HashMap<>();
            BigDecimal effectiveBase = rule.getBasePrice().add(rule.getFormatSurcharge());
            for (SeatType st : seatTypes) {
                row.put(st.getSeatTypeId(), calculateTicketUnitPrice(effectiveBase, st.getPriceMultiplier()));
            }
            matrix.put(rule.getPricingId(), row);
        }
        return matrix;
    }

    public BigDecimal getAverageTicketPrice() {
        return ticketPricingDao.getAverageTicketPrice();
    }

    /**
     * P1-03: Determine time slot based on strict boundary rules:
     * - MORNING: < 12:00
     * - STANDARD: 12:00 - 17:00
     * - GOLDEN_HOUR: >= 17:00
     */
    public String determineTimeSlot(java.time.LocalTime time) {
        if (time == null) return "STANDARD";
        if (time.isBefore(java.time.LocalTime.of(12, 0))) {
            return "MORNING";
        } else if (time.isBefore(java.time.LocalTime.of(17, 0))) {
            return "STANDARD";
        } else {
            return "GOLDEN_HOUR";
        }
    }

    /**
     * Calculates ticket price for a seat by SeatTypeID, with fallback to SeatTypeName per P1-02.
     */
    public BigDecimal calculatePriceForSeat(BigDecimal basePrice, Integer seatTypeId, String seatTypeName) {
        BigDecimal multiplier = BigDecimal.ONE;
        if (seatTypeId != null) {
            java.util.Optional<SeatType> st = seatTypeDao.findById(seatTypeId);
            if (st.isPresent()) {
                multiplier = st.get().getPriceMultiplier();
                return calculateTicketUnitPrice(basePrice, multiplier);
            }
        }
        if (seatTypeName != null && !seatTypeName.isBlank()) {
            java.util.Optional<SeatType> st = seatTypeDao.findByName(seatTypeName.trim());
            if (st.isEmpty()) {
                st = seatTypeDao.findByCode(seatTypeName.trim());
            }
            if (st.isPresent()) {
                multiplier = st.get().getPriceMultiplier();
            }
        }
        return calculateTicketUnitPrice(basePrice, multiplier);
    }

    private void validatePricingForm(TicketPricingForm form, Integer currentId) {
        if (form.getBasePrice() == null || form.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPricingConfigurationException("Giá vé cơ sở không được để trống hoặc là số âm");
        }
        if (form.getFormatSurcharge() == null || form.getFormatSurcharge().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPricingConfigurationException("Phụ thu định dạng không được để trống hoặc là số âm");
        }

        ticketPricingDao.findByCode(form.getPricingCode().trim()).ifPresent(tp -> {
            if (currentId == null || !tp.getPricingId().equals(currentId)) {
                throw new InvalidPricingConfigurationException("Mã cấu hình giá '" + form.getPricingCode() + "' đã tồn tại trong hệ thống");
            }
        });

        if (form.getDayType() == null || form.getDayType().isBlank()) {
            throw new InvalidPricingConfigurationException("Loại ngày không được để trống");
        }
        if (form.getTimeSlot() == null || form.getTimeSlot().isBlank()) {
            throw new InvalidPricingConfigurationException("Khung giờ chiếu không được để trống");
        }

        // P1-03: Collision check for active pricing rules
        boolean isActive = form.getStatus() == null || "Active".equalsIgnoreCase(form.getStatus().trim());
        if (isActive) {
            ticketPricingDao.findActiveConflict(form.getDayType().trim(), form.getTimeSlot().trim(), form.getExperienceType(), currentId)
                    .ifPresent(conflict -> {
                        throw new DuplicatePricingRuleException(
                                "Đã tồn tại quy tắc giá đang kích hoạt '" + conflict.getRuleName() +
                                "' (Mã: " + conflict.getPricingCode() + ") cho tổ hợp DayType=" + form.getDayType() +
                                ", TimeSlot=" + form.getTimeSlot() + ", ExperienceType=" + form.getExperienceType() + "."
                        );
                    });
        }
    }
}
