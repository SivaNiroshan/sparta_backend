package com.Sparta.GatewayService.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtCookieFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Endpoints that return JWT tokens and should have cookies set
    // These endpoints return JWT tokens that should be converted to HttpOnly cookies
    private static final List<String> AUTH_ENDPOINTS = Arrays.asList(
            "/account/auth/login",
            "/account/auth/verify-signup-otp",
            "/account/auth/verify-forgot-password-otp",
            "/account/auth/refresh"
    );

    // Endpoints that should clear cookies (logout)
    private static final List<String> LOGOUT_ENDPOINTS = Arrays.asList(
            "/account/auth/logout"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Handle logout endpoints - extract tokens from cookies and clear them
        if (isLogoutEndpoint(path)) {
            ServerHttpRequest request = exchange.getRequest();
            
            // Extract tokens from cookies to pass to UserService for blacklisting
            String accessToken = extractTokenFromCookie(request, "jwt_token");
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = extractTokenFromCookie(request, "access_token");
            }
            String refreshToken = extractTokenFromCookie(request, "refresh_token");
            
            // If tokens exist, add them to request body for blacklisting
            ServerWebExchange modifiedExchange = exchange;
            if ((accessToken != null && !accessToken.isEmpty()) || 
                (refreshToken != null && !refreshToken.isEmpty())) {
                try {
                    // Create request body with tokens
                    Map<String, String> logoutRequest = new HashMap<>();
                    if (accessToken != null && !accessToken.isEmpty()) {
                        logoutRequest.put("accessToken", accessToken);
                    }
                    if (refreshToken != null && !refreshToken.isEmpty()) {
                        logoutRequest.put("refreshToken", refreshToken);
                    }
                    
                    // Modify request to include tokens in headers
                    ServerHttpRequest modifiedRequest = request.mutate()
                        .header("Content-Type", "application/json")
                        .build();
                    
                    // We'll let the downstream service handle the body
                    // For now, we'll add tokens as headers (simpler approach)
                    ServerHttpRequest requestWithTokens = modifiedRequest.mutate()
                        .header("X-Access-Token", accessToken != null ? accessToken : "")
                        .header("X-Refresh-Token", refreshToken != null ? refreshToken : "")
                        .build();
                    
                    modifiedExchange = exchange.mutate().request(requestWithTokens).build();
                } catch (Exception e) {
                    // If JSON serialization fails, continue without tokens
                }
            }
            
            ServerHttpResponse response = modifiedExchange.getResponse();
            // Clear access token cookie
            response.getHeaders().add(HttpHeaders.SET_COOKIE, 
                "jwt_token=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
            response.getHeaders().add(HttpHeaders.SET_COOKIE, 
                "access_token=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
            // Clear refresh token cookie
            response.getHeaders().add(HttpHeaders.SET_COOKIE, 
                "refresh_token=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0");
            
            return chain.filter(modifiedExchange);
        }

        // Only process auth endpoints
        if (!isAuthEndpoint(path)) {
            return chain.filter(exchange);
        }

        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            @NonNull
            public Mono<Void> writeWith(@NonNull org.reactivestreams.Publisher<? extends DataBuffer> body) {
                // Convert both Mono and Flux to Flux for processing
                Flux<DataBuffer> fluxBody = Flux.from(body);
                return fluxBody.collectList().flatMap(list -> {
                    // Combine all data buffers
                    int totalSize = list.stream().mapToInt(DataBuffer::readableByteCount).sum();
                    DataBuffer combined = bufferFactory.allocateBuffer(totalSize);
                    for (DataBuffer buffer : list) {
                        combined.write(buffer);
                        DataBufferUtils.release(buffer);
                    }

                    // Read the response body
                    byte[] content = new byte[combined.readableByteCount()];
                    combined.read(content);
                    DataBufferUtils.release(combined);

                    String responseBody = new String(content, StandardCharsets.UTF_8);

                    try {
                        // Parse JSON response
                        JsonNode jsonNode = objectMapper.readTree(responseBody);

                        // Extract token from response
                        if (jsonNode.has("token")) {
                            String token = jsonNode.get("token").asText();

                            // Set cookie with JWT token (7 days = 604800 seconds)
                            String cookieValue = String.format("jwt_token=%s; Path=/; HttpOnly; SameSite=Strict; Max-Age=604800", token);
                            getHeaders().add(HttpHeaders.SET_COOKIE, cookieValue);
                            
                            // If refresh token is present, set it as well
                            if (jsonNode.has("refreshToken")) {
                                String refreshToken = jsonNode.get("refreshToken").asText();
                                String refreshCookieValue = String.format("refresh_token=%s; Path=/; HttpOnly; SameSite=Strict; Max-Age=604800", refreshToken);
                                getHeaders().add(HttpHeaders.SET_COOKIE, refreshCookieValue);
                            }
                        }
                    } catch (Exception e) {
                        // If parsing fails, just continue without setting cookie
                    }

                    // Write the original response body
                    DataBuffer buffer = bufferFactory.wrap(content);
                    return getDelegate().writeWith(Mono.just(buffer));
                });
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private boolean isAuthEndpoint(String path) {
        return AUTH_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private boolean isLogoutEndpoint(String path) {
        return LOGOUT_ENDPOINTS.stream().anyMatch(path::startsWith);
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

    @Override
    public int getOrder() {
        return -50; // Run after JwtAuthenticationFilter but before routing
    }
}

