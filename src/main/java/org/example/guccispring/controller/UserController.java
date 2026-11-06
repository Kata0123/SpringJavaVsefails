
package org.example.guccispring.controller;

import org.example.guccispring.dto.Requests;
import org.example.guccispring.model.Models;
import org.example.guccispring.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final Logger log =
            LoggerFactory.getLogger(UserController.class);

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    private <T> T ratingRequest(Supplier<T> action) {
        try {
            return action.get();
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw e;
            }

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Сервис оценок и списка просмотра временно недоступен",
                    e
            );
        } catch (RuntimeException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Сервис оценок и списка просмотра временно недоступен",
                    e
            );
        }
    }

    private void ratingAction(Runnable action) {
        ratingRequest(() -> {
            action.run();
            return null;
        });
    }

    @GetMapping("/session")
    public Map<String, Object> session(CsrfToken csrfToken) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("csrfToken", csrfToken.getToken());

        Optional<Models.AppUser> current = service.currentUser();

        if (current.isEmpty()) {
            result.put("user", null);
            result.put("ratings", Map.of());
            result.put("watchlist", List.of());
            result.put("ratingServiceAvailable", true);
            return result;
        }

        Models.AppUser user = current.get();

        result.put("user", service.publicUser(user));

        try {
            Map<Long, Integer> ratings =
                    service.repository().ratings(user.id());

            List<Long> watchlist =
                    service.repository().watchlist(user.id());

            result.put("ratings", ratings);
            result.put("watchlist", watchlist);
            result.put("ratingServiceAvailable", true);

        } catch (RuntimeException e) {
            log.warn(
                    "Rating service unavailable for user {}",
                    user.id(),
                    e
            );

            result.put("ratings", Map.of());
            result.put("watchlist", List.of());
            result.put("ratingServiceAvailable", false);
        }

        return result;
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(
            @Valid @RequestBody Requests.Register body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return service.publicUser(
                service.register(body, request, response)
        );
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(
            @Valid @RequestBody Requests.Login body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return service.publicUser(
                service.login(body, request, response)
        );
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        new SecurityContextLogoutHandler().logout(
                request,
                response,
                authentication
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    public Map<String, Object> rename(
            @Valid @RequestBody Requests.Rename body,
            HttpSession session
    ) {
        return service.publicUser(
                service.rename(session, body)
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        HttpSession session = request.getSession(false);

        service.deleteAccount(session);
        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/ratings")
    public Map<Long, Integer> ratings(HttpSession session) {
        long userId = service.authenticated(session).id();

        return ratingRequest(
                () -> service.repository().ratings(userId)
        );
    }

    @PutMapping("/me/ratings/{mediaId}")
    public ResponseEntity<Void> rate(
            @PathVariable long mediaId,
            @Valid @RequestBody Requests.Rating body,
            HttpSession session
    ) {
        long userId = service.authenticated(session).id();

        service.checkMedia(mediaId);

        ratingAction(() ->
                service.repository().rate(
                        userId,
                        mediaId,
                        body.rating()
                )
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/ratings/{mediaId}")
    public ResponseEntity<Void> removeRating(
            @PathVariable long mediaId,
            HttpSession session
    ) {
        long userId = service.authenticated(session).id();

        ratingAction(() ->
                service.repository().removeRating(
                        userId,
                        mediaId
                )
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/watchlist")
    public List<Long> watchlist(HttpSession session) {
        long userId = service.authenticated(session).id();

        return ratingRequest(
                () -> service.repository().watchlist(userId)
        );
    }

    @PostMapping("/me/watchlist/{mediaId}")
    public ResponseEntity<Void> addWatchlist(
            @PathVariable long mediaId,
            HttpSession session
    ) {
        long userId = service.authenticated(session).id();

        service.checkMedia(mediaId);

        ratingAction(() ->
                service.repository().addWatchlist(
                        userId,
                        mediaId
                )
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/watchlist/{mediaId}")
    public ResponseEntity<Void> removeWatchlist(
            @PathVariable long mediaId,
            HttpSession session
    ) {
        long userId = service.authenticated(session).id();

        ratingAction(() ->
                service.repository().removeWatchlist(
                        userId,
                        mediaId
                )
        );

        return ResponseEntity.noContent().build();
    }
}
