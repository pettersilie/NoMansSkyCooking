# User Guide

## Purpose

NMS Recipe Graph helps you inspect how No Man's Sky cooking products and refinery outputs are built from ingredients, intermediate products, and alternative production paths.

## Navigation

The main menu is available on every page. It contains:

- `Cooking Recipes`
- `Cooking Recipe Overview`
- `Refinery`
- `Refinery Overview`
- `Add cooking category`
- `Add cooking recipe`

The application opens in English by default. German can be selected from the language picker in the menu.

## Language Selection

The UI supports:

- English
- German

Changing the language updates:

- menu labels
- page titles and helper text
- cooking product names
- refinery product names
- category names
- ingredient labels
- graph node labels
- overview table labels
- search result labels
- authoring page labels

Canonical API keys remain German in both language modes.

## Cooking Recipes Page

The `Cooking Recipes` page is the main cooking workspace.

The left panel contains:

- the cooking product filter
- sort mode
- category filter
- product list with optional price inputs
- ingredient search
- a graph legend

The right side contains the graph canvas.

### Product List

Each cooking product row shows:

- localized product name
- variant count
- minimum and maximum ingredient slot count
- optional stored price per 100 units

Selecting a product loads it as the root of the graph.

### Sorting And Filtering

The cooking list supports:

- filtering by product name
- filtering by category
- sorting by category, name, or price

Pressing Enter in the product filter loads the first visible result.

### Ingredient Search

The cooking ingredient search checks the recursive ingredient closure of each cooking product, not just direct ingredients.

Search results display:

- product name
- category
- variant count
- matching ingredient labels
- optional price

Selecting a result loads that product as the graph root.

### Graph View

The cooking graph is rendered top-down.

Node types:

- product
- variant
- ingredient slot
- raw ingredient
- cycle marker

Rules:

- products can expand into direct ingredient slots or variant nodes
- variants represent alternative recipes
- slots represent ingredient positions
- multiple entries inside one slot represent OR alternatives

### Expanding, Collapsing, And Navigation

Graph behavior:

- the selected root product opens automatically
- nested craftable products start collapsed
- clicking the toggle expands or collapses a product branch
- clicking a non-root product card reloads it as the new root product

Navigation behavior:

- use normal scrolling for standard page navigation
- hold the right mouse button in the graph canvas to pan horizontally and vertically

### Editing Prices

Only cooking products support price editing.

Prices are stored and displayed per 100 units.

To save a price:

1. enter a value in the product row
2. press Enter

To remove a price:

1. clear the input
2. press Enter

Saved prices appear in:

- the product list
- ingredient search results
- graph node detail text
- the cooking recipe overview

## Cooking Recipe Overview

The `Cooking Recipe Overview` page shows every cooking recipe variant in a table.

Each row contains:

- the target cooking product
- the localized cooking category
- top-level ingredient 1
- top-level ingredient 2
- top-level ingredient 3
- stored price per 100 units

You can:

- filter by ingredient text
- filter by category
- sort by target product
- sort by price

Filtering checks the target product and the three top-level ingredient columns.

Clickable overview entries:

- the target product opens the cooking detail page
- a craftable cooking ingredient opens the cooking detail page
- a craftable refinery ingredient opens the refinery detail page

## Refinery Page

The `Refinery` page is the refinery equivalent of the cooking graph page.

The left panel contains:

- the refinery product filter
- sort mode
- category filter
- refinery output list
- refinery ingredient search
- a graph legend

The right side contains the refinery graph canvas.

### Refinery List

Each refinery row shows:

- localized output name
- variant count
- minimum and maximum ingredient count

There is no refinery price editing.

### Sorting And Filtering

The refinery list supports:

- filtering by product name
- filtering by category
- sorting by category or name

### Refinery Ingredient Search

The refinery ingredient search checks the recursive refinery dependency closure of each output.

Search results display:

- product name
- category
- variant count
- matching ingredient labels

Selecting a result loads that output as the graph root.

### Refinery Graph View

The refinery graph is also rendered top-down.

Node types:

- product
- variant
- ingredient slot
- raw ingredient
- cycle marker

Additional refinery behavior:

- ingredients may show quantities
- repeated craftable ingredients remain expandable, even when their deeper branch is loaded lazily
- refinery process detail labels do not show output/time helper prefixes in the UI

## Refinery Overview

The `Refinery Overview` page shows every refinery variant in a table.

Each row contains:

- the target refinery product
- the localized refinery category
- top-level ingredient 1
- top-level ingredient 2
- top-level ingredient 3

Ingredient quantities are included inline when applicable, for example `2 x Cursed Dust`.

You can:

- filter by ingredient text
- filter by category
- sort by target product

The refinery overview does not include a price column.

Clickable overview entries:

- the target product opens the refinery detail page
- a craftable refinery ingredient opens the refinery detail page
- a craftable cooking ingredient opens the cooking detail page

## Adding A Cooking Category

Use the `Add cooking category` menu entry to open the category form.

The page requires:

- a German category name
- an English category name

When you save:

- the new category is written to `data/recipes.json`
- it becomes available in the cooking category list immediately
- it can be used by newly created cooking recipes even if no recipe currently belongs to it

## Adding A Cooking Recipe

Use the `Add cooking recipe` menu entry to open the recipe builder.

The root recipe requires:

- a German recipe name
- an English recipe name
- an existing cooking category

Recipe builder capabilities:

- multiple recipe variants
- up to three ingredient positions per variant
- empty ingredient slots
- existing ingredients or craftable products
- brand-new raw ingredients with German and English names
- nested sub-recipes with their own German and English names
- searchable dropdown support for existing ingredients

When you save:

- the root recipe is written to `data/recipes.json`
- nested sub-recipes are also written as regular recipe entries
- new raw ingredient translations are added to the cooking dataset
- the return link points back to the calling page

## Data Persistence

Runtime data uses three separate files:

- cooking categories, cooking recipes, and cooking term translations are stored in `data/recipes.json`
- refinery categories, refinery processes, and refinery term translations are stored in `data/refinery-recipes.json`
- cooking prices are stored in `data/product-prices.json`

Current write behavior:

- the UI can create cooking categories and cooking recipes
- the UI can update cooking prices
- refinery data is currently read-only in the UI
