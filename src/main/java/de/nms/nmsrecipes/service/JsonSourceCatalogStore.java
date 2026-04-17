package de.nms.nmsrecipes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nms.nmsrecipes.model.NameNormalizer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonSourceCatalogStore {

    private final ObjectMapper objectMapper;

    public JsonSourceCatalogStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SourceCatalogPayload load(Path jsonFile) {
        if (!Files.exists(jsonFile)) {
            throw new IllegalStateException("Source catalog file not found: " + jsonFile.toAbsolutePath());
        }

        try {
            JsonNode root = objectMapper.readTree(jsonFile.toFile());
            if (root == null || !root.isObject()) {
                throw new IllegalStateException("Unsupported source catalog JSON structure: " + jsonFile.toAbsolutePath());
            }

            List<String> excluded = new ArrayList<>();
            JsonNode excludedNode = root.get("excluded");
            if (excludedNode != null && excludedNode.isArray()) {
                for (JsonNode item : excludedNode) {
                    String value = NameNormalizer.display(item == null ? "" : item.asText(""));
                    if (!value.isBlank()) {
                        excluded.add(value);
                    }
                }
            }

            List<SourceGroupPayload> groups = new ArrayList<>();
            JsonNode groupsNode = root.get("groups");
            if (groupsNode != null && groupsNode.isArray()) {
                for (JsonNode groupNode : groupsNode) {
                    if (groupNode == null || !groupNode.isObject()) {
                        continue;
                    }

                    List<SourceLinkPayload> links = new ArrayList<>();
                    JsonNode linksNode = groupNode.get("links");
                    if (linksNode != null && linksNode.isArray()) {
                        for (JsonNode linkNode : linksNode) {
                            if (linkNode == null || !linkNode.isObject()) {
                                continue;
                            }

                            links.add(new SourceLinkPayload(
                                    NameNormalizer.display(linkNode.path("label").asText("")),
                                    NameNormalizer.display(linkNode.path("url").asText(""))));
                        }
                    }

                    List<String> materials = new ArrayList<>();
                    JsonNode materialsNode = groupNode.get("materials");
                    if (materialsNode != null && materialsNode.isArray()) {
                        for (JsonNode materialNode : materialsNode) {
                            String value = NameNormalizer.display(materialNode == null ? "" : materialNode.asText(""));
                            if (!value.isBlank()) {
                                materials.add(value);
                            }
                        }
                    }

                    groups.add(new SourceGroupPayload(
                            NameNormalizer.display(groupNode.path("key").asText("")),
                            NameNormalizer.display(groupNode.path("name").asText("")),
                            NameNormalizer.display(groupNode.path("englishName").asText("")),
                            NameNormalizer.display(groupNode.path("summary").asText("")),
                            NameNormalizer.display(groupNode.path("englishSummary").asText("")),
                            NameNormalizer.display(groupNode.path("where").asText("")),
                            NameNormalizer.display(groupNode.path("englishWhere").asText("")),
                            NameNormalizer.display(groupNode.path("how").asText("")),
                            NameNormalizer.display(groupNode.path("englishHow").asText("")),
                            NameNormalizer.display(groupNode.path("notes").asText("")),
                            NameNormalizer.display(groupNode.path("englishNotes").asText("")),
                            links,
                            materials));
                }
            }

            List<SourceItemNotePayload> itemNotes = new ArrayList<>();
            JsonNode itemNotesNode = root.get("itemNotes");
            if (itemNotesNode != null && itemNotesNode.isArray()) {
                for (JsonNode noteNode : itemNotesNode) {
                    if (noteNode == null || !noteNode.isObject()) {
                        continue;
                    }

                    String name = NameNormalizer.display(noteNode.path("name").asText(""));
                    if (name.isBlank()) {
                        continue;
                    }

                    itemNotes.add(new SourceItemNotePayload(
                            name,
                            NameNormalizer.display(noteNode.path("note").asText("")),
                            NameNormalizer.display(noteNode.path("englishNote").asText(""))));
                }
            }

            return new SourceCatalogPayload(excluded, groups, itemNotes);
        } catch (IOException exception) {
            throw new IllegalStateException("Source catalog file could not be read: " + jsonFile.toAbsolutePath(), exception);
        }
    }

    public record SourceCatalogPayload(List<String> excluded,
                                       List<SourceGroupPayload> groups,
                                       List<SourceItemNotePayload> itemNotes) {
    }

    public record SourceGroupPayload(String key,
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
                                     List<SourceLinkPayload> links,
                                     List<String> materials) {
    }

    public record SourceLinkPayload(String label, String url) {
    }

    public record SourceItemNotePayload(String name, String note, String englishNote) {
    }
}
