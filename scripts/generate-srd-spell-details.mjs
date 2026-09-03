#!/usr/bin/env node

/**
 * Generates exact casting fields and rules text for concentration spells in the
 * checked-in SRD catalog. Source text comes from the two pinned official SRD
 * PDFs; application builds consume only the generated Kotlin file.
 *
 * Usage:
 *   node scripts/generate-srd-spell-details.mjs \
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
    shortWork: "SRD 5.1",
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
    shortWork: "SRD 5.2.1",
    pdf: args.get("--srd521"),
    sha256: "8974902d109d6e63672d7c490bde9ccf052410503d9cfa768237154fbc5e3d87",
    firstPage: 107,
    lastPage: 208,
    joinPositionedWords: false,
    classification: /^(?:Level [1-9] (?:Abjuration|Conjuration|Divination|Enchantment|Evocation|Illusion|Necromancy|Transmutation)|(?:Abjuration|Conjuration|Divination|Enchantment|Evocation|Illusion|Necromancy|Transmutation) Cantrip) \(/i,
  },
];

if (definitions.some(definition => !definition.pdf)) {
  throw new Error("Both --srd51 and --srd521 are required.");
}

const catalog = JSON.parse(readFileSync(join(repoRoot, "content", "catalogs", "srd-wizard-sorcerer-spells.json"), "utf8"));
const temporaryRoot = mkdtempSync(join(tmpdir(), "dullies-spell-details-"));
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

    for (const entry of revisionEntries.filter(entry => entry.concentration)) {
      const headingIndex = headings.findIndex(candidate => normalizeName(candidate.name) === normalizeName(entry.en.name));
      if (headingIndex < 0) {
        missing.push(entry.en.name);
        continue;
      }
      const heading = headings[headingIndex];
      const nextHeading = headings[headingIndex + 1];
      const details = parseDetails(lines.slice(heading.index + 1, nextHeading?.index ?? lines.length), entry.en.name);
      generated.push({
        revision: definition.kotlinRevision,
        id: entry.id,
        source: definition.shortWork,
        ...details,
      });
    }

    if (missing.length > 0) {
      throw new Error(`${definition.revision}: missing concentration spell headings: ${missing.join(", ")}`);
    }
    const revisionDetails = generated.filter(entry => entry.revision === definition.kotlinRevision);
    const expected = revisionEntries.filter(entry => entry.concentration).length;
    if (revisionDetails.length !== expected) {
      throw new Error(`${definition.revision}: generated ${revisionDetails.length} details; expected ${expected}`);
    }
    console.log(`${definition.revision}: ${revisionDetails.length} concentration spell descriptions generated`);
  }

  writeKotlin(generated);
} finally {
  rmSync(temporaryRoot, { recursive: true, force: true });
}

function normalizedLines(raw, joinPositionedWords) {
  let value = raw;
  if (joinPositionedWords) value = value.replace(/\t\r[ \u00a0]*/g, " ");
  return value
    .replace(/\u00ad/g, "")
    .replace(/[‐‑‒–—]/g, "-")
    .replace(/-{2,}/g, "-")
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

function parseDetails(rawLines, spellName) {
  const lines = rawLines.filter(line =>
    line && !/^System Reference Document (?:5\.1|5\.2\.1)(?: \d+)?$/.test(line) && !/^\d+$/.test(line),
  );
  const castingIndex = lines.findIndex(line => line.startsWith("Casting Time:"));
  const rangeIndex = lines.findIndex(line => line.startsWith("Range:"));
  const componentsIndex = lines.findIndex(line => /^Components?:/.test(line));
  const durationIndex = lines.findIndex(line => line.startsWith("Duration:"));
  if (!(castingIndex >= 0 && castingIndex < rangeIndex && rangeIndex < componentsIndex && componentsIndex < durationIndex)) {
    throw new Error(`${spellName}: incomplete or out-of-order casting fields`);
  }

  const castingTime = fieldValue(lines.slice(castingIndex, rangeIndex), "Casting Time:");
  const range = fieldValue(lines.slice(rangeIndex, componentsIndex), "Range:");
  const componentsLine = lines[componentsIndex];
  const components = fieldValue(lines.slice(componentsIndex, durationIndex), componentsLine.startsWith("Components:") ? "Components:" : "Component:");
  const duration = fieldValue([lines[durationIndex]], "Duration:");
  const effect = joinProse(lines.slice(durationIndex + 1));
  if (![castingTime, range, components, duration, effect].every(Boolean)) {
    throw new Error(`${spellName}: one or more spell details are blank`);
  }
  const durationRounds = roundsFor(duration);
  if (durationRounds == null) throw new Error(`${spellName}: unsupported concentration duration ${duration}`);
  return { castingTime, range, components, duration, effect, durationRounds };
}

function fieldValue(lines, prefix) {
  const first = lines[0].slice(prefix.length).trim();
  return joinProse([first, ...lines.slice(1)]);
}

function joinProse(lines) {
  return lines.join("\n")
    .replace(/-\n(?=[a-z])/g, "")
    .replace(/\n+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function roundsFor(duration) {
  const match = /(?:up to\s+)?(\d+|one|two|three|four|five|six|seven|eight|nine|ten)\s+(round|minute|hour|day)s?/i.exec(duration);
  if (!match) return null;
  const multiplier = { round: 1, minute: 10, hour: 600, day: 14_400 }[match[2].toLowerCase()];
  const words = { one: 1, two: 2, three: 3, four: 4, five: 5, six: 6, seven: 7, eight: 8, nine: 9, ten: 10 };
  const amount = Number(match[1]) || words[match[1].toLowerCase()];
  return amount * multiplier;
}

function writeKotlin(entries) {
  const chunks = Array.from({ length: Math.ceil(entries.length / 20) }, (_, index) => entries.slice(index * 20, index * 20 + 20));
  const lines = [
    "package app.dulliesanddungeons.rules",
    "",
    "/** Generated by scripts/generate-srd-spell-details.mjs. Do not hand-edit. */",
    "internal data class SrdSpellDetails(",
    "    val castingTime: String,",
    "    val range: String,",
    "    val components: String,",
    "    val duration: String,",
    "    val effect: String,",
    "    val durationRounds: Int,",
    "    val source: String,",
    ")",
    "",
    "internal object SrdSpellDetailsCatalog {",
    "    private val entries: Map<String, SrdSpellDetails> = listOf(",
  ];
  for (let index = 0; index < chunks.length; index += 1) lines.push(`        entries${index}(),`);
  lines.push("    ).flatten().toMap()", "");
  chunks.forEach((chunk, index) => {
    lines.push(`    private fun entries${index}(): List<Pair<String, SrdSpellDetails>> = listOf(`);
    for (const entry of chunk) {
      lines.push(
        `        ${kotlinString(`${entry.revision}:${entry.id}`)} to SrdSpellDetails(` +
        `${kotlinString(entry.castingTime)}, ${kotlinString(entry.range)}, ${kotlinString(entry.components)}, ` +
        `${kotlinString(entry.duration)}, ${kotlinString(entry.effect)}, ${entry.durationRounds}, ${kotlinString(entry.source)}),`,
      );
    }
    lines.push("    )", "");
  });
  lines.push(
    "    fun find(revision: SrdSpellRevision, spellId: String): SrdSpellDetails? =",
    "        entries[\"${revision.name}:$spellId\"]",
    "}",
    "",
  );
  const path = join(
    repoRoot,
    "shared", "src", "commonMain", "kotlin", "app", "dulliesanddungeons", "rules", "SrdSpellDetailsCatalog.kt",
  );
  writeFileSync(path, lines.join("\n"), "utf8");
}

function kotlinString(value) {
  return JSON.stringify(value).replaceAll("$", "\\$");
}
