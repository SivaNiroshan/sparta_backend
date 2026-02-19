package com.Sparta.StreamingService.config;

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
    public OpenAPI streamingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StreamingService API")
                        .description("REST API documentation for StreamingService - DASH video streaming, manifest and segment delivery from S3, " +
                                "bandwidth measurement and quality selection. Endpoints: /stream (manifest, video/audio segments, catch-all file), " +
                                "/bandwidth (report, quality select, statistics, test), /debug/s3 (videos list, files, check).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sparta Team")
                                .email("support@sparta.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8083")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://streaming.sparta.com")
                                .description("Production Server")
                ));
    }
}
