# Architecture

## Overview

NMS Recipe Graph is a server-rendered static frontend plus REST backend application built with Spring Boot.

At a high level:

1. The backend loads recipe definitions from `recipes.json`.
2. The backend stores product prices in a separate JSON file.
3. The frontend fetches localized product lists, ingredient search results, and graph trees from the backend.
4. The frontend renders the dependency structure as an interactive top-down graph.

## Main Runtime Components

## Backend

- `de.nms.nmsrecipes.NmsRecipesApplication`
  Spring Boot entry point.

- `de.nms.nmsrecipes.config.RecipeProperties`
  Holds the configured paths for the recipe dataset and the product price storage.

- `de.nms.nmsrecipes.service.JsonRecipeBookStore`
  Loads and writes the recipe dataset in JSON format.

- `de.nms.nmsrecipes.service.RecipeCatalogService`
  Loads the dataset at startup, resolves canonical product names, provides recipe definitions, and performs ingredient search.

- `de.nms.nmsrecipes.service.RecipeGraphService`
  Expands a selected product into a graph tree for frontend rendering.

- `de.nms.nmsrecipes.service.ProductPriceService`
  Loads, validates, formats, and persists product prices in a separate JSON file.

- `de.nms.nmsrecipes.service.LocalizationService`
  Localizes labels, categories, terms, and selected error messages for German and English.

- `de.nms.nmsrecipes.service.EnglishTerminology`
  Contains the German-to-English No Man's Sky terminology mapping.

- `de.nms.nmsrecipes.web.RecipeController`
  Exposes the REST API used by the frontend.

## Frontend

The frontend is a static application served by Spring Boot from `src/main/resources/static`.

- `index.html`
  Main layout and static shell of the application.

- `app.js`
  Fetches API data, manages UI state, localization, filtering, and graph rendering.

- `app.css`
  Defines the visual layout and graph presentation.

## Data Flow

## Startup

1. Spring Boot starts.
2. `RecipeCatalogService` resolves the configured `recipes.source-path`.
3. `JsonRecipeBookStore` loads the dataset into a `RecipeBook`.
4. `ProductPriceService` loads persisted prices from `recipes.price-path`.
5. The web UI becomes available.

## Product Selection

1. The frontend loads `/api/products`.
2. The user selects a product from the list.
3. The frontend requests `/api/graph?product=<key>&lang=<lang>`.
4. `RecipeGraphService` recursively expands the selected product.
5. The frontend renders the result as a top-down dependency graph.

## Ingredient Search

1. The user enters a search term.
2. The frontend calls `/api/ingredients/search?query=<text>&lang=<lang>`.
3. `RecipeCatalogService` recursively collects ingredient names for each product.
4. Matching products are returned and displayed in the sidebar.

## Price Editing

1. The user edits a price in the product list.
2. The frontend sends `PUT /api/prices`.
3. `ProductPriceService` validates and persists the value.
4. The frontend refreshes the visible list and graph.

## Domain Model

The backend uses a small in-memory domain model:

- `RecipeBook`
  A keyed collection of all product definitions.

- `RecipeDefinition`
  One craftable product, including category and recipe variants.

- `RecipeVariant`
  One alternative way to produce a product.

- `IngredientSlot`
  One ingredient column within a variant.

- `TreeNode`
  A view model for graph rendering.

## Graph Semantics

The recipe structure is not treated as a pure tree internally, because products may be reused and recipes may contain alternatives.

Rendering rules:

- separate ingredient columns represent AND relationships
- multiple entries inside one slot represent OR relationships
- products with multiple recipes get explicit variant nodes
- raw ingredients become leaf nodes
- cycles are detected and rendered as cycle nodes instead of expanding endlessly

## Localization Model

The internal dataset keeps canonical product names from the source data.

Important consequence:

- API keys remain stable and are based on canonical product names
- localized display names are generated at runtime
- English UI mode changes labels and product names, but does not change the canonical identifier used in requests
