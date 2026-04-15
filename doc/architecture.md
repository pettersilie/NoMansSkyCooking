# Architecture

## Overview

NMS Recipe Graph is a Spring Boot application that serves a static frontend and a small REST API for two production domains:

- cooking recipes
- refinery processes

At runtime:

1. the backend loads the cooking dataset from `data/recipes.json`
2. the backend loads the refinery dataset from `data/refinery-recipes.json`
3. the backend loads cooking prices from `data/product-prices.json`
4. the frontend requests localized data through `/api` and `/api/refinery`
5. the frontend renders interactive graphs, overview tables, and authoring forms

## Runtime Layers

### Backend

- `de.nms.nmsrecipes.NmsRecipesApplication`
  Spring Boot entry point.

- `de.nms.nmsrecipes.config.RecipeProperties`
  Holds the configured paths for the cooking dataset and the cooking price file.

- `de.nms.nmsrecipes.service.JsonRecipeBookStore`
  Reads and writes the cooking dataset.

- `de.nms.nmsrecipes.service.RecipeCatalogService`
  Loads the in-memory cooking catalog, resolves canonical names, provides category and ingredient catalogs, handles ingredient search, and persists new cooking categories and cooking recipes.

- `de.nms.nmsrecipes.service.RecipeGraphService`
  Expands a selected cooking product into the graph structure used by the UI.

- `de.nms.nmsrecipes.service.JsonRefineryBookStore`
  Reads the refinery dataset.

- `de.nms.nmsrecipes.service.RefineryCatalogService`
  Loads the in-memory refinery catalog, resolves canonical names, provides refinery category and ingredient catalogs, and handles refinery ingredient search.

- `de.nms.nmsrecipes.service.RefineryGraphService`
  Expands a selected refinery output into the graph structure used by the refinery UI.

- `de.nms.nmsrecipes.service.ProductPriceService`
  Validates, formats, and persists cooking prices in the separate price file.

- `de.nms.nmsrecipes.service.LocalizationService`
  Normalizes language selection and localizes terms, categories, graph labels, detail strings, and selected validation messages.

- `de.nms.nmsrecipes.service.EnglishTerminology`
  Provides built-in German-to-English terminology mappings.

- `de.nms.nmsrecipes.web.RecipeController`
  Exposes the REST API for cooking product browsing, graph loading, ingredient search, overview rows, price editing, category creation, and recipe creation.

- `de.nms.nmsrecipes.web.RefineryController`
  Exposes the REST API for refinery browsing, graph loading, ingredient search, and overview rows.

### Frontend

The frontend is served from `src/main/resources/static`.

- `index.html` and `app.js`
  Main cooking application shell, product loading, graph rendering, filtering, searching, localization, icons, and price editing.

- `recipe-overview.html` and `recipe-overview.js`
  Tabular overview for cooking variants.

- `refinery.html` and `refinery.js`
  Refinery graph page, including lazy loading for deeper craftable branches.

- `refinery-overview.html` and `refinery-overview.js`
  Tabular overview for refinery variants.

- `add-category.html` and `add-category.js`
  Cooking category authoring UI.

- `add-recipe.html` and `add-recipe.js`
  Cooking recipe builder UI with variant and nested sub-recipe support.

- `main-menu.js`
  Shared menu, language selection, and page-to-page navigation.

- `app.css`
  Shared styling for graph pages, overview pages, and authoring pages.

## Main Data Flow

### Startup

1. Spring Boot starts.
2. `RecipeCatalogService` resolves `recipes.source-path`.
3. `JsonRecipeBookStore` reads `data/recipes.json` into a `RecipeBook`.
4. `RefineryCatalogService` resolves `recipes.refinery-path`.
5. `JsonRefineryBookStore` reads `data/refinery-recipes.json` into a `RefineryBook`.
6. `ProductPriceService` reads `recipes.price-path`.
7. Static frontend assets become available.

### Cooking Browsing

1. The frontend requests `/api/products` and `/api/categories`.
2. The user filters or sorts the cooking list.
3. Selecting a product triggers `/api/graph?product=<key>&lang=<lang>`.
4. `RecipeGraphService` builds a recursive graph tree.
5. The frontend renders product, variant, slot, raw, and cycle nodes.

### Cooking Overview

1. The frontend requests `/api/recipes/overview`.
2. `RecipeController` flattens cooking variants into table rows.
3. The frontend filters rows by ingredient text and sorts by target product or price.

### Refinery Browsing

1. The frontend requests `/api/refinery/products` and `/api/refinery/categories`.
2. The user filters or sorts the refinery list.
3. Selecting an output triggers `/api/refinery/graph?product=<key>&lang=<lang>`.
4. `RefineryGraphService` builds a recursive graph tree.
5. The frontend renders product, variant, slot, raw, and cycle nodes.

### Refinery Overview

1. The frontend requests `/api/refinery/overview`.
2. `RefineryController` flattens refinery variants into table rows.
3. The frontend filters rows by ingredient text and sorts by target product.

### Ingredient Search

Cooking search:

1. the user enters a search term
2. the frontend calls `/api/ingredients/search`
3. `RecipeCatalogService` computes the recursive cooking ingredient closure per product
4. matching products are returned with localized names, matching ingredient labels, and optional prices

Refinery search:

1. the user enters a search term
2. the frontend calls `/api/refinery/ingredients/search`
3. `RefineryCatalogService` computes the recursive refinery ingredient closure per output
4. matching outputs are returned with localized names and matching ingredient labels

### Price Editing

1. the user edits a cooking price in the product list
2. the frontend sends `PUT /api/prices`
3. `ProductPriceService` validates and persists the value
4. the frontend refreshes visible cooking data

Refinery data does not use the price service.

### Cooking Category Creation

1. the user opens `add-category.html`
2. the page submits `POST /api/categories`
3. `RecipeCatalogService` adds the category to `RecipeBook`
4. `JsonRecipeBookStore` writes the updated dataset back to `data/recipes.json`

### Cooking Recipe Creation

1. the user opens `add-recipe.html`
2. the page loads `/api/categories` and `/api/ingredients/catalog`
3. the user builds a recipe draft with variants and ingredient slots
4. the page submits `POST /api/recipes`
5. `RecipeCatalogService` validates the draft, creates any nested sub-recipes, registers English term overrides, and saves the result to `data/recipes.json`

## Domain Model

### Cooking Types

- `RecipeBook`
  In-memory catalog of cooking recipes, cooking categories, and English term/category overrides.

- `RecipeDefinition`
  One craftable cooking product.

- `RecipeVariant`
  One alternative cooking path for a product.

- `IngredientSlot`
  One ingredient position within a cooking variant.

- `RecipeDraft`
  Incoming API payload for a new cooking recipe.

- `RecipeDraftVariant`
  One incoming draft variant.

- `RecipeDraftIngredient`
  One incoming draft ingredient reference, raw ingredient, or nested recipe.

### Refinery Types

- `RefineryBook`
  In-memory catalog of refinery outputs, categories, and English term/category overrides.

- `RefineryDefinition`
  One craftable refinery output.

- `RefineryVariant`
  One refinery process, including operation metadata, output quantity, and ingredient list.

- `RefineryIngredient`
  One refinery input position with an explicit quantity.

### Shared View Model

- `TreeNode`
  Graph view model returned to the frontend for both domains.

## Graph Semantics

The graph is not treated as a strict tree internally because recipes and refinery chains can reuse products and ingredients.

Rendering rules:

- separate slots represent AND relationships
- multiple values inside one cooking slot represent OR relationships
- products with multiple variants get explicit variant nodes
- raw ingredients are leaves
- cycles are rendered as dedicated cycle nodes instead of recursing forever

Refinery-specific additions:

- input quantities are carried into the graph detail labels
- deeper craftable refinery branches can be loaded lazily when the user expands them

## Localization Model

Both persisted datasets keep canonical German keys.

Important consequences:

- API keys stay stable regardless of UI language
- English labels are generated at runtime
- custom English labels for cooking data are stored in `data/recipes.json`
- custom English labels for refinery data are stored in `data/refinery-recipes.json`
- switching to English does not change the canonical key used in requests
