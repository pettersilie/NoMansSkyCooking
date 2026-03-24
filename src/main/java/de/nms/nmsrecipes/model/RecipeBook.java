package de.nms.nmsrecipes.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RecipeBook {

    private final Map<String, RecipeDefinition> definitionsByKey;

    public RecipeBook(Map<String, RecipeDefinition> definitionsByKey) {
        this.definitionsByKey = Map.copyOf(new LinkedHashMap<>(definitionsByKey));
    }

    public List<RecipeDefinition> definitions() {
        return definitionsByKey.values().stream()
                .sorted(Comparator.comparing(RecipeDefinition::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
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
}
