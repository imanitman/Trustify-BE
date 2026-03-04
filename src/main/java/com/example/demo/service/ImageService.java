package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ImageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.max-size:5242880}")
    private long maxFileSize;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Map<String, byte[]> MAGIC_BYTES = new HashMap<>();
    static {
        MAGIC_BYTES.put("image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        MAGIC_BYTES.put("image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        MAGIC_BYTES.put("image/gif", new byte[]{0x47, 0x49, 0x46, 0x38});
        MAGIC_BYTES.put("image/webp", new byte[]{0x52, 0x49, 0x46, 0x46});
    }

    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Upload directory created: {}", path);
        }
    }

    public ImageUploadResult uploadImage(MultipartFile file) throws ImageUploadException {
        validateFile(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String secureFileName = generateSecureFileName(file, extension);

        Path targetPath = resolveAndValidatePath(secureFileName);

        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Image uploaded: {}", secureFileName);

            return ImageUploadResult.builder()
                    .fileName(secureFileName)
                    .url("/api/images/" + secureFileName)
                    .size(file.getSize())
                    .contentType(file.getContentType())
                    .build();

        } catch (IOException e) {
            log.error("Upload failed: {}", secureFileName, e);
            throw new ImageUploadException("Failed to store file", e);
        }
    }

    public Resource loadImageAsResource(String fileName) throws ImageNotFoundException {
        try {
            validateFileName(fileName);
            Path path = resolveAndValidatePath(fileName);

            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ImageNotFoundException("Image not found: " + fileName);

        } catch (IOException | ImageUploadException e) {
            throw new ImageNotFoundException("Image not found: " + fileName, e);
        }
    }

    public boolean deleteImage(String fileName) {
        try {
            validateFileName(fileName);
            Path path = resolveAndValidatePath(fileName);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("Image deleted: {}", fileName);
            }
            return deleted;
        } catch (Exception e) {
            log.error("Failed to delete image: {}", fileName, e);
            return false;
        }
    }

    private void validateFile(MultipartFile file) throws ImageUploadException {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("File is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new ImageUploadException("File too large. Max: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new ImageUploadException("Invalid file type: " + contentType);
        }

        if (!validateMagicBytes(file, contentType)) {
            throw new ImageUploadException("File content doesn't match type");
        }
    }

    private boolean validateMagicBytes(MultipartFile file, String contentType) {
        try {
            byte[] header = new byte[12];
            try (InputStream is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 3) return false;
            }

            byte[] magic = MAGIC_BYTES.get(contentType);
            if (magic == null) return true;

            for (int i = 0; i < magic.length; i++) {
                if (header[i] != magic[i]) return false;
            }
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    private void validateFileName(String fileName) throws ImageUploadException {
        if (fileName == null || fileName.contains("..") ||
                fileName.contains("/") || fileName.contains("\\")) {
            throw new ImageUploadException("Invalid filename");
        }
    }

    private Path resolveAndValidatePath(String fileName) throws ImageUploadException {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetPath = uploadPath.resolve(fileName).normalize();

            if (!targetPath.startsWith(uploadPath)) {
                throw new ImageUploadException("Path traversal detected");
            }

            return targetPath;
        } catch (Exception e) {
            throw new ImageUploadException("Invalid path", e);
        }
    }

    private String generateSecureFileName(MultipartFile file, String ext) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(file.getBytes());
            String hashStr = bytesToHex(hash).substring(0, 16);
            return System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, 8) + "_" +
                    hashStr + ext;
        } catch (Exception e) {
            return UUID.randomUUID().toString() + ext;
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."))
                .replaceAll("[^a-zA-Z0-9.]", "").toLowerCase();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // DTOs and Exceptions

    @lombok.Data
    @lombok.Builder
    public static class ImageUploadResult {
        private String fileName;
        private String url;
        private long size;
        private String contentType;
    }

    public static class ImageUploadException extends Exception {
        public ImageUploadException(String message) {
            super(message);
        }
        public ImageUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ImageNotFoundException extends Exception {
        public ImageNotFoundException(String message) {
            super(message);
        }
        public ImageNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}