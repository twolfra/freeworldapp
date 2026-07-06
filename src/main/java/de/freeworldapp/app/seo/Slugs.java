package de.freeworldapp.app.seo;

import java.text.Normalizer;
import java.util.Locale;

/** Cosmetic URL slugs — routing only ever uses the id. */
public final class Slugs {

    private Slugs() {}

    public static String of(String title) {
        if (title == null) return "";
        String s = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.GERMAN)
                .replace("ß", "ss")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return s.length() > 60 ? s.substring(0, 60).replaceAll("-$", "") : s;
    }
}
