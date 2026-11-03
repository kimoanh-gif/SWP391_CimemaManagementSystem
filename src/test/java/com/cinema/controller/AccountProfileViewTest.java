package com.cinema.controller;

import com.cinema.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountProfileViewTest {
    @ParameterizedTest @ValueSource(strings={"MANAGER","STAFF","CUSTOMER"})
    void allProfileViewsRenderSharedOtpAndPasswordControls(String role) throws Exception {
        var servlet=new MockServletContext();var request=new MockHttpServletRequest(servlet);request.setRequestURI("/"+role.toLowerCase()+"/profile");
        var exchange=JakartaServletWebApplication.buildApplication(servlet).buildExchange(request,new MockHttpServletResponse());
        var resolver=new FileTemplateResolver();resolver.setPrefix("src/main/resources/templates/");resolver.setSuffix(".html");resolver.setCharacterEncoding("UTF-8");
        var engine=new SpringTemplateEngine();engine.setTemplateResolver(resolver);
        var variables=new HashMap<String,Object>();
        var account=new Account();account.setAccountId(42);account.setUsername("qa.user");account.setFullName("QA User");account.setEmail("qa@example.com");account.setStatus("ACTIVE");account.setPasswordHash("do-not-expose-this-hash");
        variables.put("account",account);variables.put("roleName",role);variables.put("dashboardUrl","/"+role.toLowerCase()+"/dashboard");
        variables.put("passwordMinLength",12);variables.put("_csrf",Map.of("headerName","X-XSRF-TOKEN","token","test-token","parameterName","_csrf"));
        variables.put("permissionUser",new AuthenticatedUser(42,"qa.user","qa@example.com","QA User",null,role,Set.of(),true,"PASSWORD"));
        if(role.equals("CUSTOMER")) {
            var profile=new CustomerProfile();profile.setFullName("QA User");profile.setUsername("qa.user");profile.setEmail("qa@example.com");profile.setMembershipRankName("Member");
            variables.put("profile",profile);variables.put("currentUser",new AuthenticatedUser(42,"qa.user","qa@example.com","QA User",null,"CUSTOMER",Set.of(),true,"PASSWORD"));
        }
        String html=engine.process(role.equals("CUSTOMER")?"customer/profile":"account/profile",new WebContext(exchange,Locale.ENGLISH,variables));
        assertTrue(html.contains("qa@example.com"));assertTrue(html.contains("id=\"account-security\""));
        assertTrue(html.contains("data-min-length=\"12\""));assertTrue(html.contains("id=\"security-otp\""));
        assertTrue(html.contains("/js/account-security.js"));assertFalse(html.contains("do-not-expose-this-hash"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/profile-"+role.toLowerCase()+"-preview.html"),html);
    }
}
