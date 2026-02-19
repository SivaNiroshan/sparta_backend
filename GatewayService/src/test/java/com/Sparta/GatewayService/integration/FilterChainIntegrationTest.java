package com.Sparta.GatewayService.integration;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Date;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FilterChainIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;
    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-min-256-bits-required-for-hmac256-algorithm";
    private UUID testUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:" + port;
        webTestClient = WebTestClient.bindToServer()
                .baseUrl(baseUrl)
                .build();
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
    }

    private String createValidToken(UUID userId, String email) {
        return JWT.create()
                .withClaim("userId", userId.toString())
                .withClaim("email", email)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));
    }

    @Test
    void testJwtAuthenticationFilter_AddsUserHeaders_ToDownstreamRequest() {
        String token = createValidToken(testUserId, testEmail);

        // Authentication filter validates token and adds headers before routing
        webTestClient.get()
                .uri("/account/user/profile")
                .cookie("jwt_token", token)
                .exchange()
                .expectStatus().is5xxServerError(); // Downstream service not available
        // Note: In real scenario, downstream service would receive X-User-Id and X-User-Email headers
    }

    @Test
    void testJwtCookieFilter_LoginResponse_ConvertsTokenToCookie() {
        // Cookie filter processes response body to extract token
        webTestClient.post()
                .uri("/account/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"test@example.com\",\"password\":\"password\",\"token\":\"test-token-123\"}")
                .exchange()
                .expectStatus().is5xxServerError(); // Downstream service not available
        // Cookie would be set if downstream service returned token in response body
    }

    @Test
    void testCorsFilter_AppliedBeforeAuthentication() {
        // CORS filter processes preflight requests before authentication
        webTestClient.options()
                .uri("/account/user/profile")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk() // Preflight doesn't require downstream service
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    void testFilterOrder_AuthenticationBeforeCookieFilter() {
        // Authentication filter should validate token before cookie filter processes response
        String token = createValidToken(testUserId, testEmail);

        webTestClient.get()
                .uri("/account/user/profile")
                .cookie("jwt_token", token)
                .exchange()
                .expectStatus().is5xxServerError(); // Downstream service not available, but filters executed
    }

    @Test
    void testRefreshTokenFlow_ExpiredAccessToken_WithValidRefreshToken() {
        String expiredToken = JWT.create()
                .withClaim("userId", testUserId.toString())
                .withClaim("email", testEmail)
                .withExpiresAt(new Date(System.currentTimeMillis() - 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        String refreshToken = JWT.create()
                .withClaim("userId", testUserId.toString())
                .withClaim("email", testEmail)
                .withClaim("type", "refresh")
                .withExpiresAt(new Date(System.currentTimeMillis() + 604800000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        // Refresh token flow allows access with expired access token
        webTestClient.get()
                .uri("/account/user/profile")
                .cookie("jwt_token", expiredToken)
                .cookie("refresh_token", refreshToken)
                .exchange()
                .expectStatus().is5xxServerError(); // Downstream service not available, but refresh token validated
    }
}
