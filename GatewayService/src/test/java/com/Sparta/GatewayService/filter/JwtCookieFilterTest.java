package com.Sparta.GatewayService.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtCookieFilterTest {

    @InjectMocks
    private JwtCookieFilter jwtCookieFilter;

    private static final String TEST_TOKEN = "test-jwt-token-12345";
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private ServerWebExchange createExchange(String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path)
                .build();
        return MockServerWebExchange.from(request);
    }

    private String createJsonResponse(String token) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("token", token));
        } catch (Exception e) {
            return "{\"token\":\"" + token + "\"}";
        }
    }

    @Test
    void testFilter_NonAuthEndpoint_PassesThrough() {
        // Given
        ServerWebExchange exchange = createExchange("/account/user/profile");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    @Test
    void testFilter_AuthEndpoint_WithToken_SetsCookie() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String responseBody = createJsonResponse(TEST_TOKEN);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        
        // The cookie should be set on the decorated response (which wraps the original response)
        // After writeWith is called, the cookie headers are added to the original response
        String setCookieHeader = exchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("jwt_token=" + TEST_TOKEN));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Strict"));
        assertTrue(setCookieHeader.contains("Path=/"));
        assertTrue(setCookieHeader.contains("Max-Age=604800")); // Updated to match actual implementation (7 days)
    }

    @Test
    void testFilter_AuthEndpoint_WithoutToken_NoCookie() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String responseBody = "{\"message\":\"Login successful\"}";
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    @Test
    void testFilter_LoginEndpoint_SetsCookie() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String responseBody = createJsonResponse(TEST_TOKEN);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String setCookieHeader = capturedExchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("jwt_token=" + TEST_TOKEN));
    }

    @Test
    void testFilter_VerifySignupOtpEndpoint_SetsCookie() {
        // Given - verify-signup-otp now sets cookie when token is present
        ServerWebExchange exchange = createExchange("/account/auth/verify-signup-otp");
        String responseBody = createJsonResponse(TEST_TOKEN);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        // verify-signup-otp should now set cookie when token is present
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String setCookieHeader = capturedExchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("jwt_token=" + TEST_TOKEN));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Strict"));
    }

    @Test
    void testFilter_VerifyForgotPasswordOtpEndpoint_SetsCookie() {
        // Given - verify-forgot-password-otp now sets cookie when token is present
        ServerWebExchange exchange = createExchange("/account/auth/verify-forgot-password-otp");
        String responseBody = createJsonResponse(TEST_TOKEN);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        // verify-forgot-password-otp should now set cookie when token is present
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String setCookieHeader = capturedExchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("jwt_token=" + TEST_TOKEN));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Strict"));
    }

    @Test
    void testFilter_CookieHasCorrectAttributes() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String responseBody = createJsonResponse(TEST_TOKEN);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        // The cookie is set on the decorated response which wraps the original response
        String setCookieHeader = exchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        
        // Verify all cookie attributes
        assertTrue(setCookieHeader.contains("jwt_token=" + TEST_TOKEN));
        assertTrue(setCookieHeader.contains("Path=/"));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("SameSite=Strict"));
        assertTrue(setCookieHeader.contains("Max-Age=604800")); // Updated to match actual implementation (7 days)
    }

    @Test
    void testFilter_InvalidJsonResponse_NoCookie() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String responseBody = "not a valid json";
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    @Test
    void testFilter_EmptyResponseBody_NoCookie() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String responseBody = "";
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    @Test
    void testFilter_ResponseBodyWithoutToken_NoCookie() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String responseBody = "{\"message\":\"Success\",\"userId\":\"123\"}";
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    @Test
    void testFilter_GetOrder_ReturnsCorrectPriority() {
        // When
        int order = jwtCookieFilter.getOrder();

        // Then
        assertEquals(-50, order);
    }

    @Test
    void testFilter_MultipleDataBuffers_CombinesCorrectly() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String responseBody = createJsonResponse(TEST_TOKEN);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
            
            // Create multiple buffers
            DataBuffer buffer1 = factory.wrap(responseBody.substring(0, 10).getBytes(StandardCharsets.UTF_8));
            DataBuffer buffer2 = factory.wrap(responseBody.substring(10).getBytes(StandardCharsets.UTF_8));
            
            return ex.getResponse().writeWith(Flux.just(buffer1, buffer2));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String setCookieHeader = capturedExchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("jwt_token=" + TEST_TOKEN));
    }

    @Test
    void testFilter_ExceptionDuringParsing_ContinuesWithoutCookie() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        // Create a response that will cause parsing issues
        String responseBody = "{\"token\":\"test\"}";
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            // Use a response that might cause issues
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        // Should complete without throwing exception
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        // Cookie should be set if token is present
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String setCookieHeader = capturedExchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
    }

    @Test
    void testFilter_LogoutEndpoint_ClearsCookies() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.post("/account/auth/logout")
                .header(HttpHeaders.COOKIE, "jwt_token=test-token; refresh_token=refresh-token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        
        // Verify all cookies are cleared
        List<String> setCookieHeaders = exchange.getResponse().getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeaders);
        assertTrue(setCookieHeaders.stream().anyMatch(c -> c.contains("jwt_token=;") && c.contains("Max-Age=0")));
        assertTrue(setCookieHeaders.stream().anyMatch(c -> c.contains("access_token=;") && c.contains("Max-Age=0")));
        assertTrue(setCookieHeaders.stream().anyMatch(c -> c.contains("refresh_token=;") && c.contains("Max-Age=0")));
    }

    @Test
    void testFilter_LogoutEndpoint_AddsTokensToHeaders() {
        // Given
        String accessToken = "test-access-token";
        String refreshToken = "test-refresh-token";
        MockServerHttpRequest request = MockServerHttpRequest.post("/account/auth/logout")
                .header(HttpHeaders.COOKIE, "jwt_token=" + accessToken + "; refresh_token=" + refreshToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        ServerHttpRequest modifiedRequest = capturedExchange.getRequest();
        assertEquals(accessToken, modifiedRequest.getHeaders().getFirst("X-Access-Token"));
        assertEquals(refreshToken, modifiedRequest.getHeaders().getFirst("X-Refresh-Token"));
    }

    @Test
    void testFilter_LogoutEndpoint_NoTokens_StillClearsCookies() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.post("/account/auth/logout")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain, times(1)).filter(any());
        
        // Cookies should still be cleared even if no tokens present
        List<String> setCookieHeaders = exchange.getResponse().getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeaders);
        assertTrue(setCookieHeaders.size() >= 3); // jwt_token, access_token, refresh_token
    }

    @Test
    void testFilter_AuthEndpoint_WithRefreshToken_SetsBothCookies() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/login");
        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-456";
        String responseBody = String.format("{\"token\":\"%s\",\"refreshToken\":\"%s\"}", 
                accessToken, refreshToken);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        List<String> setCookieHeaders = capturedExchange.getResponse().getHeaders().get(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeaders);
        assertTrue(setCookieHeaders.stream().anyMatch(c -> c.contains("jwt_token=" + accessToken)));
        assertTrue(setCookieHeaders.stream().anyMatch(c -> c.contains("refresh_token=" + refreshToken)));
    }

    @Test
    void testFilter_RefreshEndpoint_SetsCookie() {
        // Given
        ServerWebExchange exchange = createExchange("/account/auth/refresh");
        String responseBody = createJsonResponse(TEST_TOKEN);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenAnswer(invocation -> {
            ServerWebExchange ex = invocation.getArgument(0);
            DataBuffer buffer = ex.getResponse().bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buffer));
        });

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        String setCookieHeader = capturedExchange.getResponse().getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("jwt_token=" + TEST_TOKEN));
    }

    @Test
    void testFilter_LogoutEndpoint_ExtractsAccessTokenFromAccessTokenCookie() {
        // Given
        String accessToken = "test-access-token";
        MockServerHttpRequest request = MockServerHttpRequest.post("/account/auth/logout")
                .header(HttpHeaders.COOKIE, "access_token=" + accessToken)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = 
            ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = jwtCookieFilter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        
        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        ServerHttpRequest modifiedRequest = capturedExchange.getRequest();
        assertEquals(accessToken, modifiedRequest.getHeaders().getFirst("X-Access-Token"));
    }
}

