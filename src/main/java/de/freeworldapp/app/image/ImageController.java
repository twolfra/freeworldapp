package de.freeworldapp.app.image;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final long MAX_BYTES = 5 * 1024 * 1024;
    /** Longest edge of the stored full-size image; larger uploads are downscaled. */
    static final int MAX_EDGE = 2560;
    /** Longest edge of the stored thumbnail variant. */
    static final int THUMB_EDGE = 480;

    private final StorageService storage;

    public ImageController(StorageService storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided."));
        if (file.getSize() > MAX_BYTES)
            return ResponseEntity.badRequest().body(Map.of("error", "File must be under 5 MB."));

        // Magic-byte validation: probe the actual bytes instead of trusting the
        // Content-Type header. Anything ImageIO can't decode is rejected.
        // Note: animated GIFs decode as their first frame and are stored static.
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        if (image == null)
            return ResponseEntity.badRequest().body(Map.of("error", "File must be an image."));

        // Re-encode server-side (strips ALL metadata, including EXIF/GPS).
        // JPEG for opaque images, PNG when the image has an alpha channel.
        boolean hasAlpha = image.getColorModel().hasAlpha();
        String extension = hasAlpha ? "png" : "jpg";
        byte[] full = reencode(image, MAX_EDGE, hasAlpha);
        byte[] thumb = reencode(image, THUMB_EDGE, hasAlpha);

        String url = storage.store(full, thumb, extension);
        Map<String, String> body = new LinkedHashMap<>();
        body.put("url", url);
        body.put("thumbUrl", ImageNaming.thumbVariant(url));
        return ResponseEntity.ok(body);
    }

    /** Re-encode, capping the longest edge at {@code maxEdge} (downscale only, never upscale). */
    private byte[] reencode(BufferedImage image, int maxEdge, boolean hasAlpha) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(image);
        int longestEdge = Math.max(image.getWidth(), image.getHeight());
        if (longestEdge > maxEdge) {
            builder.size(maxEdge, maxEdge); // keeps aspect ratio by default
        } else {
            builder.scale(1.0);
        }
        if (hasAlpha) {
            builder.outputFormat("png");
        } else {
            builder.imageType(BufferedImage.TYPE_INT_RGB).outputFormat("jpg").outputQuality(0.85);
        }
        builder.toOutputStream(out);
        return out.toByteArray();
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
