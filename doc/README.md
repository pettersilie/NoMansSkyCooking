# NMS Recipe Graph Documentation

This directory contains the project documentation for the NMS Recipe Graph application.

## Documents

- [Architecture](./architecture.md)
  Explains the application structure, the main backend services, and the frontend responsibilities.

- [Data Format](./data-format.md)
  Describes the `recipes.json` dataset and the separate product price storage.

- [API](./api.md)
  Documents the HTTP endpoints used by the frontend.

- [User Guide](./user-guide.md)
  Describes the visible UI features and user workflows.

- [Operations](./operations.md)
  Covers local development, packaging, Docker, distribution, and dataset migration.

## Scope

The application is a Spring Boot web app that:

- loads recipe definitions from `recipes.json`
- stores product prices in a separate JSON file
- exposes a small REST API
- renders an interactive dependency graph in the browser
- supports German and English in the UI

## Source Of Truth

For runtime behavior, the active data source is `recipes.json`.


