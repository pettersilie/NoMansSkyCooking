package de.nms.nmsrecipes.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RefineryBook {

    private final Map<String, RefineryDefinition> definitionsByKey;
    private final List<String> categories;
    private final Map<String, String> englishCategoryNamesByKey;
    private final Map<String, String> englishTermNamesByKey;

    public RefineryBook(Map<String, RefineryDefinition> definitionsByKey,
                        List<String> categories,
                        Map<String, String> englishCategoryNamesByKey,
                        Map<String, String> englishTermNamesByKey) {
        this.definitionsByKey = Map.copyOf(new LinkedHashMap<>(definitionsByKey));
        this.categories = List.copyOf(mergeCategories(definitionsByKey.values(), categories));
        this.englishCategoryNamesByKey = Map.copyOf(normalizeEnglishNames(englishCategoryNamesByKey));
        this.englishTermNamesByKey = Map.copyOf(normalizeEnglishNames(englishTermNamesByKey));
    }

    public List<RefineryDefinition> definitions() {
        return definitionsByKey.values().stream()
                .sorted(Comparator.comparing(RefineryDefinition::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> categories() {
        return categories;
    }

    public Optional<String> englishCategoryName(String category) {
        String categoryKey = NameNormalizer.key(category);
        if (categoryKey.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(englishCategoryNamesByKey.get(categoryKey));
    }

    public Optional<String> englishTermName(String term) {
        String termKey = NameNormalizer.key(term);
        if (termKey.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(englishTermNamesByKey.get(termKey));
    }

    public Optional<RefineryDefinition> findDefinition(String productName) {
        if (productName == null || productName.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(definitionsByKey.get(NameNormalizer.key(productName)));
    }

    public String canonicalNameOrSelf(String productName) {
        return findDefinition(productName)
                .map(RefineryDefinition::name)
                .orElse(NameNormalizer.display(productName));
    }

    public List<String> allKnownTerms() {
        Map<String, String> termsByKey = new LinkedHashMap<>();
        for (RefineryDefinition definition : definitions()) {
            addTerm(termsByKey, definition.name());
            for (RefineryVariant variant : definition.variants()) {
                for (RefineryIngredient ingredient : variant.ingredients()) {
                    addTerm(termsByKey, ingredient.name());
                }
            }
        }

        return termsByKey.values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static List<String> mergeCategories(Collection<RefineryDefinition> definitions, List<String> explicitCategories) {
        Map<String, String> categoriesByKey = new LinkedHashMap<>();
        addCategories(categoriesByKey, explicitCategories);

        for (RefineryDefinition definition : definitions) {
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

    private static Map<String, String> normalizeEnglishNames(Map<String, String> englishNamesByKey) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (englishNamesByKey == null) {
            return normalized;
        }

        englishNamesByKey.forEach((value, englishName) -> {
            String key = NameNormalizer.key(value);
            String normalizedEnglishName = NameNormalizer.display(englishName);
            if (key.isBlank() || normalizedEnglishName.isBlank()) {
                return;
            }

            normalized.put(key, normalizedEnglishName);
        });
        return normalized;
    }
}
