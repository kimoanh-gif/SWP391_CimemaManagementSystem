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
 * Enforces BR-50 (Formula: Unit Price = (BasePrice + Surcharge) * Multiplier,
 * rounded to nearest 1,000 VND)
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
                .orElseThrow(() -> new TicketPricingNotFoundException("Ticket pricing rule not found with ID: " + id));
    }

    public List<SeatType> getAllSeatTypes(String status) {
        return seatTypeDao.findAll(status);
    }

    public SeatType getSeatTypeById(int id) {
        return seatTypeDao.findById(id)
                .orElseThrow(() -> new SeatTypeNotFoundException("Seat type not found with ID: " + id));
    }

    @Transactional
    public TicketPricing createPricing(TicketPricingForm form) {
        validatePricingForm(form, null);
        ticketPricingDao.insert(form);
        return ticketPricingDao.findByCode(form.getPricingCode().trim())
                .orElseThrow(() -> new IllegalStateException("Failed to save ticket pricing configuration"));
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
            throw new InvalidPricingConfigurationException("Base ticket price cannot be empty or negative");
        }
        TicketPricing existing = getPricingById(id);
        ticketPricingDao.updateBasePrice(id, basePrice);
        return getPricingById(id);
    }

    @Transactional
    public TicketPricing updatePricingStatus(int id, String status) {
        if (status == null || (!"Active".equalsIgnoreCase(status.trim()) && !"Inactive".equalsIgnoreCase(status.trim()))) { throw new InvalidPricingConfigurationException("Pricing rule status must be Active or Inactive"); }
        TicketPricing existing = getPricingById(id);
        if ("Active".equalsIgnoreCase(status)) {
            ticketPricingDao
                    .findActiveConflict(existing.getDayType(), existing.getTimeSlot(), existing.getExperienceType(), id)
                    .ifPresent(conflict -> {
                        throw new DuplicatePricingRuleException(
                                "An active pricing rule already exists: '" + conflict.getRuleName() +
                                        "' (Code: " + conflict.getPricingCode() + ") for combination DayType="
                                        + existing.getDayType() +
                                        ", TimeSlot=" + existing.getTimeSlot() + ", ExperienceType="
                                        + existing.getExperienceType() +
                                        ". Please deactivate the existing rule before activating this one.");
                    });
        }
        ticketPricingDao.updateStatus(id, status);
        return getPricingById(id);
    }

    @Transactional
    public SeatType updateSeatTypeMultiplier(int seatTypeId, BigDecimal multiplier) {
        if (multiplier == null || multiplier.compareTo(new BigDecimal("0.50")) < 0
                || multiplier.compareTo(new BigDecimal("3.00")) > 0) {
            throw new InvalidPricingConfigurationException("Seat price multiplier must be between 0.50 and 3.00");
        }
        SeatType existing = getSeatTypeById(seatTypeId);
        seatTypeDao.updateMultiplier(seatTypeId, multiplier);
        return getSeatTypeById(seatTypeId);
    }

    @Transactional
    public SeatType createSeatType(SeatTypeForm form) {
        if (form.getPriceMultiplier() == null || form.getPriceMultiplier().compareTo(new BigDecimal("0.50")) < 0 || form.getPriceMultiplier().compareTo(new BigDecimal("3.00")) > 0) { throw new InvalidPricingConfigurationException("Seat price multiplier must be between 0.50 and 3.00"); }
        seatTypeDao.findByCode(form.getTypeCode().trim()).ifPresent(s -> {
            throw new InvalidPricingConfigurationException(
                    "Seat type code '" + form.getTypeCode() + "' already exists in the system");
        });
        seatTypeDao.findByName(form.getTypeName().trim()).ifPresent(s -> {
            throw new InvalidPricingConfigurationException(
                    "Seat type name '" + form.getTypeName() + "' already exists in the system");
        });
        seatTypeDao.insert(form);
        return seatTypeDao.findByCode(form.getTypeCode().trim())
                .orElseThrow(() -> new IllegalStateException("Failed to create new seat type"));
    }

    @Transactional
    public SeatType updateSeatType(int id, SeatTypeForm form) {
        SeatType existing = getSeatTypeById(id);
        if (form.getPriceMultiplier() == null || form.getPriceMultiplier().compareTo(new BigDecimal("0.50")) < 0
                || form.getPriceMultiplier().compareTo(new BigDecimal("3.00")) > 0) {
            throw new InvalidPricingConfigurationException("Seat price multiplier must be between 0.50 and 3.00");
        }
        if (form.getPriceMultiplier() == null || form.getPriceMultiplier().compareTo(new BigDecimal("0.50")) < 0 || form.getPriceMultiplier().compareTo(new BigDecimal("3.00")) > 0) { throw new InvalidPricingConfigurationException("Seat price multiplier must be between 0.50 and 3.00"); }
        seatTypeDao.findByCode(form.getTypeCode().trim()).ifPresent(s -> {
            if (!s.getSeatTypeId().equals(id)) {
                throw new InvalidPricingConfigurationException(
                        "Seat type code '" + form.getTypeCode() + "' is already in use");
            }
        });
        seatTypeDao.findByName(form.getTypeName().trim()).ifPresent(s -> {
            if (!s.getSeatTypeId().equals(id)) {
                throw new InvalidPricingConfigurationException(
                        "Seat type name '" + form.getTypeName() + "' is already in use");
            }
        });
        if (form.getStatus() != null && "Inactive".equalsIgnoreCase(form.getStatus().trim())
                && seatTypeDao.hasActiveOrUpcomingShowtimes(id)) {
            throw new InvalidPricingConfigurationException("Cannot deactivate seat type '" + form.getTypeName() + "' because screening rooms with active or upcoming showtimes are using it.");
        }
        seatTypeDao.update(id, form);
        return getSeatTypeById(id);
    }

    @Transactional
    public SeatType updateSeatTypeStatus(int id, String status) {
        SeatType existing = getSeatTypeById(id);
        if (status == null || (!"Active".equalsIgnoreCase(status.trim()) && !"Inactive".equalsIgnoreCase(status.trim()))) { throw new InvalidPricingConfigurationException("Seat type status must be Active or Inactive"); }
        if ("Inactive".equalsIgnoreCase(status.trim()) && seatTypeDao.hasActiveOrUpcomingShowtimes(id)) {
            throw new InvalidPricingConfigurationException("Cannot deactivate seat type '" + existing.getTypeName() + "' because screening rooms with active or upcoming showtimes are using it.");
        }
        seatTypeDao.updateStatus(id, status);
        return getSeatTypeById(id);
    }

    @Transactional
    public void deleteSeatType(int id) {
        SeatType st = getSeatTypeById(id);
        if (seatTypeDao.isReferencedBySeats(id)) {
            throw new InvalidPricingConfigurationException(
                    "Cannot delete seat type '" + st.getTypeName() + "' because seats in screening rooms are using this seat type. Please change seat types for related seats first or switch status to Inactive."
            );
        }
        seatTypeDao.delete(id);
    }

    /**
     * Calculates the calculated ticket unit price according to BR-50:
     * Ticket unit price = Showtime base ticket price x SeatType price multiplier,
     * rounded to the nearest 1,000 VND.
     */
    public BigDecimal calculateTicketUnitPrice(BigDecimal basePrice, BigDecimal multiplier) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPricingConfigurationException("Invalid base ticket price: " + basePrice);
        }
        if (multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0) {
            multiplier = BigDecimal.ONE;
        }
        BigDecimal rawPrice = basePrice.multiply(multiplier);
        BigDecimal thousand = new BigDecimal("1000");
        return rawPrice.divide(thousand, 0, RoundingMode.HALF_UP).multiply(thousand);
    }

    /**
     * Generates a matrix map of pricing calculations for all active pricing rules
     * against all active seat types.
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
     * Determine day type for ticket pricing:
     * - WEEKEND: Saturday, Sunday
     * - WEEKDAY: Monday through Friday
     */
    public String determineDayType(java.time.LocalDate date) {
        if (date == null) return "WEEKDAY";
        java.time.DayOfWeek dow = date.getDayOfWeek();
        if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) {
            return "WEEKEND";
        }
        return "WEEKDAY";
    }

    /**
     * Resolves the authoritative effective base price for a screening from active TicketPricing rules,
     * adding FormatSurcharge exactly once, or falling back to the showtime base price.
     */
    public BigDecimal resolveEffectiveBasePrice(java.time.LocalDateTime showtimeStart, String experienceType, BigDecimal fallbackBasePrice) {
        if (showtimeStart == null) {
            return (fallbackBasePrice != null && fallbackBasePrice.compareTo(BigDecimal.ZERO) >= 0)
                    ? fallbackBasePrice : new BigDecimal("85000");
        }
        String dayType = determineDayType(showtimeStart.toLocalDate());
        String timeSlot = determineTimeSlot(showtimeStart.toLocalTime());
        String exp = (experienceType != null && !experienceType.isBlank()) ? experienceType.trim() : "2D Digital";

        // 1. Try exact active rule match (dayType, timeSlot, exp)
        java.util.Optional<TicketPricing> matched = ticketPricingDao.findActiveConflict(dayType, timeSlot, exp, null);

        // 2. Fallback to generic 2D Digital if specific format not found
        if (matched.isEmpty() && !exp.equalsIgnoreCase("2D Digital")) {
            matched = ticketPricingDao.findActiveConflict(dayType, timeSlot, "2D Digital", null);
        }

        // 3. Fallback to ALL_DAY if specific timeSlot not found
        if (matched.isEmpty()) {
            matched = ticketPricingDao.findActiveConflict(dayType, "ALL_DAY", exp, null);
        }

        if (matched.isPresent()) {
            TicketPricing rule = matched.get();
            BigDecimal base = rule.getBasePrice() != null ? rule.getBasePrice() : BigDecimal.ZERO;
            BigDecimal surcharge = rule.getFormatSurcharge() != null ? rule.getFormatSurcharge() : BigDecimal.ZERO;
            return base.add(surcharge);
        }

        // 4. Fallback to showtime base price or system default
        return (fallbackBasePrice != null && fallbackBasePrice.compareTo(BigDecimal.ZERO) >= 0)
                ? fallbackBasePrice : new BigDecimal("85000");
    }

    /**
     * Resolves the authoritative ticket unit price for a seat at checkout according to BR-50:
     * Effective base price x SeatType price multiplier, rounded to the nearest 1,000 VND (HALF_UP).
     */
    public BigDecimal resolveSeatPrice(java.time.LocalDateTime showtimeStart, String experienceType, BigDecimal fallbackBasePrice, BigDecimal multiplier) {
        BigDecimal effectiveBase = resolveEffectiveBasePrice(showtimeStart, experienceType, fallbackBasePrice);
        return calculateTicketUnitPrice(effectiveBase, multiplier);
    }

    /**
     * P1-03: Determine time slot based on strict boundary rules:
     * - MORNING: < 12:00
     * - STANDARD: 12:00 - 17:00
     * - GOLDEN_HOUR: >= 17:00
     */
    public String determineTimeSlot(java.time.LocalTime time) {
        if (time == null)
            return "STANDARD";
        if (time.isBefore(java.time.LocalTime.of(12, 0))) {
            return "MORNING";
        } else if (time.isBefore(java.time.LocalTime.of(17, 0))) {
            return "STANDARD";
        } else {
            return "GOLDEN_HOUR";
        }
    }

    /**
     * Calculates ticket price for a seat by SeatTypeID, with fallback to
     * SeatTypeName per P1-02.
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
            throw new InvalidPricingConfigurationException("Base ticket price cannot be empty or negative");
        }
        if (form.getFormatSurcharge() == null || form.getFormatSurcharge().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPricingConfigurationException("Format surcharge cannot be empty or negative");
        }

        ticketPricingDao.findByCode(form.getPricingCode().trim()).ifPresent(tp -> {
            if (currentId == null || !tp.getPricingId().equals(currentId)) {
                throw new InvalidPricingConfigurationException(
                        "Pricing code '" + form.getPricingCode() + "' already exists in the system");
            }
        });

        if (form.getDayType() == null || form.getDayType().isBlank()) {
            throw new InvalidPricingConfigurationException("Day type cannot be empty");
        }
        if (form.getTimeSlot() == null || form.getTimeSlot().isBlank()) {
            throw new InvalidPricingConfigurationException("Time slot cannot be empty");
        }

        // P1-03: Collision check for active pricing rules
        if (form.getStatus() != null && !form.getStatus().isBlank() && !"Active".equalsIgnoreCase(form.getStatus().trim()) && !"Inactive".equalsIgnoreCase(form.getStatus().trim())) { throw new InvalidPricingConfigurationException("Invalid status: " + form.getStatus()); }
        if (!form.getDayType().trim().matches("(?i)^(WEEKDAY|WEEKEND|HOLIDAY)$")) { throw new InvalidPricingConfigurationException("Invalid day type: " + form.getDayType()); }
        if (!form.getTimeSlot().trim().matches("(?i)^(MORNING|STANDARD|GOLDEN_HOUR|ALL_DAY)$")) { throw new InvalidPricingConfigurationException("Invalid time slot: " + form.getTimeSlot()); }
        boolean isActive = form.getStatus() == null || "Active".equalsIgnoreCase(form.getStatus().trim());
        if (isActive) {
            ticketPricingDao
                    .findActiveConflict(form.getDayType().trim(), form.getTimeSlot().trim(), form.getExperienceType(),
                            currentId)
                    .ifPresent(conflict -> {
                        throw new DuplicatePricingRuleException(
                                "An active pricing rule already exists: '"
                                        + conflict.getRuleName() +
                                        "' (Code: " + conflict.getPricingCode() + ") for combination DayType="
                                        + form.getDayType() +
                                        ", TimeSlot=" + form.getTimeSlot() + ", ExperienceType="
                                        + form.getExperienceType() + ".");
                    });
        }
    }
}
