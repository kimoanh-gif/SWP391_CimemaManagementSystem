package com.cinema.service;

import com.cinema.dao.SeatTypeDao;
import com.cinema.dao.TicketPricingDao;
import com.cinema.exception.InvalidPricingConfigurationException;
import com.cinema.exception.SeatTypeNotFoundException;
import com.cinema.exception.TicketPricingNotFoundException;
import com.cinema.model.SeatType;
import com.cinema.model.SeatTypeForm;
import com.cinema.model.TicketPricing;
import com.cinema.model.TicketPricingForm;
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
    }
}
