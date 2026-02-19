package com.Sparta.UploadService.unit;

import com.Sparta.UploadService.Rabbit.EncodingJobDTO;
import com.Sparta.UploadService.Rabbit.EncodingJobProducer;
import com.Sparta.UploadService.Rabbit.RabbitMQConfig;
import com.Sparta.UploadService.TusServer.model.MetaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EncodingJobProducer with mocked RabbitTemplate.
 * Tests message sending logic without actual RabbitMQ calls.
 */
@ExtendWith(MockitoExtension.class)
class EncodingJobProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EncodingJobProducer encodingJobProducer;

    @BeforeEach
    void setUp() {
        encodingJobProducer = new EncodingJobProducer(rabbitTemplate);
    }

    @Test
    void testSendJob_Success() {
        // Arrange
        MetaRequest metaRequest = new MetaRequest();
        metaRequest.setName("test-video.mp4");
        metaRequest.setDescription("Test video");
        
        EncodingJobDTO job = new EncodingJobDTO(
                "/path/to/input.mp4",
                "output_encoded.mp4",
                metaRequest
        );

        // Act
        encodingJobProducer.sendJob(job);

        // Assert
        ArgumentCaptor<EncodingJobDTO> captor = ArgumentCaptor.forClass(EncodingJobDTO.class);
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.ENCODING_QUEUE), captor.capture());
        
        EncodingJobDTO sentJob = captor.getValue();
        assertEquals(job.getInputPath(), sentJob.getInputPath());
        assertEquals(job.getOutputPath(), sentJob.getOutputPath());
        assertEquals(job.getFile(), sentJob.getFile());
    }

    @Test
    void testSendJob_WithNullJob() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            encodingJobProducer.sendJob(null);
        });
    }

    @Test
    void testSendJob_RabbitMQException() {
        // Arrange
        EncodingJobDTO job = new EncodingJobDTO(
                "/path/to/input.mp4",
                "output_encoded.mp4",
                new MetaRequest()
        );
        
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), any(EncodingJobDTO.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            encodingJobProducer.sendJob(job);
        });
        
        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(RabbitMQConfig.ENCODING_QUEUE), any(EncodingJobDTO.class));
    }
}
