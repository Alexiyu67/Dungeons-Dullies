# Local-first architecture

The project is split along boundaries that keep gameplay fast, data auditable,
and a future iOS client possible.

```text
androidApp         Android entry point, photo picker, private-file storage adapter
shared             Compose UI, serializable domain models, dice and turn reducers
content/           declarative, licensed source data and schemas
SharedPreferences  current versioned character/settings snapshot
files/portraits    normalized private portrait sources and rendered crops
```

The current Android vertical slice deliberately uses a versioned JSON snapshot rather than a
database dependency. This keeps the APK and implementation small while the data model is still
changing. The intended compendium-scale evolution is a generated read-only SQLite pack plus a
transactional user database; that compiler/runtime migration is not represented as complete in
this repository.

## Runtime data flow

The UI sends a typed command to the selected ruleset engine. The engine combines
the immutable character build, mutable character/turn state, and pack facts into
a deterministic transition. The transition contains prompts, rolls, resource
changes, disabled reasons, and explanation terms. Only committed transitions are
appended to history.

Pack data never decides behavior by matching localized labels. Stable IDs and
closed command enums connect content to code. Values that the physical table
must decide—hit/miss, targets, terrain, cover, and ambiguous interactions—return
a typed prompt rather than a guessed result.

## Local-only boundary

The production manifest has no `INTERNET` permission. There are no accounts,
analytics, remote fonts, crash-upload SDKs, or content downloads. Portraits are
copied and normalized into app-private storage. Android cloud backup is disabled;
manual import/export goes through the system document picker.

Search in the current slice is an in-memory index over the selected character, approved private
entries, built-in equipment, and the generated SRD spell/Wiki catalogs for that character's exact
rules revision. Search normalizes English and German terms and never sends a query off-device. A
future pack compiler can replace the index with local SQLite FTS without changing that boundary.

## Persistence and recovery

- Characters, language, feature counters, and conditions are stored after each committed edit.
- Portrait sources are orientation-corrected, resized to at most 2048 px, and
  kept in app-private files for later re-cropping. Display crops are stored
  separately at 512 px.
- The domain layer already defines pinned pack references and append-only activity records.
- Transactional history, active-turn process restoration, SQLite FTS, and manual archive
  import/export remain the next persistence milestone rather than being silently simulated.
