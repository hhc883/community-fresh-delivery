package com.sky.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.controller.PublicImageController;
import com.sky.controller.admin.CommonController;
import com.sky.handler.GlobalExceptionHandler;
import com.sky.service.LocalImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ImageUploadContractTest {
    @TempDir Path directory;
    private MockMvc mvc;
    @BeforeEach void setup() {
        LocalImageStorage storage = new LocalImageStorage(directory.toString());
        mvc = MockMvcBuilders.standaloneSetup(new CommonController(storage), new PublicImageController(storage))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void uploadedImageIsReadableAndFilenameComesFromContent() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        String body = mvc.perform(multipart("/admin/common/upload")
                .file(new MockMultipartFile("file", "../../image.html", "image/png", bytes.toByteArray())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1))
                .andReturn().getResponse().getContentAsString();
        String url = new ObjectMapper().readTree(body).path("data").asText();
        assertTrue(url.matches("/uploads/[a-f0-9-]{36}\\.png"));
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(bytes.toByteArray()));
    }
    @Test void scriptDisguisedAsImageIsRejected() throws Exception {
        mvc.perform(multipart("/admin/common/upload").file(new MockMultipartFile("file", "image.png", "image/png", "<svg><script>bad</script></svg>".getBytes())))
                .andExpect(jsonPath("$.code").value(0));
    }
    @Test void emptyAndOversizedFilesAreRejected() throws Exception {
        for (byte[] bytes : new byte[][]{new byte[0], new byte[2 * 1024 * 1024 + 1]}) {
            mvc.perform(multipart("/admin/common/upload").file(new MockMultipartFile("file", "image.png", "image/png", bytes)))
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
    @Test void unknownFilesCannotBeRead() throws Exception {
        mvc.perform(get("/uploads/application.yml")).andExpect(status().isNotFound());
        mvc.perform(get("/uploads/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
    }
}
