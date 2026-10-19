package com.cinema.controller.api;

import com.cinema.model.FBItem;
import com.cinema.service.FBItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class FBItemApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FBItemService fbItemService;

    @Test
    void testGetAllItems_AuthorizedManager() throws Exception {
        FBItem item = new FBItem(1, "CB-SOLO", "Combo Solo", "Combo",
                "1 Bắp + 1 Nước", new BigDecimal("79000"), new BigDecimal("22000"),
                "https://image.png", "Active");

        when(fbItemService.getAllItems(any(), any(), any())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/manager/fb")
                        .header("X-User-Role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].itemCode").value("CB-SOLO"));
    }

    @Test
    void testGetAllItems_ForbiddenNonManager() throws Exception {
        mockMvc.perform(get("/api/manager/fb")
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteItemApi_AuthorizedManager() throws Exception {
        doNothing().when(fbItemService).deleteItem(1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/manager/fb/1")
                        .header("X-User-Role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testDeleteItemApi_ForbiddenNonManager() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/manager/fb/1")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }
}
