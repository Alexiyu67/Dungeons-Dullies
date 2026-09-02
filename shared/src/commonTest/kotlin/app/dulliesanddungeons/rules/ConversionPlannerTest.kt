package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.ChoiceSelection
import app.dulliesanddungeons.domain.FiveEBuildData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ConversionPlannerTest {
    @Test
    fun conversionCreatesSeparateDraftAndDoesNotMutateSource() {
        val source = sampleBuild(level = 5)

        val conversion = ConversionPlanner.copyForRuleset(
            source = source,
            targetCharacterId = "character-2",
            targetRuleset = RulesetId.FIFTH_EDITION_2014,
            explicitMappings = mapOf("human" to "human", "guard" to "soldier", "fighter" to "fighter"),
        )

        assertEquals("character-1", source.id)
        assertEquals(RulesetId.FIFTH_EDITION_2024, source.ruleset)
        assertEquals("character-2", conversion.character.id)
        assertEquals(RulesetId.FIFTH_EDITION_2014, conversion.character.ruleset)
        assertNotEquals(source, conversion.character)
        assertTrue(conversion.plan.preservedFields.contains("name"))
    }

    @Test
    fun crossSystemConversionClearsDerivedChoicesAndRequiresReview() {
        val base = sampleBuild(level = 5)
        val source = base.copy(
            rules = (base.rules as FiveEBuildData).copy(
                knownSpells = listOf(ChoiceSelection("misty_step")),
                proficiencyIds = setOf("save:strength"),
            ),
        )

        val conversion = ConversionPlanner.copyForRuleset(
            source,
            targetCharacterId = "pf2e-character",
            targetRuleset = RulesetId.PF2E_REMASTER,
        )

        assertTrue(conversion.character.classes.isEmpty())
        assertTrue(conversion.character.proficiencyIds.isEmpty())
        assertTrue(conversion.plan.unresolvedChoiceIds.contains("fighter"))
        assertTrue(conversion.plan.warnings.single().contains("guided rebuild"))
    }
}
