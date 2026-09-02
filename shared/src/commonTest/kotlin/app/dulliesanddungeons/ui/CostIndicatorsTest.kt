package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Recovery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CostIndicatorsTest {
    @Test
    fun finiteUseFeatureDoesNotRepeatItsNameAsAResourceCost() {
        val feature = FeatureUi(
            id = "second-wind",
            name = "Second Wind",
            summary = "Regain hit points.",
            remaining = 1,
            maximum = 1,
            recovery = Recovery.SHORT_REST,
            actionCost = ActionCost(bonusActions = 1),
        )

        assertEquals(
            listOf(CostTokenUi(CostTokenKind.BonusAction)),
            feature.toCostTokens(),
        )
    }

    @Test
    fun finiteUseFeatureWithoutAnotherCostHasNoCostToken() {
        val feature = FeatureUi(
            id = "indomitable",
            name = "Indomitable",
            summary = "Reroll a failed saving throw.",
            remaining = 1,
            maximum = 1,
            recovery = Recovery.LONG_REST,
        )

        assertTrue(feature.toCostTokens().isEmpty())
    }

    @Test
    fun featureUsingASeparatePoolKeepsItsReadableResourceCost() {
        val feature = FeatureUi(
            id = "maneuver-riposte",
            name = "Riposte",
            summary = "Spend a Superiority Die to attack.",
            resourceId = "superiority-dice",
            actionCost = ActionCost(reactions = 1),
        )

        assertEquals(
            listOf(
                CostTokenUi(CostTokenKind.Reaction),
                CostTokenUi(CostTokenKind.Resource, labelOverride = "Superiority Dice"),
            ),
            feature.toCostTokens(),
        )
    }

    @Test
    fun searchOnlyLabelsASeparateSharedResource() {
        val state = DndAppState()

        assertEquals(
            null,
            state.search("Indomitable").single { it.id == "feature-indomitable" }.resourceLabel,
        )
        assertEquals(
            "Superiority Dice",
            state.search("Riposte").single { it.id == "feature-maneuver-riposte" }.resourceLabel,
        )
    }
}
