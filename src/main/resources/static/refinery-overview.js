const languageSelect = document.getElementById("languageSelect");
const ingredientFilterInput = document.getElementById("ingredientFilter");
const overviewStatus = document.getElementById("overviewStatus");
const overviewTableBody = document.getElementById("overviewTableBody");
const sortByNameButton = document.getElementById("sortByNameButton");

const DEFAULT_LANGUAGE = "en";
const SUPPORTED_LANGUAGES = new Set(["de", "en"]);

const UI_TEXT = {
    de: {
        documentTitle: "Raffinerie Übersicht",
        pageTitle: "Raffinerie Übersicht",
        intro: "Diese Tabelle zeigt jede Raffinerie-Variante mit Zielprodukt, den drei obersten Zutaten und dem gespeicherten Preis.",
        hint: "Nach beliebigen Zutaten filtern und direkt nach Zielprodukt oder Preis sortieren.",
        ingredientFilterLabel: "Nach Zutat filtern",
        ingredientFilterPlaceholder: "Zutat eingeben",
        targetProduct: "Zielprodukt",
        ingredient1: "Zutat 1",
        ingredient2: "Zutat 2",
        ingredient3: "Zutat 3",
        loadingStatus: "Lade Raffinerie-Übersicht...",
        loadedStatus: "{visibleCount} von {totalCount} Raffinerie-Varianten sichtbar.",
        noRecipes: "Keine Raffinerie-Varianten gefunden.",
        noMatches: "Keine passenden Raffinerie-Varianten gefunden.",
        loadError: "Die Raffinerie-Übersicht konnte nicht geladen werden.",
        emptyValue: "-",
        sortByTargetTitle: "Nach Zielprodukt sortieren"
    },
    en: {
        documentTitle: "Refinery Overview",
        pageTitle: "Refinery Overview",
        intro: "This table lists every refinery process variant with its target product, the three top-level inputs and the stored price.",
        hint: "Filter by any input name, then sort directly by target product or price.",
        ingredientFilterLabel: "Filter by ingredient",
        ingredientFilterPlaceholder: "Enter an ingredient name",
        targetProduct: "Target Product",
        ingredient1: "Ingredient 1",
        ingredient2: "Ingredient 2",
        ingredient3: "Ingredient 3",
        loadingStatus: "Loading refinery overview...",
        loadedStatus: "Showing {visibleCount} of {totalCount} refinery variants.",
        noRecipes: "No refinery variants found.",
        noMatches: "No matching refinery variants found.",
        loadError: "The refinery overview could not be loaded.",
        emptyValue: "-",
        sortByTargetTitle: "Sort by target product"
    }
};

let currentLanguage = resolveInitialLanguage();
let allRows = [];
let loadToken = 0;

function resolveInitialLanguage() {
    const urlLanguage = normalizeLanguage(new URLSearchParams(window.location.search).get("lang"));
    if (urlLanguage) {
        return urlLanguage;
    }

    return DEFAULT_LANGUAGE;
}

function normalizeLanguage(language) {
    if (!language) {
        return null;
    }

    const normalized = String(language).trim().toLowerCase();
    return SUPPORTED_LANGUAGES.has(normalized) ? normalized : null;
}

function locale() {
    return currentLanguage === "en" ? "en" : "de";
}

function t(key, values = {}) {
    const template = UI_TEXT[currentLanguage]?.[key] ?? UI_TEXT[DEFAULT_LANGUAGE][key] ?? key;
    return template.replace(/\{(\w+)}/g, (_, token) => String(values[token] ?? ""));
}

function buildApiUrl(path, params = {}) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && String(value) !== "") {
            searchParams.set(key, value);
        }
    });
    searchParams.set("lang", currentLanguage);

    const query = searchParams.toString();
    return query ? `${path}?${query}` : path;
}

function replaceLanguageInUrl(language) {
    const url = new URL(window.location.href);
    url.searchParams.set("lang", language);
    window.history.replaceState({}, "", `${url.pathname}?${url.searchParams.toString()}`);
}

function normalizeSearchText(value) {
    return String(value ?? "")
        .normalize("NFKD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/\u00A0/g, " ")
        .replace(/ß/g, "ss")
        .toLocaleLowerCase(locale())
        .trim();
}

function filteredRows() {
    const needle = normalizeSearchText(ingredientFilterInput.value);
    const visibleRows = allRows.filter(row => {
        if (!needle) {
            return true;
        }

        return [row.name, row.ingredient1, row.ingredient2, row.ingredient3]
            .some(value => normalizeSearchText(value).includes(needle));
    });

    return [...visibleRows].sort((left, right) => {
        const collator = new Intl.Collator(locale(), { sensitivity: "base", numeric: true });
        const nameResult = collator.compare(left.name || "", right.name || "");
        if (nameResult !== 0) {
            return nameResult;
        }

        return left.variantIndex - right.variantIndex;
    });
}

function setStatus(message, tone = "") {
    overviewStatus.textContent = message;
    overviewStatus.className = "status";
    if (tone) {
        overviewStatus.classList.add(`is-${tone}`);
    }
}

function updateSortButtons() {
    sortByNameButton.textContent = `${t("targetProduct")} ↑`;
    sortByNameButton.title = t("sortByTargetTitle");
    sortByNameButton.setAttribute("aria-pressed", "true");
}

function renderRows() {
    updateSortButtons();
    overviewTableBody.innerHTML = "";

    if (!allRows.length) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 4;
        cell.className = "overview-empty";
        cell.textContent = t("noRecipes");
        row.appendChild(cell);
        overviewTableBody.appendChild(row);
        setStatus(t("noRecipes"));
        return;
    }

    const visibleRows = filteredRows();
    if (!visibleRows.length) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 4;
        cell.className = "overview-empty";
        cell.textContent = t("noMatches");
        row.appendChild(cell);
        overviewTableBody.appendChild(row);
        setStatus(t("noMatches"));
        return;
    }

    visibleRows.forEach(rowData => {
        const row = document.createElement("tr");
        appendTextCell(row, rowData.name);
        appendTextCell(row, rowData.ingredient1 || t("emptyValue"));
        appendTextCell(row, rowData.ingredient2 || t("emptyValue"));
        appendTextCell(row, rowData.ingredient3 || t("emptyValue"));
        overviewTableBody.appendChild(row);
    });

    setStatus(t("loadedStatus", {
        visibleCount: visibleRows.length,
        totalCount: allRows.length
    }));
}

function appendTextCell(row, text, className = "") {
    const cell = document.createElement("td");
    cell.textContent = text;
    if (className) {
        cell.classList.add(className);
    }
    row.appendChild(cell);
}

function applyLanguage(language) {
    currentLanguage = normalizeLanguage(language) ?? DEFAULT_LANGUAGE;
    if (languageSelect) {
        languageSelect.value = currentLanguage;
    }

    document.documentElement.lang = currentLanguage;
    document.title = t("documentTitle");
    document.getElementById("pageTitle").textContent = t("pageTitle");
    document.getElementById("pageIntro").textContent = t("intro");
    document.getElementById("pageHint").textContent = t("hint");
    document.getElementById("ingredientFilterLabel").textContent = t("ingredientFilterLabel");
    ingredientFilterInput.placeholder = t("ingredientFilterPlaceholder");
    document.getElementById("ingredient1Header").textContent = t("ingredient1");
    document.getElementById("ingredient2Header").textContent = t("ingredient2");
    document.getElementById("ingredient3Header").textContent = t("ingredient3");
    window.NmsMainMenu?.update(currentLanguage);
    replaceLanguageInUrl(currentLanguage);
    renderRows();
}

async function loadOverview() {
    const requestToken = ++loadToken;
    setStatus(t("loadingStatus"));

    try {
        const response = await fetch(buildApiUrl("/api/refinery/overview"), { cache: "no-store" });
        const payload = await response.json().catch(() => []);
        if (!response.ok) {
            throw new Error(payload.message || t("loadError"));
        }

        if (requestToken !== loadToken) {
            return;
        }

        allRows = Array.isArray(payload) ? payload : [];
        renderRows();
    } catch (error) {
        if (requestToken !== loadToken) {
            return;
        }

        allRows = [];
        overviewTableBody.innerHTML = "";
        setStatus(error.message || t("loadError"), "error");
    }
}

ingredientFilterInput.addEventListener("input", () => {
    renderRows();
});

sortByNameButton.addEventListener("click", () => {
    renderRows();
});

if (languageSelect) {
    languageSelect.addEventListener("change", async () => {
        applyLanguage(languageSelect.value);
        await loadOverview();
    });
}

applyLanguage(currentLanguage);
loadOverview();
