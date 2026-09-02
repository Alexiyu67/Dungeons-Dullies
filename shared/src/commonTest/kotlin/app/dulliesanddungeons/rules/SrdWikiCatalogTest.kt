package app.dulliesanddungeons.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SrdWikiCatalogTest {
    @Test
    fun revisionsHaveAuditedPlayerFacingCoverage() {
        val old = SrdWikiCatalog.forRevision(SrdWikiRevision.SRD_5_1)
        val current = SrdWikiCatalog.forRevision(SrdWikiRevision.SRD_5_2_1)

        assertEquals(385, old.size)
        assertEquals(419, current.size)
        assertEquals(317, old.count { it.kind == SrdWikiKind.CREATURE })
        assertEquals(330, current.count { it.kind == SrdWikiKind.CREATURE })
        assertEquals(old.size, old.distinctBy { it.id }.size)
        assertEquals(current.size, current.distinctBy { it.id }.size)
    }

    @Test
    fun schoolsAndGolemsAreBilingualAndPlayerSafe() {
        SrdWikiRevision.entries.forEach { revision ->
            val entries = SrdWikiCatalog.forRevision(revision)
            assertEquals(8, entries.count { it.id.startsWith("school.") })
            assertTrue(entries.any { it.id == "school.abjuration" && it.de.name == "Bannmagie" })
            assertTrue(entries.any { it.id == "school.conjuration" && it.de.name == "Beschwörung" })

            listOf("Flesh Golem", "Iron Golem", "Stone Golem").forEach { name ->
                val golem = entries.single { it.en.name == name }
                assertEquals(SrdWikiKind.CREATURE, golem.kind)
                assertTrue(golem.metadata.endsWith("|Construct"))
                assertTrue(golem.en.beginnerTip.contains("without revealing its stat block"))
                assertTrue(golem.de.name.endsWith("golem"))
            }
        }
    }

    @Test
    fun everyEntryHasCompleteLocaleText() {
        assertTrue(SrdWikiCatalog.entries.all { entry ->
            entry.en.name.isNotBlank() && entry.en.summary.isNotBlank() && entry.en.beginnerTip.isNotBlank() &&
                entry.de.name.isNotBlank() && entry.de.summary.isNotBlank() && entry.de.beginnerTip.isNotBlank()
        })
    }
}
