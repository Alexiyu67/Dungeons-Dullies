import { existsSync, readFileSync, readdirSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const packsRoot = join(repoRoot, "content", "packs");
const reportsRoot = join(repoRoot, "content", "reports");
const schemasRoot = join(repoRoot, "content", "schema");
const failures = [];

function readJson(path) {
  try {
    return JSON.parse(readFileSync(path, "utf8"));
  } catch (error) {
    failures.push(`${path}: ${error.message}`);
    return null;
  }
}

function requireValue(condition, message) {
  if (!condition) failures.push(message);
}

function validatePack(packDir) {
  const manifestPath = join(packDir, "manifest.json");
  const manifest = readJson(manifestPath);
  if (!manifest) return;

  requireValue(manifest.schemaVersion === 1, `${manifestPath}: unsupported schemaVersion`);
  requireValue(typeof manifest.id === "string" && manifest.id.length > 0, `${manifestPath}: missing id`);
  requireValue(manifest.contentStatus !== "representative-seed" || manifest.distributionReady === false,
    `${manifestPath}: a representative seed cannot be distribution-ready`);

  const localeIds = new Map();
  const localeSpells = new Map();
  for (const locale of manifest.locales ?? []) {
    const relative = manifest.entryFiles?.[locale];
    requireValue(typeof relative === "string", `${manifestPath}: missing entry file for ${locale}`);
    if (typeof relative !== "string") continue;

    const entryPath = join(packDir, relative);
    const file = readJson(entryPath);
    if (!file) continue;
    requireValue(file.schemaVersion === 1, `${entryPath}: unsupported schemaVersion`);
    requireValue(file.packId === manifest.id, `${entryPath}: packId does not match manifest`);
    requireValue(file.locale === locale, `${entryPath}: locale does not match manifest key`);

    const ids = (file.entries ?? []).map((entry) => entry.id);
    requireValue(new Set(ids).size === ids.length, `${entryPath}: duplicate entry ID`);
    for (const entry of file.entries ?? []) {
      requireValue(typeof entry.name === "string" && entry.name.length > 0, `${entryPath}: ${entry.id} has no name`);
      requireValue(["automatic", "guided", "informational"].includes(entry.automation?.level),
        `${entryPath}: ${entry.id} has an invalid automation level`);
      requireValue(typeof entry.source?.work === "string" && typeof entry.source?.locator === "string",
        `${entryPath}: ${entry.id} is missing provenance`);
      if (entry.kind === "spell" && entry.spell) {
        requireValue(/^spell\.[a-z0-9][a-z0-9.-]+$/.test(entry.spell.stableId ?? ""),
          `${entryPath}: ${entry.id} has an invalid stable spell ID`);
        requireValue(Number.isInteger(entry.spell.level) && entry.spell.level >= 0 && entry.spell.level <= 9,
          `${entryPath}: ${entry.id} has an invalid spell level`);
        requireValue(Array.isArray(entry.spell.classes) && entry.spell.classes.length > 0,
          `${entryPath}: ${entry.id} has no spell class membership`);
      }
      if (entry.kind === "subclass") {
        requireValue(/^subclass\.[a-z0-9][a-z0-9.-]+$/.test(entry.subclass?.stableId ?? ""),
          `${entryPath}: ${entry.id} has an invalid stable subclass ID`);
        requireValue(typeof entry.subclass?.parentClassId === "string" && entry.subclass.parentClassId.length > 0,
          `${entryPath}: ${entry.id} has no parent class`);
        requireValue(Number.isInteger(entry.subclass?.selectionLevel) && entry.subclass.selectionLevel >= 1 && entry.subclass.selectionLevel <= 20,
          `${entryPath}: ${entry.id} has an invalid subclass selection level`);
      }
    }
    localeIds.set(locale, new Set(ids));
    localeSpells.set(locale, new Map((file.entries ?? [])
      .filter(entry => entry.kind === "spell" && entry.spell)
      .map(entry => [entry.id, entry.spell])));
  }

  const [referenceLocale, referenceIds] = localeIds.entries().next().value ?? [];
  if (referenceIds) {
    for (const [locale, ids] of localeIds) {
      const missing = [...referenceIds].filter((id) => !ids.has(id));
      const extra = [...ids].filter((id) => !referenceIds.has(id));
      requireValue(missing.length === 0 && extra.length === 0,
        `${manifestPath}: ${locale} IDs differ from ${referenceLocale}; missing=${missing.join(",")} extra=${extra.join(",")}`);
    }
  }

  const [spellReferenceLocale, spellReference] = localeSpells.entries().next().value ?? [];
  if (spellReference) {
    for (const [locale, spells] of localeSpells) {
      for (const [id, reference] of spellReference) {
        const candidate = spells.get(id);
        requireValue(candidate?.stableId === reference.stableId && candidate?.revision === reference.revision &&
          candidate?.level === reference.level && JSON.stringify(candidate?.classes) === JSON.stringify(reference.classes),
          `${manifestPath}: ${locale} spell metadata differs from ${spellReferenceLocale} for ${id}`);
      }
    }
  }

  if (manifest.distributionReady) {
    requireValue(manifest.contentStatus === "audited-snapshot", `${manifestPath}: public packs must be audited snapshots`);
    requireValue(typeof manifest.coverageReport === "string", `${manifestPath}: public packs require a coverage report`);
    const reportPath = resolve(packDir, manifest.coverageReport ?? "");
    requireValue(existsSync(reportPath), `${manifestPath}: coverage report is not readable`);
    const report = existsSync(reportPath) ? readJson(reportPath) : null;
    requireValue(report?.packId === manifest.id, `${manifestPath}: coverage report packId does not match`);
    const noticePath = resolve(packDir, manifest.license?.noticeFile ?? "");
    let notice = "";
    try { notice = readFileSync(noticePath, "utf8"); } catch { failures.push(`${manifestPath}: notice is not readable`); }
    requireValue(!notice.includes("{BRACES}") && !notice.includes("{EXACT TITLE"),
      `${manifestPath}: distribution notice still contains placeholders`);
    for (const source of manifest.license?.sourceWorks ?? []) {
      requireValue(typeof source.sha256 === "string" && /^[a-f0-9]{64}$/.test(source.sha256),
        `${manifestPath}: distribution source ${source.title} needs a pinned SHA-256`);
    }
  }
}

function validateSrdSpellCatalog() {
  const path = join(repoRoot, "content", "catalogs", "srd-wizard-sorcerer-spells.json");
  const catalog = readJson(path);
  if (!catalog) return;
  requireValue(catalog.schemaVersion === 1, `${path}: unsupported schemaVersion`);
  const revisionEntries = new Map();
  for (const entry of catalog.entries ?? []) {
    const key = `${entry.revision}:${entry.id}`;
    requireValue(!revisionEntries.has(key), `${path}: duplicate revision/stable ID ${key}`);
    revisionEntries.set(key, entry);
    requireValue(entry.en?.name && entry.de?.name && entry.en?.summary && entry.de?.summary,
      `${path}: ${key} lacks bilingual text`);
    requireValue(Array.isArray(entry.classes) && entry.classes.length > 0,
      `${path}: ${key} has no class membership`);
    requireValue((entry.castPreviews ?? []).every(preview => preview.slotLevel >= Math.max(1, entry.level) && preview.slotLevel <= 9),
      `${path}: ${key} has an invalid cast preview`);
  }
  for (const source of catalog.sources ?? []) {
    const entries = (catalog.entries ?? []).filter(entry => entry.revision === source.revision);
    requireValue(entries.length === source.uniqueSpellCount,
      `${path}: ${source.revision} unique count does not match entries`);
    requireValue(entries.filter(entry => entry.classes.includes("SORCERER")).length === source.sorcererSpellCount,
      `${path}: ${source.revision} Sorcerer count does not match entries`);
    requireValue(entries.filter(entry => entry.classes.includes("WIZARD")).length === source.wizardSpellCount,
      `${path}: ${source.revision} Wizard count does not match entries`);
    requireValue(/^[a-f0-9]{64}$/.test(source.sha256 ?? ""),
      `${path}: ${source.revision} lacks a pinned source hash`);
    const audited = {
      "2014-srd-5.1": { unique: 211, sorcerer: 120, wizard: 204 },
      "2024-srd-5.2.1": { unique: 225, sorcerer: 138, wizard: 217 },
    }[source.revision];
    requireValue(source.uniqueSpellCount === audited?.unique && source.sorcererSpellCount === audited?.sorcerer &&
      source.wizardSpellCount === audited?.wizard,
      `${path}: ${source.revision} coverage differs from the audited class-list totals`);
  }
  const oldMind = revisionEntries.get("2014-srd-5.1:spell.feeblemind");
  const newMind = revisionEntries.get("2024-srd-5.2.1:spell.feeblemind");
  requireValue(oldMind?.en?.name === "Feeblemind" && newMind?.en?.name === "Befuddlement" &&
    newMind?.aliases?.includes("feeblemind"), `${path}: cross-edition Feeblemind alias continuity is missing`);
}

for (const entry of readdirSync(packsRoot, { withFileTypes: true })) {
  if (entry.isDirectory()) validatePack(join(packsRoot, entry.name));
}

for (const entry of readdirSync(reportsRoot, { withFileTypes: true })) {
  if (!entry.isFile() || !entry.name.endsWith(".json")) continue;
  const path = join(reportsRoot, entry.name);
  const report = readJson(path);
  if (!report) continue;
  const counts = { included: 0, excluded: 0, "pending-review": 0 };
  for (const record of report.records ?? []) counts[record.decision] = (counts[record.decision] ?? 0) + 1;
  requireValue(report.summary?.seen === (report.records ?? []).length, `${path}: seen count does not match records`);
  requireValue(report.summary?.included === counts.included, `${path}: included count does not match records`);
  requireValue(report.summary?.excluded === counts.excluded, `${path}: excluded count does not match records`);
  requireValue(report.summary?.pendingReview === counts["pending-review"], `${path}: pendingReview count does not match records`);
}

for (const entry of readdirSync(schemasRoot, { withFileTypes: true })) {
  if (entry.isFile() && entry.name.endsWith(".json")) readJson(join(schemasRoot, entry.name));
}

validateSrdSpellCatalog();

if (failures.length > 0) {
  console.error(`Content validation failed with ${failures.length} issue(s):`);
  for (const failure of failures) console.error(`- ${failure}`);
  process.exitCode = 1;
} else {
  console.log("Content validation passed.");
}
