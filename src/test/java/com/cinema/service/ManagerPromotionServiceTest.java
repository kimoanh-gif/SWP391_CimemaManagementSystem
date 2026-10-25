package com.cinema.service;

import com.cinema.dao.ManagerPromotionDAO;
import com.cinema.dao.ManagerVoucherDAO;
import com.cinema.dto.PromotionForm;
import com.cinema.exception.PromotionAdminException;
import com.cinema.model.AdminPage;
import com.cinema.model.ManagerPromotionFilter;
import com.cinema.model.ManagerPromotionRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UC33 M11 – promotion campaigns. Assignee: Nguyễn Đức Anh */
@ExtendWith(MockitoExtension.class)
class ManagerPromotionServiceTest {

    @Mock
    private ManagerPromotionDAO promotionDAO;
    @Mock
    private ManagerVoucherDAO voucherDAO;

    private ManagerPromotionService service;

    @BeforeEach
    void setUp() {
        service = new ManagerPromotionService(promotionDAO, voucherDAO);
    }

    @Test
    void createValidPromotionInsertsIt() {
        when(promotionDAO.insert(any(PromotionForm.class))).thenReturn(7);
        PromotionForm form = form("  Summer Deal  ", LocalDateTime.now(), LocalDateTime.now().plusDays(30));

        assertEquals(7, service.create(form));
        assertEquals("Summer Deal", form.getName());
        assertEquals("ACTIVE", form.getStatus());
    }

    @Test
    void createRejectsEndBeforeStartAndMissingName() {
        PromotionForm form = form(" ", LocalDateTime.now(), LocalDateTime.now().minusDays(1));

        PromotionAdminException ex = assertThrows(PromotionAdminException.class, () -> service.create(form));
        assertTrue(ex.getFieldErrors().containsKey("name"));
        assertTrue(ex.getFieldErrors().containsKey("endDate"));
        verify(promotionDAO, never()).insert(any());
    }

    @Test
    void createCannotStartAsPausedOrStopped() {
        when(promotionDAO.insert(any(PromotionForm.class))).thenReturn(1);
        PromotionForm form = form("Deal", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        form.setStatus("INACTIVE");

        service.create(form);
        assertEquals("ACTIVE", form.getStatus());
    }

    @Test
    void stoppedPromotionCannotBeReactivated() {
        when(promotionDAO.findById(3)).thenReturn(Optional.of(row(3, "INACTIVE", 0)));

        assertThrows(PromotionAdminException.class, () -> service.changeStatus(3, "ACTIVE"));
        verify(promotionDAO, never()).updateStatus(anyInt(), anyString());
    }

    @Test
    void activePromotionCanBePaused() {
        when(promotionDAO.findById(3)).thenReturn(Optional.of(row(3, "ACTIVE", 0)));

        service.changeStatus(3, "paused");
        verify(promotionDAO).updateStatus(3, "PAUSED");
    }

    @Test
    void cannotActivateWhenEndDatePassed() {
        ManagerPromotionRow r = row(3, "PAUSED", 0);
        r.setEndDate(LocalDateTime.now().minusDays(1));
        when(promotionDAO.findById(3)).thenReturn(Optional.of(r));

        assertThrows(PromotionAdminException.class, () -> service.changeStatus(3, "ACTIVE"));
    }

    @Test
    void usedPromotionCannotBeDeleted() {
        when(promotionDAO.findById(4)).thenReturn(Optional.of(row(4, "ACTIVE", 12)));

        PromotionAdminException ex = assertThrows(PromotionAdminException.class, () -> service.delete(4));
        assertTrue(ex.getMessage().contains("12 bookings"));
        verify(promotionDAO, never()).deleteIfUnused(anyInt());
    }

    @Test
    void unusedPromotionIsDeleted() {
        when(promotionDAO.findById(5)).thenReturn(Optional.of(row(5, "DRAFT", 0)));
        when(promotionDAO.deleteIfUnused(5)).thenReturn(1);

        service.delete(5);
        verify(promotionDAO).deleteIfUnused(5);
    }

    @Test
    void searchFiltersByKeywordAndStatusAndPages() {
        ManagerPromotionRow a = row(1, "ACTIVE", 0);
        a.setName("Wednesday Student");
        ManagerPromotionRow b = row(2, "PAUSED", 0);
        b.setName("Wednesday Night");
        ManagerPromotionRow c = row(3, "ACTIVE", 0);
        c.setName("Summer");
        when(promotionDAO.findAll()).thenReturn(List.of(a, b, c));

        AdminPage<ManagerPromotionRow> page = service.search(ManagerPromotionFilter.of("wednesday", "ACTIVE", null, 1, 6));
        assertEquals(1, page.totalItems());
        assertEquals(1, page.items().get(0).getPromotionId());
    }

    @Test
    void countTextsUseSingularPluralAndThousandsSeparator() {
        ManagerPromotionRow r = row(1, "ACTIVE", 1);
        r.setVoucherCount(2);
        assertEquals("1 booking", r.getBookingCountText());
        assertEquals("2 codes", r.getVoucherCountText());
        assertEquals("+1 more code", r.getMoreCodesText());
        assertEquals("1,840 bookings", ManagerPromotionRow.count(1840, "booking", "bookings"));
    }

    private static PromotionForm form(String name, LocalDateTime start, LocalDateTime end) {
        PromotionForm f = new PromotionForm();
        f.setName(name);
        f.setStartDate(start);
        f.setEndDate(end);
        return f;
    }

    static ManagerPromotionRow row(int id, String status, int references) {
        ManagerPromotionRow r = new ManagerPromotionRow();
        r.setPromotionId(id);
        r.setName("Promotion " + id);
        r.setStatus(status);
        r.setStartDate(LocalDateTime.now().minusDays(10));
        r.setEndDate(LocalDateTime.now().plusDays(30));
        r.setReferenceCount(references);
        r.setUsageCount(references);
        return r;
    }
}
