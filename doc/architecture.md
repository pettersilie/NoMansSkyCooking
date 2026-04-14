# Architecture

## Overview

NMS Recipe Graph is a Spring Boot application that serves a static frontend and a small REST API.

At runtime:

1. the backend loads the recipe dataset from `data/recipes.json`
2. the backend loads prices from `data/product-prices.json`
3. the frontend requests localized data through `/api`
4. the frontend renders an interactive dependency graph and authoring forms

`data/recipes.json` is the single source of truth for categories, term translations, and recipes.

## Runtime Layers

### Backend

- `de.nms.nmsrecipes.NmsRecipesApplication`
  Spring Boot entry point.

- `de.nms.nmsrecipes.config.RecipeProperties`
  Holds the configured paths for the recipe dataset and the price file.

- `de.nms.nmsrecipes.service.JsonRecipeBookStore`
  Reads and writes the recipe dataset.

- `de.nms.nmsrecipes.service.RecipeCatalogService`
  Loads the in-memory recipe catalog, resolves canonical names, provides category and ingredient catalogs, handles ingredient search, and persists new categories and recipes.

- `de.nms.nmsrecipes.service.RecipeGraphService`
  Expands a selected product into the graph structure used by the UI.

- `de.nms.nmsrecipes.service.ProductPriceService`
  Validates, formats, and persists product prices in the separate price file.

- `de.nms.nmsrecipes.service.LocalizationService`
  Normalizes language selection and localizes terms, categories, graph labels, and selected validation messages.

- `de.nms.nmsrecipes.service.EnglishTerminology`
  Provides built-in German-to-English terminology mappings.

- `de.nms.nmsrecipes.web.RecipeController`
  Exposes the REST API for product browsing, graph loading, ingredient search, price editing, category creation, and recipe creation.

### Frontend

The frontend is served from `src/main/resources/static`.

- `index.html`
  Main application shell.

- `app.js`
  Main-page behavior, product loading, graph rendering, filtering, searching, localization, and price editing.

- `add-category.html` and `add-category.js`
  Category authoring UI.

- `add-recipe.html` and `add-recipe.js`
  Recipe builder UI with variant and nested sub-recipe support.

- `app.css`
  Shared styling for the main page and authoring pages.

## Main Data Flow

### Startup

1. Spring Boot starts.
2. `RecipeCatalogService` resolves `recipes.source-path`.
3. `JsonRecipeBookStore` reads `data/recipes.json` into a `RecipeBook`.
4. `ProductPriceService` reads `recipes.price-path`.
5. Static frontend assets become available.

### Product Browsing

1. The frontend requests `/api/products` and `/api/categories`.
2. The user filters or sorts the list.
3. Selecting a product triggers `/api/graph?product=<key>&lang=<lang>`.
4. `RecipeGraphService` builds a recursive graph tree.
5. The frontend renders product, variant, slot, raw, and cycle nodes.

### Ingredient Search

1. The user enters a search term.
2. The frontend calls `/api/ingredients/search`.
3. `RecipeCatalogService` computes the recursive ingredient closure per product.
4. Matching products are returned with localized names and matching ingredient labels.

### Price Editing

1. The user edits a price in the product list.
2. The frontend sends `PUT /api/prices`.
3. `ProductPriceService` validates and persists the value.
4. The frontend refreshes visible data.

### Category Creation

1. The user opens `add-category.html`.
2. The page submits `POST /api/categories`.
3. `RecipeCatalogService` adds the category to `RecipeBook`.
4. `JsonRecipeBookStore` writes the updated dataset back to `data/recipes.json`.

### Recipe Creation

1. The user opens `add-recipe.html`.
2. The page loads `/api/categories` and `/api/ingredients/catalog`.
3. The user builds a recipe draft with variants and ingredient slots.
4. The page submits `POST /api/recipes`.
5. `RecipeCatalogService` validates the draft, creates any nested sub-recipes, registers English term overrides, and saves the result to `data/recipes.json`.

## Domain Model

The backend centers around these types:

- `RecipeBook`
  In-memory catalog of recipes, categories, and English term/category overrides.

- `RecipeDefinition`
  One craftable product.

- `RecipeVariant`
  One alternative production path for a product.

- `IngredientSlot`
  One ingredient position within a variant.

- `RecipeDraft`
  Incoming API payload for a new recipe.

- `RecipeDraftVariant`
  One incoming draft variant.

- `RecipeDraftIngredient`
  One incoming draft ingredient reference, raw ingredient, or nested recipe.

- `TreeNode`
  Graph view model returned to the frontend.

## Graph Semantics

The graph is not treated as a strict tree internally because recipes can reuse products and ingredients.

Rendering rules:

- separate slots represent AND relationships
- multiple values inside one slot represent OR relationships
- products with multiple variants get explicit variant nodes
- raw ingredients are leaves
- cycles are rendered as dedicated cycle nodes instead of recursing forever

## Localization Model

The dataset keeps canonical German keys.

Important consequences:

- API keys stay stable regardless of UI language
- English labels are generated at runtime
- custom English labels for categories, recipes, and ingredients are stored in `data/recipes.json`
- switching to English does not change the canonical key used in requests
