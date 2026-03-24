@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
java -jar "%SCRIPT_DIR%nms-recipes.jar" --recipes.source-path="%SCRIPT_DIR%data\recipes.json" --recipes.price-path="%SCRIPT_DIR%data\product-prices.json" %*
