package com.Sparta.UploadService.TusServer;

import com.Sparta.UploadService.TusServer.model.MetaRequest;
import com.Sparta.UploadService.TusServer.model.TusFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
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
        // Simulate PATCH chunk upload
        ClassPathResource imageResource = new ClassPathResource("testimage.jpg");
        byte[] dummyData = StreamUtils.copyToByteArray(imageResource.getInputStream());

        mockMvc.perform(
                        patch("/upload/" + uuid)
                                .header("Upload-Offset", 0)
                                .header("Content-Length", dummyData.length)
                                .header("Content-Type", "application/offset+octet-stream") // ✅ CORRECT FOR PATCH
                                .content(dummyData)
                ).andExpect(status().isNoContent())
                .andExpect(header().string("Upload-Offset", String.valueOf(dummyData.length)))
                .andExpect(header().string("Tus-Resumable", "1.0.0"));
    }
}
