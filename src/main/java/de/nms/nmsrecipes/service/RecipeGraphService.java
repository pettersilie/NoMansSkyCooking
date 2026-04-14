package de.nms.nmsrecipes.service;

import de.nms.nmsrecipes.model.IngredientSlot;
import de.nms.nmsrecipes.model.NameNormalizer;
import de.nms.nmsrecipes.model.RecipeDefinition;
import de.nms.nmsrecipes.model.RecipeVariant;
import de.nms.nmsrecipes.model.TreeNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RecipeGraphService {

    private static final int SLOT_COUNT = 3;

    private final RecipeCatalogService catalogService;
    private final ProductPriceService priceService;
    private final LocalizationService localizationService;

    public RecipeGraphService(RecipeCatalogService catalogService,
                              ProductPriceService priceService,
                              LocalizationService localizationService) {
        this.catalogService = catalogService;
        this.priceService = priceService;
        this.localizationService = localizationService;
    }

    public TreeNode buildGraph(String productName, String language) {
        RecipeDefinition definition = catalogService.requireDefinition(productName);
        return buildProductNode(definition.name(), language, new LinkedHashSet<>(), new AtomicLong());
    }

    private TreeNode buildProductNode(String productName,
                                      String language,
                                      LinkedHashSet<String> stack,
                                      AtomicLong sequence) {
        RecipeDefinition definition = catalogService.findDefinition(productName).orElse(null);
        String canonicalName = catalogService.canonicalNameOrSelf(productName);

        if (definition == null) {
            return new TreeNode(nextId(sequence, "raw"),
                    canonicalName,
                    localizeTerm(canonicalName, language),
                    "raw",
                    localizationService.priceDetail(priceService.findDisplayPrice(canonicalName).orElse(null), language),
                    List.of());
        }

        String key = NameNormalizer.key(definition.name());
        if (stack.contains(key)) {
            return new TreeNode(nextId(sequence, "cycle"),
                    definition.name(),
                    localizeTerm(definition.name(), language),
                    "cycle",
                    localizationService.priceDetail(priceService.findDisplayPrice(definition.name()).orElse(null), language),
                    List.of());
        }

        stack.add(key);
        try {
            List<TreeNode> children;
            if (definition.variants().size() == 1) {
                children = buildSlotNodes(definition.variants().getFirst(), language, stack, sequence);
            } else {
                children = new ArrayList<>();
                for (RecipeVariant variant : definition.variants()) {
                    children.add(new TreeNode(nextId(sequence, "variant"),
                            null,
                            localizationService.variantLabel(variant.index(), language),
                            "variant",
                            null,
                            buildSlotNodes(variant, language, stack, sequence)));
                }
            }

            return new TreeNode(nextId(sequence, "product"),
                    canonicalName,
                    localizeTerm(definition.name(), language),
                    "product",
                    localizationService.priceDetail(priceService.findDisplayPrice(definition.name()).orElse(null), language),
                    children);
        } finally {
            stack.remove(key);
        }
    }

    private List<TreeNode> buildSlotNodes(RecipeVariant variant,
                                          String language,
                                          LinkedHashSet<String> stack,
                                          AtomicLong sequence) {
        Map<Integer, IngredientSlot> slotsByPosition = new LinkedHashMap<>();
        for (IngredientSlot slot : variant.slots()) {
            slotsByPosition.put(slot.position(), slot);
        }

        List<TreeNode> slots = new ArrayList<>();
        for (int position = 1; position <= SLOT_COUNT; position++) {
            IngredientSlot slot = slotsByPosition.get(position);
            List<TreeNode> ingredients = new ArrayList<>();
            if (slot != null) {
                LinkedHashSet<String> seenIngredients = new LinkedHashSet<>();
                for (String option : slot.options()) {
                    String canonicalOption = catalogService.canonicalNameOrSelf(option);
                    if (seenIngredients.add(NameNormalizer.key(canonicalOption))) {
                        ingredients.add(buildProductNode(canonicalOption, language, stack, sequence));
                    }
                }
            }

            slots.add(new TreeNode(nextId(sequence, "slot"),
                    null,
                    localizationService.slotLabel(position, language),
                    "slot",
                    null,
                    ingredients));
        }

        return slots;
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
