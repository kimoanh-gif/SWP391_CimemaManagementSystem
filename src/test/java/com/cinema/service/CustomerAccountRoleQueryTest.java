package com.cinema.service;

import com.cinema.dao.CustomerAccountDAO;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.mockito.Mockito.*;

class CustomerAccountRoleQueryTest {
    @Test void detailAndUpdatesAreRestrictedToCustomerRoleInDatabase() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        CustomerAccountDAO dao = new CustomerAccountDAO(jdbc);
        dao.findById(42);
        verify(jdbc).queryForObject(
                argThat(sql -> sql.contains("a.AccountID = ?") && sql.contains("r.Name") && sql.contains("= 'CUSTOMER'")),
                any(RowMapper.class), eq(42));
        dao.updateAccount(42,"Test User",null,"user@example.com","validuser");
        verify(jdbc).update(argThat(sql -> sql.contains("RoleID IN") && sql.contains("= 'CUSTOMER'")),
                eq("Test User"), isNull(), eq("user@example.com"), eq("validuser"), eq(42));
        dao.suspendAccount(42);
        dao.reactivateAccount(42);
        verify(jdbc, times(2)).update(argThat(sql -> sql.contains("SET Status") && sql.contains("RoleID IN") && sql.contains("= 'CUSTOMER'")), eq(42));
    }
}
