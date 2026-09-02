package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.AvailableAction
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.DerivedStats
import app.dulliesanddungeons.domain.RulePredicate
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.TurnDraft
import app.dulliesanddungeons.domain.ValidationIssue

interface RulesetPlugin {
    val id: RulesetId

    fun validate(build: CharacterBuild, state: CharacterState? = null): List<ValidationIssue>

    fun derive(build: CharacterBuild, state: CharacterState): DerivedStats

    fun availableActions(build: CharacterBuild, state: CharacterState): List<AvailableAction>

    fun startTurn(build: CharacterBuild, state: CharacterState): TurnDraft

    val turnReducer: TurnReducer
}

fun RulePredicate.matches(build: CharacterBuild, state: CharacterState? = null): Boolean = when (this) {
    RulePredicate.Always -> true
    is RulePredicate.MinimumLevel -> build.level >= level
    is RulePredicate.HasClass -> build.classes.any { it.classId == classId && it.levels >= minimumLevels }
    is RulePredicate.HasSubclass -> build.classes.any { it.subclassId == subclassId && it.levels >= minimumClassLevels }
    is RulePredicate.HasFeature -> featureId in build.featureIds || featureId in build.featIds
    is RulePredicate.HasFeat -> featId in build.featIds
    is RulePredicate.AbilityAtLeast -> build.abilities.getOrDefault(ability, 0) >= score
    is RulePredicate.HasEquipment -> state?.equipment?.any {
        (it.id == itemId || it.definitionId == itemId) && (!mustBeEquipped || it.equipped)
    } == true
    is RulePredicate.All -> predicates.all { it.matches(build, state) }
    is RulePredicate.Any -> predicates.any { it.matches(build, state) }
    is RulePredicate.Not -> !predicate.matches(build, state)
}
