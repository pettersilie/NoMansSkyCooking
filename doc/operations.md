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

## Build

Build the JAR and distribution:

```bash
mvn clean package
```

Typical outputs:

- `target/nms-recipes.jar`
- `target/nms-recipes-dist.zip`
- `target/nms-recipes-dist/`

## Distribution Layout

The distribution contains:

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

The start scripts explicitly pass the dataset path and the price storage path.

## Docker

Build:

```bash
docker build -t nms-recipes .
```

Run:

```bash
docker run --rm -p 9999:9999 nms-recipes
```

The container is self-contained and includes:

- application JAR
- `recipes.json`
- `product-prices.json`

## Runtime Files

Runtime state consists of:

- immutable recipe dataset
- mutable price file

Recommended handling:

- treat `recipes.json` as versioned application data
- treat `product-prices.json` as environment-specific state

## Validation And Tests

Run tests:

```bash
mvn test
```

Recommended checks after changes:

- `mvn test`
- `mvn -DskipTests package`

## Operational Notes

- product keys are canonical names from the dataset
- display names are localized at runtime
- price formatting currently uses German number formatting on the backend
- the graph is rebuilt server-side per selected root product
