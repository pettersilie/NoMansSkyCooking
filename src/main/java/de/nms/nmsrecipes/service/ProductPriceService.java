package de.nms.nmsrecipes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.nms.nmsrecipes.config.RecipeProperties;
import de.nms.nmsrecipes.model.NameNormalizer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductPriceService {

    private static final TypeReference<Map<String, String>> PRICE_MAP_TYPE = new TypeReference<>() {
    };

    private final RecipeProperties properties;
    private final RecipeCatalogService catalogService;
    private final ObjectMapper objectMapper;
    private final Map<String, BigDecimal> pricesByProduct = new LinkedHashMap<>();

    public ProductPriceService(RecipeProperties properties,
                               RecipeCatalogService catalogService,
                               ObjectMapper objectMapper) {
        this.properties = properties;
        this.catalogService = catalogService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        Path storagePath = resolveStoragePath();
        if (!Files.exists(storagePath)) {
            return;
        }

        try {
            Map<String, String> rawPrices = objectMapper.readValue(storagePath.toFile(), PRICE_MAP_TYPE);
            synchronized (pricesByProduct) {
                pricesByProduct.clear();
                for (Map.Entry<String, String> entry : rawPrices.entrySet()) {
                    String canonicalName = catalogService.canonicalNameOrSelf(entry.getKey());
                    pricesByProduct.put(canonicalName, parsePrice(entry.getValue()));
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Preisdatei konnte nicht geladen werden: " + storagePath.toAbsolutePath(), exception);
        }
    }

    public Optional<String> findDisplayPrice(String productName) {
        String canonicalName = catalogService.canonicalNameOrSelf(productName);
        synchronized (pricesByProduct) {
            return Optional.ofNullable(pricesByProduct.get(canonicalName))
                    .map(this::formatPrice);
        }
    }

    public Optional<String> savePrice(String productName, String rawPrice) {
        String canonicalName = catalogService.requireDefinition(productName).name();
        String normalizedValue = NameNormalizer.display(rawPrice);

        synchronized (pricesByProduct) {
            if (normalizedValue.isBlank()) {
                pricesByProduct.remove(canonicalName);
                persist();
                return Optional.empty();
            }

            BigDecimal parsedPrice = parsePrice(normalizedValue);
            pricesByProduct.put(canonicalName, parsedPrice);
            persist();
            return Optional.of(formatPrice(parsedPrice));
        }
    }

    Path resolveStoragePath() {
        Path configuredPath = Paths.get(properties.pricePath());
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        Path primaryPath = configuredPath.toAbsolutePath().normalize();
        if (Files.exists(primaryPath)) {
            return primaryPath;
        }

        Path fileName = configuredPath.getFileName();
        if (fileName != null) {
            Path fallbackPath = Paths.get(fileName.toString()).toAbsolutePath().normalize();
            if (Files.exists(fallbackPath)) {
                return fallbackPath;
            }
        }

        return primaryPath;
    }

    private void persist() {
        Path storagePath = resolveStoragePath();
        try {
            Path parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Map<String, String> rawPrices = new LinkedHashMap<>();
            pricesByProduct.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                    .forEach(entry -> rawPrices.put(entry.getKey(), entry.getValue().toPlainString()));

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), rawPrices);
        } catch (IOException exception) {
            throw new IllegalStateException("Preisdatei konnte nicht gespeichert werden: " + storagePath.toAbsolutePath(), exception);
        }
    }

    private BigDecimal parsePrice(String rawPrice) {
        String candidate = rawPrice
                .replace("EUR", "")
                .replace("\u20AC", "")
                .replace(" ", "")
                .trim();

        int commaIndex = candidate.lastIndexOf(',');
        int dotIndex = candidate.lastIndexOf('.');
        if (commaIndex > dotIndex) {
            candidate = candidate.replace(".", "").replace(',', '.');
        } else if (dotIndex > commaIndex && commaIndex >= 0) {
            candidate = candidate.replace(",", "");
        } else if (commaIndex >= 0) {
            candidate = candidate.replace(',', '.');
        }

        try {
            BigDecimal parsed = new BigDecimal(candidate);
            if (parsed.signum() < 0) {
                throw new IllegalArgumentException("Der Preis darf nicht negativ sein.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Ungueltiger Preis: " + rawPrice, exception);
        }
    }

    private String formatPrice(BigDecimal price) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.GERMAN);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(price);
    }
}
