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
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageUploadService Unit Tests")
class ImageUploadServiceTest {

    @TempDir
    Path tempDir;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService();
        ReflectionTestUtils.setField(imageUploadService, "uploadDir", tempDir.toString());
    }

    // ─── null / empty ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("save — returns null for null file")
    void save_returnsNull_forNullFile() throws IOException {
        assertThat(imageUploadService.save(null)).isNull();
    }

    @Test
    @DisplayName("save — returns null for empty file")
    void save_returnsNull_forEmptyFile() throws IOException {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThat(imageUploadService.save(empty)).isNull();
    }

    // ─── allowed types ────────────────────────────────────────────────────────

    @Test
    @DisplayName("save — accepts JPEG and returns /uploads/products/ URL")
    void save_acceptsJpeg_returnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes());
        String url = imageUploadService.save(file);
        assertThat(url).startsWith("/uploads/products/").endsWith(".jpg");
    }

    @Test
    @DisplayName("save — accepts PNG and returns correct extension")
    void save_acceptsPng_returnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "fake-png-bytes".getBytes());
        String url = imageUploadService.save(file);
        assertThat(url).startsWith("/uploads/products/").endsWith(".png");
    }

    @Test
    @DisplayName("save — accepts WebP and returns correct extension")
    void save_acceptsWebp_returnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.webp", "image/webp", "fake-webp-bytes".getBytes());
        String url = imageUploadService.save(file);
        assertThat(url).startsWith("/uploads/products/").endsWith(".webp");
    }

    @Test
    @DisplayName("save — accepts GIF and returns correct extension")
    void save_acceptsGif_returnsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "anim.gif", "image/gif", "fake-gif-bytes".getBytes());
        String url = imageUploadService.save(file);
        assertThat(url).startsWith("/uploads/products/").endsWith(".gif");
    }

    // ─── rejected types ───────────────────────────────────────────────────────

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

    @Test
    @DisplayName("save — rejects application/octet-stream (binary disguise)")
    void save_rejects_octetStream() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream",
                "MZ-fake-exe".getBytes());
        assertThatThrownBy(() -> imageUploadService.save(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPG, PNG, WebP and GIF");
    }

    // ─── file written to disk ─────────────────────────────────────────────────

    @Test
    @DisplayName("save — file is physically written to the upload directory")
    void save_writesFileToDisk() throws IOException {
        byte[] content = "real-image-data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "product.jpg", "image/jpeg", content);
        String url = imageUploadService.save(file);
        String filename = url.replace("/uploads/products/", "");
        assertThat(tempDir.resolve(filename)).exists().isRegularFile();
        assertThat(Files.readAllBytes(tempDir.resolve(filename))).isEqualTo(content);
    }

    @Test
    @DisplayName("save — each call generates a unique filename")
    void save_generatesUniqueFilename_eachCall() throws IOException {
        byte[] content = "image-data".getBytes();
        String url1 = imageUploadService.save(new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", content));
        String url2 = imageUploadService.save(new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", content));
        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    @DisplayName("save — falls back to .jpg extension when filename has no extension")
    void save_fallsBackToJpg_whenNoExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noextension", "image/jpeg", "bytes".getBytes());
        assertThat(imageUploadService.save(file)).endsWith(".jpg");
    }

    @Test
    @DisplayName("save — creates upload directory automatically if it does not exist")
    void save_createsUploadDir_ifNotExists() throws IOException {
        Path newSubDir = tempDir.resolve("new/nested/dir");
        ReflectionTestUtils.setField(imageUploadService, "uploadDir", newSubDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "img.jpg", "image/jpeg", "bytes".getBytes());
        imageUploadService.save(file);
        assertThat(newSubDir).isDirectory();
    }

    @Test
    @DisplayName("save — saved file size on disk matches original byte content length")
    void save_fileSizeOnDisk_matchesOriginalContentLength() throws IOException {
        byte[] content = new byte[1024];
        Arrays.fill(content, (byte) 0xAB);
        MockMultipartFile file = new MockMultipartFile(
                "file", "sized.jpg", "image/jpeg", content);
        String url = imageUploadService.save(file);
        String filename = url.replace("/uploads/products/", "");
        assertThat(Files.size(tempDir.resolve(filename))).isEqualTo(content.length);
    }
}
