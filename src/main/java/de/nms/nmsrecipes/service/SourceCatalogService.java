package de.nms.nmsrecipes.service;

import de.nms.nmsrecipes.model.IngredientSlot;
import de.nms.nmsrecipes.model.NameNormalizer;
import de.nms.nmsrecipes.model.RecipeDefinition;
import de.nms.nmsrecipes.model.RecipeVariant;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class SourceCatalogService {

    private static final Logger log = LoggerFactory.getLogger(SourceCatalogService.class);

    private final JsonSourceCatalogStore sourceStore;
    private final RecipeCatalogService recipeCatalogService;
    private final RefineryCatalogService refineryCatalogService;
    private final LocalizationService localizationService;
    private final String sourcesPath;

    private volatile SourceCatalog catalog = new SourceCatalog(Map.of(), Map.of(), Map.of(), Set.of());

    public SourceCatalogService(JsonSourceCatalogStore sourceStore,
                                RecipeCatalogService recipeCatalogService,
                                RefineryCatalogService refineryCatalogService,
                                LocalizationService localizationService,
                                @Value("${recipes.sources-path:./data/sources/material-sources.json}") String sourcesPath) {
        this.sourceStore = sourceStore;
        this.recipeCatalogService = recipeCatalogService;
        this.refineryCatalogService = refineryCatalogService;
        this.localizationService = localizationService;
        this.sourcesPath = sourcesPath;
    }

    @PostConstruct
    public void load() {
        Path sourcePath = resolveSourcePath();
        JsonSourceCatalogStore.SourceCatalogPayload payload = sourceStore.load(sourcePath);

        Set<String> eligibleMaterials = eligibleMaterialNames();
        Map<String, String> eligibleBySourceKey = new LinkedHashMap<>();
        for (String material : eligibleMaterials) {
            eligibleBySourceKey.put(sourceKey(material), material);
        }

        Set<String> excludedMaterials = new LinkedHashSet<>();
        for (String excluded : payload.excluded()) {
            String actual = eligibleBySourceKey.get(sourceKey(excluded));
            if (actual != null) {
                excludedMaterials.add(actual);
            } else {
                log.warn("Ignoring excluded source entry '{}' because no matching material exists.", excluded);
            }
        }

        Map<String, SourceGroup> groupsByKey = new LinkedHashMap<>();
        Map<String, SourceMaterial> materialsByName = new LinkedHashMap<>();
        Map<String, String> materialsBySourceKey = new LinkedHashMap<>();

        for (JsonSourceCatalogStore.SourceGroupPayload rawGroup : payload.groups()) {
            if (rawGroup.key().isBlank()) {
                continue;
            }

            SourceGroup group = new SourceGroup(
                    rawGroup.key(),
                    rawGroup.name(),
                    rawGroup.englishName(),
                    rawGroup.summary(),
                    rawGroup.englishSummary(),
                    rawGroup.where(),
                    rawGroup.englishWhere(),
                    rawGroup.how(),
                    rawGroup.englishHow(),
                    rawGroup.notes(),
                    rawGroup.englishNotes(),
                    rawGroup.links().stream()
                            .filter(link -> !link.label().isBlank() && !link.url().isBlank())
                            .map(link -> new SourceLink(link.label(), link.url()))
                            .toList());
            groupsByKey.put(group.key(), group);

            for (String configuredMaterial : rawGroup.materials()) {
                String actualMaterial = eligibleBySourceKey.get(sourceKey(configuredMaterial));
                if (actualMaterial == null) {
                    log.warn("Ignoring source material '{}' in group '{}' because no matching catalog material exists.",
                            configuredMaterial,
                            rawGroup.key());
                    continue;
                }

                if (excludedMaterials.contains(actualMaterial)) {
                    continue;
                }

                if (materialsByName.containsKey(actualMaterial)) {
                    throw new IllegalStateException("Duplicate source material mapping for " + actualMaterial);
                }

                SourceMaterial material = new SourceMaterial(actualMaterial, group.key(), "", "");
                materialsByName.put(actualMaterial, material);
                materialsBySourceKey.put(sourceKey(actualMaterial), actualMaterial);
            }
        }

        for (JsonSourceCatalogStore.SourceItemNotePayload rawNote : payload.itemNotes()) {
            String actualMaterial = eligibleBySourceKey.get(sourceKey(rawNote.name()));
            if (actualMaterial == null || excludedMaterials.contains(actualMaterial)) {
                log.warn("Ignoring source note '{}' because no matching material exists.", rawNote.name());
                continue;
            }

            SourceMaterial existing = materialsByName.get(actualMaterial);
            if (existing == null) {
                log.warn("Ignoring source note '{}' because no source group mapping exists.", rawNote.name());
                continue;
            }

            materialsByName.put(actualMaterial, existing.withNotes(rawNote.note(), rawNote.englishNote()));
        }

        Set<String> uncoveredMaterials = new LinkedHashSet<>(eligibleMaterials);
        uncoveredMaterials.removeAll(excludedMaterials);
        uncoveredMaterials.removeAll(materialsByName.keySet());
        if (!uncoveredMaterials.isEmpty()) {
            throw new IllegalStateException("Missing source entries for materials: " + String.join(", ", uncoveredMaterials));
        }

        catalog = new SourceCatalog(
                groupsByKey,
                materialsByName,
                materialsBySourceKey,
                excludedMaterials);
        log.info("Loaded source data: {} source materials from {}",
                materialsByName.size(),
                sourcePath.toAbsolutePath());
    }

    public Path resolveSourcePath() {
        Path configuredPath = Paths.get(sourcesPath);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        return configuredPath.toAbsolutePath().normalize();
    }

    public List<SourceMaterialSummary> materials(String language) {
        return catalog.materialsByName().values().stream()
                .map(material -> toSummary(material, language))
                .sorted(Comparator.comparing(SourceMaterialSummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Optional<SourceMaterialDetail> detail(String materialName, String language) {
        String resolvedName = catalog.materialsBySourceKey().get(sourceKey(materialName));
        if (resolvedName == null) {
            return Optional.empty();
        }

        SourceMaterial material = catalog.materialsByName().get(resolvedName);
        if (material == null) {
            return Optional.empty();
        }

        SourceGroup group = catalog.groupsByKey().get(material.groupKey());
        if (group == null) {
            return Optional.empty();
        }

        return Optional.of(new SourceMaterialDetail(
                material.key(),
                localizeTerm(material.key(), language),
                group.key(),
                localized(group.name(), group.englishName(), language),
                localized(group.summary(), group.englishSummary(), language),
                localized(group.where(), group.englishWhere(), language),
                localized(group.how(), group.englishHow(), language),
                localized(group.notes(), group.englishNotes(), language),
                localized(material.note(), material.englishNote(), language),
                group.links().stream()
                        .map(link -> new SourceLink(link.label(), link.url()))
                        .toList()));
    }

    public int materialCount() {
        return catalog.materialsByName().size();
    }

    private SourceMaterialSummary toSummary(SourceMaterial material, String language) {
        SourceGroup group = catalog.groupsByKey().get(material.groupKey());
        return new SourceMaterialSummary(
                material.key(),
                localizeTerm(material.key(), language),
                material.groupKey(),
                group == null ? material.groupKey() : localized(group.name(), group.englishName(), language),
                !localized(material.note(), material.englishNote(), language).isBlank());
    }

    private Set<String> eligibleMaterialNames() {
        Set<String> cookingOutputs = recipeCatalogService.definitions().stream()
                .map(RecipeDefinition::name)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Set<String> refineryOutputs = refineryCatalogService.definitions().stream()
                .map(definition -> definition.name())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        Set<String> usedMaterials = new LinkedHashSet<>();
        for (RecipeDefinition definition : recipeCatalogService.definitions()) {
            for (RecipeVariant variant : definition.variants()) {
                for (IngredientSlot slot : variant.slots()) {
                    usedMaterials.addAll(slot.options());
                }
            }
        }

        refineryCatalogService.definitions().forEach(definition ->
                definition.variants().forEach(variant ->
                        variant.ingredients().forEach(ingredient -> usedMaterials.add(ingredient.name()))));

        Set<String> results = new LinkedHashSet<>();
        for (String material : usedMaterials) {
            String canonicalMaterial = canonicalNameOrSelf(material);
            if (cookingOutputs.contains(canonicalMaterial) || refineryOutputs.contains(canonicalMaterial)) {
                continue;
            }

            results.add(canonicalMaterial);
        }

        return results;
    }

    private String canonicalNameOrSelf(String name) {
        String refineryCanonical = refineryCatalogService.canonicalNameOrSelf(name);
        return recipeCatalogService.canonicalNameOrSelf(refineryCanonical);
    }

    private String localizeTerm(String term, String language) {
        return localizationService.localizeTerm(
                term,
                language,
                recipeCatalogService.findEnglishTermName(term)
                        .or(() -> refineryCatalogService.findEnglishTermName(term))
                        .orElse(null));
    }

    private String localized(String germanText, String englishText, String language) {
        String german = NameNormalizer.display(germanText);
        if (!localizationService.isEnglish(language)) {
            return german;
        }

        String english = NameNormalizer.display(englishText);
        return english.isBlank() ? german : english;
    }

    private String sourceKey(String value) {
        String germanNormalized = NameNormalizer.display(value)
                .replace("Ä", "Ae")
                .replace("Ö", "Oe")
                .replace("Ü", "Ue")
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss");
        String normalized = Normalizer.normalize(germanNormalized, Normalizer.Form.NFKD)
                .replace('\u00A0', ' ')
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\"' ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private record SourceCatalog(Map<String, SourceGroup> groupsByKey,
                                 Map<String, SourceMaterial> materialsByName,
                                 Map<String, String> materialsBySourceKey,
                                 Set<String> excludedMaterials) {
    }

    private record SourceGroup(String key,
                               String name,
                               String englishName,
                               String summary,
                               String englishSummary,
                               String where,
                               String englishWhere,
                               String how,
                               String englishHow,
                               String notes,
                               String englishNotes,
                               List<SourceLink> links) {
    }

    private record SourceMaterial(String key, String groupKey, String note, String englishNote) {
        private SourceMaterial withNotes(String updatedNote, String updatedEnglishNote) {
            return new SourceMaterial(key, groupKey, updatedNote, updatedEnglishNote);
        }
    }

    public record SourceLink(String label, String url) {
    }

    public record SourceMaterialSummary(String key,
                                        String name,
                                        String groupKey,
                                        String groupName,
                                        boolean hasItemNote) {
    }

    public record SourceMaterialDetail(String key,
                                       String name,
                                       String groupKey,
                                       String groupName,
                                       String summary,
                                       String where,
                                       String how,
                                       String notes,
                                       String itemNote,
                                       List<SourceLink> links) {
    }
}
