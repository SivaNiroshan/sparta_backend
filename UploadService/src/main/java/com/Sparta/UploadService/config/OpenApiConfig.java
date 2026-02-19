package com.Sparta.UploadService.config;

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
    public OpenAPI uploadServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UploadService API")
                        .description("REST API documentation for UploadService - handles video file uploads using TUS protocol, " +
                                "manages upload sessions, pause/resume/cancel operations, and integrates with encoding pipeline. " +
                                "This service implements the TUS (Resumable Upload Protocol) specification for reliable large file uploads.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sparta Team")
                                .email("support@sparta.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://upload.sparta.com")
                                .description("Production Server")
                ));
    }
}
