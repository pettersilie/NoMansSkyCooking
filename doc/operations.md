# Operations

## Local Development

Start the application:

```bash
mvn spring-boot:run
```

Default URL:

```text
http://localhost:9999
```

## Configuration

Relevant configuration keys:

```yaml
server:
  port: 9999

recipes:
  source-path: ./data/recipes.json
  refinery-path: ./data/refinery-recipes.json
  price-path: ./data/product-prices.json
```

Operational expectations:

- `recipes.source-path` should point to the cooking dataset
- `recipes.refinery-path` should point to the refinery dataset
- `recipes.price-path` should point to the separate cooking price storage file
- there is no longer a project-root fallback recipe file
- the application expects the refinery dataset to exist at startup

## Build

Build the application and distribution package:

```bash
mvn clean package
```

Typical outputs:

- `target/nms-recipes.jar`
- `target/nms-recipes-dist.zip`
- `target/nms-recipes-dist/`

## Distribution Layout

The packaged distribution contains:

- `nms-recipes.jar`
- `data/recipes.json`
- `data/refinery-recipes.json`
- `data/product-prices.json`
- `start.cmd`
- `start.sh`
- `README.txt`

## Start Scripts

Windows:

```bat
start.cmd
```

Linux or macOS:

```sh
./start.sh
```

The scripts pass all three data paths explicitly so the application always starts against the packaged cooking dataset, refinery dataset, and price file.

## Docker

Build the image:

```bash
docker build -t nms-recipes .
```

Run the container:

```bash
docker run --rm -p 9999:9999 nms-recipes
```

The image contains:

- the application JAR
- `data/recipes.json`
- `data/refinery-recipes.json`
- `data/product-prices.json`

Important note:

- cooking authoring and cooking price editing write to files inside `/app/data`
- refinery data is shipped inside `/app/data` and loaded at startup
- mount `/app/data` from the host if you need persistence across container recreation

Example:

```bash
docker run --rm -p 9999:9999 -v "$(pwd)/data:/app/data" nms-recipes
```

## Runtime Files

Runtime state consists of three JSON files:

- `data/recipes.json`
- `data/refinery-recipes.json`
- `data/product-prices.json`

Recommended handling:

- treat `data/recipes.json` as the authoritative cooking dataset
- treat `data/refinery-recipes.json` as the authoritative refinery dataset
- version-control both datasets if content maintenance is part of your workflow
- back them up before bulk edits or imports
- keep `data/product-prices.json` separate for cooking price management

## Validation And Tests

Run tests:

```bash
mvn test
```

Recommended checks after changes:

- `mvn test`
- `mvn -DskipTests package`

## Operational Notes

- product keys are canonical German names from the persisted datasets
- display names are localized at runtime
- cooking prices are formatted with German-style decimal formatting by the backend
- both graph types are rebuilt server-side for each selected root product
- adding cooking categories or cooking recipes from the UI immediately mutates `data/recipes.json`
- refinery data is currently browsed from `data/refinery-recipes.json` but not edited from the UI
