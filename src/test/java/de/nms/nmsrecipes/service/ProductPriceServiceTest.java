package de.nms.nmsrecipes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nms.nmsrecipes.config.RecipeProperties;
import de.nms.nmsrecipes.model.IngredientSlot;
import de.nms.nmsrecipes.model.RecipeBook;
import de.nms.nmsrecipes.model.RecipeDefinition;
import de.nms.nmsrecipes.model.RecipeVariant;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductPriceServiceTest {

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loadsExistingPricesAndCanonicalizesKnownProductNames() throws Exception {
        Path tempPriceFile = Files.createTempFile("prices-", ".json");
        Files.writeString(tempPriceFile, """
                {
                  " testprodukt ": "12.5",
                  "Unbekannt": "7"
                }
                """);

        ProductPriceService service = new ProductPriceService(
                new RecipeProperties("./target/test-recipes.json", tempPriceFile.toString()),
                createCatalogService(),
                objectMapper);

        service.load();

        assertThat(service.findDisplayPrice("Testprodukt")).contains("12,50");
        assertThat(service.findDisplayPrice("Unbekannt")).contains("7,00");
    }

    @Test
    void savesAndRemovesPricesUsingSeparateStorageFile() throws Exception {
        Path tempPriceFile = Files.createTempFile("prices-save-", ".json");

        ProductPriceService service = new ProductPriceService(
                new RecipeProperties("./target/test-recipes.json", tempPriceFile.toString()),
                createCatalogService(),
                objectMapper);

        assertThat(service.savePrice("Testprodukt", "12,5")).contains("12,50");
        assertThat(service.findDisplayPrice("Testprodukt")).contains("12,50");

        Map<String, String> storedPrices = objectMapper.readValue(tempPriceFile.toFile(), STRING_MAP);
        assertThat(storedPrices).containsEntry("Testprodukt", "12.5");

        assertThat(service.savePrice("Testprodukt", "   ")).isEmpty();
        assertThat(service.findDisplayPrice("Testprodukt")).isEmpty();

        Map<String, String> storedPricesAfterRemoval = objectMapper.readValue(tempPriceFile.toFile(), STRING_MAP);
        assertThat(storedPricesAfterRemoval).isEmpty();
    }

    @Test
    void rejectsInvalidAndNegativePrices() throws Exception {
        Path tempPriceFile = Files.createTempFile("prices-invalid-", ".json");

        ProductPriceService service = new ProductPriceService(
                new RecipeProperties("./target/test-recipes.json", tempPriceFile.toString()),
                createCatalogService(),
                objectMapper);

        assertThatThrownBy(() -> service.savePrice("Testprodukt", "-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Der Preis darf nicht negativ sein.");

        assertThatThrownBy(() -> service.savePrice("Testprodukt", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ungueltiger Preis: abc");
    }

    private RecipeCatalogService createCatalogService() {
        RecipeDefinition definition = new RecipeDefinition(
                "Testprodukt",
                "Tests",
                List.of(new RecipeVariant(
                        1,
                        "sheet",
                        1,
                        List.of(new IngredientSlot(1, List.of("Basis A"))))));
        RecipeBook recipeBook = new RecipeBook(Map.of("testprodukt", definition));

        return new RecipeCatalogService(null, null) {
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
    }
}
