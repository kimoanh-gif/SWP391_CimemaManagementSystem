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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        customerBookingService = new CustomerBookingService(bookingDAO,
                org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
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
    void reusesExistingBookingWhenSeatsDidNotChange() {
        stubFutureShowtime();
        SeatOption seat = seat(7, 1, 1, true);
        when(bookingDAO.findSeats(5, 9)).thenReturn(List.of(seat));
        BookingSummary active = new BookingSummary();
        active.setStatus("PENDING");
        active.setHeldUntil(LocalDateTime.now().plusMinutes(4));
        when(bookingDAO.findBookingSummary(9, 1)).thenReturn(Optional.of(active));
        when(bookingDAO.findBookingSeatIds(9, 1)).thenReturn(List.of(7));

        assertEquals(9, customerBookingService.holdSeats(1, 5, List.of(7), 9));
        verify(bookingDAO, never()).releaseBooking(9, 1);
        verify(bookingDAO, never()).createPendingBooking(eq(1), eq(5), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsSelectionThatLeavesOneOrphanSeat() {
        stubFutureShowtime();
        SeatOption selectedLeft = seat(1, 1, 1, true);
        SeatOption orphan = seat(2, 1, 2, true);
        SeatOption selectedRight = seat(3, 1, 3, true);
        when(bookingDAO.findSeats(5)).thenReturn(List.of(selectedLeft, orphan, selectedRight));

        assertThrows(CustomerBookingService.BookingException.class,
                () -> customerBookingService.holdSeats(1, 5, List.of(1, 3), null));
        verify(bookingDAO, never()).createPendingBooking(
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void allowsTwoAdjacentSelectedSeats() {
        stubFutureShowtime();
        SeatOption before = seat(1, 4, 3, true);
        SeatOption selectedD4 = seat(2, 4, 4, true);
        SeatOption selectedD5 = seat(3, 4, 5, true);
        SeatOption after = seat(4, 4, 6, true);
        when(bookingDAO.findSeats(5)).thenReturn(
                List.of(before, selectedD4, selectedD5, after));
        when(bookingDAO.findCustomerId(1)).thenReturn(Optional.of(2));
        when(bookingDAO.createPendingBooking(eq(2), eq(5), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(13);

        assertEquals(13, customerBookingService.holdSeats(1, 5, List.of(2, 3), null));
    }

    @Test
    void allowsASingleSelectedSeat() {
        stubFutureShowtime();
        SeatOption selected = seat(1, 4, 3, true);
        SeatOption next = seat(2, 4, 4, true);
        when(bookingDAO.findSeats(5)).thenReturn(List.of(selected, next));
        when(bookingDAO.findCustomerId(1)).thenReturn(Optional.of(2));
        when(bookingDAO.createPendingBooking(eq(2), eq(5), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(14);

        assertEquals(14, customerBookingService.holdSeats(1, 5, List.of(1), null));
    }

    @Test
    void allowsSelectedSeatsSeparatedByTwoEmptySeats() {
        stubFutureShowtime();
        SeatOption selectedLeft = seat(1, 4, 3, true);
        SeatOption emptyOne = seat(2, 4, 4, true);
        SeatOption emptyTwo = seat(3, 4, 5, true);
        SeatOption selectedRight = seat(4, 4, 6, true);
        when(bookingDAO.findSeats(5)).thenReturn(
                List.of(selectedLeft, emptyOne, emptyTwo, selectedRight));
        when(bookingDAO.findCustomerId(1)).thenReturn(Optional.of(2));
        when(bookingDAO.createPendingBooking(eq(2), eq(5), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(15);

        assertEquals(15, customerBookingService.holdSeats(1, 5, List.of(1, 4), null));
    }

    @Test
    void ignoresAnOrphanSeatThatAlreadyExistedBeforeSelection() {
        stubFutureShowtime();
        SeatOption occupiedLeft = seat(1, 1, 1, false);
        SeatOption existingOrphan = seat(2, 1, 2, true);
        SeatOption occupiedRight = seat(3, 1, 3, false);
        SeatOption selected = seat(4, 2, 1, true);
        when(bookingDAO.findSeats(5)).thenReturn(
                List.of(occupiedLeft, existingOrphan, occupiedRight, selected));
        when(bookingDAO.findCustomerId(1)).thenReturn(Optional.of(2));
        when(bookingDAO.createPendingBooking(eq(2), eq(5), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(12);

        assertEquals(12, customerBookingService.holdSeats(1, 5, List.of(4), null));
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

    @Test void membershipPriceChangesRequireReviewBeforePaymentStarts() {
        var discount = org.mockito.Mockito.mock(MembershipDiscountService.class);
        customerBookingService.setMembershipDiscountService(discount);
        var before = new BookingSummary();
        before.setStatus("PENDING"); before.setHeldUntil(LocalDateTime.now().plusMinutes(5));
        before.setTotalAmount(new java.math.BigDecimal("100000"));
        var after = new BookingSummary();
        after.setStatus("PENDING"); after.setHeldUntil(before.getHeldUntil());
        after.setTotalAmount(new java.math.BigDecimal("90000"));
        when(bookingDAO.findBookingSummary(9, 1)).thenReturn(Optional.of(before), Optional.of(before), Optional.of(after));
        assertThrows(CustomerBookingService.BookingException.class, () -> customerBookingService.startSimulatedPayment(9, 1, "VNPAY"));
        verify(discount).refreshOnline(9, 1);
        verify(bookingDAO, never()).createPayment(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
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

    private SeatOption seat(int id, int row, int column, boolean available) {
        SeatOption seat = new SeatOption();
        seat.setSeatId(id);
        seat.setGridRow(row);
        seat.setGridColumn(column);
        seat.setAvailable(available);
        seat.setPrice(java.math.BigDecimal.valueOf(100000));
        return seat;
    }
}
