package com.example.demo.controller.Customer;

import com.trustify.trustify.service.ImageService;
import com.trustify.trustify.service.ImageService.ImageUploadException;
import com.trustify.trustify.service.ImageService.ImageNotFoundException;
import com.trustify.trustify.service.ImageService.ImageUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            ImageUploadResult result = imageService.uploadImage(file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload successful",
                    "data", result
            ));

        } catch (ImageUploadException e) {
            log.warn("Upload validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            Resource resource = imageService.loadImageAsResource(fileName);

            String contentType = determineContentType(resource);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (ImageNotFoundException e) {
            log.debug("Image not found: {}", fileName);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{fileName:.+}")
    public ResponseEntity<?> deleteImage(@PathVariable String fileName) {
        boolean deleted = imageService.deleteImage(fileName);

        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Image deleted successfully"
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String determineContentType(Resource resource) {
        try {
            String contentType = Files.probeContentType(
                    Paths.get(resource.getURI())
            );
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}