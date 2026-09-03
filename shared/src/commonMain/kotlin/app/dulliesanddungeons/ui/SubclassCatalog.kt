package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.RollMode
import kotlinx.serialization.Serializable
import kotlin.math.floor

@Serializable
enum class SubclassArmorFormulaUi { NONE, DEXTERITY_13, DEXTERITY_AND_CHARISMA_10 }

@Serializable
data class SubclassStatRulesUi(
    val minimumClassLevel: Int = 1,
    val armorClassBonus: Int = 0,
    val armorFormula: SubclassArmorFormulaUi = SubclassArmorFormulaUi.NONE,
    val hitPointsPerClassLevel: Int = 0,
    val initiativeBonus: Int = 0,
    val initiativeHalfProficiencyBonus: Boolean = false,
    val speedBonusFeet: Int = 0,
    val savingThrowBonus: Int = 0,
    val attackBonus: Int = 0,
    val criticalThreshold: Int? = null,
    val initiativeRollMode: RollMode = RollMode.NORMAL,
)

@Serializable
enum class SubclassUseScalingUi { NONE, FIXED, CLASS_LEVEL, PROFICIENCY_BONUS, WISDOM_MODIFIER }

@Serializable
data class SubclassFeatureGrantUi(
    val minimumClassLevel: Int,
    val feature: FeatureUi,
    val useScaling: SubclassUseScalingUi = SubclassUseScalingUi.NONE,
    val fixedUses: Int = 0,
)

@Serializable
data class SubclassSpellGrantUi(
    val minimumClassLevel: Int,
    val spell: SpellUi,
)

@Serializable
data class SubclassMechanicsUi(
    val parentClassName: String,
    val ruleset: Ruleset,
    val selectionLevel: Int,
    val statRules: List<SubclassStatRulesUi> = emptyList(),
    val features: List<SubclassFeatureGrantUi> = emptyList(),
    val spells: List<SubclassSpellGrantUi> = emptyList(),
)

data class SubclassOptionUi(
    val id: String,
    val name: String,
    val summaryEnglish: String,
    val summaryGerman: String,
    val mechanics: SubclassMechanicsUi,
    val local: Boolean = false,
    val sourceNote: String = "SRD",
) {
    fun summary(language: UiLanguage): String =
        if (language == UiLanguage.German) summaryGerman else summaryEnglish
}

data class ResolvedSubclassStatsUi(
    val armorClassBonus: Int = 0,
    val armorFormula: SubclassArmorFormulaUi = SubclassArmorFormulaUi.NONE,
    val hitPointBonus: Int = 0,
    val initiativeBonus: Int = 0,
    val speedBonusFeet: Int = 0,
    val savingThrowBonus: Int = 0,
    val attackBonus: Int = 0,
    val criticalThreshold: Int = 20,
    val initiativeRollMode: RollMode = RollMode.NORMAL,
)

internal fun SubclassOptionUi.resolveStats(classLevel: Int, proficiencyBonus: Int = 0): ResolvedSubclassStatsUi {
    val active = mechanics.statRules.filter { classLevel >= it.minimumClassLevel }
    return ResolvedSubclassStatsUi(
        armorClassBonus = active.sumOf(SubclassStatRulesUi::armorClassBonus),
        armorFormula = active.lastOrNull { it.armorFormula != SubclassArmorFormulaUi.NONE }?.armorFormula
            ?: SubclassArmorFormulaUi.NONE,
        hitPointBonus = active.sumOf { it.hitPointsPerClassLevel * classLevel },
        initiativeBonus = active.sumOf(SubclassStatRulesUi::initiativeBonus) +
            if (active.any(SubclassStatRulesUi::initiativeHalfProficiencyBonus)) (proficiencyBonus + 1) / 2 else 0,
        speedBonusFeet = active.sumOf(SubclassStatRulesUi::speedBonusFeet),
        savingThrowBonus = active.sumOf(SubclassStatRulesUi::savingThrowBonus),
        attackBonus = active.sumOf(SubclassStatRulesUi::attackBonus),
        criticalThreshold = active.mapNotNull(SubclassStatRulesUi::criticalThreshold).minOrNull() ?: 20,
        initiativeRollMode = active.lastOrNull { it.initiativeRollMode != RollMode.NORMAL }?.initiativeRollMode
            ?: RollMode.NORMAL,
    )
}

internal fun SubclassOptionUi.resolveFeatures(
    classLevel: Int,
    proficiencyBonus: Int,
    existing: List<FeatureUi> = emptyList(),
    abilities: Map<String, Int> = emptyMap(),
): List<FeatureUi> = mechanics.features.filter { classLevel >= it.minimumClassLevel }.map { grant ->
    val maximum = when (grant.useScaling) {
        SubclassUseScalingUi.NONE -> null
        SubclassUseScalingUi.FIXED -> grant.fixedUses.coerceAtLeast(1)
        SubclassUseScalingUi.CLASS_LEVEL -> classLevel.coerceAtLeast(1)
        SubclassUseScalingUi.PROFICIENCY_BONUS -> proficiencyBonus.coerceAtLeast(1)
        SubclassUseScalingUi.WISDOM_MODIFIER -> floor(((abilities["WIS"] ?: 10) - 10) / 2.0).toInt().coerceAtLeast(1)
    }
    val previous = existing.firstOrNull { it.id == grant.feature.id }
    val remaining = maximum?.let { newMaximum ->
        val spent = previous?.let { ((it.maximum ?: newMaximum) - (it.remaining ?: newMaximum)).coerceAtLeast(0) } ?: 0
        (newMaximum - spent).coerceIn(0, newMaximum)
    }
    grant.feature.copy(remaining = remaining, maximum = maximum)
}

internal fun SubclassOptionUi.resolveSpells(classLevel: Int): List<SpellUi> =
    mechanics.spells.filter { classLevel >= it.minimumClassLevel }.map(SubclassSpellGrantUi::spell)

internal fun subclassFeaturePrefix(subclassId: String): String = "subclass-grant-${subclassId.replace('.', '-')}-"

private data class FeatureSeed(
    val level: Int,
    val name: String,
    val summary: String,
    val actionCost: ActionCost = ActionCost(),
    val recovery: Recovery = Recovery.MANUAL,
    val effect: FeatureEffect = FeatureEffect.RESOURCE_ONLY,
    val useScaling: SubclassUseScalingUi = SubclassUseScalingUi.NONE,
    val fixedUses: Int = 0,
    val suggest: Boolean = false,
)

private fun slug(value: String): String = value.lowercase().map { if (it.isLetterOrDigit()) it else '-' }
    .joinToString("").replace(Regex("-+"), "-").trim('-')

private fun option(
    id: String,
    ruleset: Ruleset,
    parent: String,
    name: String,
    summaryEnglish: String,
    summaryGerman: String,
    selectionLevel: Int,
    statRules: List<SubclassStatRulesUi> = emptyList(),
    spells: List<SubclassSpellGrantUi> = emptyList(),
    features: List<FeatureSeed> = emptyList(),
): SubclassOptionUi {
    val prefix = subclassFeaturePrefix(id)
    return SubclassOptionUi(
        id = id,
        name = name,
        summaryEnglish = summaryEnglish,
        summaryGerman = summaryGerman,
        mechanics = SubclassMechanicsUi(
            parentClassName = parent,
            ruleset = ruleset,
            selectionLevel = selectionLevel,
            statRules = statRules,
            features = features.map { seed ->
                SubclassFeatureGrantUi(
                    minimumClassLevel = seed.level,
                    feature = FeatureUi(
                        id = "$prefix${slug(seed.name)}",
                        name = seed.name,
                        summary = seed.summary,
                        recovery = seed.recovery,
                        effect = seed.effect,
                        actionCost = seed.actionCost,
                        turnGuideEligible = seed.suggest,
                    ),
                    useScaling = seed.useScaling,
                    fixedUses = seed.fixedUses,
                )
            },
            spells = spells,
        ),
        sourceNote = if (ruleset == Ruleset.Fifth2014) "SRD 5.1 · CC-BY-4.0" else "SRD 5.2.1 · CC-BY-4.0",
    )
}

private fun passive(level: Int, name: String, summary: String) = FeatureSeed(level, name, summary)
private fun active(
    level: Int,
    name: String,
    summary: String,
    cost: ActionCost = ActionCost(actions = 1),
    recovery: Recovery = Recovery.MANUAL,
    uses: Int = 0,
    scaling: SubclassUseScalingUi = if (uses > 0) SubclassUseScalingUi.FIXED else SubclassUseScalingUi.NONE,
    effect: FeatureEffect = FeatureEffect.RESOURCE_ONLY,
) = FeatureSeed(level, name, summary, cost, recovery, effect, scaling, uses, suggest = true)

private fun spell(level: Int, id: String, name: String, spellLevel: Int, summary: String) =
    SubclassSpellGrantUi(
        level,
        SpellUi(
            id,
            name,
            spellLevel,
            summary,
            sourceKind = SpellSourceKind.FEATURE,
            activationCost = CombatPotentialEngine.standardSpellActivationCost("spell.${slug(name)}"),
        ),
    )

/** Complete subclass index from the two CC-licensed fifth-edition SRDs. */
internal object BuiltInSubclassCatalog {
    val entries: List<SubclassOptionUi> = listOf(
        option("srd51.subclass.berserker", Ruleset.Fifth2014, "Barbarian", "Path of the Berserker", "Turns rage into relentless offense.", "Verwandelt Kampfrausch in unerbittliche Offensive.", 3,
            features = listOf(passive(3, "Frenzy", "Rage can fuel an additional melee attack, with the rule's exhaustion cost."), passive(6, "Mindless Rage", "Rage protects against being charmed or frightened."), active(10, "Intimidating Presence", "Attempt to frighten a creature.", uses = 1, recovery = Recovery.LONG_REST), passive(14, "Retaliation", "A nearby attacker can trigger a retaliatory Reaction."))),
        option("srd51.subclass.lore", Ruleset.Fifth2014, "Bard", "College of Lore", "Adds skills, magical secrets, and reactive support.", "Erweitert Fertigkeiten, magische Geheimnisse und reaktive Unterstützung.", 3,
            features = listOf(passive(3, "Bonus Proficiencies", "Gain three skill proficiencies."), FeatureSeed(3, "Cutting Words", "Spend Bardic Inspiration as a Reaction to hinder another creature's roll.", ActionCost(reactions = 1)), passive(6, "Additional Magical Secrets", "Learn two spells from any class."), passive(14, "Peerless Skill", "Add Bardic Inspiration to one of your own ability checks."))),
        option("srd51.subclass.life", Ruleset.Fifth2014, "Cleric", "Life Domain", "Strengthens healing magic and grants protective training.", "Stärkt Heilmagie und verleiht schützende Ausbildung.", 1,
            spells = lifeDomainSpells(Ruleset.Fifth2014),
            features = listOf(passive(1, "Bonus Proficiency", "Gain proficiency with heavy armor."), passive(1, "Disciple of Life", "Healing spells restore additional Hit Points."), active(2, "Preserve Life", "Channel Divinity to restore Hit Points.", recovery = Recovery.SHORT_REST, uses = 1), passive(6, "Blessed Healer", "Healing another creature also restores your Hit Points."), passive(8, "Divine Strike", "A weapon hit can deal extra radiant damage once per turn."), passive(17, "Supreme Healing", "Healing dice use their maximum result."))),
        option("srd51.subclass.land", Ruleset.Fifth2014, "Druid", "Circle of the Land", "Expands spellcasting through a chosen landscape.", "Erweitert Zauberei durch eine gewählte Landschaft.", 2,
            features = listOf(passive(2, "Bonus Cantrip", "Learn one additional Druid cantrip."), active(2, "Natural Recovery", "Recover spell slots during a Short Rest.", recovery = Recovery.LONG_REST, uses = 1), passive(3, "Circle Spells", "Your chosen land grants additional prepared spells."), passive(6, "Land's Stride", "Move through nonmagical difficult terrain and plants more freely."), passive(10, "Nature's Ward", "Gain protection from poison, disease, and certain creatures."), passive(14, "Nature's Sanctuary", "Beasts and plants hesitate to attack you."))),
        champion(Ruleset.Fifth2014, "srd51.subclass.champion"),
        option("srd51.subclass.open-hand", Ruleset.Fifth2014, "Monk", "Way of the Open Hand", "Adds control and recovery to martial arts.", "Ergänzt Kampfkünste um Kontrolle und Erholung.", 3,
            features = listOf(passive(3, "Open Hand Technique", "Flurry of Blows can push, topple, or deny reactions."), active(6, "Wholeness of Body", "Restore your own Hit Points.", recovery = Recovery.LONG_REST, uses = 1), passive(11, "Tranquility", "Begin the day protected by Sanctuary."), active(17, "Quivering Palm", "Set up a potentially devastating delayed effect.", cost = ActionCost(actions = 1)))),
        devotion(Ruleset.Fifth2014, "srd51.subclass.devotion"),
        hunter(Ruleset.Fifth2014, "srd51.subclass.hunter"),
        thief(Ruleset.Fifth2014, "srd51.subclass.thief"),
        draconic(Ruleset.Fifth2014, "srd51.subclass.draconic-bloodline", "Draconic Bloodline", 1, SubclassArmorFormulaUi.DEXTERITY_13),
        fiend(Ruleset.Fifth2014, "srd51.subclass.fiend", "The Fiend", 1),
        evoker(Ruleset.Fifth2014, "srd51.subclass.evocation", "School of Evocation", 2),

        option("srd52.subclass.berserker", Ruleset.Fifth2024, "Barbarian", "Path of the Berserker", "Turns rage into forceful, fearless offense.", "Verwandelt Kampfrausch in kraftvolle, furchtlose Offensive.", 3,
            features = listOf(passive(3, "Frenzy", "Reckless Attack can add extra damage while raging."), passive(6, "Mindless Rage", "Rage protects against being charmed or frightened."), active(10, "Intimidating Presence", "Frighten nearby creatures as a Bonus Action.", ActionCost(bonusActions = 1), Recovery.LONG_REST, scaling = SubclassUseScalingUi.PROFICIENCY_BONUS), passive(14, "Retaliation", "A nearby attacker can trigger a retaliatory Reaction."))),
        option("srd52.subclass.lore", Ruleset.Fifth2024, "Bard", "College of Lore", "Adds expertise, magical discoveries, and reactive support.", "Erweitert Expertise, magische Entdeckungen und reaktive Unterstützung.", 3,
            features = listOf(passive(3, "Bonus Proficiencies", "Gain three skill proficiencies."), FeatureSeed(3, "Cutting Words", "Spend Bardic Inspiration as a Reaction to hinder a creature's roll.", ActionCost(reactions = 1)), passive(6, "Magical Discoveries", "Learn additional spells from broader lists."), passive(14, "Peerless Skill", "Add Bardic Inspiration to one of your own ability checks."))),
        option("srd52.subclass.life", Ruleset.Fifth2024, "Cleric", "Life Domain", "Strengthens healing and life-preserving magic.", "Stärkt Heilung und lebensbewahrende Magie.", 3,
            spells = lifeDomainSpells(Ruleset.Fifth2024),
            features = listOf(passive(3, "Disciple of Life", "Healing spells restore additional Hit Points."), active(3, "Preserve Life", "Channel Divinity to restore Hit Points.", recovery = Recovery.SHORT_REST, uses = 1), passive(6, "Blessed Healer", "Healing another creature also restores your Hit Points."), passive(17, "Supreme Healing", "Healing dice use their maximum result."))),
        option("srd52.subclass.land", Ruleset.Fifth2024, "Druid", "Circle of the Land", "Adapts spellcasting to a chosen environment.", "Passt Zauberei an eine gewählte Umgebung an.", 3,
            features = listOf(passive(3, "Circle of the Land Spells", "Choose a land whose spells are always prepared."), active(3, "Land's Aid", "Channel nature to harm foes and restore an ally."), active(6, "Natural Recovery", "Recover spell slots and prepare a free casting of a Circle spell.", recovery = Recovery.LONG_REST, uses = 1), passive(10, "Nature's Ward", "Your land choice grants a defensive benefit."), passive(14, "Nature's Sanctuary", "Your land magic can protect nearby allies."))),
        champion(Ruleset.Fifth2024, "srd52.subclass.champion"),
        option("srd52.subclass.open-hand", Ruleset.Fifth2024, "Monk", "Warrior of the Open Hand", "Adds control, mobility, and recovery to unarmed combat.", "Ergänzt waffenlosen Kampf um Kontrolle, Mobilität und Erholung.", 3,
            features = listOf(passive(3, "Open Hand Technique", "Flurry of Blows can add one of several control effects."), active(6, "Wholeness of Body", "Restore Hit Points as a Bonus Action.", ActionCost(bonusActions = 1), Recovery.LONG_REST, scaling = SubclassUseScalingUi.WISDOM_MODIFIER), passive(11, "Fleet Step", "Step of the Wind can be used with greater freedom."), active(17, "Quivering Palm", "Empower an Unarmed Strike with a delayed destructive effect."))),
        devotion(Ruleset.Fifth2024, "srd52.subclass.devotion"),
        hunter(Ruleset.Fifth2024, "srd52.subclass.hunter"),
        thief(Ruleset.Fifth2024, "srd52.subclass.thief"),
        draconic(Ruleset.Fifth2024, "srd52.subclass.draconic-sorcery", "Draconic Sorcery", 3, SubclassArmorFormulaUi.DEXTERITY_AND_CHARISMA_10),
        fiend(Ruleset.Fifth2024, "srd52.subclass.fiend-patron", "Fiend Patron", 3),
        evoker(Ruleset.Fifth2024, "srd52.subclass.evoker", "Evoker", 3),
    )

    fun forClass(ruleset: Ruleset, className: String): List<SubclassOptionUi> = entries
        .filter { it.mechanics.ruleset == ruleset && it.mechanics.parentClassName.equals(className, true) }
        .sortedBy { it.name }

    fun find(ruleset: Ruleset, id: String): SubclassOptionUi? =
        entries.firstOrNull { it.mechanics.ruleset == ruleset && it.id == id }
}

private fun champion(ruleset: Ruleset, id: String) = option(
    id, ruleset, "Fighter", "Champion", "Improves critical hits, athletic performance, and resilience.", "Verbessert kritische Treffer, Athletik und Widerstandskraft.", 3,
    statRules = listOf(
        SubclassStatRulesUi(minimumClassLevel = 3, criticalThreshold = 19, initiativeRollMode = if (ruleset == Ruleset.Fifth2024) RollMode.ADVANTAGE else RollMode.NORMAL),
        SubclassStatRulesUi(minimumClassLevel = 7, initiativeHalfProficiencyBonus = ruleset == Ruleset.Fifth2014),
        SubclassStatRulesUi(minimumClassLevel = 15, criticalThreshold = 18),
    ),
    features = if (ruleset == Ruleset.Fifth2014) listOf(
        passive(3, "Improved Critical", "Weapon attacks score critical hits on a 19 or 20."),
        passive(7, "Remarkable Athlete", "Add half your Proficiency Bonus to eligible physical checks and initiative."),
        passive(10, "Additional Fighting Style", "Gain another Fighting Style."),
        passive(15, "Superior Critical", "Weapon attacks score critical hits on an 18–20."),
        passive(18, "Survivor", "Regain Hit Points while below half your maximum."),
    ) else listOf(
        passive(3, "Improved Critical", "Weapon and Unarmed Strike attacks score critical hits on a 19 or 20."),
        passive(3, "Remarkable Athlete", "Gain advantage on initiative and selected Strength checks."),
        passive(7, "Additional Fighting Style", "Gain another Fighting Style."),
        passive(10, "Heroic Warrior", "Gain Heroic Inspiration at the start of your turn if you lack it."),
        passive(15, "Superior Critical", "Attacks score critical hits on an 18–20."),
        passive(18, "Survivor", "Gain the subclass's high-level survival benefits."),
    ),
)

private fun devotion(ruleset: Ruleset, id: String) = option(
    id, ruleset, "Paladin", "Oath of Devotion", "Supports allies with sacred weapons, protection, and devotion magic.", "Unterstützt Verbündete mit heiligen Waffen, Schutz und Hingabemagie.", if (ruleset == Ruleset.Fifth2014) 3 else 3,
    spells = devotionSpells(ruleset),
    features = if (ruleset == Ruleset.Fifth2014) listOf(
        active(3, "Sacred Weapon", "Empower one weapon with Channel Divinity.", recovery = Recovery.SHORT_REST, uses = 1),
        active(3, "Turn the Unholy", "Present your holy symbol to turn fiends and undead.", recovery = Recovery.SHORT_REST, uses = 1),
        passive(7, "Aura of Devotion", "Nearby allies are protected from being charmed."),
        passive(15, "Purity of Spirit", "Gain enduring protection from evil and good."),
        active(20, "Holy Nimbus", "Radiate sunlight and damage nearby enemies.", recovery = Recovery.LONG_REST, uses = 1),
    ) else listOf(
        active(3, "Sacred Weapon", "Empower a melee weapon when you take the Attack action.", ActionCost(), Recovery.SHORT_REST, uses = 1),
        passive(7, "Aura of Devotion", "You and allies in your aura are immune to being charmed."),
        passive(15, "Smite of Protection", "Divine Smite briefly grants cover to allies in your aura."),
        active(20, "Holy Nimbus", "Empower your aura with protective sunlight.", ActionCost(bonusActions = 1), Recovery.LONG_REST, uses = 1),
    ),
)

private fun hunter(ruleset: Ruleset, id: String) = option(
    id, ruleset, "Ranger", "Hunter", "Chooses adaptable techniques against different foes.", "Wählt anpassbare Techniken gegen unterschiedliche Gegner.", 3,
    features = listOf(
        passive(3, "Hunter's Prey", "Choose a technique for dealing with dangerous prey."),
        passive(7, "Defensive Tactics", "Choose a defensive technique."),
        passive(11, "Multiattack", "Gain the edition-specific multiattack option."),
        passive(15, "Superior Hunter's Defense", "Gain an advanced defensive technique."),
    ),
)

private fun thief(ruleset: Ruleset, id: String) = option(
    id, ruleset, "Rogue", "Thief", "Improves mobility, stealth, object use, and opportunistic turns.", "Verbessert Mobilität, Heimlichkeit, Gegenstandsnutzung und Gelegenheiten.", 3,
    features = listOf(
        FeatureSeed(3, "Fast Hands", "Use eligible objects and thief tools with Cunning Action.", ActionCost(bonusActions = 1), suggest = true),
        passive(3, "Second-Story Work", "Climb and jump with exceptional speed and reach."),
        passive(9, "Supreme Sneak", "Gain a stronger stealth benefit."),
        passive(13, "Use Magic Device", "Use magic items with fewer restrictions."),
        passive(17, "Thief's Reflexes", "Gain an additional turn at the start of combat."),
    ),
)

private fun draconic(ruleset: Ruleset, id: String, name: String, selectionLevel: Int, armor: SubclassArmorFormulaUi) = option(
    id, ruleset, "Sorcerer", name, "Gains draconic resilience, elemental magic, and flight.", "Erhält drakonische Widerstandskraft, Elementarmagie und Flugfähigkeit.", selectionLevel,
    statRules = listOf(SubclassStatRulesUi(minimumClassLevel = selectionLevel, armorFormula = armor, hitPointsPerClassLevel = 1)),
    spells = if (ruleset == Ruleset.Fifth2024) listOf(
        spell(3, "subclass-spell-alter-self", "Alter Self", 2, "Always prepared Draconic spell"),
        spell(3, "subclass-spell-chromatic-orb", "Chromatic Orb", 1, "Always prepared Draconic spell"),
        spell(3, "subclass-spell-command-draconic", "Command", 1, "Always prepared Draconic spell"),
        spell(3, "subclass-spell-dragons-breath", "Dragon's Breath", 2, "Always prepared Draconic spell"),
        spell(5, "subclass-spell-fear", "Fear", 3, "Always prepared Draconic spell"),
        spell(5, "subclass-spell-fly", "Fly", 3, "Always prepared Draconic spell"),
        spell(7, "subclass-spell-arcane-eye", "Arcane Eye", 4, "Always prepared Draconic spell"),
        spell(7, "subclass-spell-charm-monster", "Charm Monster", 4, "Always prepared Draconic spell"),
        spell(9, "subclass-spell-legend-lore", "Legend Lore", 5, "Always prepared Draconic spell"),
        spell(9, "subclass-spell-summon-dragon", "Summon Dragon", 5, "Always prepared Draconic spell"),
    ) else emptyList(),
    features = listOf(
        passive(selectionLevel, "Draconic Resilience", "Draconic magic increases Hit Points and can improve unarmored Armor Class."),
        passive(if (ruleset == Ruleset.Fifth2014) 6 else 6, "Elemental Affinity", "Matching elemental spells receive the subclass benefit."),
        active(14, "Dragon Wings", "Manifest draconic wings and gain flight.", ActionCost(bonusActions = 1)),
        active(18, "Draconic Presence", "Project a powerful draconic presence.", recovery = Recovery.LONG_REST, uses = 1),
    ),
)

private fun fiend(ruleset: Ruleset, id: String, name: String, selectionLevel: Int) = option(
    id, ruleset, "Warlock", name, "Draws temporary vitality, luck, resilience, and destructive magic from a fiendish pact.", "Bezieht zeitweilige Vitalität, Glück, Widerstandskraft und zerstörerische Magie aus einem infernalischen Pakt.", selectionLevel,
    spells = if (ruleset == Ruleset.Fifth2024) listOf(
        spell(3, "subclass-spell-burning-hands-fiend", "Burning Hands", 1, "Always prepared Fiend Patron spell"),
        spell(3, "subclass-spell-command-fiend", "Command", 1, "Always prepared Fiend Patron spell"),
        spell(3, "subclass-spell-scorching-ray-fiend", "Scorching Ray", 2, "Always prepared Fiend Patron spell"),
        spell(3, "subclass-spell-suggestion-fiend", "Suggestion", 2, "Always prepared Fiend Patron spell"),
        spell(5, "subclass-spell-fireball-fiend", "Fireball", 3, "Always prepared Fiend Patron spell"),
        spell(5, "subclass-spell-stinking-cloud-fiend", "Stinking Cloud", 3, "Always prepared Fiend Patron spell"),
        spell(7, "subclass-spell-fire-shield-fiend", "Fire Shield", 4, "Always prepared Fiend Patron spell"),
        spell(7, "subclass-spell-wall-of-fire-fiend", "Wall of Fire", 4, "Always prepared Fiend Patron spell"),
        spell(9, "subclass-spell-geas-fiend", "Geas", 5, "Always prepared Fiend Patron spell"),
        spell(9, "subclass-spell-insect-plague-fiend", "Insect Plague", 5, "Always prepared Fiend Patron spell"),
    ) else emptyList(),
    features = listOf(
        passive(selectionLevel, "Dark One's Blessing", "Defeating a hostile creature can grant Temporary Hit Points."),
        active(6, "Dark One's Own Luck", "Add a d10 to an ability check or saving throw.", recovery = Recovery.SHORT_REST, uses = 1),
        passive(10, "Fiendish Resilience", "Choose a damage type to resist."),
        active(14, "Hurl Through Hell", "A hit can send a target through a terrifying realm.", recovery = Recovery.LONG_REST, uses = 1),
    ),
)

private fun evoker(ruleset: Ruleset, id: String, name: String, selectionLevel: Int) = option(
    id, ruleset, "Wizard", name, "Shapes evocation spells for reliable, controlled damage.", "Formt Hervorrufungszauber für zuverlässigen, kontrollierten Schaden.", selectionLevel,
    features = if (ruleset == Ruleset.Fifth2014) listOf(
        passive(2, "Evocation Savant", "Copy evocation spells into your spellbook with reduced gold and time."),
        passive(2, "Sculpt Spells", "Protect selected creatures from your evocation areas."),
        passive(6, "Potent Cantrip", "Damaging cantrips remain effective when a target resists them."),
        passive(10, "Empowered Evocation", "Add Intelligence to one eligible evocation damage roll."),
        active(14, "Overchannel", "Deal maximum damage with an eligible Wizard spell, accepting risk on repeated uses."),
    ) else listOf(
        passive(3, "Evocation Savant", "Add two eligible evocation spells and gain more as new spell levels unlock."),
        passive(3, "Potent Cantrip", "Damaging cantrips remain effective when a target resists them."),
        passive(6, "Sculpt Spells", "Protect selected creatures from your evocation areas."),
        passive(10, "Empowered Evocation", "Add Intelligence to one eligible evocation damage roll."),
        active(14, "Overchannel", "Deal maximum damage with an eligible Wizard spell, accepting risk on repeated uses."),
    ),
)

private fun lifeDomainSpells(ruleset: Ruleset): List<SubclassSpellGrantUi> = if (ruleset == Ruleset.Fifth2014) listOf(
    spell(1, "subclass-spell-bless-life-51", "Bless", 1, "Always prepared Life Domain spell"),
    spell(1, "subclass-spell-cure-wounds-life-51", "Cure Wounds", 1, "Always prepared Life Domain spell"),
    spell(3, "subclass-spell-lesser-restoration-life-51", "Lesser Restoration", 2, "Always prepared Life Domain spell"),
    spell(3, "subclass-spell-spiritual-weapon-life-51", "Spiritual Weapon", 2, "Always prepared Life Domain spell"),
    spell(5, "subclass-spell-beacon-of-hope-life-51", "Beacon of Hope", 3, "Always prepared Life Domain spell"),
    spell(5, "subclass-spell-revivify-life-51", "Revivify", 3, "Always prepared Life Domain spell"),
    spell(7, "subclass-spell-death-ward-life-51", "Death Ward", 4, "Always prepared Life Domain spell"),
    spell(7, "subclass-spell-guardian-of-faith-life-51", "Guardian of Faith", 4, "Always prepared Life Domain spell"),
    spell(9, "subclass-spell-mass-cure-wounds-life-51", "Mass Cure Wounds", 5, "Always prepared Life Domain spell"),
    spell(9, "subclass-spell-raise-dead-life-51", "Raise Dead", 5, "Always prepared Life Domain spell"),
) else listOf(
    spell(3, "subclass-spell-aid-life-52", "Aid", 2, "Always prepared Life Domain spell"),
    spell(3, "subclass-spell-bless-life-52", "Bless", 1, "Always prepared Life Domain spell"),
    spell(3, "subclass-spell-cure-wounds-life-52", "Cure Wounds", 1, "Always prepared Life Domain spell"),
    spell(3, "subclass-spell-lesser-restoration-life-52", "Lesser Restoration", 2, "Always prepared Life Domain spell"),
    spell(5, "subclass-spell-mass-healing-word-life-52", "Mass Healing Word", 3, "Always prepared Life Domain spell"),
    spell(5, "subclass-spell-revivify-life-52", "Revivify", 3, "Always prepared Life Domain spell"),
    spell(7, "subclass-spell-aura-of-life-life-52", "Aura of Life", 4, "Always prepared Life Domain spell"),
    spell(7, "subclass-spell-death-ward-life-52", "Death Ward", 4, "Always prepared Life Domain spell"),
    spell(9, "subclass-spell-greater-restoration-life-52", "Greater Restoration", 5, "Always prepared Life Domain spell"),
    spell(9, "subclass-spell-mass-cure-wounds-life-52", "Mass Cure Wounds", 5, "Always prepared Life Domain spell"),
)

private fun devotionSpells(ruleset: Ruleset): List<SubclassSpellGrantUi> {
    val secondLevelOneSpell = if (ruleset == Ruleset.Fifth2014) "Sanctuary" else "Shield of Faith"
    val firstLevelTwoSpell = if (ruleset == Ruleset.Fifth2014) "Lesser Restoration" else "Aid"
    return listOf(
        spell(3, "subclass-spell-protection-devotion-${ruleset.name}", "Protection from Evil and Good", 1, "Always prepared Oath spell"),
        spell(3, "subclass-spell-level-one-devotion-${ruleset.name}", secondLevelOneSpell, 1, "Always prepared Oath spell"),
        spell(5, "subclass-spell-level-two-devotion-${ruleset.name}", firstLevelTwoSpell, 2, "Always prepared Oath spell"),
        spell(5, "subclass-spell-zone-of-truth-devotion-${ruleset.name}", "Zone of Truth", 2, "Always prepared Oath spell"),
        spell(9, "subclass-spell-beacon-of-hope-devotion-${ruleset.name}", "Beacon of Hope", 3, "Always prepared Oath spell"),
        spell(9, "subclass-spell-dispel-magic-devotion-${ruleset.name}", "Dispel Magic", 3, "Always prepared Oath spell"),
        spell(13, "subclass-spell-freedom-of-movement-devotion-${ruleset.name}", "Freedom of Movement", 4, "Always prepared Oath spell"),
        spell(13, "subclass-spell-guardian-of-faith-devotion-${ruleset.name}", "Guardian of Faith", 4, "Always prepared Oath spell"),
        spell(17, "subclass-spell-commune-devotion-${ruleset.name}", "Commune", 5, "Always prepared Oath spell"),
        spell(17, "subclass-spell-flame-strike-devotion-${ruleset.name}", "Flame Strike", 5, "Always prepared Oath spell"),
    )
}
