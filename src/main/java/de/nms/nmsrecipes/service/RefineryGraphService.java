package de.nms.nmsrecipes.service;

import de.nms.nmsrecipes.model.NameNormalizer;
import de.nms.nmsrecipes.model.RefineryDefinition;
import de.nms.nmsrecipes.model.RefineryIngredient;
import de.nms.nmsrecipes.model.RefineryVariant;
import de.nms.nmsrecipes.model.TreeNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RefineryGraphService {

    private static final int SLOT_COUNT = 3;
    private static final int MAX_EXPANDED_PRODUCTS = 160;

    private final RefineryCatalogService catalogService;
    private final LocalizationService localizationService;

    public RefineryGraphService(RefineryCatalogService catalogService,
                                LocalizationService localizationService) {
        this.catalogService = catalogService;
        this.localizationService = localizationService;
    }

    public TreeNode buildGraph(String productName, String language) {
        RefineryDefinition definition = catalogService.requireDefinition(productName);
        return buildProductNode(
                definition.name(),
                0,
                language,
                new LinkedHashSet<>(),
                new AtomicLong(),
                new AtomicInteger(MAX_EXPANDED_PRODUCTS),
                true);
    }

    private TreeNode buildProductNode(String productName,
                                      int quantity,
                                      String language,
                                      LinkedHashSet<String> stack,
                                      AtomicLong sequence,
                                      AtomicInteger remainingExpansions,
                                      boolean forceExpand) {
        RefineryDefinition definition = catalogService.findDefinition(productName).orElse(null);
        String canonicalName = catalogService.canonicalNameOrSelf(productName);
        String detail = localizationService.quantityDetail(quantity, language);

        if (definition == null) {
            return new TreeNode(
                    nextId(sequence, "raw"),
                    canonicalName,
                    localizeTerm(canonicalName, language),
                    "raw",
                    detail,
                    List.of());
        }

        String key = NameNormalizer.key(definition.name());
        if (stack.contains(key)) {
            return new TreeNode(
                    nextId(sequence, "cycle"),
                    definition.name(),
                    localizeTerm(definition.name(), language),
                    "cycle",
                    detail,
                    List.of());
        }

        if (!forceExpand && remainingExpansions.getAndDecrement() <= 0) {
            return new TreeNode(
                    nextId(sequence, "product"),
                    canonicalName,
                    localizeTerm(definition.name(), language),
                    "product",
                    localizationService.expansionLimitedDetail(detail, language),
                    List.of());
        }

        stack.add(key);
        try {
            List<TreeNode> children;
            if (definition.variants().size() == 1) {
                children = buildSlotNodes(definition.variants().getFirst(), language, stack, sequence, remainingExpansions);
            } else {
                children = new ArrayList<>();
                for (RefineryVariant variant : definition.variants()) {
                    children.add(new TreeNode(
                            nextId(sequence, "variant"),
                            null,
                            localizationService.variantLabel(variant.index(), language),
                            "variant",
                            localizationService.refineryVariantDetail(
                                    localizedOperation(variant, language),
                                    variant.outputQuantity(),
                                    variant.time(),
                                    language),
                            buildSlotNodes(variant, language, stack, sequence, remainingExpansions)));
                }
            }

            return new TreeNode(
                    nextId(sequence, "product"),
                    canonicalName,
                    localizeTerm(definition.name(), language),
                    "product",
                    detail,
                    children);
        } finally {
            stack.remove(key);
        }
    }

    private List<TreeNode> buildSlotNodes(RefineryVariant variant,
                                          String language,
                                          LinkedHashSet<String> stack,
                                          AtomicLong sequence,
                                          AtomicInteger remainingExpansions) {
        List<TreeNode> slots = new ArrayList<>();
        List<RefineryIngredient> sortedIngredients = variant.ingredients().stream()
                .sorted(Comparator.comparingInt(RefineryIngredient::position))
                .toList();

        for (int position = 1; position <= SLOT_COUNT; position++) {
            int slotPosition = position;
            RefineryIngredient ingredient = sortedIngredients.stream()
                    .filter(candidate -> candidate.position() == slotPosition)
                    .findFirst()
                    .orElse(null);

            List<TreeNode> ingredients = ingredient == null
                    ? List.of()
                    : List.of(buildProductNode(
                            ingredient.name(),
                            ingredient.quantity(),
                            language,
                            stack,
                            sequence,
                            remainingExpansions,
                            false));

            slots.add(new TreeNode(
                    nextId(sequence, "slot"),
                    null,
                    localizationService.slotLabel(slotPosition, language),
                    "slot",
                    null,
                    ingredients));
        }

        return slots;
    }

    private String localizedOperation(RefineryVariant variant, String language) {
        if (localizationService.isEnglish(language)) {
            return NameNormalizer.display(variant.englishOperation());
        }

        return NameNormalizer.display(variant.operation());
    }

    private String nextId(AtomicLong sequence, String prefix) {
        return prefix + "-" + sequence.incrementAndGet();
    }

    private String localizeTerm(String term, String language) {
        return localizationService.localizeTerm(
                term,
                language,
                catalogService.findEnglishTermName(term).orElse(null));
    }
}
