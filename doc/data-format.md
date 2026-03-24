# Data Format

## Runtime Dataset

The application runtime uses `recipes.json` as its primary recipe dataset.

This file is a JSON array of recipe definitions.

## Structure

Each entry has this shape:

```json
{
  "name": "Alarmierende Torte",
  "category": "Erweiterte Kuchen",
  "variants": [
    [
      ["Proto-Teig"],
      ["Immerbrennende Marmelade"],
      ["Sahne", "Knochensahne"]
    ]
  ]
}
```

## Meaning Of The Structure

- the outer array is the full product catalog
- `name` is the canonical product name
- `category` is the canonical category name
- `variants` is a list of alternative recipes for the product
- each variant is a list of ingredient slots
- each slot is a list of one or more ingredient options

## Semantics

In one variant:

- different slots mean AND
- multiple values inside one slot mean OR

Example:

```json
[
  ["Verfeinertes Mehl"],
  ["Kreaturenei", "Große Eier", "Riesenei"],
  ["Gesüßte Butter", "Honigbutter"]
]
```

This means:

- slot 1 is mandatory and requires `Verfeinertes Mehl`
- slot 2 accepts one of `Kreaturenei`, `Große Eier`, or `Riesenei`
- slot 3 accepts one of `Gesüßte Butter` or `Honigbutter`

## Canonical Names

The JSON dataset keeps the original canonical names from the source data.

This is intentional:

- recipe lookup stays stable
- internal references do not depend on UI language
- localization can change independently of the dataset

## Price Storage

Prices are stored separately in `product-prices.json`.

The file structure is a simple object:

```json
{
  "Sahne": "12.5",
  "Alarmierende Torte": "39"
}
```

Notes:

- keys are canonical product names
- values are stored as normalized numeric strings
- displayed prices are formatted by the backend

## In-Memory Transformation

At runtime, the JSON dataset is converted into:

- `RecipeBook`
- `RecipeDefinition`
- `RecipeVariant`
- `IngredientSlot`

The JSON file itself is intentionally compact and optimized for data maintenance, not for direct graph rendering.
