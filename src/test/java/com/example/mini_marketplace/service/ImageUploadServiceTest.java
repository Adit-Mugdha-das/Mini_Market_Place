package com.example.mini_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageUploadService Unit Tests")
class ImageUploadServiceTest {

    @TempDir
    Path tempDir;                       // JUnit 5 creates and cleans up a temp folder

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService();
        // Inject the temp directory as the upload dir via reflection
        ReflectionTestUtils.setField(imageUploadService, "uploadDir", tempDir.toString());
    }

    // ─── save: null / empty file ──────────────────────────────────────────────

    @Test
    @DisplayName("save — returns null for null file")
    void save_returnsNull_forNullFile() throws IOException {
        String result = imageUploadService.save(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("save — returns null for empty file")
    void save_returnsNull_forEmptyFile() throws IOException {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        String result = imageUploadService.save(empty);
        assertThat(result).isNull();
    }

    // ─── save: allowed types ──────────────────────────────────────────────────

    @Test
    @DisplayName("save — accepts JPEG and returns /uploads/products/ URL")
    void save_acceptsJpeg_returnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes());

        String url = imageUploadService.save(file);

        assertThat(url).startsWith("/uploads/products/");
        assertThat(url).endsWith(".jpg");
    }

    @Test
    @DisplayName("save — accepts PNG and returns correct extension")
    void save_acceptsPng_returnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "fake-png-bytes".getBytes());

        String url = imageUploadService.save(file);

        assertThat(url).startsWith("/uploads/products/");
        assertThat(url).endsWith(".png");
    }

    @Test
    @DisplayName("save — accepts WebP and returns correct extension")
    void save_acceptsWebp_returnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.webp", "image/webp", "fake-webp-bytes".getBytes());

        String url = imageUploadService.save(file);

        assertThat(url).startsWith("/uploads/products/");
        assertThat(url).endsWith(".webp");
    }

    @Test
    @DisplayName("save — accepts GIF and returns correct extension")
    void save_acceptsGif_returnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "anim.gif", "image/gif", "fake-gif-bytes".getBytes());

        String url = imageUploadService.save(file);

        assertThat(url).startsWith("/uploads/products/");
        assertThat(url).endsWith(".gif");
    }

    // ─── save: rejected types ─────────────────────────────────────────────────

    @Test
    @DisplayName("save — rejects PDF content type")
    void save_rejects_pdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "fake-pdf".getBytes());

        assertThatThrownBy(() -> imageUploadService.save(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("save — rejects text/plain content type")
    void save_rejects_textPlain() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> imageUploadService.save(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("save — rejects null content type")
    void save_rejects_nullContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "unknown", null, "bytes".getBytes());

        assertThatThrownBy(() -> imageUploadService.save(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file type");
    }

    // ─── save: file actually written to disk ─────────────────────────────────

    @Test
    @DisplayName("save — file is physically written to the upload directory")
    void save_writesFileToDisk() throws IOException {
        byte[] content = "real-image-data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "product.jpg", "image/jpeg", content);

        String url = imageUploadService.save(file);

        // Extract filename from URL and verify the file exists with correct content
        String filename = url.substring("/uploads/products/".length());
        Path saved = tempDir.resolve(filename);
        assertThat(saved).exists();
        assertThat(Files.readAllBytes(saved)).isEqualTo(content);
    }

    @Test
    @DisplayName("save — each call generates a unique filename (UUID-based)")
    void save_generatesUniqueFilename_eachCall() throws IOException {
        byte[] content = "image-data".getBytes();
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", content);
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", content);

        String url1 = imageUploadService.save(file1);
        String url2 = imageUploadService.save(file2);

        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    @DisplayName("save — falls back to .jpg extension when filename has no extension")
    void save_fallsBackToJpg_whenNoExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noextension", "image/jpeg", "bytes".getBytes());

        String url = imageUploadService.save(file);

        assertThat(url).endsWith(".jpg");
    }

    @Test
    @DisplayName("save — creates upload directory automatically if it does not exist")
    void save_createsUploadDir_ifNotExists() throws IOException {
        // Point to a non-existent sub-directory inside the temp folder
        Path newSubDir = tempDir.resolve("new/nested/dir");
        ReflectionTestUtils.setField(imageUploadService, "uploadDir", newSubDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", "bytes".getBytes());

        imageUploadService.save(file);

        assertThat(newSubDir).isDirectory();
    }
}
