package de.freeworldapp.app.image;

import java.io.IOException;

public interface StorageService {
    /**
     * Persist a processed (re-encoded) image plus its thumbnail variant and return the
     * public URL of the main image. The thumbnail is stored alongside under the derived
     * name {@code {name}_t.{ext}} (see {@link ImageNaming#thumbVariant}).
     *
     * @param bytes     re-encoded full-size image bytes
     * @param thumbnail re-encoded thumbnail bytes
     * @param extension file extension without dot, e.g. "jpg" or "png"
     */
    String store(byte[] bytes, byte[] thumbnail, String extension) throws IOException;

    /**
     * Delete the file at the given public URL, plus its derived thumbnail variant if one
     * exists. No-op if url is null or the file doesn't exist.
     */
    void delete(String url);
}
