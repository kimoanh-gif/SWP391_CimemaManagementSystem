package com.cinema.controller;
import com.cinema.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SystemConfigRankApiTest {
    @Test void rejectsMissingNullFractionalStringsNegativeAndOverflowBeforeService() throws Exception {
        var service=mock(SystemConfigService.class);var controller=new SystemConfigApiController();
        ReflectionTestUtils.setField(controller,"service",service);var mapper=new ObjectMapper();
        for(String json:new String[]{"{}","{\"minPoints\":null}","{\"minPoints\":1.5}","{\"minPoints\":\"500\"}","{\"minPoints\":-1}","{\"minPoints\":2147483648}"}) {
            assertEquals(400,controller.updateRankThreshold(2,mapper.readTree(json)).getStatusCode().value());
        }
        verifyNoInteractions(service);
    }
}
