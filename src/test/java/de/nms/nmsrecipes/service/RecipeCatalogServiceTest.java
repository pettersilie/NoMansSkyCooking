package de.nms.nmsrecipes.service;

import de.nms.nmsrecipes.config.RecipeProperties;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeCatalogServiceTest {

    @Test
    void fallsBackToProjectRootRecipeFileWhenDataDirectoryIsMissing() {
        RecipeCatalogService service = new RecipeCatalogService(
                null,
                new RecipeProperties("./data/recipes.json", "./data/product-prices.json"));

        assertThat(service.resolveSourcePath().getFileName()).hasToString("recipes.json");
        assertThat(Files.exists(service.resolveSourcePath())).isTrue();
    }
}
