package com.example.marketplace.image;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    /**
     * Persist an uploaded file and return its public URL.
     * For local storage this is /api/images/{filename}.
     * Swap this bean for an S3 or GCS implementation in production.
     */
    String store(MultipartFile file) throws IOException;
}
