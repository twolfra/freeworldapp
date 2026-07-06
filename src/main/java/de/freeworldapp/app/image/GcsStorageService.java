package de.freeworldapp.app.image;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

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
    public String store(byte[] bytes, byte[] thumbnail, String extension) {
        String blobName = UUID.randomUUID() + "." + extension;
        String contentType = ImageNaming.contentTypeFor(extension);
        createBlob(blobName, contentType, bytes);
        createBlob(ImageNaming.thumbVariant(blobName), contentType, thumbnail);
        return String.format("https://storage.googleapis.com/%s/%s", bucket, blobName);
    }

    private void createBlob(String blobName, String contentType, byte[] bytes) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, blobName))
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, bytes);
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
        storage.delete(BlobId.of(bucket, ImageNaming.thumbVariant(blobName)));
    }
}
