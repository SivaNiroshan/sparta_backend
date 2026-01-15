package com.Sparta.GatewayService.filter;

import com.Sparta.GatewayService.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/account/auth/login",
            "/account/auth/signup",
            "/account/auth/verify-signup-otp",
            "/account/auth/forgot-password",
            "/account/auth/verify-forgot-password-otp",
            "/account/auth/resend-signup-otp",
            "/account/auth/resend-forgot-password-otp",
            "/account/auth/email-exists",
            "/account/auth/updatepassword",
            "/account/auth/refresh",
            "/account/auth/logout"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Check if the path is a public endpoint
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Extract access token from cookie
        String token = extractTokenFromCookie(request, "jwt_token");
        if (token == null || token.isEmpty()) {
            token = extractTokenFromCookie(request, "access_token");
        }

        // If no token in cookie, try Authorization header as fallback
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // Check if token is expired
        boolean isExpired = false;
        if (token != null && !token.isEmpty()) {
            isExpired = jwtUtil.isTokenExpired(token);
        }

        // If token is expired, try to refresh using refresh token
        if (isExpired || token == null || token.isEmpty()) {
            String refreshToken = extractTokenFromCookie(request, "refresh_token");
            if (refreshToken != null && !refreshToken.isEmpty()) {
                // Validate refresh token
                if (jwtUtil.validateRefreshToken(refreshToken) != null) {
                    // Refresh token is valid, but we can't refresh here
                    // The client should call /account/auth/refresh endpoint
                    // For now, we'll allow the request to proceed if refresh token is valid
                    // The downstream service or client should handle the refresh
                    // We'll extract user info from refresh token
                    UUID userId = jwtUtil.getUserIdFromToken(refreshToken);
                    String email = jwtUtil.getEmailFromToken(refreshToken);
                    
                    if (userId != null && email != null) {
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-User-Id", userId.toString())
                                .header("X-User-Email", email)
                                .header("X-Token-Expired", "true")
                                .build();
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    }
                }
            }
            return handleUnauthorized(exchange);
        }

        // Validate token
        if (jwtUtil.validateToken(token) == null) {
            return handleUnauthorized(exchange);
        }

        // Add user info to request headers for downstream services
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", jwtUtil.getUserIdFromToken(token) != null ? 
                        jwtUtil.getUserIdFromToken(token).toString() : "")
                .header("X-User-Email", jwtUtil.getEmailFromToken(token) != null ? 
                        jwtUtil.getEmailFromToken(token) : "")
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private String extractTokenFromCookie(ServerHttpRequest request, String cookieName) {
        String cookieHeader = request.getHeaders().getFirst("Cookie");
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return null;
        }

        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith(cookieName + "=")) {
                return trimmed.substring((cookieName + "=").length());
            }
        }
        return null;
    }

    @SuppressWarnings("null")
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        
        String body = "{\"error\":\"Unauthorized: Invalid or missing token\"}";
        byte[] bodyBytes = Objects.requireNonNull(body.getBytes(StandardCharsets.UTF_8), "Body bytes cannot be null");
        DataBuffer buffer = Objects.requireNonNull(
            response.bufferFactory().wrap(bodyBytes), 
            "Buffer cannot be null"
        );
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // High priority, run early in the filter chain
    }
}

