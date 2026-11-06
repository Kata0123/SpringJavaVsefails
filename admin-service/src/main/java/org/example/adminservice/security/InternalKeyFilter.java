
package org.example.adminservice.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalKeyFilter implements Filter {

    private final byte[] expected;

    public InternalKeyFilter(
            @Value("${internal.service.key}") String secret
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "INTERNAL_SERVICE_KEY должен содержать минимум 32 символа"
            );
        }

        expected = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void doFilter(
            ServletRequest req,
            ServletResponse resp,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        if (!request.getRequestURI().startsWith("/internal/")) {
            chain.doFilter(req, resp);
            return;
        }

        String supplied = request.getHeader("X-Internal-Key");

        if (supplied == null || !MessageDigest.isEqual(
                expected,
                supplied.getBytes(StandardCharsets.UTF_8)
        )) {
            HttpServletResponse response = (HttpServletResponse) resp;
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Доступ запрещён\"}"
            );
            return;
        }

        chain.doFilter(req, resp);
    }
}
