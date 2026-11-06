
package org.example.adminservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

@Configuration
class RestLoadBalancerConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder internalRestClientBuilder() {
        return RestClient.builder();
    }
}

@Component
public class ServiceHttp {

    private final RestClient client;
    private final String key;
    private final ObjectMapper mapper;

    public ServiceHttp(
            @LoadBalanced RestClient.Builder builder,
            @Value("${internal.service.key}") String key,
            ObjectMapper mapper
    ) {
        if (key == null || key.length() < 32) {
            throw new IllegalStateException(
                    "Не задан INTERNAL_SERVICE_KEY (минимум 32 символа)"
            );
        }

        this.client = builder.build();
        this.key = key;
        this.mapper = mapper;
    }

    public <T> T call(
            String service,
            HttpMethod method,
            String path,
            Object body,
            Map<String, String> headers,
            ParameterizedTypeReference<T> type
    ) {
        try {
            RestClient.RequestBodySpec request = client.method(method)
                    .uri(URI.create("http://" + service + path))
                    .header("X-Internal-Key", key);

            if (headers != null) {
                headers.forEach(request::header);
            }

            if (body != null) {
                request.body(body);
            }

            if (type == null) {
                request.retrieve().toBodilessEntity();
                return null;
            }

            return request.retrieve().body(type);

        } catch (RestClientResponseException e) {
            String message = "Ошибка внутреннего сервиса";

            try {
                JsonNode json = mapper.readTree(
                        e.getResponseBodyAsString()
                );

                if (json.hasNonNull("error")) {
                    message = json.get("error").asText();
                } else if (json.hasNonNull("detail")) {
                    message = json.get("detail").asText();
                }
            } catch (Exception ignored) {
            }

            throw new ResponseStatusException(
                    e.getStatusCode(), message, e
            );

        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Микросервис временно недоступен",
                    e
            );
        }
    }

    public <T> T call(
            String service,
            HttpMethod method,
            String path,
            Object body,
            ParameterizedTypeReference<T> type
    ) {
        return call(service, method, path, body, null, type);
    }
}
