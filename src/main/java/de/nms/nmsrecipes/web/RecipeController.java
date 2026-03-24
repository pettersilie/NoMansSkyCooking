package de.nms.nmsrecipes.web;

import de.nms.nmsrecipes.model.TreeNode;
import de.nms.nmsrecipes.service.LocalizationService;
import de.nms.nmsrecipes.service.ProductPriceService;
import de.nms.nmsrecipes.service.RecipeCatalogService;
import de.nms.nmsrecipes.service.RecipeGraphService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RecipeController {

    private final RecipeCatalogService catalogService;
    private final RecipeGraphService graphService;
    private final ProductPriceService priceService;
    private final LocalizationService localizationService;

    public RecipeController(RecipeCatalogService catalogService,
                            RecipeGraphService graphService,
                            ProductPriceService priceService,
                            LocalizationService localizationService) {
        this.catalogService = catalogService;
        this.graphService = graphService;
        this.priceService = priceService;
        this.localizationService = localizationService;
    }

    @GetMapping("/products")
    public List<ProductSummary> products(@RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        return catalogService.definitions().stream()
                .map(definition -> new ProductSummary(
                        definition.name(),
                        localizationService.localizeTerm(definition.name(), normalizedLanguage),
                        definition.category(),
                        localizationService.localizeCategory(definition.category(), normalizedLanguage),
                        definition.variants().size(),
                        definition.variants().stream()
                                .mapToInt(variant -> variant.slots().size())
                                .min()
                                .orElse(0),
                        definition.variants().stream()
                                .mapToInt(variant -> variant.slots().size())
                                .max()
                                .orElse(0),
                        priceService.findDisplayPrice(definition.name()).orElse(null)))
                .toList();
    }

    @GetMapping("/graph")
    public TreeNode graph(@RequestParam("product") String productName,
                          @RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        try {
            return graphService.buildGraph(productName, normalizedLanguage);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    localizationService.productNotFoundMessage(normalizedLanguage),
                    exception);
        }
    }

    @GetMapping("/ingredients/search")
    public List<IngredientSearchResult> searchByIngredient(@RequestParam("query") String query,
                                                           @RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        return catalogService.searchByIngredient(query,
                        ingredient -> localizationService.localizeTerm(ingredient, normalizedLanguage)).stream()
                .map(hit -> new IngredientSearchResult(
                        hit.name(),
                        localizationService.localizeTerm(hit.name(), normalizedLanguage),
                        hit.category(),
                        localizationService.localizeCategory(hit.category(), normalizedLanguage),
                        hit.variantCount(),
                        hit.matches(),
                        priceService.findDisplayPrice(hit.name()).orElse(null)))
                .toList();
    }

    @PutMapping("/prices")
    public PriceUpdateResponse savePrice(@Valid @RequestBody PriceUpdateRequest request,
                                         @RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        try {
            return new PriceUpdateResponse(
                    request.key(),
                    priceService.savePrice(request.key(), request.price()).orElse(null));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    localizationService.localizeErrorMessage(exception.getMessage(), normalizedLanguage),
                    exception);
        }
    }

    public record ProductSummary(String key,
                                 String name,
                                 String categoryKey,
                                 String category,
                                 int variantCount,
                                 int minIngredientCount,
                                 int maxIngredientCount,
                                 String price) {
    }

    public record IngredientSearchResult(String key,
                                         String name,
                                         String categoryKey,
                                         String category,
                                         int variantCount,
                                         List<String> matches,
                                         String price) {
    }

    public record PriceUpdateRequest(@NotBlank String key, String price) {
    }

    public record PriceUpdateResponse(String key, String price) {
    }
}
