package com.Sparta.GatewayService.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/gateway")
@Tag(name = "Gateway Service", description = "API Gateway endpoints and route information")
public class GatewayController {

    @Operation(
            summary = "Get Gateway Information",
            description = "Returns information about the Gateway Service including available routes and services"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Gateway information retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/info")
    public Mono<Map<String, Object>> getGatewayInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "GatewayService");
        info.put("version", "1.0.0");
        info.put("port", 8000);
        
        Map<String, Object> routes = new HashMap<>();
        
        Map<String, Object> userService = new HashMap<>();
        userService.put("path", "/account/**");
        userService.put("target", "http://localhost:8081");
        userService.put("description", "Routes to UserService - handles authentication, user management, and friend management");
        routes.put("userService", userService);
        
        Map<String, Object> uploadService = new HashMap<>();
        uploadService.put("path", "/upload/**");
        uploadService.put("target", "http://localhost:8082");
        uploadService.put("description", "Routes to UploadService - handles file uploads");
        routes.put("uploadService", uploadService);
        
        Map<String, Object> streamingService = new HashMap<>();
        streamingService.put("path", "/stream/**");
        streamingService.put("target", "http://localhost:8083");
        streamingService.put("description", "Routes to StreamingService - handles media streaming");
        routes.put("streamingService", streamingService);
        
        info.put("routes", routes);
        
        Map<String, Object> features = new HashMap<>();
        features.put("jwtAuthentication", "JWT token validation for protected endpoints");
        features.put("cors", "Cross-Origin Resource Sharing enabled");
        features.put("cookieHandling", "Automatic JWT cookie extraction and management");
        info.put("features", features);
        
        return Mono.just(info);
    }
}
