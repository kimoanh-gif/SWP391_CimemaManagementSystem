package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.model.Movie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HomePageRenderingTest {
    @ParameterizedTest
    @ValueSource(strings = {"GUEST", "PASSWORD", "OAUTH_GOOGLE"})
    void homeRendersForGuestAndBothCustomerLoginMethods(String method) {
        var servlet = new MockServletContext();
        var request = new MockHttpServletRequest(servlet);
        request.setRequestURI("/");
        var exchange = JakartaServletWebApplication.buildApplication(servlet)
                .buildExchange(request, new MockHttpServletResponse());
        var resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var movie = new Movie();
        movie.setMovieId(1);
        movie.setTitle("QA Movie");
        movie.setDuration(120);
        movie.setReleaseDate(LocalDate.now());
        movie.setStatus("NOW_SHOWING");
        var variables = new HashMap<String,Object>();
        for (var key : List.of("nowShowingMovies", "featuredMovies", "topGrossingMovies", "allMovies"))
            variables.put(key, List.of(movie));
        for (var key : List.of("upcomingMovies", "todayShowtimes", "promotions", "newsList"))
            variables.put(key, List.of());
        variables.put("upcomingDates", List.of(LocalDate.now(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));
        variables.put("activeNav", "home");
        variables.put("_csrf", Map.of("token", "test", "parameterName", "_csrf"));
        if (!method.equals("GUEST")) {
            variables.put("currentUser", new AuthenticatedUser(42,"qa.customer","qa@example.com","QA Customer",null,
                    "CUSTOMER", Set.of("movie:read"),true,method));
            variables.put("availablePoints",120);
        }
        String html = engine.process("index", new WebContext(exchange, Locale.ENGLISH, variables));
        assertTrue(html.contains("QA Movie"));
        if (!method.equals("GUEST")) assertTrue(html.contains("QA Customer"));
    }
}
