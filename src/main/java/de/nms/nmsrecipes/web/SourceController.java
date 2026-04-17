package de.nms.nmsrecipes.web;

import de.nms.nmsrecipes.service.LocalizationService;
import de.nms.nmsrecipes.service.SourceCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final SourceCatalogService sourceCatalogService;
    private final LocalizationService localizationService;

    public SourceController(SourceCatalogService sourceCatalogService,
                            LocalizationService localizationService) {
        this.sourceCatalogService = sourceCatalogService;
        this.localizationService = localizationService;
    }

    @GetMapping("/materials")
    public List<SourceCatalogService.SourceMaterialSummary> materials(
            @RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        return sourceCatalogService.materials(normalizedLanguage);
    }

    @GetMapping("/details")
    public SourceCatalogService.SourceMaterialDetail details(@RequestParam("material") String material,
                                                            @RequestParam(value = "lang", required = false) String language) {
        String normalizedLanguage = localizationService.normalizeLanguage(language);
        return sourceCatalogService.detail(material, normalizedLanguage)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        localizationService.sourceMaterialNotFoundMessage(normalizedLanguage)));
    }
}
