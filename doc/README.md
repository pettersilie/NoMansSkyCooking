# Documentation

This directory contains the project documentation for the NMS Recipe Graph application.

## Reading Order

If you are new to the project, this order works well:

1. [Architecture](./architecture.md)
2. [Data Format](./data-format.md)
3. [API](./api.md)
4. [User Guide](./user-guide.md)
5. [Operations](./operations.md)

## Document Overview

- [Architecture](./architecture.md)
  Describes the application structure, runtime flow, and the main backend and frontend components for both cooking and refinery data.

- [Data Format](./data-format.md)
  Explains the structure and semantics of `data/recipes.json`, `data/refinery-recipes.json`, and the separate price file.

- [API](./api.md)
  Documents the REST endpoints used by the frontend and external tools, including `/api` for cooking and `/api/refinery` for refinery data.

- [User Guide](./user-guide.md)
  Covers the visible UI, including menu navigation, graph pages, overview pages, price editing, category creation, and recipe creation.

- [Operations](./operations.md)
  Covers local development, packaging, Docker, runtime files, and operational recommendations.

## Scope

The application is a Spring Boot web app that:

- reads and writes cooking recipes through `data/recipes.json`
- reads refinery processes through `data/refinery-recipes.json`
- keeps cooking prices in `data/product-prices.json`
- exposes REST APIs under `/api` and `/api/refinery`
- renders interactive dependency graphs and overview tables in the browser
- supports English and German UI modes, with English as the default

## Source Of Truth

- `data/recipes.json` is the single source of truth for cooking categories, cooking recipes, and cooking term translations.
- `data/refinery-recipes.json` is the single source of truth for refinery categories, refinery processes, and refinery term translations.
