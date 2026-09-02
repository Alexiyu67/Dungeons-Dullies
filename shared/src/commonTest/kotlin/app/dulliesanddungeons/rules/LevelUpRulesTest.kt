package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterDocument
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.ClassDefinition
import app.dulliesanddungeons.domain.ClassLevel
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.HitPointGainMethod
import app.dulliesanddungeons.domain.HitPointGainRecord
import app.dulliesanddungeons.domain.LevelProgressionEntry
import app.dulliesanddungeons.domain.ProgressionLedger
import app.dulliesanddungeons.domain.RulePredicate
import app.dulliesanddungeons.domain.RulesetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelUpRulesTest {
    @Test
    fun applyingAValidLevelUpUpdatesBuildHealthAndLedgerAtomically() {
        val current = document()
        val target = current.build.copy(rules = rules(listOf(ClassLevel("fighter", 6))))
        val entry = LevelProgressionEntry(
            characterLevel = 6,
            classId = "fighter",
            classLevel = 6,
            hitPoints = HitPointGainRecord(HitPointGainMethod.FIXED, 10, constitutionModifier = 2, totalGain = 8),
        )

        val updated = LevelUpRules.apply(current, target, entry, mapOf("fighter" to fighter()))

        assertEquals(6, updated.build.level)
        assertEquals(38, updated.state.maximumHitPoints)
        assertEquals(28, updated.state.currentHitPoints)
        assertEquals(entry, updated.progression.entries.single())
    }

    @Test
    fun levelUpPreservesMaximumHitPointReductionAndUsesTheEffectiveMaximum() {
        val current = document().copy(
            state = document().state.copy(
                currentHitPoints = 20,
                maximumHitPointReduction = 10,
            ),
        )
        val target = current.build.copy(rules = rules(listOf(ClassLevel("fighter", 6))))
        val entry = LevelProgressionEntry(
            characterLevel = 6,
            classId = "fighter",
            classLevel = 6,
            hitPoints = HitPointGainRecord(HitPointGainMethod.FIXED, 10, constitutionModifier = 2, totalGain = 8),
        )

        val updated = LevelUpRules.apply(current, target, entry, mapOf("fighter" to fighter()))

        assertEquals(10, updated.state.maximumHitPointReduction)
        assertEquals(28, updated.state.effectiveMaximumHitPoints(target.ruleset))
        assertEquals(28, updated.state.currentHitPoints)
    }

    @Test
    fun multiclassPrerequisitesAreCheckedAgainstTheCurrentCharacter() {
        val current = document()
        val target = current.build.copy(
            rules = rules(listOf(ClassLevel("fighter", 5), ClassLevel("wizard", 1))),
        )
        val entry = LevelProgressionEntry(
            characterLevel = 6,
            classId = "wizard",
            classLevel = 1,
            hitPoints = HitPointGainRecord(HitPointGainMethod.FIXED, 6, constitutionModifier = 2, totalGain = 6),
        )
        val wizard = fighter().copy(
            id = "wizard",
            hitDieSides = 6,
            primaryAbilities = setOf(Ability.INTELLIGENCE),
            multiclassPrerequisite = RulePredicate.AbilityAtLeast(Ability.INTELLIGENCE, 13),
        )

        val issues = LevelUpRules.validateTransition(current, target, entry, mapOf("wizard" to wizard))

        assertTrue(issues.any { it.code == "multiclass.prerequisite" })
    }

    @Test
    fun characterHitDieOverrideIsUsedForLevelUpValidation() {
        val current = document()
        val targetRules = rules(listOf(ClassLevel("fighter", 6))).copy(
            classHitDieOverrides = mapOf("fighter" to 8),
        )
        val target = current.build.copy(rules = targetRules)
        val entry = LevelProgressionEntry(
            characterLevel = 6,
            classId = "fighter",
            classLevel = 6,
            hitPoints = HitPointGainRecord(HitPointGainMethod.FIXED, 8, constitutionModifier = 2, totalGain = 7),
        )

        val issues = LevelUpRules.validateTransition(current, target, entry, mapOf("fighter" to fighter()))

        assertTrue(issues.none { it.code == "level_up.hit_die" })
    }

    private fun document(): CharacterDocument {
        val build = CharacterBuild(
            id = "hero",
            name = "Hero",
            ruleset = RulesetId.FIFTH_EDITION_2024,
            rules = rules(listOf(ClassLevel("fighter", 5))),
        )
        return CharacterDocument(
            build = build,
            state = CharacterState("hero", currentHitPoints = 20, maximumHitPoints = 30),
            progression = ProgressionLedger(baselineLevel = 5, baselineClassLevels = mapOf("fighter" to 5)),
        )
    }

    private fun rules(classes: List<ClassLevel>) = FiveEBuildData(
        ancestryId = "human",
        backgroundId = "guard",
        classes = classes,
        abilities = Ability.entries.associateWith {
            when (it) {
                Ability.STRENGTH -> 16
                Ability.CONSTITUTION -> 14
                else -> 10
            }
        },
    )

    private fun fighter() = ClassDefinition(
        id = "fighter",
        name = "Fighter",
        ruleset = RulesetId.FIFTH_EDITION_2024,
        hitDieSides = 10,
        primaryAbilities = setOf(Ability.STRENGTH),
        savingThrowAbilities = setOf(Ability.STRENGTH, Ability.CONSTITUTION),
        beginnerComplexity = 1,
    )
}
