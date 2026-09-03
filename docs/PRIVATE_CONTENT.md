# Private local content

Public builds contain only audited redistributable material. Content from a book
you own must be converted and imported privately; do not commit or share the
book, extracted data, or resulting pack unless you have distribution rights.

The app accepts two direct import formats:

- a UTF-8 JSON document conforming to
  [`private-content-v1.schema.json`](../content/schema/private-content-v1.schema.json); or
- a `.dndpack` ZIP containing exactly `manifest.json` and `content.json`.

The Android picker grants access only to the selected file. Importing is fully
offline, and accepted content is copied into app-private storage. PDF, text, and
Markdown are deliberately not import formats; prepare structured JSON first.

## Build and validate

Start from the fictional
[`private-content.example.json`](../content/private-template/private-content.example.json).
The same schema can be downloaded inside the app from **Local content**.

```powershell
python scripts\import-private-content.py private-local\my-content.json
python scripts\import-private-content.py private-local\my-content.dndpack --check
```

The first command validates the JSON and creates a neighboring `.dndpack`.
Unknown fields, duplicate IDs, invalid mechanics, and broken same-file references
are rejected. Pack creation is deterministic and never sends data over a network.

## JSON document

Every document identifies one English 2024-rules pack. Entries have a stable ID,
kind, short player-facing summary, optional search aliases, and optional typed
mechanics. Typed mechanics cover only player-side facts the app can safely track:
unlock levels, spell slots and concentration metadata, action/resource costs,
rest recovery, equipment, and unconditional self statistics. Target selection,
DM decisions, conditional outcomes, and rules interpretation remain table-facing.

References such as `parentClassId`, `parentSubclassId`, and `grantedSpellIds`
must point to a compatible entry in the same JSON file. Pack dependencies use
exact `id` and `version` pairs. Android prevents installation until all listed
dependencies are present.

## `.dndpack` v1

A `.dndpack` is a ZIP with exactly these root files:

```text
manifest.json
content.json
```

`content.json` is the JSON document above. `manifest.json` conforms to
[`dndpack-v1.schema.json`](../content/schema/dndpack-v1.schema.json) and stores
only its path, byte size, and SHA-256 digest. No executable code, extra files, or
archive paths are accepted.

Installing another version with the same pack ID atomically replaces that pack's
entries. New characters can select installed entries immediately. Existing
characters remain unchanged until **Use local handbook data** is selected in the
character Build editor.

## Local-only storage

Keep generated content below an ignored directory such as `private-local/`,
`local-content/`, or `privateContent/`. Public tests use only the fictional
example. Never add scans, copied book prose, publisher art, credentials, receipts,
or privately generated packs to the repository.
