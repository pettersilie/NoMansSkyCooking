# User Guide

## Purpose

NMS Recipe Graph helps you inspect how a No Man's Sky food item is built from ingredients, intermediate products, and alternative recipe paths.

## Main Page

The left panel contains the primary controls:

- language selector
- `Add recipe` button
- `Add category` button
- product filter
- product sort mode
- category filter
- product list with price inputs
- ingredient search
- legend

The right side contains the graph canvas.

## Language Selection

The UI supports:

- English
- German

Changing the language updates:

- UI labels
- product names
- category names
- ingredient labels
- graph node labels
- search result labels
- the labels on the add-category and add-recipe pages

## Product List

Each product row shows:

- localized product name
- variant count
- minimum and maximum ingredient slot count
- optional stored price

Selecting a product loads it as the root of the graph.

## Sorting And Filtering

The main list supports:

- filtering by product name
- filtering by category
- sorting by category, name, or price

Pressing Enter in the product filter loads the first visible result.

## Ingredient Search

The ingredient search checks the recursive ingredient closure of each product, not just direct ingredients.

Search results display:

- product name
- category
- variant count
- matching ingredient labels
- optional price

Selecting a result loads that product as the graph root.

## Graph View

The graph is rendered top-down.

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

## Expanding, Collapsing, And Navigation

Graph behavior:

- the selected root product opens automatically
- nested craftable products start collapsed
- clicking the toggle expands or collapses a product branch
- clicking a non-root product card reloads it as the new root product

Navigation behavior:

- use normal scrolling for standard page navigation
- hold the right mouse button in the graph canvas to pan horizontally and vertically

## Editing Prices

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

## Adding A Category

Use the `Add category` button on the main page to open the category form.

The page requires:

- a German category name
- an English category name

When you save:

- the new category is written to `data/recipes.json`
- it becomes available in the category list immediately
- it can be used by newly created recipes even if no recipe currently belongs to it

## Adding A Recipe

Use the `Add recipe` button on the main page to open the recipe builder.

The root recipe requires:

- a German recipe name
- an English recipe name
- an existing category

Recipe builder capabilities:

- multiple recipe variants
- up to three ingredient positions per variant
- empty ingredient slots
- existing ingredients or craftable products
- brand-new raw ingredients with German and English names
- nested sub-recipes with their own German and English names

When you save:

- the root recipe is written to `data/recipes.json`
- nested sub-recipes are also written as regular recipe entries
- new raw ingredient translations are added to the dataset
- the return link points back to the main page and can preload the saved recipe as the selected product

## Data Persistence

Authoring actions use two separate files:

- recipes, categories, and term translations are stored in `data/recipes.json`
- prices are stored in `data/product-prices.json`

This separation allows price maintenance without mixing price data into the recipe dataset.
