@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
java -jar "%SCRIPT_DIR%nms-recipes.jar" --recipes.source-path="%SCRIPT_DIR%data\recipes.json" --recipes.refinery-path="%SCRIPT_DIR%data\refinery-recipes.json" --recipes.sources-path="%SCRIPT_DIR%data\sources\material-sources.json" --recipes.price-path="%SCRIPT_DIR%data\product-prices.json" %*
