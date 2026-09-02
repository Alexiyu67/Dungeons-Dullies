package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.RulesetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RulesCatalogTest {
    @Test
    fun seededCatalogKeepsRulesetIdsSeparate() {
        val catalog = SeededSampleCatalog.create()

        val oldFighter = assertNotNull(catalog.classById(RulesetId.FIFTH_EDITION_2014, "fighter"))
        val newFighter = assertNotNull(catalog.classById(RulesetId.FIFTH_EDITION_2024, "fighter"))
        val pf2eFighter = assertNotNull(catalog.classById(RulesetId.PF2E_REMASTER, "fighter"))

        assertEquals(RulesetId.FIFTH_EDITION_2014, oldFighter.ruleset)
        assertEquals(RulesetId.FIFTH_EDITION_2024, newFighter.ruleset)
        assertEquals(RulesetId.PF2E_REMASTER, pf2eFighter.ruleset)
        assertTrue(catalog.rules(RulesetId.FIFTH_EDITION_2024).any { it.id == "attack" })
    }

    @Test
    fun predicatesUseBuildFactsInsteadOfDisplayText() {
        val build = sampleBuild(level = 5)

        assertTrue(app.dulliesanddungeons.domain.RulePredicate.HasClass("fighter", 5).matches(build))
        val subclassBuild = build.copy(
            rules = (build.rules as app.dulliesanddungeons.domain.FiveEBuildData).copy(
                classes = listOf(app.dulliesanddungeons.domain.ClassLevel("fighter", 5, "champion"))
            )
        )
        assertTrue(
            app.dulliesanddungeons.domain.RulePredicate.HasSubclass("champion", 3).matches(subclassBuild)
        )
        assertTrue(app.dulliesanddungeons.domain.RulePredicate.MinimumLevel(5).matches(build))
    }
}
