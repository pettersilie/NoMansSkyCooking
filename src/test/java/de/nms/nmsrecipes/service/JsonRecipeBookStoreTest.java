package de.nms.nmsrecipes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class JsonRecipeBookStoreTest {

    private final JsonRecipeBookStore store = new JsonRecipeBookStore(new ObjectMapper());

    @Test
    void savesAndLoadsRecipeBooksUsingJson() throws Exception {
        RecipeDefinition definition = new RecipeDefinition(
                "Testprodukt",
                "Tests",
                List.of(new RecipeVariant(
                        1,
                        "sheet",
                        5,
                        List.of(
                                new IngredientSlot(1, List.of("Basis A")),
                                new IngredientSlot(3, List.of("Basis C1", "Basis C2"))
                        ))));

        RecipeBook sourceBook = new RecipeBook(Map.of("testprodukt", definition));
        Path tempFile = Files.createTempFile("recipes-", ".json");

        store.save(tempFile, sourceBook);
        RecipeBook loadedBook = store.load(tempFile);

        RecipeDefinition loadedDefinition = loadedBook.findDefinition("Testprodukt").orElseThrow();
        assertThat(loadedDefinition.category()).isEqualTo("Tests");
        assertThat(loadedDefinition.variants()).hasSize(1);
        assertThat(loadedDefinition.variants().getFirst().slots()).hasSize(2);
        assertThat(loadedDefinition.variants().getFirst().slots().get(0).position()).isEqualTo(1);
        assertThat(loadedDefinition.variants().getFirst().slots().get(0).options()).containsExactly("Basis A");
        assertThat(loadedDefinition.variants().getFirst().slots().get(1).position()).isEqualTo(3);
        assertThat(loadedDefinition.variants().getFirst().slots().get(1).options()).containsExactly("Basis C1", "Basis C2");
    }
}
