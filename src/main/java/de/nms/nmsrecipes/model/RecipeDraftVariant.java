package de.nms.nmsrecipes.model;

import java.util.List;

public record RecipeDraftVariant(List<RecipeDraftIngredient> ingredients) {

    public RecipeDraftVariant {
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
    }
}
