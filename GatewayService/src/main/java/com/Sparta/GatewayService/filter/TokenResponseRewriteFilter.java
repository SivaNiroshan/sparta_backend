package com.Sparta.GatewayService.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DataBuffer;

@Component
public class TokenResponseRewriteFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (!path.equals("/account/auth/login")) {
            return chain.filter(exchange);
        }

        ServerHttpResponse originalResponse = exchange.getResponse();
        var bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void>  writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(body)
                        .flatMap(dataBuffer -> {
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            DataBufferUtils.release(dataBuffer);

                            try {
                                String jsonResponse = new String(content, StandardCharsets.UTF_8);
                                JsonNode jsonNode = objectMapper.readTree(jsonResponse);

                                String accessToken = jsonNode.get("access_token").asText();
                                String refreshToken = jsonNode.get("refresh_token").asText();
                                long expiresIn = jsonNode.get("expires_in").asLong();

                                HttpHeaders headers = this.getHeaders();

                                headers.add("Set-Cookie", "accessToken=" + accessToken + "; HttpOnly; Path=/; Max-Age=" + expiresIn);
                                headers.add("Set-Cookie", "refreshToken=" + refreshToken + "; HttpOnly; Path=/; Max-Age=" + (7 * 24 * 60 * 60));

                                // Replace response body with user data only
                                String userId = jsonNode.get("user").get("id").asText();
                                byte[] newBody = objectMapper.writeValueAsBytes(Map.of("id", userId));
                                DataBuffer buffer = bufferFactory.wrap(newBody);
                                headers.setContentLength(newBody.length);
                                return super.writeWith(Mono.just(buffer));

                            } catch (Exception e) {

                                this.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                                return super.writeWith(Mono.just(bufferFactory.wrap("{}".getBytes())));
                            }
                        });
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        return -2; // Ensure it runs before default response writing
    }
}
