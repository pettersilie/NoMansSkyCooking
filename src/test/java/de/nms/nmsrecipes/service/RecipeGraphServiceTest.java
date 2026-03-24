package de.nms.nmsrecipes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.nms.nmsrecipes.model.IngredientSlot;
import de.nms.nmsrecipes.model.RecipeBook;
import de.nms.nmsrecipes.model.RecipeDefinition;
import de.nms.nmsrecipes.model.RecipeVariant;
import de.nms.nmsrecipes.model.TreeNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeGraphServiceTest {

    @Test
    void alwaysExposesThreeIngredientColumns() {
        RecipeDefinition recipe = new RecipeDefinition(
                "Testprodukt",
                "Tests",
                List.of(new RecipeVariant(
                        1,
                        "Test",
                        2,
                        List.of(
                                new IngredientSlot(1, List.of("Basis A")),
                                new IngredientSlot(3, List.of("Basis C"))
                        ))));

        RecipeCatalogService catalogService = new RecipeCatalogService(null, null) {
            private final RecipeBook recipeBook = new RecipeBook(Map.of("testprodukt", recipe));

            @Override
            public java.util.Optional<RecipeDefinition> findDefinition(String productName) {
                return recipeBook.findDefinition(productName);
            }

            @Override
            public RecipeDefinition requireDefinition(String productName) {
                return recipeBook.findDefinition(productName).orElseThrow();
            }

            @Override
            public String canonicalNameOrSelf(String productName) {
                return recipeBook.canonicalNameOrSelf(productName);
            }
        };

        ProductPriceService priceService = new ProductPriceService(null, catalogService, new ObjectMapper()) {
            @Override
            public Optional<String> findDisplayPrice(String productName) {
                return Optional.empty();
            }
        };

        RecipeGraphService graphService = new RecipeGraphService(catalogService, priceService, new LocalizationService());

        TreeNode graph = graphService.buildGraph("Testprodukt", "de");

        assertThat(graph.children()).hasSize(3);
        assertThat(graph.children()).extracting(TreeNode::label)
                .containsExactly("Zutat 1", "Zutat 2", "Zutat 3");
        assertThat(graph.children().get(0).children()).extracting(TreeNode::label).containsExactly("Basis A");
        assertThat(graph.children().get(1).children()).isEmpty();
        assertThat(graph.children().get(2).children()).extracting(TreeNode::label).containsExactly("Basis C");
    }

    @Test
    void localizesSlotLabelsForEnglish() {
        RecipeDefinition recipe = new RecipeDefinition(
                "Testprodukt",
                "Tests",
                List.of(new RecipeVariant(
                        1,
                        "Test",
                        1,
                        List.of(new IngredientSlot(1, List.of("Basis A"))))));

        RecipeCatalogService catalogService = new RecipeCatalogService(null, null) {
            private final RecipeBook recipeBook = new RecipeBook(Map.of("testprodukt", recipe));

            @Override
            public Optional<RecipeDefinition> findDefinition(String productName) {
                return recipeBook.findDefinition(productName);
            }

            @Override
            public RecipeDefinition requireDefinition(String productName) {
                return recipeBook.findDefinition(productName).orElseThrow();
            }

            @Override
            public String canonicalNameOrSelf(String productName) {
                return recipeBook.canonicalNameOrSelf(productName);
            }
        };

        ProductPriceService priceService = new ProductPriceService(null, catalogService, new ObjectMapper()) {
            @Override
            public Optional<String> findDisplayPrice(String productName) {
                return Optional.empty();
            }
        };

        RecipeGraphService graphService = new RecipeGraphService(catalogService, priceService, new LocalizationService());

        TreeNode graph = graphService.buildGraph("Testprodukt", "en");

        assertThat(graph.children()).extracting(TreeNode::label)
                .containsExactly("Ingredient 1", "Ingredient 2", "Ingredient 3");
    }
}
