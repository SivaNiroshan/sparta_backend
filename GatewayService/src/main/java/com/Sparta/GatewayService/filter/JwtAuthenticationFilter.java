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
            "/account/auth/email-exists"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Check if the path is a public endpoint
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Extract token from cookie
        String token = extractTokenFromCookie(request);

        // If no token in cookie, try Authorization header as fallback
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // Validate token
        if (token == null || token.isEmpty() || jwtUtil.validateToken(token) == null) {
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

    private String extractTokenFromCookie(ServerHttpRequest request) {
        String cookieHeader = request.getHeaders().getFirst("Cookie");
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return null;
        }

        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith("jwt_token=")) {
                return trimmed.substring("jwt_token=".length());
            } else if (trimmed.startsWith("access_token=")) {
                return trimmed.substring("access_token=".length());
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

