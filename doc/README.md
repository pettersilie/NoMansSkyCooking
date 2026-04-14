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
  Describes the application structure, runtime flow, and main backend and frontend components.

- [Data Format](./data-format.md)
  Explains the structure and semantics of `data/recipes.json` and the separate price file.

- [API](./api.md)
  Documents the REST endpoints used by the frontend and external tools.

- [User Guide](./user-guide.md)
  Covers the visible UI, including browsing, searching, price editing, category creation, and recipe creation.

- [Operations](./operations.md)
  Covers local development, packaging, Docker, runtime files, and operational recommendations.

## Scope

The application is a Spring Boot web app that:

- reads and writes recipes through `data/recipes.json`
- keeps prices in `data/product-prices.json`
- exposes a REST API under `/api`
- renders an interactive dependency graph in the browser
- supports German and English UI modes

## Source Of Truth

`data/recipes.json` is the single source of truth for categories, term translations, and recipes.
