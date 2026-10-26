package com.cinema.service;
import com.cinema.dao.*;
import com.cinema.model.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RolePermissionServiceTest {
    RoleDAO roles = mock(RoleDAO.class);
    PermissionDAO permissions = mock(PermissionDAO.class);
    RolePermissionService service = new RolePermissionService(roles, permissions);
    Permission read = new Permission(1, "movie:read", "Read");
    Permission book = new Permission(2, "booking:create", "Book");
    Permission manage = new Permission(3, "account:manage", "Manage");
    @BeforeEach void setup() {
        when(roles.findById(9)).thenReturn(Optional.of(new Role(9, "CUSTOMER", "Customer")));
        when(permissions.findAll()).thenReturn(List.of(read, book, manage));
        when(permissions.findAllByRoleId(9)).thenReturn(List.of(read));
        when(permissions.updateRolePermissions(anyInt(), anyList())).thenReturn(1);
    }
    @Test void rejectsInternalPermissionEvenWhenCalledDirectly() {
        assertEquals("INVALID_ROLE_CONFIGURATION", service.addPermission(9, "account:manage").getCode());
        assertEquals("INVALID_ROLE_CONFIGURATION", service.updateRolePermissions(9, Set.of("movie:read", "account:manage")).getCode());
        verify(permissions, never()).updateRolePermissions(anyInt(), anyList());
    }
    @Test void retainsBasicPublicBrowsingPermission() {
        assertEquals("MANDATORY_PERMISSION", service.removePermission(9, "movie:read").getCode());
        verify(permissions, never()).updateRolePermissions(anyInt(), anyList());
    }
    @Test void addingBookingReportsActualDifference() {
        var result = service.addPermission(9, "booking:create");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("booking:create"), result.getAddedPermissions());
        assertTrue(result.getRemovedPermissions().isEmpty());
        verify(roles, atLeastOnce()).lockById(9);
    }
    @Test void removingBookingReportsActualDifference() {
        when(permissions.findAllByRoleId(9)).thenReturn(List.of(read, book));
        var result = service.removePermission(9, "booking:create");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("booking:create"), result.getRemovedPermissions());
    }
    @Test void databaseFailureRollsBackThePermissionTransaction() throws Exception {
        var source = mock(javax.sql.DataSource.class);
        var connection = mock(java.sql.Connection.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        var transactions = new org.springframework.jdbc.datasource.DataSourceTransactionManager(source);
        var factory = new org.springframework.aop.framework.ProxyFactory(service);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var transactionalService = (RolePermissionService) factory.getProxy();
        when(permissions.updateRolePermissions(anyInt(), anyList()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("insert failed"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> transactionalService.addPermission(9, "booking:create"));
        verify(connection).rollback();
        verify(connection, never()).commit();
    }
    @Test void exposesScopeToUi() {
        var role = service.getRoleWithPermissions(9).orElseThrow();
        assertEquals(Set.of("movie:read", "booking:create"), role.allowedPermissions());
        assertEquals(Set.of("movie:read"), role.mandatoryPermissions());
    }
    @Test void seedsNewPermissionsOnceAndDoesNotRestoreRevokedOptionalPermission() {
        Role staff = new Role(8, "STAFF", "Staff");
        Permission checkin = new Permission(4, "ticket:checkin", "Check in");
        Permission news = new Permission(5, "news:manage", "Write news");
        when(roles.findAll()).thenReturn(List.of(staff));
        when(permissions.findAll()).thenReturn(List.of(read, book, manage, checkin, news));
        when(permissions.findAllByRoleId(8)).thenReturn(List.of(read, checkin));
        when(permissions.insertIfMissing(eq("news:manage"), anyString())).thenReturn(true);
        service.removeInvalidAssignments();
        verify(permissions).updateRolePermissions(eq(8), argThat(ids -> ids.size() == 3 && ids.containsAll(List.of(1, 4, 5))));
        clearInvocations(permissions);
        when(permissions.insertIfMissing(eq("news:manage"), anyString())).thenReturn(false);
        service.removeInvalidAssignments();
        verify(permissions, never()).updateRolePermissions(anyInt(), anyList());
    }
    @Test void startupRemovesOnlyInvalidGrantsAndPreservesOptionalChoices() {
        when(roles.findAll()).thenReturn(List.of(new Role(9, "CUSTOMER", "Customer")));
        when(permissions.findAllByRoleId(9)).thenReturn(List.of(read, manage));
        service.removeInvalidAssignments();
        verify(permissions).updateRolePermissions(9, List.of(1));
    }
}
