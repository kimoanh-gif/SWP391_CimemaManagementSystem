package com.cinema.service;

import com.cinema.dao.StaffNewsDAO;
import com.cinema.dao.StaffOperationsDAO;
import com.cinema.model.StaffBookingDetail;
import com.cinema.model.StaffBookingPage;
import com.cinema.model.StaffBookingPrint;
import com.cinema.model.StaffDashboardData;
import com.cinema.model.StaffTicketCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
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

    // Gate clock: 18:45 on 01/11/2026, 15 minutes before the 19:00 showtime.
    private static final LocalDateTime SHOW_START = LocalDateTime.of(2026, 11, 1, 19, 0);
    private static final String QR = "CINEORA-1f2e3d4c";

    private StaffOperationsService gateService() {
        Clock clock = Clock.fixed(SHOW_START.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        return new StaffOperationsService(staffNewsDAO, operationsDAO, clock);
    }

    private static StaffTicketCheck.Ticket ticket(String ticketStatus, String bookingStatus,
                                                  LocalDateTime start, LocalDateTime checkedInAt) {
        return new StaffTicketCheck.Ticket(8, QR, ticketStatus, checkedInAt, "BK8", bookingStatus,
                "Blue Planet", "P", "Cinema 02", "A1", "STANDARD", start, start.plusMinutes(105));
    }

    @Test
    void verifiesTicketScannedFromItsQrLink() {
        when(operationsDAO.findTicketForCheckIn(QR, null))
                .thenReturn(Optional.of(ticket("VALID", "CONFIRMED", SHOW_START, null)));

        StaffTicketCheck check = gateService().verifyTicket(" http://192.168.1.5:8080/ticket/verify/" + QR + " ");

        assertEquals(StaffTicketCheck.Result.VALID, check.result());
        assertEquals("TK-00000008", check.ticket().ticketCode());
    }

    @Test
    void verifiesTicketByTypedTicketId() {
        when(operationsDAO.findTicketForCheckIn(null, 8))
                .thenReturn(Optional.of(ticket("VALID", "CONFIRMED", SHOW_START, null)));

        assertEquals(StaffTicketCheck.Result.VALID, gateService().verifyTicket("tk-00000008").result());
    }

    @Test
    void warnsWhenTicketWasAlreadyCheckedIn() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(Optional.of(
                ticket("USED", "CONFIRMED", SHOW_START, SHOW_START.minusMinutes(20))));

        StaffTicketCheck check = gateService().verifyTicket(QR);

        assertEquals(StaffTicketCheck.Result.ALREADY_USED, check.result());
        assertTrue(check.message().contains("18:40 01/11/2026"));
    }

    @Test
    void rejectsTicketOutsideTheCheckInWindow() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(Optional.of(
                ticket("VALID", "CONFIRMED", SHOW_START.plusHours(2), null)));

        StaffTicketCheck check = gateService().verifyTicket(QR);

        assertEquals(StaffTicketCheck.Result.WRONG_SHOWTIME, check.result());
        assertTrue(check.message().contains("Check-in opens at 20:30"));
    }

    @Test
    void rejectsTicketOfUnpaidBooking() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(Optional.of(
                ticket("VALID", "PAYMENT_PENDING", SHOW_START, null)));

        assertEquals(StaffTicketCheck.Result.INVALID, gateService().verifyTicket(QR).result());
    }

    @Test
    void reportsUnknownTicket() {
        when(operationsDAO.findTicketForCheckIn("CINEORA-unknown", null)).thenReturn(Optional.empty());

        assertEquals(StaffTicketCheck.Result.NOT_FOUND, gateService().verifyTicket("CINEORA-unknown").result());
    }

    @Test
    void requiresATicketCode() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gateService().verifyTicket("  "));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void confirmChecksInAValidTicket() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(
                Optional.of(ticket("VALID", "CONFIRMED", SHOW_START, null)),
                Optional.of(ticket("USED", "CONFIRMED", SHOW_START, SHOW_START.minusMinutes(15))));
        when(operationsDAO.checkInTicket(8)).thenReturn(1);

        StaffTicketCheck check = gateService().confirmCheckIn(QR, false);

        assertEquals(StaffTicketCheck.Result.CHECKED_IN, check.result());
        assertEquals("USED", check.ticket().ticketStatus());
    }

    @Test
    void confirmDoesNotCheckInAUsedTicket() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(Optional.of(
                ticket("USED", "CONFIRMED", SHOW_START, SHOW_START.minusMinutes(20))));

        assertEquals(StaffTicketCheck.Result.ALREADY_USED, gateService().confirmCheckIn(QR, false).result());
        verify(operationsDAO, never()).checkInTicket(anyInt());
    }

    @Test
    void confirmReportsTicketAdmittedByAnotherGateMeanwhile() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(
                Optional.of(ticket("VALID", "CONFIRMED", SHOW_START, null)),
                Optional.of(ticket("USED", "CONFIRMED", SHOW_START, SHOW_START.minusMinutes(16))));
        when(operationsDAO.checkInTicket(8)).thenReturn(0);

        assertEquals(StaffTicketCheck.Result.ALREADY_USED, gateService().confirmCheckIn(QR, false).result());
    }

    private static StaffTicketCheck.Ticket restrictedTicket(String ageRating) {
        return new StaffTicketCheck.Ticket(8, QR, "VALID", null, "BK8", "CONFIRMED",
                "Night Shift", ageRating, "Cinema 02", "A1", "STANDARD", SHOW_START, SHOW_START.plusMinutes(105));
    }

    @Test
    void flagsAgeRestrictedTicketForAnIdCheck() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(Optional.of(restrictedTicket("T18")));

        StaffTicketCheck check = gateService().verifyTicket(QR);

        assertEquals(StaffTicketCheck.Result.VALID, check.result());
        assertTrue(check.ageCheckRequired());
        assertEquals("T18 - Restricted to persons aged 18 and above", check.ageNotice());
    }

    @Test
    void allAgesTicketNeedsNoIdCheck() {
        when(operationsDAO.findTicketForCheckIn(QR, null))
                .thenReturn(Optional.of(ticket("VALID", "CONFIRMED", SHOW_START, null)));

        assertFalse(gateService().verifyTicket(QR).ageCheckRequired());
    }

    @Test
    void describesEachAgeRating() {
        assertEquals("T13 - Restricted to persons aged 13 and above", StaffOperationsService.ageNotice(" t13 "));
        assertEquals("K - Viewers under 13 must be accompanied by a parent or guardian",
                StaffOperationsService.ageNotice("K"));
        assertNull(StaffOperationsService.ageNotice("P"));
        assertNull(StaffOperationsService.ageNotice(null));
    }

    @Test
    void confirmRequiresAgeConfirmationForRestrictedMovie() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(Optional.of(restrictedTicket("T16")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> gateService().confirmCheckIn(QR, false));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(operationsDAO, never()).checkInTicket(anyInt());
    }

    @Test
    void confirmChecksInRestrictedMovieAfterAgeConfirmation() {
        when(operationsDAO.findTicketForCheckIn(QR, null)).thenReturn(Optional.of(restrictedTicket("T16")));
        when(operationsDAO.checkInTicket(8)).thenReturn(1);

        assertEquals(StaffTicketCheck.Result.CHECKED_IN, gateService().confirmCheckIn(QR, true).result());
    }
}
