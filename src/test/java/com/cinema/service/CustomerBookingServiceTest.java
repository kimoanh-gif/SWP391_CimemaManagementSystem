package com.cinema.service;

import com.cinema.dao.CustomerBookingDAO;
import com.cinema.model.BookingSummary;
import com.cinema.model.FoodBeverageOption;
import com.cinema.model.SeatOption;
import com.cinema.model.ShowtimeOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerBookingServiceTest {

    @Mock
    private CustomerBookingDAO bookingDAO;

    private CustomerBookingService customerBookingService;

    @BeforeEach
    void setUp() {
        customerBookingService = new CustomerBookingService(bookingDAO);
    }

    @Test
    void rejectsAnEmptySeatSelection() {
        stubFutureShowtime();
        assertThrows(CustomerBookingService.BookingException.class,
                () -> customerBookingService.holdSeats(1, 5, List.of(), null));

        verify(bookingDAO, never()).findSeats(5);
    }

    @Test
    void rejectsMoreThanTenSeatsBeforeCreatingABooking() {
        stubFutureShowtime();
        List<Integer> seatIds = IntStream.rangeClosed(1, 11).boxed().toList();

        assertThrows(CustomerBookingService.BookingException.class,
                () -> customerBookingService.holdSeats(1, 5, seatIds, null));

        verify(bookingDAO, never()).findSeats(5);
    }

    @Test
    void rejectsSeatsThatAreNoLongerAvailable() {
        stubFutureShowtime();
        SeatOption unavailableSeat = new SeatOption();
        unavailableSeat.setSeatId(7);
        unavailableSeat.setAvailable(false);
        when(bookingDAO.findSeats(5)).thenReturn(List.of(unavailableSeat));

        assertThrows(CustomerBookingService.BookingException.class,
                () -> customerBookingService.holdSeats(1, 5, List.of(7), null));

        verify(bookingDAO, never()).findCustomerId(1);
    }

    @Test
    void rejectsFoodQuantityAboveTwentyWithoutSaving() {
        BookingSummary activeBooking = new BookingSummary();
        activeBooking.setStatus("PENDING");
        activeBooking.setHeldUntil(LocalDateTime.now().plusMinutes(5));
        when(bookingDAO.findBookingSummary(9, 1)).thenReturn(Optional.of(activeBooking));

        FoodBeverageOption food = new FoodBeverageOption();
        food.setItemId(3);
        food.setPrice(java.math.BigDecimal.valueOf(65000));
        when(bookingDAO.findFoodItems(9)).thenReturn(List.of(food));

        assertThrows(CustomerBookingService.BookingException.class,
                () -> customerBookingService.saveFoodSelections(9, 1, List.of(3), List.of(21)));

        verify(bookingDAO, never()).replaceFoodSelections(eq(9), anyList());
        verify(bookingDAO, never()).recalculateBookingTotal(9);
    }

    private ShowtimeOption futureShowtime() {
        ShowtimeOption showtime = new ShowtimeOption();
        showtime.setShowtimeId(5);
        showtime.setStartTime(LocalDateTime.now().plusHours(2));
        return showtime;
    }

    private void stubFutureShowtime() {
        when(bookingDAO.findShowtime(5)).thenReturn(Optional.of(futureShowtime()));
    }
}
