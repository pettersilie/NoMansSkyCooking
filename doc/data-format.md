# Data Format

## Runtime Datasets

The application uses three runtime JSON files:

- `data/recipes.json` for cooking categories, cooking term translations, and cooking recipes
- `data/refinery-recipes.json` for refinery categories, refinery term translations, and refinery processes
- `data/product-prices.json` for cooking prices

The two production datasets are independent. Cooking authoring writes only to `data/recipes.json`. Refinery data is currently read-only in the UI.

## Common Dataset Shape

Both `data/recipes.json` and `data/refinery-recipes.json` use the same top-level object shape:

```json
{
  "categories": [],
  "terms": [],
  "recipes": []
}
```

### `categories`

Each category entry has:

- `name`
  Canonical German category key.

- `englishName`
  Optional English label used in English UI mode.

### `terms`

Each term entry has:

- `name`
  Canonical German term key.

- `englishName`
  English label used in English UI mode.

`terms` stores overrides for custom names and imported canonical names that need an explicit English mapping in this project.

## Cooking Dataset: `data/recipes.json`

`data/recipes.json` is the single source of truth for:

- cooking categories
- cooking recipe definitions
- cooking-specific English labels for categories, recipes, and ingredients

### Cooking Recipe Entry

Each cooking recipe entry has this structure:

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

### Cooking Variant Semantics

Within one cooking variant:

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

### Cooking Authoring Constraints

The current UI and backend enforce these rules for newly created cooking recipes:

- each recipe must have a German and English name
- each recipe must reference an existing cooking category
- a variant may contain at most three ingredient positions
- each position can be empty, reference an existing item, define a new raw ingredient, or define a nested sub-recipe
- nested sub-recipes are persisted as regular recipe entries in the same `recipes` array

## Refinery Dataset: `data/refinery-recipes.json`

`data/refinery-recipes.json` is the single source of truth for:

- refinery categories
- refinery process definitions
- refinery-specific English labels for categories, outputs, and ingredients

### Refinery Recipe Entry

Each refinery entry has this structure:

```json
{
  "name": "Restsubstanz",
  "category": "Schrott",
  "variants": [
    {
      "operation": "Gewünschter Vorgang: Untergangs-Zyklisierung",
      "englishOperation": "Requested Operation: Doom Cycling",
      "time": "0.04",
      "outputQuantity": 1,
      "ingredients": [
        {
          "position": 1,
          "name": "Verfluchter Staub",
          "quantity": 2
        }
      ]
    }
  ]
}
```

Field meaning:

- `name`
  Canonical German refinery output key.

- `category`
  Canonical German refinery category key.

- `variants`
  List of refinery process variants for this output.

### Refinery Variant Fields

Each refinery variant contains:

- `operation`
  Canonical German operation label as stored in the dataset.

- `englishOperation`
  English operation label used in English UI mode.

- `time`
  Source time metadata as stored in the dataset.

- `outputQuantity`
  Output quantity metadata as stored in the dataset.

- `ingredients`
  Ordered refinery inputs.

### Refinery Ingredient Fields

Each refinery ingredient contains:

- `position`
  One-based input position.

- `name`
  Canonical German input key.

- `quantity`
  Required amount for that input.

### Refinery Semantics

For refinery variants:

- different ingredient positions mean AND
- there is no OR structure inside one refinery input position in the persisted format
- quantities are part of the persisted data
- operation/time/output metadata remains in the file even when parts of it are intentionally hidden in the UI

## Canonical Naming Rules

Both datasets keep canonical German keys.

This is intentional:

- internal references stay stable
- API keys stay stable
- localization can be layered on top without rewriting references

English labels are produced at runtime from:

- built-in terminology mappings
- `categories[].englishName`
- `terms[].englishName`

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

- keys are canonical German cooking product names
- values are normalized numeric strings
- display formatting is applied by the backend
- refinery outputs do not use this price file

## Runtime Transformation

At runtime, the cooking dataset is transformed into:

- `RecipeBook`
- `RecipeDefinition`
- `RecipeVariant`
- `IngredientSlot`

At runtime, the refinery dataset is transformed into:

- `RefineryBook`
- `RefineryDefinition`
- `RefineryVariant`
- `RefineryIngredient`

The persisted JSON stays compact and authoring-oriented rather than graph-rendering-oriented.
