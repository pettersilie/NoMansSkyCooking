# Data Format

## Primary Dataset

The runtime recipe dataset is `data/recipes.json`.

This file is the single source of truth for:

- categories
- custom English labels for categories, recipes, and ingredients
- recipe definitions

The preferred runtime shape is a JSON object with `categories`, `terms`, and `recipes`.

## Preferred Object Shape

```json
{
  "categories": [
    {
      "name": "Erweiterte Kuchen",
      "englishName": "Advanced Cakes"
    },
    {
      "name": "Neue Kategorie",
      "englishName": "New Category"
    }
  ],
  "terms": [
    {
      "name": "Neue Zutat",
      "englishName": "New Ingredient"
    },
    {
      "name": "Neues Rezept",
      "englishName": "New Recipe"
    }
  ],
  "recipes": [
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
  ]
}
```

## Top-Level Sections

### `categories`

Each category entry has:

- `name`
  Canonical German category key.

- `englishName`
  Optional English label used in English UI mode.

Categories may exist even when no recipe currently references them. This is how standalone categories created from the UI are persisted.

### `terms`

Each term entry has:

- `name`
  Canonical German term key.

- `englishName`
  English label used in English UI mode.

`terms` stores overrides for:

- custom recipes
- custom raw ingredients
- imported canonical names that need explicit English mapping in this project

### `recipes`

Each recipe entry has this structure:

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

Field meaning:

- `name`
  Canonical German product key.

- `category`
  Canonical German category key.

- `variants`
  List of alternative ways to craft the product.

## Variant Semantics

Within one variant:

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

Meaning:

- slot 1 requires `Verfeinertes Mehl`
- slot 2 accepts one of `Kreaturenei`, `Große Eier`, or `Riesenei`
- slot 3 accepts one of `Gesüßte Butter` or `Honigbutter`

## Canonical Naming Rules

The dataset keeps canonical German keys.

This is intentional:

- internal references stay stable
- API keys stay stable
- localization can be layered on top without rewriting recipe references

English labels are produced at runtime from:

- built-in terminology mappings
- `categories[].englishName`
- `terms[].englishName`

## Recipe Authoring Constraints

The current UI and backend enforce these rules for newly created recipes:

- each recipe must have a German and English name
- each recipe must reference an existing category
- a variant may contain at most three ingredient positions
- each position can be empty, reference an existing item, define a new raw ingredient, or define a nested sub-recipe
- nested sub-recipes are persisted as regular recipe entries in the same `recipes` array

## Legacy Import Shape

For backward compatibility, the backend can still read a legacy top-level array of recipe definitions:

```json
[
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
]
```

This format is accepted for import, but the current application writes the object shape shown above.

## Price Storage

Prices are intentionally stored in a separate file: `data/product-prices.json`.

Structure:

```json
{
  "Sahne": "12.5",
  "Alarmierende Torte": "39"
}
```

Notes:

- keys are canonical German product names
- values are normalized numeric strings
- display formatting is applied by the backend

## Runtime Transformation

At runtime, `data/recipes.json` is transformed into:

- `RecipeBook`
- `RecipeDefinition`
- `RecipeVariant`
- `IngredientSlot`

The persisted JSON stays compact and authoring-oriented rather than graph-rendering-oriented.
