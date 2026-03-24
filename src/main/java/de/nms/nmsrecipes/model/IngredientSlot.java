package de.nms.nmsrecipes.model;

import java.util.List;

public record IngredientSlot(int position, List<String> options) {

    public IngredientSlot {
        options = List.copyOf(options);
    }
}
