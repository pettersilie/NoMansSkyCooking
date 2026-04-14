package de.nms.nmsrecipes.model;

import java.util.List;

public record RecipeDraft(String germanName,
                          String englishName,
                          String categoryKey,
                          List<RecipeDraftVariant> variants) {

    public RecipeDraft {
        variants = variants == null ? List.of() : List.copyOf(variants);
    }
}
