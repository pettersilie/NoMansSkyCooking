const languageSelect = document.getElementById("languageSelect");
const materialFilterInput = document.getElementById("materialFilter");
const sourcesStatus = document.getElementById("sourcesStatus");
const sourceMaterialList = document.getElementById("sourceMaterialList");
const sourceEmptyState = document.getElementById("sourceEmptyState");
const sourceDetailCard = document.getElementById("sourceDetailCard");
const sourceIdentity = document.getElementById("sourceIdentity");
const sourceGroupBadge = document.getElementById("sourceGroupBadge");
const sourceSummary = document.getElementById("sourceSummary");
const sourceWhere = document.getElementById("sourceWhere");
const sourceHow = document.getElementById("sourceHow");
const sourceNotes = document.getElementById("sourceNotes");
const itemHintBlock = document.getElementById("itemHintBlock");
const sourceItemHint = document.getElementById("sourceItemHint");
const sourceLinks = document.getElementById("sourceLinks");

const ICON_MANIFEST_URL = "/icons/manifest.json?v=20260415e";
const DEFAULT_ICON_PATH = "/icons/fallback.svg";
const DEFAULT_LANGUAGE = "en";
const SUPPORTED_LANGUAGES = new Set(["de", "en"]);

const UI_TEXT = {
    de: {
        documentTitle: "Quellen",
        pageTitle: "Quellen",
        intro: "Waehle einen Basiseingang aus, der in den aktuellen Koch- und Raffineriekatalogen nicht selbst hergestellt wird, um zu sehen, wie du ihn ueblicherweise bekommst.",
        hint: "Mit dem Suchfeld grenzt du die Liste ein. Die Detailansicht zeigt dir anschliessend Fundorte, Vorgehensweise, Zusatzhinweise und die verwendeten Community-Quellen.",
        materialFilterLabel: "Rohstoff filtern",
        materialFilterPlaceholder: "Material eingeben",
        loadingMaterials: "Lade Quellenmaterialien...",
        loadedStatus: "{visibleCount} von {totalCount} Quellenmaterialien sichtbar.",
        noMaterials: "Keine Quellenmaterialien gefunden.",
        noMatches: "Keine passenden Materialien gefunden.",
        loadError: "Die Quellenliste konnte nicht geladen werden.",
        detailError: "Die Quelldetails konnten nicht geladen werden.",
        emptyState: "Waehle links ein Material aus, um dessen Bezugsquelle zu sehen.",
        whereTitle: "Wo",
        howTitle: "Wie",
        notesTitle: "Allgemeine Hinweise",
        itemHintTitle: "Hinweis zum Material",
        linksTitle: "Community-Quellen",
        openDetails: "Quelldetails fuer {name} oeffnen",
        hasTip: "Mit Hinweis",
        fallbackSummary: "Keine Zusammenfassung verfuegbar.",
        fallbackLinks: "Keine Quellenlinks hinterlegt."
    },
    en: {
        documentTitle: "Sources",
        pageTitle: "Sources",
        intro: "Select a base ingredient that is not produced by the current cooking and refinery catalogues to see how players usually obtain it.",
        hint: "Use the search field to narrow the list. The detail view then shows where to find it, how to obtain it, extra notes, and the community references used.",
        materialFilterLabel: "Filter materials",
        materialFilterPlaceholder: "Enter a material name",
        loadingMaterials: "Loading source materials...",
        loadedStatus: "Showing {visibleCount} of {totalCount} source materials.",
        noMaterials: "No source materials found.",
        noMatches: "No matching materials found.",
        loadError: "The source list could not be loaded.",
        detailError: "The source details could not be loaded.",
        emptyState: "Select a material on the left to view its source details.",
        whereTitle: "Where",
        howTitle: "How",
        notesTitle: "General Notes",
        itemHintTitle: "Material Note",
        linksTitle: "Community Sources",
        openDetails: "Open source details for {name}",
        hasTip: "Has note",
        fallbackSummary: "No summary is available.",
        fallbackLinks: "No source links are available."
    }
};

let currentLanguage = resolveInitialLanguage();
let iconManifest = {
    defaultIcon: DEFAULT_ICON_PATH,
    icons: {}
};
let allMaterials = [];
let selectedMaterialKey = "";
const detailCache = new Map();

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
    if (selectedMaterialKey) {
        url.searchParams.set("material", selectedMaterialKey);
    }
    window.history.replaceState({}, "", `${url.pathname}?${url.searchParams.toString()}`);
}

function replaceMaterialInUrl(materialKey) {
    const url = new URL(window.location.href);
    url.searchParams.set("lang", currentLanguage);
    if (materialKey) {
        url.searchParams.set("material", materialKey);
    } else {
        url.searchParams.delete("material");
    }
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

async function loadIconManifest() {
    try {
        const response = await fetch(ICON_MANIFEST_URL, { cache: "no-store" });
        if (!response.ok) {
            return;
        }

        const payload = await response.json();
        iconManifest = {
            defaultIcon: typeof payload.defaultIcon === "string" && payload.defaultIcon ? payload.defaultIcon : DEFAULT_ICON_PATH,
            icons: payload && typeof payload.icons === "object" && payload.icons ? payload.icons : {}
        };
    } catch (error) {
        iconManifest = {
            defaultIcon: DEFAULT_ICON_PATH,
            icons: {}
        };
    }
}

function normalizeLookupKey(value) {
    return String(value ?? "")
        .normalize("NFKC")
        .replace(/\u00A0/g, " ")
        .trim()
        .replace(/\s+/g, " ")
        .toLocaleLowerCase("de-DE")
        .replace(/ß/g, "ss");
}

function resolveIconPath(value) {
    const lookupKey = normalizeLookupKey(value);
    return iconManifest.icons[lookupKey] || iconManifest.defaultIcon;
}

function createTermIcon(value, className) {
    const image = document.createElement("img");
    image.className = className;
    image.src = resolveIconPath(value);
    image.alt = "";
    image.loading = "lazy";
    image.decoding = "async";
    image.setAttribute("aria-hidden", "true");
    image.addEventListener("error", () => {
        if (image.dataset.fallbackApplied === "true") {
            return;
        }

        image.dataset.fallbackApplied = "true";
        image.src = iconManifest.defaultIcon || DEFAULT_ICON_PATH;
    });
    return image;
}

function applyLanguage(language) {
    currentLanguage = normalizeLanguage(language) ?? DEFAULT_LANGUAGE;
    languageSelect.value = currentLanguage;

    document.documentElement.lang = currentLanguage;
    document.title = t("documentTitle");
    document.getElementById("pageTitle").textContent = t("pageTitle");
    document.getElementById("pageIntro").textContent = t("intro");
    document.getElementById("pageHint").textContent = t("hint");
    document.getElementById("materialFilterLabel").textContent = t("materialFilterLabel");
    materialFilterInput.placeholder = t("materialFilterPlaceholder");
    sourceEmptyState.textContent = t("emptyState");
    document.getElementById("whereTitle").textContent = t("whereTitle");
    document.getElementById("howTitle").textContent = t("howTitle");
    document.getElementById("notesTitle").textContent = t("notesTitle");
    document.getElementById("itemHintTitle").textContent = t("itemHintTitle");
    document.getElementById("linksTitle").textContent = t("linksTitle");

    if (window.NmsMainMenu && typeof window.NmsMainMenu.update === "function") {
        window.NmsMainMenu.update(currentLanguage);
    }
}

function setStatus(message, tone = "") {
    sourcesStatus.textContent = message;
    sourcesStatus.className = "status";
    if (tone) {
        sourcesStatus.classList.add(`is-${tone}`);
    }
}

function filteredMaterials() {
    const needle = normalizeSearchText(materialFilterInput.value);
    if (!needle) {
        return [...allMaterials];
    }

    return allMaterials.filter(material =>
        normalizeSearchText(material.name).includes(needle)
        || normalizeSearchText(material.groupName).includes(needle));
}

function createListIdentity(titleText, metaText, iconKey) {
    const wrapper = document.createElement("div");
    wrapper.className = "list-entry-main";
    wrapper.appendChild(createTermIcon(iconKey, "term-icon list-term-icon"));

    const copy = document.createElement("div");
    copy.className = "list-copy";

    const title = document.createElement("span");
    title.className = "list-button-title";
    title.textContent = titleText;
    copy.appendChild(title);

    const meta = document.createElement("span");
    meta.className = "list-button-meta";
    meta.textContent = metaText;
    copy.appendChild(meta);

    wrapper.appendChild(copy);
    return wrapper;
}

function renderMaterialList() {
    sourceMaterialList.innerHTML = "";

    if (!allMaterials.length) {
        setStatus(t("noMaterials"));
        sourceMaterialList.innerHTML = `<div class="empty-state">${t("noMaterials")}</div>`;
        return;
    }

    const visibleMaterials = filteredMaterials();
    if (!visibleMaterials.length) {
        setStatus(t("noMatches"));
        sourceMaterialList.innerHTML = `<div class="empty-state">${t("noMatches")}</div>`;
        return;
    }

    visibleMaterials.forEach((material) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "list-button";
        if (material.key === selectedMaterialKey) {
            button.classList.add("is-selected");
        }
        button.title = t("openDetails", { name: material.name });
        button.appendChild(createListIdentity(material.name, material.groupName, material.key));

        if (material.hasItemNote) {
            const tip = document.createElement("span");
            tip.className = "match-chip";
            tip.textContent = t("hasTip");
            button.appendChild(tip);
        }

        button.addEventListener("click", () => {
            void selectMaterial(material.key);
        });
        sourceMaterialList.appendChild(button);
    });

    setStatus(t("loadedStatus", {
        visibleCount: visibleMaterials.length,
        totalCount: allMaterials.length
    }));
}

function renderDetail(detail) {
    sourceEmptyState.hidden = true;
    sourceDetailCard.hidden = false;

    sourceIdentity.innerHTML = "";
    sourceIdentity.appendChild(createListIdentity(detail.name, detail.groupName, detail.key));

    sourceGroupBadge.textContent = detail.groupName;
    sourceSummary.textContent = detail.summary || t("fallbackSummary");
    sourceWhere.textContent = detail.where || "";
    sourceHow.textContent = detail.how || "";
    sourceNotes.textContent = detail.notes || "";

    const itemNote = detail.itemNote || "";
    itemHintBlock.hidden = itemNote.trim() === "";
    sourceItemHint.textContent = itemNote;

    sourceLinks.innerHTML = "";
    if (Array.isArray(detail.links) && detail.links.length > 0) {
        detail.links.forEach((link) => {
            const item = document.createElement("li");
            const anchor = document.createElement("a");
            anchor.className = "source-link-anchor";
            anchor.href = link.url;
            anchor.target = "_blank";
            anchor.rel = "noreferrer noopener";
            anchor.textContent = link.label;
            item.appendChild(anchor);
            sourceLinks.appendChild(item);
        });
    } else {
        const item = document.createElement("li");
        item.className = "empty-state";
        item.textContent = t("fallbackLinks");
        sourceLinks.appendChild(item);
    }
}

async function loadMaterials(preferredMaterialKey = "") {
    setStatus(t("loadingMaterials"));

    try {
        const response = await fetch(buildApiUrl("/api/sources/materials"), { cache: "no-store" });
        if (!response.ok) {
            throw new Error(`Unexpected response: ${response.status}`);
        }

        const payload = await response.json();
        allMaterials = Array.isArray(payload) ? payload : [];
        renderMaterialList();

        const requestedMaterial = preferredMaterialKey
            || new URLSearchParams(window.location.search).get("material")
            || selectedMaterialKey
            || allMaterials[0]?.key
            || "";
        if (requestedMaterial) {
            await selectMaterial(requestedMaterial, { updateUrl: false, forceReload: true });
        } else {
            sourceEmptyState.hidden = false;
            sourceDetailCard.hidden = true;
        }
    } catch (error) {
        allMaterials = [];
        renderMaterialList();
        setStatus(t("loadError"), "error");
    }
}

async function selectMaterial(materialKey, options = {}) {
    const { updateUrl = true, forceReload = false } = options;
    if (!materialKey) {
        return;
    }

    const matchingMaterial = allMaterials.find(material => material.key === materialKey);
    if (!matchingMaterial) {
        return;
    }

    selectedMaterialKey = matchingMaterial.key;
    renderMaterialList();
    if (updateUrl) {
        replaceMaterialInUrl(selectedMaterialKey);
    }

    if (!forceReload && detailCache.has(selectedMaterialKey)) {
        renderDetail(detailCache.get(selectedMaterialKey));
        return;
    }

    try {
        const response = await fetch(buildApiUrl("/api/sources/details", { material: selectedMaterialKey }), { cache: "no-store" });
        if (!response.ok) {
            throw new Error(`Unexpected response: ${response.status}`);
        }

        const payload = await response.json();
        detailCache.set(selectedMaterialKey, payload);
        renderDetail(payload);
    } catch (error) {
        sourceEmptyState.hidden = false;
        sourceDetailCard.hidden = true;
        setStatus(t("detailError"), "error");
    }
}

async function reloadForLanguage(language) {
    applyLanguage(language);
    detailCache.clear();
    replaceLanguageInUrl(currentLanguage);
    await loadMaterials(selectedMaterialKey);
}

async function init() {
    applyLanguage(currentLanguage);
    await loadIconManifest();
    materialFilterInput.addEventListener("input", () => {
        renderMaterialList();
    });
    languageSelect.addEventListener("change", (event) => {
        void reloadForLanguage(event.target.value);
    });

    await loadMaterials();
}

void init();
