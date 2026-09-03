# Third-party notices

The following notices apply when the corresponding content pack is included in
an application build. A build must omit notices for packs it does not contain
and must not ship a pack whose manifest has `distributionReady: false` as if it
were complete.

## Application logo

`logo.png` is project-supplied application artwork. Android launcher, themed,
and loading resources are deterministic derivatives of that file; they do not
incorporate third-party icon artwork.

## System Reference Document 5.1

This work includes material taken from the System Reference Document 5.1
(“SRD 5.1”) by Wizards of the Coast LLC and available at
https://dnd.wizards.com/resources/systems-reference-document. The SRD 5.1 is
licensed under the Creative Commons Attribution 4.0 International License
available at https://creativecommons.org/licenses/by/4.0/legalcode.

The checked-in app entries are short, transformed beginner summaries. The
original SRD remains the authoritative source.

## System Reference Document 5.2.1

This work includes material from the System Reference Document 5.2.1
(“SRD 5.2.1”) by Wizards of the Coast LLC, available at
https://www.dndbeyond.com/srd. The SRD 5.2.1 is licensed under the Creative
Commons Attribution 4.0 International License, available at
https://creativecommons.org/licenses/by/4.0/legalcode.

The checked-in app entries are short, transformed beginner summaries. The
original SRD remains the authoritative source.

## Open RPG Creative (ORC) content

No upstream ORC-licensed publication is included in the repository seed. The
neutral three-action examples are project-original development fixtures. Before
an ORC-based pack can be distributed, replace every placeholder in
`ORC-NOTICE.template.md`, include the complete upstream attribution chain, and
pass the content audit described in `docs/LEGAL_CONTENT_POLICY.md`.
