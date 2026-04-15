package de.nms.nmsrecipes.service;

import de.nms.nmsrecipes.model.NameNormalizer;
import de.nms.nmsrecipes.model.RefineryBook;
import de.nms.nmsrecipes.model.RefineryDefinition;
import de.nms.nmsrecipes.model.RefineryIngredient;
import de.nms.nmsrecipes.model.RefineryVariant;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Service
public class RefineryCatalogService {

    private static final Logger log = LoggerFactory.getLogger(RefineryCatalogService.class);

    private final JsonRefineryBookStore refineryStore;
    private final String refineryPath;
    private volatile RefineryBook refineryBook = new RefineryBook(Map.of(), List.of(), Map.of(), Map.of());

    public RefineryCatalogService(JsonRefineryBookStore refineryStore,
                                  @Value("${recipes.refinery-path:./data/refinery-recipes.json}") String refineryPath) {
        this.refineryStore = refineryStore;
        this.refineryPath = refineryPath;
    }

    @PostConstruct
    public void load() {
        Path sourcePath = resolveSourcePath();
        refineryBook = refineryStore.load(sourcePath);
        log.info("Loaded refinery data: {} outputs from {}", refineryBook.definitions().size(), sourcePath.toAbsolutePath());
    }

    public List<RefineryDefinition> definitions() {
        return refineryBook.definitions();
    }

    public List<String> categories() {
        return refineryBook.categories();
    }

    public Optional<String> findEnglishCategoryName(String category) {
        return refineryBook.englishCategoryName(category);
    }

    public Optional<String> findEnglishTermName(String term) {
        return refineryBook.englishTermName(term);
    }

    public Optional<RefineryDefinition> findDefinition(String productName) {
        return refineryBook.findDefinition(productName);
    }

    public String canonicalNameOrSelf(String productName) {
        return refineryBook.canonicalNameOrSelf(productName);
    }

    public List<IngredientCatalogEntry> ingredientCatalog() {
        return refineryBook.allKnownTerms().stream()
                .map(term -> new IngredientCatalogEntry(term, findDefinition(term).isPresent()))
                .toList();
    }

    public Path resolveSourcePath() {
        Path configuredPath = Paths.get(refineryPath);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        return configuredPath.toAbsolutePath().normalize();
    }

    public RefineryDefinition requireDefinition(String productName) {
        return findDefinition(productName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + NameNormalizer.display(productName)));
    }

    public List<IngredientSearchHit> searchByIngredient(String query, UnaryOperator<String> ingredientLocalizer) {
        String normalizedQuery = NameNormalizer.display(query);
        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        String needle = normalizedQuery.toLowerCase(Locale.ROOT);
        Map<String, LinkedHashSet<String>> ingredientCache = new java.util.HashMap<>();

        return definitions().stream()
                .map(definition -> toIngredientSearchHit(definition, needle, ingredientCache, ingredientLocalizer))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(IngredientSearchHit::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Optional<IngredientSearchHit> toIngredientSearchHit(RefineryDefinition definition,
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

        RefineryDefinition definition = findDefinition(canonicalName).orElse(null);
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
            for (RefineryVariant variant : definition.variants()) {
                for (RefineryIngredient ingredient : variant.ingredients()) {
                    String canonicalIngredient = canonicalNameOrSelf(ingredient.name());
                    String ingredientKey = NameNormalizer.key(canonicalIngredient);
                    if (ingredientKey.equals(key)) {
                        continue;
                    }

                    ingredients.add(canonicalIngredient);
                    ingredients.addAll(collectIngredientNames(canonicalIngredient, stack, ingredientCache));
                }
            }

            ingredientCache.put(key, new LinkedHashSet<>(ingredients));
            return ingredients;
        } finally {
            stack.remove(key);
        }
    }

    public record IngredientCatalogEntry(String key, boolean craftable) {
    }

    public record IngredientSearchHit(String name,
                                      String category,
                                      int variantCount,
                                      List<String> matches) {
    }
}
