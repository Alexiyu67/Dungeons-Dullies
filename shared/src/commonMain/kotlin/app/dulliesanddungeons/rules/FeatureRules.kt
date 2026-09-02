package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.FeatureActivationDefinition
import app.dulliesanddungeons.domain.FeatureDefinition
import app.dulliesanddungeons.domain.FeatureTrigger
import app.dulliesanddungeons.domain.LevelSource
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.RecoveryAmount
import app.dulliesanddungeons.domain.RecoveryRule
import app.dulliesanddungeons.domain.ResourceDefinition
import app.dulliesanddungeons.domain.ResourcePool
import app.dulliesanddungeons.domain.ResourceSpendDefinition
import app.dulliesanddungeons.domain.RollFormula
import app.dulliesanddungeons.domain.RulePredicate
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.ScalingRule
import app.dulliesanddungeons.domain.ScalingStep
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterState

data class ScalingContext(
    val totalLevel: Int,
    val classLevels: Map<String, Int>,
    val abilities: Map<Ability, Int>,
    val proficiencyBonus: Int,
)

fun ScalingRule.evaluate(context: ScalingContext): Int = when (this) {
    is ScalingRule.Constant -> value
    is ScalingRule.TotalLevel -> context.totalLevel * multiplier + offset
    is ScalingRule.ClassLevel -> context.classLevels.getOrDefault(classId, 0) * multiplier + offset
    is ScalingRule.AbilityModifier -> DerivedStatRules.abilityModifier(context.abilities.getOrDefault(ability, 10))
    ScalingRule.ProficiencyBonus -> context.proficiencyBonus
    is ScalingRule.Sum -> terms.sumOf { it.evaluate(context) }
    is ScalingRule.StepTable -> {
        val level = when (val source = levelSource) {
            LevelSource.Total -> context.totalLevel
            is LevelSource.Class -> context.classLevels.getOrDefault(source.classId, 0)
        }
        steps.filter { level >= it.minimumLevel }.maxByOrNull { it.minimumLevel }?.value ?: 0
    }
}

object ResourceRules {
    fun create(definition: ResourceDefinition, context: ScalingContext): ResourcePool {
        val maximum = definition.maximum.evaluate(context).coerceAtLeast(0)
        return ResourcePool(definition.id, definition.label, maximum, maximum, definition.recoveryRules)
    }

    fun recover(
        pool: ResourcePool,
        trigger: Recovery,
        build: CharacterBuild? = null,
        state: CharacterState? = null,
    ): ResourcePool {
        var current = pool.current
        pool.recoveryRules.filter { rule ->
            rule.trigger == trigger && when (rule.predicate) {
                RulePredicate.Always -> true
                else -> build?.let { rule.predicate.matches(it, state) } == true
            }
        }.forEach { rule ->
            current = when (val amount = rule.amount) {
                RecoveryAmount.Full -> pool.maximum
                is RecoveryAmount.Fixed -> (current + amount.amount).coerceAtMost(pool.maximum)
            }
        }
        return pool.copy(current = current.coerceIn(0, pool.maximum))
    }

    /** Preserve spent uses when a level change alters a resource maximum. */
    fun rescaleMaximum(pool: ResourcePool, newMaximum: Int): ResourcePool {
        require(newMaximum >= 0)
        val spent = (pool.maximum - pool.current).coerceAtLeast(0)
        return pool.copy(
            current = (newMaximum - spent).coerceIn(0, newMaximum),
            maximum = newMaximum,
        )
    }
}

/** Audited SRD definitions used as rule-engine fixtures, not localized rules prose. */
object FifthEditionFeatureRules {
    fun secondWind(ruleset: RulesetId): FeatureDefinition {
        require(ruleset.isFiveEdition)
        val maximum = if (ruleset == RulesetId.FIFTH_EDITION_2014) {
            ScalingRule.Constant(1)
        } else {
            ScalingRule.StepTable(
                LevelSource.Class("fighter"),
                listOf(
                    ScalingStep(1, 2),
                    ScalingStep(4, 3),
                    ScalingStep(10, 4),
                ),
            )
        }
        val recovery = if (ruleset == RulesetId.FIFTH_EDITION_2014) {
            listOf(
                RecoveryRule(Recovery.SHORT_REST, RecoveryAmount.Full),
                RecoveryRule(Recovery.LONG_REST, RecoveryAmount.Full),
            )
        } else {
            listOf(
                RecoveryRule(Recovery.SHORT_REST, RecoveryAmount.Fixed(1)),
                RecoveryRule(Recovery.LONG_REST, RecoveryAmount.Full),
            )
        }
        return FeatureDefinition(
            id = "second_wind",
            name = "Second Wind",
            ruleset = ruleset,
            predicate = RulePredicate.HasClass("fighter"),
            groupId = "fighter_resources",
            resource = ResourceDefinition("second_wind", "Second Wind", maximum, recovery),
            activations = listOf(
                FeatureActivationDefinition(
                    id = "second_wind.heal",
                    name = "Second Wind",
                    trigger = FeatureTrigger.MANUAL,
                    actionCost = ActionCost(bonusActions = 1),
                    resourceSpends = listOf(ResourceSpendDefinition("second_wind")),
                    roll = RollFormula(
                        dice = listOf(DiceExpression(1, 10)),
                        modifier = ScalingRule.ClassLevel("fighter"),
                    ),
                ),
            ),
        )
    }
}
