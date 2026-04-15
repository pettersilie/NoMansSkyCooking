package de.nms.nmsrecipes.model;

import java.util.List;

public record RefineryVariant(int index,
                              String operation,
                              String englishOperation,
                              String time,
                              int outputQuantity,
                              List<RefineryIngredient> ingredients) {

    public RefineryVariant {
        ingredients = List.copyOf(ingredients);
    }
}
