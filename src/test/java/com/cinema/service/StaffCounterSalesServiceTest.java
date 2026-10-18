package com.cinema.service;

import com.cinema.dao.CustomerBookingDAO;
import com.cinema.dao.MemberLookupDAO;
import com.cinema.dao.StaffNewsDAO;
import com.cinema.model.BookingSummary;
import com.cinema.model.FoodBeverageOption;
import com.cinema.model.SeatOption;
import com.cinema.model.ShowtimeOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffCounterSalesServiceTest {

    @Mock
    private CustomerBookingDAO bookingDAO;
    @Mock
    private CustomerBookingService customerBookingService;
    @Mock
    private MemberLookupDAO memberLookupDAO;
    @Mock
    private StaffNewsDAO staffNewsDAO;

    private StaffCounterSalesService service;

    @BeforeEach
    void setUp() {
        service = new StaffCounterSalesService(bookingDAO, customerBookingService, memberLookupDAO, staffNewsDAO);
    }

    @Test
    void rejectsEmptySeatSelectionBeforeCreatingAnOrder() {
        when(staffNewsDAO.findActiveStaffId(12)).thenReturn(Optional.of(4));
        when(customerBookingService.getBookableShowtime(8)).thenReturn(bookableShowtime());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createPendingCounterBooking(12, 8, List.of(), null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(bookingDAO, never()).createPendingCounterBooking(eq(null), eq(4), eq(8), anyString());
    }

    @Test
    void createsPendingCounterBookingAndHoldsEachValidatedSeat() {
        when(staffNewsDAO.findActiveStaffId(12)).thenReturn(Optional.of(4));
        when(customerBookingService.getBookableShowtime(8)).thenReturn(bookableShowtime());
        SeatOption seat = new SeatOption();
        seat.setSeatId(21);
        seat.setAvailable(true);
        when(bookingDAO.findSeats(8)).thenReturn(List.of(seat));
        when(memberLookupDAO.findCustomerIdByPhoneOrEmail("member@example.com")).thenReturn(Optional.of(99));
        when(bookingDAO.createPendingCounterBooking(eq(99), eq(4), eq(8), anyString())).thenReturn(31);

        int bookingId = service.createPendingCounterBooking(12, 8, List.of(21), " member@example.com ");

        assertEquals(31, bookingId);
        verify(bookingDAO).holdSeat(eq(31), eq(8), eq(seat), any(LocalDateTime.class));
        verify(bookingDAO).recalculateBookingTotal(31);
    }

    @Test
    void rejectsSeatThatBecameUnavailableBeforeCreatingTheOrder() {
        when(staffNewsDAO.findActiveStaffId(12)).thenReturn(Optional.of(4));
        when(customerBookingService.getBookableShowtime(8)).thenReturn(bookableShowtime());
        SeatOption unavailable = new SeatOption();
        unavailable.setSeatId(21);
        unavailable.setAvailable(false);
        when(bookingDAO.findSeats(8)).thenReturn(List.of(unavailable));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createPendingCounterBooking(12, 8, List.of(21), null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingDAO, never()).createPendingCounterBooking(any(), eq(4), eq(8), anyString());
    }

    @Test
    void validatesFoodQuantitiesAndSavesTheSelectedItems() {
        when(staffNewsDAO.findActiveStaffId(12)).thenReturn(Optional.of(4));
        BookingSummary summary = activeBooking();
        when(bookingDAO.findCounterBookingSummary(31, 4)).thenReturn(Optional.of(summary));
        FoodBeverageOption item = new FoodBeverageOption();
        item.setItemId(5);
        when(bookingDAO.findFoodItems(31)).thenReturn(List.of(item));

        service.saveFoodSelections(12, 31, List.of(5), List.of(2));

        assertEquals(2, item.getQuantity());
        verify(bookingDAO).replaceFoodSelections(31, List.of(item));
        verify(bookingDAO).recalculateBookingTotal(31);
    }

    @Test
    void rejectsInvalidFoodQuantityWithoutPersistingSelections() {
        when(staffNewsDAO.findActiveStaffId(12)).thenReturn(Optional.of(4));
        when(bookingDAO.findCounterBookingSummary(31, 4)).thenReturn(Optional.of(activeBooking()));
        FoodBeverageOption item = new FoodBeverageOption();
        item.setItemId(5);
        when(bookingDAO.findFoodItems(31)).thenReturn(List.of(item));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.saveFoodSelections(12, 31, List.of(5), List.of(21)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(bookingDAO, never()).replaceFoodSelections(eq(31), any());
        verify(bookingDAO, never()).recalculateBookingTotal(31);
    }

    private BookingSummary activeBooking() {
        BookingSummary summary = new BookingSummary();
        summary.setStatus("PENDING");
        summary.setHeldUntil(LocalDateTime.now().plusMinutes(5));
        return summary;
    }

    private ShowtimeOption bookableShowtime() {
        ShowtimeOption showtime = new ShowtimeOption();
        showtime.setShowtimeId(8);
        return showtime;
    }
}
