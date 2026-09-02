package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.ActionKind
import app.dulliesanddungeons.domain.AncestryDefinition
import app.dulliesanddungeons.domain.AutomationLevel
import app.dulliesanddungeons.domain.AvailableAction
import app.dulliesanddungeons.domain.ClassDefinition
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.RollRequest
import app.dulliesanddungeons.domain.RuleDefinition
import app.dulliesanddungeons.domain.RulePredicate
import app.dulliesanddungeons.domain.RulesetId

interface RulesCatalog {
    fun classById(ruleset: RulesetId, id: String): ClassDefinition?
    fun ancestryById(ruleset: RulesetId, id: String): AncestryDefinition?
    fun ruleById(ruleset: RulesetId, id: String): RuleDefinition?
    fun classes(ruleset: RulesetId): List<ClassDefinition>
    fun ancestries(ruleset: RulesetId): List<AncestryDefinition>
    fun rules(ruleset: RulesetId): List<RuleDefinition>
}

class InMemoryRulesCatalog(
    classDefinitions: Iterable<ClassDefinition>,
    ancestryDefinitions: Iterable<AncestryDefinition>,
    ruleDefinitions: Iterable<RuleDefinition>,
) : RulesCatalog {
    private val classesByKey = classDefinitions.associateBy { it.ruleset to it.id }
    private val ancestriesByKey = ancestryDefinitions.associateBy { it.ruleset to it.id }
    private val rulesByKey = ruleDefinitions.associateBy { it.ruleset to it.id }

    init {
        require(classesByKey.size == classDefinitions.count()) { "Duplicate class IDs in a ruleset" }
        require(ancestriesByKey.size == ancestryDefinitions.count()) { "Duplicate ancestry IDs in a ruleset" }
        require(rulesByKey.size == ruleDefinitions.count()) { "Duplicate rule IDs in a ruleset" }
    }

    override fun classById(ruleset: RulesetId, id: String) = classesByKey[ruleset to id]
    override fun ancestryById(ruleset: RulesetId, id: String) = ancestriesByKey[ruleset to id]
    override fun ruleById(ruleset: RulesetId, id: String) = rulesByKey[ruleset to id]
    override fun classes(ruleset: RulesetId) = classesByKey.values.filter { it.ruleset == ruleset }.sortedBy { it.name }
    override fun ancestries(ruleset: RulesetId) =
        ancestriesByKey.values.filter { it.ruleset == ruleset }.sortedBy { it.name }
    override fun rules(ruleset: RulesetId) = rulesByKey.values.filter { it.ruleset == ruleset }.sortedBy { it.name }
}

/**
 * Tiny fixtures for previews and integration tests. Production compendia are supplied by compiled content packs.
 */
object SeededSampleCatalog {
    fun create(): RulesCatalog {
        val fifthEditions = listOf(RulesetId.FIFTH_EDITION_2014, RulesetId.FIFTH_EDITION_2024)
        val classes = fifthEditions.flatMap { ruleset ->
            listOf(
                ClassDefinition(
                    id = "fighter",
                    name = "Fighter",
                    ruleset = ruleset,
                    hitDieSides = 10,
                    primaryAbilities = setOf(Ability.STRENGTH, Ability.DEXTERITY),
                    savingThrowAbilities = setOf(Ability.STRENGTH, Ability.CONSTITUTION),
                    beginnerComplexity = 1,
                    featureIdsByLevel = mapOf(2 to listOf("action_surge"), 5 to listOf("extra_attack")),
                ),
                ClassDefinition(
                    id = "wizard",
                    name = "Wizard",
                    ruleset = ruleset,
                    hitDieSides = 6,
                    primaryAbilities = setOf(Ability.INTELLIGENCE),
                    savingThrowAbilities = setOf(Ability.INTELLIGENCE, Ability.WISDOM),
                    beginnerComplexity = 3,
                    featureIdsByLevel = mapOf(1 to listOf("spellcasting")),
                ),
            )
        } + ClassDefinition(
            id = "fighter",
            name = "Fighter",
            ruleset = RulesetId.PF2E_REMASTER,
            hitDieSides = 10,
            primaryAbilities = setOf(Ability.STRENGTH, Ability.DEXTERITY),
            savingThrowAbilities = setOf(Ability.CONSTITUTION),
            beginnerComplexity = 1,
        )
        val ancestries = fifthEditions.map { ruleset ->
            AncestryDefinition(
                id = "human",
                name = "Human",
                ruleset = ruleset,
                baseSpeedsFeet = mapOf(MovementMode.WALK to 30),
                languageIds = setOf("common"),
            )
        } + listOf(
            AncestryDefinition(
                id = "sample_flying_ancestry",
                name = "Sample Flying Ancestry",
                ruleset = RulesetId.FIFTH_EDITION_2024,
                baseSpeedsFeet = mapOf(MovementMode.WALK to 30, MovementMode.FLY to 30),
                languageIds = setOf("common"),
            ),
            AncestryDefinition(
                id = "human",
                name = "Human",
                ruleset = RulesetId.PF2E_REMASTER,
                baseSpeedsFeet = mapOf(MovementMode.WALK to 25),
                languageIds = setOf("common"),
            ),
        )
        val rules = fifthEditions.flatMap { ruleset ->
            listOf(
                RuleDefinition(
                    id = "attack",
                    name = "Attack",
                    ruleset = ruleset,
                    action = AvailableAction(
                        id = "attack",
                        name = "Attack",
                        kind = ActionKind.ATTACK,
                        automation = AutomationLevel.GUIDED,
                        cost = ActionCost(actions = 1, attacks = 1),
                        explanation = "Roll against the target; the DM confirms whether it hits.",
                        roll = RollRequest("Attack", DiceExpression(1, 20)),
                    ),
                    beginnerExplanation = "Use a weapon or unarmed strike against a target.",
                ),
                RuleDefinition(
                    id = "action_surge",
                    name = "Action Surge",
                    ruleset = ruleset,
                    predicate = RulePredicate.HasClass("fighter", 2),
                    action = AvailableAction(
                        id = "action_surge",
                        name = "Action Surge",
                        kind = ActionKind.FEATURE,
                        automation = AutomationLevel.AUTOMATIC,
                        cost = ActionCost(resources = mapOf("action_surge" to 1)),
                        explanation = "Regain one action this turn.",
                    ),
                    beginnerExplanation = "A Fighter can push beyond normal limits for another action.",
                ),
            )
        } + RuleDefinition(
            id = "strike",
            name = "Strike",
            ruleset = RulesetId.PF2E_REMASTER,
            action = AvailableAction(
                id = "strike",
                name = "Strike",
                kind = ActionKind.ATTACK,
                automation = AutomationLevel.GUIDED,
                cost = ActionCost(pf2eActions = 1),
                explanation = "Spend one action and apply the current multiple attack penalty.",
                roll = RollRequest("Strike", DiceExpression(1, 20)),
            ),
            beginnerExplanation = "Attack with a weapon or unarmed attack.",
        )
        return InMemoryRulesCatalog(classes, ancestries, rules)
    }
}
