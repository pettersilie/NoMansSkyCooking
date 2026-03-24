const languageSelect = document.getElementById("languageSelect");
const productFilterLabel = document.getElementById("productFilterLabel");
const productFilterInput = document.getElementById("productFilter");
const productSortLabel = document.getElementById("productSortLabel");
const productSortSelect = document.getElementById("productSort");
const productCategoryFilterLabel = document.getElementById("productCategoryFilterLabel");
const productCategoryFilter = document.getElementById("productCategoryFilter");
const productList = document.getElementById("productList");
const ingredientFilterLabel = document.getElementById("ingredientFilterLabel");
const ingredientFilterInput = document.getElementById("ingredientFilter");
const ingredientResults = document.getElementById("ingredientResults");
const statusElement = document.getElementById("status");
const treeRoot = document.getElementById("treeRoot");
const canvasElement = document.querySelector(".canvas");

const NO_CATEGORY_FILTER_VALUE = "__NO_CATEGORY__";
const DEFAULT_LANGUAGE = "en";
const SUPPORTED_LANGUAGES = new Set(["de", "en"]);

const UI_TEXT = {
    de: {
        documentTitle: "NMS Rezeptgraph",
        appTitle: "Rezeptgraph",
        intro: "Diese Ansicht zeigt Produkte von oben nach unten. Unterprodukte werden rekursiv weiter aufgeloest, und Zutaten werden pro Rezept unter den Spalten Zutat 1, Zutat 2 und Zutat 3 gruppiert. Produktkarten lassen sich direkt erneut als Wurzel laden.",
        navigationHint: "Im Grafikbereich kann mit gedrueckter rechter Maustaste horizontal und vertikal navigiert werden.",
        languageLabel: "Sprache",
        productFilterLabel: "Produktliste filtern",
        productFilterPlaceholder: "Produktname eingeben",
        productSortLabel: "Sortierung",
        sortCategory: "Kategorie",
        sortName: "Name",
        sortPrice: "Preis",
        productCategoryFilterLabel: "Kategorie",
        allCategories: "Alle Kategorien",
        uncategorized: "Ohne Kategorie",
        productListAria: "Produktliste",
        ingredientFilterLabel: "Nach Zutat suchen",
        ingredientFilterPlaceholder: "Zutat eingeben",
        ingredientResultsAria: "Zutaten-Suchergebnisse",
        legendProduct: "Herstellbares Produkt",
        legendVariant: "Rezeptvariante",
        legendSlot: "Zutatenspalte",
        legendRaw: "Grundzutat",
        legendCycle: "Zyklus",
        statusLoadingProducts: "Lade Produktliste...",
        statusNoProducts: "Keine Produkte gefunden.",
        noMatchingProducts: "Keine passenden Produkte gefunden.",
        noGraphData: "Keine Daten vorhanden.",
        statusLoadingGraph: "Lade Rezept fuer {product}...",
        statusLoaded: "Geladen: {product}",
        productListLoadError: "Produktliste konnte nicht geladen werden.",
        graphLoadError: "Graph konnte nicht geladen werden.",
        searchLoadError: "Suche konnte nicht geladen werden.",
        ingredientSearchLoadError: "Zutaten-Suche konnte nicht geladen werden.",
        enterAtLeastTwo: "Mindestens 2 Zeichen eingeben.",
        searching: "Suche...",
        priceLabel: "Preis",
        priceInputTitle: "Mit Enter speichern, mit leerem Feld entfernen",
        statusSavingPrice: "Speichere Preis fuer {product}...",
        priceSaveError: "Preis konnte nicht gespeichert werden.",
        statusPriceSaved: "Preis gespeichert: {product} = {price}",
        statusPriceRemoved: "Preis entfernt: {product}",
        collapseIngredients: "Zutaten einklappen",
        expandIngredients: "Zutaten aufklappen",
        loadAsRoot: "Als neues Endprodukt laden",
        orLabel: "ODER",
        extraMatches: "+{count} weitere",
        oneVariant: "1 Variante",
        multipleVariants: "{count} Varianten",
        oneIngredient: "1 Zutat",
        multipleIngredients: "{count} Zutaten"
    },
    en: {
        documentTitle: "NMS Recipe Graph",
        appTitle: "Recipe Graph",
        intro: "This view lays products out from top to bottom. Sub-products are resolved recursively, and ingredients are grouped per recipe under Ingredient 1, Ingredient 2 and Ingredient 3. Product cards can be loaded again as the root.",
        navigationHint: "In the graph area, hold the right mouse button to pan horizontally and vertically.",
        languageLabel: "Language",
        productFilterLabel: "Filter products",
        productFilterPlaceholder: "Enter a product name",
        productSortLabel: "Sort by",
        sortCategory: "Category",
        sortName: "Name",
        sortPrice: "Price",
        productCategoryFilterLabel: "Category",
        allCategories: "All categories",
        uncategorized: "Uncategorized",
        productListAria: "Product list",
        ingredientFilterLabel: "Search by ingredient",
        ingredientFilterPlaceholder: "Enter an ingredient",
        ingredientResultsAria: "Ingredient search results",
        legendProduct: "Craftable product",
        legendVariant: "Recipe variant",
        legendSlot: "Ingredient column",
        legendRaw: "Base ingredient",
        legendCycle: "Cycle",
        statusLoadingProducts: "Loading product list...",
        statusNoProducts: "No products found.",
        noMatchingProducts: "No matching products found.",
        noGraphData: "No data available.",
        statusLoadingGraph: "Loading recipe for {product}...",
        statusLoaded: "Loaded: {product}",
        productListLoadError: "The product list could not be loaded.",
        graphLoadError: "The graph could not be loaded.",
        searchLoadError: "The search results could not be loaded.",
        ingredientSearchLoadError: "The ingredient search could not be loaded.",
        enterAtLeastTwo: "Enter at least 2 characters.",
        searching: "Searching...",
        priceLabel: "Price",
        priceInputTitle: "Press Enter to save, clear the field to remove the price",
        statusSavingPrice: "Saving price for {product}...",
        priceSaveError: "The price could not be saved.",
        statusPriceSaved: "Saved price: {product} = {price}",
        statusPriceRemoved: "Removed price: {product}",
        collapseIngredients: "Collapse ingredients",
        expandIngredients: "Expand ingredients",
        loadAsRoot: "Load as the new root product",
        orLabel: "OR",
        extraMatches: "+{count} more",
        oneVariant: "1 variant",
        multipleVariants: "{count} variants",
        oneIngredient: "1 ingredient",
        multipleIngredients: "{count} ingredients"
    }
};

let connectorFrame = null;
let ingredientSearchHandle = null;
let ingredientSearchToken = 0;
let allProducts = [];
let selectedProductKey = "";
let currentLanguage = resolveInitialLanguage();
const canvasPanState = {
    active: false,
    moved: false,
    suppressContextMenu: false,
    startX: 0,
    startY: 0,
    scrollLeft: 0,
    scrollTop: 0
};

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

function applyLanguage(language) {
    currentLanguage = normalizeLanguage(language) ?? DEFAULT_LANGUAGE;
    languageSelect.value = currentLanguage;
    window.localStorage.setItem("nms-recipes-language", currentLanguage);

    document.documentElement.lang = currentLanguage;
    document.title = t("documentTitle");
    document.getElementById("appTitle").textContent = t("appTitle");
    document.getElementById("introText").textContent = t("intro");
    document.getElementById("navigationHint").textContent = t("navigationHint");
    document.getElementById("languageLabel").textContent = t("languageLabel");
    productFilterLabel.textContent = t("productFilterLabel");
    productFilterInput.placeholder = t("productFilterPlaceholder");
    productSortLabel.textContent = t("productSortLabel");
    productCategoryFilterLabel.textContent = t("productCategoryFilterLabel");
    ingredientFilterLabel.textContent = t("ingredientFilterLabel");
    ingredientFilterInput.placeholder = t("ingredientFilterPlaceholder");
    document.getElementById("legendProductLabel").textContent = t("legendProduct");
    document.getElementById("legendVariantLabel").textContent = t("legendVariant");
    document.getElementById("legendSlotLabel").textContent = t("legendSlot");
    document.getElementById("legendRawLabel").textContent = t("legendRaw");
    document.getElementById("legendCycleLabel").textContent = t("legendCycle");
    productList.setAttribute("aria-label", t("productListAria"));
    ingredientResults.setAttribute("aria-label", t("ingredientResultsAria"));

    productSortSelect.options[0].text = t("sortCategory");
    productSortSelect.options[1].text = t("sortName");
    productSortSelect.options[2].text = t("sortPrice");
}

async function loadProducts(preferredProductKey = null) {
    setStatus(t("statusLoadingProducts"));

    const response = await fetch(buildApiUrl("/api/products"));
    if (!response.ok) {
        throw new Error(t("productListLoadError"));
    }

    allProducts = await response.json();
    renderProductCategoryOptions();
    renderProductList();

    const urlProduct = new URLSearchParams(window.location.search).get("product");
    const initialProductKey = allProducts.find((product) => product.key === preferredProductKey)?.key
        ?? allProducts.find((product) => product.key === urlProduct)?.key
        ?? allProducts[0]?.key;

    if (!initialProductKey) {
        treeRoot.innerHTML = "";
        treeRoot.className = "empty-state";
        setStatus(t("statusNoProducts"));
        return;
    }

    await loadGraph(initialProductKey);

    const ingredientQuery = ingredientFilterInput.value.trim();
    if (ingredientQuery.length >= 2) {
        await searchIngredients(ingredientQuery);
    } else if (!ingredientQuery) {
        ingredientResults.innerHTML = "";
    }
}

function renderProductList() {
    productList.innerHTML = "";

    const visibleProducts = getVisibleProducts();
    if (visibleProducts.length === 0) {
        productList.appendChild(createInfoBlock(t("noMatchingProducts")));
        return;
    }

    if (productSortSelect.value === "category") {
        renderGroupedProductList(visibleProducts);
        return;
    }

    const fragment = document.createDocumentFragment();
    sortProducts(visibleProducts).forEach((product) => {
        fragment.appendChild(createProductListItem(product));
    });
    productList.appendChild(fragment);
}

function renderGroupedProductList(products) {
    const groupedProducts = new Map();
    for (const product of products) {
        const category = productCategoryName(product);
        const bucket = groupedProducts.get(category) ?? [];
        bucket.push(product);
        groupedProducts.set(category, bucket);
    }

    const categories = Array.from(groupedProducts.keys()).sort((left, right) => left.localeCompare(right, locale()));
    const fragment = document.createDocumentFragment();

    for (const category of categories) {
        const group = document.createElement("section");
        group.className = "list-group";

        const heading = document.createElement("h3");
        heading.className = "list-group-title";
        heading.textContent = category;
        group.appendChild(heading);

        const items = document.createElement("div");
        items.className = "list-items";

        sortProducts(groupedProducts.get(category), "name").forEach((product) => {
            items.appendChild(createProductListItem(product));
        });

        group.appendChild(items);
        fragment.appendChild(group);
    }

    productList.appendChild(fragment);
}

function renderProductCategoryOptions() {
    const previousValue = productCategoryFilter.value;
    const categories = new Map();
    let hasEmptyCategory = false;

    allProducts.forEach((product) => {
        if (product.categoryKey) {
            categories.set(product.categoryKey, product.category);
            return;
        }

        hasEmptyCategory = true;
    });

    productCategoryFilter.innerHTML = "";
    productCategoryFilter.appendChild(new Option(t("allCategories"), ""));

    Array.from(categories.entries())
        .sort((left, right) => left[1].localeCompare(right[1], locale()))
        .forEach(([categoryKey, categoryLabel]) => {
            productCategoryFilter.appendChild(new Option(categoryLabel, categoryKey));
        });

    if (hasEmptyCategory) {
        productCategoryFilter.appendChild(new Option(t("uncategorized"), NO_CATEGORY_FILTER_VALUE));
    }

    const validValues = Array.from(productCategoryFilter.options).map((option) => option.value);
    productCategoryFilter.value = validValues.includes(previousValue) ? previousValue : "";
}

function getVisibleProducts() {
    const nameFilter = normalizeSearchText(productFilterInput.value);
    const categoryFilter = productCategoryFilter.value;

    return allProducts.filter((product) => {
        if (nameFilter && !normalizeSearchText(product.name).includes(nameFilter)) {
            return false;
        }

        return matchesProductCategoryFilter(product, categoryFilter);
    });
}

function matchesProductCategoryFilter(product, selectedCategory) {
    if (!selectedCategory) {
        return true;
    }

    if (selectedCategory === NO_CATEGORY_FILTER_VALUE) {
        return !product.categoryKey;
    }

    return product.categoryKey === selectedCategory;
}

function sortProducts(products, sortMode = productSortSelect.value) {
    return [...products].sort((left, right) => compareProducts(left, right, sortMode));
}

function compareProducts(left, right, sortMode) {
    if (sortMode === "price") {
        return compareByPrice(left, right);
    }

    if (sortMode === "name") {
        return compareByName(left, right);
    }

    return compareByCategory(left, right);
}

function compareByName(left, right) {
    const nameComparison = left.name.localeCompare(right.name, locale());
    if (nameComparison !== 0) {
        return nameComparison;
    }

    return productCategoryName(left).localeCompare(productCategoryName(right), locale());
}

function compareByCategory(left, right) {
    const categoryComparison = productCategoryName(left).localeCompare(productCategoryName(right), locale());
    if (categoryComparison !== 0) {
        return categoryComparison;
    }

    return compareByName(left, right);
}

function compareByPrice(left, right) {
    const leftPrice = parsePriceValue(left.price);
    const rightPrice = parsePriceValue(right.price);

    if (leftPrice === null && rightPrice === null) {
        return compareByName(left, right);
    }

    if (leftPrice === null) {
        return 1;
    }

    if (rightPrice === null) {
        return -1;
    }

    if (leftPrice !== rightPrice) {
        return leftPrice - rightPrice;
    }

    return compareByName(left, right);
}

function parsePriceValue(price) {
    if (!price) {
        return null;
    }

    const normalized = String(price)
        .replace(/\./g, "")
        .replace(",", ".")
        .trim();
    const parsed = Number.parseFloat(normalized);
    return Number.isFinite(parsed) ? parsed : null;
}

function productCategoryName(product) {
    return product.category || t("uncategorized");
}

function createProductListItem(product) {
    const entry = document.createElement("article");
    entry.className = "list-entry";

    const button = document.createElement("button");
    button.type = "button";
    button.className = `list-button${product.key === selectedProductKey ? " is-selected" : ""}`;
    button.dataset.productKey = product.key;
    button.addEventListener("click", async () => {
        await loadGraph(product.key);
    });

    const title = document.createElement("span");
    title.className = "list-button-title";
    title.textContent = product.name;
    button.appendChild(title);

    const meta = document.createElement("span");
    meta.className = "list-button-meta";
    meta.textContent = buildProductMeta(product);
    button.appendChild(meta);
    entry.appendChild(button);

    const priceRow = document.createElement("div");
    priceRow.className = "list-price-row";

    const priceLabel = document.createElement("label");
    priceLabel.className = "list-price-label";
    priceLabel.textContent = t("priceLabel");
    priceLabel.htmlFor = priceInputId(product.key);
    priceRow.appendChild(priceLabel);

    const priceInput = document.createElement("input");
    priceInput.id = priceInputId(product.key);
    priceInput.type = "text";
    priceInput.className = "list-price-input";
    priceInput.inputMode = "decimal";
    priceInput.placeholder = "-";
    priceInput.value = product.price ?? "";
    priceInput.title = t("priceInputTitle");
    priceInput.addEventListener("keydown", async (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            await saveProductPrice(product.key, priceInput);
            return;
        }

        if (event.key === "Escape") {
            priceInput.value = product.price ?? "";
            priceInput.blur();
        }
    });
    priceInput.addEventListener("click", (event) => {
        event.stopPropagation();
    });
    priceRow.appendChild(priceInput);

    entry.appendChild(priceRow);
    return entry;
}

function renderIngredientResults(results) {
    ingredientResults.innerHTML = "";

    if (results.length === 0) {
        ingredientResults.appendChild(createInfoBlock(t("noMatchingProducts")));
        return;
    }

    const fragment = document.createDocumentFragment();
    results.forEach((result) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `list-button search-result${result.key === selectedProductKey ? " is-selected" : ""}`;
        button.dataset.productKey = result.key;
        button.addEventListener("click", async () => {
            await loadGraph(result.key);
        });

        const title = document.createElement("span");
        title.className = "list-button-title";
        title.textContent = result.name;
        button.appendChild(title);

        const meta = document.createElement("span");
        meta.className = "list-button-meta";
        meta.textContent = buildIngredientResultMeta(result);
        button.appendChild(meta);

        if (Array.isArray(result.matches) && result.matches.length > 0) {
            const matches = document.createElement("div");
            matches.className = "result-matches";

            result.matches.slice(0, 4).forEach((match) => {
                const chip = document.createElement("span");
                chip.className = "match-chip";
                chip.textContent = match;
                matches.appendChild(chip);
            });

            if (result.matches.length > 4) {
                const extra = document.createElement("span");
                extra.className = "match-chip match-chip-muted";
                extra.textContent = t("extraMatches", { count: result.matches.length - 4 });
                matches.appendChild(extra);
            }

            button.appendChild(matches);
        }

        fragment.appendChild(button);
    });

    ingredientResults.appendChild(fragment);
}

function createInfoBlock(message) {
    const element = document.createElement("div");
    element.className = "info-block";
    element.textContent = message;
    return element;
}

async function loadGraph(productKey) {
    if (!productKey) {
        return;
    }

    selectedProductKey = productKey;
    renderProductList();
    renderIngredientSelection();
    scrollSelectedProductIntoView();

    const displayName = displayProductName(productKey);
    setStatus(t("statusLoadingGraph", { product: displayName }));

    const response = await fetch(buildApiUrl("/api/graph", { product: productKey }));
    if (!response.ok) {
        const payload = await response.json().catch(() => ({ message: t("graphLoadError") }));
        throw new Error(payload.message || t("graphLoadError"));
    }

    const graph = await response.json();
    renderGraph(graph);
    replaceUrlState(productKey);
    setStatus(t("statusLoaded", { product: graph.label || displayName }));
}

function replaceUrlState(productKey) {
    const searchParams = new URLSearchParams();
    searchParams.set("product", productKey);
    searchParams.set("lang", currentLanguage);
    window.history.replaceState({}, "", `/?${searchParams.toString()}`);
}

function renderIngredientSelection() {
    ingredientResults.querySelectorAll(".search-result").forEach((result) => {
        result.classList.toggle("is-selected", result.dataset.productKey === selectedProductKey);
    });
}

function renderGraph(root) {
    treeRoot.innerHTML = "";

    if (!root) {
        treeRoot.textContent = t("noGraphData");
        treeRoot.className = "empty-state";
        return;
    }

    treeRoot.className = "graph-root";
    treeRoot.appendChild(createFlowNode(root, true));
    queueConnectorRender();
}

function createFlowNode(node, isRoot) {
    if (node.type === "product") {
        return createProductNode(node, isRoot);
    }

    if (node.type === "variant") {
        return createVariantNode(node);
    }

    if (node.type === "slot") {
        return createSlotNode(node);
    }

    return createLeafNode(node, isRoot);
}

function createProductNode(node, isRoot) {
    const wrapper = document.createElement("section");
    wrapper.className = `flow-node product-node${isRoot ? " root-node" : ""}`;
    const card = createCard(node, isRoot);
    card.classList.add("connect-source", "connect-target");
    let branch = null;

    if (node.children && node.children.length > 0) {
        const toggle = createToggleButton(isRoot);
        toggle.addEventListener("click", (event) => {
            event.stopPropagation();
            setExpanded(wrapper, branch, toggle, branch.hidden);
        });
        card.appendChild(toggle);
    }

    wrapper.appendChild(card);

    if (node.children && node.children.length > 0) {
        wrapper.classList.add("connect-scope");
        branch = document.createElement("div");
        branch.className = node.children[0].type === "variant" ? "variant-row connect-branch" : "slot-grid connect-branch";
        node.children.forEach((child) => {
            const childElement = createFlowNode(child, false);
            childElement.classList.add("connect-child");
            branch.appendChild(childElement);
        });
        branch.hidden = !isRoot;
        wrapper.classList.toggle("is-collapsed", !isRoot);
        wrapper.appendChild(branch);
    }

    return wrapper;
}

function createVariantNode(node) {
    const wrapper = document.createElement("section");
    wrapper.className = "flow-node variant-node";
    const card = createCard(node, false);
    card.classList.add("connect-source", "connect-target");
    wrapper.appendChild(card);

    if (node.children && node.children.length > 0) {
        wrapper.classList.add("connect-scope");
        const branch = document.createElement("div");
        branch.className = "slot-grid connect-branch";
        node.children.forEach((child) => {
            const childElement = createFlowNode(child, false);
            childElement.classList.add("connect-child");
            branch.appendChild(childElement);
        });
        wrapper.appendChild(branch);
    }

    return wrapper;
}

function createSlotNode(node) {
    const wrapper = document.createElement("section");
    wrapper.className = "slot-column";

    const title = document.createElement("h3");
    title.className = "slot-title connect-source connect-target";
    title.textContent = node.label;
    wrapper.appendChild(title);

    const stack = document.createElement("div");
    const hasAlternatives = node.children.length > 1;
    stack.className = `slot-stack connect-branch${hasAlternatives ? " slot-options" : ""}`;
    if (hasAlternatives) {
        const optionLabel = document.createElement("div");
        optionLabel.className = "slot-choice-label";
        optionLabel.textContent = t("orLabel");
        wrapper.appendChild(optionLabel);
    }

    if (node.children.length === 0) {
        const emptyState = document.createElement("div");
        emptyState.className = "slot-empty";
        emptyState.setAttribute("aria-hidden", "true");
        stack.appendChild(emptyState);
    } else {
        node.children.forEach((child) => {
            const childElement = createFlowNode(child, false);
            childElement.classList.add("connect-child");
            stack.appendChild(childElement);
        });
    }
    wrapper.appendChild(stack);

    if (node.children && node.children.length > 0) {
        wrapper.classList.add("connect-scope");
    }

    return wrapper;
}

function createLeafNode(node, isRoot) {
    const wrapper = document.createElement("section");
    wrapper.className = `flow-node leaf-node ${node.type}-node${isRoot ? " root-node" : ""}`;
    const card = createCard(node, isRoot);
    card.classList.add("connect-target");
    wrapper.appendChild(card);
    return wrapper;
}

function createCard(node, isRoot) {
    const element = document.createElement("article");
    element.className = `node-card ${node.type}${isRoot ? " root-card" : ""}`;

    const label = document.createElement("div");
    label.className = "node-label";
    label.textContent = node.label;
    element.appendChild(label);

    if (node.detail) {
        const detail = document.createElement("div");
        detail.className = "node-detail";
        detail.textContent = node.detail;
        element.appendChild(detail);
    }

    if (node.type === "product" && !isRoot && node.key) {
        element.title = t("loadAsRoot");
        element.addEventListener("click", async () => {
            await loadGraph(node.key);
        });
    }

    return element;
}

function setStatus(message) {
    statusElement.textContent = message;
}

function createToggleButton(expanded) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "toggle-button";
    button.title = expanded ? t("collapseIngredients") : t("expandIngredients");
    button.setAttribute("aria-expanded", String(expanded));
    button.textContent = expanded ? "-" : "+";
    return button;
}

function setExpanded(wrapper, branch, toggle, expanded) {
    if (!branch || !toggle) {
        return;
    }

    branch.hidden = !expanded;
    wrapper.classList.toggle("is-collapsed", !expanded);
    toggle.setAttribute("aria-expanded", String(expanded));
    toggle.title = expanded ? t("collapseIngredients") : t("expandIngredients");
    toggle.textContent = expanded ? "-" : "+";
    queueConnectorRender();
}

function queueConnectorRender() {
    if (connectorFrame !== null) {
        window.cancelAnimationFrame(connectorFrame);
    }

    connectorFrame = window.requestAnimationFrame(() => {
        connectorFrame = window.requestAnimationFrame(() => {
            connectorFrame = null;
            drawAllConnectors();
        });
    });
}

function drawAllConnectors() {
    const scopes = treeRoot.querySelectorAll(".connect-scope");
    scopes.forEach((scope) => drawConnectorsForScope(scope));
}

function drawConnectorsForScope(scope) {
    scope.querySelectorAll(":scope > .connector-layer").forEach((layer) => layer.remove());

    const source = scope.querySelector(":scope > .connect-source");
    const branch = scope.querySelector(":scope > .connect-branch");
    if (!source || !branch || branch.hidden) {
        return;
    }

    const children = Array.from(branch.children).filter((child) => child.classList.contains("connect-child"));
    if (children.length === 0) {
        return;
    }

    const scopeRect = scope.getBoundingClientRect();
    if (scopeRect.width === 0 || scopeRect.height === 0) {
        return;
    }

    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.classList.add("connector-layer");
    svg.setAttribute("viewBox", `0 0 ${scopeRect.width} ${scopeRect.height}`);
    svg.setAttribute("preserveAspectRatio", "none");
    svg.setAttribute("aria-hidden", "true");

    const defs = document.createElementNS("http://www.w3.org/2000/svg", "defs");
    const marker = document.createElementNS("http://www.w3.org/2000/svg", "marker");
    const markerId = `arrow-${Math.random().toString(36).slice(2, 10)}`;
    marker.setAttribute("id", markerId);
    marker.setAttribute("markerWidth", "10");
    marker.setAttribute("markerHeight", "10");
    marker.setAttribute("refX", "8");
    marker.setAttribute("refY", "5");
    marker.setAttribute("orient", "auto");
    marker.setAttribute("markerUnits", "strokeWidth");

    const arrowHead = document.createElementNS("http://www.w3.org/2000/svg", "path");
    arrowHead.setAttribute("d", "M 0 0 L 10 5 L 0 10 z");
    arrowHead.setAttribute("fill", "var(--line)");
    marker.appendChild(arrowHead);
    defs.appendChild(marker);
    svg.appendChild(defs);

    const sourcePoint = rectBottomCenter(source.getBoundingClientRect(), scopeRect);

    children.forEach((child) => {
        const target = child.querySelector(":scope > .connect-target");
        if (!target) {
            return;
        }

        const targetPoint = rectTopCenter(target.getBoundingClientRect(), scopeRect);
        const midY = sourcePoint.y + Math.max(26, (targetPoint.y - sourcePoint.y) * 0.5);

        const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
        path.setAttribute(
            "d",
            `M ${sourcePoint.x} ${sourcePoint.y} C ${sourcePoint.x} ${midY}, ${targetPoint.x} ${midY}, ${targetPoint.x} ${targetPoint.y - 8}`
        );
        path.setAttribute("class", "connector-path");
        path.setAttribute("marker-end", `url(#${markerId})`);
        svg.appendChild(path);
    });

    scope.appendChild(svg);
}

function rectBottomCenter(rect, containerRect) {
    return {
        x: rect.left + rect.width / 2 - containerRect.left,
        y: rect.bottom - containerRect.top
    };
}

function rectTopCenter(rect, containerRect) {
    return {
        x: rect.left + rect.width / 2 - containerRect.left,
        y: rect.top - containerRect.top
    };
}

function normalizeSearchText(value) {
    return (value || "").trim().toLocaleLowerCase(locale());
}

function variantLabel(count) {
    if (count === 1) {
        return t("oneVariant");
    }

    return t("multipleVariants", { count });
}

function ingredientCountLabel(minCount, maxCount) {
    if (minCount === maxCount) {
        return ingredientValueLabel(minCount);
    }

    return `${minCount}-${maxCount} ${t("multipleIngredients", { count: "" }).trim()}`;
}

function ingredientValueLabel(count) {
    if (count === 1) {
        return t("oneIngredient");
    }

    return t("multipleIngredients", { count });
}

function buildProductMeta(product) {
    const parts = [];

    if (shouldShowCategoryInProductMeta()) {
        parts.push(productCategoryName(product));
    }

    parts.push(
        variantLabel(product.variantCount),
        ingredientCountLabel(product.minIngredientCount, product.maxIngredientCount)
    );

    if (product.price) {
        parts.push(`${t("priceLabel")} ${product.price}`);
    }

    return parts.join(" | ");
}

function shouldShowCategoryInProductMeta() {
    return productSortSelect.value !== "category" && !productCategoryFilter.value;
}

function buildIngredientResultMeta(result) {
    const parts = [
        result.category || t("uncategorized"),
        variantLabel(result.variantCount)
    ];

    if (result.price) {
        parts.push(`${t("priceLabel")} ${result.price}`);
    }

    return parts.join(" | ");
}

function priceInputId(productKey) {
    return `price-${String(productKey).toLocaleLowerCase(locale()).replace(/[^a-z0-9]+/g, "-")}`;
}

async function saveProductPrice(productKey, inputElement) {
    const rawPrice = inputElement.value.trim();
    const displayName = displayProductName(productKey);
    inputElement.disabled = true;
    inputElement.classList.add("is-saving");
    setStatus(t("statusSavingPrice", { product: displayName }));

    try {
        const response = await fetch(buildApiUrl("/api/prices"), {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                key: productKey,
                price: rawPrice
            })
        });

        const payload = await response.json().catch(() => ({ message: t("priceSaveError") }));
        if (!response.ok) {
            throw new Error(payload.message || t("priceSaveError"));
        }

        updateStoredProductPrice(productKey, payload.price ?? null);
        renderProductList();
        scrollSelectedProductIntoView();

        const ingredientQuery = ingredientFilterInput.value.trim();
        if (ingredientQuery.length >= 2) {
            await searchIngredients(ingredientQuery);
        } else {
            renderIngredientSelection();
        }

        if (selectedProductKey) {
            await loadGraph(selectedProductKey);
        }

        setStatus(payload.price
            ? t("statusPriceSaved", { product: displayName, price: payload.price })
            : t("statusPriceRemoved", { product: displayName }));
    } catch (error) {
        setStatus(error.message || t("priceSaveError"));
    } finally {
        if (document.body.contains(inputElement)) {
            inputElement.disabled = false;
            inputElement.classList.remove("is-saving");
        }
    }
}

function updateStoredProductPrice(productKey, price) {
    const product = allProducts.find((entry) => entry.key === productKey);
    if (product) {
        product.price = price;
    }
}

function displayProductName(productKey) {
    return allProducts.find((product) => product.key === productKey)?.name ?? productKey;
}

function scrollSelectedProductIntoView() {
    const selectedButton = productList.querySelector(".list-button.is-selected");
    if (selectedButton) {
        selectedButton.scrollIntoView({ block: "nearest" });
    }
}

function startCanvasPan(event) {
    if (!canvasElement || event.button !== 2) {
        return;
    }

    canvasPanState.active = true;
    canvasPanState.moved = false;
    canvasPanState.suppressContextMenu = false;
    canvasPanState.startX = event.clientX;
    canvasPanState.startY = event.clientY;
    canvasPanState.scrollLeft = canvasElement.scrollLeft;
    canvasPanState.scrollTop = canvasElement.scrollTop;
    canvasElement.classList.add("is-panning");
    event.preventDefault();
}

function moveCanvasPan(event) {
    if (!canvasElement || !canvasPanState.active) {
        return;
    }

    const deltaX = event.clientX - canvasPanState.startX;
    const deltaY = event.clientY - canvasPanState.startY;
    if (!canvasPanState.moved && (Math.abs(deltaX) > 2 || Math.abs(deltaY) > 2)) {
        canvasPanState.moved = true;
    }

    canvasElement.scrollLeft = canvasPanState.scrollLeft - deltaX;
    canvasElement.scrollTop = canvasPanState.scrollTop - deltaY;
    event.preventDefault();
}

function endCanvasPan() {
    if (!canvasElement || !canvasPanState.active) {
        return;
    }

    canvasPanState.active = false;
    canvasPanState.suppressContextMenu = canvasPanState.moved;
    canvasElement.classList.remove("is-panning");
}

function handleCanvasContextMenu(event) {
    if (!canvasPanState.active && !canvasPanState.suppressContextMenu) {
        return;
    }

    canvasPanState.suppressContextMenu = false;
    event.preventDefault();
}

function queueIngredientSearch() {
    if (ingredientSearchHandle !== null) {
        window.clearTimeout(ingredientSearchHandle);
    }

    const query = ingredientFilterInput.value.trim();
    if (!query) {
        ingredientSearchToken += 1;
        ingredientResults.innerHTML = "";
        return;
    }

    ingredientSearchHandle = window.setTimeout(() => {
        ingredientSearchHandle = null;
        searchIngredients(query).catch((error) => {
            ingredientResults.innerHTML = "";
            ingredientResults.appendChild(createInfoBlock(t("searchLoadError")));
            setStatus(error.message || t("searchLoadError"));
        });
    }, 180);
}

async function searchIngredients(query) {
    const normalizedQuery = query.trim();
    if (!normalizedQuery) {
        ingredientResults.innerHTML = "";
        return;
    }

    if (normalizedQuery.length < 2) {
        ingredientResults.innerHTML = "";
        ingredientResults.appendChild(createInfoBlock(t("enterAtLeastTwo")));
        return;
    }

    const token = ++ingredientSearchToken;
    ingredientResults.innerHTML = "";
    ingredientResults.appendChild(createInfoBlock(t("searching")));

    const response = await fetch(buildApiUrl("/api/ingredients/search", { query: normalizedQuery }));
    if (token !== ingredientSearchToken) {
        return;
    }

    if (!response.ok) {
        throw new Error(t("ingredientSearchLoadError"));
    }

    const results = await response.json();
    if (token !== ingredientSearchToken) {
        return;
    }

    renderIngredientResults(results);
}

productFilterInput.addEventListener("input", () => {
    renderProductList();
});

productSortSelect.addEventListener("change", () => {
    renderProductList();
    scrollSelectedProductIntoView();
});

productCategoryFilter.addEventListener("change", () => {
    renderProductList();
    scrollSelectedProductIntoView();
});

productFilterInput.addEventListener("keydown", async (event) => {
    if (event.key !== "Enter") {
        return;
    }

    const firstMatch = productList.querySelector(".list-button[data-product-key]");
    if (!firstMatch) {
        return;
    }

    event.preventDefault();
    await loadGraph(firstMatch.dataset.productKey);
});

ingredientFilterInput.addEventListener("input", queueIngredientSearch);

ingredientFilterInput.addEventListener("keydown", (event) => {
    if (event.key !== "Enter") {
        return;
    }

    event.preventDefault();
    if (ingredientSearchHandle !== null) {
        window.clearTimeout(ingredientSearchHandle);
        ingredientSearchHandle = null;
    }

    searchIngredients(ingredientFilterInput.value).catch((error) => {
        ingredientResults.innerHTML = "";
        ingredientResults.appendChild(createInfoBlock(t("searchLoadError")));
        setStatus(error.message || t("searchLoadError"));
    });
});

languageSelect.addEventListener("change", async () => {
    const previousProductKey = selectedProductKey;
    applyLanguage(languageSelect.value);
    try {
        await loadProducts(previousProductKey);
    } catch (error) {
        setStatus(error.message || t("productListLoadError"));
    }
});

if (canvasElement) {
    canvasElement.addEventListener("mousedown", startCanvasPan);
    canvasElement.addEventListener("contextmenu", handleCanvasContextMenu);
    window.addEventListener("mousemove", moveCanvasPan);
    window.addEventListener("mouseup", endCanvasPan);
    window.addEventListener("blur", endCanvasPan);
}

applyLanguage(currentLanguage);
loadProducts().catch((error) => {
    setStatus(error.message || t("productListLoadError"));
});

window.addEventListener("resize", queueConnectorRender);
