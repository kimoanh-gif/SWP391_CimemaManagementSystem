package com.cinema.controller;
import com.cinema.dao.*;
import com.cinema.model.*;
import com.cinema.service.RolePermissionService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RolePermissionApiTest {
    RoleDAO roles; PermissionDAO permissions; MockMvc mvc;
    @BeforeEach void setup() {
        roles=mock(RoleDAO.class);permissions=mock(PermissionDAO.class);
        when(roles.findById(1)).thenReturn(Optional.of(new Role(1,"CUSTOMER","Customer")));
        var read = new Permission(1,"movie:read","Read");
        when(permissions.findAll()).thenReturn(List.of(read,new Permission(2,"booking:create","Book"),new Permission(3,"account:manage","Manage")));
        when(permissions.findAllByRoleId(1)).thenReturn(List.of(read));
        when(permissions.updateRolePermissions(anyInt(),anyList())).thenReturn(1);
        var controller = new RolePermissionApiController();
        ReflectionTestUtils.setField(controller,"service",new RolePermissionService(roles,permissions));
        mvc=MockMvcBuilders.standaloneSetup(controller).build();
    }
    @Test void rejectsMissingBlankUnknownAndOutOfScopeGrantsWithoutSaving() throws Exception {
        for(String json: List.of("{}","{\"permissions\":null}","{\"permissions\":[null]}","{\"permissions\":[\"\"]}",
                "{\"permissions\":[\"movie:read\",\"unknown\"]}","{\"permissions\":[\"movie:read\",\"account:manage\"]}","{\"permissions\":[]}")) {
            mvc.perform(put("/api/admin/roles/1/permissions").contentType("application/json").content(json)).andExpect(status().isBadRequest());
        }
        verify(permissions,never()).updateRolePermissions(anyInt(),anyList());
    }
    @Test void addIsSavedDuplicateIsRejectedAndMandatoryCannotBeRemoved() throws Exception {
        mvc.perform(post("/api/admin/roles/1/permissions/add").contentType("application/json").content("{\"permissionName\":\"booking:create\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.addedPermissions[0]").value("booking:create"));
        verify(permissions).updateRolePermissions(eq(1),argThat(ids -> ids.size()==2 && ids.containsAll(List.of(1,2))));
        clearInvocations(permissions);
        mvc.perform(post("/api/admin/roles/1/permissions/add").contentType("application/json").content("{\"permissionName\":\"movie:read\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PERMISSION_EXISTS"));
        mvc.perform(post("/api/admin/roles/1/permissions/remove").contentType("application/json").content("{\"permissionName\":\"movie:read\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MANDATORY_PERMISSION"));
        verify(permissions,never()).updateRolePermissions(anyInt(),anyList());
    }
    @Test void optionalPermissionRemovalAndUnknownRoleAreHandled() throws Exception {
        when(permissions.findAllByRoleId(1)).thenReturn(List.of(new Permission(1,"movie:read","Read"),new Permission(2,"booking:create","Book")));
        mvc.perform(post("/api/admin/roles/1/permissions/remove").contentType("application/json").content("{\"permissionName\":\"booking:create\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.removedPermissions[0]").value("booking:create"));
        verify(permissions).updateRolePermissions(1,List.of(1));
        mvc.perform(get("/api/admin/roles/999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/roles/999/permissions").contentType("application/json").content("{\"permissions\":[\"movie:read\"]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ROLE"));
    }
}
