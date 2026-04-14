const categoryForm = document.getElementById("categoryForm");
const categoryNameDeInput = document.getElementById("categoryNameDe");
const categoryNameEnInput = document.getElementById("categoryNameEn");
const saveCategoryButton = document.getElementById("saveCategoryButton");
const backLink = document.getElementById("backLink");
const categoryStatus = document.getElementById("categoryStatus");

const DEFAULT_LANGUAGE = "de";
const SUPPORTED_LANGUAGES = new Set(["de", "en"]);

const UI_TEXT = {
    de: {
        documentTitle: "Kategorie hinzufügen",
        pageTitle: "Kategorie hinzufügen",
        intro: "Lege hier eine neue Kategorie an. Sie wird direkt in der Rezeptdatei gespeichert und steht danach in der Anwendung zur Verfügung.",
        categoryNameDeLabel: "Kategoriename Deutsch",
        categoryNameDePlaceholder: "Neue Kategorie auf Deutsch eingeben",
        categoryNameEnLabel: "Kategoriename Englisch",
        categoryNameEnPlaceholder: "Neue Kategorie auf Englisch eingeben",
        saveButton: "Speichern",
        backButton: "Zurück",
        germanNameRequired: "Bitte einen deutschen Kategorienamen eingeben.",
        englishNameRequired: "Bitte einen englischen Kategorienamen eingeben.",
        saving: "Speichere Kategorie {germanName} / {englishName}...",
        saved: "Kategorie gespeichert: {germanName} / {englishName}",
        saveError: "Kategorie konnte nicht gespeichert werden."
    },
    en: {
        documentTitle: "Add Category",
        pageTitle: "Add Category",
        intro: "Create a new category here. It is stored directly in the recipe file and then becomes available in the application.",
        categoryNameDeLabel: "Category name German",
        categoryNameDePlaceholder: "Enter the German category name",
        categoryNameEnLabel: "Category name English",
        categoryNameEnPlaceholder: "Enter the English category name",
        saveButton: "Save",
        backButton: "Back",
        germanNameRequired: "Please enter a German category name.",
        englishNameRequired: "Please enter an English category name.",
        saving: "Saving category {germanName} / {englishName}...",
        saved: "Saved category: {germanName} / {englishName}",
        saveError: "The category could not be saved."
    }
};

let currentLanguage = resolveInitialLanguage();

function resolveInitialLanguage() {
    const urlLanguage = normalizeLanguage(new URLSearchParams(window.location.search).get("lang"));
    if (urlLanguage) {
        return urlLanguage;
    }

    const storedLanguage = normalizeLanguage(window.localStorage.getItem("nms-recipes-language"));
    return storedLanguage ?? DEFAULT_LANGUAGE;
}

function normalizeLanguage(language) {
    if (!language) {
        return null;
    }

    const normalized = String(language).trim().toLowerCase();
    return SUPPORTED_LANGUAGES.has(normalized) ? normalized : null;
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

function applyLanguage(language) {
    currentLanguage = normalizeLanguage(language) ?? DEFAULT_LANGUAGE;
    window.localStorage.setItem("nms-recipes-language", currentLanguage);
    document.documentElement.lang = currentLanguage;
    document.title = t("documentTitle");
    document.getElementById("pageTitle").textContent = t("pageTitle");
    document.getElementById("pageIntro").textContent = t("intro");
    document.getElementById("categoryNameDeLabel").textContent = t("categoryNameDeLabel");
    document.getElementById("categoryNameEnLabel").textContent = t("categoryNameEnLabel");
    categoryNameDeInput.placeholder = t("categoryNameDePlaceholder");
    categoryNameEnInput.placeholder = t("categoryNameEnPlaceholder");
    saveCategoryButton.textContent = t("saveButton");
    backLink.textContent = t("backButton");
    backLink.href = buildReturnUrl();
}

function buildReturnUrl() {
    const rawReturn = new URLSearchParams(window.location.search).get("return");
    const fallbackUrl = new URL("/", window.location.origin);
    fallbackUrl.searchParams.set("lang", currentLanguage);

    if (!rawReturn || !rawReturn.startsWith("/")) {
        return `${fallbackUrl.pathname}?${fallbackUrl.searchParams.toString()}`;
    }

    const returnUrl = new URL(rawReturn, window.location.origin);
    returnUrl.searchParams.set("lang", currentLanguage);
    const search = returnUrl.searchParams.toString();
    return `${returnUrl.pathname}${search ? `?${search}` : ""}`;
}

function normalizeCategoryName(value) {
    return (value || "").trim().replace(/\s+/g, " ");
}

function setStatus(message, tone = "") {
    categoryStatus.textContent = message;
    categoryStatus.className = "status";
    if (tone) {
        categoryStatus.classList.add(`is-${tone}`);
    }
}

function setSaving(isSaving) {
    saveCategoryButton.disabled = isSaving;
    categoryNameDeInput.disabled = isSaving;
    categoryNameEnInput.disabled = isSaving;
}

async function saveCategory(event) {
    event.preventDefault();

    const germanName = normalizeCategoryName(categoryNameDeInput.value);
    const englishName = normalizeCategoryName(categoryNameEnInput.value);
    if (!germanName) {
        setStatus(t("germanNameRequired"), "error");
        categoryNameDeInput.focus();
        return;
    }

    if (!englishName) {
        setStatus(t("englishNameRequired"), "error");
        categoryNameEnInput.focus();
        return;
    }

    setSaving(true);
    setStatus(t("saving", { germanName, englishName }));

    try {
        const response = await fetch(buildApiUrl("/api/categories"), {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ germanName, englishName })
        });

        const payload = await response.json().catch(() => ({ message: t("saveError") }));
        if (!response.ok) {
            throw new Error(payload.message || t("saveError"));
        }

        categoryNameDeInput.value = "";
        categoryNameEnInput.value = "";
        setStatus(t("saved", {
            germanName,
            englishName: payload.englishName || englishName
        }), "success");
        categoryNameDeInput.focus();
    } catch (error) {
        setStatus(error.message || t("saveError"), "error");
    } finally {
        setSaving(false);
    }
}

categoryForm.addEventListener("submit", saveCategory);

applyLanguage(currentLanguage);
categoryNameDeInput.focus();
