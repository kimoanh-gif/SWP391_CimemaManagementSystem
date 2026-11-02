package com.cinema.security;

import com.cinema.config.SecurityConfig;
import com.cinema.controller.AccountProfileController;
import com.cinema.dao.AccountDAO;
import com.cinema.model.AuthenticatedUser;
import com.cinema.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfilePasswordAccessTest {
    private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
        return request -> {
            var token=new org.springframework.security.web.csrf.DefaultCsrfToken("X-XSRF-TOKEN","_csrf","qa-csrf-token");
            new org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler()
                    .handle(request,new MockHttpServletResponse(),()->token);
            var masked=(org.springframework.security.web.csrf.CsrfToken)request.getAttribute("_csrf");
            request.setCookies(new jakarta.servlet.http.Cookie("XSRF-TOKEN",token.getToken()));
            request.addHeader(token.getHeaderName(),masked.getToken());
            return request;
        };
    }
    AnnotationConfigWebApplicationContext context;MockMvc mvc;
    @BeforeEach void setup() {
        context=new AnnotationConfigWebApplicationContext();context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
        clearInvocations(context.getBean(AuthService.class));
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build();
    }
    @AfterEach void close(){context.close();}
    @ParameterizedTest @ValueSource(strings={"ADMIN","MANAGER","STAFF","CUSTOMER"})
    void everyRoleCanRequestOwnOtpWithoutManagementPermission(String role)throws Exception{
        var session=new MockHttpSession();var user=new AuthenticatedUser(42,"user","own@example.com","Own",null,role,Set.of(),true,"PASSWORD");
        session.setAttribute(SessionService.ATTR_USER,user);when(context.getBean(AuthorizationSnapshotService.class).refresh(session)).thenReturn(user);
        when(context.getBean(SessionService.class).currentAccountId(any())).thenReturn(42);
        when(context.getBean(AuthService.class).requestProfilePasswordOtp(42)).thenReturn(AuthService.RegisterResult.ok());
        mvc.perform(post("/account/password/request").session(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
    @Test void csrfIsRequiredAndGuestCannotUsePersonalApi()throws Exception{
        mvc.perform(post("/account/password/request")).andExpect(status().isForbidden());
        mvc.perform(post("/account/password/request").with(csrf())).andExpect(status().is3xxRedirection());
        verifyNoInteractions(context.getBean(AuthService.class));
    }
    @Configuration @EnableWebMvc @Import(SecurityConfig.class)
    static class Config {
        @Bean SystemSecuritySettings settings(){
            var settings=mock(SystemSecuritySettings.class);
            when(settings.read(anyString(),anyInt())).thenAnswer(call->call.getArgument(1));
            return settings;
        }
        @Bean AuthorizationSnapshotService snapshots(){return mock(AuthorizationSnapshotService.class);}
        @Bean SessionAuthenticationFilter filter(AuthorizationSnapshotService snapshots){return new SessionAuthenticationFilter(snapshots);}
        @Bean SessionService sessions(){return mock(SessionService.class);}
        @Bean AuthService auth(){return mock(AuthService.class);}
        @Bean AccountDAO accounts(){return mock(AccountDAO.class);}
        @Bean OtpService otp(){return mock(OtpService.class);}
        @Bean AccountProfileController controller(AccountDAO a,SessionService s,AuthService auth,OtpService otp){return new AccountProfileController(a,s,auth,otp);}
    }
}
