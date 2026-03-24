package de.nms.nmsrecipes.model;

import java.util.List;

public record RecipeVariant(int index, String sourceSheet, int sourceRow, List<IngredientSlot> slots) {

    public RecipeVariant {
        slots = List.copyOf(slots);
    }
}
