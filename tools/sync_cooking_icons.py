#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import shutil
import subprocess
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
DATA_FILE = REPO_ROOT / "data/recipes.json"
ENGLISH_TERMS_FILE = REPO_ROOT / "src/main/java/de/nms/nmsrecipes/service/EnglishTerminology.java"
ICON_DIR = REPO_ROOT / "src/main/resources/static/icons"
MANIFEST_FILE = ICON_DIR / "manifest.json"

UPSTREAM_ICON_BASE = "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/images/"
DEFAULT_ICON_PATH = "/icons/fallback.svg"
UPSTREAM_JSON_SOURCES = [
    (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/Cooking.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/Cooking.lang.json",
    ),
    (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/Fishing.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/Fishing.lang.json",
    ),
    (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/Products.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/Products.lang.json",
    ),
    (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/ProceduralProducts.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/ProceduralProducts.lang.json",
    ),
    (
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/en/TradeItems.lang.json",
        "https://raw.githubusercontent.com/AssistantNMS/App/main/assets/json/de/TradeItems.lang.json",
    ),
]

# Local names that intentionally differ from the official in-game wording.
MANUAL_ALIASES = {
    "anomale torte": "Anormale Torte",
    "die brüter-torte": "Brüter-Torte",
    "fruchtpudding": "Fruchtiger Pudding",
    "gebackene käsetorte": "Käsetorte",
    "honigkuchen": "Funkelnder Honigkuchen",
    "knochensahne (käse)": "Knochensahne",
    "puddingtorte (kuchen)": "Puddingtorte",
    "schokoladeneis": "Schokoladeneiscreme",
}


def normalize(value: str | None) -> str:
    if value is None:
        return ""
    return " ".join(str(value).replace("\u00A0", " ").split()).strip().casefold()


def download_json(url: str) -> list[dict]:
    response = subprocess.run(
        ["curl", "-fsSL", "--max-time", "60", url],
        check=True,
        capture_output=True,
        text=True
    )
    return json.loads(response.stdout)


def load_local_data() -> tuple[list[dict], list[dict]]:
    payload = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    return payload["terms"], payload["recipes"]


def load_english_term_map() -> dict[str, str]:
    source = ENGLISH_TERMS_FILE.read_text(encoding="utf-8")
    term_map: dict[str, str] = {}
    for german_name, english_name in re.findall(r'term\(terms,\s*"([^"]+)",\s*"([^"]+)"\);', source):
        term_map[normalize(german_name)] = english_name
    return term_map


def build_local_translation_map(terms: list[dict], recipes: list[dict]) -> dict[str, str]:
    english_terms = load_english_term_map()
    translations: dict[str, str] = {}

    for term in terms:
        german_name = term["name"]
        english_name = term.get("englishName") or english_terms.get(normalize(german_name)) or german_name
        translations[normalize(german_name)] = english_name

    for recipe in recipes:
        german_name = recipe["name"]
        translations.setdefault(normalize(german_name), english_terms.get(normalize(german_name), german_name))

    return translations


def build_upstream_index(entries: list[dict]) -> dict[str, str]:
    by_name: dict[str, str] = {}
    for entry in entries:
        if not isinstance(entry, dict):
            continue
        name = entry.get("Name")
        icon = entry.get("Icon")
        if name and isinstance(icon, str) and icon.strip():
            by_name[normalize(name)] = icon
    return by_name


def candidate_names(local_name: str, english_name: str | None) -> list[str]:
    candidates = [local_name]
    if english_name:
        candidates.append(english_name)

    alias = MANUAL_ALIASES.get(normalize(local_name))
    if alias:
        candidates.append(alias)

    stripped_parenthetical = re.sub(r"\s+\([^)]*\)$", "", local_name).strip()
    if stripped_parenthetical and stripped_parenthetical != local_name:
        candidates.append(stripped_parenthetical)

    for article in ("Die ", "Der ", "Das "):
        if local_name.startswith(article):
            candidates.append(local_name[len(article):].strip())

    seen: set[str] = set()
    ordered: list[str] = []
    for candidate in candidates:
        key = normalize(candidate)
        if key and key not in seen:
            seen.add(key)
            ordered.append(candidate)
    return ordered


def resolve_icon(local_name: str,
                 english_name: str | None,
                 upstream_sources: list[dict[str, str]]) -> str | None:
    for candidate in candidate_names(local_name, english_name):
        candidate_key = normalize(candidate)
        for upstream_source in upstream_sources:
            icon_path = upstream_source.get(candidate_key)
            if icon_path:
                return icon_path
    return None


def download_icons(icon_paths: set[str]) -> None:
    ICON_DIR.mkdir(parents=True, exist_ok=True)

    for existing_path in ICON_DIR.iterdir():
        if existing_path.name == "fallback.svg":
            continue
        if existing_path.is_dir():
            shutil.rmtree(existing_path)
        else:
            existing_path.unlink()

    for icon_path in sorted(icon_paths):
        target_file = ICON_DIR / icon_path
        target_file.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            [
                "curl",
                "-fsSL",
                "--max-time",
                "30",
                "-o",
                str(target_file),
                f"{UPSTREAM_ICON_BASE}{icon_path}"
            ],
            check=True
        )


def write_manifest(manifest: dict[str, str]) -> None:
    payload = {
        "defaultIcon": DEFAULT_ICON_PATH,
        "icons": dict(sorted(manifest.items()))
    }
    MANIFEST_FILE.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    terms, recipes = load_local_data()
    translations = build_local_translation_map(terms, recipes)

    upstream_sources: list[dict[str, str]] = []

    local_names = sorted({entry["name"] for entry in terms} | {entry["name"] for entry in recipes})
    manifest: dict[str, str] = {}
    missing: list[str] = []
    referenced_icon_paths: set[str] = set()

    for english_url, german_url in UPSTREAM_JSON_SOURCES:
        source_index: dict[str, str] = {}
        for url in (english_url, german_url):
            source_index.update(build_upstream_index(download_json(url)))
        upstream_sources.append(source_index)

    for local_name in local_names:
        english_name = translations.get(normalize(local_name))
        icon_path = resolve_icon(local_name, english_name, upstream_sources)
        if icon_path:
            icon_url_path = Path(icon_path).as_posix()
            manifest[normalize(local_name)] = f"/icons/{icon_url_path}"
            referenced_icon_paths.add(icon_url_path)
        else:
            missing.append(local_name)

    download_icons(referenced_icon_paths)
    write_manifest(manifest)

    print(f"Local names: {len(local_names)}")
    print(f"Mapped icons: {len(manifest)}")
    print(f"Missing icons: {len(missing)}")
    if missing:
        print("Missing icon mappings:")
        for name in missing:
            print(f" - {name}")


if __name__ == "__main__":
    main()
