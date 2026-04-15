#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
OUTPUT_FILE = REPO_ROOT / "data" / "refinery-recipes.json"

CATALOG_SOURCES = {
    "raw": (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/RawMaterials.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/RawMaterials.lang.json",
    ),
    "prod": (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/Products.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/Products.lang.json",
    ),
    "cur": (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/Curiosity.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/Curiosity.lang.json",
    ),
    "conTech": (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/ConstructedTechnology.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/ConstructedTechnology.lang.json",
    ),
}

REFINERY_SOURCES = (
    "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/Refinery.lang.json",
    "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/Refinery.lang.json",
)


def download_json(url: str) -> list[dict]:
    response = subprocess.run(
        ["curl", "-fsSL", "--max-time", "60", url],
        check=True,
        capture_output=True,
        text=True,
    )
    return json.loads(response.stdout)


def item_prefix(item_id: str) -> str:
    if item_id.startswith("conTech"):
        return "conTech"
    if item_id.startswith("prod"):
        return "prod"
    if item_id.startswith("cur"):
        return "cur"
    if item_id.startswith("raw"):
        return "raw"
    raise KeyError(f"Unsupported refinery item id prefix: {item_id}")


def normalize_text(value: str | None) -> str:
    return " ".join(str(value or "").replace("\u00A0", " ").split()).strip()


def load_item_catalog() -> dict[str, dict[str, str]]:
    catalog: dict[str, dict[str, str]] = {}

    for english_url, german_url in CATALOG_SOURCES.values():
        english_entries = {entry["Id"]: entry for entry in download_json(english_url)}
        german_entries = {entry["Id"]: entry for entry in download_json(german_url)}

        for item_id, english_entry in english_entries.items():
            german_entry = german_entries.get(item_id)
            if german_entry is None:
                raise KeyError(f"Missing German catalog entry for {item_id}")

            english_name = normalize_text(english_entry.get("Name"))
            german_name = normalize_text(german_entry.get("Name"))
            english_group = normalize_text(english_entry.get("Group"))
            german_group = normalize_text(german_entry.get("Group"))

            if not english_name or not german_name:
                raise KeyError(f"Missing item name for {item_id}")

            catalog[item_id] = {
                "englishName": english_name,
                "germanName": german_name,
                "englishGroup": english_group,
                "germanGroup": german_group,
            }

    return catalog


def build_payload() -> dict:
    item_catalog = load_item_catalog()

    english_refinery = {entry["Id"]: entry for entry in download_json(REFINERY_SOURCES[0])}
    german_refinery = {entry["Id"]: entry for entry in download_json(REFINERY_SOURCES[1])}

    terms: dict[str, str] = {}
    categories: dict[str, str] = {}
    recipes_by_output: dict[str, list[dict]] = defaultdict(list)

    for recipe_id, english_entry in english_refinery.items():
        german_entry = german_refinery.get(recipe_id)
        if german_entry is None:
            raise KeyError(f"Missing German refinery entry for {recipe_id}")

        output_id = english_entry["Output"]["Id"]
        output_item = item_catalog.get(output_id)
        if output_item is None:
            raise KeyError(f"Unknown refinery output item id: {output_id}")

        output_name = output_item["germanName"]
        terms[output_name] = output_item["englishName"]

        german_category = output_item["germanGroup"]
        english_category = output_item["englishGroup"]
        if german_category:
            categories[german_category] = english_category or german_category

        ingredients = []
        for position, english_input in enumerate(english_entry.get("Inputs", []), start=1):
            german_input = german_entry.get("Inputs", [])[position - 1]
            input_id = english_input["Id"]
            input_item = item_catalog.get(input_id)
            if input_item is None:
                raise KeyError(f"Unknown refinery input item id: {input_id}")

            input_name = input_item["germanName"]
            terms[input_name] = input_item["englishName"]
            ingredients.append({
                "position": position,
                "name": input_name,
                "quantity": int(english_input["Quantity"]),
            })

        recipes_by_output[output_name].append({
            "operation": normalize_text(german_entry.get("Operation")),
            "englishOperation": normalize_text(english_entry.get("Operation")),
            "time": normalize_text(english_entry.get("Time")),
            "outputQuantity": int(english_entry["Output"]["Quantity"]),
            "ingredients": ingredients,
        })

    recipes = []
    for output_name, variants in recipes_by_output.items():
        output_id = next(
            item_id for item_id, item in item_catalog.items()
            if item["germanName"] == output_name
        )
        output_item = item_catalog[output_id]

        recipes.append({
            "name": output_name,
            "category": output_item["germanGroup"],
            "variants": sorted(
                variants,
                key=lambda variant: (
                    len(variant["ingredients"]),
                    tuple((ingredient["name"], ingredient["quantity"]) for ingredient in variant["ingredients"]),
                    variant["outputQuantity"],
                    variant["operation"],
                ),
            ),
        })

    payload = {
        "categories": [
            {"name": name, "englishName": english_name}
            for name, english_name in sorted(categories.items(), key=lambda entry: entry[0].casefold())
        ],
        "terms": [
            {"name": name, "englishName": english_name}
            for name, english_name in sorted(terms.items(), key=lambda entry: entry[0].casefold())
        ],
        "recipes": sorted(recipes, key=lambda recipe: recipe["name"].casefold()),
    }
    return payload


def main() -> None:
    payload = build_payload()
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_FILE.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    variant_count = sum(len(recipe["variants"]) for recipe in payload["recipes"])
    print(f"Saved {len(payload['recipes'])} refinery outputs and {variant_count} refinery variants to {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
