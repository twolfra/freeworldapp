package de.freeworldapp.app;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Upload hardening (AP 1.4): magic-byte validation, server-side re-encoding
 * (EXIF/GPS stripping), dimension capping, and thumbnail variants.
 */
class ImageUploadIntegrationTest extends IntegrationTestBase {

    private static final Path UPLOAD_DIR = Paths.get("uploads");
    private static final byte[] EXIF_MARKER = "Exif".getBytes(StandardCharsets.US_ASCII);

    private final List<Path> createdFiles = new ArrayList<>();

    @AfterEach
    void cleanUpFiles() throws Exception {
        for (Path p : createdFiles) Files.deleteIfExists(p);
        createdFiles.clear();
    }

    // ---------- tests ----------

    @Test
    void exifSegmentIsStrippedByReencoding() throws Exception {
        AuthedUser user = signUp(uniqueName());
        byte[] jpegWithExif = spliceExifSegment(toJpeg(opaqueImage(320, 240)));
        assertTrue(indexOf(jpegWithExif, EXIF_MARKER) >= 0, "test precondition: upload must contain an Exif segment");

        JsonNode body = uploadOk(user, new MockMultipartFile("file", "photo.jpg", "image/jpeg", jpegWithExif));

        byte[] stored = readStored(body.get("url").asText());
        assertEquals(-1, indexOf(stored, EXIF_MARKER), "stored file must not contain an Exif marker");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(stored));
        assertNotNull(decoded, "stored file must still be a decodable image");
        assertEquals(320, decoded.getWidth());
        assertEquals(240, decoded.getHeight());
    }

    @Test
    void disguisedNonImageIsRejected() throws Exception {
        AuthedUser user = signUp(uniqueName());
        byte[] notAnImage = "This is definitely not an image.".getBytes(StandardCharsets.UTF_8);

        mvc.perform(multipart("/api/images")
                        .file(new MockMultipartFile("file", "malware.jpg", "image/jpeg", notAnImage))
                        .header("X-Session-Token", user.token())
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File must be an image."));
    }

    @Test
    void oversizedImageIsDownscaledTo2560LongestEdge() throws Exception {
        AuthedUser user = signUp(uniqueName());
        byte[] wide = toJpeg(opaqueImage(4000, 1000));

        JsonNode body = uploadOk(user, new MockMultipartFile("file", "wide.jpg", "image/jpeg", wide));

        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(readStored(body.get("url").asText())));
        assertEquals(2560, Math.max(stored.getWidth(), stored.getHeight()));
        assertEquals(2560, stored.getWidth());
        assertEquals(640, stored.getHeight(), "aspect ratio must be preserved");
    }

    @Test
    void smallImageIsNeverUpscaled() throws Exception {
        AuthedUser user = signUp(uniqueName());
        JsonNode body = uploadOk(user, new MockMultipartFile("file", "small.jpg", "image/jpeg",
                toJpeg(opaqueImage(100, 80))));

        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(readStored(body.get("url").asText())));
        assertEquals(100, stored.getWidth());
        assertEquals(80, stored.getHeight());
    }

    @Test
    void responseContainsUrlAndThumbUrlAndThumbFileExists() throws Exception {
        AuthedUser user = signUp(uniqueName());
        JsonNode body = uploadOk(user, new MockMultipartFile("file", "photo.jpg", "image/jpeg",
                toJpeg(opaqueImage(1600, 1200))));

        String url = body.get("url").asText();
        String thumbUrl = body.get("thumbUrl").asText();
        assertTrue(url.startsWith("/api/images/"));
        assertEquals(insertThumbSuffix(url), thumbUrl, "thumbUrl must be the _t variant of url");

        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(readStored(thumbUrl)));
        assertNotNull(thumb, "thumbnail file must exist and decode");
        assertTrue(Math.max(thumb.getWidth(), thumb.getHeight()) <= 480,
                "thumbnail longest edge must be <= 480, was " + thumb.getWidth() + "x" + thumb.getHeight());
        // 1600x1200 downscales to exactly 480 on the longest edge
        assertEquals(480, Math.max(thumb.getWidth(), thumb.getHeight()));
    }

    @Test
    void transparentImageIsStoredAsPngWithAlpha() throws Exception {
        AuthedUser user = signUp(uniqueName());
        BufferedImage argb = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        argb.setRGB(10, 10, 0x40FF0000); // semi-transparent pixel
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(argb, "png", out);

        JsonNode body = uploadOk(user, new MockMultipartFile("file", "logo.png", "image/png", out.toByteArray()));

        String url = body.get("url").asText();
        assertTrue(url.endsWith(".png"), "image with alpha channel must be re-encoded as PNG: " + url);
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(readStored(url)));
        assertTrue(stored.getColorModel().hasAlpha());
    }

    @Test
    void uploadWithoutSessionTokenIsRejected() throws Exception {
        mvc.perform(multipart("/api/images")
                        .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg",
                                toJpeg(opaqueImage(50, 50))))
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private String uniqueName() {
        return "imguser" + ThreadLocalRandom.current().nextInt(1_000_000);
    }

    private JsonNode uploadOk(AuthedUser user, MockMultipartFile file) throws Exception {
        MvcResult result = mvc.perform(multipart("/api/images")
                        .file(file)
                        .header("X-Session-Token", user.token())
                        .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.thumbUrl").exists())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        trackForCleanup(body.get("url").asText());
        trackForCleanup(body.get("thumbUrl").asText());
        return body;
    }

    private void trackForCleanup(String url) {
        createdFiles.add(UPLOAD_DIR.resolve(url.substring("/api/images/".length())));
    }

    private byte[] readStored(String url) throws Exception {
        Path file = UPLOAD_DIR.resolve(url.substring("/api/images/".length()));
        assertTrue(Files.exists(file), "expected stored file at " + file);
        return Files.readAllBytes(file);
    }

    private static String insertThumbSuffix(String url) {
        int dot = url.lastIndexOf('.');
        return url.substring(0, dot) + "_t" + url.substring(dot);
    }

    private static BufferedImage opaqueImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x += 10)
            for (int y = 0; y < height; y += 10)
                img.setRGB(x, y, (x * 31 + y * 17) & 0xFFFFFF);
        return img;
    }

    private static byte[] toJpeg(BufferedImage img) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    /**
     * Splices a fake EXIF APP1 segment (FF E1 <len> "Exif\0\0" + payload) right after
     * the SOI marker (FF D8) of a JPEG, mimicking camera metadata such as GPS tags.
     */
    private static byte[] spliceExifSegment(byte[] jpeg) {
        assertEquals((byte) 0xFF, jpeg[0]);
        assertEquals((byte) 0xD8, jpeg[1]);
        byte[] payload = "Exif\0\0FAKE-GPS-METADATA-1234567890".getBytes(StandardCharsets.US_ASCII);
        int segLen = payload.length + 2; // length field includes itself
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF); out.write(0xD8);            // SOI
        out.write(0xFF); out.write(0xE1);            // APP1 marker
        out.write((segLen >> 8) & 0xFF); out.write(segLen & 0xFF);
        out.write(payload, 0, payload.length);
        out.write(jpeg, 2, jpeg.length - 2);         // rest of the original JPEG
        return out.toByteArray();
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++)
                if (haystack[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }
}
