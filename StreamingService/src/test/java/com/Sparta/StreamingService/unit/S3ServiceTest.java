package com.Sparta.StreamingService.unit;

import com.Sparta.StreamingService.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for S3Service with mocked S3Client. No real AWS or Testcontainers.
 */
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;
    private static final String BUCKET = "test-bucket";
    private static final String BASE_PATH = "videos";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, BUCKET, BASE_PATH);
    }

    @Test
    void getS3Url_buildsCorrectUrl() {
        String url = s3Service.getS3Url("video-1", "manifest.mpd");
        assertThat(url).isEqualTo("s3://test-bucket/videos/video-1/manifest.mpd");
    }

    @Test
    void listAllVideos_returnsVideoIdsFromCommonPrefixes() {
        var commonPrefix = software.amazon.awssdk.services.s3.model.CommonPrefix.builder()
                .prefix("videos/v1/")
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().commonPrefixes(commonPrefix).build());
        List<String> videos = s3Service.listAllVideos();
        assertThat(videos).containsExactly("v1");
    }

    @Test
    void listVideoFiles_returnsKeysFromContents() {
        var obj = S3Object.builder().key("videos/vid1/manifest.mpd").build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(obj).build());
        List<String> files = s3Service.listVideoFiles("vid1");
        assertThat(files).containsExactly("videos/vid1/manifest.mpd");
    }
}
