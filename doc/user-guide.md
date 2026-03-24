# User Guide

## Purpose

The application helps you inspect how a No Man's Sky food product is built from ingredients and intermediate products.

## Sidebar

The left panel contains the main controls:

- language selector
- product filter
- product sort mode
- category filter
- product list
- ingredient search
- legend

## Language Selection

The UI supports:

- English
- German

Changing the language updates:

- UI labels
- product names
- category names
- graph slot labels
- search result labels

## Product List

The product list shows:

- product name
- number of variants
- minimum and maximum ingredient slot count
- optional price

Each product entry also contains a small price input field.

## Sorting

The product list can be sorted by:

- category
- name
- price

## Category Filter

The category filter lets you restrict the visible product list to one category only.

## Product Search

Use the product filter field to narrow the sidebar list by product name.

Pressing Enter loads the first visible match.

## Ingredient Search

Use the ingredient search field to find all products whose recursive ingredient set contains the entered text.

The search works across full dependency chains, not only direct ingredients.

Each result can be clicked to load the full recipe graph.

## Graph View

The graph is rendered top-down.

Rules:

- product cards represent craftable products
- variant nodes represent alternative recipes
- ingredient columns represent ingredient slots
- multiple options within one slot are shown as OR alternatives

## Expand And Collapse

Products with sub-recipes can be expanded and collapsed.

Behavior:

- the root product opens automatically
- nested craftable products start collapsed
- toggling a product recalculates the connector layout

## Navigation

In the graph area:

- use normal scrolling for standard navigation
- hold the right mouse button to pan horizontally and vertically

## Prices

To store a price:

1. enter a value into the product row
2. press Enter

To remove a price:

1. clear the value
2. press Enter

The saved price is then visible:

- in the product list
- in ingredient search results
- inside the graph node detail

## Clicking Product Nodes

Non-root product nodes inside the graph can be clicked.

This loads the clicked product as the new root and rebuilds the graph around it.
