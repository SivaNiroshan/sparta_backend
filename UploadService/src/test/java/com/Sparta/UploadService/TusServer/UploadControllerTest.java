package com.Sparta.UploadService.TusServer;

import com.Sparta.UploadService.TusServer.model.MetaRequest;
import com.Sparta.UploadService.TusServer.model.TusFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UploadController.
 * Tests TUS protocol endpoints with mocked external dependencies (S3, RabbitMQ).
 * Uses test profile for isolated test environment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String uuid;

    @BeforeEach
    void setUp() throws Exception {
        // Initiate upload: POST with proper JSON content type
        String json = """
            {
              "name": "test.mp4",
              "description": "desc",
              "distributor": "user",
              "timeline": "0-10"
            }
        """;

        var result = mockMvc.perform(
                        post("/upload")
                                .header("Upload-Length", 100000)
                                .contentType(MediaType.APPLICATION_JSON) // ✅ FIXED
                                .content(json)
                ).andExpect(status().isCreated())
                .andReturn();

        uuid = result.getResponse().getHeader("Location");
        assert uuid != null;
    }

    @Test
    void testProcessPatch() throws Exception {
        // Simulate PATCH chunk upload with inline bytes (no external test file needed)
        byte[] dummyData = new byte[1024]; // 1KB dummy payload
        java.util.Arrays.fill(dummyData, (byte) 0x00);

        mockMvc.perform(
                        patch("/upload/" + uuid)
                                .content(dummyData)
                                .header("Content-Type", "application/offset+octet-stream")
                                .header("Upload-Offset", "0")
                                .header("Content-Length", String.valueOf(dummyData.length))
                ).andExpect(status().isNoContent())
                .andExpect(header().string("Upload-Offset", String.valueOf(dummyData.length)))
                .andExpect(header().string("Tus-Resumable", "1.0.0"));
    }

    @Test
    void testProcessOptions() throws Exception {
        mockMvc.perform(options("/upload"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Tus-Resumable", "1.0.0"))
                .andExpect(header().string("Tus-Version", "1.0.0,0.2.2,0.2.1"))
                .andExpect(header().exists("Tus-Max-Size"));
    }

    @Test
    void testProcessHead() throws Exception {
        mockMvc.perform(head("/upload/" + uuid))
                .andExpect(status().isOk())
                .andExpect(header().string("Upload-Offset", "0"))
                .andExpect(header().string("Upload-Length", "100000"))
                .andExpect(header().string("Tus-Resumable", "1.0.0"));
    }

    @Test
    void testProcessHead_NotFound() throws Exception {
        mockMvc.perform(head("/upload/non-existent-uuid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testProcessPost_InvalidUploadLength() throws Exception {
        String json = """
            {
              "name": "test.mp4",
              "description": "desc",
              "distributor": "user",
              "timeline": "0-10"
            }
        """;

        mockMvc.perform(
                        post("/upload")
                                .header("Upload-Length", -1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                ).andExpect(status().isBadRequest());
    }

    @Test
    void testProcessPost_MissingUploadLength() throws Exception {
        String json = """
            {
              "name": "test.mp4"
            }
        """;

        mockMvc.perform(
                        post("/upload")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                ).andExpect(status().isBadRequest());
    }
}
