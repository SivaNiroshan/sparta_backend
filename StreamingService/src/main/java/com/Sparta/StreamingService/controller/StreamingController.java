package com.Sparta.StreamingService.controller;

import com.Sparta.StreamingService.service.NetworkQualityService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/stream")
public class StreamingController {

    private static final Logger logger = LoggerFactory.getLogger(StreamingController.class);
    
    private final String dashStoragePath;
    private final NetworkQualityService networkQualityService;

    public StreamingController(
            org.springframework.core.env.Environment env,
            NetworkQualityService networkQualityService) {
        this.dashStoragePath = env.getProperty("dash.storage.path", 
            "C:\\Users\\THABENDRA\\Desktop\\sparta_backend\\storage");
        this.networkQualityService = networkQualityService;
    }

    /**
     * Serve DASH manifest file (MPD)
     * Must be before the catch-all endpoint
     */
    @GetMapping(value = "/{videoId}/manifest.mpd", produces = "application/dash+xml")
    public ResponseEntity<Resource> getManifest(@PathVariable String videoId) {
        try {
            Path manifestPath = Paths.get(dashStoragePath, "dash_output" + videoId, "manifest.mpd");
            File file = manifestPath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/dash+xml"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"manifest.mpd\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Serve video segments based on quality
     * Supports automatic quality selection based on network bandwidth
     */
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
            
            // Try to find file with the determined quality
            Path videoPath = Paths.get(dashStoragePath, "dash_output" + videoId, filename);
            File file = videoPath.toFile();
            
            // If file doesn't exist, try with the actual quality in filename
            if (!file.exists() && !quality.equals(actualQuality)) {
                String adjustedFilename = filename.replace(quality + "p", actualQuality + "p");
                videoPath = Paths.get(dashStoragePath, "dash_output" + videoId, adjustedFilename);
                file = videoPath.toFile();
            }
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(videoPath);
            if (contentType == null) {
                contentType = "video/mp4";
            }

            // Measure bandwidth if Range header is present
            if (rangeHeader != null && sessionId != null) {
                measureBandwidthFromRange(rangeHeader, file.length(), startTime, session, request);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session)
                    .header("X-Quality-Served", actualQuality)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving video segment", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Serve audio segments
     */
    @GetMapping("/{videoId}/audio/{filename}")
    public ResponseEntity<Resource> getAudioSegment(
            @PathVariable String videoId,
            @PathVariable String filename,
            @RequestParam(required = false) String sessionId,
            HttpServletRequest request) {
        try {
            Path audioPath = Paths.get(dashStoragePath, "dash_output" + videoId, filename);
            File file = audioPath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(audioPath);
            if (contentType == null) {
                contentType = "audio/mp4";
            }

            // Get or generate session ID
            String session = (sessionId != null && !sessionId.isEmpty()) 
                ? sessionId 
                : generateSessionId(request);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving audio segment", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Serve any file from the DASH output folder (for direct file access from manifest)
     * This endpoint handles files that are referenced directly in the manifest.mpd
     */
    @GetMapping("/{videoId}/**")
    public ResponseEntity<Resource> getFile(
            @PathVariable String videoId,
            @RequestParam(required = false) String sessionId,
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
            
            Path filePath = Paths.get(dashStoragePath, "dash_output" + videoId, relativePath);
            File file = filePath.toFile();
            
            if (!file.exists() || !file.isFile()) {
                logger.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                String filename = file.getName().toLowerCase();
                if (filename.endsWith(".mp4")) {
                    contentType = filename.contains("audio") ? "audio/mp4" : "video/mp4";
                } else if (filename.endsWith(".mpd")) {
                    contentType = "application/dash+xml";
                } else {
                    contentType = "application/octet-stream";
                }
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Get or generate session ID
            String session = (sessionId != null && !sessionId.isEmpty()) 
                ? sessionId 
                : generateSessionId(request);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("X-Session-Id", session)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving file", e);
            return ResponseEntity.internalServerError().build();
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
     * Measure bandwidth from HTTP Range request
     */
    private void measureBandwidthFromRange(String rangeHeader, long fileSize, 
                                           long startTime, String sessionId, 
                                           HttpServletRequest request) {
        try {
            // Parse Range header: "bytes=start-end"
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String range = rangeHeader.substring(6);
                String[] parts = range.split("-");
                
                if (parts.length == 2) {
                    long rangeStart = Long.parseLong(parts[0]);
                    long rangeEnd = parts[1].isEmpty() 
                        ? fileSize - 1 
                        : Long.parseLong(parts[1]);
                    
                    long downloadTime = System.currentTimeMillis() - startTime;
                    double bandwidth = networkQualityService.estimateBandwidthFromRange(
                        rangeStart, rangeEnd, downloadTime
                    );
                    
                    String clientIp = getClientIpAddress(request);
                    networkQualityService.recordBandwidth(sessionId, clientIp, bandwidth);
                    
                    logger.debug("Measured bandwidth from Range request: {} Mbps for session {}", 
                        bandwidth, sessionId);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to measure bandwidth from Range header", e);
        }
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

