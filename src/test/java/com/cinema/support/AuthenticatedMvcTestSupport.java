package com.cinema.support;
import com.cinema.model.AuthenticatedUser;
import com.cinema.service.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Set;
import static org.mockito.Mockito.when;
/** Session authentication fixture; role headers must never authenticate a user. */
public abstract class AuthenticatedMvcTestSupport {
    @MockitoBean protected AuthorizationSnapshotService authorizationSnapshots;
    protected MockHttpSession authSession(String role) {
        var user = new AuthenticatedUser(1, "test", "test@example.com", "Test User", null, role,
                com.cinema.security.RolePermissionPolicy.allowed(role), true, "PASSWORD");
        var session = new MockHttpSession(); session.setAttribute(SessionService.ATTR_USER, user);
        when(authorizationSnapshots.refresh(session)).thenReturn(user);
        return session;
    }
}
