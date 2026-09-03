#!/usr/bin/env python3
"""Validate private-content JSON and build the compact two-file .dndpack archive."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import zipfile
from pathlib import Path

ID = re.compile(r"^[a-z0-9][a-z0-9._-]{1,79}$")
VERSION = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+(?:-[a-z0-9.-]+)?$")
KINDS = {
    "class", "subclass", "species", "background", "feat", "feature", "spell",
    "weapon", "armor", "item", "tool", "gear", "mount", "vehicle", "magic-item",
    "action", "condition",
}
ROOT_KEYS = {"schemaVersion", "id", "version", "ruleset", "locale", "requires", "entries"}
ENTRY_KEYS = {"id", "kind", "name", "summary", "aliases", "mechanics"}
MECHANICS_KEYS = {
    "parentClassId", "parentSubclassId", "parentSpeciesId", "selectionLevel", "unlockLevel",
    "classIds", "hitDie", "primaryAbility", "caster", "grantedSkillIds", "skillChoiceCount",
    "originFeatId", "speedFeet", "actionCost", "resource", "spell", "item",
    "grantedSpellIds", "grantAutomatically",
}
ACTION_KEYS = {"actions", "bonusActions", "reactions", "attacks", "objectInteractions", "pf2eActions", "resources"}
SPELL_KEYS = {"level", "school", "concentration", "ritual", "castingTime", "range", "components", "duration", "spellAttack", "saveAbility", "actionCost", "castPreviews"}
ITEM_KEYS = {"type", "damage", "damageType", "ability", "properties", "range", "mastery", "armorClass", "shieldBonus", "rarity", "requiresAttunement", "quantity"}
ABILITIES = {"STR", "DEX", "CON", "INT", "WIS", "CHA"}


def exact_keys(value: dict, allowed: set[str], label: str) -> None:
    unknown = set(value) - allowed
    if unknown:
        raise ValueError(f"{label} has unsupported fields: {', '.join(sorted(unknown))}")


def integer(value: object, low: int, high: int, label: str) -> None:
    if not isinstance(value, int) or isinstance(value, bool) or not low <= value <= high:
        raise ValueError(f"{label} must be an integer from {low} to {high}")


def validate_action(value: dict, label: str) -> None:
    if not isinstance(value, dict):
        raise ValueError(f"{label} must be an object")
    exact_keys(value, ACTION_KEYS, label)
    limits = {"actions": 10, "bonusActions": 1, "reactions": 1, "attacks": 10, "objectInteractions": 10, "pf2eActions": 3}
    for key, maximum in limits.items():
        if key in value:
            integer(value[key], 0, maximum, f"{label}.{key}")
    resources = value.get("resources", {})
    if not isinstance(resources, dict) or any(not ID.fullmatch(key) for key in resources):
        raise ValueError(f"{label}.resources has an invalid ID")
    for key, amount in resources.items():
        integer(amount, 1, 20, f"{label}.resources.{key}")


def validate_document(document: object) -> dict:
    if not isinstance(document, dict):
        raise ValueError("content root must be an object")
    exact_keys(document, ROOT_KEYS, "content")
    required = {"schemaVersion", "id", "version", "ruleset", "locale", "entries"}
    if not required <= set(document):
        raise ValueError(f"content is missing: {', '.join(sorted(required - set(document)))}")
    if document["schemaVersion"] != 1 or document["ruleset"] != "2024" or document["locale"] != "en":
        raise ValueError("schemaVersion, ruleset, and locale must be 1, 2024, and en")
    if not isinstance(document["id"], str) or not ID.fullmatch(document["id"]):
        raise ValueError("invalid pack id")
    if not isinstance(document["version"], str) or not VERSION.fullmatch(document["version"]):
        raise ValueError("invalid pack version")
    requirements = document.get("requires", [])
    if not isinstance(requirements, list) or len(requirements) > 32:
        raise ValueError("requires must contain at most 32 items")
    for requirement in requirements:
        if not isinstance(requirement, dict) or set(requirement) != {"id", "version"}:
            raise ValueError("each requirement needs exactly id and version")
        if not ID.fullmatch(requirement["id"]) or not VERSION.fullmatch(requirement["version"]):
            raise ValueError("invalid requirement")

    entries = document["entries"]
    if not isinstance(entries, list) or not 1 <= len(entries) <= 5000:
        raise ValueError("entries must contain 1 to 5000 items")
    ids: dict[str, str] = {}
    for index, entry in enumerate(entries):
        label = f"entries[{index}]"
        if not isinstance(entry, dict):
            raise ValueError(f"{label} must be an object")
        exact_keys(entry, ENTRY_KEYS, label)
        if not {"id", "kind", "name"} <= set(entry):
            raise ValueError(f"{label} needs id, kind, and name")
        if not isinstance(entry["id"], str) or not ID.fullmatch(entry["id"]) or entry["id"] in ids:
            raise ValueError(f"{label} has an invalid or duplicate id")
        if entry["kind"] not in KINDS:
            raise ValueError(f"{label} has an unsupported kind")
        if not isinstance(entry["name"], str) or not 1 <= len(entry["name"]) <= 120:
            raise ValueError(f"{label} has an invalid name")
        if not isinstance(entry.get("summary", ""), str) or len(entry.get("summary", "")) > 1200:
            raise ValueError(f"{label} has an invalid summary")
        aliases = entry.get("aliases", [])
        if not isinstance(aliases, list) or len(aliases) > 24 or any(not isinstance(alias, str) or not 1 <= len(alias) <= 120 for alias in aliases):
            raise ValueError(f"{label} has invalid aliases")
        ids[entry["id"]] = entry["kind"]

        mechanics = entry.get("mechanics", {})
        if not isinstance(mechanics, dict):
            raise ValueError(f"{label}.mechanics must be an object")
        exact_keys(mechanics, MECHANICS_KEYS, f"{label}.mechanics")
        for key in ("selectionLevel", "unlockLevel"):
            if key in mechanics:
                integer(mechanics[key], 1, 20, f"{label}.{key}")
        if "hitDie" in mechanics and mechanics["hitDie"] not in {6, 8, 10, 12}:
            raise ValueError(f"{label}.hitDie must be 6, 8, 10, or 12")
        if "primaryAbility" in mechanics and mechanics["primaryAbility"] not in ABILITIES:
            raise ValueError(f"{label}.primaryAbility is invalid")
        if "speedFeet" in mechanics:
            integer(mechanics["speedFeet"], 0, 200, f"{label}.speedFeet")
        if "actionCost" in mechanics:
            validate_action(mechanics["actionCost"], f"{label}.actionCost")
        if "resource" in mechanics:
            resource = mechanics["resource"]
            if not isinstance(resource, dict) or not set(resource) <= {"maximum", "recovery"} or "maximum" not in resource:
                raise ValueError(f"{label}.resource is invalid")
            integer(resource["maximum"], 1, 999, f"{label}.resource.maximum")
        if "spell" in mechanics:
            spell = mechanics["spell"]
            if not isinstance(spell, dict) or "level" not in spell:
                raise ValueError(f"{label}.spell is invalid")
            exact_keys(spell, SPELL_KEYS, f"{label}.spell")
            integer(spell["level"], 0, 9, f"{label}.spell.level")
            if "actionCost" in spell:
                validate_action(spell["actionCost"], f"{label}.spell.actionCost")
        if "item" in mechanics:
            item = mechanics["item"]
            if not isinstance(item, dict):
                raise ValueError(f"{label}.item is invalid")
            exact_keys(item, ITEM_KEYS, f"{label}.item")

    for entry in entries:
        mechanics = entry.get("mechanics", {})
        expected = {"parentClassId": "class", "parentSubclassId": "subclass", "parentSpeciesId": "species", "originFeatId": "feat"}
        for key, kind in expected.items():
            if key in mechanics and ids.get(mechanics[key]) != kind:
                raise ValueError(f"{entry['id']}.{key} must reference a {kind} in the same file")
        for class_id in mechanics.get("classIds", []):
            if ids.get(class_id) != "class":
                raise ValueError(f"{entry['id']}.classIds has an invalid reference")
        for spell_id in mechanics.get("grantedSpellIds", []):
            if ids.get(spell_id) != "spell":
                raise ValueError(f"{entry['id']}.grantedSpellIds has an invalid reference")
    return document


def content_bytes(path: Path) -> bytes:
    raw = path.read_text(encoding="utf-8")
    document = validate_document(json.loads(raw))
    return (json.dumps(document, ensure_ascii=False, indent=2) + "\n").encode("utf-8")


def manifest(content: bytes) -> bytes:
    value = {"schemaVersion": 1, "content": {"path": "content.json", "size": len(content), "sha256": hashlib.sha256(content).hexdigest()}}
    return (json.dumps(value, indent=2) + "\n").encode("utf-8")


def write_pack(destination: Path, content: bytes) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, payload in (("manifest.json", manifest(content)), ("content.json", content)):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o600 << 16
            archive.writestr(info, payload)


def validate_pack(path: Path) -> dict:
    with zipfile.ZipFile(path) as archive:
        if archive.namelist() != ["manifest.json", "content.json"]:
            raise ValueError("a .dndpack must contain exactly manifest.json and content.json")
        packed_manifest = json.loads(archive.read("manifest.json"))
        content = archive.read("content.json")
    if set(packed_manifest) != {"schemaVersion", "content"} or packed_manifest["schemaVersion"] != 1:
        raise ValueError("invalid manifest")
    metadata = packed_manifest["content"]
    if set(metadata) != {"path", "size", "sha256"} or metadata["path"] != "content.json":
        raise ValueError("invalid content metadata")
    if metadata["size"] != len(content) or metadata["sha256"] != hashlib.sha256(content).hexdigest():
        raise ValueError("content integrity check failed")
    return validate_document(json.loads(content))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="private-content JSON or .dndpack")
    parser.add_argument("-o", "--output", type=Path, help="output .dndpack path for JSON input")
    parser.add_argument("--check", action="store_true", help="validate only")
    args = parser.parse_args()
    try:
        if args.input.suffix.lower() == ".dndpack":
            document = validate_pack(args.input)
            if args.output:
                raise ValueError("--output is only valid for JSON input")
        else:
            payload = content_bytes(args.input)
            document = json.loads(payload)
            if not args.check:
                output = args.output or args.input.with_suffix(".dndpack")
                write_pack(output, payload)
                print(output)
        print(f"valid {document['id']} {document['version']} ({len(document['entries'])} entries)")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, zipfile.BadZipFile) as failure:
        print(f"error: {failure}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
