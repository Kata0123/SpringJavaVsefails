
package org.example.guccispring.controller;

import org.example.guccispring.client.ServiceHttp;
import org.example.guccispring.dto.TitleRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/titles")
public class AdminTitleController {

    private final ServiceHttp http;

    public AdminTitleController(ServiceHttp http) {
        this.http = http;
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.GET,
                "/internal/admin/titles/" + id,
                null,
                new ParameterizedTypeReference<>() {}
        );
    }

    @PostMapping
    public Map<String, Long> create(
            @Valid @RequestBody TitleRequest body
    ) {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.POST,
                "/internal/admin/titles",
                body,
                new ParameterizedTypeReference<>() {}
        );
    }

    @PutMapping("/{id}")
    public Map<String, Long> update(
            @PathVariable long id,
            @Valid @RequestBody TitleRequest body
    ) {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.PUT,
                "/internal/admin/titles/" + id,
                body,
                new ParameterizedTypeReference<>() {}
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        http.call(
                "ADMIN-SERVICE",
                HttpMethod.DELETE,
                "/internal/admin/titles/" + id,
                null,
                null
        );

        return ResponseEntity.noContent().build();
    }
}
