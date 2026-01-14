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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtCookieFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Endpoints that return JWT tokens and should have cookies set
    // These endpoints return JWT tokens that should be converted to HttpOnly cookies
    private static final List<String> AUTH_ENDPOINTS = Arrays.asList(
            "/account/auth/login",
            "/account/auth/verify-signup-otp",
            "/account/auth/verify-forgot-password-otp"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

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

                            // Set cookie with JWT token
                            String cookieValue = String.format("jwt_token=%s; Path=/; HttpOnly; SameSite=Strict; Max-Age=86400", token);
                            getHeaders().add(HttpHeaders.SET_COOKIE, cookieValue);
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

    @Override
    public int getOrder() {
        return -50; // Run after JwtAuthenticationFilter but before routing
    }
}

