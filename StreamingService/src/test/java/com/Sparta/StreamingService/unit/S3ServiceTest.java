package com.Sparta.StreamingService.unit;

import com.Sparta.StreamingService.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void getS3Url_normalizesLeadingSlashInFilePath() {
        String url = s3Service.getS3Url("v1", "/video/seg.mp4");
        assertThat(url).isEqualTo("s3://test-bucket/videos/v1/video/seg.mp4");
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
    void listAllVideos_returnsEmptyWhenNoCommonPrefixes() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().build());
        List<String> videos = s3Service.listAllVideos();
        assertThat(videos).isEmpty();
    }

    @Test
    void listAllVideos_returnsEmptyWhenCommonPrefixesNull() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().build());
        List<String> videos = s3Service.listAllVideos();
        assertThat(videos).isEmpty();
    }

    @Test
    void listVideoFiles_returnsKeysFromContents() {
        var obj = S3Object.builder().key("videos/vid1/manifest.mpd").build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(obj).build());
        List<String> files = s3Service.listVideoFiles("vid1");
        assertThat(files).containsExactly("videos/vid1/manifest.mpd");
    }

    @Test
    void listVideoFiles_returnsEmptyWhenNoContents() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().build());
        List<String> files = s3Service.listVideoFiles("vid1");
        assertThat(files).isEmpty();
    }

    @Test
    void objectExists_returnsTrueWhenHeadSucceeds() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(100L)
                .contentType("application/xml")
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);
        assertThat(s3Service.objectExists("v1", "manifest.mpd")).isTrue();
    }

    @Test
    void objectExists_returnsFalseWhenNoSuchKey() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("Not found").build());
        assertThat(s3Service.objectExists("v1", "missing.mpd")).isFalse();
    }

    @Test
    void getObjectMetadata_returnsMetadataWhenObjectExists() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .contentLength(256L)
                .contentType("application/dash+xml")
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);
        HeadObjectResponse result = s3Service.getObjectMetadata("v1", "manifest.mpd");
        assertThat(result.contentLength()).isEqualTo(256L);
        assertThat(result.contentType()).isEqualTo("application/dash+xml");
    }

    @Test
    void getObjectMetadata_throwsRuntimeExceptionWhenNoSuchKey() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("Not found").build());
        assertThatThrownBy(() -> s3Service.getObjectMetadata("v1", "missing.mpd"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void getObject_returnsStreamWhenObjectExists() {
        ResponseInputStream<GetObjectResponse> mockStream = new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(10L).build(),
                new ByteArrayInputStream("test data".getBytes()));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);
        ResponseInputStream<GetObjectResponse> result = s3Service.getObject("v1", "file.txt", null, null);
        assertThat(result).isNotNull();
    }

    @Test
    void getObject_throwsRuntimeExceptionWhenNoSuchKey() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().message("Not found").build());
        assertThatThrownBy(() -> s3Service.getObject("v1", "missing.txt", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("File not found");
    }
}
