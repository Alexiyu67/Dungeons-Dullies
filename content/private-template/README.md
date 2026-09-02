# Private local content template

Use this only for facts you are legally entitled to transcribe and use. It is
not a mechanism for sharing, scraping, or reconstructing commercial books.

1. Copy this directory to `privateContent/<your-pack-id>/` at the repository
   root.
2. Add `/privateContent/` to the root `.gitignore` before entering any content.
3. Rename the example files and choose a globally unique pack ID.
4. Keep `contentStatus: private-local` and `distributionReady: false`.
5. Record where each fact came from for your own audit; do not put copyrighted
   prose, art, or book scans into logs, test fixtures, or bug reports.
6. Build with the explicitly private Gradle flavor once that flavor is enabled.

The template includes a fictional option solely to show the JSON shape. It does
not include any non-SRD class, ancestry, feat, spell, or item rules.

Private packs must remain out of source control, CI artifacts, public APKs,
screenshots, sample backups, and automated test snapshots. A personal backup may
include them only when the user explicitly exports one.
