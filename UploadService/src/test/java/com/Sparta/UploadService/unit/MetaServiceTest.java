package com.Sparta.UploadService.unit;

import com.Sparta.UploadService.MongoDB.MetaRepository;
import com.Sparta.UploadService.MongoDB.MetaService;
import com.Sparta.UploadService.TusServer.model.MetaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetaService with mocked MetaRepository.
 * Tests service logic without actual MongoDB calls.
 */
@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    @Mock
    private MetaRepository metaRepository;

    @InjectMocks
    private MetaService metaService;

    private MetaRequest metaRequest;

    @BeforeEach
    void setUp() {
        metaRequest = new MetaRequest();
        metaRequest.setName("test-video.mp4");
        metaRequest.setDescription("Test video description");
        metaRequest.setDistributor_id("user123");
        metaRequest.setTimeline("0-100");
    }

    @Test
    void testSaveMeta_Success() {
        // Arrange
        when(metaRepository.save(any(MetaRequest.class))).thenReturn(metaRequest);

        // Act
        assertDoesNotThrow(() -> metaService.saveMeta(metaRequest));

        // Assert
        verify(metaRepository, times(1)).save(metaRequest);
    }

    @Test
    void testSaveMeta_RepositoryException() {
        // Arrange
        when(metaRepository.save(any(MetaRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            metaService.saveMeta(metaRequest);
        });
        
        verify(metaRepository, times(1)).save(metaRequest);
    }
}
