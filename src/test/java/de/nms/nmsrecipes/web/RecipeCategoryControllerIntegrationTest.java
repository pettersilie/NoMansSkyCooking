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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "recipes.source-path=./target/test-categories-recipes.json",
                "recipes.price-path=./target/test-categories-product-prices.json"
        })
class RecipeCategoryControllerIntegrationTest {

    private static final Path TEST_RECIPE_FILE = Path.of("target", "test-categories-recipes.json");

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

    @Test
    void savesStandaloneCategoriesIntoConfiguredRecipeFile() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> saveResponse = restTemplate.exchange(
                "/api/categories",
                HttpMethod.POST,
                new HttpEntity<>("{\"germanName\":\"Neue Testkategorie\",\"englishName\":\"New Test Category\"}", headers),
                JsonNode.class);

        assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(saveResponse.getBody()).isNotNull();
        assertThat(saveResponse.getBody().get("key").asText()).isEqualTo("Neue Testkategorie");
        assertThat(saveResponse.getBody().get("name").asText()).isEqualTo("Neue Testkategorie");

        JsonNode savedFile = new ObjectMapper().readTree(TEST_RECIPE_FILE.toFile());
        assertThat(savedFile.isObject()).isTrue();
        assertThat(savedFile.get("categories").isArray()).isTrue();
        assertThat(savedFile.get("categories").toString()).contains("Neue Testkategorie");
        assertThat(savedFile.get("categories").toString()).contains("New Test Category");

        ResponseEntity<JsonNode> categoriesResponse = restTemplate.getForEntity("/api/categories?lang=en", JsonNode.class);
        assertThat(categoriesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(categoriesResponse.getBody()).isNotNull();
        assertThat(categoriesResponse.getBody().toString()).contains("New Test Category");
    }
}
