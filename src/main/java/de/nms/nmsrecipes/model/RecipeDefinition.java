package de.nms.nmsrecipes.model;

import java.util.List;

public record RecipeDefinition(String name, String category, List<RecipeVariant> variants) {

    public RecipeDefinition {
        variants = List.copyOf(variants);
    }
}
