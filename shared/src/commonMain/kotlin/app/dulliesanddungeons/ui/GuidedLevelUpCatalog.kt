package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ActionCost

import app.dulliesanddungeons.domain.Recovery

/**
 * Small, data-driven catalog for choices that cannot be inferred from a character level alone.
 * It deliberately contains summaries rather than rulebook prose; installed/private content can
 * contribute additional choices without changing the level-up reducer.
 */
internal enum class GuidedLevelChoiceKind { SUBCLASS, SPELL, CLASS_OPTION }

internal data class GuidedFeatureUnlock(
    val id: String,
    val name: String,
    val summary: String,
    val minimumLevel: Int,
    val subclass: String? = null,
    val recovery: Recovery = Recovery.MANUAL,
)

internal data class GuidedSubclassOption(
    val id: String,
    val name: String,
    val summary: String,
)

internal data class GuidedClassDefinition(
    val className: String,
    val subclassLevel2014: Int,
    val subclassLevel2024: Int,
    val subclasses: List<GuidedSubclassOption>,
    val featureUnlocks: List<GuidedFeatureUnlock>,
    val spellsLearnedEachLevel: Int = 0,
    val spellOptions: List<SpellUi> = emptyList(),
)

internal data class GuidedLevelOptionUi(
    val id: String,
    val name: String,
    val summary: String,
    val subclassId: String? = null,
    val subclassName: String? = null,
    val feature: FeatureUi? = null,
    val spell: SpellUi? = null,
)

internal data class GuidedLevelChoiceUi(
    val id: String,
    val title: String,
    val kind: GuidedLevelChoiceKind,
    val chooseCount: Int,
    val options: List<GuidedLevelOptionUi>,
)

internal val guidedClassDefinitions: Map<String, GuidedClassDefinition> = listOf(
    GuidedClassDefinition(
        className = "Fighter",
        subclassLevel2014 = 3,
        subclassLevel2024 = 3,
        subclasses = listOf(
            GuidedSubclassOption("champion", "Champion", "Straightforward martial improvements."),
            GuidedSubclassOption("battle-master", "Battle Master", "Superiority Dice fuel chosen maneuvers."),
        ),
        featureUnlocks = listOf(
            GuidedFeatureUnlock("second-wind", "Second Wind", "Regain HP as a Bonus Action.", 1, recovery = Recovery.SHORT_REST),
            GuidedFeatureUnlock("action-surge", "Action Surge", "Take one additional action this turn.", 2, recovery = Recovery.SHORT_REST),
            GuidedFeatureUnlock("extra-attack", "Extra Attack", "Attack more than once with the Attack action.", 5),
            GuidedFeatureUnlock("indomitable", "Indomitable", "Reroll a failed saving throw.", 9, recovery = Recovery.LONG_REST),
        ),
    ),
    GuidedClassDefinition(
        className = "Wizard",
        subclassLevel2014 = 2,
        subclassLevel2024 = 3,
        subclasses = listOf(
            GuidedSubclassOption("evoker", "Evoker", "Specializes in reliable evocation magic."),
        ),
        featureUnlocks = listOf(
            GuidedFeatureUnlock("wizard-spellcasting", "Spellcasting", "Prepare and cast Wizard spells from a spellbook.", 1),
            GuidedFeatureUnlock("arcane-recovery", "Arcane Recovery", "Recover expended spell slots after a Short Rest.", 1, recovery = Recovery.LONG_REST),
            GuidedFeatureUnlock("potent-cantrip", "Potent Cantrip", "Your damaging cantrips remain effective when a target resists them.", 6, subclass = "Evoker"),
        ),
        spellsLearnedEachLevel = 2,
        spellOptions = listOf(
            SpellUi("chromatic-orb", "Chromatic Orb", 1, "Ranged spell attack · choose an energy type"),
            SpellUi("feather-fall", "Feather Fall", 1, "Reaction · slow falling creatures", activationCost = ActionCost(reactions = 1)),
            SpellUi("invisibility", "Invisibility", 2, "Concentration · make a creature invisible"),
            SpellUi("misty-step", "Misty Step", 2, "Bonus-action teleport", activationCost = ActionCost(bonusActions = 1)),
            SpellUi("web", "Web", 2, "Concentration · restraining area"),
            SpellUi("counterspell", "Counterspell", 3, "Reaction · interrupt a spell", activationCost = ActionCost(reactions = 1)),
            SpellUi("dispel-magic", "Dispel Magic", 3, "End a spell on a creature or object"),
            SpellUi("fly", "Fly", 3, "Concentration · grant a flying speed"),
            SpellUi("haste", "Haste", 3, "Concentration · improve speed and actions"),
            SpellUi("lightning-bolt", "Lightning Bolt", 3, "Dexterity save · line of lightning"),
        ),
    ),
    GuidedClassDefinition(
        className = "Sorcerer",
        subclassLevel2014 = 1,
        subclassLevel2024 = 3,
        subclasses = listOf(
            GuidedSubclassOption("draconic-bloodline", "Draconic Bloodline", "Innate magic shaped by draconic ancestry."),
        ),
        featureUnlocks = listOf(
            GuidedFeatureUnlock("sorcerer-spellcasting", "Spellcasting", "Cast Sorcerer spells using Charisma.", 1),
            GuidedFeatureUnlock("sorcery-points", "Sorcery Points", "Fuel Sorcerer class options.", 2, recovery = Recovery.LONG_REST),
            GuidedFeatureUnlock("metamagic", "Metamagic", "Alter spells with chosen Metamagic options.", 3),
            GuidedFeatureUnlock("elemental-affinity", "Elemental Affinity", "Your draconic ancestry empowers matching elemental spells.", 6, subclass = "Draconic Bloodline"),
        ),
        spellsLearnedEachLevel = 1,
        spellOptions = listOf(
            SpellUi("chromatic-orb", "Chromatic Orb", 1, "Ranged spell attack · choose an energy type"),
            SpellUi("thunderwave", "Thunderwave", 1, "Constitution save · thunderous wave"),
            SpellUi("scorching-ray", "Scorching Ray", 2, "Multiple ranged spell attacks"),
            SpellUi("suggestion", "Suggestion", 2, "Influence a creature with a course of action"),
            SpellUi("counterspell", "Counterspell", 3, "Reaction · interrupt a spell", activationCost = ActionCost(reactions = 1)),
            SpellUi("fireball", "Fireball", 3, "Dexterity save · area fire damage"),
            SpellUi("fly", "Fly", 3, "Concentration · grant a flying speed"),
            SpellUi("haste", "Haste", 3, "Concentration · improve speed and actions"),
        ),
    ),
    GuidedClassDefinition(
        className = "Monk",
        subclassLevel2014 = 3,
        subclassLevel2024 = 3,
        subclasses = listOf(
            GuidedSubclassOption("warrior-open-hand", "Warrior of the Open Hand", "Adds control and recovery to unarmed combat."),
        ),
        featureUnlocks = listOf(
            GuidedFeatureUnlock("martial-arts", "Martial Arts", "Use martial training with Monk weapons and Unarmed Strikes.", 1),
            GuidedFeatureUnlock("focus-points", "Focus Points", "Fuel Monk techniques.", 2, recovery = Recovery.SHORT_REST),
            GuidedFeatureUnlock("extra-attack", "Extra Attack", "Attack twice with the Attack action.", 5),
            GuidedFeatureUnlock("stunning-strike", "Stunning Strike", "Spend Focus after a hit to hinder the target.", 5),
            GuidedFeatureUnlock("empowered-strikes", "Empowered Strikes", "Your Unarmed Strikes overcome resistance to ordinary damage.", 6),
            GuidedFeatureUnlock("wholeness-of-body", "Wholeness of Body", "Use Focus to restore Hit Points.", 6, subclass = "Warrior of the Open Hand"),
        ),
    ),
).associateBy(GuidedClassDefinition::className)

internal val battleMasterManeuverOptions = listOf(
    GuidedLevelOptionUi(
        "maneuver-precision-attack",
        "Precision Attack",
        "Add a Superiority Die to an attack roll.",
        feature = FeatureUi("maneuver-precision-attack", "Precision Attack", "Spend a Superiority Die after an attack roll to add it to the roll.", resourceId = "superiority-dice"),
    ),
    GuidedLevelOptionUi(
        "maneuver-trip-attack",
        "Trip Attack",
        "Add damage and possibly knock the target Prone.",
        feature = FeatureUi("maneuver-trip-attack", "Trip Attack", "After a weapon hit, spend a Superiority Die to add damage and possibly knock the target Prone.", resourceId = "superiority-dice"),
    ),
    GuidedLevelOptionUi(
        "maneuver-riposte",
        "Riposte",
        "Use your Reaction to answer a missed melee attack.",
        feature = FeatureUi("maneuver-riposte", "Riposte", "When a creature misses you in melee, spend a Superiority Die and your Reaction to attack it.", actionCost = ActionCost(reactions = 1), resourceId = "superiority-dice"),
    ),
    GuidedLevelOptionUi(
        "maneuver-pushing-attack",
        "Pushing Attack",
        "Add damage and possibly push the target.",
        feature = FeatureUi("maneuver-pushing-attack", "Pushing Attack", "After a weapon hit, spend a Superiority Die to add damage and possibly push the target.", resourceId = "superiority-dice"),
    ),
    GuidedLevelOptionUi(
        "maneuver-menacing-attack",
        "Menacing Attack",
        "Add damage and possibly frighten the target.",
        feature = FeatureUi("maneuver-menacing-attack", "Menacing Attack", "After a weapon hit, spend a Superiority Die to add damage and possibly frighten the target.", resourceId = "superiority-dice"),
    ),
)

internal val metamagicOptions = listOf(
    GuidedLevelOptionUi("metamagic-careful", "Careful Spell", "Protect selected creatures from part of your spell.", feature = FeatureUi("metamagic-careful", "Careful Spell", "Spend Sorcery Points to protect selected creatures from part of a spell.", resourceId = "sorcery-points")),
    GuidedLevelOptionUi("metamagic-distant", "Distant Spell", "Increase a spell's range.", feature = FeatureUi("metamagic-distant", "Distant Spell", "Spend Sorcery Points to increase a spell's range.", resourceId = "sorcery-points")),
    GuidedLevelOptionUi("metamagic-empowered", "Empowered Spell", "Reroll some damage dice.", feature = FeatureUi("metamagic-empowered", "Empowered Spell", "Spend a Sorcery Point to reroll some spell damage dice.", resourceId = "sorcery-points")),
    GuidedLevelOptionUi("metamagic-quickened", "Quickened Spell", "Cast an eligible spell as a Bonus Action.", feature = FeatureUi("metamagic-quickened", "Quickened Spell", "Spend 2 Sorcery Points to cast an eligible spell as a Bonus Action.", actionCost = ActionCost(bonusActions = 1), resourceId = "sorcery-points", resourceCost = 2)),
    GuidedLevelOptionUi("metamagic-subtle", "Subtle Spell", "Cast without verbal or somatic components.", feature = FeatureUi("metamagic-subtle", "Subtle Spell", "Spend a Sorcery Point to cast without verbal or somatic components.", resourceId = "sorcery-points")),
)

internal val guidedAutomaticFeatureIds: Set<String> = guidedClassDefinitions.values
    .flatMap { definition -> definition.featureUnlocks.map(GuidedFeatureUnlock::id) }
    .toSet() + "superiority-dice"
