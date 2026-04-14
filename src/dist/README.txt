NMS Recipe Graph Distribution

Contents:
- nms-recipes.jar
- data/recipes.json
- data/product-prices.json
- start.cmd
- start.sh

Start:
- Windows: start.cmd
- Linux/macOS: ./start.sh

Alternative start command:
java -jar nms-recipes.jar --recipes.source-path=./data/recipes.json --recipes.price-path=./data/product-prices.json

Notes:
- data/recipes.json is the single source of truth for categories, terms, and recipes
- data/product-prices.json remains a separate file for price storage
- changes made through the UI are written back to these files
