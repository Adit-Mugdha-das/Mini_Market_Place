package com.example.mini_marketplace.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

// Service for handling file uploads (e.g., product images) to the local filesystem.
@Slf4j
@Service
public class ImageUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    /**
     * Saves the uploaded file to disk and returns the public URL path.
     *
     * @param file the multipart image file from the form
     * @return relative URL like "/uploads/products/abc123.jpg", or null if empty
     * @throws IllegalArgumentException if file type is not allowed
     * @throws IOException              if saving fails
     */
    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid file type. Only JPG, PNG, WebP and GIF are allowed.");
        }

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // Generate unique filename preserving original extension
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : ".jpg";

        String filename = UUID.randomUUID().toString() + ext;
        Path destination = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        log.info("[IMAGE] Saved uploaded file: {}", destination.toAbsolutePath());

        // Return the URL path Spring will serve via the static resource handler
        return "/uploads/products/" + filename;
    }
}
