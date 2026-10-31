package com.cinema.job;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoyaltyLifecycleJobTest {

    @Test
    void lifecycleExpiresPointBatchesAndStartsTheCurrentTierCycle() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        new LoyaltyLifecycleJob(jdbc).applyLifecycle();

        verify(jdbc).update(contains("TransactionType='EARN'"));
        verify(jdbc).update(contains("TierPoints=0"));
    }
}
