# API

## Overview

The frontend communicates with the backend through two REST API roots:

- `/api` for cooking data and authoring
- `/api/refinery` for refinery browsing

Most endpoints accept the optional `lang` query parameter.

Supported values:

- `en`
- `de`

Language behavior:

- `lang=en` returns English display labels where available
- any other value, or an omitted value, is normalized to German on the backend
- canonical keys remain German even in English UI mode

## Cooking API

### GET /api/products

Returns the cooking product list shown on the `Cooking Recipes` page.

#### Query Parameters

- `lang`
  Optional display language.

#### Response

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

### GET /api/categories

Returns all known cooking categories.

#### Query Parameters

- `lang`
  Optional display language.

#### Response

```json
[
  {
    "key": "Erweiterte Kuchen",
    "name": "Advanced Cakes"
  }
]
```

### GET /api/ingredients/catalog

Returns the cooking catalog used by the recipe builder for existing ingredients and existing craftable products.

#### Query Parameters

- `lang`
  Optional display language.

#### Response

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

### GET /api/graph

Returns the cooking dependency graph for a selected product.

#### Query Parameters

- `product`
  Required canonical product key.

- `lang`
  Optional display language.

#### Response

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

#### Node Types

- `product`
- `variant`
- `slot`
- `raw`
- `cycle`

### GET /api/ingredients/search

Searches for cooking products whose recursive ingredient set contains the given text.

#### Query Parameters

- `query`
  Required search string.

- `lang`
  Optional display language.

#### Response

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

### GET /api/recipes/overview

Returns one row per cooking recipe variant for the `Cooking Recipe Overview` page.

#### Query Parameters

- `lang`
  Optional display language.

#### Response

```json
[
  {
    "key": "Sahne",
    "name": "Cream",
    "variantIndex": 1,
    "ingredient1": "Fresh Milk",
    "ingredient2": "",
    "ingredient3": "",
    "price": "12,50"
  }
]
```

#### Notes

- each row represents one cooking variant
- `ingredient1` through `ingredient3` contain only the top-level slots of that variant
- `price` is formatted for display and may be `null`

### PUT /api/prices

Creates, updates, or removes a cooking product price in `data/product-prices.json`.

#### Query Parameters

- `lang`
  Optional language for localized validation errors.

#### Request Body

```json
{
  "key": "Sahne",
  "price": "12,5"
}
```

#### Behavior

- if `price` contains a valid number, the price is stored
- if `price` is blank, the existing price is removed
- the `key` must identify a known cooking product

#### Response

```json
{
  "key": "Sahne",
  "price": "12,50"
}
```

### POST /api/categories

Creates a new cooking category and persists it to `data/recipes.json`.

#### Query Parameters

- `lang`
  Optional language for localized responses and validation errors.

#### Request Body

```json
{
  "germanName": "Neue Kategorie",
  "englishName": "New Category"
}
```

#### Response

```json
{
  "key": "Neue Kategorie",
  "name": "New Category"
}
```

### POST /api/recipes

Creates a new cooking recipe and persists it to `data/recipes.json`.

The request supports:

- German and English names for the root recipe
- an existing category
- multiple variants
- at most three ingredients per variant
- existing ingredients
- new raw ingredients
- nested sub-recipes

#### Query Parameters

- `lang`
  Optional language for localized responses and validation errors.

#### Request Body

```json
{
  "germanName": "Test Recipe DE",
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

#### Response

```json
{
  "key": "Test Recipe DE",
  "name": "Test Recipe",
  "createdRecipeCount": 2
}
```

## Refinery API

### GET /api/refinery/products

Returns the refinery output list shown on the `Refinery` page.

#### Query Parameters

- `lang`
  Optional display language.

#### Response

```json
[
  {
    "key": "Restsubstanz",
    "name": "Residual Goop",
    "categoryKey": "Schrott",
    "category": "Junk",
    "variantCount": 1,
    "minIngredientCount": 1,
    "maxIngredientCount": 1
  }
]
```

### GET /api/refinery/categories

Returns all known refinery categories.

#### Query Parameters

- `lang`
  Optional display language.

#### Response

```json
[
  {
    "key": "Schrott",
    "name": "Junk"
  }
]
```

### GET /api/refinery/ingredients/catalog

Returns the refinery ingredient catalog used by the refinery UI.

#### Query Parameters

- `lang`
  Optional display language.

#### Response

```json
[
  {
    "key": "Restsubstanz",
    "name": "Residual Goop",
    "craftable": true
  },
  {
    "key": "Verfluchter Staub",
    "name": "Cursed Dust",
    "craftable": false
  }
]
```

### GET /api/refinery/graph

Returns the refinery dependency graph for a selected output.

#### Query Parameters

- `product`
  Required canonical refinery output key.

- `lang`
  Optional display language.

#### Response

```json
{
  "id": "product-1",
  "key": "Restsubstanz",
  "label": "Residual Goop",
  "type": "product",
  "detail": null,
  "children": []
}
```

#### Notes

- ingredient quantities may appear in node details
- refinery operation labels are localized before the graph is returned

### GET /api/refinery/ingredients/search

Searches for refinery outputs whose recursive ingredient set contains the given text.

#### Query Parameters

- `query`
  Required search string.

- `lang`
  Optional display language.

#### Response

```json
[
  {
    "key": "Restsubstanz",
    "name": "Residual Goop",
    "categoryKey": "Schrott",
    "category": "Junk",
    "variantCount": 1,
    "matches": ["Cursed Dust"]
  }
]
```

### GET /api/refinery/overview

Returns one row per refinery variant for the `Refinery Overview` page.

#### Query Parameters

- `lang`
  Optional display language.

#### Response

```json
[
  {
    "key": "Restsubstanz",
    "name": "Residual Goop",
    "variantIndex": 1,
    "ingredient1": "2 x Cursed Dust",
    "ingredient2": "",
    "ingredient3": ""
  }
]
```

#### Notes

- each row represents one refinery variant
- only top-level refinery inputs are returned
- there is no price field in the refinery overview

## Error Handling

Typical status codes:

- `400 Bad Request`
  Returned for invalid cooking price updates, invalid cooking category creation, and invalid cooking recipe drafts.

- `404 Not Found`
  Returned by `/api/graph` and `/api/refinery/graph` when the selected product key is unknown.

Localized validation messages are provided through `LocalizationService`.
