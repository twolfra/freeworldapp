package de.freeworldapp.app.image;

/**
 * Shared naming rules for stored images and their thumbnail variants.
 * The thumbnail of {@code {name}.{ext}} is always {@code {name}_t.{ext}},
 * so a thumbnail URL/blob name can be derived from the main one without
 * callers having to track it separately.
 */
final class ImageNaming {

    private ImageNaming() {}

    /** Derive the thumbnail variant of a URL/filename: inserts "_t" before the extension. */
    static String thumbVariant(String urlOrName) {
        int dot = urlOrName.lastIndexOf('.');
        int slash = urlOrName.lastIndexOf('/');
        if (dot <= slash) return urlOrName + "_t"; // no extension
        return urlOrName.substring(0, dot) + "_t" + urlOrName.substring(dot);
    }

    /** Content-Type for a stored (re-encoded) image extension. */
    static String contentTypeFor(String extension) {
        return switch (extension.toLowerCase()) {
            case "png"         -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default            -> "application/octet-stream";
        };
    }
}
