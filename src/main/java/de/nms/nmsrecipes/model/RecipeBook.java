package de.nms.nmsrecipes.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RecipeBook {

    private final Map<String, RecipeDefinition> definitionsByKey;
    private final List<String> categories;
    private final Map<String, String> englishCategoryNamesByKey;
    private final Map<String, String> englishTermNamesByKey;

    public RecipeBook(Map<String, RecipeDefinition> definitionsByKey) {
        this(definitionsByKey, List.of(), Map.of(), Map.of());
    }

    public RecipeBook(Map<String, RecipeDefinition> definitionsByKey, List<String> categories) {
        this(definitionsByKey, categories, Map.of(), Map.of());
    }

    public RecipeBook(Map<String, RecipeDefinition> definitionsByKey,
                      List<String> categories,
                      Map<String, String> englishCategoryNamesByKey) {
        this(definitionsByKey, categories, englishCategoryNamesByKey, Map.of());
    }

    public RecipeBook(Map<String, RecipeDefinition> definitionsByKey,
                      List<String> categories,
                      Map<String, String> englishCategoryNamesByKey,
                      Map<String, String> englishTermNamesByKey) {
        this.definitionsByKey = Map.copyOf(new LinkedHashMap<>(definitionsByKey));
        this.categories = List.copyOf(mergeCategories(definitionsByKey.values(), categories));
        this.englishCategoryNamesByKey = Map.copyOf(normalizeEnglishCategoryNames(englishCategoryNamesByKey));
        this.englishTermNamesByKey = Map.copyOf(normalizeEnglishNames(englishTermNamesByKey));
    }

    public List<RecipeDefinition> definitions() {
        return definitionsByKey.values().stream()
                .sorted(Comparator.comparing(RecipeDefinition::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> categories() {
        return categories;
    }

    public boolean hasCategory(String category) {
        String categoryKey = NameNormalizer.key(category);
        if (categoryKey.isBlank()) {
            return false;
        }

        return categories.stream()
                .anyMatch(existingCategory -> NameNormalizer.key(existingCategory).equals(categoryKey));
    }

    public Optional<String> englishCategoryName(String category) {
        String categoryKey = NameNormalizer.key(category);
        if (categoryKey.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(englishCategoryNamesByKey.get(categoryKey));
    }

    public RecipeBook withAddedCategory(String category, String englishCategoryName) {
        List<String> updatedCategories = new ArrayList<>(categories);
        updatedCategories.add(category);
        Map<String, String> updatedEnglishNames = new LinkedHashMap<>(englishCategoryNamesByKey);
        String categoryKey = NameNormalizer.key(category);
        String normalizedEnglishName = NameNormalizer.display(englishCategoryName);
        if (!normalizedEnglishName.isBlank()) {
            updatedEnglishNames.put(categoryKey, normalizedEnglishName);
        }
        return new RecipeBook(definitionsByKey, updatedCategories, updatedEnglishNames, englishTermNamesByKey);
    }

    public Optional<String> englishTermName(String term) {
        String termKey = NameNormalizer.key(term);
        if (termKey.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(englishTermNamesByKey.get(termKey));
    }

    public RecipeBook withMergedRecipes(List<RecipeDefinition> definitions, Map<String, String> englishTermNames) {
        Map<String, RecipeDefinition> updatedDefinitions = new LinkedHashMap<>(definitionsByKey);
        if (definitions != null) {
            for (RecipeDefinition definition : definitions) {
                if (definition == null) {
                    continue;
                }
                updatedDefinitions.put(NameNormalizer.key(definition.name()), definition);
            }
        }

        Map<String, String> updatedEnglishTerms = new LinkedHashMap<>(englishTermNamesByKey);
        updatedEnglishTerms.putAll(normalizeEnglishNames(englishTermNames));
        return new RecipeBook(updatedDefinitions, categories, englishCategoryNamesByKey, updatedEnglishTerms);
    }

    public Optional<RecipeDefinition> findDefinition(String productName) {
        if (productName == null || productName.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(definitionsByKey.get(NameNormalizer.key(productName)));
    }

    public String canonicalNameOrSelf(String productName) {
        return findDefinition(productName)
                .map(RecipeDefinition::name)
                .orElse(NameNormalizer.display(productName));
    }

    public List<String> allProductNames() {
        List<String> names = new ArrayList<>();
        for (RecipeDefinition definition : definitions()) {
            names.add(definition.name());
        }
        return names;
    }

    public List<String> allKnownTerms() {
        Map<String, String> termsByKey = new LinkedHashMap<>();
        for (RecipeDefinition definition : definitions()) {
            addTerm(termsByKey, definition.name());
            for (RecipeVariant variant : definition.variants()) {
                for (IngredientSlot slot : variant.slots()) {
                    for (String option : slot.options()) {
                        addTerm(termsByKey, option);
                    }
                }
            }
        }

        return termsByKey.values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static List<String> mergeCategories(Collection<RecipeDefinition> definitions, List<String> explicitCategories) {
        Map<String, String> categoriesByKey = new LinkedHashMap<>();
        addCategories(categoriesByKey, explicitCategories);

        for (RecipeDefinition definition : definitions) {
            addCategory(categoriesByKey, definition.category());
        }

        return categoriesByKey.values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static void addCategories(Map<String, String> categoriesByKey, List<String> categories) {
        if (categories == null) {
            return;
        }

        for (String category : categories) {
            addCategory(categoriesByKey, category);
        }
    }

    private static void addCategory(Map<String, String> categoriesByKey, String category) {
        String normalizedCategory = NameNormalizer.display(category);
        if (normalizedCategory.isBlank()) {
            return;
        }

        categoriesByKey.putIfAbsent(NameNormalizer.key(normalizedCategory), normalizedCategory);
    }

    private static void addTerm(Map<String, String> termsByKey, String term) {
        String normalizedTerm = NameNormalizer.display(term);
        if (normalizedTerm.isBlank()) {
            return;
        }

        termsByKey.putIfAbsent(NameNormalizer.key(normalizedTerm), normalizedTerm);
    }

    private static Map<String, String> normalizeEnglishCategoryNames(Map<String, String> englishCategoryNamesByKey) {
        return normalizeEnglishNames(englishCategoryNamesByKey);
    }

    private static Map<String, String> normalizeEnglishNames(Map<String, String> englishNamesByKey) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (englishNamesByKey == null) {
            return normalized;
        }

        englishNamesByKey.forEach((value, englishName) -> {
            String categoryKey = NameNormalizer.key(value);
            String normalizedEnglishName = NameNormalizer.display(englishName);
            if (categoryKey.isBlank() || normalizedEnglishName.isBlank()) {
                return;
            }

            normalized.put(categoryKey, normalizedEnglishName);
        });
        return normalized;
    }
}
