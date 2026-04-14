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
        return localizeTerm(germanTerm, language, null);
    }

    public String localizeTerm(String germanTerm, String language, String englishOverride) {
        String normalized = NameNormalizer.display(germanTerm);
        if (!isEnglish(language) || normalized.isBlank()) {
            return normalized;
        }

        String normalizedOverride = NameNormalizer.display(englishOverride);
        if (!normalizedOverride.isBlank()) {
            return normalizedOverride;
        }

        return englishTerms.getOrDefault(normalized, normalized);
    }

    public String localizeCategory(String germanCategory, String language) {
        return localizeCategory(germanCategory, language, null);
    }

    public String localizeCategory(String germanCategory, String language, String englishOverride) {
        String normalized = NameNormalizer.display(germanCategory);
        if (!isEnglish(language)) {
            return normalized;
        }

        if (normalized.isBlank()) {
            return uncategorizedLabel(language);
        }

        String normalizedOverride = NameNormalizer.display(englishOverride);
        if (!normalizedOverride.isBlank()) {
            return normalizedOverride;
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

        if ("Der deutsche Kategoriename darf nicht leer sein.".equals(message)) {
            return "German category name must not be empty.";
        }

        if ("Der englische Kategoriename darf nicht leer sein.".equals(message)) {
            return "English category name must not be empty.";
        }

        if ("Kategorie existiert bereits.".equals(message)) {
            return "Category already exists.";
        }

        if ("Der Rezeptname auf Deutsch darf nicht leer sein.".equals(message)) {
            return "German recipe name must not be empty.";
        }

        if ("Der Rezeptname auf Englisch darf nicht leer sein.".equals(message)) {
            return "English recipe name must not be empty.";
        }

        if ("Bitte waehle eine Kategorie aus.".equals(message)) {
            return "Please select a category.";
        }

        if (message.startsWith("Unbekannte Kategorie: ")) {
            return "Unknown category: " + message.substring("Unbekannte Kategorie: ".length());
        }

        if ("Mindestens eine Rezeptvariante ist erforderlich.".equals(message)) {
            return "At least one recipe variant is required.";
        }

        if ("Jede Rezeptvariante muss mindestens eine Zutat haben.".equals(message)) {
            return "Each recipe variant must contain at least one ingredient.";
        }

        if ("Jede Rezeptvariante darf hoechstens 3 Zutaten haben.".equals(message)) {
            return "Each recipe variant may contain at most 3 ingredients.";
        }

        if (message.startsWith("Ungueltige Zutatenposition: ")) {
            return "Invalid ingredient position: " + message.substring("Ungueltige Zutatenposition: ".length());
        }

        if (message.startsWith("Doppelte Zutatenposition: ")) {
            return "Duplicate ingredient position: " + message.substring("Doppelte Zutatenposition: ".length());
        }

        if ("Bestehende Zutat fehlt.".equals(message)) {
            return "Existing ingredient is missing.";
        }

        if (message.startsWith("Unbekannte bestehende Zutat: ")) {
            return "Unknown existing ingredient: " + message.substring("Unbekannte bestehende Zutat: ".length());
        }

        if ("Neue Zutat auf Deutsch fehlt.".equals(message)) {
            return "German name for the new ingredient is missing.";
        }

        if ("Neue Zutat auf Englisch fehlt.".equals(message)) {
            return "English name for the new ingredient is missing.";
        }

        if (message.startsWith("Zutat existiert bereits: ")) {
            return "Ingredient already exists: " + message.substring("Zutat existiert bereits: ".length());
        }

        if ("Teilrezept fehlt.".equals(message)) {
            return "Sub-recipe is missing.";
        }

        if (message.startsWith("Rezept existiert bereits: ")) {
            return "Recipe already exists: " + message.substring("Rezept existiert bereits: ".length());
        }

        return message;
    }
}
