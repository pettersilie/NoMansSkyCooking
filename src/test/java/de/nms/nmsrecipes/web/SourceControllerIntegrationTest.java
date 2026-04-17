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
class SourceControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesLocalizedSourceMaterialsAndDetails() {
        ResponseEntity<JsonNode> materialsResponse =
                restTemplate.getForEntity("/api/sources/materials?lang=en", JsonNode.class);

        assertThat(materialsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(materialsResponse.getBody()).isNotNull();
        assertThat(materialsResponse.getBody().isArray()).isTrue();
        assertThat(materialsResponse.getBody()).isNotEmpty();
        assertThat(materialsResponse.getBody().get(0).has("key")).isTrue();
        assertThat(materialsResponse.getBody().get(0).has("name")).isTrue();
        assertThat(materialsResponse.getBody().get(0).has("groupKey")).isTrue();
        assertThat(materialsResponse.getBody().get(0).has("groupName")).isTrue();

        JsonNode larvalCore = findByKey(materialsResponse.getBody(), "Larvenkern");
        assertThat(larvalCore).isNotNull();
        assertThat(larvalCore.path("name").asText()).isEqualTo("Larval Core");
        assertThat(larvalCore.path("groupName").asText()).isEqualTo("Infested and Cursed Sites");

        JsonNode placeholder = findByKey(materialsResponse.getBody(), "Beliebige andere gekochte Meeresfrucht");
        assertThat(placeholder).isNull();

        ResponseEntity<JsonNode> detailResponse =
                restTemplate.getForEntity("/api/sources/details?material={material}&lang=en", JsonNode.class, "Aktiviertes Indium");

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody()).isNotNull();
        assertThat(detailResponse.getBody().path("key").asText()).isEqualTo("Aktiviertes Indium");
        assertThat(detailResponse.getBody().path("groupName").asText()).isEqualTo("Activated Deposits");
        assertThat(detailResponse.getBody().path("summary").asText()).isNotBlank();
        assertThat(detailResponse.getBody().path("where").asText()).contains("extreme");
        assertThat(detailResponse.getBody().path("how").asText()).isNotBlank();
        assertThat(detailResponse.getBody().path("notes").asText()).contains("star colour");
        assertThat(detailResponse.getBody().path("itemNote").asText()).contains("blue star systems");
        assertThat(detailResponse.getBody().path("links").isArray()).isTrue();
        assertThat(detailResponse.getBody().path("links")).isNotEmpty();
        assertThat(detailResponse.getBody().path("links").get(0).path("url").asText()).startsWith("https://");

        ResponseEntity<JsonNode> missingResponse =
                restTemplate.getForEntity("/api/sources/details?material=DoesNotExist&lang=en", JsonNode.class);
        assertThat(missingResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
