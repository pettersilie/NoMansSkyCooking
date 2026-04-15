package de.nms.nmsrecipes.web;

import de.nms.nmsrecipes.model.RecipeDraft;
import de.nms.nmsrecipes.model.RecipeDefinition;
import de.nms.nmsrecipes.model.RecipeVariant;
import de.nms.nmsrecipes.model.TreeNode;
import de.nms.nmsrecipes.model.IngredientSlot;
import de.nms.nmsrecipes.service.LocalizationService;
import de.nms.nmsrecipes.service.ProductPriceService;
import de.nms.nmsrecipes.service.RecipeCatalogService;
import de.nms.nmsrecipes.service.RecipeGraphService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
                        localizeTerm(definition.name(), normalizedLanguage),
                        definition.category(),
                        localizeCategory(definition.category(), normalizedLanguage),
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

    @GetMapping("/ingredients/catalog")
    public List<IngredientCatalogEntry> ingredientCatalog(@RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        return catalogService.ingredientCatalog().stream()
                .map(ingredient -> new IngredientCatalogEntry(
                        ingredient.key(),
                        localizeTerm(ingredient.key(), normalizedLanguage),
                        ingredient.craftable()))
                .sorted(Comparator.comparing(IngredientCatalogEntry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @GetMapping("/categories")
    public List<CategorySummary> categories(@RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        return catalogService.categories().stream()
                .map(category -> new CategorySummary(
                        category,
                        localizeCategory(category, normalizedLanguage)))
                .toList();
    }

    @GetMapping("/recipes/overview")
    public List<RecipeOverviewRow> recipeOverview(@RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        return catalogService.definitions().stream()
                .sorted(Comparator.comparing(
                        definition -> localizeTerm(definition.name(), normalizedLanguage),
                        String.CASE_INSENSITIVE_ORDER))
                .flatMap(definition -> definition.variants().stream()
                        .sorted(Comparator.comparingInt(RecipeVariant::index))
                        .map(variant -> toRecipeOverviewRow(definition, variant, normalizedLanguage)))
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
                        ingredient -> localizeTerm(ingredient, normalizedLanguage)).stream()
                .map(hit -> new IngredientSearchResult(
                        hit.name(),
                        localizeTerm(hit.name(), normalizedLanguage),
                        hit.category(),
                        localizeCategory(hit.category(), normalizedLanguage),
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

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategorySummary saveCategory(@RequestBody CategoryCreateRequest request,
                                        @RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        try {
            String savedCategory = catalogService.saveCategory(
                    request == null ? null : request.germanName(),
                    request == null ? null : request.englishName());
            return new CategorySummary(
                    savedCategory,
                    localizeCategory(savedCategory, normalizedLanguage));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    localizationService.localizeErrorMessage(exception.getMessage(), normalizedLanguage),
                    exception);
        }
    }

    @PostMapping("/recipes")
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeSaveResponse saveRecipe(@RequestBody RecipeDraft request,
                                         @RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        try {
            RecipeCatalogService.SavedRecipe savedRecipe = catalogService.saveRecipe(request);
            return new RecipeSaveResponse(
                    savedRecipe.key(),
                    localizeTerm(savedRecipe.key(), normalizedLanguage),
                    savedRecipe.createdRecipeCount());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    localizationService.localizeErrorMessage(exception.getMessage(), normalizedLanguage),
                    exception);
        }
    }

    private String localizeCategory(String category, String language) {
        return localizationService.localizeCategory(
                category,
                language,
                catalogService.findEnglishCategoryName(category).orElse(null));
    }

    private String localizeTerm(String term, String language) {
        return localizationService.localizeTerm(
                term,
                language,
                catalogService.findEnglishTermName(term).orElse(null));
    }

    private RecipeOverviewRow toRecipeOverviewRow(RecipeDefinition definition,
                                                  RecipeVariant variant,
                                                  String language) {
        String[] topLevelIngredients = {"", "", ""};
        variant.slots().stream()
                .sorted(Comparator.comparingInt(IngredientSlot::position))
                .forEach(slot -> {
                    int index = slot.position() - 1;
                    if (index >= 0 && index < topLevelIngredients.length) {
                        topLevelIngredients[index] = localizeIngredientOptions(slot.options(), language);
                    }
                });

        return new RecipeOverviewRow(
                definition.name(),
                localizeTerm(definition.name(), language),
                variant.index(),
                topLevelIngredients[0],
                topLevelIngredients[1],
                topLevelIngredients[2],
                priceService.findDisplayPrice(definition.name()).orElse(null));
    }

    private String localizeIngredientOptions(List<String> options, String language) {
        return options.stream()
                .map(option -> localizeTerm(option, language))
                .map(String::trim)
                .filter(option -> !option.isBlank())
                .distinct()
                .collect(Collectors.joining(" / "));
    }

    public record CategorySummary(String key, String name) {
    }

    public record IngredientCatalogEntry(String key, String name, boolean craftable) {
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

    public record RecipeOverviewRow(String key,
                                    String name,
                                    int variantIndex,
                                    String ingredient1,
                                    String ingredient2,
                                    String ingredient3,
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

    public record CategoryCreateRequest(String germanName, String englishName) {
    }

    public record RecipeSaveResponse(String key, String name, int createdRecipeCount) {
    }
}
