package de.freeworldapp.app.image;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@ConditionalOnExpression("'${GCS_BUCKET:}' != ''")
public class GcsStorageService implements StorageService {

    private final Storage storage;
    private final String bucket;

    public GcsStorageService(@Value("${GCS_BUCKET}") String bucket) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucket = bucket;
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
        String blobName = UUID.randomUUID() + ext;

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, blobName))
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());

        return String.format("https://storage.googleapis.com/%s/%s", bucket, blobName);
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("https://storage.googleapis.com/")) return;
        // URL: https://storage.googleapis.com/{bucket}/{blobName}
        String path = url.substring("https://storage.googleapis.com/".length());
        int slash = path.indexOf('/');
        if (slash < 0) return;
        String blobName = path.substring(slash + 1);
        storage.delete(BlobId.of(bucket, blobName));
    }
}
