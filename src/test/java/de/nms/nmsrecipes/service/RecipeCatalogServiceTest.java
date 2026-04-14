package de.nms.nmsrecipes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.nms.nmsrecipes.config.RecipeProperties;
import de.nms.nmsrecipes.model.RecipeBook;
import de.nms.nmsrecipes.model.RecipeDraft;
import de.nms.nmsrecipes.model.RecipeDraftIngredient;
import de.nms.nmsrecipes.model.RecipeDraftVariant;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipeCatalogServiceTest {

    @Test
    void resolvesConfiguredRecipePathWithoutProjectRootFallback() {
        RecipeCatalogService service = new RecipeCatalogService(
                null,
                new RecipeProperties("./target/missing-dir/recipes.json", "./data/product-prices.json"));

        assertThat(service.resolveSourcePath().toString()).endsWith(Path.of("target", "missing-dir", "recipes.json").toString());
        assertThat(service.resolveSourcePath().getFileName()).hasToString("recipes.json");
        assertThat(Files.exists(service.resolveSourcePath())).isFalse();
    }

    @Test
    void savesStandaloneCategoriesIntoRecipeFile() throws Exception {
        Path tempRecipeFile = Files.createTempFile("recipes-categories-", ".json");
        Files.writeString(tempRecipeFile, "[]");

        RecipeCatalogService service = new RecipeCatalogService(
                new JsonRecipeBookStore(new ObjectMapper()),
                new RecipeProperties(tempRecipeFile.toString(), "./target/test-product-prices.json"));
        service.load();

        String savedCategory = service.saveCategory("Neue Kategorie", "New Category");

        assertThat(savedCategory).isEqualTo("Neue Kategorie");
        assertThat(service.categories()).contains("Neue Kategorie");
        assertThat(service.findEnglishCategoryName("Neue Kategorie")).contains("New Category");
        RecipeBook loadedBook = new JsonRecipeBookStore(new ObjectMapper()).load(tempRecipeFile);
        assertThat(loadedBook.categories()).containsExactly("Neue Kategorie");
        assertThat(loadedBook.englishCategoryName("Neue Kategorie")).contains("New Category");
    }

    @Test
    void rejectsDuplicateCategoriesIgnoringCaseAndWhitespace() throws Exception {
        Path tempRecipeFile = Files.createTempFile("recipes-categories-", ".json");
        Files.writeString(tempRecipeFile, "[]");

        RecipeCatalogService service = new RecipeCatalogService(
                new JsonRecipeBookStore(new ObjectMapper()),
                new RecipeProperties(tempRecipeFile.toString(), "./target/test-product-prices.json"));
        service.load();
        service.saveCategory("Neue Kategorie", "New Category");

        assertThatThrownBy(() -> service.saveCategory("  neue   kategorie  ", "Another Name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Kategorie existiert bereits.");
    }

    @Test
    void savesRecipesWithNestedRecipesAndNewIngredients() throws Exception {
        Path tempRecipeFile = Files.createTempFile("recipes-custom-", ".json");
        Files.writeString(tempRecipeFile, """
                {
                  "categories": [
                    {
                      "name": "Tests",
                      "englishName": "Tests"
                    }
                  ],
                  "recipes": []
                }
                """);

        RecipeCatalogService service = new RecipeCatalogService(
                new JsonRecipeBookStore(new ObjectMapper()),
                new RecipeProperties(tempRecipeFile.toString(), "./target/test-product-prices.json"));
        service.load();

        RecipeDraft draft = new RecipeDraft(
                "Testrezept",
                "Test Recipe",
                "Tests",
                List.of(new RecipeDraftVariant(List.of(
                        new RecipeDraftIngredient(1, "new_raw", null, "Neue Zutat", "New Ingredient", null),
                        new RecipeDraftIngredient(2, "new_recipe", null, null, null, new RecipeDraft(
                                "Teilrezept",
                                "Sub Recipe",
                                "Tests",
                                List.of(new RecipeDraftVariant(List.of(
                                        new RecipeDraftIngredient(1, "new_raw", null, "Schichtzutat", "Layer Ingredient", null)
                                )))))
                ))));

        RecipeCatalogService.SavedRecipe savedRecipe = service.saveRecipe(draft);

        assertThat(savedRecipe.key()).isEqualTo("Testrezept");
        assertThat(savedRecipe.createdRecipeCount()).isEqualTo(2);
        assertThat(service.findDefinition("Testrezept")).isPresent();
        assertThat(service.findDefinition("Teilrezept")).isPresent();
        assertThat(service.findEnglishTermName("Testrezept")).contains("Test Recipe");
        assertThat(service.findEnglishTermName("Neue Zutat")).contains("New Ingredient");
        RecipeBook loadedBook = new JsonRecipeBookStore(new ObjectMapper()).load(tempRecipeFile);
        assertThat(loadedBook.findDefinition("Testrezept")).isPresent();
        assertThat(loadedBook.findDefinition("Teilrezept")).isPresent();
        assertThat(loadedBook.englishTermName("Teilrezept")).contains("Sub Recipe");
        assertThat(loadedBook.englishTermName("Schichtzutat")).contains("Layer Ingredient");
    }

    @Test
    void rejectsRecipeNamesThatCollideWithExistingIngredients() throws Exception {
        Path tempRecipeFile = Files.createTempFile("recipes-collision-", ".json");
        Files.writeString(tempRecipeFile, """
                [
                  {
                    "name": "Bestehendes Rezept",
                    "category": "Tests",
                    "variants": [
                      [
                        ["Rohstoff"]
                      ]
                    ]
                  }
                ]
                """);

        RecipeCatalogService service = new RecipeCatalogService(
                new JsonRecipeBookStore(new ObjectMapper()),
                new RecipeProperties(tempRecipeFile.toString(), "./target/test-product-prices.json"));
        service.load();

        RecipeDraft draft = new RecipeDraft(
                "Rohstoff",
                "Raw Material",
                "Tests",
                List.of(new RecipeDraftVariant(List.of(
                        new RecipeDraftIngredient(1, "new_raw", null, "Noch eine Zutat", "Another Ingredient", null)
                ))));

        assertThatThrownBy(() -> service.saveRecipe(draft))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rezept existiert bereits: Rohstoff");
    }
}
