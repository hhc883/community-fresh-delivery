package com.sky.controller;

import com.sky.service.LocalImageStorage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
public class PublicImageController {
    private final LocalImageStorage storage;
    public PublicImageController(LocalImageStorage storage) { this.storage = storage; }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        Path path = storage.find(filename);
        if (path == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .header("X-Content-Type-Options", "nosniff")
                .contentType(filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .body(new FileSystemResource(path.toFile()));
    }
}
