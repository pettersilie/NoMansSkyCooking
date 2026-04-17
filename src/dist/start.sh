#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -jar "$SCRIPT_DIR/nms-recipes.jar" --recipes.source-path="$SCRIPT_DIR/data/recipes.json" --recipes.refinery-path="$SCRIPT_DIR/data/refinery-recipes.json" --recipes.sources-path="$SCRIPT_DIR/data/sources/material-sources.json" --recipes.price-path="$SCRIPT_DIR/data/product-prices.json" "$@"
