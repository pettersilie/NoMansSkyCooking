package de.nms.nmsrecipes.model;

import java.util.Locale;

public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String key(String value) {
        return display(value).toLowerCase(Locale.ROOT);
    }

    public static String display(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace('\u00A0', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }
}
