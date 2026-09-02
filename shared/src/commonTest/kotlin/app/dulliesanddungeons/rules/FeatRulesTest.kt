package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.ClassLevel
import app.dulliesanddungeons.domain.FeatCategory
import app.dulliesanddungeons.domain.FeatDefinition
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.RulePredicate
import app.dulliesanddungeons.domain.RulesetId
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatRulesTest {
    @Test
    fun recommendationsExcludeIneligibleFeatsAndExplainBasicMatches() {
        val definitions = listOf(
            FeatDefinition(
                "heavy_armor_master",
                "Heavy Armor Master",
                RulesetId.FIFTH_EDITION_2024,
                FeatCategory.GENERAL,
                prerequisite = RulePredicate.AbilityAtLeast(Ability.STRENGTH, 13),
                recommendationTags = setOf("fighter", "strength"),
            ),
            FeatDefinition(
                "war_caster",
                "War Caster",
                RulesetId.FIFTH_EDITION_2024,
                FeatCategory.GENERAL,
                prerequisite = RulePredicate.HasFeature("spellcasting"),
                recommendationTags = setOf("wizard"),
            ),
        )

        val recommendations = FeatRules.recommendations(build(), definitions)

        assertEquals(listOf("heavy_armor_master"), recommendations.map { it.featId })
        assertEquals(listOf("matches_class", "matches_strong_ability"), recommendations.single().reasonKeys)
    }

    private fun build() = CharacterBuild(
        id = "fighter",
        name = "Fighter",
        ruleset = RulesetId.FIFTH_EDITION_2024,
        rules = FiveEBuildData(
            ancestryId = "human",
            backgroundId = "guard",
            classes = listOf(ClassLevel("fighter", 4)),
            abilities = Ability.entries.associateWith { if (it == Ability.STRENGTH) 18 else 10 },
        ),
    )
}
