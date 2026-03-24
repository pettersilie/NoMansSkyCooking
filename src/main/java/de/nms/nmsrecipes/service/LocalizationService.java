package de.nms.nmsrecipes.service;

import de.nms.nmsrecipes.model.NameNormalizer;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LocalizationService {

    private static final String LANGUAGE_ENGLISH = "en";
    private static final String LANGUAGE_GERMAN = "de";

    private final Map<String, String> englishTerms = EnglishTerminology.terms();
    private final Map<String, String> englishCategories = EnglishTerminology.categories();

    public String normalizeLanguage(String language) {
        return LANGUAGE_ENGLISH.equalsIgnoreCase(language) ? LANGUAGE_ENGLISH : LANGUAGE_GERMAN;
    }

    public boolean isEnglish(String language) {
        return LANGUAGE_ENGLISH.equals(normalizeLanguage(language));
    }

    public String localizeTerm(String germanTerm, String language) {
        String normalized = NameNormalizer.display(germanTerm);
        if (!isEnglish(language) || normalized.isBlank()) {
            return normalized;
        }

        return englishTerms.getOrDefault(normalized, normalized);
    }

    public String localizeCategory(String germanCategory, String language) {
        String normalized = NameNormalizer.display(germanCategory);
        if (!isEnglish(language)) {
            return normalized;
        }

        if (normalized.isBlank()) {
            return uncategorizedLabel(language);
        }

        return englishCategories.getOrDefault(normalized, normalized);
    }

    public String slotLabel(int position, String language) {
        if (isEnglish(language)) {
            return "Ingredient " + position;
        }

        return "Zutat " + position;
    }

    public String variantLabel(int index, String language) {
        if (isEnglish(language)) {
            return "Variant " + index;
        }

        return "Variante " + index;
    }

    public String priceDetail(String displayPrice, String language) {
        if (displayPrice == null || displayPrice.isBlank()) {
            return null;
        }

        if (isEnglish(language)) {
            return "Price " + displayPrice;
        }

        return "Preis " + displayPrice;
    }

    public String uncategorizedLabel(String language) {
        if (isEnglish(language)) {
            return "Uncategorized";
        }

        return "Ohne Kategorie";
    }

    public String productNotFoundMessage(String language) {
        if (isEnglish(language)) {
            return "Product not found.";
        }

        return "Produkt nicht gefunden.";
    }

    public String localizeErrorMessage(String message, String language) {
        if (!isEnglish(language) || message == null || message.isBlank()) {
            return message;
        }

        if ("Der Preis darf nicht negativ sein.".equals(message)) {
            return "Price must not be negative.";
        }

        if (message.startsWith("Ungueltiger Preis: ")) {
            return "Invalid price: " + message.substring("Ungueltiger Preis: ".length());
        }

        if (message.startsWith("Ungültiger Preis: ")) {
            return "Invalid price: " + message.substring("Ungültiger Preis: ".length());
        }

        return message;
    }
}
