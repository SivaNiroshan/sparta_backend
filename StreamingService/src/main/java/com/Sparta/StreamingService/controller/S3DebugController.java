package com.Sparta.StreamingService.controller;

import com.Sparta.StreamingService.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Debug S3", description = "S3 debugging: list videos, list files per video, check file existence. Restrict or remove in production.")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/debug/s3")
public class S3DebugController {

    private static final Logger logger = LoggerFactory.getLogger(S3DebugController.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String basePath;
    private final S3Service s3Service;

    public S3DebugController(S3Client s3Client,
                            @Value("${aws.s3.bucket-name}") String bucketName,
                            @Value("${aws.s3.base-path:videos}") String basePath,
                            S3Service s3Service) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.basePath = basePath;
        this.s3Service = s3Service;
    }

    @Operation(summary = "List videos", description = "Returns bucket, basePath, and list of video IDs (folder names) in S3.")
    @GetMapping("/videos")
    public ResponseEntity<Map<String, Object>> listVideos() {
        try {
            List<String> videos = new ArrayList<>();
            String prefix = basePath + "/";
            
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .delimiter("/")
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            // Get common prefixes (video folders)
            if (response.commonPrefixes() != null) {
                for (var commonPrefix : response.commonPrefixes()) {
                    String videoId = commonPrefix.prefix()
                            .replace(prefix, "")
                            .replace("/", "");
                    if (!videoId.isEmpty()) {
                        videos.add(videoId);
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("bucket", bucketName);
            result.put("basePath", basePath);
            result.put("videos", videos);
            result.put("count", videos.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error listing videos from S3", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "List video files", description = "Returns files (key, size, lastModified) for a video, plus manifestExists and mpdFiles.")
    @GetMapping("/videos/{videoId}/files")
    public ResponseEntity<Map<String, Object>> listVideoFiles(@PathVariable String videoId) {
        try {
            List<Map<String, String>> files = new ArrayList<>();
            String prefix = basePath + "/" + videoId + "/";
            
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            if (response.contents() != null) {
                for (S3Object s3Object : response.contents()) {
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("key", s3Object.key());
                    fileInfo.put("size", String.valueOf(s3Object.size()));
                    fileInfo.put("lastModified", s3Object.lastModified().toString());
                    files.add(fileInfo);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("videoId", videoId);
            result.put("bucket", bucketName);
            result.put("prefix", prefix);
            result.put("files", files);
            result.put("count", files.size());
            
            // Check if manifest exists with different variations
            boolean manifestExists = s3Service.objectExists(videoId, "manifest.mpd");
            boolean maifestExists = s3Service.objectExists(videoId, "maifest.mpd");
            
            result.put("manifestExists", manifestExists);
            result.put("maifestExists", maifestExists); // Check for typo
            result.put("expectedManifestKey", basePath + "/" + videoId + "/manifest.mpd");
            
            // Find any .mpd files
            List<String> mpdFiles = new ArrayList<>();
            if (response.contents() != null) {
                for (S3Object s3Object : response.contents()) {
                    if (s3Object.key().endsWith(".mpd") || s3Object.key().endsWith(".xml")) {
                        mpdFiles.add(s3Object.key());
                    }
                }
            }
            result.put("mpdFiles", mpdFiles);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error listing files for video: {}", videoId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Check file exists", description = "Returns whether the given file path exists for the video in S3 and the expected S3 key.")
    @GetMapping("/check/{videoId}/{filePath}")
    public ResponseEntity<Map<String, Object>> checkFile(
            @PathVariable String videoId,
            @PathVariable String filePath) {
        try {
            boolean exists = s3Service.objectExists(videoId, filePath);
            String expectedKey = basePath + "/" + videoId + "/" + filePath;
            
            Map<String, Object> result = new HashMap<>();
            result.put("videoId", videoId);
            result.put("filePath", filePath);
            result.put("exists", exists);
            result.put("expectedS3Key", expectedKey);
            result.put("bucket", bucketName);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error checking file: {}/{}", videoId, filePath, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}

