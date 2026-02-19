package com.Sparta.StreamingService.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Integration tests for StreamingController HTTP endpoints with real S3 (LocalStack).
 * Full Spring Boot context, random port, TestRestTemplate.
 * Requires Docker – remove @Disabled when Docker is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Disabled("Requires Docker - remove @Disabled when Docker is available to run")
class StreamingControllerIntegrationTest {

    private static final String BUCKET = "sparta-streaming-test-bucket";
    private static final String BASE_PATH = "videos-test";
    private static final String VIDEO_ID = "streaming-integration-video";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3);

    @DynamicPropertySource
    static void configureS3(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint-override", () -> localStack.getEndpointOverride(S3).toString());
        registry.add("aws.s3.access-key", localStack::getAccessKey);
        registry.add("aws.s3.secret-key", localStack::getSecretKey);
        registry.add("aws.s3.region", localStack::getRegion);
        registry.add("aws.s3.bucket-name", () -> BUCKET);
        registry.add("aws.s3.base-path", () -> BASE_PATH);
        try (S3Client client = buildClient()) {
            client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    private static S3Client buildClient() {
        return S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        try (S3Client client = buildClient()) {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(BASE_PATH + "/" + VIDEO_ID + "/manifest.mpd")
                            .contentType("application/dash+xml")
                            .build(),
                    RequestBody.fromString("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\">content</MPD>"));
        }
    }

    @Test
    void getManifest_returns200AndBodyWhenVideoExists() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/stream/" + VIDEO_ID + "/manifest.mpd",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content");
        assertThat(response.getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("manifest.mpd");
    }

    @Test
    void getManifest_returns404WhenVideoNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/stream/nonexistent-video-id-12345/manifest.mpd",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getFile_pathTraversal_returns400() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/stream/v1/../etc/passwd",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
