# NMS Recipe Graph

Lightweight Spring Boot prototype for reading a JSON recipe dataset for No Man's Sky cooking recipes and visualizing the dependencies as an interactive production tree.

The UI supports both German and English. Product, ingredient, and recipe names are translated to the official English No Man's Sky terminology through an internal mapping based on the public wiki and recipe references.

Full project documentation is available in [`doc/`](./doc/README.md).

## Development

```bash
mvn spring-boot:run
```

The application is then available at `http://localhost:9999`.

## Configuration

By default, the application loads `./data/recipes.json`. If that path does not exist, development mode falls back to `./recipes.json`.

You can override the paths through Spring configuration:

```yaml
recipes:
  source-path: C:/path/to/recipes.json
  price-path: C:/path/to/product-prices.json
```

Product prices are stored separately from the recipe dataset in `./data/product-prices.json`. If the file does not exist yet, it is created automatically on the first save.

The default HTTP port is `9999`:

```yaml
server:
  port: 9999
```

## Build And Distribution

```bash
mvn clean package
```

This creates:

- `target/nms-recipes.jar`
- `target/nms-recipes-dist.zip`
- `target/nms-recipes-dist/nms-recipes-dist/`

The distribution contains:

- `nms-recipes.jar`
- `data/recipes.json`
- `data/product-prices.json`
- `start.cmd`
- `start.sh`
- `README.txt`

The ZIP distribution is the recommended handover format. After unpacking, use the included start scripts.

Start on Linux/macOS:

```bash
cd <unpacked-folder>/nms-recipes-dist
./start.sh
```

Start on Windows:

```bat
cd <unpacked-folder>\nms-recipes-dist
start.cmd
```

The start scripts pass the data paths explicitly, so the JAR, recipe dataset, and price storage stay together as a self-contained package.

## Docker

The application can also be built as a self-contained container image. The image includes:

- the runnable application JAR
- the JSON recipe dataset
- the `product-prices.json` file

Build:

```bash
docker build -t nms-recipes .
```

Run:

```bash
docker run --rm -p 9999:9999 nms-recipes
```

The application is then available at `http://localhost:9999`.

## Model

- Product nodes represent craftable components.
- Recipe variants represent alternative production paths.
- Ingredient slots represent AND relationships.
- Multiple values inside one slot represent OR relationships.
