package com.cinema.service;

import com.cinema.dao.FBItemDao;
import com.cinema.exception.FBItemNotFoundException;
import com.cinema.exception.InvalidFBItemException;
import com.cinema.model.FBItem;
import com.cinema.dto.FBItemForm;
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
public class FBItemServiceTest {

    @Mock
    private FBItemDao fbItemDao;

    @InjectMocks
    private FBItemService fbItemService;

    private FBItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = new FBItem(1, "CB-SOLO", "Combo Solo", "Combo",
                "1 Bắp + 1 Nước", new BigDecimal("79000"), new BigDecimal("22000"),
                "https://image.png", "Active");
    }

    @Test
    void testGetAllItems() {
        when(fbItemDao.findAll("Solo", "Combo", "Active")).thenReturn(List.of(sampleItem));

        List<FBItem> result = fbItemService.getAllItems("Solo", "Combo", "Active");
        assertEquals(1, result.size());
        assertEquals("CB-SOLO", result.get(0).getItemCode());
    }

    @Test
    void testGetItemById_Success() {
        when(fbItemDao.findById(1)).thenReturn(Optional.of(sampleItem));

        FBItem result = fbItemService.getItemById(1);
        assertNotNull(result);
        assertEquals(1, result.getItemId());
    }

    @Test
    void testGetItemById_NotFound() {
        when(fbItemDao.findById(99)).thenReturn(Optional.empty());

        assertThrows(FBItemNotFoundException.class, () -> fbItemService.getItemById(99));
    }

    @Test
    void testCreateItem_DuplicateCode_ThrowsException() {
        FBItemForm form = new FBItemForm();
        form.setItemCode("CB-SOLO");
        form.setItemName("Combo Solo New");
        form.setSellingPrice(new BigDecimal("80000"));
        form.setCostPrice(new BigDecimal("25000"));

        when(fbItemDao.findByCode("CB-SOLO")).thenReturn(Optional.of(sampleItem));

        assertThrows(InvalidFBItemException.class, () -> fbItemService.createItem(form));
    }

    @Test
    void testCreateItem_NegativePrice_ThrowsException() {
        FBItemForm form = new FBItemForm();
        form.setItemCode("CB-NEW");
        form.setItemName("Combo New");
        form.setSellingPrice(new BigDecimal("-1000"));
        form.setCostPrice(new BigDecimal("20000"));

        assertThrows(InvalidFBItemException.class, () -> fbItemService.createItem(form));
    }

    @Test
    void testUpdatePrice_Success() {
        when(fbItemDao.findById(1)).thenReturn(Optional.of(sampleItem));
        when(fbItemDao.updatePrice(eq(1), any())).thenReturn(1);

        FBItem updated = fbItemService.updatePrice(1, new BigDecimal("85000"));
        assertNotNull(updated);
        verify(fbItemDao).updatePrice(1, new BigDecimal("85000"));
    }

    @Test
    void testDeactivateItem_Success() {
        when(fbItemDao.findById(1)).thenReturn(Optional.of(sampleItem));
        when(fbItemDao.updateStatus(1, "Inactive")).thenReturn(1);

        FBItem deactivated = fbItemService.deactivateItem(1);
        assertNotNull(deactivated);
        verify(fbItemDao).updateStatus(1, "Inactive");
    }

    @Test
    void testDeleteItem_Success() {
        when(fbItemDao.findById(1)).thenReturn(Optional.of(sampleItem));
        when(fbItemDao.isReferencedInOrders(1)).thenReturn(false);
        when(fbItemDao.isComponentOfAnyCombo(1)).thenReturn(false);
        when(fbItemDao.delete(1)).thenReturn(1);

        assertDoesNotThrow(() -> fbItemService.deleteItem(1));
        verify(fbItemDao).delete(1);
    }

    @Test
    void testDeleteItem_ReferencedInOrders_ThrowsExceptionAndDeactivates() {
        when(fbItemDao.findById(1)).thenReturn(Optional.of(sampleItem));
        when(fbItemDao.isReferencedInOrders(1)).thenReturn(true);

        InvalidFBItemException ex = assertThrows(InvalidFBItemException.class, () -> fbItemService.deleteItem(1));
        assertTrue(ex.getMessage().contains("đã phát sinh trong đơn hàng"));
        verify(fbItemDao).updateStatus(1, "Inactive");
        verify(fbItemDao, never()).delete(1);
    }

    @Test
    void testDeleteItem_ComponentOfCombo_ThrowsException() {
        when(fbItemDao.findById(1)).thenReturn(Optional.of(sampleItem));
        when(fbItemDao.isReferencedInOrders(1)).thenReturn(false);
        when(fbItemDao.isComponentOfAnyCombo(1)).thenReturn(true);

        InvalidFBItemException ex = assertThrows(InvalidFBItemException.class, () -> fbItemService.deleteItem(1));
        assertTrue(ex.getMessage().contains("đang là thành phần của một gói Combo"));
        verify(fbItemDao, never()).delete(1);
    }
}
