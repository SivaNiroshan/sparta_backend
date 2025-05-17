package com.Sparta.GatewayService.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;

@Component
public class AuthValidationFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.create();

    @Value("${supabase.auth.url}")
    private String SUPABASE_URL;

    @Value("${supabase.api.key}")
    private String SUPABASE_ANON_KEY;

//    @Value("${jwt.secret}")
//    private String JWT_SECRET; // optional if you want to verify signature (not required for decoding only)

    private static final String LOGIN = "/account/auth/login";
    private static final String SIGNUP = "/account/auth/signup";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.equals(LOGIN) || path.equals(SIGNUP)) {
            return chain.filter(exchange);
        }



//



        try {
            String accessToken = getCookie(exchange);
            DecodedJWT decodedJWT = JWT.decode(accessToken);

            Date expiresAt = decodedJWT.getExpiresAt();
            if (expiresAt == null || expiresAt.before(new Date())) {
                throw new JWTVerificationException("Token expired");
            }


            return chain.filter(exchange);
        } catch (JWTVerificationException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap(("Invalid or expired token").getBytes()))
            );
        }
        catch(RuntimeException e){
            exchange.getResponse().setStatusCode(HttpStatus.valueOf(401));
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap((e.getMessage()).getBytes()))
            );
        }
        catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap((e.getMessage()).getBytes()))
            );
        }

        // Try refresh
    }

    private Mono<Void> refreshAccessToken(ServerWebExchange exchange, GatewayFilterChain chain, String refreshToken) {
        String url = SUPABASE_URL + "token?grant_type=refresh_token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", refreshToken);

        return webClient.post()
                .uri(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(body -> {
                    try {
                        JsonNode json = objectMapper.readTree(body);
                        String newAccessToken = json.get("access_token").asText();
                        String newRefreshToken = json.get("refresh_token").asText();
                        long expiresIn = json.get("expires_in").asLong();

                        var response = exchange.getResponse();

                        ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
                                .httpOnly(true)
                                .path("/")
                                .maxAge(Duration.ofSeconds(expiresIn))
                                .build();

                        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                                .httpOnly(true)
                                .path("/")
                                .maxAge(Duration.ofDays(7))
                                .build();

                        response.addCookie(accessCookie);
                        response.addCookie(refreshCookie);

                        return chain.filter(exchange);
                    } catch (Exception e) {
                        Logger logger = LoggerFactory.getLogger(AuthValidationFilter.class);
                        logger.warn("Refresh token invalid or request failed: {}", e.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                });
    }

    private String getCookie(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("accessToken");
        if ( cookie==null) {
            throw new RuntimeException("accessToken" + " cookie is missing or empty");
        }
        return cookie.getValue();
    }


    @Override
    public int getOrder() {
        return -1;
    }
}
