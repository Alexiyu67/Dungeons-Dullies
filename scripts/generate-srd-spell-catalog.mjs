#!/usr/bin/env node

/**
 * Generates the checked-in, offline spell catalog for every SRD class from the two
 * official CC-BY SRD PDFs. The PDFs are inputs to this audit tool only; Android
 * builds consume the generated JSON/Kotlin and never access the network.
 *
 * Usage:
 *   node scripts/generate-srd-spell-catalog.mjs \
 *     --srd51 path/to/SRD_CC_v5.1.pdf \
 *     --srd521 path/to/SRD_CC_v5.2.1.pdf
 *
 * Requires Poppler's `pdftotext` on PATH.
 */

import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const args = new Map();
for (let index = 2; index < process.argv.length; index += 2) {
  args.set(process.argv[index], process.argv[index + 1]);
}

const pdf51 = args.get("--srd51");
const pdf521 = args.get("--srd521");
if (!pdf51 || !pdf521) {
  throw new Error("Both --srd51 and --srd521 are required.");
}

const sourceDefinitions = {
  SRD_5_1: {
    revision: "2014-srd-5.1",
    packId: "srd-5.1-spells",
    work: "System Reference Document 5.1",
    shortWork: "SRD 5.1",
    sourceUrl: "https://media.wizards.com/2023/downloads/dnd/SRD_CC_v5.1.pdf",
    expectedSha256: "2504d2a0abb0a4d491a939be4f17910a2dde0312570ab8d208080225ccf0a1f0",
    expectedCounts: { unique: 319, bard: 112, cleric: 105, druid: 105, paladin: 31, ranger: 37, sorcerer: 120, warlock: 64, wizard: 204 },
    pdf: resolve(pdf51),
  },
  SRD_5_2_1: {
    revision: "2024-srd-5.2.1",
    packId: "srd-5.2.1-spells",
    work: "System Reference Document 5.2.1",
    shortWork: "SRD 5.2.1",
    sourceUrl: "https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf",
    expectedSha256: "8974902d109d6e63672d7c490bde9ccf052410503d9cfa768237154fbc5e3d87",
    expectedCounts: { unique: 338, bard: 129, cleric: 109, druid: 124, paladin: 38, ranger: 48, sorcerer: 138, warlock: 72, wizard: 217 },
    pdf: resolve(pdf521),
  },
};

const schools = [
  "Abjuration",
  "Conjuration",
  "Divination",
  "Enchantment",
  "Evocation",
  "Illusion",
  "Necromancy",
  "Transmutation",
];

const spellClasses = ["BARD", "CLERIC", "DRUID", "PALADIN", "RANGER", "SORCERER", "WARLOCK", "WIZARD"];
const classDisplayNames = Object.fromEntries(spellClasses.map(value => [value, titleCase(value)]));

const schoolDe = {
  Abjuration: "Bannmagie",
  Conjuration: "Beschwörung",
  Divination: "Erkenntnismagie",
  Enchantment: "Verzauberung",
  Evocation: "Hervorrufung",
  Illusion: "Illusion",
  Necromancy: "Nekromantie",
  Transmutation: "Verwandlung",
};

const progressionDefinitions = {
  "spell.burning-hands": { baseLevel: 1, base: 3, perLevel: 1, die: 6, suffixEn: "fire", suffixDe: "Feuer" },
  "spell.chromatic-orb": { baseLevel: 1, base: 3, perLevel: 1, die: 8, suffixEn: "chosen damage", suffixDe: "gewählter Schaden" },
  "spell.thunderwave": { baseLevel: 1, base: 2, perLevel: 1, die: 8, suffixEn: "thunder", suffixDe: "Schall" },
  "spell.ray-of-sickness": { baseLevel: 1, base: 2, perLevel: 1, die: 8, suffixEn: "poison", suffixDe: "Gift" },
  "spell.shatter": { baseLevel: 2, base: 3, perLevel: 1, die: 8, suffixEn: "thunder", suffixDe: "Schall" },
  "spell.fireball": { baseLevel: 3, base: 8, perLevel: 1, die: 6, suffixEn: "fire", suffixDe: "Feuer" },
  "spell.lightning-bolt": { baseLevel: 3, base: 8, perLevel: 1, die: 6, suffixEn: "lightning", suffixDe: "Blitz" },
  "spell.blight": { baseLevel: 4, base: 8, perLevel: 1, die: 8, suffixEn: "necrotic", suffixDe: "nekrotisch" },
  "spell.cone-of-cold": { baseLevel: 5, base: 8, perLevel: 1, die: 8, suffixEn: "cold", suffixDe: "Kälte" },
};

const tempRoot = mkdtempSync(join(tmpdir(), "dullies-srd-spells-"));

try {
  for (const source of Object.values(sourceDefinitions)) {
    const actualHash = sha256(readFileSync(source.pdf));
    if (actualHash !== source.expectedSha256) {
      throw new Error(`${source.work}: SHA-256 ${actualHash} does not match pinned ${source.expectedSha256}`);
    }
  }

  const raw51Lists = extractPdf(sourceDefinitions.SRD_5_1.pdf, ["-raw", "-f", "103", "-l", "114"]);
  const raw51All = extractPdf(sourceDefinitions.SRD_5_1.pdf, ["-raw"]);
  const raw521Classes = {
    BARD: extractPdf(sourceDefinitions.SRD_5_2_1.pdf, ["-raw", "-f", "33", "-l", "36"]),
    CLERIC: extractPdf(sourceDefinitions.SRD_5_2_1.pdf, ["-raw", "-f", "38", "-l", "41"]),
    DRUID: extractPdf(sourceDefinitions.SRD_5_2_1.pdf, ["-raw", "-f", "44", "-l", "48"]),
    PALADIN: extractPdf(sourceDefinitions.SRD_5_2_1.pdf, ["-raw", "-f", "55", "-l", "58"]),
    RANGER: extractPdf(sourceDefinitions.SRD_5_2_1.pdf, ["-raw", "-f", "60", "-l", "63"]),
    SORCERER: extractPdf(sourceDefinitions.SRD_5_2_1.pdf, ["-raw", "-f", "67", "-l", "71"]),
    WARLOCK: extractPdf(sourceDefinitions.SRD_5_2_1.pdf, ["-raw", "-f", "74", "-l", "77"]),
    WIZARD: extractPdf(sourceDefinitions.SRD_5_2_1.pdf, ["-raw", "-f", "79", "-l", "82"]),
  };

  const lists51 = {
    BARD: parse51ClassList(raw51Lists, "Bard Spells", "Cleric Spells"),
    CLERIC: parse51ClassList(raw51Lists, "Cleric Spells", "Druid Spells"),
    DRUID: parse51ClassList(raw51Lists, "Druid Spells", "Paladin Spells"),
    PALADIN: parse51ClassList(raw51Lists, "Paladin Spells", "Ranger Spells"),
    RANGER: parse51ClassList(raw51Lists, "Ranger Spells", "Sorcerer Spells"),
    SORCERER: parse51ClassList(raw51Lists, "Sorcerer Spells", "Warlock Spells"),
    WARLOCK: parse51ClassList(raw51Lists, "Warlock Spells", "Wizard Spells"),
    WIZARD: parse51ClassList(raw51Lists, "Wizard Spells", "Spell Descriptions"),
  };
  const names51 = [...new Set(Object.values(lists51).flatMap(levels => levels.flatMap(level => level.names)))];
  const metadata51 = parse51Metadata(raw51All, names51);

  const lists521 = Object.fromEntries(spellClasses.map(className => {
    const displayName = classDisplayNames[className];
    return [className, parse521ClassList(raw521Classes[className], displayName, `${displayName} Subclass`)];
  }));

  const revisions = [
    buildRevision(sourceDefinitions.SRD_5_1, lists51, metadata51),
    buildRevision(sourceDefinitions.SRD_5_2_1, lists521),
  ];

  validateCatalog(revisions);
  writeCatalogSource(revisions);
  writePacks(revisions);
  writeKotlin(revisions);

  for (const revision of revisions) {
    const counts = spellClasses.map(className => `${classDisplayNames[className]} ${revision.spells.filter(spell => spell.classes.includes(className)).length}`);
    console.log(`${revision.source.revision}: ${revision.spells.length} unique; ${counts.join("; ")}`);
  }
} finally {
  rmSync(tempRoot, { recursive: true, force: true });
}

function sha256(buffer) {
  return createHash("sha256").update(buffer).digest("hex");
}

function extractPdf(pdf, options) {
  const output = join(tempRoot, `${createHash("sha1").update(pdf + options.join(" ")).digest("hex")}.txt`);
  execFileSync("pdftotext", [...options, pdf, output], { stdio: "inherit" });
  return readFileSync(output, "utf8");
}

function repairText(value) {
  return value
    .replaceAll("â€™", "’")
    .replaceAll("â€œ", "“")
    .replaceAll("â€", "”")
    .replaceAll("â€”", "—")
    .replaceAll("â€“", "—")
    .replaceAll("Â­", "")
    .replaceAll("­", "")
    .replace(/[‐‑‒–—]/g, "-")
    .replace(/-{2,}/g, "-")
    .replace(/[\u200b\ufeff]/g, "");
}

function normalizedLines(raw, joinPositionedWords = false) {
  let value = repairText(raw);
  if (joinPositionedWords) value = value.replace(/\t\r[ \u00a0]*/g, " ");
  return value
    .replace(/\r\n/g, "\n")
    .replace(/\r/g, "\n")
    .replace(/\f/g, "\n")
    .split("\n")
    .map(line => line.replace(/[\t\u00a0]+/g, " ").replace(/\s+/g, " ").trim());
}

function parse51ClassList(raw, startMarker, endMarker) {
  const lines = normalizedLines(raw, true);
  const start = lines.indexOf(startMarker);
  const end = lines.indexOf(endMarker, start + 1);
  if (start < 0 || end < 0) throw new Error(`Could not isolate ${startMarker}`);

  const levels = new Map();
  let level = null;
  for (const line of lines.slice(start + 1, end)) {
    if (!line || /^System Reference Document 5\.1(?: \d+)?$/.test(line) || /^\d+$/.test(line)) continue;
    if (line === "Cantrips (0 Level)") {
      level = 0;
      levels.set(level, []);
      continue;
    }
    const levelMatch = /^(\d+)(?:st|nd|rd|th) Level$/.exec(line);
    if (levelMatch) {
      level = Number(levelMatch[1]);
      levels.set(level, []);
      continue;
    }
    if (level !== null) levels.get(level).push(normalizeName(line));
  }
  return [...levels].map(([parsedLevel, names]) => ({ level: parsedLevel, names }));
}

function parse51Metadata(raw, names) {
  const lines = normalizedLines(raw, true);
  const descriptionsStart = lines.indexOf("Spell Descriptions");
  if (descriptionsStart < 0) throw new Error("SRD 5.1 Spell Descriptions section was not found");
  const descriptionLines = lines.slice(descriptionsStart + 1);
  const result = new Map();
  const classification = new RegExp(`^(?:(${schools.join("|")}) cantrip|[1-9](?:st|nd|rd|th)-level (${schools.join("|")}))(?:\\s+\\(ritual\\))?$`, "i");

  for (const name of names) {
    let found = -1;
    for (let index = 0; index < descriptionLines.length - 1; index += 1) {
      if (normalizeName(descriptionLines[index]) !== name) continue;
      const next = descriptionLines.slice(index + 1, index + 5).find(Boolean) ?? "";
      if (classification.test(next)) {
        found = index;
        break;
      }
    }
    if (found < 0) throw new Error(`SRD 5.1 metadata header not found for ${name}`);

    const nextLines = descriptionLines.slice(found + 1, found + 20).filter(Boolean);
    const header = nextLines[0];
    const headerMatch = classification.exec(header);
    const fieldWindow = nextLines.join(" ");
    const components = /Components:\s*(.*?)(?=Duration:)/i.exec(fieldWindow)?.[1] ?? "";
    const duration = /Duration:\s*(.*?)(?=(?:You|A |An |The |Choose|This |Each |One |Make |Until |Your ))/i.exec(fieldWindow)?.[1] ?? "";
    result.set(name, {
      school: titleCase(headerMatch[1] ?? headerMatch[2]),
      concentration: /Concentration/i.test(duration),
      ritual: /\(ritual\)/i.test(header),
      specificMaterial: /\bM\b/.test(components) && /(?:worth|consume|cost)/i.test(components),
    });
  }
  return result;
}

function parse521ClassList(raw, className, endMarker) {
  const lines = normalizedLines(raw);
  const start = lines.indexOf(`${className} Spell List`);
  const end = lines.findIndex((line, index) => index > start && line.startsWith(endMarker));
  if (start < 0 || end < 0) throw new Error(`Could not isolate SRD 5.2.1 ${className} list`);
  const levels = new Map();
  let level = null;
  const rowPattern = new RegExp(`^(.+?) (${schools.join("|")}) (.+)$`);

  for (const line of lines.slice(start + 1, end)) {
    if (/^Cantrips \(Level 0/.test(line)) {
      level = 0;
      levels.set(level, []);
      continue;
    }
    const levelMatch = new RegExp(`^Level (\\d+) ${className} Spells$`).exec(line);
    if (levelMatch) {
      level = Number(levelMatch[1]);
      levels.set(level, []);
      continue;
    }
    if (level === null || line === "Spell School Special" || /^System Reference Document/.test(line) || /^\d+$/.test(line)) continue;
    const row = rowPattern.exec(line);
    if (!row) continue;
    const flags = row[3];
    levels.get(level).push({
      name: normalizeName(row[1]),
      school: row[2],
      concentration: /(?:^|, )C(?:,|$)/.test(flags),
      ritual: /(?:^|, )R(?:,|$)/.test(flags),
      specificMaterial: /(?:^|, )M(?:,|$)/.test(flags),
    });
  }
  return [...levels].map(([parsedLevel, rows]) => ({ level: parsedLevel, rows }));
}

function buildRevision(source, lists, metadata = null) {
  const spells = new Map();
  for (const [className, levels] of Object.entries(lists)) {
    for (const levelData of levels) {
      const rows = levelData.rows ?? levelData.names.map(name => ({ name, ...metadata.get(name) }));
      for (const row of rows) {
        const stableId = stableSpellId(row.name);
        const existing = spells.get(stableId);
        if (existing) {
          if (existing.level !== levelData.level || existing.school !== row.school) {
            throw new Error(`${source.revision}: conflicting metadata for ${row.name}`);
          }
          existing.classes.push(className);
          continue;
        }
        const aliases = aliasesFor(row.name);
        const texts = spellTexts(row.name, levelData.level, row.school, row);
        spells.set(stableId, {
          id: stableId,
          revision: source.revision,
          level: levelData.level,
          classes: [className],
          school: row.school,
          concentration: row.concentration,
          ritual: row.ritual,
          specificMaterial: row.specificMaterial,
          aliases,
          en: texts.en,
          de: texts.de,
          castPreviews: castPreviews(source.revision, stableId, levelData.level),
        });
      }
    }
  }
  return {
    source,
    spells: [...spells.values()].sort(spellComparator),
  };
}

function normalizeName(name) {
  return repairText(name).replace(/[‘’]/g, "'").replace(/\s+/g, " ").trim();
}

function titleCase(value) {
  return value[0].toUpperCase() + value.slice(1).toLowerCase();
}

function slugify(value) {
  return value
    .normalize("NFKD")
    .replace(/[’']/g, "")
    .replace(/[^A-Za-z0-9]+/g, "-")
    .replace(/^-|-$/g, "")
    .toLowerCase();
}

function stableSpellId(name) {
  // SRD 5.2.1 renamed Feeblemind to Befuddlement; one ID keeps old
  // characters and cross-edition searches stable.
  if (name === "Befuddlement" || name === "Feeblemind") return "spell.feeblemind";
  return `spell.${slugify(name)}`;
}

function aliasesFor(name) {
  const aliases = new Set([name.toLocaleLowerCase("en"), slugify(name).replaceAll("-", " ")]);
  if (name === "Befuddlement") aliases.add("feeblemind");
  if (name === "Feeblemind") aliases.add("befuddlement");
  return [...aliases].sort();
}

function spellTexts(name, level, school, flags) {
  const enParts = [level === 0 ? `${school} cantrip` : `Level ${level} ${school} spell`];
  const deParts = [level === 0 ? `Zaubertrick der Schule ${schoolDe[school]}` : `Grad-${level}-Zauber der Schule ${schoolDe[school]}`];
  if (flags.concentration) {
    enParts.push("concentration");
    deParts.push("Konzentration");
  }
  if (flags.ritual) {
    enParts.push("ritual");
    deParts.push("Ritual");
  }
  if (flags.specificMaterial) {
    enParts.push("specific material");
    deParts.push("besondere Materialkomponente");
  }
  return {
    en: { name, summary: `${enParts.join("; ")}.` },
    de: { name, summary: `${deParts.join("; ")}.` },
  };
}

function castPreviews(revision, stableId, level) {
  const definition = progressionDefinitions[stableId];
  if (!definition || level !== definition.baseLevel) return [];
  // A spell's 2024 and 2014 dice progressions are represented separately by
  // revision entries even when the numbers happen to match.
  return Array.from({ length: 10 - level }, (_, offset) => {
    const slotLevel = level + offset;
    const dice = definition.base + offset * definition.perLevel;
    return {
      slotLevel,
      en: `${dice}d${definition.die} ${definition.suffixEn}`,
      de: `${dice}d${definition.die} ${definition.suffixDe}`,
    };
  });
}

function spellComparator(left, right) {
  return left.level - right.level || left.en.name.localeCompare(right.en.name, "en", { sensitivity: "base" }) || left.id.localeCompare(right.id);
}

function validateCatalog(revisions) {
  for (const revision of revisions) {
    const ids = revision.spells.map(spell => spell.id);
    if (new Set(ids).size !== ids.length) throw new Error(`${revision.source.revision}: duplicate stable ID`);
    for (const spell of revision.spells) {
      if (!Number.isInteger(spell.level) || spell.level < 0 || spell.level > 9) throw new Error(`${spell.id}: invalid level`);
      if (!schools.includes(spell.school)) throw new Error(`${spell.id}: invalid school ${spell.school}`);
      if (spell.classes.length === 0) throw new Error(`${spell.id}: no class membership`);
      if (!spell.en.name || !spell.de.name || !spell.en.summary || !spell.de.summary) throw new Error(`${spell.id}: incomplete locale text`);
      if (spell.castPreviews.some(preview => preview.slotLevel < Math.max(1, spell.level))) throw new Error(`${spell.id}: invalid cast preview`);
    }
    for (const className of spellClasses) {
      const classSpells = revision.spells.filter(spell => spell.classes.includes(className));
      const sorted = [...classSpells].sort(spellComparator);
      if (classSpells.some((spell, index) => spell.id !== sorted[index].id)) throw new Error(`${revision.source.revision}: ${className} not sorted`);
    }
    if (revision.source.expectedCounts) {
      const counts = Object.fromEntries([
        ["unique", revision.spells.length],
        ...spellClasses.map(className => [className.toLowerCase(), revision.spells.filter(spell => spell.classes.includes(className)).length]),
      ]);
      for (const [key, expected] of Object.entries(revision.source.expectedCounts)) {
        if (counts[key] !== expected) {
          throw new Error(`${revision.source.revision}: ${key} count ${counts[key]} does not match audited ${expected}`);
        }
      }
    }
  }
}

function writeCatalogSource(revisions) {
  const path = join(repoRoot, "content", "catalogs", "srd-wizard-sorcerer-spells.json");
  mkdirSync(dirname(path), { recursive: true });
  writeJson(path, {
    $schema: "../schema/srd-spell-catalog.schema.json",
    schemaVersion: 1,
    generatedBy: "scripts/generate-srd-spell-catalog.mjs",
    sources: revisions.map(revision => ({
      revision: revision.source.revision,
      work: revision.source.work,
      url: revision.source.sourceUrl,
      sha256: revision.source.expectedSha256,
      uniqueSpellCount: revision.spells.length,
      classSpellCounts: Object.fromEntries(spellClasses.map(className => [className.toLowerCase(), revision.spells.filter(spell => spell.classes.includes(className)).length])),
    })),
    entries: revisions.flatMap(revision => revision.spells),
  });
}

function writePacks(revisions) {
  for (const revision of revisions) {
    const { source, spells } = revision;
    const packRoot = join(repoRoot, "content", "packs", source.packId);
    mkdirSync(packRoot, { recursive: true });
    const reportName = `${source.packId}.coverage.json`;
    writeJson(join(packRoot, "manifest.json"), {
      $schema: "../../schema/content-pack-manifest.schema.json",
      schemaVersion: 1,
      id: source.packId,
      version: "1.0.0",
      ruleset: { id: "fifth-edition", revision: source.revision },
      locales: ["en", "de"],
      entryFiles: { en: "entries.en.json", de: "entries.de.json" },
      dependencies: [],
      license: {
        expression: "CC-BY-4.0",
        noticeFile: "../../../licenses/NOTICE.md",
        sourceWorks: [{ title: source.work, url: source.sourceUrl, license: "CC-BY-4.0", snapshot: source.revision, sha256: source.expectedSha256 }],
      },
      contentStatus: "audited-snapshot",
      distributionReady: true,
      coverageReport: `../../reports/${reportName}`,
      notes: "Complete eight-class spell index for this SRD revision; summaries are original compact catalog copy, not replacement rules text.",
    });
    for (const locale of ["en", "de"]) {
      writeJson(join(packRoot, `entries.${locale}.json`), {
        $schema: "../../schema/localized-entries.schema.json",
        schemaVersion: 1,
        packId: source.packId,
        locale,
        entries: spells.map(spell => packEntry(source, spell, locale)),
      });
    }
    writeJson(join(repoRoot, "content", "reports", reportName), coverageReport(revision));
  }
}

function packEntry(source, spell, locale) {
  const text = spell[locale];
  return {
    id: `${source.packId.replace("-spells", "")}.${spell.id}`,
    kind: "spell",
    name: text.name,
    summary: text.summary,
    beginnerTip: locale === "de"
      ? "Wähle beim Wirken einen verfügbaren Zauberplatz; nur die angezeigten Stufen verändern den Kurzvorschauwert."
      : "Choose an available slot when casting; only listed levels change the compact preview value.",
    keywords: [...new Set([text.name.toLocaleLowerCase(locale), ...spell.aliases, spell.school.toLocaleLowerCase("en"), ...spell.classes.map(value => value.toLocaleLowerCase("en"))])].sort(),
    automation: { level: "informational", command: null, actionCost: null, requiresTableInput: false },
    source: {
      work: source.shortWork,
      locator: `${spell.classes.map(value => titleCase(value)).join("/")} spell list; Spell Descriptions: ${spell.en.name}`,
      adaptation: locale === "de"
        ? "Originale kurze Katalogzusammenfassung; die zitierte SRD-Fassung bleibt maßgeblich."
        : "Original compact catalog summary; the cited SRD revision remains authoritative.",
    },
    spell: {
      stableId: spell.id,
      revision: spell.revision,
      level: spell.level,
      classes: spell.classes.map(value => value.toLocaleLowerCase("en")),
      school: spell.school.toLocaleLowerCase("en"),
      concentration: spell.concentration,
      ritual: spell.ritual,
      specificMaterial: spell.specificMaterial,
      aliases: spell.aliases,
      castPreviews: spell.castPreviews.map(preview => ({ slotLevel: preview.slotLevel, value: preview[locale] })),
    },
  };
}

function coverageReport(revision) {
  const { source, spells } = revision;
  return {
    $schema: "../schema/coverage-report.schema.json",
    schemaVersion: 1,
    packId: source.packId,
    generatedAt: "2026-09-02T00:00:00.000Z",
    sourceSnapshot: {
      name: source.work,
      url: source.sourceUrl,
      revision: source.revision,
      retrievedAt: "2026-09-02T00:00:00.000Z",
      sha256: source.expectedSha256,
    },
    summary: { seen: spells.length, included: spells.length, excluded: 0, pendingReview: 0 },
    records: spells.map(spell => ({
      sourceId: spell.id,
      category: "class-list-spell",
      decision: "included",
      reason: `Appears on the ${spell.classes.map(value => titleCase(value)).join(" and ")} class spell list.`,
      licenseEvidence: `${source.work}, CC-BY-4.0, pinned SHA-256 ${source.expectedSha256}`,
      outputId: `${source.packId.replace("-spells", "")}.${spell.id}`,
    })),
  };
}

function writeKotlin(revisions) {
  const entries = revisions.flatMap(revision => revision.spells);
  const chunks = Array.from({ length: Math.ceil(entries.length / 40) }, (_, index) => entries.slice(index * 40, index * 40 + 40));
  const lines = [];
  lines.push("package app.dulliesanddungeons.rules");
  lines.push("");
  lines.push("/** Generated by scripts/generate-srd-spell-catalog.mjs. Do not hand-edit. */");
  lines.push("internal enum class SrdSpellRevision { SRD_5_1, SRD_5_2_1 }");
  lines.push(`internal enum class SrdSpellClass { ${spellClasses.join(", ")} }`);
  lines.push("internal data class SrdSpellText(val name: String, val summary: String)");
  lines.push("internal data class SrdSpellCastPreview(val slotLevel: Int, val en: String, val de: String)");
  lines.push("internal data class SrdSpellCatalogEntry(");
  lines.push("    val id: String,");
  lines.push("    val revision: SrdSpellRevision,");
  lines.push("    val level: Int,");
  lines.push("    val classes: Set<SrdSpellClass>,");
  lines.push("    val school: String,");
  lines.push("    val concentration: Boolean,");
  lines.push("    val ritual: Boolean,");
  lines.push("    val specificMaterial: Boolean,");
  lines.push("    val aliases: Set<String>,");
  lines.push("    val en: SrdSpellText,");
  lines.push("    val de: SrdSpellText,");
  lines.push("    val castPreviews: List<SrdSpellCastPreview> = emptyList(),");
  lines.push(")");
  lines.push("");
  lines.push("internal object SrdSpellCatalog {");
  lines.push("    val entries: List<SrdSpellCatalogEntry> = listOf(");
  for (let index = 0; index < chunks.length; index += 1) lines.push(`        entries${index}(),`);
  lines.push("    ).flatten()");
  lines.push("");
  chunks.forEach((chunk, index) => {
    lines.push(`    private fun entries${index}(): List<SrdSpellCatalogEntry> = listOf(`);
    for (const spell of chunk) lines.push(kotlinEntry(spell));
    lines.push("    )");
    lines.push("");
  });
  lines.push("    fun forClass(revision: SrdSpellRevision, spellClass: SrdSpellClass): List<SrdSpellCatalogEntry> =");
  lines.push("        entries.filter { it.revision == revision && spellClass in it.classes }");
  lines.push("");
  lines.push("    fun find(revision: SrdSpellRevision, stableId: String): SrdSpellCatalogEntry? =");
  lines.push("        entries.firstOrNull { it.revision == revision && it.id == stableId }");
  lines.push("}");
  lines.push("");
  const path = join(repoRoot, "shared", "src", "commonMain", "kotlin", "app", "dulliesanddungeons", "rules", "SrdSpellCatalog.kt");
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, lines.join("\n"), "utf8");
}

function kotlinEntry(spell) {
  const revision = spell.revision === "2014-srd-5.1" ? "SRD_5_1" : "SRD_5_2_1";
  const classes = spell.classes.map(value => `SrdSpellClass.${value}`).join(", ");
  const aliases = spell.aliases.map(kotlinString).join(", ");
  const previews = spell.castPreviews.length === 0
    ? "emptyList()"
    : `listOf(${spell.castPreviews.map(preview => `SrdSpellCastPreview(${preview.slotLevel}, ${kotlinString(preview.en)}, ${kotlinString(preview.de)})`).join(", ")})`;
  return `        SrdSpellCatalogEntry(${kotlinString(spell.id)}, SrdSpellRevision.${revision}, ${spell.level}, setOf(${classes}), ${kotlinString(spell.school)}, ${spell.concentration}, ${spell.ritual}, ${spell.specificMaterial}, setOf(${aliases}), SrdSpellText(${kotlinString(spell.en.name)}, ${kotlinString(spell.en.summary)}), SrdSpellText(${kotlinString(spell.de.name)}, ${kotlinString(spell.de.summary)}), ${previews}),`;
}

function kotlinString(value) {
  return JSON.stringify(value).replaceAll("$", "\\$");
}

function writeJson(path, value) {
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}
