
package org.example.userservice.repository;

import org.example.userservice.model.Models;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Models.AppUser> findByName(String name) {
        return jdbc.query(
                """
                SELECT *
                FROM app_users
                WHERE LOWER(username) = LOWER(?)
                """,
                (rs, row) -> new Models.AppUser(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                name
        ).stream().findFirst();
    }

    public Optional<Models.AppUser> findById(long id) {
        return jdbc.query(
                """
                SELECT *
                FROM app_users
                WHERE id = ?
                """,
                (rs, row) -> new Models.AppUser(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                id
        ).stream().findFirst();
    }

    public String role(long id) {
        return jdbc.queryForObject(
                "SELECT role FROM app_users WHERE id = ?",
                String.class,
                id
        );
    }

    public long create(String username, String passwordHash) {
        Long id = jdbc.queryForObject(
                """
                INSERT INTO app_users(username, password_hash, role)
                VALUES (?, ?, 'USER')
                RETURNING id
                """,
                Long.class,
                username,
                passwordHash
        );

        return Objects.requireNonNull(id);
    }

    public void rename(long id, String username) {
        jdbc.update(
                "UPDATE app_users SET username = ? WHERE id = ?",
                username,
                id
        );
    }

    public void deleteUser(long id) {
        jdbc.update(
                "DELETE FROM app_users WHERE id = ?",
                id
        );
    }
}
