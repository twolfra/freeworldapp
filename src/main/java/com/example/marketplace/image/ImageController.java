package com.example.marketplace.image;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final StorageService storage;

    public ImageController(StorageService storage) {
        this.storage = storage;
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

        String url = storage.store(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> serve(@PathVariable String filename) throws IOException {
        Path uploadDir = LocalStorageService.UPLOAD_DIR.toAbsolutePath();
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir) || !Files.exists(target))
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
