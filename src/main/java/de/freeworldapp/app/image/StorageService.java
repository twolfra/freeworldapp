package de.freeworldapp.app.image;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    /** Persist an uploaded file and return its public URL (/api/images/{filename}). */
    String store(MultipartFile file) throws IOException;

    /** Delete the file at the given public URL. No-op if url is null or the file doesn't exist. */
    void delete(String url);
}
