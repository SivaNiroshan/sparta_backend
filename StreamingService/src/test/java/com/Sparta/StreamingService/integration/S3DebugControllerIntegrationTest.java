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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Integration tests for S3DebugController HTTP endpoints with real S3 (LocalStack).
 * Requires Docker – remove @Disabled when Docker is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Disabled("Requires Docker - remove @Disabled when Docker is available to run")
class S3DebugControllerIntegrationTest {

    private static final String BUCKET = "sparta-streaming-test-bucket";
    private static final String BASE_PATH = "videos-test";
    private static final String VIDEO_ID = "debug-integration-video";

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
                            .build(),
                    RequestBody.fromString("<MPD/>"));
        }
    }

    @Test
    void listVideos_returnsVideosFromS3() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/debug/s3/videos", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull().containsEntry("bucket", BUCKET).containsEntry("basePath", BASE_PATH).containsKey("count");
        @SuppressWarnings("unchecked")
        List<String> videos = (List<String>) body.get("videos");
        assertThat(videos).contains(VIDEO_ID);
    }

    @Test
    void listVideoFiles_returnsFilesAndManifestFlags() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/debug/s3/videos/" + VIDEO_ID + "/files",
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("videoId", VIDEO_ID);
        assertThat(response.getBody()).containsKey("files");
        assertThat(response.getBody()).containsKey("manifestExists");
        assertThat(response.getBody()).containsKey("expectedManifestKey");
    }

    @Test
    void checkFile_returnsExistsTrueWhenFilePresent() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/debug/s3/check/" + VIDEO_ID + "/manifest.mpd",
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("exists", true);
        assertThat(response.getBody()).containsEntry("videoId", VIDEO_ID);
    }

    @Test
    void checkFile_returnsExistsFalseWhenFileMissing() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/debug/s3/check/" + VIDEO_ID + "/nonexistent.mpd",
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("exists", false);
    }
}
