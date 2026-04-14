package de.nms.nmsrecipes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nms.nmsrecipes.model.IngredientSlot;
import de.nms.nmsrecipes.model.NameNormalizer;
import de.nms.nmsrecipes.model.RecipeBook;
import de.nms.nmsrecipes.model.RecipeDefinition;
import de.nms.nmsrecipes.model.RecipeVariant;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class JsonRecipeBookStore {

    private static final TypeReference<List<JsonRecipeDefinition>> RECIPE_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public JsonRecipeBookStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RecipeBook load(Path jsonFile) {
        if (!Files.exists(jsonFile)) {
            throw new IllegalStateException("Recipe file not found: " + jsonFile.toAbsolutePath());
        }

        try {
            JsonRecipePayload payload = readPayload(jsonFile);
            Map<String, RecipeDefinition> definitions = new LinkedHashMap<>();

            for (JsonRecipeDefinition rawDefinition : payload.recipes()) {
                RecipeDefinition definition = toDefinition(rawDefinition);
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

            return new RecipeBook(definitions, categoryNames, englishCategoryNames, englishTermNames);
        } catch (IOException exception) {
            throw new IllegalStateException("Recipe file could not be read: " + jsonFile.toAbsolutePath(), exception);
        }
    }

    public void save(Path jsonFile, RecipeBook recipeBook) {
        try {
            Path parent = jsonFile.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            JsonRecipePayload payload = new JsonRecipePayload(
                    recipeBook.categories().stream()
                            .map(category -> new JsonCategoryDefinition(
                                    category,
                                    recipeBook.englishCategoryName(category).orElse(null)))
                            .toList(),
                    recipeBook.allKnownTerms().stream()
                            .filter(term -> recipeBook.englishTermName(term).isPresent())
                            .map(term -> new JsonTermDefinition(
                                    term,
                                    recipeBook.englishTermName(term).orElse(null)))
                            .toList(),
                    recipeBook.definitions().stream()
                            .map(this::toJsonDefinition)
                            .toList());

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Recipe file could not be written: " + jsonFile.toAbsolutePath(), exception);
        }
    }

    private JsonRecipePayload readPayload(Path jsonFile) throws IOException {
        JsonNode root = objectMapper.readTree(jsonFile.toFile());
        if (root == null || root.isNull()) {
            return new JsonRecipePayload(List.of(), List.of(), List.of());
        }

        if (root.isArray()) {
            return new JsonRecipePayload(
                    List.of(),
                    List.of(),
                    objectMapper.convertValue(root, RECIPE_LIST_TYPE));
        }

        if (root.isObject()) {
            JsonNode categoriesNode = root.get("categories");
            List<JsonCategoryDefinition> categories = new ArrayList<>();
            if (categoriesNode != null && categoriesNode.isArray()) {
                for (JsonNode categoryNode : categoriesNode) {
                    if (categoryNode == null || categoryNode.isNull()) {
                        continue;
                    }

                    if (categoryNode.isTextual()) {
                        categories.add(new JsonCategoryDefinition(categoryNode.asText(), null));
                        continue;
                    }

                    if (categoryNode.isObject()) {
                        categories.add(new JsonCategoryDefinition(
                                categoryNode.path("name").asText(""),
                                categoryNode.path("englishName").asText("")));
                    }
                }
            }

            JsonNode termsNode = root.get("terms");
            List<JsonTermDefinition> terms = new ArrayList<>();
            if (termsNode != null && termsNode.isArray()) {
                for (JsonNode termNode : termsNode) {
                    if (termNode == null || termNode.isNull()) {
                        continue;
                    }

                    if (termNode.isTextual()) {
                        terms.add(new JsonTermDefinition(termNode.asText(), null));
                        continue;
                    }

                    if (termNode.isObject()) {
                        terms.add(new JsonTermDefinition(
                                termNode.path("name").asText(""),
                                termNode.path("englishName").asText("")));
                    }
                }
            }

            List<JsonRecipeDefinition> recipes = objectMapper.convertValue(
                    root.path("recipes"),
                    RECIPE_LIST_TYPE);
            return new JsonRecipePayload(
                    categories,
                    terms,
                    recipes == null ? List.of() : recipes);
        }

        throw new IllegalStateException("Unsupported recipe JSON structure: " + jsonFile.toAbsolutePath());
    }

    private RecipeDefinition toDefinition(JsonRecipeDefinition rawDefinition) {
        String name = NameNormalizer.display(rawDefinition.name());
        if (name.isBlank()) {
            throw new IllegalStateException("Encountered recipe entry without a product name.");
        }

        List<RecipeVariant> variants = new ArrayList<>();
        List<List<List<String>>> rawVariants = rawDefinition.variants() == null ? List.of() : rawDefinition.variants();
        int variantIndex = 1;
        for (List<List<String>> rawVariant : rawVariants) {
            List<List<String>> variantSlots = rawVariant == null ? List.of() : rawVariant;
            List<IngredientSlot> slots = new ArrayList<>();

            for (int position = 0; position < variantSlots.size(); position++) {
                List<String> rawOptions = variantSlots.get(position);
                if (rawOptions == null) {
                    continue;
                }

                LinkedHashSet<String> options = new LinkedHashSet<>();
                rawOptions.stream()
                        .map(NameNormalizer::display)
                        .filter(option -> !option.isBlank())
                        .forEach(options::add);

                if (!options.isEmpty()) {
                    slots.add(new IngredientSlot(position + 1, List.copyOf(options)));
                }
            }

            if (!slots.isEmpty()) {
                variants.add(new RecipeVariant(variantIndex++, "json", 0, slots));
            }
        }

        return new RecipeDefinition(name, NameNormalizer.display(rawDefinition.category()), variants);
    }

    private JsonRecipeDefinition toJsonDefinition(RecipeDefinition definition) {
        List<List<List<String>>> variants = definition.variants().stream()
                .map(this::toJsonVariant)
                .toList();

        return new JsonRecipeDefinition(definition.name(), definition.category(), variants);
    }

    private List<List<String>> toJsonVariant(RecipeVariant variant) {
        int maxPosition = variant.slots().stream()
                .mapToInt(IngredientSlot::position)
                .max()
                .orElse(0);

        Map<Integer, IngredientSlot> slotsByPosition = new LinkedHashMap<>();
        for (IngredientSlot slot : variant.slots()) {
            slotsByPosition.put(slot.position(), slot);
        }

        List<List<String>> slots = new ArrayList<>();
        for (int position = 1; position <= maxPosition; position++) {
            IngredientSlot slot = slotsByPosition.get(position);
            slots.add(slot == null ? List.of() : slot.options());
        }

        return slots;
    }

    private record JsonRecipePayload(List<JsonCategoryDefinition> categories,
                                     List<JsonTermDefinition> terms,
                                     List<JsonRecipeDefinition> recipes) {
    }

    private record JsonCategoryDefinition(String name, String englishName) {
    }

    private record JsonTermDefinition(String name, String englishName) {
    }

    private record JsonRecipeDefinition(String name, String category, List<List<List<String>>> variants) {
    }
}
