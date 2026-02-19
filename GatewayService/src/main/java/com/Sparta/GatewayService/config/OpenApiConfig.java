package com.Sparta.GatewayService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gateway Service API")
                        .description("API Gateway documentation for Sparta Backend - Routes requests to UserService, UploadService, and StreamingService. " +
                                "This gateway handles authentication, CORS, and request routing.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sparta Team")
                                .email("support@sparta.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8000")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://gateway.sparta.com")
                                .description("Production Server")
                ));
    }
}
