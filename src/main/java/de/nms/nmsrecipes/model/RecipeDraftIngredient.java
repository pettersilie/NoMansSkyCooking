package de.nms.nmsrecipes.model;

public record RecipeDraftIngredient(int position,
                                    String type,
                                    String existingKey,
                                    String germanName,
                                    String englishName,
                                    RecipeDraft recipe) {
}
