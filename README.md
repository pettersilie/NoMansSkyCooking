# NMS Recipe Graph

NMS Recipe Graph is a Spring Boot web application for browsing, editing, and extending No Man's Sky cooking recipes.

It provides:

- a bilingual German/English UI
- a graph view for recipe dependencies
- product filtering, sorting, category filtering, and recursive ingredient search
- price editing in a separate JSON file
- authoring workflows for new categories and new recipes

The main dataset lives in [`data/recipes.json`](./data/recipes.json). Product prices are stored separately in `data/product-prices.json`.

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
- `./data/product-prices.json`

You can override them through Spring configuration:

```yaml
recipes:
  source-path: C:/path/to/data/recipes.json
  price-path: C:/path/to/data/product-prices.json

server:
  port: 9999
```

Notes:

- `data/recipes.json` is the single source of truth for categories, term translations, and recipes.
- `data/product-prices.json` remains a separate file and is not merged into the recipe dataset.
- if the price file does not exist yet, it is created automatically on first save

## Core Workflows

On the main page you can:

- browse the full product catalog
- filter by name and category
- sort by category, name, or price
- search by ingredient across recursive dependency chains
- open the graph for any craftable product
- edit stored prices

You can also create data directly from the UI:

- `Add category` opens a page that stores a new category with German and English names
- `Add recipe` opens a recipe builder that supports multiple variants, up to three ingredients per variant, existing ingredients, new raw ingredients, and nested sub-recipes

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
- `data/product-prices.json`

If you want recipe or price changes made through the UI to survive container recreation, mount `/app/data` from the host.

## Data Model

The graph model uses these rules:

- product nodes represent craftable products
- variant nodes represent alternative recipes
- ingredient slots represent AND relationships
- multiple values inside one slot represent OR relationships
