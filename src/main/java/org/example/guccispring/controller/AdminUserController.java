
package org.example.guccispring.controller;

import org.example.guccispring.client.ServiceHttp;
import org.example.guccispring.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final ServiceHttp http;
    private final UserRepository users;

    public AdminUserController(
            ServiceHttp http,
            UserRepository users
    ) {
        this.http = http;
        this.users = users;
    }

    public record CreateUser(
            @NotBlank @Size(min = 2, max = 80)
            String username,

            @NotBlank @Size(min = 8, max = 72)
            String password,

            @NotBlank @Pattern(regexp = "USER|ADMIN")
            String role
    ) {}

    public record ChangeRole(
            @NotBlank @Pattern(regexp = "USER|ADMIN")
            String role
    ) {}

    private long currentId() {
        String name = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return users.findByName(name)
                .orElseThrow()
                .id();
    }

    @GetMapping
    public List<Map<String, Object>> users() {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.GET,
                "/internal/admin/users",
                null,
                new ParameterizedTypeReference<>() {}
        );
    }

    @PostMapping
    public Map<String, Long> create(
            @Valid @RequestBody CreateUser body
    ) {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.POST,
                "/internal/admin/users",
                body,
                new ParameterizedTypeReference<>() {}
        );
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable long id,
            @Valid @RequestBody ChangeRole body
    ) {
        http.call(
                "ADMIN-SERVICE",
                HttpMethod.PATCH,
                "/internal/admin/users/" + id + "/role",
                body,
                Map.of(
                        "X-Actor-Id",
                        String.valueOf(currentId())
                ),
                null
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        http.call(
                "ADMIN-SERVICE",
                HttpMethod.DELETE,
                "/internal/admin/users/" + id,
                null,
                Map.of(
                        "X-Actor-Id",
                        String.valueOf(currentId())
                ),
                null
        );

        return ResponseEntity.noContent().build();
    }
}
