package app.dulliesanddungeons.ui

import app.dulliesanddungeons.data.LocalStateStore
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.RollMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubclassCreationTest {
    @Test
    fun licensedCatalogHasOneSrdSubclassForEveryCoreClassInBothEditions() {
        val expectedClasses = setOf(
            "Barbarian", "Bard", "Cleric", "Druid", "Fighter", "Monk",
            "Paladin", "Ranger", "Rogue", "Sorcerer", "Warlock", "Wizard",
        )

        listOf(Ruleset.Fifth2014, Ruleset.Fifth2024).forEach { ruleset ->
            val entries = BuiltInSubclassCatalog.entries.filter { it.mechanics.ruleset == ruleset }
            assertEquals(12, entries.size)
            assertEquals(expectedClasses, entries.map { it.mechanics.parentClassName }.toSet())
            assertEquals(entries.size, entries.map { it.id }.distinct().size)
        }
    }

    @Test
    fun earlySubclassSelectionIsAllowedButMechanicsWaitForTheUnlockLevel() {
        val state = DndAppState(SubclassTestStore())
        state.beginCreate()
        state.creation.name = "Early Champion"
        state.selectCreationClass("Fighter")
        val champion = state.creationSubclassOptions().single { it.name == "Champion" }

        state.selectCreationSubclass(champion.id)

        assertNotNull(state.creationSubclassAdvisory())
        assertEquals(20, champion.resolveStats(2).criticalThreshold)
        assertTrue(champion.resolveFeatures(2, 2).isEmpty())
        state.finishCreate()

        val character = assertNotNull(state.selectedCharacter)
        assertEquals("Champion", character.subclass)
        assertEquals(20, character.criticalHitThreshold)
        assertFalse(character.features.any { it.name == "Improved Critical" })
    }

    @Test
    fun subclassIsRequiredAtItsSelectionLevelAndChampionMechanicsAreApplied() {
        val state = DndAppState(SubclassTestStore())
        state.beginCreate()
        state.creation.name = "Champion"
        state.setCreationLevel(3)
        val before = state.characters.size

        state.finishCreate()
        assertEquals(before, state.characters.size)

        val champion = state.creationSubclassOptions().single { it.name == "Champion" }
        state.selectCreationSubclass(champion.id)
        state.finishCreate()

        val character = assertNotNull(state.selectedCharacter)
        assertEquals(19, character.criticalHitThreshold)
        assertEquals(RollMode.ADVANTAGE, character.initiativeRollMode)
        assertTrue(character.features.any { it.name == "Improved Critical" })
        assertTrue(character.features.filter { it.id.startsWith(subclassFeaturePrefix(champion.id)) }.none { it.turnGuideEligible })

        state.handleSearchResult(state.search("Roll initiative").single { it.id == "initiative" })
        assertEquals(2, state.dicePresentation?.dice?.size)
    }

    @Test
    fun customSubclassMechanicsAffectStatsFeaturesSpellsRestAndPersistence() {
        val store = SubclassTestStore()
        val state = DndAppState(store)
        state.beginCreate()
        state.creation.name = "Storm Weaver"
        state.selectCreationClass("Wizard")
        state.setCreationLevel(3)
        state.creation.startingArmorChoice = StartingArmorChoice.Unarmored
        val mechanics = SubclassMechanicsUi(
            parentClassName = "Wizard",
            ruleset = Ruleset.Fifth2024,
            selectionLevel = 3,
            statRules = listOf(
                SubclassStatRulesUi(
                    minimumClassLevel = 3,
                    armorClassBonus = 1,
                    hitPointsPerClassLevel = 1,
                    initiativeBonus = 2,
                    speedBonusFeet = 5,
                    savingThrowBonus = 1,
                    attackBonus = 1,
                    criticalThreshold = 19,
                )
            ),
            features = listOf(
                SubclassFeatureGrantUi(
                    minimumClassLevel = 3,
                    feature = FeatureUi(
                        id = "subclass-grant-storm-burst",
                        name = "Storm Burst",
                        summary = "Release stored lightning.",
                        recovery = Recovery.SHORT_REST,
                        actionCost = ActionCost(bonusActions = 1),
                        turnGuideEligible = true,
                    ),
                    useScaling = SubclassUseScalingUi.FIXED,
                    fixedUses = 2,
                ),
                SubclassFeatureGrantUi(
                    minimumClassLevel = 3,
                    feature = FeatureUi(
                        id = "subclass-grant-storm-step",
                        name = "Storm Step",
                        summary = "Ride the wind without a usage limit.",
                        actionCost = ActionCost(actions = 1),
                        turnGuideEligible = true,
                    ),
                ),
            ),
            spells = listOf(
                SubclassSpellGrantUi(3, SpellUi("storm-spark", "Storm Spark", 1, "A custom storm spell."))
            ),
        )

        state.addCustomSubclass("Storm School", "Storm magic and speed.", mechanics)
        assertTrue(state.creationSubclassOptions().any { it.name == "Storm School" && it.local })
        state.finishCreate()

        val created = assertNotNull(state.selectedCharacter)
        val feature = created.features.single { it.name == "Storm Burst" }
        assertEquals(2, feature.remaining)
        assertEquals(2, feature.maximum)
        assertEquals(Recovery.SHORT_REST, feature.recovery)
        assertEquals(35, created.speedFeet)
        assertEquals(19, created.criticalHitThreshold)
        assertTrue(created.spells.any { it.name == "Storm Spark" })
        assertTrue(SuggestedTurnPlanner.build(created, null).any { it.featureId == feature.id })
        val unlimitedFeature = created.features.single { it.name == "Storm Step" }
        assertTrue(unlimitedFeature.isActivatable())
        assertTrue(state.useFeature(unlimitedFeature.id, null))
        assertNull(state.selectedCharacter?.features?.single { it.id == unlimitedFeature.id }?.remaining)
        assertTrue(state.useFeature(feature.id))
        assertEquals(1, state.selectedCharacter?.features?.single { it.id == feature.id }?.remaining)
        assertTrue(state.takeRest(Recovery.SHORT_REST))
        assertEquals(2, state.selectedCharacter?.features?.single { it.id == feature.id }?.remaining)

        val restoredState = DndAppState(store)
        restoredState.beginCreate()
        restoredState.selectCreationClass("Wizard")
        assertTrue(restoredState.creationSubclassOptions().any { it.name == "Storm School" && it.local })
        val restored = restoredState.characters.single { it.name == "Storm Weaver" }
        assertEquals("Storm School", restored.subclass)
        assertEquals(created.subclassIdsByClass, restored.subclassIdsByClass)
        assertEquals(19, restored.criticalHitThreshold)
        assertTrue(restored.features.any { it.name == "Storm Burst" && it.turnGuideEligible })
    }

    @Test
    fun pf2eCreationDoesNotExposeFifthEditionSubclasses() {
        val state = DndAppState(SubclassTestStore())
        state.beginCreate()
        state.creation.ruleset = Ruleset.Pf2eRemaster

        assertTrue(state.creationSubclassOptions().isEmpty())
        assertFalse(state.creationSubclassRequired())
        assertNull(state.creationSubclassAdvisory())
    }
}

private class SubclassTestStore : LocalStateStore {
    private var state: String? = null

    override fun readState(): String? = state

    override fun writeState(value: String) {
        state = value
    }
}
