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

Two separately scoped, distribution-ready packs provide complete eight-class
spell indexes for their respective CC SRDs:

- `srd-5.1-spells`: 319 unique spells across Bard, Cleric, Druid, Paladin,
  Ranger, Sorcerer, Warlock, and Wizard lists.
- `srd-5.2.1-spells`: 338 unique spells across the same eight classes.

They are compact selection/search catalogs, not replacements for the spell
rules. Each manifest pins the official PDF SHA-256, each English/German entry
set has identical stable IDs, and `reports/` records every included upstream
class-list spell.

Two companion Wiki packs provide compact bilingual search entries for SRD
character options, all eight schools of magic, conditions, common actions, and
creature identities:

- `srd-5.1-wiki`: 385 entries, including 317 creature overviews.
- `srd-5.2.1-wiki`: 419 entries, including 330 creature overviews.

Creature entries contain only names, size categories, broad creature types,
and original player-facing summaries. They deliberately contain no stat-block
numbers. Approved private entries extend the same search locally for options
that cannot be redistributed in the public app.

## Layout

```text
schema/                 JSON Schemas for manifests, localized entries, coverage
catalogs/               Generated cross-edition app catalog and source audit data
packs/<pack-id>/        Public seed manifests and localized entry files
private-template/       Fictional example for the private JSON import schema
reports/                Machine-readable coverage examples
```

All stable IDs are language-neutral. English and German files for a pack must
contain the same IDs. Search aliases are localized. Ruleset behavior is selected
by the manifest's `ruleset.id` and `ruleset.revision`, never by display text.

See [the content pipeline](../docs/CONTENT_PIPELINE.md) and
[the legal content policy](../docs/LEGAL_CONTENT_POLICY.md) before importing any
new source.
