package com.cinema.controller;

import com.cinema.model.AuthenticatedUser;
import com.cinema.service.MovieCatalogService;
import com.cinema.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieReviewControllerTest {

    @Mock
    private MovieCatalogService movieService;

    @Mock
    private SessionService sessionService;

    @Mock
    private HttpSession session;

    private MovieReviewController controller;

    @BeforeEach
    void setUp() {
        controller = new MovieReviewController(movieService, sessionService);
    }

    private AuthenticatedUser customer() {
        AuthenticatedUser user = mock(AuthenticatedUser.class);
        when(user.getRole()).thenReturn("CUSTOMER");
        return user;
    }

    @Test
    void testGuestIsRedirectedToLogin() {
        when(sessionService.getCurrentUser(session)).thenReturn(null);

        String view = controller.submitReview(5, 5, "Great", session, new RedirectAttributesModelMap());

        assertEquals("redirect:/login?redirect=/movies/5", view);
        verify(movieService, never()).addReview(anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void testInvalidRatingIsRejected() {
        AuthenticatedUser user = customer();
        when(sessionService.getCurrentUser(session)).thenReturn(user);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String view = controller.submitReview(5, 9, "Great", session, ra);

        assertEquals("redirect:/movies/5#reviews", view);
        assertTrue(ra.getFlashAttributes().containsKey("errorMessage"));
        verify(movieService, never()).addReview(anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void testValidReviewIsSaved() {
        AuthenticatedUser user = customer();
        when(user.getAccountId()).thenReturn(7);
        when(sessionService.getCurrentUser(session)).thenReturn(user);
        when(movieService.addReview(7, 5, 4, "Great movie")).thenReturn(true);

        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();
        String view = controller.submitReview(5, 4, "  Great movie ", session, ra);

        assertEquals("redirect:/movies/5#reviews", view);
        assertTrue(ra.getFlashAttributes().containsKey("successMessage"));
    }
}
