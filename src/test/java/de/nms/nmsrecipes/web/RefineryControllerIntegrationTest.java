package de.nms.nmsrecipes.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RefineryControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesLocalizedRefineryProductsGraphAndIngredientSearch() {
        ResponseEntity<JsonNode> productsResponse =
                restTemplate.getForEntity("/api/refinery/products?lang=en", JsonNode.class);

        assertThat(productsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(productsResponse.getBody()).isNotNull();
        assertThat(productsResponse.getBody().isArray()).isTrue();
        assertThat(productsResponse.getBody()).isNotEmpty();

        JsonNode residualGoop = findByKey(productsResponse.getBody(), "Restsubstanz");
        assertThat(residualGoop).isNotNull();
        assertThat(residualGoop.get("name").asText()).isEqualTo("Residual Goop");
        assertThat(residualGoop.get("category").asText()).isEqualTo("Junk");
        assertThat(residualGoop.get("variantCount").asInt()).isEqualTo(1);

        ResponseEntity<JsonNode> categoriesResponse =
                restTemplate.getForEntity("/api/refinery/categories?lang=en", JsonNode.class);

        assertThat(categoriesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(categoriesResponse.getBody()).isNotNull();
        assertThat(categoriesResponse.getBody().isArray()).isTrue();
        JsonNode alloyCategory = findByKey(categoriesResponse.getBody(), "Metalllegierung");
        assertThat(alloyCategory).isNotNull();
        assertThat(alloyCategory.path("name").asText()).isEqualTo("Alloy Metal");

        ResponseEntity<JsonNode> graphResponse =
                restTemplate.getForEntity("/api/refinery/graph?product=Restsubstanz&lang=en", JsonNode.class);

        assertThat(graphResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(graphResponse.getBody()).isNotNull();
        assertThat(graphResponse.getBody().get("key").asText()).isEqualTo("Restsubstanz");
        assertThat(graphResponse.getBody().get("label").asText()).isEqualTo("Residual Goop");
        assertThat(graphResponse.getBody().get("type").asText()).isEqualTo("product");
        assertThat(graphResponse.getBody().toString()).contains("\"type\":\"slot\"");
        assertThat(graphResponse.getBody().toString()).contains("Quantity 2");

        ResponseEntity<JsonNode> searchResponse =
                restTemplate.getForEntity("/api/refinery/ingredients/search?query=cursed&lang=en", JsonNode.class);

        assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(searchResponse.getBody()).isNotNull();
        assertThat(searchResponse.getBody().isArray()).isTrue();
        JsonNode cursedDustHit = findByKey(searchResponse.getBody(), "Restsubstanz");
        assertThat(cursedDustHit).isNotNull();
        assertThat(cursedDustHit.get("name").asText()).isEqualTo("Residual Goop");
        assertThat(cursedDustHit.get("matches").toString()).contains("Cursed Dust");

        ResponseEntity<JsonNode> ingredientCatalogResponse =
                restTemplate.getForEntity("/api/refinery/ingredients/catalog?lang=en", JsonNode.class);

        assertThat(ingredientCatalogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ingredientCatalogResponse.getBody()).isNotNull();
        JsonNode residualGoopCatalogEntry = findByKey(ingredientCatalogResponse.getBody(), "Restsubstanz");
        assertThat(residualGoopCatalogEntry).isNotNull();
        assertThat(residualGoopCatalogEntry.get("name").asText()).isEqualTo("Residual Goop");
        assertThat(residualGoopCatalogEntry.get("craftable").asBoolean()).isTrue();
    }

    @Test
    void exposesRefineryOverviewRows() {
        ResponseEntity<JsonNode> overviewResponse =
                restTemplate.getForEntity("/api/refinery/overview?lang=en", JsonNode.class);

        assertThat(overviewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(overviewResponse.getBody()).isNotNull();
        assertThat(overviewResponse.getBody().isArray()).isTrue();
        assertThat(overviewResponse.getBody()).isNotEmpty();

        JsonNode residualGoopRow = findByKey(overviewResponse.getBody(), "Restsubstanz");
        assertThat(residualGoopRow).isNotNull();
        assertThat(residualGoopRow.path("name").asText()).isEqualTo("Residual Goop");
        assertThat(residualGoopRow.path("categoryKey").asText()).isNotBlank();
        assertThat(residualGoopRow.path("category").asText()).isNotBlank();
        assertThat(residualGoopRow.has("variantIndex")).isTrue();
        assertThat(residualGoopRow.has("ingredient1")).isTrue();
        assertThat(residualGoopRow.has("ingredient2")).isTrue();
        assertThat(residualGoopRow.has("ingredient3")).isTrue();
        assertThat(residualGoopRow.has("target")).isTrue();
        assertThat(residualGoopRow.has("ingredient1Entries")).isTrue();
        assertThat(residualGoopRow.has("ingredient2Entries")).isTrue();
        assertThat(residualGoopRow.has("ingredient3Entries")).isTrue();
        assertThat(residualGoopRow.has("price")).isFalse();
        assertThat(residualGoopRow.path("ingredient1").asText()).isNotBlank();
        assertThat(residualGoopRow.path("target").path("destination").asText()).isEqualTo("refinery");

        JsonNode activatedQuartziteRow = findByKey(overviewResponse.getBody(), "Aktiviertes Quarzit");
        assertThat(activatedQuartziteRow).isNotNull();
        assertThat(activatedQuartziteRow.path("ingredient1Entries").get(0).path("destination").asText()).isEqualTo("refinery");
        assertThat(activatedQuartziteRow.path("ingredient1Entries").get(0).path("key").asText()).isEqualTo("Quarzit");
        assertThat(activatedQuartziteRow.path("ingredient1Entries").get(0).path("name").asText()).isEqualTo("Quartzite");
    }

    private JsonNode findByKey(JsonNode entries, String key) {
        for (JsonNode entry : entries) {
            if (key.equals(entry.path("key").asText())) {
                return entry;
            }
        }
        return null;
    }
}
