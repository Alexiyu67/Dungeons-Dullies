#!/usr/bin/env node

/**
 * Generates saving-throw and spell-attack metadata for the checked-in SRD
 * Wizard/Sorcerer catalog. Source text is read from the pinned official SRD
 * PDFs; application builds consume only the generated Kotlin file.
 *
 * Usage:
 *   node scripts/generate-srd-spell-combat-metadata.mjs \
 *     --srd51 path/to/SRD_CC_v5.1.pdf \
 *     --srd521 path/to/SRD_CC_v5.2.1.pdf
 *
 * Requires Poppler's `pdftotext` on PATH.
 */

import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const args = new Map();
for (let index = 2; index < process.argv.length; index += 2) {
  args.set(process.argv[index], process.argv[index + 1]);
}

const definitions = [
  {
    revision: "2014-srd-5.1",
    kotlinRevision: "SRD_5_1",
    pdf: args.get("--srd51"),
    sha256: "2504d2a0abb0a4d491a939be4f17910a2dde0312570ab8d208080225ccf0a1f0",
    firstPage: 114,
    lastPage: 206,
    joinPositionedWords: true,
    classification: /^(?:(?:Abjuration|Conjuration|Divination|Enchantment|Evocation|Illusion|Necromancy|Transmutation) cantrip|[1-9](?:st|nd|rd|th).*?(?:Abjuration|Conjuration|Divination|Enchantment|Evocation|Illusion|Necromancy|Transmutation))(?:\s+\(ritual\))?$/i,
  },
  {
    revision: "2024-srd-5.2.1",
    kotlinRevision: "SRD_5_2_1",
    pdf: args.get("--srd521"),
    sha256: "8974902d109d6e63672d7c490bde9ccf052410503d9cfa768237154fbc5e3d87",
    firstPage: 107,
    lastPage: 208,
    classification: /^(?:Level [1-9] (?:Abjuration|Conjuration|Divination|Enchantment|Evocation|Illusion|Necromancy|Transmutation)|(?:Abjuration|Conjuration|Divination|Enchantment|Evocation|Illusion|Necromancy|Transmutation) Cantrip) \(/i,
  },
];

if (definitions.some(definition => !definition.pdf)) {
  throw new Error("Both --srd51 and --srd521 are required.");
}

const catalog = JSON.parse(readFileSync(join(repoRoot, "content", "catalogs", "srd-wizard-sorcerer-spells.json"), "utf8"));
const temporaryRoot = mkdtempSync(join(tmpdir(), "dullies-spell-combat-"));
const generated = [];

try {
  for (const definition of definitions) {
    const pdf = resolve(definition.pdf);
    const actualHash = createHash("sha256").update(readFileSync(pdf)).digest("hex");
    if (actualHash !== definition.sha256) {
      throw new Error(`${definition.revision}: PDF SHA-256 ${actualHash} does not match the pinned source`);
    }

    const output = join(temporaryRoot, `${definition.kotlinRevision}.txt`);
    execFileSync(
      "pdftotext",
      ["-raw", "-f", String(definition.firstPage), "-l", String(definition.lastPage), pdf, output],
      { stdio: "inherit" },
    );
    const lines = normalizedLines(readFileSync(output, "utf8"), definition.joinPositionedWords);
    const headings = spellHeadings(lines, definition.classification);
    const revisionEntries = catalog.entries.filter(entry => entry.revision === definition.revision);
    const missing = [];

    for (const entry of revisionEntries) {
      const heading = headings.find(candidate => normalizeName(candidate.name) === normalizeName(entry.en.name));
      if (!heading) {
        missing.push(entry.en.name);
        continue;
      }
      const nextHeading = headings.find(candidate => candidate.index > heading.index);
      const body = lines.slice(heading.index + 1, nextHeading?.index ?? lines.length).join(" ")
        .replace(/([A-Za-z])-\s+([a-z])/g, "$1$2");
      // Narrative spell rules use lower-case "saving throw". Keeping that
      // casing excludes unrelated monster-stat-block action headings that can
      // be interleaved into the PDF's extraction order.
      const savingThrowAbilities = [...body.matchAll(/\b(Strength|Dexterity|Constitution|Intelligence|Wisdom|Charisma) saving throw\b/g)]
        .map(match => match[1].toUpperCase())
        .filter((ability, index, values) => values.indexOf(ability) === index);
      generated.push({
        revision: definition.kotlinRevision,
        id: entry.id,
        savingThrowAbilities,
        spellAttack: /\b(?:melee|ranged) spell attack\b/i.test(body),
      });
    }

    if (missing.length > 0) {
      throw new Error(`${definition.revision}: missing spell description headings: ${missing.join(", ")}`);
    }
    const relevant = generated.filter(entry => entry.revision === definition.kotlinRevision);
    console.log(
      `${definition.revision}: ${relevant.length} spells audited; ` +
      `${relevant.filter(entry => entry.savingThrowAbilities.length > 0).length} use saves; ` +
      `${relevant.filter(entry => entry.spellAttack).length} use spell attacks`,
    );
  }

  writeKotlin(generated.filter(entry => entry.savingThrowAbilities.length > 0 || entry.spellAttack));
} finally {
  rmSync(temporaryRoot, { recursive: true, force: true });
}

function normalizedLines(raw, joinPositionedWords = false) {
  let value = raw;
  if (joinPositionedWords) value = value.replace(/\t\r[ \u00a0]*/g, " ");
  return value
    .replace(/\u00ad/g, "")
    .replace(/[‐‑‒–—]/g, "-")
    .replace(/\r\n?/g, "\n")
    .replace(/\f/g, "\n")
    .split("\n")
    .map(line => line.replace(/[\t\u00a0]+/g, " ").replace(/\s+/g, " ").trim());
}

function normalizeName(value) {
  return value
    .normalize("NFKD")
    .replace(/[’‘]/g, "'")
    .replace(/[^A-Za-z0-9']+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

function spellHeadings(lines, classification) {
  const result = [];
  for (let index = 0; index < lines.length - 1; index += 1) {
    if (!lines[index]) continue;
    const classificationIndex = lines.findIndex((line, candidateIndex) =>
      candidateIndex > index && candidateIndex <= index + 4 && Boolean(line),
    );
    if (classificationIndex > index && classification.test(lines[classificationIndex])) {
      result.push({ name: lines[index], index });
    }
  }
  return result;
}

function writeKotlin(entries) {
  const lines = [
    "package app.dulliesanddungeons.rules",
    "",
    "/** Generated by scripts/generate-srd-spell-combat-metadata.mjs. Do not hand-edit. */",
    "internal data class SrdSpellCombatMetadata(",
    "    val savingThrowAbilities: Set<String> = emptySet(),",
    "    val spellAttack: Boolean = false,",
    ")",
    "",
    "internal object SrdSpellCombatCatalog {",
    "    private val entries: Map<String, SrdSpellCombatMetadata> = mapOf(",
  ];
  for (const entry of entries) {
    const abilities = entry.savingThrowAbilities.length === 0
      ? "emptySet()"
      : `setOf(${entry.savingThrowAbilities.map(kotlinString).join(", ")})`;
    lines.push(
      `        ${kotlinString(`${entry.revision}:${entry.id}`)} to SrdSpellCombatMetadata(${abilities}, ${entry.spellAttack}),`,
    );
  }
  lines.push(
    "    )",
    "",
    "    fun find(revision: SrdSpellRevision, spellId: String): SrdSpellCombatMetadata =",
    "        entries[\"${revision.name}:$spellId\"] ?: SrdSpellCombatMetadata()",
    "}",
    "",
  );
  const path = join(
    repoRoot,
    "shared", "src", "commonMain", "kotlin", "app", "dulliesanddungeons", "rules", "SrdSpellCombatCatalog.kt",
  );
  writeFileSync(path, lines.join("\n"), "utf8");
}

function kotlinString(value) {
  return JSON.stringify(value).replaceAll("$", "\\$");
}
