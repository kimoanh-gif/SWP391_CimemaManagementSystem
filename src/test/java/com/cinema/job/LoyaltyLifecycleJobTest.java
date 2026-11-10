package com.cinema.job;

import com.cinema.dao.LoyaltyPointDAO;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoyaltyLifecycleJobTest {

    @Test
    void lifecycleExpiresPointBatchesAndStartsTheCurrentTierCycle() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LoyaltyPointDAO loyaltyPoints = mock(LoyaltyPointDAO.class);

        new LoyaltyLifecycleJob(jdbc, loyaltyPoints).applyLifecycle();

        verify(jdbc).update(contains("TransactionType='EARN'"));
        verify(loyaltyPoints).reviewTierCycle(null);
    }
}
