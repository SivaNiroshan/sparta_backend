package com.Sparta.StreamingService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.ArrayList;
import java.util.List;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String basePath;

    public S3Service(S3Client s3Client,
                     @Value("${aws.s3.bucket-name}") String bucketName,
                     @Value("${aws.s3.base-path:videos}") String basePath) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.basePath = basePath;
    }

    /**
     * Get S3 object as InputStream with range support
     * 
     * @param videoId Video identifier
     * @param filePath Relative path to file (e.g., "manifest.mpd", "video/video_1080p_dash.mp4")
     * @param rangeStart Start byte position (null for full file)
     * @param rangeEnd End byte position (null for full file)
     * @return InputStream of the file content
     */
    public ResponseInputStream<GetObjectResponse> getObject(String videoId, String filePath, Long rangeStart, Long rangeEnd) {
        String s3Key = buildS3Key(videoId, filePath);
        
        try {
            GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key);

            // Add range header if specified
            if (rangeStart != null && rangeEnd != null) {
                String range = String.format("bytes=%d-%d", rangeStart, rangeEnd);
                requestBuilder.range(range);
                logger.debug("Requesting S3 object with range: {} for key: {}", range, s3Key);
            } else if (rangeStart != null) {
                String range = String.format("bytes=%d-", rangeStart);
                requestBuilder.range(range);
                logger.debug("Requesting S3 object with range: {} for key: {}", range, s3Key);
            }

            GetObjectRequest request = requestBuilder.build();
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            
            logger.debug("Successfully retrieved S3 object: {}", s3Key);
            return response;

        } catch (NoSuchKeyException e) {
            logger.warn("S3 object not found: {}", s3Key);
            throw new RuntimeException("File not found: " + s3Key, e);
        } catch (S3Exception e) {
            logger.error("S3 error retrieving object: {}", s3Key, e);
            throw new RuntimeException("S3 error retrieving object: " + s3Key, e);
        }
    }

    /**
     * Get object metadata (size, content type, etc.)
     */
    public HeadObjectResponse getObjectMetadata(String videoId, String filePath) {
        String s3Key = buildS3Key(videoId, filePath);
        
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            logger.debug("Retrieved metadata for S3 object: {}", s3Key);
            return response;

        } catch (NoSuchKeyException e) {
            logger.warn("S3 object not found: {}", s3Key);
            throw new RuntimeException("File not found: " + s3Key, e);
        } catch (S3Exception e) {
            logger.error("S3 error retrieving metadata: {}", s3Key, e);
            throw new RuntimeException("S3 error retrieving metadata: " + s3Key, e);
        }
    }

    /**
     * Check if object exists in S3
     */
    public boolean objectExists(String videoId, String filePath) {
        String s3Key = buildS3Key(videoId, filePath);
        logger.info("🔍 Checking if S3 object exists - Bucket: {}, Key: {}", bucketName, s3Key);
        
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            logger.info("✅ S3 object FOUND - Key: {}, Size: {} bytes, ContentType: {}", 
                s3Key, response.contentLength(), response.contentType());
            return true;

        } catch (NoSuchKeyException e) {
            logger.warn("❌ S3 object NOT FOUND - Bucket: {}, Key: {}", bucketName, s3Key);
            return false;
        } catch (S3Exception e) {
            logger.error("⚠️ S3 error checking object - Bucket: {}, Key: {}, Error: {}", 
                bucketName, s3Key, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * List all videos in S3 bucket
     */
    public List<String> listAllVideos() {
        logger.info("📋 Listing all videos in S3 - Bucket: {}, BasePath: {}", bucketName, basePath);
        List<String> videos = new ArrayList<>();
        String prefix = basePath + "/";
        
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .delimiter("/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            if (response.commonPrefixes() != null) {
                logger.info("📁 Found {} video folders", response.commonPrefixes().size());
                for (var commonPrefix : response.commonPrefixes()) {
                    String videoId = commonPrefix.prefix()
                            .replace(prefix, "")
                            .replace("/", "");
                    if (!videoId.isEmpty()) {
                        videos.add(videoId);
                        logger.info("  ✓ Video ID: {}", videoId);
                    }
                }
            } else {
                logger.warn("⚠️ No video folders found in S3");
            }
            
            logger.info("✅ Total videos found: {}", videos.size());
            return videos;
        } catch (S3Exception e) {
            logger.error("❌ Error listing videos from S3: {}", e.getMessage(), e);
            return videos;
        }
    }
    
    /**
     * List all files for a specific video
     */
    public List<String> listVideoFiles(String videoId) {
        logger.info("📋 Listing files for video: {} - Bucket: {}, BasePath: {}", videoId, bucketName, basePath);
        List<String> files = new ArrayList<>();
        String prefix = basePath + "/" + videoId + "/";
        
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            if (response.contents() != null) {
                logger.info("📄 Found {} files for video: {}", response.contents().size(), videoId);
                for (var s3Object : response.contents()) {
                    String key = s3Object.key();
                    files.add(key);
                    logger.info("  ✓ File: {} (Size: {} bytes)", key, s3Object.size());
                }
            } else {
                logger.warn("⚠️ No files found for video: {}", videoId);
            }
            
            return files;
        } catch (S3Exception e) {
            logger.error("❌ Error listing files for video {}: {}", videoId, e.getMessage(), e);
            return files;
        }
    }

    /**
     * Build S3 key from video ID and file path
     * Structure: videos/{videoId}/{filePath}
     */
    private String buildS3Key(String videoId, String filePath) {
        // Normalize file path (remove leading slashes)
        String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        String s3Key = String.format("%s/%s/%s", basePath, videoId, normalizedPath);
        logger.debug("Built S3 key: {} (basePath: {}, videoId: {}, filePath: {})", s3Key, basePath, videoId, normalizedPath);
        return s3Key;
    }

    /**
     * Get full S3 URL for a file (useful for direct CDN access if configured)
     */
    public String getS3Url(String videoId, String filePath) {
        String s3Key = buildS3Key(videoId, filePath);
        return String.format("s3://%s/%s", bucketName, s3Key);
    }
}

