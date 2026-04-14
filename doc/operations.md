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
  price-path: ./data/product-prices.json
```

Operational expectations:

- `recipes.source-path` should point to the single recipe dataset
- `recipes.price-path` should point to the separate price storage file
- there is no longer a project-root fallback recipe file

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

The scripts pass both data paths explicitly so the application always starts against the packaged dataset and price file.

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
- `data/product-prices.json`

Important note:

- both UI recipe authoring and UI price editing write to files inside `/app/data`
- mount `/app/data` from the host if you need persistence across container recreation

Example:

```bash
docker run --rm -p 9999:9999 -v "$(pwd)/data:/app/data" nms-recipes
```

## Runtime Files

Runtime state consists of two JSON files:

- `data/recipes.json`
- `data/product-prices.json`

Recommended handling:

- treat `data/recipes.json` as the authoritative recipe dataset
- version-control it if recipe authoring is part of your workflow
- back it up before bulk edits or imports
- keep `data/product-prices.json` separate for price management

## Validation And Tests

Run tests:

```bash
mvn test
```

Recommended checks after changes:

- `mvn test`
- `mvn -DskipTests package`

## Operational Notes

- product keys are canonical German names from the dataset
- display names are localized at runtime
- the backend formats display prices with German-style decimal formatting
- the graph is rebuilt server-side for each selected root product
- adding categories or recipes from the UI immediately mutates `data/recipes.json`
