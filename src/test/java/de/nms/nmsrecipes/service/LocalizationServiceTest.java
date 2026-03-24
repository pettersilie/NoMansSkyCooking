package de.nms.nmsrecipes.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizationServiceTest {

    private final LocalizationService localizationService = new LocalizationService();

    @Test
    void translatesKnownTermsAndCategoriesToEnglish() {
        assertThat(localizationService.localizeTerm("Pilzschimmel", "en")).isEqualTo("Fungal Mould");
        assertThat(localizationService.localizeTerm("Pilzschimmel", "de")).isEqualTo("Pilzschimmel");
        assertThat(localizationService.localizeCategory("Suppen", "en")).isEqualTo("Stews and Soups");
        assertThat(localizationService.uncategorizedLabel("en")).isEqualTo("Uncategorized");
    }

    @Test
    void translatesKnownValidationErrorsToEnglish() {
        assertThat(localizationService.localizeErrorMessage("Der Preis darf nicht negativ sein.", "en"))
                .isEqualTo("Price must not be negative.");
        assertThat(localizationService.localizeErrorMessage("Ungueltiger Preis: xx", "en"))
                .isEqualTo("Invalid price: xx");
    }
}
