package com.cinema.service;

import com.cinema.model.Account;
import com.cinema.model.CustomerAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountJsonSecurityTest {
    @Test void accountResponsesNeverExposePasswordHashes() throws Exception {
        Account account = new Account();
        account.setUsername("staffuser");
        account.setPasswordHash("secret-hash");
        CustomerAccount customer = new CustomerAccount();
        customer.setUsername("customeruser");
        customer.setPasswordHash("secret-hash");
        ObjectMapper mapper = new ObjectMapper();
        for (Object model : new Object[]{account, customer}) {
            String json = mapper.writeValueAsString(model);
            assertFalse(json.contains("passwordHash"));
            assertFalse(json.contains("secret-hash"));
            assertTrue(json.contains("username"));
        }
        assertEquals("secret-hash",account.getPasswordHash());
        assertEquals("secret-hash",customer.getPasswordHash());
    }
}
