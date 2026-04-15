package de.nms.nmsrecipes.model;

import java.util.List;

public record RefineryDefinition(String name, String category, List<RefineryVariant> variants) {

    public RefineryDefinition {
        variants = List.copyOf(variants);
    }
}
