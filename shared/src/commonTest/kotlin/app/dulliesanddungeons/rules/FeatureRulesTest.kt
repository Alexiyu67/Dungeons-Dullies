package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.ResourcePool
import app.dulliesanddungeons.domain.RulesetId
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureRulesTest {
    private fun context(fighterLevel: Int) = ScalingContext(
        totalLevel = fighterLevel,
        classLevels = mapOf("fighter" to fighterLevel),
        abilities = Ability.entries.associateWith { 10 },
        proficiencyBonus = DerivedStatRules.fiveEProficiencyBonus(fighterLevel),
    )

    @Test
    fun secondWindUsesFighterLevelAndVersionedUseProgression() {
        val definition = FifthEditionFeatureRules.secondWind(RulesetId.FIFTH_EDITION_2024)

        assertEquals(2, ResourceRules.create(requireNotNull(definition.resource), context(1)).maximum)
        assertEquals(3, ResourceRules.create(requireNotNull(definition.resource), context(4)).maximum)
        assertEquals(4, ResourceRules.create(requireNotNull(definition.resource), context(10)).maximum)
        assertEquals(10, requireNotNull(definition.activations.single().roll).modifier.evaluate(context(10)))
        assertEquals("second_wind", definition.activations.single().resourceSpends.single().resourceId)
    }

    @Test
    fun secondWind2024RecoversOneOnShortRestAndAllOnLongRest() {
        val definition = requireNotNull(
            FifthEditionFeatureRules.secondWind(RulesetId.FIFTH_EDITION_2024).resource,
        )
        val spent = ResourcePool(
            "second_wind",
            "Second Wind",
            current = 1,
            maximum = 4,
            recoveryRules = definition.recoveryRules,
        )

        assertEquals(2, ResourceRules.recover(spent, Recovery.SHORT_REST).current)
        assertEquals(4, ResourceRules.recover(spent, Recovery.LONG_REST).current)
    }

    @Test
    fun increasingMaximumPreservesSpentUses() {
        val old = ResourcePool("second_wind", "Second Wind", current = 1, maximum = 2)

        assertEquals(3, ResourceRules.rescaleMaximum(old, 4).current)
    }
}
