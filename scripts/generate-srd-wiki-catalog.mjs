#!/usr/bin/env node

/**
 * Generates the offline, player-facing SRD Wiki catalog. Creature stat blocks
 * are used only to identify a creature's name, size, and broad type; numerical
 * combat statistics and rules text are deliberately not emitted.
 *
 * Usage:
 *   node scripts/generate-srd-wiki-catalog.mjs \
 *     --srd51 path/to/SRD_CC_v5.1.pdf \
 *     --srd51-de path/to/SRD_CC_v5.1_DE.pdf \
 *     --srd521 path/to/SRD_CC_v5.2.1.pdf \
 *     --srd521-de path/to/DE_SRD_CC_v5.2.1.pdf
 *
 * Requires Poppler's `pdftohtml` on PATH.
 */

import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const args = new Map();
for (let index = 2; index < process.argv.length; index += 2) args.set(process.argv[index], process.argv[index + 1]);

const requiredArgs = ["--srd51", "--srd51-de", "--srd521", "--srd521-de"];
for (const name of requiredArgs) {
  if (!args.get(name)) throw new Error(`${name} is required.`);
}

const sourceDefinitions = [
  {
    revision: "2014-srd-5.1",
    kotlinRevision: "SRD_5_1",
    packId: "srd-5.1-wiki",
    work: "System Reference Document 5.1",
    shortWork: "SRD 5.1",
    url: "https://media.dndbeyond.com/compendium-images/srd/5.1/SRD_CC_v5.1.pdf",
    sha256: "2504d2a0abb0a4d491a939be4f17910a2dde0312570ab8d208080225ccf0a1f0",
    pdf: resolve(args.get("--srd51")),
    deWork: "System-Referenzdokument 5.1",
    deUrl: "https://media.dndbeyond.com/compendium-images/srd/5.1/SRD_CC_v5.1_DE.pdf",
    deSha256: "36d877d00554f9bbb90289b50db6da89498e812606a9a1bda4769f45d1df1b3a",
    dePdf: resolve(args.get("--srd51-de")),
    monsterPages: [253, 403],
    expectedCreatureCount: 317,
  },
  {
    revision: "2024-srd-5.2.1",
    kotlinRevision: "SRD_5_2_1",
    packId: "srd-5.2.1-wiki",
    work: "System Reference Document 5.2.1",
    shortWork: "SRD 5.2.1",
    url: "https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf",
    sha256: "8974902d109d6e63672d7c490bde9ccf052410503d9cfa768237154fbc5e3d87",
    pdf: resolve(args.get("--srd521")),
    deWork: "System-Referenzdokument 5.2.1",
    deUrl: "https://media.dndbeyond.com/compendium-images/srd/5.2/DE_SRD_CC_v5.2.1.pdf",
    deSha256: "f06989dcdf98c8e93b52ba5ea9db52d5e1b054d6d87a148ff295138bafe8a65b",
    dePdf: resolve(args.get("--srd521-de")),
    monsterPages: [254, 364],
    expectedCreatureCount: 330,
  },
];

const sizes = ["Tiny", "Small", "Medium", "Large", "Huge", "Gargantuan"];
const types = ["Aberration", "Beast", "Celestial", "Construct", "Dragon", "Elemental", "Fey", "Fiend", "Giant", "Humanoid", "Monstrosity", "Ooze", "Plant", "Undead"];
const sizeDe = { Tiny: "Winzig", Small: "Klein", Medium: "Mittelgroß", Large: "Groß", Huge: "Riesig", Gargantuan: "Gigantisch" };
const typeDe = {
  Aberration: "Aberration", Beast: "Tier", Celestial: "Celestisches Wesen", Construct: "Konstrukt",
  Dragon: "Drache", Elemental: "Elementarwesen", Fey: "Fee", Fiend: "Unhold", Giant: "Riese",
  Humanoid: "Humanoider", Monstrosity: "Monstrosität", Ooze: "Schlick", Plant: "Pflanze", Undead: "Untoter",
};
const creatureNamesDe = {
  "Clay Golem": "Lehmgolem",
  "Flesh Golem": "Fleischgolem",
  "Iron Golem": "Eisengolem",
  "Stone Golem": "Steingolem",
  Owlbear: "Eulenbär",
  Skeleton: "Skelett",
  Zombie: "Zombie",
};

const tempRoot = mkdtempSync(join(tmpdir(), "dullies-srd-wiki-"));

try {
  for (const source of sourceDefinitions) {
    verifyHash(source.pdf, source.sha256, source.work);
    verifyHash(source.dePdf, source.deSha256, source.deWork);
    const xml = extractMonsterXml(source);
    const creatures = parseCreatures(xml, source);
    const entries = [...staticEntries(source), ...creatures].sort(entryComparator);
    validateEntries(source, entries, creatures);
    writePack(source, entries);
    source.entries = entries;
    console.log(`${source.revision}: ${entries.length} Wiki entries, including ${creatures.length} creature overviews`);
  }
  writeCatalog(sourceDefinitions);
  writeKotlin(sourceDefinitions);
} finally {
  rmSync(tempRoot, { recursive: true, force: true });
}

function verifyHash(path, expected, work) {
  const actual = createHash("sha256").update(readFileSync(path)).digest("hex");
  if (actual !== expected) throw new Error(`${work}: SHA-256 ${actual} does not match pinned ${expected}`);
}

function extractMonsterXml(source) {
  const output = join(tempRoot, `${source.packId}.xml`);
  execFileSync("pdftohtml", [
    "-xml", "-hidden", "-i", "-f", String(source.monsterPages[0]), "-l", String(source.monsterPages[1]), source.pdf, output,
  ], { stdio: "ignore" });
  return readFileSync(output, "utf8");
}

function parseCreatures(xml, source) {
  const entries = new Map();
  const unmatchedTypeLines = [];
  const fonts = new Map([...xml.matchAll(/<fontspec\s+id="(\d+)"\s+size="(\d+)"/g)].map(match => [match[1], Number(match[2])]));
  const typePattern = new RegExp(
    `^(${sizes.join("|")})(?:\\s+or\\s+(?:${sizes.join("|")}))?\\s+(?:swarm\\s+of\\s+(?:${sizes.join("|")})\\s+)?(${types.map(value => `${value}s?`).join("|")})\\b`,
    "i",
  );
  for (const pageMatch of xml.matchAll(/<page\b[\s\S]*?<\/page>/g)) {
    const page = pageMatch[0];
    const pageNumber = /<page\s+number="(\d+)"/.exec(page)?.[1] ?? "?";
    const nodes = [...page.matchAll(/<text\b([^>]*)>([\s\S]*?)<\/text>/g)].map(match => ({
      fontSize: fonts.get(/\bfont="(\d+)"/.exec(match[1])?.[1]) ?? 0,
      bold: /<b>/.test(match[2]),
      text: cleanXmlText(match[2]),
    }));
    for (let index = 0; index < nodes.length; index += 1) {
      const match = typePattern.exec(nodes[index].text);
      if (!match) continue;
      const heading = nodes.slice(Math.max(0, index - 8), index).reverse().find(node =>
        node.bold && (source.kotlinRevision === "SRD_5_1" ? node.fontSize === 18 : node.fontSize === 23) && isCreatureHeading(node.text)
      );
      if (!heading) {
        if (nodes[index].text.includes("into a Large giant")) continue;
        unmatchedTypeLines.push(`page ${pageNumber}: ${nodes[index].text}`);
        continue;
      }
      const name = normalizeText(heading.text);
      const size = titleCase(match[1]);
      const type = titleCase(match[2].replace(/s$/i, ""));
      const id = `creature.${slugify(name)}`;
      if (entries.has(id)) continue;
      const deName = creatureNamesDe[name] ?? name;
      const enSummary = `A ${size} ${type} in the ${source.shortWork} creature collection.`;
      const deSummary = `Ein ${sizeDe[size]}es Wesen des Typs ${typeDe[type]} in der Kreaturensammlung des ${source.shortWork}.`;
      entries.set(id, wikiEntry({
        id,
        kind: "creature",
        enName: name,
        deName,
        enSummary,
        deSummary,
        enTip: "This player overview identifies the creature without revealing its stat block.",
        deTip: "Diese Spielerübersicht beschreibt die Kreatur, ohne ihren Werteblock offenzulegen.",
        aliases: [name.toLowerCase(), deName.toLowerCase()],
        keywords: ["creature", "enemy", "monster", "kreatur", "gegner", "monster", type.toLowerCase(), typeDe[type].toLowerCase()],
        locator: `Monsters A-Z: ${name} (PDF page ${pageNumber})`,
        metadata: `${size}|${type}`,
        creature: { stableId: id, revision: source.revision, size: size.toLowerCase(), type: type.toLowerCase() },
      }));
    }
  }
  if (unmatchedTypeLines.length > 0) {
    throw new Error(`${source.revision}: creature type lines without headings:\n${unmatchedTypeLines.slice(0, 10).join("\n")}`);
  }
  return [...entries.values()];
}

function isCreatureHeading(value) {
  return value.length >= 2 && value.length <= 80 && !/^(Actions|Bonus Actions|Reactions|Legendary Actions|Traits|Monsters)$/i.test(value);
}

function staticEntries(source) {
  const entries = [];
  entries.push(...schoolEntries());

  const is2014 = source.kotlinRevision === "SRD_5_1";
  const ancestries = is2014
    ? [["Dwarf", "Zwerg"], ["Elf", "Elf"], ["Halfling", "Halbling"], ["Human", "Mensch"], ["Dragonborn", "Drachenblütiger"], ["Gnome", "Gnom"], ["Half-Elf", "Halbelf"], ["Half-Orc", "Halbork"], ["Tiefling", "Tiefling"]]
    : [["Dragonborn", "Drachenblütiger"], ["Dwarf", "Zwerg"], ["Elf", "Elf"], ["Gnome", "Gnom"], ["Goliath", "Goliath"], ["Halfling", "Halbling"], ["Human", "Mensch"], ["Orc", "Ork"], ["Tiefling", "Tiefling"]];
  for (const [enName, deName] of ancestries) {
    entries.push(wikiEntry({
      id: `ancestry.${slugify(enName)}`, kind: "ancestry", enName, deName,
      enSummary: `${enName} is a playable ${is2014 ? "race" : "species"} in ${source.shortWork}.`,
      deSummary: `${deName} ist eine spielbare ${is2014 ? "Volkszugehörigkeit" : "Spezies"} im ${source.shortWork}.`,
      enTip: "Use the entry as a quick identity reference; character creation applies the actual traits.",
      deTip: "Nutze den Eintrag zur schnellen Einordnung; die Charaktererschaffung wendet die eigentlichen Merkmale an.",
      aliases: [enName.toLowerCase(), deName.toLowerCase()], keywords: ["ancestry", "race", "species", "volk", "spezies"],
      locator: is2014 ? "Races" : "Character Origins: Species",
    }));
  }

  const classes = ["Barbarian", "Bard", "Cleric", "Druid", "Fighter", "Monk", "Paladin", "Ranger", "Rogue", "Sorcerer", "Warlock", "Wizard"];
  const classDe = {
    Barbarian: "Barbar", Bard: "Barde", Cleric: "Kleriker", Druid: "Druide", Fighter: "Kämpfer", Monk: "Mönch",
    Paladin: "Paladin", Ranger: "Waldläufer", Rogue: "Schurke", Sorcerer: "Zauberer", Warlock: "Hexenmeister", Wizard: "Magier",
  };
  for (const className of classes) {
    entries.push(wikiEntry({
      id: `class.${slugify(className)}`, kind: "class", enName: className, deName: classDe[className],
      enSummary: `${className} is a player class with its own progression, resources, and features.`,
      deSummary: `${classDe[className]} ist eine Spielerklasse mit eigener Entwicklung, Ressourcen und Merkmalen.`,
      enTip: "Open character creation or level-up to apply class choices.",
      deTip: "Wende Klassenentscheidungen in der Charaktererschaffung oder beim Stufenaufstieg an.",
      aliases: [className.toLowerCase(), classDe[className].toLowerCase()], keywords: ["class", "klasse"], locator: `Classes: ${className}`,
    }));
  }

  const subclasses = is2014 ? [
    ["Path of the Berserker", "Pfad des Berserkers", "Barbarian"], ["College of Lore", "Kolleg des Wissens", "Bard"],
    ["Life Domain", "Domäne des Lebens", "Cleric"], ["Circle of the Land", "Zirkel des Landes", "Druid"],
    ["Champion", "Champion", "Fighter"], ["Way of the Open Hand", "Weg der offenen Hand", "Monk"],
    ["Oath of Devotion", "Schwur der Hingabe", "Paladin"], ["Hunter", "Jäger", "Ranger"], ["Thief", "Dieb", "Rogue"],
    ["Draconic Bloodline", "Drachenblutlinie", "Sorcerer"], ["The Fiend", "Der Unhold", "Warlock"], ["School of Evocation", "Schule der Hervorrufung", "Wizard"],
  ] : [
    ["Path of the Berserker", "Pfad des Berserkers", "Barbarian"], ["College of Lore", "Kolleg des Wissens", "Bard"],
    ["Life Domain", "Domäne des Lebens", "Cleric"], ["Circle of the Land", "Zirkel des Landes", "Druid"],
    ["Champion", "Champion", "Fighter"], ["Warrior of the Open Hand", "Krieger der offenen Hand", "Monk"],
    ["Oath of Devotion", "Schwur der Hingabe", "Paladin"], ["Hunter", "Jäger", "Ranger"], ["Thief", "Dieb", "Rogue"],
    ["Draconic Sorcery", "Drachenzauberei", "Sorcerer"], ["Fiend Patron", "Unholdpatron", "Warlock"], ["Evoker", "Hervorrufer", "Wizard"],
  ];
  for (const [enName, deName, parentClass] of subclasses) {
    const selectionLevel = is2014 && ["Cleric", "Sorcerer", "Warlock"].includes(parentClass)
      ? 1
      : is2014 && ["Druid", "Wizard"].includes(parentClass) ? 2 : 3;
    entries.push(wikiEntry({
      id: `subclass.${slugify(parentClass)}.${slugify(enName)}`, kind: "subclass", enName, deName,
      enSummary: `${enName} is the ${source.shortWork} subclass for the ${parentClass}.`,
      deSummary: `${deName} ist die Unterklasse des ${source.shortWork} für ${classDe[parentClass]}.`,
      enTip: "The character sheet reveals subclass features at the appropriate levels.",
      deTip: "Der Charakterbogen zeigt Unterklassenmerkmale auf den passenden Stufen.",
      aliases: [enName.toLowerCase(), deName.toLowerCase()], keywords: ["subclass", "unterklasse", parentClass.toLowerCase(), classDe[parentClass].toLowerCase()],
      locator: `Classes: ${parentClass} Subclass`, metadata: parentClass,
      subclass: {
        stableId: `subclass.${slugify(parentClass)}.${slugify(enName)}`,
        revision: source.revision,
        parentClassId: `class.${slugify(parentClass)}`,
        selectionLevel,
        featureIdsByLevel: {},
      },
    }));
  }

  const backgrounds = is2014
    ? [["Acolyte", "Akolyth"]]
    : [["Acolyte", "Akolyth"], ["Criminal", "Krimineller"], ["Sage", "Weiser"], ["Soldier", "Soldat"]];
  for (const [enName, deName] of backgrounds) {
    entries.push(wikiEntry({
      id: `background.${slugify(enName)}`, kind: "background", enName, deName,
      enSummary: `${enName} is a character background in ${source.shortWork}.`, deSummary: `${deName} ist ein Hintergrund im ${source.shortWork}.`,
      enTip: "Choose a background during character creation.", deTip: "Wähle einen Hintergrund bei der Charaktererschaffung.",
      aliases: [enName.toLowerCase(), deName.toLowerCase()], keywords: ["background", "hintergrund"], locator: `Character Origins: ${enName}`,
    }));
  }

  const feats = is2014
    ? [["Grappler", "Ringer"]]
    : [
      ["Alert", "Wachsam"], ["Magic Initiate", "Magieinitiierter"], ["Savage Attacker", "Wilder Angreifer"], ["Skilled", "Talentiert"],
      ["Ability Score Improvement", "Attributswerterhöhung"], ["Grappler", "Ringer"],
      ["Archery", "Bogenschießen"], ["Defense", "Verteidigung"], ["Great Weapon Fighting", "Kampf mit großen Waffen"], ["Two-Weapon Fighting", "Kampf mit zwei Waffen"],
      ["Boon of Combat Prowess", "Segen des Kampfgeschicks"], ["Boon of Dimensional Travel", "Segen der Dimensionsreise"],
      ["Boon of Fate", "Segen des Schicksals"], ["Boon of Irresistible Offense", "Segen des unwiderstehlichen Angriffs"],
      ["Boon of Spell Recall", "Segen der Zaubererinnerung"], ["Boon of the Night Spirit", "Segen des Nachtgeists"], ["Boon of Truesight", "Segen der Wahren Sicht"],
    ];
  for (const [enName, deName] of feats) {
    entries.push(wikiEntry({
      id: `feat.${slugify(enName)}`, kind: "feat", enName, deName,
      enSummary: `${enName} is a feat available in ${source.shortWork}.`, deSummary: `${deName} ist ein Talent aus dem ${source.shortWork}.`,
      enTip: "Check the character's level and feature choices before adding a feat.",
      deTip: "Prüfe Stufe und Merkmalsauswahl des Charakters, bevor du ein Talent hinzufügst.",
      aliases: [enName.toLowerCase(), deName.toLowerCase()], keywords: ["feat", "talent"], locator: `Feats: ${enName}`,
    }));
  }
  entries.push(...conditionEntries(source));
  entries.push(...actionEntries(source));
  return entries;
}

function conditionEntries(source) {
  const values = [
    ["Blinded", "Geblendet", "You cannot see; sight-based checks fail, your attacks have Disadvantage, and attacks against you have Advantage.", "Du kannst nicht sehen; sichtbasierte Würfe scheitern, deine Angriffe haben Nachteil und Angriffe gegen dich Vorteil."],
    ["Charmed", "Bezaubert", "You cannot harm the charmer, who also gains an edge in social interaction with you.", "Du kannst dem Bezaubernden nicht schaden; er ist außerdem bei sozialen Interaktionen mit dir im Vorteil."],
    ["Deafened", "Taub", "You cannot hear and fail checks that require hearing.", "Du kannst nicht hören und scheiterst an Würfen, die Gehör erfordern."],
    ["Exhaustion", "Erschöpfung", "Exhaustion applies cumulative penalties defined by this SRD revision.", "Erschöpfung verursacht kumulative Nachteile nach dieser SRD-Fassung."],
    ["Frightened", "Verängstigt", "While the source is visible, relevant tests are hindered and you cannot willingly move closer to it.", "Solange die Quelle sichtbar ist, sind passende Würfe erschwert und du kannst dich ihr nicht freiwillig nähern."],
    ["Grappled", "Gepackt", "Your Speed becomes 0 until the grapple ends.", "Deine Bewegungsrate wird 0, bis der Griff endet."],
    ["Incapacitated", "Handlungsunfähig", "You cannot take actions, Bonus Actions, or Reactions.", "Du kannst keine Aktionen, Bonusaktionen oder Reaktionen ausführen."],
    ["Invisible", "Unsichtbar", "You cannot be seen without special senses, affecting attacks when your position is known.", "Ohne besondere Sinne kannst du nicht gesehen werden, was Angriffe bei bekannter Position beeinflusst."],
    ["Paralyzed", "Gelähmt", "You are Incapacitated and cannot move or speak; nearby hits can become critical hits.", "Du bist handlungsunfähig und kannst dich weder bewegen noch sprechen; nahe Treffer können kritisch werden."],
    ["Petrified", "Versteinert", "You are transformed and Incapacitated, with no movement or speech and strong resistance to harm.", "Du bist verwandelt und handlungsunfähig, kannst dich weder bewegen noch sprechen und widerstehst Schaden stark."],
    ["Poisoned", "Vergiftet", "You have Disadvantage on attack rolls and ability checks.", "Du hast Nachteil bei Angriffswürfen und Attributswürfen."],
    ["Prone", "Liegend", "Your movement and attacks are hindered until you stand, while nearby attacks against you are helped.", "Bewegung und Angriffe sind erschwert, bis du aufstehst; nahe Angriffe gegen dich sind erleichtert."],
    ["Restrained", "Festgesetzt", "Your Speed is 0; your attacks and Dexterity saves are hindered, while attacks against you are helped.", "Deine Bewegungsrate ist 0; Angriffe und Geschicklichkeitsrettungswürfe sind erschwert, Angriffe gegen dich erleichtert."],
    ["Stunned", "Betäubt", "You are Incapacitated, cannot move, and attacks against you have Advantage.", "Du bist handlungsunfähig, kannst dich nicht bewegen und Angriffe gegen dich haben Vorteil."],
    ["Unconscious", "Bewusstlos", "You are Incapacitated, unaware, unable to move or speak, drop held items, and fall Prone.", "Du bist handlungsunfähig und ohne Wahrnehmung, kannst dich weder bewegen noch sprechen, lässt Gehaltenes fallen und liegst."],
  ];
  return values.map(([enName, deName, enSummary, deSummary]) => wikiEntry({
    id: `condition.${slugify(enName)}`, kind: "condition", enName, deName, enSummary, deSummary,
    enTip: "Apply and remove conditions from the character sheet; the selected ruleset controls their exact effects.",
    deTip: "Wende Zustände auf dem Charakterbogen an oder entferne sie; das gewählte Regelwerk bestimmt die genaue Wirkung.",
    aliases: [enName.toLowerCase(), deName.toLowerCase()], keywords: ["condition", "status", "zustand"], locator: `Conditions: ${enName}`,
  }));
}

function actionEntries(source) {
  const is2014 = source.kotlinRevision === "SRD_5_1";
  const actions = is2014
    ? [["Attack", "Angreifen"], ["Cast a Spell", "Zauber wirken"], ["Dash", "Sprinten"], ["Disengage", "Lösen"], ["Dodge", "Ausweichen"], ["Help", "Helfen"], ["Hide", "Verstecken"], ["Ready", "Bereithalten"], ["Search", "Suchen"], ["Use an Object", "Gegenstand verwenden"]]
    : [["Attack", "Angreifen"], ["Dash", "Sprinten"], ["Disengage", "Lösen"], ["Dodge", "Ausweichen"], ["Help", "Helfen"], ["Hide", "Verstecken"], ["Influence", "Beeinflussen"], ["Magic", "Magie"], ["Ready", "Bereithalten"], ["Search", "Suchen"], ["Study", "Studieren"], ["Utilize", "Benutzen"]];
  return actions.map(([enName, deName]) => wikiEntry({
    id: `action.${slugify(enName)}`, kind: "action", enName, deName,
    enSummary: `${enName} is an action option defined by ${source.shortWork}.`, deSummary: `${deName} ist eine Aktionsoption des ${source.shortWork}.`,
    enTip: "The Turn Guide can help track the action, but using the guide is optional.",
    deTip: "Der Zugführer kann die Aktion nachhalten, seine Nutzung ist jedoch optional.",
    aliases: [enName.toLowerCase(), deName.toLowerCase()], keywords: ["action", "turn", "aktion", "zug"], locator: `Actions: ${enName}`,
  }));
}

function schoolEntries() {
  const values = [
    ["Abjuration", "Bannmagie", "Protective magic that wards, counters, or banishes.", "Schutzmagie, die abwehrt, aufhebt oder verbannt."],
    ["Conjuration", "Beschwörung", "Magic that summons, creates, or transports creatures and objects.", "Magie, die Kreaturen und Gegenstände beschwört, erschafft oder transportiert."],
    ["Divination", "Erkenntnismagie", "Magic that reveals information, possibilities, or hidden things.", "Magie, die Informationen, Möglichkeiten oder Verborgenes offenbart."],
    ["Enchantment", "Verzauberung", "Magic that influences thoughts, emotions, or behavior.", "Magie, die Gedanken, Gefühle oder Verhalten beeinflusst."],
    ["Evocation", "Hervorrufung", "Magic that channels energy into direct effects such as damage or healing.", "Magie, die Energie in direkte Effekte wie Schaden oder Heilung lenkt."],
    ["Illusion", "Illusion", "Magic that deceives the senses or disguises what is present.", "Magie, die Sinne täuscht oder Vorhandenes verschleiert."],
    ["Necromancy", "Nekromantie", "Magic concerned with life force, death, and undeath.", "Magie rund um Lebenskraft, Tod und Untod."],
    ["Transmutation", "Verwandlung", "Magic that changes a creature, object, or environment.", "Magie, die Kreaturen, Gegenstände oder die Umgebung verändert."],
  ];
  return values.map(([enName, deName, enSummary, deSummary]) => wikiEntry({
    id: `school.${slugify(enName)}`, kind: "knowledge", enName, deName, enSummary, deSummary,
    enTip: "A spell's school is a category, not an extra casting requirement.",
    deTip: "Die Schule eines Zaubers ist eine Kategorie, keine zusätzliche Voraussetzung zum Wirken.",
    aliases: [enName.toLowerCase(), deName.toLowerCase(), `school of ${enName.toLowerCase()}`],
    keywords: ["magic school", "school", "magieschule", "zauberschule"], locator: "Spellcasting: Schools of Magic",
  }));
}

function wikiEntry(value) {
  return {
    id: value.id,
    kind: value.kind,
    aliases: unique(value.aliases ?? []),
    keywords: unique(value.keywords ?? []),
    en: { name: value.enName, summary: value.enSummary, beginnerTip: value.enTip },
    de: { name: value.deName, summary: value.deSummary, beginnerTip: value.deTip },
    locator: value.locator,
    metadata: value.metadata ?? "",
    creature: value.creature ?? null,
    subclass: value.subclass ?? null,
  };
}

function validateEntries(source, entries, creatures) {
  if (creatures.length !== source.expectedCreatureCount) {
    throw new Error(`${source.revision}: ${creatures.length} creature headings differ from audited ${source.expectedCreatureCount}`);
  }
  const ids = entries.map(entry => entry.id);
  if (new Set(ids).size !== ids.length) throw new Error(`${source.revision}: duplicate Wiki entry ID`);
  for (const entry of entries) {
    if (!entry.en.name || !entry.de.name || !entry.en.summary || !entry.de.summary || !entry.en.beginnerTip || !entry.de.beginnerTip) {
      throw new Error(`${source.revision}:${entry.id}: incomplete bilingual text`);
    }
    if (entry.kind === "creature" && !entry.creature) throw new Error(`${source.revision}:${entry.id}: creature metadata is missing`);
  }
  for (const name of source.kotlinRevision === "SRD_5_1" ? ["Clay Golem", "Flesh Golem", "Iron Golem", "Stone Golem"] : ["Flesh Golem", "Iron Golem", "Stone Golem"]) {
    if (!entries.some(entry => entry.en.name === name)) throw new Error(`${source.revision}: ${name} was not found`);
  }
}

function writePack(source, entries) {
  const packRoot = join(repoRoot, "content", "packs", source.packId);
  mkdirSync(packRoot, { recursive: true });
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
      sourceWorks: [
        { title: source.work, url: source.url, license: "CC-BY-4.0", snapshot: source.revision, sha256: source.sha256 },
        { title: source.deWork, url: source.deUrl, license: "CC-BY-4.0", snapshot: source.revision, sha256: source.deSha256 },
      ],
    },
    contentStatus: "audited-snapshot",
    distributionReady: true,
    coverageReport: `../../reports/${source.packId}.coverage.json`,
    notes: "Player-facing index of SRD classes, subclasses, ancestries/species, backgrounds, feats, schools, and creatures. Creature stat blocks are deliberately excluded.",
  });
  for (const locale of ["en", "de"]) {
    writeJson(join(packRoot, `entries.${locale}.json`), {
      $schema: "../../schema/localized-entries.schema.json",
      schemaVersion: 1,
      packId: source.packId,
      locale,
      entries: entries.map(entry => packEntry(source, entry, locale)),
    });
  }
  writeJson(join(repoRoot, "content", "reports", `${source.packId}.coverage.json`), coverageReport(source, entries));
}

function packEntry(source, entry, locale) {
  const text = entry[locale];
  const result = {
    id: `${source.packId.replace("-wiki", "")}.${entry.id}`,
    kind: entry.kind,
    name: text.name,
    summary: text.summary,
    beginnerTip: text.beginnerTip,
    keywords: unique([text.name.toLocaleLowerCase(locale), ...entry.aliases, ...entry.keywords]),
    automation: { level: "informational", command: null, actionCost: null, requiresTableInput: false },
    source: {
      work: source.shortWork,
      locator: entry.locator,
      adaptation: locale === "de"
        ? "Originale kurze Spielerzusammenfassung; die zitierte SRD-Fassung bleibt maßgeblich. Kreaturenwerte sind ausgeschlossen."
        : "Original compact player summary; the cited SRD revision remains authoritative. Creature statistics are excluded.",
    },
  };
  if (entry.creature) result.creature = entry.creature;
  if (entry.subclass) result.subclass = entry.subclass;
  return result;
}

function coverageReport(source, entries) {
  return {
    $schema: "../schema/coverage-report.schema.json",
    schemaVersion: 1,
    packId: source.packId,
    generatedAt: "2026-09-02T00:00:00.000Z",
    sourceSnapshot: { name: source.work, url: source.url, revision: source.revision, retrievedAt: "2026-09-02T00:00:00.000Z", sha256: source.sha256 },
    summary: { seen: entries.length, included: entries.length, excluded: 0, pendingReview: 0 },
    records: entries.map(entry => ({
      sourceId: entry.id,
      category: entry.kind,
      decision: "included",
      reason: entry.kind === "creature" ? "Included as a player-facing identity overview; stat-block data is excluded." : "Included in the compact offline SRD index.",
      licenseEvidence: `${source.work}, CC-BY-4.0, pinned SHA-256 ${source.sha256}`,
      outputId: `${source.packId.replace("-wiki", "")}.${entry.id}`,
    })),
  };
}

function writeCatalog(sources) {
  const path = join(repoRoot, "content", "catalogs", "srd-wiki.json");
  writeJson(path, {
    schemaVersion: 1,
    generatedBy: "scripts/generate-srd-wiki-catalog.mjs",
    sources: sources.map(source => ({ revision: source.revision, work: source.work, url: source.url, sha256: source.sha256, entryCount: source.entries.length })),
    entries: sources.flatMap(source => source.entries.map(entry => ({ revision: source.revision, ...entry }))),
  });
}

function writeKotlin(sources) {
  const entries = sources.flatMap(source => source.entries.map(entry => ({ source, entry })));
  const chunks = Array.from({ length: Math.ceil(entries.length / 40) }, (_, index) => entries.slice(index * 40, index * 40 + 40));
  const lines = [
    "package app.dulliesanddungeons.rules",
    "",
    "/** Generated by scripts/generate-srd-wiki-catalog.mjs. Do not hand-edit. */",
    "internal enum class SrdWikiRevision { SRD_5_1, SRD_5_2_1 }",
    "internal enum class SrdWikiKind { ACTION, ANCESTRY, BACKGROUND, CLASS, CONDITION, CREATURE, FEAT, KNOWLEDGE, SUBCLASS }",
    "internal data class SrdWikiText(val name: String, val summary: String, val beginnerTip: String)",
    "internal data class SrdWikiCatalogEntry(",
    "    val id: String,",
    "    val revision: SrdWikiRevision,",
    "    val kind: SrdWikiKind,",
    "    val aliases: Set<String>,",
    "    val keywords: Set<String>,",
    "    val en: SrdWikiText,",
    "    val de: SrdWikiText,",
    "    val source: String,",
    "    val locator: String,",
    "    val metadata: String = \"\",",
    ")",
    "",
    "internal object SrdWikiCatalog {",
    "    val entries: List<SrdWikiCatalogEntry> = listOf(",
  ];
  for (let index = 0; index < chunks.length; index += 1) lines.push(`        entries${index}(),`);
  lines.push("    ).flatten()");
  lines.push("");
  chunks.forEach((chunk, index) => {
    lines.push(`    private fun entries${index}(): List<SrdWikiCatalogEntry> = listOf(`);
    for (const { source, entry } of chunk) {
      lines.push(`        SrdWikiCatalogEntry(${kotlinString(entry.id)}, SrdWikiRevision.${source.kotlinRevision}, SrdWikiKind.${entry.kind.toUpperCase()}, setOf(${entry.aliases.map(kotlinString).join(", ")}), setOf(${entry.keywords.map(kotlinString).join(", ")}), SrdWikiText(${kotlinString(entry.en.name)}, ${kotlinString(entry.en.summary)}, ${kotlinString(entry.en.beginnerTip)}), SrdWikiText(${kotlinString(entry.de.name)}, ${kotlinString(entry.de.summary)}, ${kotlinString(entry.de.beginnerTip)}), ${kotlinString(source.shortWork)}, ${kotlinString(entry.locator)}, ${kotlinString(entry.metadata)}),`);
    }
    lines.push("    )");
    lines.push("");
  });
  lines.push("");
  lines.push("    fun forRevision(revision: SrdWikiRevision): List<SrdWikiCatalogEntry> = entries.filter { it.revision == revision }");
  lines.push("}");
  lines.push("");
  const path = join(repoRoot, "shared", "src", "commonMain", "kotlin", "app", "dulliesanddungeons", "rules", "SrdWikiCatalog.kt");
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, lines.join("\n"), "utf8");
}

function cleanXmlText(value) {
  return normalizeText(value
    .replace(/<[^>]+>/g, "")
    .replaceAll("&amp;", "&")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", "\"")
    .replaceAll("&apos;", "'")
    .replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_, code) => String.fromCodePoint(Number.parseInt(code, 16))));
}

function normalizeText(value) {
  return value.replace(/Â\s*/g, " ").replace(/[‘’]/g, "'").replace(/\s+/g, " ").trim();
}

function slugify(value) {
  return value.normalize("NFKD").replace(/[’']/g, "").replace(/[^A-Za-z0-9]+/g, "-").replace(/^-|-$/g, "").toLowerCase();
}

function titleCase(value) {
  return value[0].toUpperCase() + value.slice(1).toLowerCase();
}

function unique(values) {
  return [...new Set(values.filter(Boolean))].sort((left, right) => left.localeCompare(right, "en", { sensitivity: "base" }));
}

function entryComparator(left, right) {
  return left.kind.localeCompare(right.kind) || left.en.name.localeCompare(right.en.name, "en", { sensitivity: "base" }) || left.id.localeCompare(right.id);
}

function kotlinString(value) {
  return `"${String(value).replaceAll("\\", "\\\\").replaceAll("\"", "\\\"").replaceAll("\n", "\\n")}"`;
}

function writeJson(path, value) {
  mkdirSync(dirname(path), { recursive: true });
  writeFileSync(path, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}
