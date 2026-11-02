package com.cinema.dao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromotionSchemaCompatibilityTest {
    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void allPromotionReadsSupportOptionalBannerColumn(boolean hasBanner) {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT COL_LENGTH('dbo.Promotion', 'BannerURL')",Integer.class))
                .thenReturn(hasBanner ? 1000 : null);
        var dao = new PromotionDAO(jdbc);
        dao.findActivePromotions();
        dao.findLatestPromotions(4);
        dao.findPublicById(1);
        dao.findById(1);
        for (var call : mockingDetails(jdbc).getInvocations()) {
            String sql = call.getArgument(0);
            if (!sql.contains("FROM dbo.Promotion p")) continue;
            assertEquals(hasBanner, sql.contains("p.BannerURL"));
            if (!hasBanner) assertTrue(sql.contains("CAST(NULL AS NVARCHAR(500)) AS BannerURL"));
        }
        verify(jdbc,never()).execute(anyString());
    }
}
