# Private local content

Public builds intentionally contain only audited open content. A user who owns
additional source material can create a personal, unshared pack without changing
the public dataset.

Start with `content/private-template`. Copy it to the root `privateContent`
directory, install the supplied ignore rules, and keep every private manifest at
`distributionReady: false`. The build must require an explicit private flavor;
the normal debug/release tasks must not scan that directory.

## Offline import and manual entry

The character-sheet menu includes a small local-content editor for individual
classes, subclasses, species, backgrounds, feats, spells, items, weapons,
conditions, actions, and resources. Formulas remain optional; entries without a
complete reviewed formula are informational and excluded from Suggested Turn.

An approved ancestry or species entry can provide positive creation-time ability
bonuses with a formula such as `ability STR=+2 CON=+1`. Each ability may appear
once, values must be between `+1` and `+10`, and an optional `ruleset=2014`,
`ruleset=2024`, or `ruleset=pf2e` marker limits where the formula applies.

The Android phone flow is the primary importer; no desktop importer is required.
The app can select a local PDF, UTF-8 text/Markdown, JSON, or `.dndpack` through
the system document picker. Selection grants access only to that file; the app
requests no broad storage permission. PDF text extraction and candidate
generation run in a persistent local worker. Scanned PDFs must be OCRed before
import.

The network-free desktop importer is optional batch tooling. It accepts only
local PDF, UTF-8 text/Markdown, or JSON and refuses to write into the public
content directory:

```powershell
python scripts/import-private-content.py `
  --input local-content/my-source.pdf `
  --output private-local/my-pack.dndpack `
  --pack-id my-private-pack `
  --language en
```

PDF extraction uses `pdfplumber` when installed and falls back to `pypdf`. The
importer records the source file hash and page number, emits review candidates
without printing source prose to the console, and gives every candidate
`needs_review` plus `automation.eligible: false`. A reviewer must correct and
approve the candidate before a later pack-install step may enable automation.

## `.dndpack` container v1

Every pack is a ZIP whose root `manifest.json` conforms to
`content/schema/dndpack-v1.schema.json`. Payload paths are flat, explicitly
listed with byte sizes and SHA-256 digests, and no unlisted ZIP entry is allowed.

- `review-candidates` uses `candidates.json`, is always `needs-review`, and has
  zero automation-eligible entries.
- `installable-content` uses `content-manifest.json`, must be explicitly
  `reviewed`, and its content manifest must remain `private-local` and
  `distributionReady: false`.

Android always asks the player to review and approve imported entries. Approved
typed `ancestry`/`species`, `class`, `feat`, `spell`, `language`, `item`/`equipment`,
and `weapon` entries become selectable in the matching creation, item, or level-up
flow and are recorded on the character. Items and weapons with incomplete mechanics
open a prefilled custom editor before they can be added.
Generic `rule`, `content-pack`, generic PDF-derived, unreviewed, and unknown
candidate kinds remain informational; they are never treated as character mechanics merely
because a file or ZIP labels itself installable.

Raw PDF, text, Markdown, and JSON input can only create the first kind. Renaming
arbitrary JSON or a ZIP does not make it installable. Android additionally
limits entry count, expanded bytes, and compression ratio, checks every digest,
and never extracts ZIP paths to the filesystem.

Desktop private packs live only under ignored `privateContent`, `private-local`,
or `local-content` paths. Android stores staged and completed packs below its
app-private files directory and deletes the staged source after processing. The
importers contain no URL, login, scraping, or access-control bypass support.

Private input processing should run locally and expose only validation errors by
stable ID and field name. It must never echo source prose into CI logs. Public
tests use the fictional template entry, not a user's pack.

## Allowed use

- data the user wrote themselves;
- facts the user is permitted to store for personal use;
- project-original homebrew; and
- properly licensed third-party material with its notice retained.

## Not allowed in the repository

- copied book chapters or large prose excerpts;
- scans, screenshots, publisher art, or fonts;
- access tokens, purchase receipts, account exports, or decryption tools;
- scraped services or data obtained by bypassing technical controls; and
- a pack presented as redistributable without documented permission.

The application should label private entries as `Local` and include their pack
ID in exports. Import must warn when a required private pack is absent and must
not silently substitute similarly named public content.
