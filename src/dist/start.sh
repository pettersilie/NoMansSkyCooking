#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -jar "$SCRIPT_DIR/nms-recipes.jar" --recipes.source-path="$SCRIPT_DIR/data/recipes.json" --recipes.price-path="$SCRIPT_DIR/data/product-prices.json" "$@"
