package com.Sparta.StreamingService.unit;

import com.Sparta.StreamingService.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for S3DebugController with mocked S3Client and S3Service.
 */
@WebMvcTest(controllers = com.Sparta.StreamingService.controller.S3DebugController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"aws.s3.bucket-name=test-bucket", "aws.s3.base-path=videos"})
class S3DebugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Client s3Client;

    @MockBean
    private S3Service s3Service;

    @Test
    void listVideos_returnsVideosFromS3() throws Exception {
        var prefix = software.amazon.awssdk.services.s3.model.CommonPrefix.builder()
                .prefix("videos/v1/")
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(
                ListObjectsV2Response.builder().commonPrefixes(prefix).build());
        mockMvc.perform(get("/debug/s3/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bucket").value("test-bucket"))
                .andExpect(jsonPath("$.basePath").value("videos"))
                .andExpect(jsonPath("$.videos").isArray())
                .andExpect(jsonPath("$.videos[0]").value("v1"))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void listVideoFiles_returnsFilesAndManifestFlags() throws Exception {
        S3Object obj = S3Object.builder()
                .key("videos/vid1/manifest.mpd")
                .size(100L)
                .lastModified(java.time.Instant.now())
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(
                ListObjectsV2Response.builder().contents(obj).build());
        when(s3Service.objectExists("vid1", "manifest.mpd")).thenReturn(true);
        when(s3Service.objectExists("vid1", "maifest.mpd")).thenReturn(false);

        mockMvc.perform(get("/debug/s3/videos/vid1/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value("vid1"))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.manifestExists").value(true))
                .andExpect(jsonPath("$.expectedManifestKey").value("videos/vid1/manifest.mpd"));
    }

    @Test
    void checkFile_returnsExistsTrueWhenFilePresent() throws Exception {
        when(s3Service.objectExists("v1", "manifest.mpd")).thenReturn(true);
        mockMvc.perform(get("/debug/s3/check/v1/manifest.mpd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value("v1"))
                .andExpect(jsonPath("$.filePath").value("manifest.mpd"))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.expectedS3Key").value("videos/v1/manifest.mpd"));
    }

    @Test
    void checkFile_returnsExistsFalseWhenFileMissing() throws Exception {
        when(s3Service.objectExists("v1", "missing.mpd")).thenReturn(false);
        mockMvc.perform(get("/debug/s3/check/v1/missing.mpd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }
}
