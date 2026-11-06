
package org.example.guccispring.repository;

import org.example.guccispring.client.ServiceHttp;
import org.example.guccispring.model.Models;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Repository
public class UserRepository {

    private final ServiceHttp client;

    public UserRepository(ServiceHttp client) {
        this.client = client;
    }

    private String enc(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    public Optional<Models.AppUser> findByName(String name) {
        try {
            Models.AppUser user = client.call(
                    "USER-SERVICE",
                    HttpMethod.GET,
                    "/internal/users/by-name?name=" + enc(name),
                    null,
                    new ParameterizedTypeReference<Models.AppUser>() {}
            );

            return Optional.ofNullable(user);

        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }

            throw e;
        }
    }

    public Optional<Models.AppUser> findById(long id) {
        try {
            Models.AppUser user = client.call(
                    "USER-SERVICE",
                    HttpMethod.GET,
                    "/internal/users/" + id,
                    null,
                    new ParameterizedTypeReference<Models.AppUser>() {}
            );

            return Optional.ofNullable(user);

        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }

            throw e;
        }
    }

    public String role(long id) {
        Map<String, String> response = client.call(
                "USER-SERVICE",
                HttpMethod.GET,
                "/internal/users/" + id + "/role",
                null,
                new ParameterizedTypeReference<>() {}
        );

        return response.get("role");
    }

    public long create(String username, String passwordHash) {
        Map<String, Long> response = client.call(
                "USER-SERVICE",
                HttpMethod.POST,
                "/internal/users",
                Map.of(
                        "username", username,
                        "passwordHash", passwordHash
                ),
                new ParameterizedTypeReference<>() {}
        );

        return response.get("id");
    }

    public void rename(long id, String username) {
        client.call(
                "USER-SERVICE",
                HttpMethod.PATCH,
                "/internal/users/" + id,
                Map.of("name", username),
                null
        );
    }

    public void deleteUser(long id) {
        client.call(
                "USER-SERVICE",
                HttpMethod.DELETE,
                "/internal/users/" + id,
                null,
                null
        );
    }

    public Map<Long, Integer> ratings(long userId) {
        return client.call(
                "RATING-SERVICE",
                HttpMethod.GET,
                "/internal/ratings/users/" + userId,
                null,
                new ParameterizedTypeReference<Map<Long, Integer>>() {}
        );
    }

    public void rate(
            long userId,
            long mediaId,
            int rating
    ) {
        client.call(
                "RATING-SERVICE",
                HttpMethod.PUT,
                "/internal/ratings/users/" + userId
                        + "/media/" + mediaId,
                Map.of("rating", rating),
                null
        );
    }

    public void removeRating(long userId, long mediaId) {
        client.call(
                "RATING-SERVICE",
                HttpMethod.DELETE,
                "/internal/ratings/users/" + userId
                        + "/media/" + mediaId,
                null,
                null
        );
    }

    public List<Long> watchlist(long userId) {
        return client.call(
                "RATING-SERVICE",
                HttpMethod.GET,
                "/internal/watchlist/users/" + userId,
                null,
                new ParameterizedTypeReference<List<Long>>() {}
        );
    }

    public void addWatchlist(long userId, long mediaId) {
        client.call(
                "RATING-SERVICE",
                HttpMethod.POST,
                "/internal/watchlist/users/" + userId
                        + "/media/" + mediaId,
                null,
                null
        );
    }

    public void removeWatchlist(long userId, long mediaId) {
        client.call(
                "RATING-SERVICE",
                HttpMethod.DELETE,
                "/internal/watchlist/users/" + userId
                        + "/media/" + mediaId,
                null,
                null
        );
    }
}
