package com.Sparta.StreamingService.integration;

import com.Sparta.StreamingService.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Integration tests for StreamingService S3 with real S3-compatible storage using Testcontainers (LocalStack).
 * Uses application-test.properties; S3 endpoint and credentials are overridden via @DynamicPropertySource.
 * Unit tests should use mocks instead of this class.
 * Requires Docker – remove @Disabled when Docker is available.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Disabled("Requires Docker - remove @Disabled when Docker is available to run")
class S3StreamingIntegrationTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3);

    @DynamicPropertySource
    static void configureS3(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint-override", () -> localStack.getEndpointOverride(S3).toString());
        registry.add("aws.s3.access-key", localStack::getAccessKey);
        registry.add("aws.s3.secret-key", localStack::getSecretKey);
        registry.add("aws.s3.region", localStack::getRegion);
        registry.add("aws.s3.bucket-name", () -> "sparta-streaming-test-bucket");
        registry.add("aws.s3.base-path", () -> "videos-test");
        createBucketInLocalStack();
    }

    private static void createBucketInLocalStack() {
        try (S3Client client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build()) {
            client.createBucket(CreateBucketRequest.builder().bucket("sparta-streaming-test-bucket").build());
        }
    }

    @Autowired
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        // Seed test object so list/head tests have something to work with
        try (S3Client client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build()) {
            client.putObject(
                    PutObjectRequest.builder()
                            .bucket("sparta-streaming-test-bucket")
                            .key("videos-test/integration-video-1/manifest.mpd")
                            .build(),
                    RequestBody.fromString("<MPD>test</MPD>"));
        }
    }

    @Test
    void listAllVideos_returnsVideosFromLocalStack() {
        var videos = s3Service.listAllVideos();
        assertThat(videos).contains("integration-video-1");
    }

    @Test
    void objectExists_returnsTrueForExistingObject() {
        boolean exists = s3Service.objectExists("integration-video-1", "manifest.mpd");
        assertThat(exists).isTrue();
    }

    @Test
    void objectExists_returnsFalseForMissingObject() {
        boolean exists = s3Service.objectExists("integration-video-1", "nonexistent.mpd");
        assertThat(exists).isFalse();
    }
}
