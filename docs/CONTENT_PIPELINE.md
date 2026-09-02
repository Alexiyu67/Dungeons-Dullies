# Content pipeline

Content compilation is intentionally deterministic and offline after sources
have been reviewed and pinned.

## Inputs and output

The compiler consumes one `manifest.json` plus `entries.<locale>.json` files.
It validates them against `content/schema`, normalizes search text, and emits a
versioned read-only SQLite database with normal lookup tables and FTS indexes.
The mutable character database stores only pack IDs and versions, so an older
pack remains available while any character references it.

The expected compiler stages are:

1. validate JSON Schema and reject unknown fields;
2. verify that locale files exactly match the manifest locale map;
3. require identical entry-ID sets across English and German;
4. verify unique IDs, known entry kinds, known automation commands, and valid
   ruleset/action-cost combinations;
5. verify license evidence, attribution, and coverage totals;
6. normalize Unicode and build locale-specific alias/search columns;
7. write the database in stable ID order;
8. run integrity checks and record the database SHA-256 in build metadata.

The compiler must not execute pack-provided code or deserialize arbitrary class
names. Pack commands map to a closed application enum.

## SRD spell and Wiki catalogs

The complete SRD spell indexes are generated only during a reviewed content
update. Download the official CC PDFs separately, verify that Poppler's
`pdftotext` is available, then run:

```powershell
node scripts/generate-srd-spell-catalog.mjs `
  --srd51 C:\reviewed-sources\SRD_CC_v5.1.pdf `
  --srd521 C:\reviewed-sources\SRD_CC_v5.2.1.pdf

node scripts/generate-srd-wiki-catalog.mjs `
  --srd51 C:\reviewed-sources\SRD_CC_v5.1.pdf `
  --srd51-de C:\reviewed-sources\SRD_CC_v5.1_DE.pdf `
  --srd521 C:\reviewed-sources\SRD_CC_v5.2.1.pdf `
  --srd521-de C:\reviewed-sources\DE_SRD_CC_v5.2.1.pdf
```

The generator rejects PDFs whose SHA-256 differs from the pinned snapshots and
rejects extracted class lists whose totals differ from the audited counts. The
spell generator emits the complete lists for all eight SRD spellcasting classes.
The Wiki generator verifies both language snapshots and emits character-option,
school, condition, action, and creature indexes; its creature parser retains only
name, size, and broad type and never emits a stat block. Both generators write
localized packs, coverage reports, JSON audit catalogs, and Kotlin runtime
catalogs. These generated files are checked in; normal Gradle/Android builds are
offline and do not invoke the generators or retain the PDFs.

## Versioning

- Pack versions are immutable. Never replace data under an existing version.
- Corrected content receives a new semantic version and a migration description.
- A character upgrade is an explicit reviewed copy, not an in-place silent edit.
- Ruleset revision is independent of pack version: the former selects engine
  semantics; the latter identifies a particular data snapshot.

## Representative seeds

The checked-in seeds exist to support UI, search, localization, and compiler
tests. They are deliberately incomplete. Tests and product copy must use the
manifest's `contentStatus` and `distributionReady` fields rather than assuming a
pack is complete because it can compile.

## Coverage reports

`coverage-report.schema.json` defines the machine contract. A real import emits
one record for every upstream item it inspected. Exclusion reasons should be
specific, for example `reserved-art`, `setting-lore`, `ogl-only`,
`non-player-facing`, `duplicate`, or `license-unknown`.

The checked-in ORC report documents only the six local fixtures and is not
evidence of coverage for an external rules dataset.
