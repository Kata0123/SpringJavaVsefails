
package org.example.adminservice.service;

import org.example.adminservice.client.ServiceHttp;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AdminUserService {

    private final ServiceHttp http;

    public AdminUserService(ServiceHttp http) {
        this.http = http;
    }

    public List<Map<String, Object>> findAll() {
        return http.call(
                "USER-SERVICE",
                HttpMethod.GET,
                "/internal/users/admin",
                null,
                new ParameterizedTypeReference<>() {}
        );
    }

    public long create(
            String username,
            String password,
            String role
    ) {
        Map<String, Long> answer = http.call(
                "USER-SERVICE",
                HttpMethod.POST,
                "/internal/users/admin",
                Map.of(
                        "username", username,
                        "password", password,
                        "role", role
                ),
                new ParameterizedTypeReference<>() {}
        );

        return answer.get("id");
    }

    public void changeRole(
            long id,
            String role,
            long actorId
    ) {
        http.call(
                "USER-SERVICE",
                HttpMethod.PATCH,
                "/internal/users/admin/" + id + "/role",
                Map.of(
                        "role", role,
                        "actorId", actorId
                ),
                null
        );
    }

    public void delete(long id, long actorId) {
        http.call(
                "USER-SERVICE",
                HttpMethod.DELETE,
                "/internal/users/admin/" + id
                        + "?actorId=" + actorId,
                null,
                null
        );
    }
}
