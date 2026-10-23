package com.cinema.service;

import com.cinema.dao.*;
import com.cinema.model.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAccountResetRoutingTest {
    @Test void customerResetUsesEmailedPasswordFlow() {
        CustomerAccountDAO dao=mock(CustomerAccountDAO.class);
        when(dao.findById(42)).thenReturn(Optional.of(new CustomerAccount()));
        AdminPasswordResetService reset=mock(AdminPasswordResetService.class);
        when(reset.reset(42)).thenReturn(new AdminPasswordResetService.Result(true,null,"Emailed"));
        CustomerAccountService service=new CustomerAccountService(dao,reset);
        assertTrue(service.resetPassword(42).isSuccess());verify(reset).reset(42);
        when(reset.reset(42)).thenThrow(new AdminPasswordResetService.DeliveryFailure());
        assertEquals("EMAIL_FAILED",service.resetPassword(42).getCode());
    }
    @Test void staffResetUsesTheSameCooldownAndEmailFlow() {
        AccountDAO dao=mock(AccountDAO.class);RoleDAO roles=mock(RoleDAO.class);
        Account staff=new Account();staff.setRoleId(3);
        when(dao.findById(43)).thenReturn(Optional.of(staff));
        when(roles.findById(3)).thenReturn(Optional.of(new Role(3,"STAFF","Staff")));
        AdminPasswordResetService reset=mock(AdminPasswordResetService.class);
        when(reset.reset(43)).thenReturn(new AdminPasswordResetService.Result(false,"RESET_COOLDOWN","Wait"));
        StaffAccountService service=new StaffAccountService(dao,roles,reset);
        assertEquals("RESET_COOLDOWN",service.resetPassword(43).getCode());verify(reset).reset(43);
        when(reset.reset(43)).thenThrow(new AdminPasswordResetService.DeliveryFailure());
        assertEquals("EMAIL_FAILED",service.resetPassword(43).getCode());
    }
}
