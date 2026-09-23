package com.pantry.identity;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import com.pantry.shared.PantryProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guest-sessions")
class GuestSessionController {
    static final String COOKIE_NAME = "PANTRY_GUEST";
    private final GuestSessionService service;
    private final PantryProperties properties;

    GuestSessionController(GuestSessionService service, PantryProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    GuestSessionResponse create(HttpServletResponse response) {
        GuestSessionService.IssuedGuestSession issued = service.create();
        Cookie cookie = new Cookie(COOKIE_NAME, issued.token());
        cookie.setHttpOnly(true);
        cookie.setSecure(properties.secureCookie());
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.between(Instant.now(), issued.session().expiresAt()).toSeconds());
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return new GuestSessionResponse(issued.session().id(), issued.session().expiresAt());
    }

    @GetMapping("/current")
    GuestSessionResponse current(@CookieValue(value = COOKIE_NAME, required = false) String token) {
        GuestSession current = service.requireActive(requireToken(token));
        return new GuestSessionResponse(current.id(), current.expiresAt());
    }

    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CookieValue(value = COOKIE_NAME, required = false) String token, HttpServletResponse response) {
        service.delete(requireToken(token));
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(properties.secureCookie());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private String requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new GuestSessionNotFoundException("Guest session is missing or expired");
        }
        return token;
    }

    record GuestSessionResponse(UUID id, Instant expiresAt) {}
}
