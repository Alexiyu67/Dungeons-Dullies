package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.ClassLevel
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.RulesetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DerivedStatsTest {
    @Test
    fun fifthEditionProgressionsAreCalculated() {
        assertEquals(-5, DerivedStatRules.abilityModifier(1))
        assertEquals(0, DerivedStatRules.abilityModifier(10))
        assertEquals(2, DerivedStatRules.fiveEProficiencyBonus(1))
        assertEquals(3, DerivedStatRules.fiveEProficiencyBonus(5))
        assertEquals(6, DerivedStatRules.fiveEProficiencyBonus(20))
        assertEquals(8, DerivedStatRules.fiveESkillModifier(14, 5, proficient = true, expertise = true))
    }

    @Test
    fun pf2eProgressionsStackTypedBonusesAndApplyMap() {
        assertEquals(7, DerivedStatRules.pf2eProficiencyModifier(5, Pf2eProficiencyRank.TRAINED))
        assertEquals(0, DerivedStatRules.pf2eProficiencyModifier(5, Pf2eProficiencyRank.UNTRAINED))
        assertEquals(-4, DerivedStatRules.pf2eMultipleAttackPenalty(2, agile = true))
        assertEquals(-10, DerivedStatRules.pf2eMultipleAttackPenalty(3, agile = false))
        assertEquals(
            3,
            DerivedStatRules.stackPf2eModifiers(
                listOf(
                    TypedModifier(2, ModifierStackType.STATUS),
                    TypedModifier(1, ModifierStackType.STATUS),
                    TypedModifier(-1, ModifierStackType.STATUS),
                    TypedModifier(-2, ModifierStackType.STATUS),
                    TypedModifier(3, ModifierStackType.ITEM),
                ),
            ),
        )
    }

    @Test
    fun fifthEditionMulticlassCasterLevelsUsePerClassRounding() {
        val casterLevel = DerivedStatRules.combinedFiveECasterLevel(
            listOf(
                FiveECasterContribution(classLevels = 3),
                FiveECasterContribution(classLevels = 5, divisor = 2),
                FiveECasterContribution(classLevels = 4, divisor = 3),
            ),
        )

        assertEquals(6, casterLevel)
        assertEquals(listOf(4, 3, 3, 0, 0, 0, 0, 0, 0), DerivedStatRules.fiveESpellSlots(casterLevel))
    }

    @Test
    fun validatorFindsCrossRecordProblems() {
        val build = sampleBuild(level = 3)
        val state = CharacterState(build.id, currentHitPoints = 13, maximumHitPoints = 10)

        val issues = CharacterValidator.validate(build, state)

        assertTrue(issues.any { it.code == "hp.current" })
    }
}

internal fun sampleBuild(
    level: Int = 1,
    classLevels: Int = level,
) = CharacterBuild(
    id = "character-1",
    name = "Ada",
    ruleset = RulesetId.FIFTH_EDITION_2024,
    rules = FiveEBuildData(
        ancestryId = "human",
        heritageId = "versatile",
        backgroundId = "guard",
        classes = listOf(ClassLevel("fighter", classLevels)),
        abilities = Ability.entries.associateWith { 10 },
    ),
)
