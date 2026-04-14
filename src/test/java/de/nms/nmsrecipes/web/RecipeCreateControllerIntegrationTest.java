package de.nms.nmsrecipes.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "recipes.source-path=./target/test-create-recipes.json",
                "recipes.price-path=./target/test-create-product-prices.json"
        })
class RecipeCreateControllerIntegrationTest {

    private static final Path TEST_RECIPE_FILE = Path.of("target", "test-create-recipes.json");

    static {
        try {
            Files.createDirectories(TEST_RECIPE_FILE.getParent());
            Files.copy(Path.of("data", "recipes.json"), TEST_RECIPE_FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void savesRecipesWithVariantsNestedRecipesAndCustomIngredients() throws Exception {
        ResponseEntity<JsonNode> categoriesResponse = restTemplate.getForEntity("/api/categories", JsonNode.class);
        assertThat(categoriesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(categoriesResponse.getBody()).isNotNull();
        String categoryKey = categoriesResponse.getBody().get(0).get("key").asText();

        ResponseEntity<JsonNode> ingredientCatalogResponse = restTemplate.getForEntity("/api/ingredients/catalog", JsonNode.class);
        assertThat(ingredientCatalogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ingredientCatalogResponse.getBody()).isNotNull();
        JsonNode existingIngredient = findByKey(ingredientCatalogResponse.getBody(), "Sahne");
        String existingIngredientKey = existingIngredient != null
                ? existingIngredient.get("key").asText()
                : ingredientCatalogResponse.getBody().get(0).get("key").asText();

        Map<String, Object> payload = recipe(
                "Integrationstest Rezept",
                "Integration Test Recipe",
                categoryKey,
                List.of(
                        variant(List.of(
                                existingIngredient(1, existingIngredientKey),
                                newRawIngredient(2, "Integrationstest Zutat", "Integration Ingredient"),
                                newRecipeIngredient(3, recipe(
                                        "Integrationstest Teilrezept",
                                        "Integration Sub Recipe",
                                        categoryKey,
                                        List.of(variant(List.of(
                                                existingIngredient(1, existingIngredientKey),
                                                newRawIngredient(2, "Schichtzutat Integration", "Integration Layer Ingredient")
                                        )))
                                ))
                        )),
                        variant(List.of(
                                existingIngredient(1, existingIngredientKey)
                        ))
                ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> saveResponse = restTemplate.exchange(
                "/api/recipes?lang=en",
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(payload), headers),
                JsonNode.class);

        assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(saveResponse.getBody()).isNotNull();
        assertThat(saveResponse.getBody().get("key").asText()).isEqualTo("Integrationstest Rezept");
        assertThat(saveResponse.getBody().get("name").asText()).isEqualTo("Integration Test Recipe");
        assertThat(saveResponse.getBody().get("createdRecipeCount").asInt()).isEqualTo(2);

        JsonNode savedFile = objectMapper.readTree(TEST_RECIPE_FILE.toFile());
        assertThat(savedFile.isObject()).isTrue();
        assertThat(savedFile.get("recipes").toString()).contains("Integrationstest Rezept");
        assertThat(savedFile.get("recipes").toString()).contains("Integrationstest Teilrezept");
        assertThat(savedFile.get("terms").toString()).contains("Integration Test Recipe");
        assertThat(savedFile.get("terms").toString()).contains("Integration Ingredient");
        assertThat(savedFile.get("terms").toString()).contains("Integration Sub Recipe");
        assertThat(savedFile.get("terms").toString()).contains("Integration Layer Ingredient");

        ResponseEntity<JsonNode> englishProductsResponse = restTemplate.getForEntity("/api/products?lang=en", JsonNode.class);
        assertThat(englishProductsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(englishProductsResponse.getBody()).isNotNull();
        JsonNode savedProduct = findByKey(englishProductsResponse.getBody(), "Integrationstest Rezept");
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.get("name").asText()).isEqualTo("Integration Test Recipe");

        ResponseEntity<JsonNode> englishCatalogResponse =
                restTemplate.getForEntity("/api/ingredients/catalog?lang=en", JsonNode.class);
        assertThat(englishCatalogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(englishCatalogResponse.getBody()).isNotNull();
        JsonNode customIngredient = findByKey(englishCatalogResponse.getBody(), "Integrationstest Zutat");
        JsonNode subRecipe = findByKey(englishCatalogResponse.getBody(), "Integrationstest Teilrezept");
        assertThat(customIngredient).isNotNull();
        assertThat(customIngredient.get("name").asText()).isEqualTo("Integration Ingredient");
        assertThat(customIngredient.get("craftable").asBoolean()).isFalse();
        assertThat(subRecipe).isNotNull();
        assertThat(subRecipe.get("name").asText()).isEqualTo("Integration Sub Recipe");
        assertThat(subRecipe.get("craftable").asBoolean()).isTrue();
    }

    private JsonNode findByKey(JsonNode entries, String key) {
        for (JsonNode entry : entries) {
            if (key.equals(entry.path("key").asText())) {
                return entry;
            }
        }
        return null;
    }

    private Map<String, Object> recipe(String germanName,
                                       String englishName,
                                       String categoryKey,
                                       List<Object> variants) {
        return Map.of(
                "germanName", germanName,
                "englishName", englishName,
                "categoryKey", categoryKey,
                "variants", variants);
    }

    private Map<String, Object> variant(List<Object> ingredients) {
        return Map.of("ingredients", ingredients);
    }

    private Map<String, Object> existingIngredient(int position, String existingKey) {
        return Map.of(
                "position", position,
                "type", "existing",
                "existingKey", existingKey);
    }

    private Map<String, Object> newRawIngredient(int position, String germanName, String englishName) {
        return Map.of(
                "position", position,
                "type", "new_raw",
                "germanName", germanName,
                "englishName", englishName);
    }

    private Map<String, Object> newRecipeIngredient(int position, Map<String, Object> recipe) {
        return Map.of(
                "position", position,
                "type", "new_recipe",
                "recipe", recipe);
    }
}
