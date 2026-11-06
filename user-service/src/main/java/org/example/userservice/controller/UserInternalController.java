
package org.example.userservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.userservice.model.Models;
import org.example.userservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/users")
public class UserInternalController {

    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public UserInternalController(
            UserRepository users,
            JdbcTemplate jdbc,
            PasswordEncoder encoder
    ) {
        this.users = users;
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    public record CreateRequest(
            @NotBlank String username,
            @NotBlank String passwordHash
    ) {}

    public record RenameRequest(
            @NotBlank String name
    ) {}

    public record CreateAdminRequest(
            @NotBlank @Size(min = 2, max = 80) String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Pattern(regexp = "USER|ADMIN") String role
    ) {}

    public record RoleRequest(
            @NotBlank @Pattern(regexp = "USER|ADMIN") String role,
            @Positive long actorId
    ) {}

    @GetMapping("/by-name")
    public Models.AppUser byName(@RequestParam String name) {
        return users.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));
    }

    @GetMapping("/{id}")
    public Models.AppUser byId(@PathVariable long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));
    }

    @GetMapping("/{id}/role")
    public Map<String, String> role(@PathVariable long id) {
        return Map.of("role", users.role(id));
    }

    @PostMapping
    public Map<String, Long> create(
            @Valid @RequestBody CreateRequest request
    ) {
        String username = request.username().trim();

        if (users.findByName(username).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Это имя уже занято"
            );
        }

        return Map.of(
                "id",
                users.create(username, request.passwordHash())
        );
    }

    @PatchMapping("/{id}")
    public void rename(
            @PathVariable long id,
            @Valid @RequestBody RenameRequest request
    ) {
        String name = request.name().trim();

        users.findByName(name).ifPresent(other -> {
            if (other.id() != id) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Это имя уже занято"
                );
            }
        });

        users.rename(id, name);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        users.deleteUser(id);
    }

    @GetMapping("/admin")
    public List<Map<String, Object>> listAdmins() {
        return jdbc.queryForList(
                """
                SELECT id, username, email, role, created_at
                FROM app_users
                ORDER BY id
                LIMIT 200
                """
        );
    }

    @PostMapping("/admin")
    @Transactional
    public Map<String, Long> createAdmin(
            @Valid @RequestBody CreateAdminRequest request
    ) {
        String username = request.username().trim();

        if (users.findByName(username).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Пользователь уже существует"
            );
        }

        long id = users.create(
                username,
                encoder.encode(request.password())
        );

        if ("ADMIN".equals(request.role())) {
            jdbc.update(
                    "UPDATE app_users SET role = 'ADMIN' WHERE id = ?",
                    id
            );
        }

        return Map.of("id", id);
    }

    @PatchMapping("/admin/{id}/role")
    public void changeRole(
            @PathVariable long id,
            @Valid @RequestBody RoleRequest request
    ) {
        if (id == request.actorId()
                && !"ADMIN".equals(request.role())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нельзя убрать свою роль администратора"
            );
        }

        int changed = jdbc.update(
                "UPDATE app_users SET role = ? WHERE id = ?",
                request.role(),
                id
        );

        if (changed == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Пользователь не найден"
            );
        }
    }

    @DeleteMapping("/admin/{id}")
    public void deleteAdmin(
            @PathVariable long id,
            @RequestParam long actorId
    ) {
        if (id == actorId) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нельзя удалить свой аккаунт из админ-панели"
            );
        }

        int changed = jdbc.update(
                "DELETE FROM app_users WHERE id = ?",
                id
        );

        if (changed == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Пользователь не найден"
            );
        }
    }
}
