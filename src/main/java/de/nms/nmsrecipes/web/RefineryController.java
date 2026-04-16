package de.nms.nmsrecipes.web;

import de.nms.nmsrecipes.model.TreeNode;
import de.nms.nmsrecipes.service.LocalizationService;
import de.nms.nmsrecipes.service.RefineryCatalogService;
import de.nms.nmsrecipes.service.RefineryGraphService;
import de.nms.nmsrecipes.service.RecipeCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/refinery")
public class RefineryController {

    private final RefineryCatalogService catalogService;
    private final RefineryGraphService graphService;
    private final LocalizationService localizationService;
    private final RecipeCatalogService recipeCatalogService;

    public RefineryController(RefineryCatalogService catalogService,
                              RefineryGraphService graphService,
                              LocalizationService localizationService,
                              RecipeCatalogService recipeCatalogService) {
        this.catalogService = catalogService;
        this.graphService = graphService;
        this.localizationService = localizationService;
        this.recipeCatalogService = recipeCatalogService;
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
                                .mapToInt(variant -> variant.ingredients().size())
                                .min()
                                .orElse(0),
                        definition.variants().stream()
                                .mapToInt(variant -> variant.ingredients().size())
                                .max()
                                .orElse(0)))
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

    @GetMapping("/overview")
    public List<RefineryOverviewRow> overview(@RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        return catalogService.definitions().stream()
                .sorted(Comparator.comparing(
                        definition -> localizeTerm(definition.name(), normalizedLanguage),
                        String.CASE_INSENSITIVE_ORDER))
                .flatMap(definition -> definition.variants().stream()
                        .sorted(Comparator.comparingInt(variant -> variant.index()))
                        .map(variant -> toOverviewRow(definition, variant, normalizedLanguage)))
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
                        hit.matches()))
                .toList();
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

    private RefineryOverviewRow toOverviewRow(de.nms.nmsrecipes.model.RefineryDefinition definition,
                                              de.nms.nmsrecipes.model.RefineryVariant variant,
                                              String language) {
        String[] ingredients = {"", "", ""};
        variant.ingredients().stream()
                .sorted(Comparator.comparingInt(ingredient -> ingredient.position()))
                .forEach(ingredient -> {
                    int index = ingredient.position() - 1;
                    if (index >= 0 && index < ingredients.length) {
                        ingredients[index] = formatIngredient(ingredient.name(), ingredient.quantity(), language);
                    }
                });

        return new RefineryOverviewRow(
                definition.name(),
                localizeAnyTerm(definition.name(), language),
                definition.category(),
                localizeCategory(definition.category(), language),
                variant.index(),
                ingredients[0],
                ingredients[1],
                ingredients[2],
                new OverviewEntry(definition.name(), localizeAnyTerm(definition.name(), language), "refinery"),
                overviewEntriesForPosition(variant, language, 1),
                overviewEntriesForPosition(variant, language, 2),
                overviewEntriesForPosition(variant, language, 3));
    }

    private String formatIngredient(String ingredientName, int quantity, String language) {
        String localizedName = localizeAnyTerm(ingredientName, language);
        if (quantity <= 1) {
            return localizedName;
        }

        return quantity + " x " + localizedName;
    }

    private List<OverviewEntry> overviewEntriesForPosition(de.nms.nmsrecipes.model.RefineryVariant variant,
                                                           String language,
                                                           int position) {
        return variant.ingredients().stream()
                .filter(ingredient -> ingredient.position() == position)
                .findFirst()
                .map(ingredient -> List.of(toOverviewEntry(ingredient.name(), ingredient.quantity(), language)))
                .orElse(List.of());
    }

    private OverviewEntry toOverviewEntry(String term, int quantity, String language) {
        String destination = null;
        String key = catalogService.canonicalNameOrSelf(term);
        if (catalogService.findDefinition(key).isPresent()) {
            destination = "refinery";
        } else {
            String cookingKey = recipeCatalogService.canonicalNameOrSelf(term);
            if (recipeCatalogService.findDefinition(cookingKey).isPresent()) {
                key = cookingKey;
                destination = "cooking";
            }
        }

        String localizedName = localizeAnyTerm(key, language);
        if (quantity > 1) {
            localizedName = quantity + " x " + localizedName;
        }
        return new OverviewEntry(key, localizedName, destination);
    }

    private String localizeAnyTerm(String term, String language) {
        return localizationService.localizeTerm(
                term,
                language,
                catalogService.findEnglishTermName(term)
                        .or(() -> recipeCatalogService.findEnglishTermName(term))
                        .orElse(null));
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
                                 int maxIngredientCount) {
    }

    public record RefineryOverviewRow(String key,
                                      String name,
                                      String categoryKey,
                                      String category,
                                      int variantIndex,
                                      String ingredient1,
                                      String ingredient2,
                                      String ingredient3,
                                      OverviewEntry target,
                                      List<OverviewEntry> ingredient1Entries,
                                      List<OverviewEntry> ingredient2Entries,
                                      List<OverviewEntry> ingredient3Entries) {
    }

    public record OverviewEntry(String key, String name, String destination) {
    }

    public record IngredientSearchResult(String key,
                                         String name,
                                         String categoryKey,
                                         String category,
                                         int variantCount,
                                         List<String> matches) {
    }
}
