# API

## Overview

The frontend communicates with the backend through a REST API rooted at `/api`.

Most endpoints accept the optional `lang` query parameter.

Supported values:

- `en`
- `de`

Language behavior:

- `lang=en` returns English display labels where available
- any other value, or an omitted value, is normalized to German on the backend
- canonical keys remain German even in English UI mode

## GET /api/products

Returns the product list shown in the main sidebar.

### Query Parameters

- `lang`
  Optional display language.

### Response

```json
[
  {
    "key": "Sahne",
    "name": "Cream",
    "categoryKey": "Butter",
    "category": "Dairy",
    "variantCount": 1,
    "minIngredientCount": 1,
    "maxIngredientCount": 1,
    "price": "12,50"
  }
]
```

### Field Meaning

- `key`
  Canonical product key used by the backend.

- `name`
  Localized display name.

- `categoryKey`
  Canonical category key.

- `category`
  Localized category name.

- `variantCount`
  Number of available recipe variants.

- `minIngredientCount`
  Minimum number of filled ingredient slots across all variants.

- `maxIngredientCount`
  Maximum number of filled ingredient slots across all variants.

- `price`
  Formatted display price or `null`.

## GET /api/categories

Returns all known categories, including categories that do not yet have any recipe assigned.

### Query Parameters

- `lang`
  Optional display language.

### Response

```json
[
  {
    "key": "Erweiterte Kuchen",
    "name": "Advanced Cakes"
  }
]
```

## GET /api/ingredients/catalog

Returns the catalog used by the recipe builder for existing ingredients and existing craftable products.

### Query Parameters

- `lang`
  Optional display language.

### Response

```json
[
  {
    "key": "Sahne",
    "name": "Cream",
    "craftable": true
  },
  {
    "key": "Frostkristall",
    "name": "Frost Crystal",
    "craftable": false
  }
]
```

### Field Meaning

- `key`
  Canonical ingredient or product key.

- `name`
  Localized display name.

- `craftable`
  `true` if the item is a recipe in the dataset, otherwise `false`.

## GET /api/graph

Returns the dependency graph for a selected product.

### Query Parameters

- `product`
  Required canonical product key.

- `lang`
  Optional display language.

### Response

```json
{
  "id": "product-1",
  "key": "Sahne",
  "label": "Cream",
  "type": "product",
  "detail": "Price 12,50",
  "children": []
}
```

### Node Types

- `product`
- `variant`
- `slot`
- `raw`
- `cycle`

### Graph Rules

- `product` nodes may contain `variant` or `slot` children
- `variant` nodes contain `slot` children
- `slot` nodes contain `product`, `raw`, or `cycle` children
- `raw` nodes are terminal ingredients
- `cycle` nodes stop recursive expansion when a loop is detected

## GET /api/ingredients/search

Searches for products whose recursive ingredient set contains the given text.

### Query Parameters

- `query`
  Required search string.

- `lang`
  Optional display language.

### Response

```json
[
  {
    "key": "Alarmierende Torte",
    "name": "Startling Fancy",
    "categoryKey": "Erweiterte Kuchen",
    "category": "Advanced Cakes",
    "variantCount": 1,
    "matches": ["Cream", "Proto-Batter"],
    "price": null
  }
]
```

## PUT /api/prices

Creates, updates, or removes a product price in `data/product-prices.json`.

### Query Parameters

- `lang`
  Optional language for localized validation errors.

### Request Body

```json
{
  "key": "Sahne",
  "price": "12,5"
}
```

### Behavior

- if `price` contains a valid number, the price is stored
- if `price` is blank, the existing price is removed
- the `key` must identify a known product

### Response

```json
{
  "key": "Sahne",
  "price": "12,50"
}
```

## POST /api/categories

Creates a new category and persists it to `data/recipes.json`.

### Query Parameters

- `lang`
  Optional language for localized responses and validation errors.

### Request Body

```json
{
  "germanName": "Neue Kategorie",
  "englishName": "New Category"
}
```

### Response

```json
{
  "key": "Neue Kategorie",
  "name": "New Category"
}
```

Notes:

- the response `name` is localized according to `lang`
- categories are stored even if no recipe uses them yet

## POST /api/recipes

Creates a new recipe and persists it to `data/recipes.json`.

The request supports:

- German and English names for the root recipe
- an existing category
- multiple variants
- at most three ingredients per variant
- existing ingredients
- new raw ingredients
- nested sub-recipes

### Query Parameters

- `lang`
  Optional language for localized responses and validation errors.

### Request Body

```json
{
  "germanName": "Testrezept",
  "englishName": "Test Recipe",
  "categoryKey": "Suppen",
  "variants": [
    {
      "ingredients": [
        {
          "position": 1,
          "type": "existing",
          "existingKey": "Sahne"
        },
        {
          "position": 2,
          "type": "new_raw",
          "germanName": "Neue Zutat",
          "englishName": "New Ingredient"
        },
        {
          "position": 3,
          "type": "new_recipe",
          "recipe": {
            "germanName": "Teilrezept",
            "englishName": "Sub Recipe",
            "categoryKey": "Suppen",
            "variants": [
              {
                "ingredients": [
                  {
                    "position": 1,
                    "type": "existing",
                    "existingKey": "Frostkristall"
                  }
                ]
              }
            ]
          }
        }
      ]
    }
  ]
}
```

### Response

```json
{
  "key": "Testrezept",
  "name": "Test Recipe",
  "createdRecipeCount": 2
}
```

### Field Meaning

- `key`
  Canonical key of the saved root recipe.

- `name`
  Localized display name of the saved root recipe.

- `createdRecipeCount`
  Number of new recipe definitions written to the dataset, including nested sub-recipes.

## Error Handling

Typical status codes:

- `400 Bad Request`
  Returned for invalid price updates, invalid category creation, and invalid recipe drafts.

- `404 Not Found`
  Returned by `/api/graph` when the product key is unknown.

Localized validation messages are provided through `LocalizationService`.
