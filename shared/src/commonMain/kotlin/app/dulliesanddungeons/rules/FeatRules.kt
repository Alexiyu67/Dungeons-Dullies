package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.FeatDefinition
import app.dulliesanddungeons.domain.FeatRecommendation

object FeatRules {
    fun eligible(
        build: CharacterBuild,
        definitions: List<FeatDefinition>,
        state: CharacterState? = null,
    ): List<FeatDefinition> = definitions.filter { definition ->
        definition.ruleset == build.ruleset &&
            (definition.repeatable || definition.id !in build.featIds) &&
            definition.prerequisite.matches(build, state)
    }

    /** Deterministic, explainable ranking from intentionally basic character facts. */
    fun recommendations(
        build: CharacterBuild,
        definitions: List<FeatDefinition>,
        state: CharacterState? = null,
        limit: Int = 5,
    ): List<FeatRecommendation> {
        require(limit >= 0)
        val classIds = build.classes.mapTo(mutableSetOf()) { it.classId }
        val strongestAbilities = build.abilities.entries
            .sortedByDescending { it.value }
            .take(2)
            .mapTo(mutableSetOf()) { it.key.name.lowercase() }
        return eligible(build, definitions, state).map { feat ->
            val tags = feat.recommendationTags.mapTo(mutableSetOf()) { it.lowercase() }
            val reasons = buildList {
                if (tags.any { it in classIds }) add("matches_class")
                if (tags.any { it in strongestAbilities }) add("matches_strong_ability")
                if (tags.any { it in build.proficiencyIds }) add("matches_proficiency")
            }
            FeatRecommendation(feat.id, score = reasons.size, reasonKeys = reasons)
        }.sortedWith(compareByDescending<FeatRecommendation> { it.score }.thenBy { it.featId }).take(limit)
    }
}
