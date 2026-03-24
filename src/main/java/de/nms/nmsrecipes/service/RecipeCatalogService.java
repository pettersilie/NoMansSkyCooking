package de.nms.nmsrecipes.service;

import de.nms.nmsrecipes.config.RecipeProperties;
import de.nms.nmsrecipes.model.NameNormalizer;
import de.nms.nmsrecipes.model.RecipeBook;
import de.nms.nmsrecipes.model.RecipeDefinition;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Service
public class RecipeCatalogService {

    private static final Logger log = LoggerFactory.getLogger(RecipeCatalogService.class);

    private final JsonRecipeBookStore recipeStore;
    private final RecipeProperties properties;
    private volatile RecipeBook recipeBook = new RecipeBook(java.util.Map.of());

    public RecipeCatalogService(JsonRecipeBookStore recipeStore, RecipeProperties properties) {
        this.recipeStore = recipeStore;
        this.properties = properties;
    }

    @PostConstruct
    public void load() {
        Path sourcePath = resolveSourcePath();
        recipeBook = recipeStore.load(sourcePath);
        log.info("Loaded recipes: {} products from {}", recipeBook.allProductNames().size(), sourcePath.toAbsolutePath());
    }

    public List<RecipeDefinition> definitions() {
        return recipeBook.definitions();
    }

    public Optional<RecipeDefinition> findDefinition(String productName) {
        return recipeBook.findDefinition(productName);
    }

    public String canonicalNameOrSelf(String productName) {
        return recipeBook.canonicalNameOrSelf(productName);
    }

    public Path resolveSourcePath() {
        Path configuredPath = Paths.get(properties.sourcePath());
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        Path primaryPath = configuredPath.toAbsolutePath().normalize();
        if (Files.exists(primaryPath)) {
            return primaryPath;
        }

        Path fileName = configuredPath.getFileName();
        if (fileName != null) {
            Path fallbackPath = Paths.get(fileName.toString()).toAbsolutePath().normalize();
            if (Files.exists(fallbackPath)) {
                return fallbackPath;
            }
        }

        return primaryPath;
    }

    public RecipeDefinition requireDefinition(String productName) {
        return findDefinition(productName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + NameNormalizer.display(productName)));
    }

    public List<IngredientSearchHit> searchByIngredient(String query, UnaryOperator<String> ingredientLocalizer) {
        String normalizedQuery = NameNormalizer.display(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        String needle = normalizedQuery.toLowerCase(Locale.ROOT);
        Map<String, LinkedHashSet<String>> ingredientCache = new HashMap<>();

        return definitions().stream()
                .map(definition -> toIngredientSearchHit(definition, needle, ingredientCache, ingredientLocalizer))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(IngredientSearchHit::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Optional<IngredientSearchHit> toIngredientSearchHit(RecipeDefinition definition,
                                                                String needle,
                                                                Map<String, LinkedHashSet<String>> ingredientCache,
                                                                UnaryOperator<String> ingredientLocalizer) {
        List<String> matches = collectIngredientNames(definition.name(), new LinkedHashSet<>(), ingredientCache).stream()
                .map(ingredientLocalizer)
                .distinct()
                .filter(ingredient -> ingredient.toLowerCase(Locale.ROOT).contains(needle))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        if (matches.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new IngredientSearchHit(
                definition.name(),
                definition.category(),
                definition.variants().size(),
                matches));
    }

    private LinkedHashSet<String> collectIngredientNames(String productName,
                                                         LinkedHashSet<String> stack,
                                                         Map<String, LinkedHashSet<String>> ingredientCache) {
        String canonicalName = canonicalNameOrSelf(productName);
        String key = NameNormalizer.key(canonicalName);
        LinkedHashSet<String> cached = ingredientCache.get(key);
        if (cached != null) {
            return new LinkedHashSet<>(cached);
        }

        RecipeDefinition definition = findDefinition(canonicalName).orElse(null);
        if (definition == null) {
            LinkedHashSet<String> rawIngredient = new LinkedHashSet<>();
            rawIngredient.add(canonicalName);
            ingredientCache.put(key, rawIngredient);
            return new LinkedHashSet<>(rawIngredient);
        }

        if (!stack.add(key)) {
            return new LinkedHashSet<>();
        }

        try {
            LinkedHashSet<String> ingredients = new LinkedHashSet<>();
            for (var variant : definition.variants()) {
                for (var slot : variant.slots()) {
                    for (String option : slot.options()) {
                        String canonicalOption = canonicalNameOrSelf(option);
                        String optionKey = NameNormalizer.key(canonicalOption);
                        if (optionKey.equals(key)) {
                            continue;
                        }

                        ingredients.add(canonicalOption);
                        ingredients.addAll(collectIngredientNames(canonicalOption, stack, ingredientCache));
                    }
                }
            }

            ingredientCache.put(key, new LinkedHashSet<>(ingredients));
            return ingredients;
        } finally {
            stack.remove(key);
        }
    }

    public record IngredientSearchHit(String name, String category, int variantCount, List<String> matches) {
    }
}
