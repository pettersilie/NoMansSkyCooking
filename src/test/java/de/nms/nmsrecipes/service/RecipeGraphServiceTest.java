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

    @Test
    void createsVariantNodesForRecipesWithMultipleVariants() {
        RecipeDefinition recipe = new RecipeDefinition(
                "Testprodukt",
                "Tests",
                List.of(
                        new RecipeVariant(1, "Test", 1, List.of(new IngredientSlot(1, List.of("Basis A")))),
                        new RecipeVariant(2, "Test", 2, List.of(new IngredientSlot(2, List.of("Basis B"))))
                ));

        RecipeCatalogService catalogService = createCatalogService(new RecipeBook(Map.of("testprodukt", recipe)));
        ProductPriceService priceService = createPriceService(catalogService, Map.of());
        RecipeGraphService graphService = new RecipeGraphService(catalogService, priceService, new LocalizationService());

        TreeNode graph = graphService.buildGraph("Testprodukt", "en");

        assertThat(graph.children()).hasSize(2);
        assertThat(graph.children()).extracting(TreeNode::type).containsOnly("variant");
        assertThat(graph.children()).extracting(TreeNode::label).containsExactly("Variant 1", "Variant 2");
        assertThat(graph.children().get(0).children()).extracting(TreeNode::label)
                .containsExactly("Ingredient 1", "Ingredient 2", "Ingredient 3");
    }

    @Test
    void emitsCycleNodesForRecursiveRecipes() {
        RecipeDefinition root = new RecipeDefinition(
                "Testprodukt",
                "Tests",
                List.of(new RecipeVariant(
                        1,
                        "Test",
                        1,
                        List.of(new IngredientSlot(1, List.of("Teilprodukt"))))));
        RecipeDefinition child = new RecipeDefinition(
                "Teilprodukt",
                "Tests",
                List.of(new RecipeVariant(
                        1,
                        "Test",
                        2,
                        List.of(new IngredientSlot(1, List.of("Testprodukt"))))));

        RecipeBook recipeBook = new RecipeBook(Map.of(
                "testprodukt", root,
                "teilprodukt", child));
        RecipeCatalogService catalogService = createCatalogService(recipeBook);
        ProductPriceService priceService = createPriceService(catalogService, Map.of());
        RecipeGraphService graphService = new RecipeGraphService(catalogService, priceService, new LocalizationService());

        TreeNode graph = graphService.buildGraph("Testprodukt", "de");

        TreeNode childProduct = graph.children().get(0).children().get(0);
        TreeNode cycleNode = childProduct.children().get(0).children().get(0);

        assertThat(childProduct.type()).isEqualTo("product");
        assertThat(cycleNode.type()).isEqualTo("cycle");
        assertThat(cycleNode.label()).isEqualTo("Testprodukt");
    }

    @Test
    void attachesPriceDetailsToProductAndRawNodes() {
        RecipeDefinition recipe = new RecipeDefinition(
                "Testprodukt",
                "Tests",
                List.of(new RecipeVariant(
                        1,
                        "Test",
                        1,
                        List.of(new IngredientSlot(1, List.of("Basis A"))))));

        RecipeCatalogService catalogService = createCatalogService(new RecipeBook(Map.of("testprodukt", recipe)));
        ProductPriceService priceService = createPriceService(catalogService, Map.of(
                "Testprodukt", "10,00",
                "Basis A", "2,50"));
        RecipeGraphService graphService = new RecipeGraphService(catalogService, priceService, new LocalizationService());

        TreeNode graph = graphService.buildGraph("Testprodukt", "en");
        TreeNode rawNode = graph.children().get(0).children().get(0);

        assertThat(graph.detail()).isEqualTo("Price per 100 units: 10,00");
        assertThat(rawNode.type()).isEqualTo("raw");
        assertThat(rawNode.detail()).isEqualTo("Price per 100 units: 2,50");
    }

    private RecipeCatalogService createCatalogService(RecipeBook recipeBook) {
        return new RecipeCatalogService(null, null) {
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

            @Override
            public Optional<String> findEnglishTermName(String term) {
                return recipeBook.englishTermName(term);
            }
        };
    }

    private ProductPriceService createPriceService(RecipeCatalogService catalogService, Map<String, String> prices) {
        return new ProductPriceService(null, catalogService, new ObjectMapper()) {
            @Override
            public Optional<String> findDisplayPrice(String productName) {
                return Optional.ofNullable(prices.get(catalogService.canonicalNameOrSelf(productName)));
            }
        };
    }
}
