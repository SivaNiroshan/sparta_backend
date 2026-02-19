package com.Sparta.UploadService.integration;

import com.Sparta.UploadService.MongoDB.MetaRepository;
import com.Sparta.UploadService.MongoDB.MetaService;
import com.Sparta.UploadService.TusServer.model.MetaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MetaService with real MongoDB using Testcontainers.
 * Tests actual database operations and persistence.
 * Requires Docker to be running - remove @Disabled to run when Docker is available.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Disabled("Requires Docker - run with Docker available to execute these tests")
class MetaServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "SpartaMongo_Test");
    }

    @Autowired
    private MetaService metaService;

    @Autowired
    private MetaRepository metaRepository;

    @BeforeEach
    void setUp() {
        // Clean up test data before each test
        metaRepository.deleteAll();
    }

    @Test
    void testSaveMeta_AndRetrieveFromDatabase() {
        // Arrange
        MetaRequest metaRequest = new MetaRequest();
        metaRequest.setName("integration-test-video.mp4");
        metaRequest.setDescription("Integration test video");
        metaRequest.setDistributor_id("user456");
        metaRequest.setTimeline("0-200");
        metaRequest.setQualities(Arrays.asList(1080, 720, 480));

        // Act
        metaService.saveMeta(metaRequest);

        // Assert - Verify it was saved to database
        Optional<MetaRequest> saved = metaRepository.findById(metaRequest.getId());
        assertTrue(saved.isPresent());
        assertEquals(metaRequest.getName(), saved.get().getName());
        assertEquals(metaRequest.getDescription(), saved.get().getDescription());
        assertEquals(metaRequest.getDistributor_id(), saved.get().getDistributor_id());
        assertEquals(metaRequest.getTimeline(), saved.get().getTimeline());
        assertNotNull(saved.get().getId());
    }

    @Test
    void testSaveMeta_MultipleRecords() {
        // Arrange
        MetaRequest meta1 = new MetaRequest();
        meta1.setName("video1.mp4");
        meta1.setDistributor_id("user1");

        MetaRequest meta2 = new MetaRequest();
        meta2.setName("video2.mp4");
        meta2.setDistributor_id("user2");

        // Act
        metaService.saveMeta(meta1);
        metaService.saveMeta(meta2);

        // Assert
        assertEquals(2, metaRepository.count());
    }

    @Test
    void testSaveMeta_UpdateExisting() {
        // Arrange
        MetaRequest metaRequest = new MetaRequest();
        metaRequest.setName("original-name.mp4");
        metaRequest.setDescription("Original description");
        metaService.saveMeta(metaRequest);
        String id = metaRequest.getId();

        // Act - Update
        metaRequest.setDescription("Updated description");
        metaService.saveMeta(metaRequest);

        // Assert
        Optional<MetaRequest> updated = metaRepository.findById(id);
        assertTrue(updated.isPresent());
        assertEquals("Updated description", updated.get().getDescription());
    }
}
