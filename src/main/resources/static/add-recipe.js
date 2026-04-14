const recipeForm = document.getElementById("recipeForm");
const recipeBuilderRoot = document.getElementById("recipeBuilderRoot");
const saveRecipeButton = document.getElementById("saveRecipeButton");
const backLink = document.getElementById("backLink");
const builderStatus = document.getElementById("builderStatus");

const DEFAULT_LANGUAGE = "de";
const SUPPORTED_LANGUAGES = new Set(["de", "en"]);

const UI_TEXT = {
    de: {
        documentTitle: "Rezept hinzufügen",
        pageTitle: "Rezept hinzufügen",
        intro: "Lege hier ein neues Rezept mit deutschem und englischem Namen an. Pro Variante sind maximal drei Zutaten moeglich, und Zutaten koennen bestehend, neu oder selbst wieder Teilrezepte sein.",
        saveButton: "Speichern",
        backButton: "Zurück",
        loadingReferences: "Lade Kategorien und Zutaten...",
        loadError: "Die Referenzdaten konnten nicht geladen werden.",
        saving: "Speichere Rezept {germanName} / {englishName}...",
        saved: "Rezept gespeichert: {name}",
        saveError: "Das Rezept konnte nicht gespeichert werden.",
        rootTitle: "Neues Rezept",
        nestedTitle: "Teilrezept",
        rootNote: "Name in Deutsch und Englisch angeben und einer bestehenden Kategorie zuordnen.",
        nestedNote: "Dieses Teilrezept wird zusammen mit dem Hauptrezept gespeichert.",
        germanNameLabel: "Name Deutsch",
        germanNamePlaceholder: "Rezeptname auf Deutsch",
        englishNameLabel: "Name Englisch",
        englishNamePlaceholder: "Recipe name in English",
        categoryLabel: "Kategorie",
        categoryPlaceholder: "Kategorie auswählen",
        addVariantButton: "Variante hinzufügen",
        removeVariantButton: "Variante entfernen",
        variantTitle: "Variante {index}",
        variantNote: "Bis zu drei Zutaten pro Variante.",
        ingredient1: "Zutat 1",
        ingredient2: "Zutat 2",
        ingredient3: "Zutat 3",
        ingredientModeLabel: "Typ",
        ingredientModeEmpty: "Leer",
        ingredientModeExisting: "Bestehende Zutat",
        ingredientModeNewRaw: "Neue Zutat",
        ingredientModeNewRecipe: "Neues Teilrezept",
        existingIngredientLabel: "Vorhandene Zutat",
        existingIngredientPlaceholder: "Zutat auswählen",
        newIngredientGermanLabel: "Neue Zutat Deutsch",
        newIngredientGermanPlaceholder: "Name der neuen Zutat auf Deutsch",
        newIngredientEnglishLabel: "Neue Zutat Englisch",
        newIngredientEnglishPlaceholder: "Name der neuen Zutat auf Englisch",
        emptySlotNote: "Dieser Platz bleibt leer.",
        existingCatalogRecipe: "Rezept",
        existingCatalogIngredient: "Zutat"
    },
    en: {
        documentTitle: "Add Recipe",
        pageTitle: "Add Recipe",
        intro: "Create a new recipe with German and English names here. Each variant can contain at most three ingredients, and ingredients can be existing items, new raw ingredients, or nested sub-recipes.",
        saveButton: "Save",
        backButton: "Back",
        loadingReferences: "Loading categories and ingredients...",
        loadError: "The reference data could not be loaded.",
        saving: "Saving recipe {germanName} / {englishName}...",
        saved: "Saved recipe: {name}",
        saveError: "The recipe could not be saved.",
        rootTitle: "New recipe",
        nestedTitle: "Sub-recipe",
        rootNote: "Provide German and English names and assign the recipe to an existing category.",
        nestedNote: "This sub-recipe will be saved together with the main recipe.",
        germanNameLabel: "German name",
        germanNamePlaceholder: "Recipe name in German",
        englishNameLabel: "English name",
        englishNamePlaceholder: "Recipe name in English",
        categoryLabel: "Category",
        categoryPlaceholder: "Select a category",
        addVariantButton: "Add variant",
        removeVariantButton: "Remove variant",
        variantTitle: "Variant {index}",
        variantNote: "Up to three ingredients per variant.",
        ingredient1: "Ingredient 1",
        ingredient2: "Ingredient 2",
        ingredient3: "Ingredient 3",
        ingredientModeLabel: "Type",
        ingredientModeEmpty: "Empty",
        ingredientModeExisting: "Existing ingredient",
        ingredientModeNewRaw: "New ingredient",
        ingredientModeNewRecipe: "New sub-recipe",
        existingIngredientLabel: "Existing ingredient",
        existingIngredientPlaceholder: "Select an ingredient",
        newIngredientGermanLabel: "New ingredient German",
        newIngredientGermanPlaceholder: "German name of the new ingredient",
        newIngredientEnglishLabel: "New ingredient English",
        newIngredientEnglishPlaceholder: "English name of the new ingredient",
        emptySlotNote: "This slot stays empty.",
        existingCatalogRecipe: "Recipe",
        existingCatalogIngredient: "Ingredient"
    }
};

let currentLanguage = resolveInitialLanguage();
let categories = [];
let ingredientCatalog = [];
let nextId = 0;
let state = createRecipeState();

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

function buildReturnUrl(savedProductKey = "") {
    const rawReturn = new URLSearchParams(window.location.search).get("return");
    const fallbackUrl = new URL("/", window.location.origin);
    fallbackUrl.searchParams.set("lang", currentLanguage);
    if (savedProductKey) {
        fallbackUrl.searchParams.set("product", savedProductKey);
    }

    if (!rawReturn || !rawReturn.startsWith("/")) {
        return `${fallbackUrl.pathname}?${fallbackUrl.searchParams.toString()}`;
    }

    const returnUrl = new URL(rawReturn, window.location.origin);
    returnUrl.searchParams.set("lang", currentLanguage);
    if (savedProductKey) {
        returnUrl.searchParams.set("product", savedProductKey);
    }
    const search = returnUrl.searchParams.toString();
    return `${returnUrl.pathname}${search ? `?${search}` : ""}`;
}

function applyLanguage(language) {
    currentLanguage = normalizeLanguage(language) ?? DEFAULT_LANGUAGE;
    window.localStorage.setItem("nms-recipes-language", currentLanguage);
    document.documentElement.lang = currentLanguage;
    document.title = t("documentTitle");
    document.getElementById("pageTitle").textContent = t("pageTitle");
    document.getElementById("pageIntro").textContent = t("intro");
    saveRecipeButton.textContent = t("saveButton");
    backLink.textContent = t("backButton");
    backLink.href = buildReturnUrl();
}

function createRecipeState() {
    return {
        id: nextNodeId(),
        germanName: "",
        englishName: "",
        categoryKey: "",
        variants: [createVariantState()]
    };
}

function createVariantState() {
    return {
        id: nextNodeId(),
        slots: [1, 2, 3].map(createSlotState)
    };
}

function createSlotState(position) {
    return {
        id: nextNodeId(),
        position,
        mode: "empty",
        existingKey: "",
        germanName: "",
        englishName: "",
        recipe: null
    };
}

function nextNodeId() {
    nextId += 1;
    return `node-${nextId}`;
}

function setStatus(message, tone = "") {
    builderStatus.textContent = message;
    builderStatus.className = "status";
    if (tone) {
        builderStatus.classList.add(`is-${tone}`);
    }
}

function setSaving(isSaving) {
    saveRecipeButton.disabled = isSaving;
}

async function loadReferenceData() {
    const [categoriesResponse, ingredientsResponse] = await Promise.all([
        fetch(buildApiUrl("/api/categories")),
        fetch(buildApiUrl("/api/ingredients/catalog"))
    ]);

    if (!categoriesResponse.ok || !ingredientsResponse.ok) {
        throw new Error(t("loadError"));
    }

    categories = (await categoriesResponse.json())
        .sort((left, right) => String(left?.name || "").localeCompare(String(right?.name || ""), currentLanguage));
    ingredientCatalog = await ingredientsResponse.json();
}

function renderBuilder() {
    recipeBuilderRoot.innerHTML = "";
    recipeBuilderRoot.appendChild(createRecipeEditor(state, true));
    backLink.href = buildReturnUrl();
}

function createRecipeEditor(recipe, isRoot) {
    const section = document.createElement("section");
    section.className = `recipe-editor-card${isRoot ? " is-root" : " is-nested"}`;

    const header = document.createElement("div");
    header.className = "recipe-editor-header";

    const title = document.createElement("h2");
    title.className = "recipe-editor-title";
    title.textContent = isRoot ? t("rootTitle") : t("nestedTitle");
    header.appendChild(title);
    section.appendChild(header);

    const note = document.createElement("div");
    note.className = "recipe-editor-note";
    note.textContent = isRoot ? t("rootNote") : t("nestedNote");
    section.appendChild(note);

    const fields = document.createElement("div");
    fields.className = "recipe-field-grid";
    fields.appendChild(createTextField(
        `${recipe.id}-de`,
        t("germanNameLabel"),
        recipe.germanName,
        t("germanNamePlaceholder"),
        (value) => {
            recipe.germanName = value;
        }
    ));
    fields.appendChild(createTextField(
        `${recipe.id}-en`,
        t("englishNameLabel"),
        recipe.englishName,
        t("englishNamePlaceholder"),
        (value) => {
            recipe.englishName = value;
        }
    ));
    fields.appendChild(createCategoryField(recipe));
    section.appendChild(fields);

    const variantsSection = document.createElement("div");
    variantsSection.className = "recipe-variants";

    const variantsHeader = document.createElement("div");
    variantsHeader.className = "variant-head";

    const variantsNote = document.createElement("div");
    variantsNote.className = "variant-note";
    variantsNote.textContent = t("variantNote");
    variantsHeader.appendChild(variantsNote);

    const addVariantButton = document.createElement("button");
    addVariantButton.type = "button";
    addVariantButton.className = "ghost-button";
    addVariantButton.textContent = t("addVariantButton");
    addVariantButton.addEventListener("click", () => {
        recipe.variants.push(createVariantState());
        renderBuilder();
    });
    variantsHeader.appendChild(addVariantButton);
    variantsSection.appendChild(variantsHeader);

    const variantList = document.createElement("div");
    variantList.className = "variant-list";
    recipe.variants.forEach((variant, index) => {
        variantList.appendChild(createVariantEditor(recipe, variant, index + 1));
    });
    variantsSection.appendChild(variantList);
    section.appendChild(variantsSection);

    return section;
}

function createTextField(id, labelText, value, placeholder, onInput) {
    const wrapper = document.createElement("div");
    wrapper.className = "inline-control";

    const label = document.createElement("label");
    label.htmlFor = id;
    label.textContent = labelText;
    wrapper.appendChild(label);

    const input = document.createElement("input");
    input.id = id;
    input.type = "text";
    input.autocomplete = "off";
    input.value = value || "";
    input.placeholder = placeholder;
    input.addEventListener("input", () => {
        onInput(input.value);
    });
    wrapper.appendChild(input);

    return wrapper;
}

function createCategoryField(recipe) {
    const wrapper = document.createElement("div");
    wrapper.className = "inline-control";

    const label = document.createElement("label");
    label.htmlFor = `${recipe.id}-category`;
    label.textContent = t("categoryLabel");
    wrapper.appendChild(label);

    const select = document.createElement("select");
    select.id = `${recipe.id}-category`;
    select.appendChild(new Option(t("categoryPlaceholder"), ""));
    categories.forEach((category) => {
        select.appendChild(new Option(category.name, category.key));
    });
    select.value = recipe.categoryKey || "";
    select.addEventListener("change", () => {
        recipe.categoryKey = select.value;
    });
    wrapper.appendChild(select);

    return wrapper;
}

function createVariantEditor(recipe, variant, index) {
    const article = document.createElement("article");
    article.className = "variant-card recipe-editor-card";

    const header = document.createElement("div");
    header.className = "variant-head";

    const title = document.createElement("h3");
    title.className = "variant-title";
    title.textContent = t("variantTitle", { index });
    header.appendChild(title);

    if (recipe.variants.length > 1) {
        const removeButton = document.createElement("button");
        removeButton.type = "button";
        removeButton.className = "danger-button";
        removeButton.textContent = t("removeVariantButton");
        removeButton.addEventListener("click", () => {
            recipe.variants = recipe.variants.filter((entry) => entry.id !== variant.id);
            renderBuilder();
        });
        header.appendChild(removeButton);
    }

    article.appendChild(header);

    const slotList = document.createElement("div");
    slotList.className = "slot-list";
    variant.slots.forEach((slot) => {
        slotList.appendChild(createSlotEditor(slot));
    });
    article.appendChild(slotList);

    return article;
}

function createSlotEditor(slot) {
    const article = document.createElement("article");
    article.className = "slot-editor";

    const title = document.createElement("h4");
    title.className = "slot-title-inline";
    title.textContent = t(`ingredient${slot.position}`);
    article.appendChild(title);

    const modeField = document.createElement("div");
    modeField.className = "inline-control";
    const modeLabel = document.createElement("label");
    modeLabel.htmlFor = `${slot.id}-mode`;
    modeLabel.textContent = t("ingredientModeLabel");
    modeField.appendChild(modeLabel);

    const modeSelect = document.createElement("select");
    modeSelect.id = `${slot.id}-mode`;
    [
        { value: "empty", label: t("ingredientModeEmpty") },
        { value: "existing", label: t("ingredientModeExisting") },
        { value: "new_raw", label: t("ingredientModeNewRaw") },
        { value: "new_recipe", label: t("ingredientModeNewRecipe") }
    ].forEach((option) => {
        modeSelect.appendChild(new Option(option.label, option.value));
    });
    modeSelect.value = slot.mode;
    modeSelect.addEventListener("change", () => {
        slot.mode = modeSelect.value;
        if (slot.mode === "new_recipe" && !slot.recipe) {
            slot.recipe = createRecipeState();
        }
        if (slot.mode !== "new_recipe") {
            slot.recipe = null;
        }
        renderBuilder();
    });
    modeField.appendChild(modeSelect);
    article.appendChild(modeField);

    const body = document.createElement("div");
    body.className = "slot-body";
    if (slot.mode === "existing") {
        body.appendChild(createExistingIngredientField(slot));
    } else if (slot.mode === "new_raw") {
        body.appendChild(createNewIngredientFields(slot));
    } else if (slot.mode === "new_recipe") {
        if (!slot.recipe) {
            slot.recipe = createRecipeState();
        }
        body.appendChild(createRecipeEditor(slot.recipe, false));
    } else {
        const note = document.createElement("div");
        note.className = "empty-inline-note";
        note.textContent = t("emptySlotNote");
        body.appendChild(note);
    }
    article.appendChild(body);

    return article;
}

function createExistingIngredientField(slot) {
    const wrapper = document.createElement("div");
    wrapper.className = "slot-fields";

    const label = document.createElement("label");
    label.htmlFor = `${slot.id}-existing`;
    label.textContent = t("existingIngredientLabel");
    wrapper.appendChild(label);

    const select = document.createElement("select");
    select.id = `${slot.id}-existing`;
    select.appendChild(new Option(t("existingIngredientPlaceholder"), ""));
    ingredientCatalog.forEach((ingredient) => {
        const suffix = ingredient.craftable ? t("existingCatalogRecipe") : t("existingCatalogIngredient");
        select.appendChild(new Option(`${ingredient.name} (${suffix})`, ingredient.key));
    });
    select.value = slot.existingKey || "";
    select.addEventListener("change", () => {
        slot.existingKey = select.value;
    });
    wrapper.appendChild(select);

    return wrapper;
}

function createNewIngredientFields(slot) {
    const wrapper = document.createElement("div");
    wrapper.className = "slot-fields";
    wrapper.appendChild(createTextField(
        `${slot.id}-new-de`,
        t("newIngredientGermanLabel"),
        slot.germanName,
        t("newIngredientGermanPlaceholder"),
        (value) => {
            slot.germanName = value;
        }
    ));
    wrapper.appendChild(createTextField(
        `${slot.id}-new-en`,
        t("newIngredientEnglishLabel"),
        slot.englishName,
        t("newIngredientEnglishPlaceholder"),
        (value) => {
            slot.englishName = value;
        }
    ));
    return wrapper;
}

function normalizeName(value) {
    return (value || "").trim().replace(/\s+/g, " ");
}

function serializeRecipe(recipe) {
    return {
        germanName: normalizeName(recipe.germanName),
        englishName: normalizeName(recipe.englishName),
        categoryKey: recipe.categoryKey || "",
        variants: recipe.variants
            .map(serializeVariant)
            .filter((variant) => variant.ingredients.length > 0)
    };
}

function serializeVariant(variant) {
    return {
        ingredients: variant.slots
            .map(serializeSlot)
            .filter(Boolean)
    };
}

function serializeSlot(slot) {
    if (slot.mode === "empty") {
        return null;
    }

    if (slot.mode === "existing") {
        return {
            position: slot.position,
            type: "existing",
            existingKey: slot.existingKey || ""
        };
    }

    if (slot.mode === "new_raw") {
        return {
            position: slot.position,
            type: "new_raw",
            germanName: normalizeName(slot.germanName),
            englishName: normalizeName(slot.englishName)
        };
    }

    return {
        position: slot.position,
        type: "new_recipe",
        recipe: slot.recipe ? serializeRecipe(slot.recipe) : null
    };
}

async function saveRecipe(event) {
    event.preventDefault();

    const payload = serializeRecipe(state);
    setSaving(true);
    setStatus(t("saving", {
        germanName: payload.germanName || "-",
        englishName: payload.englishName || "-"
    }));

    try {
        const response = await fetch(buildApiUrl("/api/recipes"), {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const body = await response.json().catch(() => ({ message: t("saveError") }));
        if (!response.ok) {
            throw new Error(body.message || t("saveError"));
        }

        await loadReferenceData();
        state = createRecipeState();
        renderBuilder();
        setStatus(t("saved", { name: body.name || body.key || payload.germanName }), "success");
        backLink.href = buildReturnUrl(body.key || "");
    } catch (error) {
        setStatus(error.message || t("saveError"), "error");
    } finally {
        setSaving(false);
    }
}

async function initialize() {
    applyLanguage(currentLanguage);
    setStatus(t("loadingReferences"));
    try {
        await loadReferenceData();
        renderBuilder();
        setStatus("");
    } catch (error) {
        setStatus(error.message || t("loadError"), "error");
    }
}

recipeForm.addEventListener("submit", saveRecipe);

initialize();
