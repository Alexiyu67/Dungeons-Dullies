# Dullies & Dungeons

An Android-first, local-only tabletop character companion designed to guide new
players through character creation, rolls, and turns. The application uses
Kotlin and Compose Multiplatform so rules and UI logic can later be shared with
iOS, while the first deliverable is a sideloadable Android APK.

[Download the latest stable Android APK](https://github.com/Alexiyu67/Dungeons-Dullies/releases/latest/download/DulliesAndDungeons.apk)

This repository is an implementation foundation and vertical slice. The checked-
in rules packs are representative seed data, not complete compendia. In
particular, commercial fifth-edition options outside SRD 5.1/5.2.1 are not
bundled. See [content licensing](docs/LEGAL_CONTENT_POLICY.md) before adding data.

## Principles

- Runs without an account, server, analytics, content download, or internet
  permission.
- Keeps each character pinned to an explicit ruleset and content-pack version.
- Automates only outcomes the rules and character state determine; table rulings
  remain with the physical DM/GM.
- Uses stable IDs and typed commands across English and German.
- Models rolls and committed actions as auditable activity records in the shared rules core.

## Implemented vertical slice

The installable Android build includes character selection, a six-step creator, rolled/standard/
point-buy/manual abilities, app-private portraits, persistent characters and conditions, one-tap
rolls, character/rules search, guided movement/attack/spell/custom-action turns, 5e extra attacks,
three-action turn resources, feature counters, and copy-based ruleset conversion. English and
German UI paths are included.

The checked-in compendium remains intentionally representative. A complete public release cannot
legally bundle every commercial ancestry, subclass, feat, spell, or rule description without a
redistributable source. The private-pack template is the extension point for lawfully supplied
non-SRD material.

## Build

Install JDK 17 and Android SDK Platform 36, then run on Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat :androidApp:assembleDebug
```

The APK is written under `androidApp/build/outputs/apk/debug/`. Full setup,
installation, and release checks are in [Local Android build](docs/LOCAL_BUILD.md).

## Repository guide

- `androidApp/` — Android entry point and packaging
- `shared/` — shared UI, domain, persistence, and rules behavior
- `content/` — manifest schemas and bilingual representative seed packs
- `licenses/` — attribution and ORC notice scaffolding
- `docs/` — architecture, content audit, private-pack, and build documentation
- `scripts/validate-content.mjs` — dependency-free seed integrity checks

Validate content independently with:

```powershell
node scripts\validate-content.mjs
```

Start with the [documentation index](docs/README.md) and
[content-pack overview](content/README.md).
