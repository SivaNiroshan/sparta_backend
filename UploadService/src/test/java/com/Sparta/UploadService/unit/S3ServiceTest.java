package com.Sparta.UploadService.unit;

import com.Sparta.UploadService.S3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for S3Service with mocked S3Client.
 * Tests business logic without actual S3 calls.
 */
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;
    private String bucketName = "test-bucket";
    private String basePath = "videos";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, bucketName, basePath);
    }

    @Test
    void testUploadDashFilesToS3_Success() throws IOException {
        // Arrange
        String videoId = "test-video-123";
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "dash_outputtest");
        Files.createDirectories(tempDir);
        String dashOutputDir = tempDir.toString();
        Files.createFile(tempDir.resolve("manifest.mpd"));
        Files.createFile(tempDir.resolve("testvideo_1080p_dash.mp4"));
        Files.createFile(tempDir.resolve("testvideo_720p_dash.mp4"));
        Files.createFile(tempDir.resolve("testaudio.mp4"));
        
        List<Integer> qualities = Arrays.asList(1080, 720);
        
        // Mock S3Client - putObject returns PutObjectResponse (not void)
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        try {
            // Act
            String result = s3Service.uploadDashFilesToS3(videoId, dashOutputDir, qualities);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains(bucketName));
            assertTrue(result.contains(videoId));
            
            // Verify S3Client was called for manifest, 2 video files, and 1 audio = 4 times
            verify(s3Client, atLeast(4)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        } finally {
            deleteDirectory(tempDir);
        }
    }

    @Test
    void testUploadDashFilesToS3_DirectoryNotFound() {
        // Arrange
        String videoId = "test-video-123";
        String nonExistentDir = "/nonexistent/directory";
        List<Integer> qualities = Arrays.asList(1080);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            s3Service.uploadDashFilesToS3(videoId, nonExistentDir, qualities);
        });
        
        // Verify S3Client was never called
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void testUploadDashFilesToS3_S3Exception() throws IOException {
        // Arrange
        String videoId = "test-video-123";
        Path tempDir = Files.createTempDirectory("dash_output");
        String dashOutputDir = tempDir.toString();
        Files.createFile(tempDir.resolve("manifest.mpd"));
        List<Integer> qualities = Arrays.asList(1080);
        
        // Mock S3Client to throw S3Exception
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
                .message("Access Denied")
                .statusCode(403)
                .build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Exception);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            s3Service.uploadDashFilesToS3(videoId, dashOutputDir, qualities);
        });
        
        // Cleanup
        deleteDirectory(tempDir);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteFile_Success() {
        // Arrange
        String s3Key = "videos/test-video-123/manifest.mpd";
        
        // Mock S3Client deleteObject - returns DeleteObjectResponse (not void)
        when(s3Client.deleteObject((Consumer<DeleteObjectRequest.Builder>) any()))
                .thenReturn(DeleteObjectResponse.builder().build());

        // Act
        assertDoesNotThrow(() -> s3Service.deleteFile(s3Key));

        // Assert
        verify(s3Client, times(1)).deleteObject((Consumer<DeleteObjectRequest.Builder>) any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteFile_ExceptionHandled() {
        // Arrange
        String s3Key = "videos/test-video-123/manifest.mpd";
        
        // Mock S3Client to throw exception - uses Consumer<DeleteObjectRequest.Builder>
        when(s3Client.deleteObject((Consumer<DeleteObjectRequest.Builder>) any()))
                .thenThrow(new RuntimeException("S3 error"));

        // Act - Should not throw, but log error (exception is caught in service)
        assertDoesNotThrow(() -> s3Service.deleteFile(s3Key));
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
