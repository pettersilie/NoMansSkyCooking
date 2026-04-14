package de.nms.nmsrecipes.service;

import de.nms.nmsrecipes.config.RecipeProperties;
import de.nms.nmsrecipes.model.IngredientSlot;
import de.nms.nmsrecipes.model.NameNormalizer;
import de.nms.nmsrecipes.model.RecipeBook;
import de.nms.nmsrecipes.model.RecipeDraft;
import de.nms.nmsrecipes.model.RecipeDraftIngredient;
import de.nms.nmsrecipes.model.RecipeDraftVariant;
import de.nms.nmsrecipes.model.RecipeDefinition;
import de.nms.nmsrecipes.model.RecipeVariant;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private static final String INGREDIENT_TYPE_EXISTING = "existing";
    private static final String INGREDIENT_TYPE_NEW_RAW = "new_raw";
    private static final String INGREDIENT_TYPE_NEW_RECIPE = "new_recipe";

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

    public List<String> categories() {
        return recipeBook.categories();
    }

    public Optional<String> findEnglishCategoryName(String category) {
        return recipeBook.englishCategoryName(category);
    }

    public Optional<String> findEnglishTermName(String term) {
        return recipeBook.englishTermName(term);
    }

    public Optional<RecipeDefinition> findDefinition(String productName) {
        return recipeBook.findDefinition(productName);
    }

    public String canonicalNameOrSelf(String productName) {
        return recipeBook.canonicalNameOrSelf(productName);
    }

    public List<IngredientCatalogEntry> ingredientCatalog() {
        return recipeBook.allKnownTerms().stream()
                .map(term -> new IngredientCatalogEntry(term, findDefinition(term).isPresent()))
                .toList();
    }

    public Path resolveSourcePath() {
        Path configuredPath = Paths.get(properties.sourcePath());
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        return configuredPath.toAbsolutePath().normalize();
    }

    public RecipeDefinition requireDefinition(String productName) {
        return findDefinition(productName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + NameNormalizer.display(productName)));
    }

    public synchronized String saveCategory(String germanCategoryName, String englishCategoryName) {
        String normalizedGermanCategory = NameNormalizer.display(germanCategoryName);
        if (normalizedGermanCategory.isBlank()) {
            throw new IllegalArgumentException("Der deutsche Kategoriename darf nicht leer sein.");
        }

        String normalizedEnglishCategory = NameNormalizer.display(englishCategoryName);
        if (normalizedEnglishCategory.isBlank()) {
            throw new IllegalArgumentException("Der englische Kategoriename darf nicht leer sein.");
        }

        if (recipeBook.hasCategory(normalizedGermanCategory)) {
            throw new IllegalArgumentException("Kategorie existiert bereits.");
        }

        RecipeBook updatedBook = recipeBook.withAddedCategory(normalizedGermanCategory, normalizedEnglishCategory);
        Path sourcePath = resolveSourcePath();
        recipeStore.save(sourcePath, updatedBook);
        recipeBook = updatedBook;
        log.info("Saved category '{}' ({}) to {}", normalizedGermanCategory, normalizedEnglishCategory, sourcePath.toAbsolutePath());
        return normalizedGermanCategory;
    }

    public synchronized SavedRecipe saveRecipe(RecipeDraft draft) {
        RecipeSaveContext context = new RecipeSaveContext(recipeBook);
        String rootRecipeName = collectRecipe(draft, context);

        RecipeBook updatedBook = recipeBook.withMergedRecipes(
                context.newDefinitions(),
                context.newEnglishTermsByName());
        Path sourcePath = resolveSourcePath();
        recipeStore.save(sourcePath, updatedBook);
        recipeBook = updatedBook;
        log.info("Saved recipe '{}' with {} new recipe definition(s) to {}",
                rootRecipeName,
                context.newDefinitions().size(),
                sourcePath.toAbsolutePath());
        return new SavedRecipe(rootRecipeName, context.newDefinitions().size());
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

    private String collectRecipe(RecipeDraft draft, RecipeSaveContext context) {
        if (draft == null) {
            throw new IllegalArgumentException("Teilrezept fehlt.");
        }

        String germanName = NameNormalizer.display(draft.germanName());
        if (germanName.isBlank()) {
            throw new IllegalArgumentException("Der Rezeptname auf Deutsch darf nicht leer sein.");
        }

        String englishName = NameNormalizer.display(draft.englishName());
        if (englishName.isBlank()) {
            throw new IllegalArgumentException("Der Rezeptname auf Englisch darf nicht leer sein.");
        }

        String categoryKey = NameNormalizer.display(draft.categoryKey());
        if (categoryKey.isBlank()) {
            throw new IllegalArgumentException("Bitte waehle eine Kategorie aus.");
        }

        if (!recipeBook.hasCategory(categoryKey)) {
            throw new IllegalArgumentException("Unbekannte Kategorie: " + categoryKey);
        }

        if (findDefinition(germanName).isPresent()
                || context.isKnownExistingIngredient(germanName)
                || context.hasRawIngredientName(germanName)
                || !context.registerRecipeName(germanName)) {
            throw new IllegalArgumentException("Rezept existiert bereits: " + germanName);
        }

        context.registerTerm(germanName, englishName);

        List<RecipeDraftVariant> variants = draft.variants() == null ? List.of() : draft.variants();
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("Mindestens eine Rezeptvariante ist erforderlich.");
        }

        List<RecipeVariant> newVariants = new java.util.ArrayList<>();
        int variantIndex = 1;
        for (RecipeDraftVariant variant : variants) {
            newVariants.add(toVariant(variant, variantIndex++, context));
        }

        context.addDefinition(new RecipeDefinition(germanName, categoryKey, newVariants));
        return germanName;
    }

    private RecipeVariant toVariant(RecipeDraftVariant variant, int variantIndex, RecipeSaveContext context) {
        List<RecipeDraftIngredient> ingredients = variant == null || variant.ingredients() == null
                ? List.of()
                : variant.ingredients();
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Jede Rezeptvariante muss mindestens eine Zutat haben.");
        }

        if (ingredients.size() > 3) {
            throw new IllegalArgumentException("Jede Rezeptvariante darf hoechstens 3 Zutaten haben.");
        }

        Map<Integer, String> optionsByPosition = new java.util.LinkedHashMap<>();
        for (RecipeDraftIngredient ingredient : ingredients) {
            int position = ingredient == null ? 0 : ingredient.position();
            if (position < 1 || position > 3) {
                throw new IllegalArgumentException("Ungueltige Zutatenposition: " + position);
            }

            if (optionsByPosition.containsKey(position)) {
                throw new IllegalArgumentException("Doppelte Zutatenposition: " + position);
            }

            optionsByPosition.put(position, resolveIngredient(ingredient, context));
        }

        List<IngredientSlot> slots = optionsByPosition.entrySet().stream()
                .map(entry -> new IngredientSlot(entry.getKey(), List.of(entry.getValue())))
                .toList();
        return new RecipeVariant(variantIndex, "custom", 0, slots);
    }

    private String resolveIngredient(RecipeDraftIngredient ingredient, RecipeSaveContext context) {
        String type = ingredient == null ? "" : NameNormalizer.display(ingredient.type()).toLowerCase(Locale.ROOT);
        return switch (type) {
            case INGREDIENT_TYPE_EXISTING -> resolveExistingIngredient(ingredient, context);
            case INGREDIENT_TYPE_NEW_RAW -> resolveNewRawIngredient(ingredient, context);
            case INGREDIENT_TYPE_NEW_RECIPE -> collectRecipe(ingredient.recipe(), context);
            default -> throw new IllegalArgumentException("Teilrezept fehlt.");
        };
    }

    private String resolveExistingIngredient(RecipeDraftIngredient ingredient, RecipeSaveContext context) {
        String existingKey = NameNormalizer.display(ingredient.existingKey());
        if (existingKey.isBlank()) {
            throw new IllegalArgumentException("Bestehende Zutat fehlt.");
        }

        if (!context.isKnownExistingIngredient(existingKey)) {
            throw new IllegalArgumentException("Unbekannte bestehende Zutat: " + existingKey);
        }

        return canonicalNameOrSelf(existingKey);
    }

    private String resolveNewRawIngredient(RecipeDraftIngredient ingredient, RecipeSaveContext context) {
        String germanName = NameNormalizer.display(ingredient.germanName());
        if (germanName.isBlank()) {
            throw new IllegalArgumentException("Neue Zutat auf Deutsch fehlt.");
        }

        String englishName = NameNormalizer.display(ingredient.englishName());
        if (englishName.isBlank()) {
            throw new IllegalArgumentException("Neue Zutat auf Englisch fehlt.");
        }

        if (context.isKnownExistingIngredient(germanName) || context.hasRecipeName(germanName)) {
            throw new IllegalArgumentException("Zutat existiert bereits: " + germanName);
        }

        context.registerRawIngredientName(germanName);
        context.registerTerm(germanName, englishName);
        return germanName;
    }

    public record IngredientCatalogEntry(String key, boolean craftable) {
    }

    public record SavedRecipe(String key, int createdRecipeCount) {
    }

    public record IngredientSearchHit(String name, String category, int variantCount, List<String> matches) {
    }

    private static final class RecipeSaveContext {

        private final LinkedHashSet<String> existingIngredientKeys;
        private final LinkedHashSet<String> newRecipeKeys = new LinkedHashSet<>();
        private final LinkedHashSet<String> newRawIngredientKeys = new LinkedHashSet<>();
        private final java.util.ArrayList<RecipeDefinition> newDefinitions = new java.util.ArrayList<>();
        private final java.util.LinkedHashMap<String, TermTranslation> newTermsByKey = new java.util.LinkedHashMap<>();

        private RecipeSaveContext(RecipeBook recipeBook) {
            this.existingIngredientKeys = recipeBook.allKnownTerms().stream()
                    .map(NameNormalizer::key)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }

        private boolean registerRecipeName(String recipeName) {
            return newRecipeKeys.add(NameNormalizer.key(recipeName));
        }

        private boolean hasRecipeName(String recipeName) {
            return newRecipeKeys.contains(NameNormalizer.key(recipeName));
        }

        private void registerRawIngredientName(String ingredientName) {
            newRawIngredientKeys.add(NameNormalizer.key(ingredientName));
        }

        private boolean hasRawIngredientName(String ingredientName) {
            return newRawIngredientKeys.contains(NameNormalizer.key(ingredientName));
        }

        private boolean isKnownExistingIngredient(String ingredientName) {
            return existingIngredientKeys.contains(NameNormalizer.key(ingredientName));
        }

        private void registerTerm(String germanName, String englishName) {
            String key = NameNormalizer.key(germanName);
            TermTranslation existingTranslation = newTermsByKey.get(key);
            String normalizedEnglishName = NameNormalizer.display(englishName);
            if (existingTranslation != null) {
                if (!existingTranslation.englishName().equals(normalizedEnglishName)) {
                    throw new IllegalArgumentException("Zutat existiert bereits: " + germanName);
                }
                return;
            }

            newTermsByKey.put(key, new TermTranslation(germanName, normalizedEnglishName));
        }

        private void addDefinition(RecipeDefinition definition) {
            newDefinitions.add(definition);
        }

        private List<RecipeDefinition> newDefinitions() {
            return newDefinitions;
        }

        private Map<String, String> newEnglishTermsByName() {
            java.util.LinkedHashMap<String, String> englishTerms = new java.util.LinkedHashMap<>();
            newTermsByKey.values().forEach(term -> englishTerms.put(term.germanName(), term.englishName()));
            return englishTerms;
        }
    }

    private record TermTranslation(String germanName, String englishName) {
    }
}
