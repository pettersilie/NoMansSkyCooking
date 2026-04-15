# NMS Recipe Graph

NMS Recipe Graph is a Spring Boot web application for browsing and extending No Man's Sky cooking recipes and refinery processes.

It provides:

- a bilingual English/German UI with English as the default language
- a cooking graph view for recursive recipe dependencies
- a refinery graph view for recursive refinery dependencies
- sortable overview tables for cooking recipes and refinery processes
- cooking product filtering, category filtering, ingredient search, and price editing
- authoring workflows for new cooking categories and new cooking recipes

The runtime data is split across three JSON files:

- [`data/recipes.json`](./data/recipes.json) for cooking categories, term translations, and cooking recipes
- [`data/refinery-recipes.json`](./data/refinery-recipes.json) for refinery categories, term translations, and refinery processes
- `data/product-prices.json` for cooking product prices

Full project documentation is available in [`doc/`](./doc/README.md).

## Quick Start

Run the application locally:

```bash
mvn spring-boot:run
```

The default URL is `http://localhost:9999`.

## Configuration

The default runtime paths are:

- `./data/recipes.json`
- `./data/refinery-recipes.json`
- `./data/product-prices.json`

You can override them through Spring configuration:

```yaml
recipes:
  source-path: C:/path/to/data/recipes.json
  refinery-path: C:/path/to/data/refinery-recipes.json
  price-path: C:/path/to/data/product-prices.json

server:
  port: 9999
```

Notes:

- `data/recipes.json` is the single source of truth for cooking categories, cooking recipes, and cooking-specific English labels.
- `data/refinery-recipes.json` is the single source of truth for refinery categories, refinery processes, and refinery-specific English labels.
- `data/product-prices.json` remains separate and applies only to cooking products.
- if the price file does not exist yet, it is created automatically on first save

## Core Workflows

The main menu is available on every page. It includes:

- `Cooking Recipes`
- `Cooking Recipe Overview`
- `Refinery`
- `Refinery Overview`
- `Add cooking category`
- `Add cooking recipe`

Cooking features:

- browse the full cooking catalog
- filter by product name and category
- sort by category, name, or price
- search by ingredient across recursive dependency chains
- open the graph for any craftable cooking product
- edit stored prices for cooking products
- create new cooking categories and cooking recipes directly from the UI

Refinery features:

- browse the full refinery output catalog
- filter by product name and category
- sort by category or name
- search by ingredient across recursive refinery chains
- open the refinery dependency graph for any refinery output
- inspect a tabular refinery overview by target product and top-level ingredients

Overview pages:

- `Cooking Recipe Overview` lists every cooking recipe variant with target product, top-level ingredients, and price
- `Refinery Overview` lists every refinery variant with target product and top-level ingredients

## Build And Distribution

Create the runnable JAR and distribution package:

```bash
mvn clean package
```

The build creates:

- `target/nms-recipes.jar`
- `target/nms-recipes-dist.zip`
- `target/nms-recipes-dist/`

The packaged distribution contains:

- `nms-recipes.jar`
- `data/recipes.json`
- `data/refinery-recipes.json`
- `data/product-prices.json`
- `start.cmd`
- `start.sh`
- `README.txt`

Start the packaged distribution on Linux or macOS:

```bash
cd <unpacked-folder>/nms-recipes-dist
./start.sh
```

Start it on Windows:

```bat
cd <unpacked-folder>\nms-recipes-dist
start.cmd
```

## Docker

Build the image:

```bash
docker build -t nms-recipes .
```

Run the container:

```bash
docker run --rm -p 9999:9999 nms-recipes
```

The container image includes:

- the application JAR
- `data/recipes.json`
- `data/refinery-recipes.json`
- `data/product-prices.json`

If you want changes made through the UI to survive container recreation, mount `/app/data` from the host.

## Data Model

The application uses two recursive production domains:

- cooking recipes with variants and up to three ingredient slots
- refinery processes with variants, quantities, and up to three input positions

Both domains keep canonical German keys in the JSON data. English labels are layered on top at runtime.
