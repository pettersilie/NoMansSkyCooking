package de.nms.nmsrecipes.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "recipes")
public record RecipeProperties(@NotBlank String sourcePath,
                               @NotBlank @DefaultValue("./data/product-prices.json") String pricePath) {
}
