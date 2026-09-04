package app.dulliesanddungeons.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharacterCreationUiLogicTest {
    @Test
    fun classSkillLimitPulseOnlyRejectsANewChoiceAtTheLimit() {
        assertTrue(shouldPulseClassSkillLimit(alreadySelected = false, selectedCount = 2, selectionLimit = 2))
        assertFalse(shouldPulseClassSkillLimit(alreadySelected = true, selectedCount = 2, selectionLimit = 2))
        assertFalse(shouldPulseClassSkillLimit(alreadySelected = false, selectedCount = 1, selectionLimit = 2))
    }

    @Test
    fun backgroundsConflictingWithClassSkillsAreFilteredButCurrentSelectionStaysVisible() {
        val soldier = BackgroundDefinitionUi(
            id = "background:soldier",
            englishName = "Soldier",
            germanName = "Soldat:in",
            grantedSkillIds = setOf("skill:athletics"),
        )
        val sage = BackgroundDefinitionUi(
            id = "background:sage",
            englishName = "Sage",
            germanName = "Gelehrte:r",
            grantedSkillIds = setOf("skill:arcana"),
        )

        assertEquals(
            listOf("background:sage"),
            filterCreationBackgrounds(listOf(soldier, sage), setOf("skill:athletics"), null).map { it.id },
        )
        assertEquals(
            listOf("background:soldier", "background:sage"),
            filterCreationBackgrounds(listOf(soldier, sage), setOf("skill:athletics"), soldier.id).map { it.id },
        )
    }

    @Test
    fun selectedSpellsAreGroupedByLevelThenAlphabetically() {
        val spells = listOf(
            SpellUi("shield", "Shield", 1, "Defense"),
            SpellUi("light", "Light", 0, "Utility"),
            SpellUi("magic-missile", "Magic Missile", 1, "Damage"),
            SpellUi("fire-bolt", "Fire Bolt", 0, "Damage"),
        )

        val grouped = groupCreationSpells(spells)

        assertEquals(listOf(0, 1), grouped.map { it.first })
        assertEquals(listOf("Fire Bolt", "Light"), grouped[0].second.map { it.name })
        assertEquals(listOf("Magic Missile", "Shield"), grouped[1].second.map { it.name })
    }

    @Test
    fun repeatedPackageWeaponsUseAQuantitySummary() {
        val fighter = startingGearPackages(Ruleset.Fifth2024, "Fighter").first { it.id == "fighter-a" }

        assertTrue("Javelin ×8" in fighter.summary())
        assertEquals(1, Regex("Javelin").findAll(fighter.summary()).count())
    }
}
