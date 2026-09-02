# Legal content policy

The app is a rules companion, not a substitute for books a user does not own.
Only content with recorded distribution rights can enter a public pack.

## Public-pack admission rules

Every imported record must have:

1. a stable upstream ID and pinned source revision;
2. a source publication and precise locator;
3. license evidence applicable to that specific record;
4. a classification as Licensed Material or allowed original material;
5. an English entry and a rights-cleared German entry;
6. an include/exclude decision in the generated coverage report; and
7. a notice/attribution chain that the application can show offline.

For Creative Commons material, record transformations and preserve the exact
requested attribution. For ORC material, preserve every upstream attribution,
Reserved Material declaration, and downstream-credit instruction.

## Always exclude unless separately authorized

- non-SRD rules text, character options, spells, monsters, and item prose from
  commercial fifth-edition books;
- logos, trademarks, trade dress, art, maps, setting lore, named characters,
  organizations, deities, locations, story text, and other Reserved Material;
- records whose only provenance is a wiki, search result, user forum, or an
  unpinned live endpoint;
- OGL-only material from an ORC pack;
- publisher translations unless the translation is itself licensed for this use;
- inferred/reconstructed text from protected books, including generated
  paraphrases intended to recreate unavailable content.

Names such as a desired class, ancestry, or feat are not evidence that its rules
are distributable. Options outside the checked-in SRDs belong in the user's
private local pack unless a reviewed public license is recorded.

## ORC audit gate

An importer may only accept a candidate after evaluating the license metadata
for the exact source publication. Dataset inclusion alone is not proof. Unknown
or mixed licensing must resolve to `pending-review` or `excluded`, never to an
optimistic include.

The frozen source must be identified by commit or immutable release checksum.
Every seen record appears in the coverage report, including filtered records.
The build fails when:

- `seen != included + excluded + pendingReview`;
- any public release has pending records;
- an included record has no license evidence or output ID;
- bilingual ID sets differ;
- a required attribution is absent; or
- placeholders remain in the final ORC Notice.

Do not describe the resulting pack as “all books.” Describe it as the audited,
legally reusable player-facing content present in the named frozen source.

## German text

Use an officially CC-licensed German SRD when the exact revision is available,
or write an original adaptation from licensed mechanical facts. Each entry must
say which route was used. Do not translate Reserved Material or unlicensed prose.

## Release evidence

Archive the manifests, coverage reports, notices, source revision/checksums,
compiler version, and validation output used for every signed APK. This makes
the contents of a historical offline build independently auditable.
