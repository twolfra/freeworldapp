package de.freeworldapp.app.image;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public String store(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
        String filename = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/api/images/" + filename;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("/api/images/")) return;
        String filename = url.substring("/api/images/".length());
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
