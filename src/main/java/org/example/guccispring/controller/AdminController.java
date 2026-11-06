
package org.example.guccispring.controller;

import org.example.guccispring.client.ServiceHttp;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminController {

    private final ServiceHttp http;

    public AdminController(ServiceHttp http) {
        this.http = http;
    }

    @GetMapping("/{table}")
    public List<Map<String, Object>> all(
            @PathVariable String table,
            @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.GET,
                "/internal/admin/" + table
                        + "?limit=" + limit
                        + "&offset=" + offset,
                null,
                new ParameterizedTypeReference<>() {}
        );
    }

    @GetMapping("/{table}/{id}")
    public Map<String, Object> one(
            @PathVariable String table,
            @PathVariable String id
    ) {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.GET,
                "/internal/admin/" + table + "/" + id,
                null,
                new ParameterizedTypeReference<>() {}
        );
    }

    @PostMapping("/{table}")
    public Map<String, Object> create(
            @PathVariable String table,
            @RequestBody Map<String, Object> body
    ) {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.POST,
                "/internal/admin/" + table,
                body,
                new ParameterizedTypeReference<>() {}
        );
    }

    @PutMapping("/{table}/{id}")
    public Map<String, Object> update(
            @PathVariable String table,
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        return http.call(
                "ADMIN-SERVICE",
                HttpMethod.PUT,
                "/internal/admin/" + table + "/" + id,
                body,
                new ParameterizedTypeReference<>() {}
        );
    }

    @DeleteMapping("/{table}/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String table,
            @PathVariable String id
    ) {
        http.call(
                "ADMIN-SERVICE",
                HttpMethod.DELETE,
                "/internal/admin/" + table + "/" + id,
                null,
                null
        );

        return ResponseEntity.noContent().build();
    }
}
