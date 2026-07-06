package de.freeworldapp.app.image;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@ConditionalOnExpression("'${GCS_BUCKET:}' == ''")
public class LocalStorageService implements StorageService {

    static final Path UPLOAD_DIR = Paths.get("uploads");

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(UPLOAD_DIR);
    }

    @Override
    public String store(byte[] bytes, byte[] thumbnail, String extension) throws IOException {
        String filename = UUID.randomUUID() + "." + extension;
        Files.write(UPLOAD_DIR.resolve(filename), bytes);
        Files.write(UPLOAD_DIR.resolve(ImageNaming.thumbVariant(filename)), thumbnail);
        return "/api/images/" + filename;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("/api/images/")) return;
        String filename = url.substring("/api/images/".length());
        deleteFile(filename);
        deleteFile(ImageNaming.thumbVariant(filename));
    }

    private void deleteFile(String filename) {
        try {
            Path target = resolve(filename);
            if (isSafe(target)) Files.deleteIfExists(target);
        } catch (IOException ignored) {}
    }

    public Path resolve(String filename) {
        return UPLOAD_DIR.resolve(filename).normalize().toAbsolutePath();
    }

    public boolean isSafe(Path resolved) {
        return resolved.startsWith(UPLOAD_DIR.toAbsolutePath());
    }
}
