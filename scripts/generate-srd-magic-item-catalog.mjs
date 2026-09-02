import fs from "node:fs";

// Official CC-BY-4.0 source PDFs used to produce the XML inputs:
// SRD 5.1: https://media.wizards.com/2023/downloads/dnd/SRD_CC_v5.1.pdf
// SRD 5.2.1: https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf

const [srd51Path, srd521Path, outputPath] = process.argv.slice(2);
if (!srd51Path || !srd521Path || !outputPath) {
  throw new Error("Usage: node generate-srd-magic-item-catalog.mjs <srd51.xml> <srd521.xml> <output.kt>");
}

function decode(value) {
  return value
    .replace(/<[^>]+>/g, "")
    .replaceAll("&amp;", "&")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", '"')
    .replaceAll("&apos;", "'")
    .replaceAll("&#39;", "'")
    .replace(/\s+/g, " ")
    .trim();
}

function parse(path, revision) {
  const xml = fs.readFileSync(path, "utf8");
  const entries = [];
  const fonts = new Map();
  for (const match of xml.matchAll(/<fontspec id="(\d+)" size="(\d+)" family="([^"]+)"[^>]*\/>/g)) {
    fonts.set(match[1], { size: Number(match[2]), family: match[3] });
  }
  for (const pageMatch of xml.matchAll(/<page\b[^>]*>([\s\S]*?)<\/page>/g)) {
    const page = pageMatch[1];
    const texts = [...page.matchAll(/<text\b([^>]*)>([\s\S]*?)<\/text>/g)].map((match) => {
      const attrs = match[1];
      const font = /font="(\d+)"/.exec(attrs)?.[1];
      return {
        top: Number(/top="(\d+)"/.exec(attrs)?.[1] ?? 0),
        left: Number(/left="(\d+)"/.exec(attrs)?.[1] ?? 0),
        value: decode(match[2]),
        spec: fonts.get(font),
      };
    });
    for (let index = 0; index < texts.length; index += 1) {
      const text = texts[index];
      if (!text.value || text.spec?.size !== 18 || !text.spec.family.includes("GillSans-SemiBold")) continue;
      const parts = [text.value];
      let cursor = index + 1;
      while (
        cursor < texts.length &&
        texts[cursor].spec?.size === 18 &&
        texts[cursor].spec?.family.includes("GillSans-SemiBold") &&
        Math.abs(texts[cursor].left - text.left) < 20 &&
        texts[cursor].top - texts[cursor - 1].top < 35
      ) {
        parts.push(texts[cursor].value);
        cursor += 1;
      }
      const name = parts.join(" ").replace(/\s+/g, " ").trim();
      if (!name || /^(Magic Items A|Command Word|Consumables|Spells|Charges)$/i.test(name)) continue;
      const metadataParts = [];
      while (cursor < texts.length && texts[cursor].spec?.family.includes("Cambria") && texts[cursor].value) {
        const candidate = texts[cursor];
        if (candidate.spec?.size !== 15) break;
        metadataParts.push(candidate.value);
        cursor += 1;
        if (/attunement[^)]*\)|(?:common|uncommon|rare|legendary|artifact|varies)(?:\s*\([^)]*\))?\s*$/i.test(metadataParts.join(" "))) break;
      }
      const metadata = metadataParts.join(" ").replace(/\s+/g, " ").trim();
      if (!/(armor|weapon|wondrous|potion|ring|rod|scroll|staff|wand|rarity|rare|common|artifact)/i.test(metadata)) continue;
      let nextHeading = cursor;
      while (nextHeading < texts.length) {
        const candidate = texts[nextHeading];
        if (candidate.spec?.size === 18 && candidate.spec?.family.includes("GillSans-SemiBold")) break;
        nextHeading += 1;
      }
      const description = texts.slice(cursor, nextHeading).map((candidate) => candidate.value).join(" ").replace(/\s+/g, " ");
      const savingThrows = [];
      const savePattern = /DC\s*(\d+)\s+(Strength|Dexterity|Constitution|Intelligence|Wisdom|Charisma)\s+saving throw/gi;
      for (const save of description.matchAll(savePattern)) {
        savingThrows.push({ ability: save[2].toUpperCase(), fixed: Number(save[1]), spellcasting: false });
      }
      const spellSavePattern = /(Strength|Dexterity|Constitution|Intelligence|Wisdom|Charisma)\s+saving throw[^.]{0,100}(?:your|the wielder's) spell save DC/gi;
      for (const save of description.matchAll(spellSavePattern)) {
        savingThrows.push({ ability: save[1].toUpperCase(), fixed: null, spellcasting: true });
      }
      entries.push({
        revision,
        name,
        category: metadata,
        rarity: /artifact/i.test(metadata) ? "Artifact" :
          /legendary/i.test(metadata) ? "Legendary" :
          /very rare/i.test(metadata) ? "VeryRare" :
          /\brare\b/i.test(metadata) ? "Rare" :
          /uncommon/i.test(metadata) ? "Uncommon" :
          /common/i.test(metadata) ? "Common" : "Unspecified",
        attunement: /requires attunement/i.test(metadata),
        savingThrows: [...new Map(savingThrows.map((save) => [`${save.ability}:${save.fixed}:${save.spellcasting}`, save])).values()],
      });
      index = cursor - 1;
    }
  }
  const byName = new Map();
  for (const entry of entries) byName.set(entry.name.toLowerCase(), entry);
  return [...byName.values()].sort((a, b) => a.name.localeCompare(b.name));
}

function slug(value) {
  return value.toLowerCase().normalize("NFKD").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

function quote(value) {
  return `"${value.replaceAll("\\", "\\\\").replaceAll('"', '\\"')}"`;
}

const srd51 = parse(srd51Path, "SRD_5_1");
const srd521 = parse(srd521Path, "SRD_5_2_1");
if (srd51.length !== 242 || srd521.length !== 258) {
  throw new Error(`Catalog extraction count changed: SRD 5.1=${srd51.length}, SRD 5.2.1=${srd521.length}`);
}

const lines = [
  "package app.dulliesanddungeons.ui",
  "",
  "/** Generated from the official CC-BY-4.0 SRD 5.1 and SRD 5.2.1 PDFs. */",
  "import app.dulliesanddungeons.domain.Ability",
  "import app.dulliesanddungeons.domain.DifficultyClass",
  "import app.dulliesanddungeons.domain.SavingThrowPrompt",
  "",
  "internal enum class SrdItemRevision { SRD_5_1, SRD_5_2_1 }",
  "",
  "internal data class SrdMagicItemCatalogEntry(",
  "    val id: String,",
  "    val name: String,",
  "    val category: String,",
  "    val rarity: ItemRarity,",
  "    val requiresAttunement: Boolean,",
  "    val savingThrows: List<SavingThrowPrompt> = emptyList(),",
  "    val revision: SrdItemRevision,",
  ")",
  "",
  "internal val srdMagicItemCatalog = listOf(",
];
for (const entry of [...srd51, ...srd521]) {
  const savingThrows = entry.savingThrows.length === 0 ? "emptyList()" : `listOf(${entry.savingThrows.map((save) => {
    const dc = save.fixed == null ? "DifficultyClass(useSpellcasting = true)" : `DifficultyClass(fixed = ${save.fixed})`;
    return `SavingThrowPrompt(Ability.${save.ability}, ${dc})`;
  }).join(", ")})`;
  lines.push(
    `    SrdMagicItemCatalogEntry(${quote(`${entry.revision.toLowerCase()}-${slug(entry.name)}`)}, ${quote(entry.name)}, ${quote(entry.category)}, ItemRarity.${entry.rarity}, ${entry.attunement}, ${savingThrows}, SrdItemRevision.${entry.revision}),`,
  );
}
lines.push(")", "");
fs.writeFileSync(outputPath, `${lines.join("\n")}\n`);
process.stdout.write(`Generated ${srd51.length} SRD 5.1 and ${srd521.length} SRD 5.2.1 magic items.\n`);
