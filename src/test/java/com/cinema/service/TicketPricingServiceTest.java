package com.cinema.service;

import com.cinema.dao.SeatTypeDao;
import com.cinema.dao.TicketPricingDao;
import com.cinema.exception.InvalidPricingConfigurationException;
import com.cinema.exception.SeatTypeNotFoundException;
import com.cinema.exception.TicketPricingNotFoundException;
import com.cinema.model.SeatType;
import com.cinema.dto.SeatTypeForm;
import com.cinema.model.TicketPricing;
import com.cinema.dto.TicketPricingForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketPricingServiceTest {

    @Mock
    private TicketPricingDao ticketPricingDao;

    @Mock
    private SeatTypeDao seatTypeDao;

    @InjectMocks
    private TicketPricingService pricingService;

    private TicketPricing standardRule;
    private SeatType vipSeat;

    @BeforeEach
    void setUp() {
        standardRule = new TicketPricing(1, "PRICE-WD-STD", "Ngày thường tiêu chuẩn",
                "WEEKDAY", "STANDARD", new BigDecimal("85000"), BigDecimal.ZERO,
                "2D Digital", "Active", "Mô tả");

        vipSeat = new SeatType(2, "VIP", "Ghế VIP", "VIP",
                new BigDecimal("1.25"), "#F59E0B", "Active");
    }

    @Test
    void testCalculateTicketUnitPrice_BR50_Rounding() {
        // Base = 85,000, Multiplier = 1.25 -> 85000 * 1.25 = 106,250 -> rounded to nearest 1,000 = 106,000
        BigDecimal price = pricingService.calculateTicketUnitPrice(new BigDecimal("85000"), new BigDecimal("1.25"));
        assertEquals(new BigDecimal("106000"), price);

        // Base = 85,000, Multiplier = 1.80 -> 85000 * 1.80 = 153,000 -> 153,000
        BigDecimal couplePrice = pricingService.calculateTicketUnitPrice(new BigDecimal("85000"), new BigDecimal("1.80"));
        assertEquals(new BigDecimal("153000"), couplePrice);

        // Base = 85,000, Multiplier = 0.90 -> 85000 * 0.90 = 76,500 -> rounded up = 77,000
        BigDecimal frontPrice = pricingService.calculateTicketUnitPrice(new BigDecimal("85000"), new BigDecimal("0.90"));
        assertEquals(new BigDecimal("77000"), frontPrice);
    }

    @Test
    void testCalculateTicketUnitPrice_NegativeBase_ThrowsException() {
        assertThrows(InvalidPricingConfigurationException.class, () ->
                pricingService.calculateTicketUnitPrice(new BigDecimal("-1000"), new BigDecimal("1.0")));
    }

    @Test
    void testGetAllPricingRules() {
        when(ticketPricingDao.findAll("WEEKDAY", "Active")).thenReturn(List.of(standardRule));

        List<TicketPricing> result = pricingService.getAllPricingRules("WEEKDAY", "Active");
        assertEquals(1, result.size());
        assertEquals("PRICE-WD-STD", result.get(0).getPricingCode());
    }

    @Test
    void testGetPricingById_Success() {
        when(ticketPricingDao.findById(1)).thenReturn(Optional.of(standardRule));

        TicketPricing result = pricingService.getPricingById(1);
        assertNotNull(result);
        assertEquals(1, result.getPricingId());
    }

    @Test
    void testGetPricingById_NotFound() {
        when(ticketPricingDao.findById(99)).thenReturn(Optional.empty());

        assertThrows(TicketPricingNotFoundException.class, () -> pricingService.getPricingById(99));
    }

    @Test
    void testCreatePricing_DuplicateCode_ThrowsException() {
        TicketPricingForm form = new TicketPricingForm();
        form.setPricingCode("PRICE-WD-STD");
        form.setRuleName("Test Rule");
        form.setBasePrice(new BigDecimal("90000"));
        form.setFormatSurcharge(BigDecimal.ZERO);

        when(ticketPricingDao.findByCode("PRICE-WD-STD")).thenReturn(Optional.of(standardRule));

        assertThrows(InvalidPricingConfigurationException.class, () -> pricingService.createPricing(form));
    }

    @Test
    void testUpdateSeatTypeMultiplier_Success() {
        when(seatTypeDao.findById(2)).thenReturn(Optional.of(vipSeat));
        when(seatTypeDao.updateMultiplier(eq(2), any())).thenReturn(1);

        SeatType updated = pricingService.updateSeatTypeMultiplier(2, new BigDecimal("1.30"));
        assertNotNull(updated);
        verify(seatTypeDao).updateMultiplier(2, new BigDecimal("1.30"));
    }

    @Test
    void testUpdateSeatTypeMultiplier_InvalidRange_ThrowsException() {
        assertThrows(InvalidPricingConfigurationException.class, () ->
                pricingService.updateSeatTypeMultiplier(2, new BigDecimal("0.20")));

        assertThrows(InvalidPricingConfigurationException.class, () ->
                pricingService.updateSeatTypeMultiplier(2, new BigDecimal("3.50")));
    }

    @Test
    void testUpdateBasePrice_Negative_ThrowsException() {
        assertThrows(InvalidPricingConfigurationException.class, () ->
                pricingService.updateBasePrice(1, new BigDecimal("-5000")));
    }
}
