package com.cinema.job;

import com.cinema.dao.OtpTokenDAO;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CleanupExpiredOtpJobTest {
    @Test void cleanupRunsOnlyOneBoundedBatchEvenWhenItIsFull() {
        var tokens = mock(OtpTokenDAO.class);
        when(tokens.deleteExpiredHistoryBatch()).thenReturn(1000);
        new CleanupExpiredOtpJob(tokens).cleanup();
        verify(tokens, times(1)).deleteExpiredHistoryBatch();
        verifyNoMoreInteractions(tokens);
    }

    @Test void databaseFailureEscapesForTransactionRollback() {
        var tokens = mock(OtpTokenDAO.class);
        when(tokens.deleteExpiredHistoryBatch()).thenThrow(new DataAccessResourceFailureException("offline"));
        assertThrows(DataAccessResourceFailureException.class, () -> new CleanupExpiredOtpJob(tokens).cleanup());
        verify(tokens, times(1)).deleteExpiredHistoryBatch();
    }

    @Test void deletionPreservesUnexpiredAndRecentRateLimitOrLockoutHistory() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString())).thenReturn(23);
        assertEquals(23, new OtpTokenDAO(jdbc).deleteExpiredHistoryBatch());
        var sql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture());
        String query = sql.getValue();
        assertTrue(query.contains("DELETE TOP (1000) FROM dbo.OtpToken"));
        assertTrue(query.contains("ExpiresAt < DATEADD(DAY, -7, SYSDATETIME())"));
        assertTrue(query.contains("CreatedAt < DATEADD(DAY, -7, SYSDATETIME())"));
        assertTrue(query.contains("LastSentAt IS NULL OR LastSentAt < DATEADD(DAY, -7, SYSDATETIME())"));
        assertTrue(query.contains("ConsumedAt IS NULL OR ConsumedAt < DATEADD(DAY, -7, SYSDATETIME())"));
        verifyNoMoreInteractions(jdbc);
    }
}
