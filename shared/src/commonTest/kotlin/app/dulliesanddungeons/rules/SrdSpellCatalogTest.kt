package app.dulliesanddungeons.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SrdSpellCatalogTest {
    @Test
    fun classListsHaveAuditedCoverageCounts() {
        val expected = mapOf(
            SrdSpellRevision.SRD_5_1 to mapOf(
                SrdSpellClass.BARD to 112, SrdSpellClass.CLERIC to 105, SrdSpellClass.DRUID to 105,
                SrdSpellClass.PALADIN to 31, SrdSpellClass.RANGER to 37, SrdSpellClass.SORCERER to 120,
                SrdSpellClass.WARLOCK to 64, SrdSpellClass.WIZARD to 204,
            ),
            SrdSpellRevision.SRD_5_2_1 to mapOf(
                SrdSpellClass.BARD to 129, SrdSpellClass.CLERIC to 109, SrdSpellClass.DRUID to 124,
                SrdSpellClass.PALADIN to 38, SrdSpellClass.RANGER to 48, SrdSpellClass.SORCERER to 138,
                SrdSpellClass.WARLOCK to 72, SrdSpellClass.WIZARD to 217,
            ),
        )
        expected.forEach { (revision, counts) ->
            counts.forEach { (spellClass, count) ->
                assertEquals(count, SrdSpellCatalog.forClass(revision, spellClass).size, "$revision $spellClass")
            }
        }
        assertEquals(319, SrdSpellCatalog.entries.count { it.revision == SrdSpellRevision.SRD_5_1 })
        assertEquals(338, SrdSpellCatalog.entries.count { it.revision == SrdSpellRevision.SRD_5_2_1 })
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

    @Test
    fun concentrationDetailsContainExactCastingFieldsDurationAndEffect() {
        val details = SrdSpellDetailsCatalog.find(
            SrdSpellRevision.SRD_5_2_1,
            "spell.protection-from-evil-and-good",
        )!!

        assertEquals("Action", details.castingTime)
        assertEquals("Touch", details.range)
        assertTrue(details.components.contains("Holy Water worth 25+ GP"))
        assertEquals("Concentration up to 10 minutes", details.duration)
        assertEquals(100, details.durationRounds)
        assertTrue(details.effect.contains("Disadvantage on attack rolls"))
        assertTrue(details.effect.contains("Charmed or Frightened conditions"))
    }

    @Test
    fun everyConcentrationSpellHasGeneratedDetails() {
        SrdSpellCatalog.entries.filter { it.concentration }.forEach { spell ->
            val details = SrdSpellDetailsCatalog.find(spell.revision, spell.id)
            assertTrue(details != null, "Missing details for ${spell.revision}:${spell.id}")
            assertTrue(details!!.castingTime.isNotBlank(), spell.id)
            assertTrue(details.range.isNotBlank(), spell.id)
            assertTrue(details.components.isNotBlank(), spell.id)
            assertTrue(details.duration.isNotBlank(), spell.id)
            assertTrue(details.effect.isNotBlank(), spell.id)
            assertTrue(details.durationRounds > 0, spell.id)
        }
    }
}
