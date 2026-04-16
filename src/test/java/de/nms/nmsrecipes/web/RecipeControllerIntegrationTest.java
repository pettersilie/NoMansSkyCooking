package de.nms.nmsrecipes.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "recipes.price-path=./target/test-product-prices.json")
class RecipeControllerIntegrationTest {

    private static final Path TEST_PRICE_FILE = Path.of("target", "test-product-prices.json");

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void resetPriceFile() throws Exception {
        Files.deleteIfExists(TEST_PRICE_FILE);
    }

    @Test
    void exposesProductsAndGraph() {
        ResponseEntity<JsonNode> categoriesResponse = restTemplate.getForEntity("/api/categories", JsonNode.class);

        assertThat(categoriesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(categoriesResponse.getBody()).isNotNull();
        assertThat(categoriesResponse.getBody().isArray()).isTrue();
        assertThat(categoriesResponse.getBody()).isNotEmpty();
        assertThat(categoriesResponse.getBody().get(0).has("key")).isTrue();
        assertThat(categoriesResponse.getBody().get(0).has("name")).isTrue();

        ResponseEntity<JsonNode> productsResponse = restTemplate.getForEntity("/api/products", JsonNode.class);

        assertThat(productsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(productsResponse.getBody()).isNotNull();
        assertThat(productsResponse.getBody().isArray()).isTrue();
        assertThat(productsResponse.getBody()).isNotEmpty();
        assertThat(productsResponse.getBody().get(0).has("key")).isTrue();
        assertThat(productsResponse.getBody().get(0).has("categoryKey")).isTrue();
        assertThat(productsResponse.getBody().get(0).has("minIngredientCount")).isTrue();
        assertThat(productsResponse.getBody().get(0).has("maxIngredientCount")).isTrue();
        assertThat(productsResponse.getBody().get(0).has("price")).isTrue();

        String productKey = productsResponse.getBody().get(0).get("key").asText();
        String productName = productsResponse.getBody().get(0).get("name").asText();
        ResponseEntity<JsonNode> graphResponse = restTemplate.getForEntity("/api/graph?product={product}", JsonNode.class, productKey);

        assertThat(graphResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(graphResponse.getBody()).isNotNull();
        assertThat(graphResponse.getBody().get("key").asText()).isEqualTo(productKey);
        assertThat(graphResponse.getBody().get("label").asText()).isEqualTo(productName);
        assertThat(graphResponse.getBody().get("type").asText()).isEqualTo("product");
        assertThat(graphResponse.getBody().get("children").isArray()).isTrue();
        assertThat(graphResponse.getBody().toString()).contains("\"type\":\"slot\"");
        assertThat(graphResponse.getBody().toString()).doesNotContain("Pflichtbestandteil");

        String ingredientName = firstIngredientLabel(graphResponse.getBody());
        assertThat(ingredientName).isNotBlank();

        ResponseEntity<JsonNode> ingredientSearchResponse =
                restTemplate.getForEntity("/api/ingredients/search?query={query}", JsonNode.class, ingredientName);

        assertThat(ingredientSearchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ingredientSearchResponse.getBody()).isNotNull();
        assertThat(ingredientSearchResponse.getBody().isArray()).isTrue();
        assertThat(ingredientSearchResponse.getBody().toString()).contains(productName);
        assertThat(ingredientSearchResponse.getBody().toString()).contains(ingredientName);
        assertThat(ingredientSearchResponse.getBody().get(0).has("price")).isTrue();

        ResponseEntity<JsonNode> ingredientCatalogResponse =
                restTemplate.getForEntity("/api/ingredients/catalog?lang=en", JsonNode.class);

        assertThat(ingredientCatalogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ingredientCatalogResponse.getBody()).isNotNull();
        assertThat(ingredientCatalogResponse.getBody().isArray()).isTrue();
        assertThat(ingredientCatalogResponse.getBody()).isNotEmpty();
        assertThat(ingredientCatalogResponse.getBody().get(0).has("craftable")).isTrue();
        JsonNode creamIngredient = findProductByKey(ingredientCatalogResponse.getBody(), "Sahne");
        assertThat(creamIngredient).isNotNull();
        assertThat(creamIngredient.get("name").asText()).isEqualTo("Cream");

        ResponseEntity<JsonNode> englishProductsResponse = restTemplate.getForEntity("/api/products?lang=en", JsonNode.class);
        assertThat(englishProductsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(englishProductsResponse.getBody()).isNotNull();
        JsonNode creamProduct = findProductByKey(englishProductsResponse.getBody(), "Sahne");
        assertThat(creamProduct).isNotNull();
        assertThat(creamProduct.get("name").asText()).isEqualTo("Cream");
    }

    @Test
    void exposesRecipeOverviewRows() {
        ResponseEntity<JsonNode> overviewResponse = restTemplate.getForEntity("/api/recipes/overview", JsonNode.class);

        assertThat(overviewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(overviewResponse.getBody()).isNotNull();
        assertThat(overviewResponse.getBody().isArray()).isTrue();
        assertThat(overviewResponse.getBody()).isNotEmpty();
        assertThat(overviewResponse.getBody().get(0).has("key")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("name")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("categoryKey")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("category")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("variantIndex")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("ingredient1")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("ingredient2")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("ingredient3")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("price")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("target")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("ingredient1Entries")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("ingredient2Entries")).isTrue();
        assertThat(overviewResponse.getBody().get(0).has("ingredient3Entries")).isTrue();

        ResponseEntity<JsonNode> englishOverviewResponse =
                restTemplate.getForEntity("/api/recipes/overview?lang=en", JsonNode.class);

        assertThat(englishOverviewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(englishOverviewResponse.getBody()).isNotNull();
        JsonNode creamRow = findOverviewRowByKey(englishOverviewResponse.getBody(), "Sahne");
        assertThat(creamRow).isNotNull();
        assertThat(creamRow.get("name").asText()).isEqualTo("Cream");
        assertThat(creamRow.get("categoryKey").asText()).isNotBlank();
        assertThat(creamRow.get("category").asText()).isNotBlank();
        assertThat(creamRow.get("ingredient1").asText()).isNotBlank();
        assertThat(creamRow.path("target").path("destination").asText()).isEqualTo("cooking");

        JsonNode flowerWaffleRow = findOverviewRowByKey(englishOverviewResponse.getBody(), "Blütenwaffel");
        assertThat(flowerWaffleRow).isNotNull();
        assertThat(flowerWaffleRow.path("ingredient1Entries").get(0).path("destination").asText()).isEqualTo("cooking");
        assertThat(flowerWaffleRow.path("ingredient2Entries").get(0).path("destination").asText()).isEqualTo("refinery");
        assertThat(flowerWaffleRow.path("ingredient2Entries").get(0).path("key").asText()).isEqualTo("Sternenknolle");
        assertThat(flowerWaffleRow.path("ingredient2Entries").get(0).path("name").asText()).isEqualTo("Star Bulb");
    }

    @Test
    void savesAndReturnsProductPrices() {
        ResponseEntity<JsonNode> productsResponse = restTemplate.getForEntity("/api/products", JsonNode.class);
        String productKey = productsResponse.getBody().get(0).get("key").asText();
        String productName = productsResponse.getBody().get(0).get("name").asText();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> saveResponse = restTemplate.exchange(
                "/api/prices",
                HttpMethod.PUT,
                new HttpEntity<>("{\"key\":\"" + productKey + "\",\"price\":\"12,5\"}", headers),
                JsonNode.class);

        assertThat(saveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saveResponse.getBody()).isNotNull();
        assertThat(saveResponse.getBody().get("price").asText()).isEqualTo("12,50");
        assertThat(saveResponse.getBody().get("key").asText()).isEqualTo(productKey);

        ResponseEntity<JsonNode> refreshedProductsResponse = restTemplate.getForEntity("/api/products", JsonNode.class);
        JsonNode matchingProduct = findProductByKey(refreshedProductsResponse.getBody(), productKey);
        assertThat(matchingProduct).isNotNull();
        assertThat(matchingProduct.get("price").asText()).isEqualTo("12,50");
        assertThat(Files.exists(TEST_PRICE_FILE)).isTrue();

        ResponseEntity<JsonNode> graphResponse = restTemplate.getForEntity("/api/graph?product={product}", JsonNode.class, productKey);
        assertThat(graphResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(graphResponse.getBody()).isNotNull();
        assertThat(graphResponse.getBody().get("detail").asText()).isEqualTo("Preis pro 100 Stück: 12,50");
        assertThat(graphResponse.getBody().get("label").asText()).isEqualTo(productName);

        ResponseEntity<JsonNode> overviewResponse = restTemplate.getForEntity("/api/recipes/overview", JsonNode.class);
        JsonNode overviewRow = findOverviewRowByKey(overviewResponse.getBody(), productKey);
        assertThat(overviewRow).isNotNull();
        assertThat(overviewRow.get("price").asText()).isEqualTo("12,50");
    }

    private JsonNode findProductByKey(JsonNode products, String productKey) {
        for (JsonNode product : products) {
            if (productKey.equals(product.get("key").asText())) {
                return product;
            }
        }
        return null;
    }

    private String firstIngredientLabel(JsonNode rootNode) {
        JsonNode match = findIngredientNode(rootNode, true);
        return match == null ? "" : match.get("label").asText();
    }

    private JsonNode findOverviewRowByKey(JsonNode rows, String productKey) {
        for (JsonNode row : rows) {
            if (productKey.equals(row.get("key").asText())) {
                return row;
            }
        }
        return null;
    }

    private JsonNode findIngredientNode(JsonNode node, boolean skipCurrent) {
        if (!skipCurrent && node.has("type")) {
            String type = node.get("type").asText();
            if ("product".equals(type) || "raw".equals(type)) {
                return node;
            }
        }

        if (node.has("children") && node.get("children").isArray()) {
            for (JsonNode child : node.get("children")) {
                JsonNode match = findIngredientNode(child, false);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }
}
