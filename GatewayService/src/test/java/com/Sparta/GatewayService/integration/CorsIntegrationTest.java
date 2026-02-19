package com.Sparta.GatewayService.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Arrays;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CorsIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        webTestClient = WebTestClient.bindToServer()
                .baseUrl(baseUrl)
                .build();
    }

    @Test
    void testCors_AllowedOrigins_ReturnsCorsHeaders() {
        String[] allowedOrigins = {
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        };

        for (String origin : allowedOrigins) {
            // CORS headers are set even when downstream service is unavailable
            webTestClient.get()
                    .uri("/account/auth/login")
                    .header(HttpHeaders.ORIGIN, origin)
                    .exchange()
                    .expectStatus().is5xxServerError() // Downstream service not available
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
                    .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        }
    }

    @Test
    void testCors_PreflightRequest_ReturnsCorrectHeaders() {
        webTestClient.options()
                .uri("/account/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type, Authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
    }

    @Test
    void testCors_AllMethods_Allowed() {
        String[] methods = {"GET", "POST", "PUT", "PATCH", "DELETE"};

        for (String method : methods) {
            webTestClient.options()
                    .uri("/account/auth/login")
                    .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueMatches(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ".*" + method + ".*");
        }
    }

    @Test
    void testCors_Credentials_Allowed() {
        // CORS headers are set even when downstream service is unavailable
        webTestClient.get()
                .uri("/account/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .cookie("test-cookie", "value")
                .exchange()
                .expectStatus().is5xxServerError() // Downstream service not available
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    @Test
    void testCors_ExposedHeaders_Configured() {
        // CORS headers are set even when downstream service is unavailable
        webTestClient.get()
                .uri("/account/auth/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .exchange()
                .expectStatus().is5xxServerError() // Downstream service not available
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)
                .expectHeader().valueMatches(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, ".*Authorization.*")
                .expectHeader().valueMatches(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, ".*Content-Type.*")
                .expectHeader().valueMatches(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, ".*X-Total-Count.*");
    }

    @Test
    void testCors_AllPaths_Applied() {
        String[] paths = {
                "/account/auth/login",
                "/account/user/profile",
                "/upload/test"
        };

        for (String path : paths) {
            // CORS headers are set even when downstream service is unavailable
            // Note: /account/user/profile returns 401 (unauthorized) before routing
            // We verify CORS headers are present regardless of response status
            var response = webTestClient.get()
                    .uri(path)
                    .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                    .exchange()
                    .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                    .returnResult(String.class);
            // Status will be 401 or 500, but CORS headers validate filter execution
        }
    }
}
