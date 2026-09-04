package app.dulliesanddungeons.ui

import app.dulliesanddungeons.rules.SrdSpellCatalog
import app.dulliesanddungeons.rules.SrdSpellClass
import app.dulliesanddungeons.rules.SrdSpellRevision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreationSpellRulesTest {
    @Test
    fun levelOneFifthEditionLimitsMatchTheSelectedEditionAndClass() {
        val neutral = mapOf("INT" to 10, "WIS" to 10, "CHA" to 10)

        assertNull(CreationSpellRules.limits(Ruleset.Fifth2014, "Paladin", 1, neutral))
        assertEquals(
            CreationSpellLimits(SrdSpellClass.BARD, 2, 4, null, 1, "Known spells"),
            CreationSpellRules.limits(Ruleset.Fifth2014, "Bard", 1, neutral),
        )
        assertEquals(
            CreationSpellLimits(SrdSpellClass.WIZARD, 3, 6, 1, 1, "Spellbook"),
            CreationSpellRules.limits(Ruleset.Fifth2014, "Wizard", 1, neutral),
        )
        assertEquals(
            CreationSpellLimits(SrdSpellClass.WIZARD, 3, 6, 4, 1, "Spellbook"),
            CreationSpellRules.limits(Ruleset.Fifth2024, "Wizard", 1, neutral),
        )
        assertEquals(2, CreationSpellRules.limits(Ruleset.Fifth2024, "Paladin", 1, neutral)?.leveledSpellLimit)
        assertEquals(2, CreationSpellRules.limits(Ruleset.Fifth2024, "Ranger", 1, neutral)?.leveledSpellLimit)
        assertEquals(4, CreationSpellRules.limits(Ruleset.Fifth2024, "Paladin", 3, neutral)?.leveledSpellLimit)
        assertEquals(16, CreationSpellRules.limits(Ruleset.Fifth2024, "Cleric", 12, neutral)?.leveledSpellLimit)
        assertEquals(21, CreationSpellRules.limits(Ruleset.Fifth2024, "Wizard", 16, neutral)?.preparedLimit)
    }

    @Test
    fun nonCasterHidesSelectionUnlessTheExactAncestryGrantsMagic() {
        val state = DndAppState()
        state.beginCreate()
        state.selectCreationClass("Fighter")
        state.creation.ancestry = "Human"

        assertNull(state.creationSpellSelection())

        state.creation.ancestry = "Tiefling"
        val selection = assertNotNull(state.creationSpellSelection())
        assertTrue(selection.options.isEmpty())
        assertEquals(listOf("Thaumaturgy"), selection.fixed.map { it.name })
    }

    @Test
    fun creationUsesOnlyLegalClassSpellsAndEnforcesSeparateCounts() {
        val state = DndAppState()
        state.beginCreate()
        state.creation.name = "Rules Wizard"
        state.selectCreationClass("Wizard")

        val selection = assertNotNull(state.creationSpellSelection())
        assertEquals(3, selection.cantripLimit)
        assertEquals(6, selection.leveledSpellLimit)
        assertEquals(4, selection.preparedLimit)
        assertTrue(selection.options.all { it.level <= 1 })
        val legalIds = SrdSpellCatalog.entries.filter {
            it.revision == SrdSpellRevision.SRD_5_2_1 &&
                it.level <= 1 && SrdSpellClass.WIZARD in it.classes
        }.mapTo(mutableSetOf()) { it.id }
        assertEquals(legalIds, selection.options.mapTo(mutableSetOf()) { it.id })

        val cantrips = selection.options.filter { it.level == 0 }
        val leveled = selection.options.filter { it.level == 1 }
        cantrips.take(3).forEach { assertTrue(state.toggleCreationSpell(it.id)) }
        assertFalse(state.toggleCreationSpell(cantrips[3].id))
        leveled.take(6).forEach { assertTrue(state.toggleCreationSpell(it.id)) }
        assertFalse(state.toggleCreationSpell(leveled[6].id))
        assertTrue(state.creationSpellSelectionValid())

        state.completeRequiredCreationProficiencies()
        state.completeRequiredCreationGear()
        state.finishCreate()

        val created = assertNotNull(state.selectedCharacter)
        assertEquals(9, created.spells.count { it.sourceKind == SpellSourceKind.CLASS })
        assertEquals(3, created.spells.count { it.sourceKind == SpellSourceKind.CLASS && it.level == 0 })
        assertEquals(6, created.spells.count { it.sourceKind == SpellSourceKind.CLASS && it.level == 1 })
        assertEquals(4, created.spells.count { it.sourceKind == SpellSourceKind.CLASS && it.level == 1 && it.prepared })
    }

    @Test
    fun classlessImportedSpellsStayOutOfCreationButRemainAvailableInTheEditor() {
        val state = DndAppState()
        state.addPrivateEntry(
            PrivateEntryUi(
                id = "imported-any-spell",
                kind = "spell",
                name = "Imported Any Spell",
                mechanics = PrivateMechanicsUi(spell = PrivateSpellMechanicsUi(level = 1)),
                sourcePackId = "private.handbook",
                sourcePackVersion = "1.0.0",
            ),
        )
        state.beginCreate()
        state.selectCreationClass("Fighter")

        assertNull(state.creationSpellSelection())
        val fighter = state.characters.first { it.className == "Fighter" }
        assertTrue(state.editableSpellCatalog(fighter).any { it.name == "Imported Any Spell" })
    }
}
