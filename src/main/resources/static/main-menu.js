(function () {
    const DEFAULT_LANGUAGE = "en";
    const SUPPORTED_LANGUAGES = new Set(["de", "en"]);
    const UI_TEXT = {
        de: {
            mainMenu: "Hauptmenü",
            languageLabel: "Sprache",
            home: "Kochrezepte",
            recipeOverview: "Kochrezept Übersicht",
            refinery: "Raffinerie",
            refineryOverview: "Raffinerie Übersicht",
            sources: "Quellen",
            addCategory: "Koch-Kategorie hinzufügen",
            addRecipe: "Kochrezept hinzufügen"
        },
        en: {
            mainMenu: "Main menu",
            languageLabel: "Language",
            home: "Cooking Recipes",
            recipeOverview: "Cooking Recipe Overview",
            refinery: "Refinery",
            refineryOverview: "Refinery Overview",
            sources: "Sources",
            addCategory: "Add cooking category",
            addRecipe: "Add cooking recipe"
        }
    };

    const menuRoot = document.getElementById("mainMenu");
    const menuToggle = document.getElementById("mainMenuToggle");
    const menuPanel = document.getElementById("mainMenuPanel");
    const languageLabel = document.getElementById("languageLabel");
    const languageSelect = document.getElementById("languageSelect");
    const homeLink = document.getElementById("menuHomeLink");
    const recipeOverviewLink = document.getElementById("menuRecipeOverviewLink");
    const refineryLink = document.getElementById("menuRefineryLink");
    const refineryOverviewLink = document.getElementById("menuRefineryOverviewLink");
    const sourcesLink = document.getElementById("menuSourcesLink");
    const addCategoryLink = document.getElementById("menuAddCategoryLink");
    const addRecipeLink = document.getElementById("menuAddRecipeLink");

    function normalizeLanguage(language) {
        if (!language) {
            return null;
        }

        const normalized = String(language).trim().toLowerCase();
        return SUPPORTED_LANGUAGES.has(normalized) ? normalized : null;
    }

    function resolveInitialLanguage() {
        const urlLanguage = normalizeLanguage(new URLSearchParams(window.location.search).get("lang"));
        if (urlLanguage) {
            return urlLanguage;
        }

        return normalizeLanguage(document.documentElement.lang) ?? DEFAULT_LANGUAGE;
    }

    function isHomePath(pathname) {
        return pathname === "/" || pathname === "/index.html";
    }

    function toLocalPath(url) {
        const search = url.searchParams.toString();
        return `${url.pathname}${search ? `?${search}` : ""}`;
    }

    function withLanguage(target, language) {
        const url = new URL(target, window.location.origin);
        url.searchParams.set("lang", language);
        return toLocalPath(url);
    }

    function currentLocationTarget() {
        return `${window.location.pathname}${window.location.search}`;
    }

    function resolvePreservedReturnTarget() {
        const rawReturn = new URLSearchParams(window.location.search).get("return");
        return rawReturn && rawReturn.startsWith("/") ? rawReturn : null;
    }

    function buildHomeUrl(language) {
        const preservedReturn = resolvePreservedReturnTarget();
        let target = null;

        if (isHomePath(window.location.pathname)) {
            target = currentLocationTarget();
        } else if (preservedReturn) {
            const preservedUrl = new URL(preservedReturn, window.location.origin);
            if (isHomePath(preservedUrl.pathname)) {
                target = preservedReturn;
            }
        }

        return withLanguage(target || "/", language);
    }

    function buildSubpageUrl(pathname, language) {
        const searchParams = new URLSearchParams();
        searchParams.set("lang", language);
        searchParams.set(
            "return",
            withLanguage(resolvePreservedReturnTarget() || currentLocationTarget(), language)
        );
        return `${pathname}?${searchParams.toString()}`;
    }

    function updateLink(link, text, href, isActive) {
        if (!link) {
            return;
        }

        link.textContent = text;
        link.href = href;
        link.classList.toggle("is-active", isActive);
        if (isActive) {
            link.setAttribute("aria-current", "page");
        } else {
            link.removeAttribute("aria-current");
        }
    }

    function update(language) {
        if (!menuRoot) {
            return;
        }

        const resolvedLanguage = normalizeLanguage(language) ?? resolveInitialLanguage();
        const text = UI_TEXT[resolvedLanguage] ?? UI_TEXT[DEFAULT_LANGUAGE];

        if (menuToggle) {
            menuToggle.textContent = text.mainMenu;
            menuToggle.setAttribute("aria-label", text.mainMenu);
            menuToggle.title = text.mainMenu;
        }

        if (menuPanel) {
            menuPanel.setAttribute("aria-label", text.mainMenu);
        }

        if (languageLabel) {
            languageLabel.textContent = text.languageLabel;
        }

        if (languageSelect) {
            languageSelect.value = resolvedLanguage;
        }

        updateLink(homeLink, text.home, buildHomeUrl(resolvedLanguage), isHomePath(window.location.pathname));
        updateLink(
            recipeOverviewLink,
            text.recipeOverview,
            buildSubpageUrl("/recipe-overview.html", resolvedLanguage),
            window.location.pathname === "/recipe-overview.html"
        );
        updateLink(
            refineryLink,
            text.refinery,
            buildSubpageUrl("/refinery.html", resolvedLanguage),
            window.location.pathname === "/refinery.html"
        );
        updateLink(
            refineryOverviewLink,
            text.refineryOverview,
            buildSubpageUrl("/refinery-overview.html", resolvedLanguage),
            window.location.pathname === "/refinery-overview.html"
        );
        updateLink(
            sourcesLink,
            text.sources,
            buildSubpageUrl("/sources.html", resolvedLanguage),
            window.location.pathname === "/sources.html"
        );
        updateLink(
            addCategoryLink,
            text.addCategory,
            buildSubpageUrl("/add-category.html", resolvedLanguage),
            window.location.pathname === "/add-category.html"
        );
        updateLink(
            addRecipeLink,
            text.addRecipe,
            buildSubpageUrl("/add-recipe.html", resolvedLanguage),
            window.location.pathname === "/add-recipe.html"
        );
    }

    window.NmsMainMenu = {
        update,
        close() {
            if (menuRoot) {
                menuRoot.open = false;
            }
        }
    };

    update(resolveInitialLanguage());
}());
