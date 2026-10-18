package com.cinema.service;

import com.cinema.dao.StaffNewsDAO;
import com.cinema.dao.StaffOperationsDAO;
import com.cinema.model.StaffBookingPage;
import com.cinema.model.StaffDashboardData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    void limitsStaffDashboardAndBookingQueriesToTheActiveStaffProfile() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));
        when(operationsDAO.getDashboardData(6, false)).thenReturn(new StaffDashboardData(3, 1, List.of()));
        when(operationsDAO.countBookings(6, false, "BK", "PENDING")).thenReturn(0L);
        when(operationsDAO.findBookings(6, false, "BK", "PENDING", 0, StaffOperationsService.PAGE_SIZE))
                .thenReturn(List.of());

        service.dashboard(15, "STAFF");
        StaffBookingPage page = service.bookings(15, "STAFF", -4, " BK ", "pending");

        assertEquals(0, page.page());
        assertEquals("BK", page.query());
        assertEquals("PENDING", page.status());
        verify(operationsDAO).getDashboardData(6, false);
        verify(operationsDAO).findBookings(6, false, "BK", "PENDING", 0, StaffOperationsService.PAGE_SIZE);
    }

    @Test
    void rejectsUnknownBookingStatusFilter() {
        when(staffNewsDAO.findActiveStaffId(15)).thenReturn(Optional.of(6));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.bookings(15, "STAFF", 0, null, "not-a-status"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(operationsDAO);
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
