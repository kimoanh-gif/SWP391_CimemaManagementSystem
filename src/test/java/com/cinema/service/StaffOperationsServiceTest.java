package com.cinema.service;

import com.cinema.dao.StaffNewsDAO;
import com.cinema.dao.StaffOperationsDAO;
import com.cinema.model.StaffBookingDetail;
import com.cinema.model.StaffBookingPage;
import com.cinema.model.StaffBookingPrint;
import com.cinema.model.StaffDashboardData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffOperationsServiceTest {

    @Mock
    private StaffNewsDAO staffNewsDAO;
    @Mock
    private StaffOperationsDAO operationsDAO;

    private StaffOperationsService service;

    @BeforeEach
    void setUp() {
        service = new StaffOperationsService(staffNewsDAO, operationsDAO);
    }

    @Test
    void limitsStaffDashboardToTheActiveStaffProfile() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));
        when(operationsDAO.getDashboardData(6, false)).thenReturn(new StaffDashboardData(3, 1, List.of()));

        service.dashboard(15, "STAFF");

        verify(operationsDAO).getDashboardData(6, false);
    }

    @Test
    void staffSearchesAllBookingsByQueryStatusAndShowDate() {
        LocalDate showDate = LocalDate.of(2026, 10, 30);
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));
        when(operationsDAO.countBookings("BK", "PENDING", showDate)).thenReturn(0L);
        when(operationsDAO.findBookings("BK", "PENDING", showDate, 0, StaffOperationsService.PAGE_SIZE))
                .thenReturn(List.of());

        StaffBookingPage page = service.bookings(15, "STAFF", -4, " BK ", "pending", showDate);

        assertEquals(0, page.page());
        assertEquals("BK", page.query());
        assertEquals("PENDING", page.status());
        assertEquals(showDate, page.showDate());
        verify(operationsDAO).findBookings("BK", "PENDING", showDate, 0, StaffOperationsService.PAGE_SIZE);
    }

    @Test
    void staffCanOpenAnOnlineBooking() {
        StaffBookingDetail online = new StaffBookingDetail(40, "BK40", "ONLINE", "CONFIRMED", null,
                "Customer", "0900000000", "c@example.com", "Movie", "Room 1", null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));
        when(operationsDAO.findBookingDetail(40)).thenReturn(Optional.of(online));

        assertEquals("ONLINE", service.bookingDetail(15, "STAFF", 40).bookingType());
    }

    @Test
    void loadsTicketsAndReceiptForAConfirmedBooking() {
        StaffBookingPrint.TicketLine ticket = new StaffBookingPrint.TicketLine(
                123, "CINEORA-abc", "E5", "VIP", new BigDecimal("120000"), "VALID");
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));
        when(operationsDAO.findBookingPrint(40)).thenReturn(Optional.of(print("CONFIRMED", List.of(ticket))));

        StaffBookingPrint print = service.printData(15, "STAFF", 40);

        assertEquals(1, print.tickets().size());
        assertEquals("TK-00000123", print.tickets().get(0).ticketCode());
        assertEquals("CASH", print.paymentMethod());
    }

    @Test
    void refusesToPrintAnUnpaidBooking() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));
        when(operationsDAO.findBookingPrint(41)).thenReturn(Optional.of(print("PAYMENT_PENDING", List.of())));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.printData(15, "STAFF", 41));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void reportsMissingBookingWhenPrinting() {
        when(operationsDAO.findBookingPrint(99)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.printData(1, "MANAGER", 99));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void bookingSearchRequiresAnActiveStaffProfile() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.bookings(15, "STAFF", 0, null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(operationsDAO);
    }

    @Test
    void rejectsUnknownBookingStatusFilter() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.bookings(15, "STAFF", 0, null, "not-a-status", null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(operationsDAO);
    }

    private static StaffBookingPrint print(String status, List<StaffBookingPrint.TicketLine> tickets) {
        return new StaffBookingPrint(40, "BK40", "COUNTER", status, "Movie", "Room 1", null,
                new BigDecimal("120000"), BigDecimal.ZERO, new BigDecimal("120000"),
                "CASH", new BigDecimal("120000"), null, "Staff Name", tickets, List.of());
    }

    @Test
    void checkInReportsUnknownBookingCode() {
        when(operationsDAO.checkInBooking("UNKNOWN")).thenReturn(0);
        when(operationsDAO.hasBooking("UNKNOWN")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.checkIn(" UNKNOWN "));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(operationsDAO).checkInBooking("UNKNOWN");
    }

    @Test
    void reportsSuccessfulCheckInCount() {
        when(operationsDAO.checkInBooking("BK123")).thenReturn(2);

        assertEquals(2, service.checkIn("BK123"));
    }
}
