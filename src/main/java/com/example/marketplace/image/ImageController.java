package com.example.marketplace.image;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Path UPLOAD_DIR = Paths.get("uploads");
    private static final long MAX_BYTES = 5 * 1024 * 1024; // 5 MB

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(UPLOAD_DIR);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided."));
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            return ResponseEntity.badRequest().body(Map.of("error", "File must be an image."));
        if (file.getSize() > MAX_BYTES)
            return ResponseEntity.badRequest().body(Map.of("error", "File must be under 5 MB."));

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
        String filename = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        return ResponseEntity.ok(Map.of("url", "/api/images/" + filename));
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> serve(@PathVariable String filename) throws IOException {
        Path target = UPLOAD_DIR.resolve(filename).normalize().toAbsolutePath();
        if (!target.startsWith(UPLOAD_DIR.toAbsolutePath()) || !Files.exists(target))
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType(filename))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(Files.readAllBytes(target));
    }

    private String contentType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"         -> "image/png";
            case "gif"         -> "image/gif";
            case "webp"        -> "image/webp";
            default            -> "application/octet-stream";
        };
    }
}
