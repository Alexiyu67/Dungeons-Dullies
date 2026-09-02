package app.dulliesanddungeons.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SrdSpellCatalogTest {
    @Test
    fun classListsHaveAuditedCoverageCounts() {
        assertEquals(120, SrdSpellCatalog.forClass(SrdSpellRevision.SRD_5_1, SrdSpellClass.SORCERER).size)
        assertEquals(204, SrdSpellCatalog.forClass(SrdSpellRevision.SRD_5_1, SrdSpellClass.WIZARD).size)
        assertEquals(138, SrdSpellCatalog.forClass(SrdSpellRevision.SRD_5_2_1, SrdSpellClass.SORCERER).size)
        assertEquals(217, SrdSpellCatalog.forClass(SrdSpellRevision.SRD_5_2_1, SrdSpellClass.WIZARD).size)
    }

    @Test
    fun classListsAreLevelThenAlphabeticallySorted() {
        SrdSpellRevision.entries.forEach { revision ->
            SrdSpellClass.entries.forEach { spellClass ->
                val entries = SrdSpellCatalog.forClass(revision, spellClass)
                val sorted = entries.sortedWith(compareBy<SrdSpellCatalogEntry> { it.level }.thenBy { it.en.name.lowercase() }.thenBy { it.id })
                assertEquals(sorted.map { it.id }, entries.map { it.id })
            }
        }
    }

    @Test
    fun renamedSpellKeepsStableIdentityAndAlias() {
        val old = SrdSpellCatalog.find(SrdSpellRevision.SRD_5_1, "spell.feeblemind")
        val current = SrdSpellCatalog.find(SrdSpellRevision.SRD_5_2_1, "spell.feeblemind")

        assertEquals("Feeblemind", old?.en?.name)
        assertEquals("Befuddlement", current?.en?.name)
        assertTrue("feeblemind" in current!!.aliases)
    }

    @Test
    fun burningHandsCarriesSlotSpecificPreviewValues() {
        val spell = SrdSpellCatalog.find(SrdSpellRevision.SRD_5_2_1, "spell.burning-hands")!!

        assertEquals("3d6 fire", spell.castPreviews.first { it.slotLevel == 1 }.en)
        assertEquals("5d6 fire", spell.castPreviews.first { it.slotLevel == 3 }.en)
        assertEquals("5d6 Feuer", spell.castPreviews.first { it.slotLevel == 3 }.de)
    }

    @Test
    fun combatMetadataTracksEditionSpecificSavesAndSpellAttacks() {
        val oldAcidSplash = SrdSpellCombatCatalog.find(SrdSpellRevision.SRD_5_1, "spell.acid-splash")
        val currentAcidSplash = SrdSpellCombatCatalog.find(SrdSpellRevision.SRD_5_2_1, "spell.acid-splash")
        val currentFireBolt = SrdSpellCombatCatalog.find(SrdSpellRevision.SRD_5_2_1, "spell.fire-bolt")

        assertEquals(setOf("DEXTERITY"), oldAcidSplash.savingThrowAbilities)
        assertEquals(setOf("DEXTERITY"), currentAcidSplash.savingThrowAbilities)
        assertTrue(currentFireBolt.spellAttack)
    }

    @Test
    fun schoolNamesInsideSpellNamesAreNotTruncated() {
        assertEquals("Minor Illusion", SrdSpellCatalog.find(SrdSpellRevision.SRD_5_2_1, "spell.minor-illusion")?.en?.name)
        assertEquals("Programmed Illusion", SrdSpellCatalog.find(SrdSpellRevision.SRD_5_2_1, "spell.programmed-illusion")?.en?.name)
    }
}
