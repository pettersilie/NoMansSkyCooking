package de.nms.nmsrecipes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nms.nmsrecipes.model.NameNormalizer;
import de.nms.nmsrecipes.model.RefineryBook;
import de.nms.nmsrecipes.model.RefineryDefinition;
import de.nms.nmsrecipes.model.RefineryIngredient;
import de.nms.nmsrecipes.model.RefineryVariant;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JsonRefineryBookStore {

    private final ObjectMapper objectMapper;

    public JsonRefineryBookStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RefineryBook load(Path jsonFile) {
        if (!Files.exists(jsonFile)) {
            throw new IllegalStateException("Refinery file not found: " + jsonFile.toAbsolutePath());
        }

        try {
            JsonRefineryPayload payload = readPayload(jsonFile);
            Map<String, RefineryDefinition> definitions = new LinkedHashMap<>();

            for (JsonRefineryDefinition rawDefinition : payload.recipes()) {
                RefineryDefinition definition = toDefinition(rawDefinition);
                definitions.put(NameNormalizer.key(definition.name()), definition);
            }

            Map<String, String> englishCategoryNames = new LinkedHashMap<>();
            List<String> categoryNames = new ArrayList<>();
            for (JsonCategoryDefinition category : payload.categories()) {
                String categoryName = NameNormalizer.display(category.name());
                if (categoryName.isBlank()) {
                    continue;
                }

                categoryNames.add(categoryName);
                String englishName = NameNormalizer.display(category.englishName());
                if (!englishName.isBlank()) {
                    englishCategoryNames.put(categoryName, englishName);
                }
            }

            Map<String, String> englishTermNames = new LinkedHashMap<>();
            for (JsonTermDefinition term : payload.terms()) {
                String termName = NameNormalizer.display(term.name());
                String englishName = NameNormalizer.display(term.englishName());
                if (termName.isBlank() || englishName.isBlank()) {
                    continue;
                }

                englishTermNames.put(termName, englishName);
            }

            return new RefineryBook(definitions, categoryNames, englishCategoryNames, englishTermNames);
        } catch (IOException exception) {
            throw new IllegalStateException("Refinery file could not be read: " + jsonFile.toAbsolutePath(), exception);
        }
    }

    private JsonRefineryPayload readPayload(Path jsonFile) throws IOException {
        JsonNode root = objectMapper.readTree(jsonFile.toFile());
        if (root == null || !root.isObject()) {
            throw new IllegalStateException("Unsupported refinery JSON structure: " + jsonFile.toAbsolutePath());
        }

        List<JsonCategoryDefinition> categories = new ArrayList<>();
        JsonNode categoriesNode = root.get("categories");
        if (categoriesNode != null && categoriesNode.isArray()) {
            for (JsonNode categoryNode : categoriesNode) {
                if (categoryNode == null || categoryNode.isNull()) {
                    continue;
                }

                categories.add(new JsonCategoryDefinition(
                        categoryNode.path("name").asText(""),
                        categoryNode.path("englishName").asText("")));
            }
        }

        List<JsonTermDefinition> terms = new ArrayList<>();
        JsonNode termsNode = root.get("terms");
        if (termsNode != null && termsNode.isArray()) {
            for (JsonNode termNode : termsNode) {
                if (termNode == null || termNode.isNull()) {
                    continue;
                }

                terms.add(new JsonTermDefinition(
                        termNode.path("name").asText(""),
                        termNode.path("englishName").asText("")));
            }
        }

        List<JsonRefineryDefinition> recipes = new ArrayList<>();
        JsonNode recipesNode = root.get("recipes");
        if (recipesNode != null && recipesNode.isArray()) {
            for (JsonNode recipeNode : recipesNode) {
                if (recipeNode == null || recipeNode.isNull()) {
                    continue;
                }

                List<JsonRefineryVariant> variants = new ArrayList<>();
                JsonNode variantsNode = recipeNode.get("variants");
                if (variantsNode != null && variantsNode.isArray()) {
                    for (JsonNode variantNode : variantsNode) {
                        if (variantNode == null || variantNode.isNull()) {
                            continue;
                        }

                        List<JsonRefineryIngredient> ingredients = new ArrayList<>();
                        JsonNode ingredientsNode = variantNode.get("ingredients");
                        if (ingredientsNode != null && ingredientsNode.isArray()) {
                            for (JsonNode ingredientNode : ingredientsNode) {
                                if (ingredientNode == null || ingredientNode.isNull()) {
                                    continue;
                                }

                                ingredients.add(new JsonRefineryIngredient(
                                        ingredientNode.path("position").asInt(0),
                                        ingredientNode.path("name").asText(""),
                                        ingredientNode.path("quantity").asInt(0)));
                            }
                        }

                        variants.add(new JsonRefineryVariant(
                                variantNode.path("operation").asText(""),
                                variantNode.path("englishOperation").asText(""),
                                variantNode.path("time").asText(""),
                                variantNode.path("outputQuantity").asInt(0),
                                ingredients));
                    }
                }

                recipes.add(new JsonRefineryDefinition(
                        recipeNode.path("name").asText(""),
                        recipeNode.path("category").asText(""),
                        variants));
            }
        }

        return new JsonRefineryPayload(categories, terms, recipes);
    }

    private RefineryDefinition toDefinition(JsonRefineryDefinition rawDefinition) {
        String name = NameNormalizer.display(rawDefinition.name());
        if (name.isBlank()) {
            throw new IllegalStateException("Encountered refinery entry without a product name.");
        }

        List<RefineryVariant> variants = new ArrayList<>();
        int variantIndex = 1;
        List<JsonRefineryVariant> rawVariants = rawDefinition.variants() == null
                ? List.of()
                : rawDefinition.variants();
        for (JsonRefineryVariant rawVariant : rawVariants) {
            List<RefineryIngredient> ingredients = new ArrayList<>();
            List<JsonRefineryIngredient> rawIngredients = rawVariant.ingredients() == null
                    ? List.of()
                    : rawVariant.ingredients();
            for (JsonRefineryIngredient rawIngredient : rawIngredients) {
                String ingredientName = NameNormalizer.display(rawIngredient.name());
                if (ingredientName.isBlank()) {
                    continue;
                }

                ingredients.add(new RefineryIngredient(
                        rawIngredient.position(),
                        ingredientName,
                        rawIngredient.quantity()));
            }

            if (!ingredients.isEmpty()) {
                variants.add(new RefineryVariant(
                        variantIndex++,
                        NameNormalizer.display(rawVariant.operation()),
                        NameNormalizer.display(rawVariant.englishOperation()),
                        NameNormalizer.display(rawVariant.time()),
                        rawVariant.outputQuantity(),
                        ingredients));
            }
        }

        return new RefineryDefinition(name, NameNormalizer.display(rawDefinition.category()), variants);
    }

    private record JsonRefineryPayload(List<JsonCategoryDefinition> categories,
                                       List<JsonTermDefinition> terms,
                                       List<JsonRefineryDefinition> recipes) {
    }

    private record JsonCategoryDefinition(String name, String englishName) {
    }

    private record JsonTermDefinition(String name, String englishName) {
    }

    private record JsonRefineryDefinition(String name, String category, List<JsonRefineryVariant> variants) {
    }

    private record JsonRefineryVariant(String operation,
                                       String englishOperation,
                                       String time,
                                       int outputQuantity,
                                       List<JsonRefineryIngredient> ingredients) {
    }

    private record JsonRefineryIngredient(int position, String name, int quantity) {
    }
}
