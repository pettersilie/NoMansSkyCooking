FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml ./
COPY src ./src
COPY data/recipes.json ./data/recipes.json
COPY data/refinery-recipes.json ./data/refinery-recipes.json

RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN mkdir -p /app/data

COPY --from=build /build/target/nms-recipes.jar /app/nms-recipes.jar
COPY --from=build /build/data/recipes.json /app/data/recipes.json
COPY --from=build /build/data/refinery-recipes.json /app/data/refinery-recipes.json
COPY --from=build /build/src/dist/product-prices.json /app/data/product-prices.json

EXPOSE 9999

ENTRYPOINT ["java", "-jar", "/app/nms-recipes.jar", "--recipes.source-path=/app/data/recipes.json", "--recipes.refinery-path=/app/data/refinery-recipes.json", "--recipes.price-path=/app/data/product-prices.json", "--server.port=9999"]
