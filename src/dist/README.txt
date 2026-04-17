NMS Recipe Graph Distribution

Contents:
- nms-recipes.jar
- data/recipes.json
- data/refinery-recipes.json
- data/sources/material-sources.json
- data/product-prices.json
- start.cmd
- start.sh

Start:
- Windows: start.cmd
- Linux/macOS: ./start.sh

Alternative start command:
java -jar nms-recipes.jar --recipes.source-path=./data/recipes.json --recipes.refinery-path=./data/refinery-recipes.json --recipes.sources-path=./data/sources/material-sources.json --recipes.price-path=./data/product-prices.json

Notes:
- data/recipes.json is the single source of truth for cooking categories, terms, and recipes
- data/refinery-recipes.json contains the refinery dataset
- data/sources/material-sources.json contains the material source dataset for the Sources page
- data/product-prices.json remains a separate file for price storage
- cooking categories, cooking recipes, and cooking prices changed through the UI are written back to these files
- refinery data is currently loaded from data/refinery-recipes.json but not edited from the UI
- source data is currently loaded from data/sources/material-sources.json and is read-only in the UI
