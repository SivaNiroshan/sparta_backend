package com.Sparta.GatewayService.filter;

import com.Sparta.GatewayService.util.JwtUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-testing-min-256-bits-required-for-hmac256-algorithm";
    private UUID testUserId;
    private String testEmail;
    private String validToken;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";
        validToken = createValidToken(testUserId, testEmail);
    }

    private String createValidToken(UUID userId, String email) {
        return JWT.create()
                .withClaim("userId", userId.toString())
                .withClaim("email", email)
                .withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));
    }

    private ServerWebExchange createExchange(String path, String cookieHeader, String authHeader) {
        var requestBuilder = MockServerHttpRequest.get(path);

        if (cookieHeader != null) {
            requestBuilder.header(HttpHeaders.COOKIE, cookieHeader);
        }
        if (authHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authHeader);
        }

        MockServerHttpRequest request = requestBuilder.build();
        return MockServerWebExchange.from(request);
    }

    @Test
    void testFilter_PublicEndpoint_AllowsAccess() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login", null, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_AllPublicEndpoints_AllowAccess() {
        // Given
        String[] publicEndpoints = {
                "/account/auth/login",
                "/account/auth/signup",
                "/account/auth/verify-signup-otp",
                "/account/auth/forgot-password",
                "/account/auth/verify-forgot-password-otp",
                "/account/auth/resend-signup-otp",
                "/account/auth/resend-forgot-password-otp",
                "/account/auth/email-exists"
        };

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When & Then
        for (String endpoint : publicEndpoints) {
            ServerWebExchange exchange = createExchange(endpoint, null, null);
            Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

            StepVerifier.create(result)
                    .verifyComplete();
        }

        verify(chain, times(publicEndpoints.length)).filter(any());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_ProtectedEndpoint_ValidTokenInCookie_AllowsAccess() {
        // Given
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        when(jwtUtil.validateToken(validToken)).thenReturn(decodedJWT);
        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(testUserId);
        when(jwtUtil.getEmailFromToken(validToken)).thenReturn(testEmail);

        String cookieHeader = "jwt_token=" + validToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        
        // Capture the modified exchange passed to chain
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        verify(jwtUtil, times(1)).validateToken(validToken);
        
        // Verify headers were added to the modified request
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        ServerHttpRequest modifiedRequest = capturedExchange.getRequest();
        assertEquals(testUserId.toString(), modifiedRequest.getHeaders().getFirst("X-User-Id"));
        assertEquals(testEmail, modifiedRequest.getHeaders().getFirst("X-User-Email"));
    }

    @Test
    void testFilter_ProtectedEndpoint_ValidTokenInHeader_AllowsAccess() {
        // Given
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        when(jwtUtil.validateToken(validToken)).thenReturn(decodedJWT);
        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(testUserId);
        when(jwtUtil.getEmailFromToken(validToken)).thenReturn(testEmail);

        String authHeader = "Bearer " + validToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", null, authHeader);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        verify(jwtUtil, times(1)).validateToken(validToken);
    }

    @Test
    void testFilter_ProtectedEndpoint_NoToken_ReturnsUnauthorized() {
        // Given
        ServerWebExchange exchange = createExchange("/account/user/profile", null, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_ProtectedEndpoint_InvalidToken_ReturnsUnauthorized() {
        // Given
        when(jwtUtil.validateToken(anyString())).thenReturn(null);
        String cookieHeader = "jwt_token=invalid-token";
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_ProtectedEndpoint_ExpiredToken_ReturnsUnauthorized() {
        // Given
        when(jwtUtil.validateToken(anyString())).thenReturn(null);
        String cookieHeader = "jwt_token=expired-token";
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_ProtectedEndpoint_ValidToken_AddsUserHeaders() {
        // Given
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        when(jwtUtil.validateToken(validToken)).thenReturn(decodedJWT);
        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(testUserId);
        when(jwtUtil.getEmailFromToken(validToken)).thenReturn(testEmail);

        String cookieHeader = "jwt_token=" + validToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        
        // Capture the modified exchange passed to chain
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        // When
        jwtAuthenticationFilter.filter(exchange, chain).block();

        // Then
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        ServerHttpRequest modifiedRequest = capturedExchange.getRequest();
        assertNotNull(modifiedRequest.getHeaders().getFirst("X-User-Id"));
        assertNotNull(modifiedRequest.getHeaders().getFirst("X-User-Email"));
        assertEquals(testUserId.toString(), modifiedRequest.getHeaders().getFirst("X-User-Id"));
        assertEquals(testEmail, modifiedRequest.getHeaders().getFirst("X-User-Email"));
    }

    @Test
    void testFilter_ProtectedEndpoint_TokenInCookie_PreferredOverHeader() {
        // Given
        String cookieToken = validToken;
        String headerToken = "different-token";
        
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        when(jwtUtil.validateToken(cookieToken)).thenReturn(decodedJWT);
        when(jwtUtil.getUserIdFromToken(cookieToken)).thenReturn(testUserId);
        when(jwtUtil.getEmailFromToken(cookieToken)).thenReturn(testEmail);

        String cookieHeader = "jwt_token=" + cookieToken;
        String authHeader = "Bearer " + headerToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, authHeader);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(jwtUtil, times(1)).validateToken(cookieToken);
        verify(jwtUtil, never()).validateToken(headerToken);
    }

    @Test
    void testFilter_ExtractTokenFromCookie_ValidCookie_ReturnsToken() {
        // Given
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        when(jwtUtil.validateToken(validToken)).thenReturn(decodedJWT);
        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(testUserId);
        when(jwtUtil.getEmailFromToken(validToken)).thenReturn(testEmail);

        String cookieHeader = "jwt_token=" + validToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(jwtUtil, times(1)).validateToken(validToken);
    }

    @Test
    void testFilter_ExtractTokenFromCookie_AccessTokenCookie_ReturnsToken() {
        // Given
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        when(jwtUtil.validateToken(validToken)).thenReturn(decodedJWT);
        when(jwtUtil.getUserIdFromToken(validToken)).thenReturn(testUserId);
        when(jwtUtil.getEmailFromToken(validToken)).thenReturn(testEmail);

        String cookieHeader = "access_token=" + validToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(jwtUtil, times(1)).validateToken(validToken);
    }

    @Test
    void testFilter_ExtractTokenFromCookie_NoCookie_ReturnsNull() {
        // Given
        ServerWebExchange exchange = createExchange("/account/user/profile", null, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_ExtractTokenFromCookie_MalformedCookie_ReturnsNull() {
        // Given
        String cookieHeader = "malformed-cookie-value";
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_GetOrder_ReturnsCorrectPriority() {
        // When
        int order = jwtAuthenticationFilter.getOrder();

        // Then
        assertEquals(-100, order);
    }

    @Test
    void testFilter_UnauthorizedResponse_HasCorrectFormat() {
        // Given
        ServerWebExchange exchange = createExchange("/account/user/profile", null, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertEquals("application/json", exchange.getResponse().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void testFilter_ExpiredToken_WithValidRefreshToken_AllowsAccess() {
        // Given
        String expiredAccessToken = createValidToken(testUserId, testEmail);
        String refreshToken = JWT.create()
                .withClaim("userId", testUserId.toString())
                .withClaim("email", testEmail)
                .withClaim("type", "refresh")
                .withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        when(jwtUtil.isTokenExpired(expiredAccessToken)).thenReturn(true);
        when(jwtUtil.validateRefreshToken(refreshToken)).thenReturn(mock(DecodedJWT.class));
        when(jwtUtil.getUserIdFromToken(refreshToken)).thenReturn(testUserId);
        when(jwtUtil.getEmailFromToken(refreshToken)).thenReturn(testEmail);

        String cookieHeader = "jwt_token=" + expiredAccessToken + "; refresh_token=" + refreshToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        ServerHttpRequest modifiedRequest = capturedExchange.getRequest();
        assertEquals(testUserId.toString(), modifiedRequest.getHeaders().getFirst("X-User-Id"));
        assertEquals(testEmail, modifiedRequest.getHeaders().getFirst("X-User-Email"));
        assertEquals("true", modifiedRequest.getHeaders().getFirst("X-Token-Expired"));
    }

    @Test
    void testFilter_ExpiredToken_WithInvalidRefreshToken_ReturnsUnauthorized() {
        // Given
        String expiredAccessToken = createValidToken(testUserId, testEmail);
        String invalidRefreshToken = "invalid-refresh-token";

        when(jwtUtil.isTokenExpired(expiredAccessToken)).thenReturn(true);
        when(jwtUtil.validateRefreshToken(invalidRefreshToken)).thenReturn(null);

        String cookieHeader = "jwt_token=" + expiredAccessToken + "; refresh_token=" + invalidRefreshToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_ExpiredToken_NoRefreshToken_ReturnsUnauthorized() {
        // Given
        String expiredAccessToken = createValidToken(testUserId, testEmail);

        when(jwtUtil.isTokenExpired(expiredAccessToken)).thenReturn(true);

        String cookieHeader = "jwt_token=" + expiredAccessToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void testFilter_RefreshEndpoint_IsPublic() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/refresh", null, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_LogoutEndpoint_IsPublic() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/logout", null, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_UpdatePasswordEndpoint_IsPublic() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/updatepassword", null, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testFilter_RefreshToken_MissingUserId_ReturnsUnauthorized() {
        // Given
        String expiredAccessToken = createValidToken(testUserId, testEmail);
        String refreshToken = JWT.create()
                .withClaim("email", testEmail)
                .withClaim("type", "refresh")
                .withExpiresAt(new java.util.Date(System.currentTimeMillis() + 3600000))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        when(jwtUtil.isTokenExpired(expiredAccessToken)).thenReturn(true);
        when(jwtUtil.validateRefreshToken(refreshToken)).thenReturn(mock(DecodedJWT.class));
        when(jwtUtil.getUserIdFromToken(refreshToken)).thenReturn(null);
        when(jwtUtil.getEmailFromToken(refreshToken)).thenReturn(testEmail);

        String cookieHeader = "jwt_token=" + expiredAccessToken + "; refresh_token=" + refreshToken;
        ServerWebExchange exchange = createExchange("/account/user/profile", cookieHeader, null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        // When
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}

