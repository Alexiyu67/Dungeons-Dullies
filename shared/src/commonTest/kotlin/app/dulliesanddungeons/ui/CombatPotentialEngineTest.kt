package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.CombatContribution
import app.dulliesanddungeons.domain.CombatContributionTiming
import app.dulliesanddungeons.domain.CombatContributionType
import app.dulliesanddungeons.domain.ActionCost
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombatPotentialEngineTest {
    @Test
    fun fighterElevenSeparatesThreeNormalAndThreeActionSurgeAttacks() {
        val character = character("Fighter", 11, features = listOf(feature("action-surge", "Action Surge")))

        val result = CombatPotentialEngine.attacks(character)

        assertEquals(3, result.baseCount)
        assertEquals(3, result.extraCount)
        assertEquals("3 + 3", result.compactLabel)
    }

    @Test
    fun monkTenUsesImprovedFlurryAsTheBonusActionBranch() {
        val character = character("Monk", 10)

        val result = CombatPotentialEngine.attacks(character)

        assertEquals(2, result.baseCount)
        assertEquals(3, result.extraCount)
        assertEquals("Flurry of Blows", result.bestSources.single().label)
        assertTrue(result.alternatives.any { it.label == "Martial Arts" })
    }

    @Test
    fun nickAttackStacksWithFlurryBecauseItDoesNotSpendTheBonusAction() {
        val weapons = listOf(
            weapon("dagger", "Dagger", "Finesse, light", "Nick"),
            weapon("shortsword", "Shortsword", "Finesse, light", "Vex"),
        )
        val character = character("Monk", 5, weapons = weapons).copy(
            features = listOf(feature("weapon-mastery", "Weapon Mastery")),
        )

        val result = CombatPotentialEngine.attacks(character)

        assertEquals(2, result.baseCount)
        assertEquals(3, result.extraCount)
        assertTrue(result.bestSources.any { it.label.contains("Nick") })
        assertTrue(result.bestSources.any { it.label == "Flurry of Blows" })
    }

    @Test
    fun localPolearmStyleRuleRequiresMatchingOwnedWeapon() {
        val rule = CombatContribution(
            type = CombatContributionType.EXTRA_ATTACKS,
            timing = CombatContributionTiming.BONUS_ACTION,
            requiresAttackAction = true,
            requiredWeaponProperties = setOf("reach"),
        )
        val feat = feature("private-polearm", "Polearm Master").copy(combatContributions = listOf(rule))

        val withoutReach = CombatPotentialEngine.attacks(character("Fighter", 5, features = listOf(feat)))
        val withReach = CombatPotentialEngine.attacks(
            character("Fighter", 5, features = listOf(feat), weapons = listOf(weapon("glaive", "Glaive", "Heavy, reach, two-handed")))
        )

        assertTrue(withoutReach.bestSources.none { it.label == "Polearm Master" })
        assertTrue(withReach.bestSources.any { it.label == "Polearm Master" })
        assertEquals("2 + 3", withReach.compactLabel)
    }

    @Test
    fun quickenedScorchingRayCountsEveryAttackRollAtTheBestFullResourceSlot() {
        val character = character(
            className = "Sorcerer",
            level = 5,
            features = listOf(feature("metamagic-quickened", "Quickened Spell")),
            spells = listOf(SpellUi("subclass-spell-scorching-ray-fiend", "Scorching Ray", 2, "")),
        )

        val result = CombatPotentialEngine.attacks(character)

        assertEquals(1, result.baseCount)
        assertEquals(4, result.extraCount)
        assertEquals("Quickened Spell · Scorching Ray", result.bestSources.single().label)
    }

    @Test
    fun castsCombineActionCantripQuickenedSpellAnd2014ActionSurge() {
        val progression = (1..2).map { LevelProgressionUi(it, "Fighter", 1, HpMethod.Fixed) } +
            (3..5).map { LevelProgressionUi(it, "Sorcerer", 1, HpMethod.Fixed) }
        val character = character(
            className = "Fighter",
            level = 5,
            ruleset = Ruleset.Fifth2014,
            features = listOf(feature("action-surge", "Action Surge"), feature("metamagic-quickened", "Quickened Spell")),
            spells = listOf(SpellUi("spell.fire-bolt", "Fire Bolt", 0, "", spellAttack = true)),
        ).copy(progression = progression, hasSpellcastingCapability = true)

        val result = CombatPotentialEngine.casts(character)

        assertEquals("1 + 2", result.compactLabel)
        assertTrue(result.bestSources.any { it.label == "Quickened Spell" })
        assertTrue(result.bestSources.any { it.label == "Action Surge" })
    }

    @Test
    fun actionSurgeCanUseAMultiAttackSpellUnder2014RulesWhenNoBonusSpellIsCast() {
        val progression = (1..2).map { LevelProgressionUi(it, "Fighter", 1, HpMethod.Fixed) } +
            (3..5).map { LevelProgressionUi(it, "Sorcerer", 1, HpMethod.Fixed) }
        val character = character(
            className = "Fighter",
            level = 5,
            ruleset = Ruleset.Fifth2014,
            spells = listOf(SpellUi("spell.scorching-ray", "Scorching Ray", 2, "", spellAttack = true)),
        ).copy(progression = progression, hasSpellcastingCapability = true)

        val result = CombatPotentialEngine.attacks(character)

        assertEquals("1 + 3", result.compactLabel)
        assertTrue(result.bestSources.any { it.label == "Action Surge · Scorching Ray" })
    }

    @Test
    fun twoPrecastConcentrationSourcesDoNotStack() {
        val character = character(
            className = "Wizard",
            level = 13,
            spells = listOf(
                SpellUi("spell.haste", "Haste", 3, "Concentration"),
                SpellUi("spell.arcane-sword", "Arcane Sword", 7, "Concentration", spellAttack = true),
            ),
        )

        val result = CombatPotentialEngine.attacks(character)

        assertEquals("1 + 1", result.compactLabel)
        assertTrue(result.bestSources.any { it.label == "Haste" })
        assertTrue(result.alternatives.any { it.label == "Arcane Sword" })
    }

    @Test
    fun combatMetadataSurvivesCharacterDocumentProjection() {
        val contribution = CombatContribution(
            CombatContributionType.EXTRA_ATTACKS,
            count = 1,
            timing = CombatContributionTiming.TRIGGERED,
            requiresHit = true,
        )
        val character = character("Fighter", 1, features = listOf(feature("local", "Local feature").copy(combatContributions = listOf(contribution))))

        val document = character.toDocument()
        val restored = document.toCharacterUi()

        assertEquals(2, document.schemaVersion)
        assertEquals(contribution, restored.features.single().combatContributions.single())
    }

    @Test
    fun pf2eDoesNotExposeFifthEditionPotential() {
        val result = CombatPotentialEngine.attacks(character("Fighter", 20, ruleset = Ruleset.Pf2eRemaster))
        assertEquals("0 + 0", result.compactLabel)
    }

    @Test
    fun turnSessionUsesTheSameExtraAttackRulesForRanger() {
        val session = TurnSession(character("Ranger", 5))
        assertEquals(2, session.attacksRemaining)
    }

    @Test
    fun officialSpellTimingOverridesTheLegacyOneActionCatalogDefault() {
        assertEquals(ActionCost(bonusActions = 1), CombatPotentialEngine.standardSpellActivationCost("spell.misty-step"))
        assertEquals(ActionCost(reactions = 1), CombatPotentialEngine.standardSpellActivationCost("spell.shield"))
        assertEquals(ActionCost(actions = 1), CombatPotentialEngine.standardSpellActivationCost("spell.fireball"))
    }

    private fun character(
        className: String,
        level: Int,
        ruleset: Ruleset = Ruleset.Fifth2024,
        features: List<FeatureUi> = emptyList(),
        weapons: List<WeaponUi> = emptyList(),
        spells: List<SpellUi> = emptyList(),
    ) = CharacterUi(
        id = "test-character",
        name = "Test",
        ruleset = ruleset,
        level = level,
        ancestry = "Human",
        className = className,
        subclass = "—",
        hp = 10,
        maxHp = 10,
        armorClass = 10,
        speedFeet = 30,
        initiative = 0,
        proficiency = 2,
        portraitSeed = 0,
        abilities = mapOf("STR" to 10, "DEX" to 10, "CON" to 10, "INT" to 10, "WIS" to 10, "CHA" to 10),
        skills = emptyMap(),
        saves = emptyMap(),
        languages = emptyList(),
        weapons = weapons,
        spells = spells,
        features = features,
        progression = (1..level).map { LevelProgressionUi(it, className, 1, HpMethod.Fixed) },
        hasSpellcastingCapability = spells.isNotEmpty(),
    )

    private fun feature(id: String, name: String) = FeatureUi(id, name, "")

    private fun weapon(id: String, name: String, properties: String, mastery: String = "") = WeaponUi(
        id = id,
        name = name,
        attackBonus = 0,
        damage = "1d6",
        damageType = "Slashing",
        properties = properties,
        mastery = mastery,
    )
}
