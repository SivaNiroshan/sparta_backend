package com.Sparta.StreamingService.config;

import com.Sparta.StreamingService.service.NetworkQualityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for cleanup and maintenance
 */
@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    
    private final NetworkQualityService networkQualityService;

    public ScheduledTasks(NetworkQualityService networkQualityService) {
        this.networkQualityService = networkQualityService;
    }

    /**
     * Clean up expired sessions every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupExpiredSessions() {
        logger.debug("Running scheduled cleanup of expired sessions");
        networkQualityService.cleanupExpiredSessions();
    }
}

