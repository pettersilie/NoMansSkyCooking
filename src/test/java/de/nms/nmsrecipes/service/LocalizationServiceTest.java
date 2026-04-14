package de.nms.nmsrecipes.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizationServiceTest {

    private final LocalizationService localizationService = new LocalizationService();

    @Test
    void translatesKnownTermsAndCategoriesToEnglish() {
        assertThat(localizationService.localizeTerm("Pilzschimmel", "en")).isEqualTo("Fungal Mould");
        assertThat(localizationService.localizeTerm("Pilzschimmel", "de")).isEqualTo("Pilzschimmel");
        assertThat(localizationService.localizeTerm("Neue Zutat", "en", "New Ingredient")).isEqualTo("New Ingredient");
        assertThat(localizationService.localizeCategory("Suppen", "en")).isEqualTo("Stews and Soups");
        assertThat(localizationService.localizeCategory("Neue Kategorie", "en", "New Category")).isEqualTo("New Category");
        assertThat(localizationService.uncategorizedLabel("en")).isEqualTo("Uncategorized");
    }

    @Test
    void translatesKnownValidationErrorsToEnglish() {
        assertThat(localizationService.localizeErrorMessage("Der Preis darf nicht negativ sein.", "en"))
                .isEqualTo("Price must not be negative.");
        assertThat(localizationService.localizeErrorMessage("Ungueltiger Preis: xx", "en"))
                .isEqualTo("Invalid price: xx");
        assertThat(localizationService.localizeErrorMessage("Der deutsche Kategoriename darf nicht leer sein.", "en"))
                .isEqualTo("German category name must not be empty.");
        assertThat(localizationService.localizeErrorMessage("Der englische Kategoriename darf nicht leer sein.", "en"))
                .isEqualTo("English category name must not be empty.");
        assertThat(localizationService.localizeErrorMessage("Bitte waehle eine Kategorie aus.", "en"))
                .isEqualTo("Please select a category.");
        assertThat(localizationService.localizeErrorMessage("Rezept existiert bereits: Test", "en"))
                .isEqualTo("Recipe already exists: Test");
    }
}
