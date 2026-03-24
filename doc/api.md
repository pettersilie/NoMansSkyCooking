# API

## Overview

The frontend communicates with the backend through a small REST API under `/api`.

All endpoints are language-aware through the optional `lang` query parameter.

Supported values:

- `en`
- `de`

If `lang` is omitted, the backend falls back to German internally, while the frontend defaults to English and usually sends `lang=en`.

## GET /api/products

Returns the product list used by the sidebar.

### Query Parameters

- `lang`
  Optional UI language.

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
  Canonical product identifier.

- `name`
  Localized display name.

- `categoryKey`
  Canonical category identifier.

- `category`
  Localized category label.

- `variantCount`
  Number of recipe variants.

- `minIngredientCount`
  Minimum number of filled ingredient slots across all variants.

- `maxIngredientCount`
  Maximum number of filled ingredient slots across all variants.

- `price`
  Formatted display price or `null`.

## GET /api/graph

Returns the dependency graph for one selected product.

### Query Parameters

- `product`
  Required canonical product key.

- `lang`
  Optional UI language.

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

### Node Rules

- `product` nodes may contain `variant` or `slot` children
- `variant` nodes contain `slot` children
- `slot` nodes contain `product`, `raw`, or `cycle` children
- `raw` nodes are terminal ingredients
- `cycle` nodes stop recursive expansion when a loop is detected

## GET /api/ingredients/search

Searches for products whose full ingredient closure contains the requested ingredient text.

### Query Parameters

- `query`
  Required search string.

- `lang`
  Optional UI language.

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

Creates, updates, or removes a product price.

### Query Parameters

- `lang`
  Optional UI language for error messages.

### Request Body

```json
{
  "key": "Sahne",
  "price": "12,5"
}
```

### Behavior

- if `price` contains a valid number, the value is stored
- if `price` is blank, the existing price is removed
- the product key must refer to a known product definition

### Response

```json
{
  "key": "Sahne",
  "price": "12,50"
}
```

## Error Handling

Typical errors:

- `404 Not Found`
  Returned by `/api/graph` when the product key is unknown.

- `400 Bad Request`
  Returned by `/api/prices` when the price is invalid.

The backend localizes selected error messages through `LocalizationService`.
