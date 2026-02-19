package com.Sparta.StreamingService.unit;

import com.Sparta.StreamingService.service.NetworkQualityService;
import com.Sparta.StreamingService.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for StreamingController with mocked S3Service and NetworkQualityService.
 */
@WebMvcTest(controllers = com.Sparta.StreamingService.controller.StreamingController.class)
@ActiveProfiles("test")
class StreamingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Service s3Service;

    @MockBean
    private NetworkQualityService networkQualityService;

    @Test
    void getManifest_returns404WhenNoVideosInS3() throws Exception {
        when(s3Service.listAllVideos()).thenReturn(List.of());
        mockMvc.perform(get("/stream/video-1/manifest.mpd").accept("application/dash+xml"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Error-Message", "No videos found in S3 bucket"));
    }

    @Test
    void getManifest_returns404WhenVideoNotFound() throws Exception {
        when(s3Service.listAllVideos()).thenReturn(List.of("other-video"));
        mockMvc.perform(get("/stream/nonexistent/manifest.mpd").accept("application/dash+xml"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Error-Message"));
    }

    @Test
    void getManifest_returns404WhenNoFilesForVideo() throws Exception {
        when(s3Service.listAllVideos()).thenReturn(List.of("video-1"));
        when(s3Service.listVideoFiles("video-1")).thenReturn(List.of());
        mockMvc.perform(get("/stream/video-1/manifest.mpd").accept("application/dash+xml"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getManifest_returns404WhenManifestNotPresent() throws Exception {
        when(s3Service.listAllVideos()).thenReturn(List.of("video-1"));
        when(s3Service.listVideoFiles("video-1")).thenReturn(List.of("video/seg.mp4"));
        when(s3Service.objectExists(eq("video-1"), anyString())).thenReturn(false);
        mockMvc.perform(get("/stream/video-1/manifest.mpd").accept("application/dash+xml"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getManifest_returns200WithManifestWhenFound() throws Exception {
        when(s3Service.listAllVideos()).thenReturn(List.of("video-1"));
        when(s3Service.listVideoFiles("video-1")).thenReturn(List.of("videos/video-1/manifest.mpd"));
        when(s3Service.objectExists("video-1", "manifest.mpd")).thenReturn(true);
        ResponseInputStream<GetObjectResponse> stream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream("<MPD/>".getBytes()));
        when(s3Service.getObject(eq("video-1"), eq("manifest.mpd"), isNull(), isNull())).thenReturn(stream);
        HeadObjectResponse meta = HeadObjectResponse.builder().contentLength(6L).contentType("application/dash+xml").build();
        when(s3Service.getObjectMetadata(eq("video-1"), eq("manifest.mpd"))).thenReturn(meta);

        mockMvc.perform(get("/stream/video-1/manifest.mpd").accept("application/dash+xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/dash+xml"))
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"manifest.mpd\""));
    }

    @Test
    void getFile_returns400ForPathTraversal() throws Exception {
        mockMvc.perform(get("/stream/v1/../etc/passwd"))
                .andExpect(status().isBadRequest());
    }
}
