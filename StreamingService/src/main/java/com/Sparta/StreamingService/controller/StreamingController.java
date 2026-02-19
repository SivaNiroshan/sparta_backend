package com.Sparta.StreamingService.controller;

import com.Sparta.StreamingService.service.NetworkQualityService;
import com.Sparta.StreamingService.service.S3Service;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Streaming", description = "DASH manifest and video/audio segment delivery from S3")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/stream")
public class StreamingController {

    private static final Logger logger = LoggerFactory.getLogger(StreamingController.class);
    
    private final NetworkQualityService networkQualityService;
    private final S3Service s3Service;

    public StreamingController(
            NetworkQualityService networkQualityService,
            S3Service s3Service) {
        this.networkQualityService = networkQualityService;
        this.s3Service = s3Service;
    }

    @Operation(summary = "Get DASH manifest (MPD)", description = "Returns the manifest.mpd file for the given video ID from S3")
    @GetMapping(value = "/{videoId}/manifest.mpd", produces = "application/dash+xml")
    public ResponseEntity<Resource> getManifest(@PathVariable String videoId) {
        try {
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("🎬 REQUEST: Getting manifest for video ID: {}", videoId);
            logger.info("═══════════════════════════════════════════════════════════");
            
            // Step 1: List all videos in S3
            logger.info("📋 STEP 1: Checking all videos in S3...");
            List<String> allVideos = s3Service.listAllVideos();
            
            if (allVideos.isEmpty()) {
                logger.error("❌ No videos found in S3 bucket!");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Error-Message", "No videos found in S3 bucket")
                    .build();
            }
            
            // Step 2: Check if specific video exists (with flexible matching)
            logger.info("📋 STEP 2: Checking if video '{}' exists...", videoId);
            String actualVideoId = findMatchingVideoId(videoId, allVideos);
            if (actualVideoId == null) {
                logger.warn("❌ Video '{}' NOT FOUND in S3!", videoId);
                logger.warn("📝 Available videos: {}", allVideos);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Error-Message", String.format("Video '%s' not found. Available videos: %s", videoId, allVideos))
                    .build();
            }
            
            // Use the actual video ID from S3 (in case of slight differences)
            if (!actualVideoId.equals(videoId)) {
                logger.info("✅ Video '{}' FOUND in S3! (matched as '{}')", videoId, actualVideoId);
                videoId = actualVideoId; // Update to use the actual ID
            } else {
                logger.info("✅ Video '{}' FOUND in S3!", videoId);
            }
            
            // Step 3: List all files for this video
            logger.info("📋 STEP 3: Listing all files for video '{}'...", videoId);
            List<String> videoFiles = s3Service.listVideoFiles(videoId);
            if (videoFiles.isEmpty()) {
                logger.error("❌ No files found for video '{}'!", videoId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Error-Message", String.format("No files found for video: %s", videoId))
                    .build();
            }
            logger.info("✅ Found {} files for video '{}'", videoFiles.size(), videoId);
            
            // Step 4: Check for manifest.mpd
            logger.info("📋 STEP 4: Checking for manifest.mpd...");
            String manifestPath = findManifestFile(videoId);
            if (manifestPath == null) {
                logger.error("❌ Manifest.mpd NOT FOUND for video: {}", videoId);
                logger.error("📝 Available files for this video:");
                for (String file : videoFiles) {
                    logger.error("   - {}", file);
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Error-Message", String.format("Manifest not found for video: %s. Available files: %s", videoId, videoFiles))
                    .build();
            }
            
            logger.info("✅ Manifest FOUND at: {}", manifestPath);
            logger.info("═══════════════════════════════════════════════════════════");

            ResponseInputStream<GetObjectResponse> s3Object = s3Service.getObject(videoId, manifestPath, null, null);
            HeadObjectResponse metadata = s3Service.getObjectMetadata(videoId, manifestPath);
            
            Resource resource = new InputStreamResource(s3Object);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/dash+xml"))
                    .contentLength(metadata.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"manifest.mpd\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving manifest from S3 for video: {}", videoId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get video segment", description = "Returns a video segment by quality (e.g. 720, 1080 or 'auto') and filename. Supports Range requests for bandwidth measurement.")
    @GetMapping("/{videoId}/video/{quality}/{filename}")
    public ResponseEntity<Resource> getVideoSegment(
            @PathVariable String videoId,
            @PathVariable String quality,
            @PathVariable String filename,
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Get or generate session ID
            String session = (sessionId != null && !sessionId.isEmpty()) 
                ? sessionId 
                : generateSessionId(request);
            
            // Auto-select quality based on bandwidth if "auto" is requested
            // Or use manually requested quality if set
            String actualQuality = quality;
            if ("auto".equalsIgnoreCase(quality)) {
                // Check if user has manually requested a quality
                var measurement = networkQualityService.getBandwidthMeasurement(session);
                if (measurement != null && measurement.getRequestedQuality() != null) {
                    // User has manually selected a quality, use it
                    actualQuality = String.valueOf(measurement.getRequestedQuality());
                    logger.debug("Using manually requested quality {} for session {}", actualQuality, session);
                } else {
                    // No manual selection, use bandwidth-based recommendation
                    int recommendedQuality = networkQualityService.determineQuality(null, session);
                    actualQuality = String.valueOf(recommendedQuality);
                    logger.debug("Auto-selected quality {} for session {}", actualQuality, session);
                }
            } else {
                // Quality is explicitly specified, use it
                logger.debug("Using explicitly requested quality {} for session {}", quality, session);
            }
            
            // Build S3 file path: video/{filename}
            // Adjust filename if quality changed
            String requestedFilename = filename;
            if (!quality.equals(actualQuality)) {
                requestedFilename = filename.replace(quality + "p", actualQuality + "p");
            }
            
            // Try to find the file (may be in video/ folder or root)
            String s3FilePath = findFileInS3(videoId, requestedFilename);
            if (s3FilePath == null) {
                // Try with video/ prefix explicitly
                s3FilePath = "video/" + requestedFilename;
                if (!s3Service.objectExists(videoId, s3FilePath)) {
                    logger.warn("Video segment not found in S3: {} or video/{} for video: {}", requestedFilename, requestedFilename, videoId);
                    return ResponseEntity.notFound().build();
                }
            }
            
            logger.debug("Using S3 path for video segment: {}", s3FilePath);

            // Get metadata
            HeadObjectResponse metadata = s3Service.getObjectMetadata(videoId, s3FilePath);
            long fileSize = metadata.contentLength();
            
            // Parse range header if present
            Long rangeStart = null;
            Long rangeEnd = null;
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String range = rangeHeader.substring(6);
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    rangeStart = parts[0].isEmpty() ? null : Long.parseLong(parts[0]);
                    rangeEnd = parts[1].isEmpty() ? null : Long.parseLong(parts[1]);
                }
            }

            // Get object from S3 with range if specified
            ResponseInputStream<GetObjectResponse> s3Object = s3Service.getObject(
                videoId, s3FilePath, rangeStart, rangeEnd
            );

            // Measure bandwidth if Range header is present
            if (rangeHeader != null && sessionId != null && rangeStart != null && rangeEnd != null) {
                long downloadTime = System.currentTimeMillis() - startTime;
                double bandwidth = networkQualityService.estimateBandwidthFromRange(
                    rangeStart, rangeEnd, downloadTime
                );
                String clientIp = getClientIpAddress(request);
                networkQualityService.recordBandwidth(session, clientIp, bandwidth);
                logger.debug("Measured bandwidth from Range request: {} Mbps for session {}", 
                    bandwidth, session);
            }

            Resource resource = new InputStreamResource(s3Object);
            String contentType = metadata.contentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "video/mp4";
            }

            // Handle partial content (206) for range requests
            if (rangeHeader != null && rangeStart != null) {
                long contentLength = (rangeEnd != null ? rangeEnd : fileSize - 1) - rangeStart + 1;
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session)
                    .header("X-Quality-Served", actualQuality)
                    .header(HttpHeaders.CONTENT_RANGE, 
                        String.format("bytes %d-%d/%d", rangeStart, 
                            rangeEnd != null ? rangeEnd : fileSize - 1, fileSize))
                    .contentLength(contentLength)
                    .body(resource);
            } else {
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session)
                    .header("X-Quality-Served", actualQuality)
                    .contentLength(fileSize)
                    .body(resource);
            }
        } catch (Exception e) {
            logger.error("Error serving video segment from S3", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get audio segment", description = "Returns an audio segment file for the given video. Supports Range requests.")
    @GetMapping("/{videoId}/audio/{filename}")
    public ResponseEntity<Resource> getAudioSegment(
            @PathVariable String videoId,
            @PathVariable String filename,
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request) {
        try {
            // Try to find the file (may be in audio/ folder or root)
            String s3FilePath = findFileInS3(videoId, filename);
            if (s3FilePath == null) {
                // Try with audio/ prefix explicitly
                s3FilePath = "audio/" + filename;
                if (!s3Service.objectExists(videoId, s3FilePath)) {
                    logger.warn("Audio segment not found in S3: {} or audio/{} for video: {}", filename, filename, videoId);
                    return ResponseEntity.notFound().build();
                }
            }
            
            logger.debug("Using S3 path for audio segment: {}", s3FilePath);

            // Get metadata
            HeadObjectResponse metadata = s3Service.getObjectMetadata(videoId, s3FilePath);
            long fileSize = metadata.contentLength();
            
            // Parse range header if present
            Long rangeStart = null;
            Long rangeEnd = null;
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String range = rangeHeader.substring(6);
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    rangeStart = parts[0].isEmpty() ? null : Long.parseLong(parts[0]);
                    rangeEnd = parts[1].isEmpty() ? null : Long.parseLong(parts[1]);
                }
            }

            // Get object from S3 with range if specified
            ResponseInputStream<GetObjectResponse> s3Object = s3Service.getObject(
                videoId, s3FilePath, rangeStart, rangeEnd
            );

            Resource resource = new InputStreamResource(s3Object);
            String contentType = metadata.contentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "audio/mp4";
            }

            // Get or generate session ID
            String session = (sessionId != null && !sessionId.isEmpty()) 
                ? sessionId 
                : generateSessionId(request);

            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session);

            // Handle partial content (206) for range requests
            if (rangeHeader != null && rangeStart != null) {
                long contentLength = (rangeEnd != null ? rangeEnd : fileSize - 1) - rangeStart + 1;
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session)
                    .header(HttpHeaders.CONTENT_RANGE, 
                        String.format("bytes %d-%d/%d", rangeStart, 
                            rangeEnd != null ? rangeEnd : fileSize - 1, fileSize))
                    .contentLength(contentLength)
                    .body(resource);
            } else {
                return responseBuilder.contentLength(fileSize).body(resource);
            }
        } catch (Exception e) {
            logger.error("Error serving audio segment from S3", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get file (catch-all)", description = "Serves any file under the video (e.g. segments referenced in manifest). Path traversal (..) is rejected with 400.")
    @GetMapping("/{videoId}/**")
    public ResponseEntity<Resource> getFile(
            @PathVariable String videoId,
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request) {
        try {
            // Get the full path after /stream/{videoId}/
            String requestPath = request.getRequestURI();
            String basePath = "/stream/" + videoId + "/";
            String relativePath = requestPath.substring(requestPath.indexOf(basePath) + basePath.length());
            
            // Remove query parameters if any
            if (relativePath.contains("?")) {
                relativePath = relativePath.substring(0, relativePath.indexOf("?"));
            }
            
            // Security: prevent path traversal
            if (relativePath.contains("..") || relativePath.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Normalize path (remove leading slash)
            if (relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
            
            // Try to find the file - check multiple possible locations
            String actualPath = findFileInS3(videoId, relativePath);
            if (actualPath == null) {
                logger.warn("File not found in S3: {} for video: {}", relativePath, videoId);
                return ResponseEntity.notFound().build();
            }
            
            // Use the actual path found in S3
            relativePath = actualPath;
            
            // Get metadata
            HeadObjectResponse metadata = s3Service.getObjectMetadata(videoId, relativePath);
            long fileSize = metadata.contentLength();
            
            // Parse range header if present
            Long rangeStart = null;
            Long rangeEnd = null;
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String range = rangeHeader.substring(6);
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    rangeStart = parts[0].isEmpty() ? null : Long.parseLong(parts[0]);
                    rangeEnd = parts[1].isEmpty() ? null : Long.parseLong(parts[1]);
                }
            }

            // Get object from S3 with range if specified
            ResponseInputStream<GetObjectResponse> s3Object = s3Service.getObject(
                videoId, relativePath, rangeStart, rangeEnd
            );
            
            // Determine content type
            String contentType = metadata.contentType();
            if (contentType == null || contentType.isEmpty()) {
                String filename = relativePath.toLowerCase();
                if (filename.endsWith(".mp4")) {
                    contentType = filename.contains("audio") ? "audio/mp4" : "video/mp4";
                } else if (filename.endsWith(".mpd")) {
                    contentType = "application/dash+xml";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            Resource resource = new InputStreamResource(s3Object);
            
            // Get or generate session ID
            String session = (sessionId != null && !sessionId.isEmpty()) 
                ? sessionId 
                : generateSessionId(request);
            
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session);

            // Handle partial content (206) for range requests
            if (rangeHeader != null && rangeStart != null) {
                long contentLength = (rangeEnd != null ? rangeEnd : fileSize - 1) - rangeStart + 1;
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session)
                    .header(HttpHeaders.CONTENT_RANGE, 
                        String.format("bytes %d-%d/%d", rangeStart, 
                            rangeEnd != null ? rangeEnd : fileSize - 1, fileSize))
                    .contentLength(contentLength)
                    .body(resource);
            } else {
                return responseBuilder.contentLength(fileSize).body(resource);
            }
        } catch (Exception e) {
            logger.error("Error serving file from S3", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate a session ID for the client
     */
    private String generateSessionId(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return java.util.UUID.nameUUIDFromBytes(
            (clientIp + userAgent + System.currentTimeMillis()).getBytes()
        ).toString();
    }


    /**
     * Find matching video ID with flexible matching (case-insensitive, ignore hyphens)
     */
    private String findMatchingVideoId(String requestedId, List<String> availableVideos) {
        // Exact match first
        if (availableVideos.contains(requestedId)) {
            return requestedId;
        }
        
        // Case-insensitive match
        for (String videoId : availableVideos) {
            if (videoId.equalsIgnoreCase(requestedId)) {
                return videoId;
            }
        }
        
        // Normalize both (remove hyphens, lowercase) and compare
        String normalizedRequested = requestedId.toLowerCase().replaceAll("-", "");
        for (String videoId : availableVideos) {
            String normalizedAvailable = videoId.toLowerCase().replaceAll("-", "");
            if (normalizedAvailable.equals(normalizedRequested)) {
                logger.info("🔍 Found video ID match: '{}' matches '{}' (normalized)", requestedId, videoId);
                return videoId;
            }
        }
        
        return null;
    }

    /**
     * Try to find manifest file with different possible names
     */
    private String findManifestFile(String videoId) {
        // Try common variations
        String[] possibleNames = {
            "manifest.mpd",
            "maifest.mpd",  // Common typo
            "manifest.xml",
            "manifest"
        };
        
        for (String name : possibleNames) {
            if (s3Service.objectExists(videoId, name)) {
                logger.info("Found manifest with name: {}", name);
                return name;
            }
        }
        
        return null;
    }

    /**
     * Find file in S3 with flexible path matching
     * Tries: direct path, video/ path, audio/ path
     */
    private String findFileInS3(String videoId, String requestedPath) {
        // Try 1: Direct path (as requested)
        if (s3Service.objectExists(videoId, requestedPath)) {
            logger.debug("Found file at direct path: {}", requestedPath);
            return requestedPath;
        }
        
        // Try 2: Check if it's a video file and try video/ folder
        String filename = requestedPath.substring(requestedPath.lastIndexOf('/') + 1);
        if (filename.contains("video") || filename.endsWith("_dash.mp4")) {
            String videoPath = "video/" + filename;
            if (s3Service.objectExists(videoId, videoPath)) {
                logger.info("Found video file at: {} (requested: {})", videoPath, requestedPath);
                return videoPath;
            }
        }
        
        // Try 3: Check if it's an audio file and try audio/ folder
        if (filename.contains("audio") || (!filename.contains("video") && filename.endsWith(".mp4"))) {
            String audioPath = "audio/" + filename;
            if (s3Service.objectExists(videoId, audioPath)) {
                logger.info("Found audio file at: {} (requested: {})", audioPath, requestedPath);
                return audioPath;
            }
        }
        
        // Try 4: Try both video/ and audio/ folders regardless
        String videoPath = "video/" + filename;
        if (s3Service.objectExists(videoId, videoPath)) {
            logger.info("Found file in video/ folder: {} (requested: {})", videoPath, requestedPath);
            return videoPath;
        }
        
        String audioPath = "audio/" + filename;
        if (s3Service.objectExists(videoId, audioPath)) {
            logger.info("Found file in audio/ folder: {} (requested: {})", audioPath, requestedPath);
            return audioPath;
        }
        
        return null;
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

