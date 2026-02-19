package com.Sparta.UploadService.integration;

import com.Sparta.UploadService.S3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

/**
 * Integration tests for S3Service with LocalStack (S3-compatible).
 * Tests actual S3 upload/download operations without AWS account.
 * Requires Docker to be running - remove @Disabled to run when Docker is available.
 */
@Testcontainers
@Disabled("Requires Docker - run with Docker available to execute these tests")
class S3ServiceIntegrationTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3);

    private S3Client s3Client;
    private S3Service s3Service;
    private String bucketName = "test-bucket";
    private String basePath = "videos-test";

    @BeforeEach
    void setUp() {
        // Create S3Client pointing to LocalStack
        s3Client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build();

        // Create bucket
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            // Bucket might already exist, ignore
        }

        // Create S3Service with LocalStack client
        s3Service = new S3Service(s3Client, bucketName, basePath);
    }

    @Test
    void testUploadDashFilesToS3_RealS3Upload() throws IOException {
        // Arrange
        String videoId = "integration-test-video-123";
        Path tempDir = Files.createTempDirectory("dash_output");
        String dashOutputDir = tempDir.toString();
        
        // Create test files
        Files.write(tempDir.resolve("manifest.mpd"), "<manifest>test</manifest>".getBytes());
        Files.write(tempDir.resolve("test_video_1080p_dash.mp4"), "fake video data".getBytes());
        Files.write(tempDir.resolve("test_video_720p_dash.mp4"), "fake video data".getBytes());
        Files.write(tempDir.resolve("test_audio.mp4"), "fake audio data".getBytes());
        
        List<Integer> qualities = Arrays.asList(1080, 720);

        // Act
        String result = s3Service.uploadDashFilesToS3(videoId, dashOutputDir, qualities);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(bucketName));
        assertTrue(result.contains(videoId));
        
        // Verify files were actually uploaded to S3
        assertTrue(s3ObjectExists(String.format("%s/%s/manifest.mpd", basePath, videoId)));
        assertTrue(s3ObjectExists(String.format("%s/%s/video/test_video_1080p_dash.mp4", basePath, videoId)));
        assertTrue(s3ObjectExists(String.format("%s/%s/video/test_video_720p_dash.mp4", basePath, videoId)));
        assertTrue(s3ObjectExists(String.format("%s/%s/audio/test_audio.mp4", basePath, videoId)));
        
        // Cleanup
        deleteDirectory(tempDir);
    }

    @Test
    void testDeleteFile_RealS3Delete() throws IOException {
        // Arrange - Upload a file first
        String s3Key = basePath + "/test-delete/file.txt";
        s3Client.putObject(builder -> builder
                .bucket(bucketName)
                .key(s3Key)
                .build(), RequestBody.fromString("test content"));
        
        assertTrue(s3ObjectExists(s3Key));

        // Act
        s3Service.deleteFile(s3Key);

        // Assert - File should be deleted
        assertFalse(s3ObjectExists(s3Key));
    }

    private boolean s3ObjectExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
    }
}
