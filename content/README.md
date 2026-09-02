# Content packs

This directory contains the source-of-truth data used to build the app's offline
rule compendia. Content is data, not executable code. A pack may only describe
typed facts and actions supported by the application rules engine.

The checked-in general-purpose packs are intentionally small vertical-slice
seeds:

- `srd-5.2.1-seed` demonstrates the default 2024 fifth-edition shape.
- `srd-5.1-seed` demonstrates the separate 2014 fifth-edition shape.
- `pf2e-remaster-sample` is project-original placeholder data used to exercise
  a three-action rules engine. It is not an extraction of any publisher's book.

Those seed packs are **not complete rules compendia** and must not be presented as
such in a release. Their manifests set `distributionReady` to `false`. A release
pipeline must replace them with audited, source-complete packs, generate a
coverage report, and verify the required notices before changing that flag.

Two separately scoped, distribution-ready packs provide complete class-list
indexes for the Sorcerer and Wizard spells in their respective CC SRDs:

- `srd-5.1-spells`: 120 Sorcerer and 204 Wizard spells (211 unique).
- `srd-5.2.1-spells`: 138 Sorcerer and 217 Wizard spells (225 unique).

They are compact selection/search catalogs, not replacements for the spell
rules. Each manifest pins the official PDF SHA-256, each English/German entry
set has identical stable IDs, and `reports/` records every included upstream
class-list spell.

## Layout

```text
schema/                 JSON Schemas for manifests, localized entries, coverage
catalogs/               Generated cross-edition app catalog and source audit data
packs/<pack-id>/        Public seed manifests and localized entry files
private-template/       Copy-only template for locally owned content
reports/                Machine-readable coverage examples
```

All stable IDs are language-neutral. English and German files for a pack must
contain the same IDs. Search aliases are localized. Ruleset behavior is selected
by the manifest's `ruleset.id` and `ruleset.revision`, never by display text.

See [the content pipeline](../docs/CONTENT_PIPELINE.md) and
[the legal content policy](../docs/LEGAL_CONTENT_POLICY.md) before importing any
new source.
