package com.Sparta.StreamingService.controller;

import com.Sparta.StreamingService.service.NetworkQualityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "Bandwidth", description = "Bandwidth reporting, quality selection, and statistics")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/bandwidth")
public class BandwidthController {

    private static final Logger logger = LoggerFactory.getLogger(BandwidthController.class);
    
    private final NetworkQualityService networkQualityService;

    public BandwidthController(NetworkQualityService networkQualityService) {
        this.networkQualityService = networkQualityService;
    }

    @Operation(summary = "Report bandwidth", description = "Record measured bandwidth for a session; returns recommended quality (1080/720/480)")
    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> reportBandwidth(
            @RequestParam String sessionId,
            @RequestParam double bandwidthMbps,
            @RequestParam(required = false) String clientIp,
            jakarta.servlet.http.HttpServletRequest request) {
        
        // Use provided IP or extract from request
        String ip = (clientIp != null && !clientIp.isEmpty()) 
            ? clientIp 
            : getClientIpAddress(request);
        
        networkQualityService.recordBandwidth(sessionId, ip, bandwidthMbps);
        
        logger.info("Bandwidth reported - Session: {}, IP: {}, Bandwidth: {} Mbps", 
            sessionId, ip, bandwidthMbps);
        
        Map<String, Object> response = Map.of(
            "status", "success",
            "sessionId", sessionId,
            "recordedBandwidth", bandwidthMbps,
            "recommendedQuality", networkQualityService.getOptimalQuality(bandwidthMbps)
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Select quality", description = "Set manual quality (1080, 720, 480) or auto for a session. Invalid quality returns 400.")
    @PostMapping("/quality/select")
    public ResponseEntity<Map<String, Object>> selectQuality(
            @RequestParam String sessionId,
            @RequestParam(required = false) Integer quality) {
        
        // Validate quality value
        if (quality != null && quality != 1080 && quality != 720 && quality != 480) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Invalid quality. Must be 1080, 720, 480, or null for auto"
            ));
        }
        
        networkQualityService.recordRequestedQuality(sessionId, quality);
        
        logger.info("Quality selected - Session: {}, Quality: {}", 
            sessionId, quality != null ? quality + "p" : "auto");
        
        Map<String, Object> response = Map.of(
            "status", "success",
            "sessionId", sessionId,
            "selectedQuality", quality != null ? quality : "auto",
            "message", quality != null 
                ? "Quality set to " + quality + "p" 
                : "Quality set to auto (network-based)"
        );
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get recommended quality", description = "Returns current/average bandwidth and recommended quality for a session, or no_data if unknown.")
    @GetMapping("/quality/{sessionId}")
    public ResponseEntity<Map<String, Object>> getRecommendedQuality(@PathVariable String sessionId) {
        var measurement = networkQualityService.getBandwidthMeasurement(sessionId);
        
        if (measurement == null) {
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "status", "no_data",
                "recommendedQuality", 720,
                "message", "No bandwidth data available for this session"
            ));
        }
        
        int recommendedQuality = measurement.getRecommendedQuality();
        
        Map<String, Object> response = Map.of(
            "sessionId", sessionId,
            "status", "success",
            "currentBandwidth", measurement.getCurrentBandwidth() != null 
                ? measurement.getCurrentBandwidth() : 0.0,
            "averageBandwidth", measurement.getAverageBandwidth() != null 
                ? measurement.getAverageBandwidth() : 0.0,
            "recommendedQuality", recommendedQuality,
            "measurementCount", measurement.getMeasurementCount()
        );
        logger.debug("response: {}", response);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get statistics", description = "Returns active session count and average bandwidth (Mbps).")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = networkQualityService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Bandwidth test", description = "Returns 1 MB test payload (application/octet-stream) for client-side bandwidth measurement.")
    @GetMapping("/test")
    public ResponseEntity<byte[]> bandwidthTest() {
        // Generate a 1MB test payload
        byte[] testData = new byte[1024 * 1024]; // 1 MB
        // Fill with test pattern
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        
        return ResponseEntity.ok()
            .header("Content-Type", "application/octet-stream")
            .header("Content-Length", String.valueOf(testData.length))
            .body(testData);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(jakarta.servlet.http.HttpServletRequest request) {
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

