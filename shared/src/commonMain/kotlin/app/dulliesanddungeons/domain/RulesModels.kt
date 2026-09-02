package app.dulliesanddungeons.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RulePredicate {
    @Serializable
    @SerialName("always")
    data object Always : RulePredicate

    @Serializable
    @SerialName("minimum_level")
    data class MinimumLevel(val level: Int) : RulePredicate

    @Serializable
    @SerialName("has_class")
    data class HasClass(val classId: String, val minimumLevels: Int = 1) : RulePredicate

    @Serializable
    @SerialName("has_subclass")
    data class HasSubclass(val subclassId: String, val minimumClassLevels: Int = 1) : RulePredicate

    @Serializable
    @SerialName("has_feature")
    data class HasFeature(val featureId: String) : RulePredicate

    @Serializable
    @SerialName("has_feat")
    data class HasFeat(val featId: String) : RulePredicate

    @Serializable
    @SerialName("ability_at_least")
    data class AbilityAtLeast(val ability: Ability, val score: Int) : RulePredicate

    @Serializable
    @SerialName("has_equipment")
    data class HasEquipment(val itemId: String, val mustBeEquipped: Boolean = false) : RulePredicate

    @Serializable
    @SerialName("all")
    data class All(val predicates: List<RulePredicate>) : RulePredicate

    @Serializable
    @SerialName("any")
    data class Any(val predicates: List<RulePredicate>) : RulePredicate

    @Serializable
    @SerialName("not")
    data class Not(val predicate: RulePredicate) : RulePredicate
}

@Serializable
enum class ModifierType { UNTYPED, CIRCUMSTANCE, ITEM, STATUS, PROFICIENCY }

@Serializable
enum class StatisticId {
    ARMOR_CLASS,
    INITIATIVE,
    SPEED,
    HIT_POINT_MAXIMUM,
    D20_TEST,
    ABILITY_CHECK,
    ATTACK_ROLL,
    SAVING_THROW,
    SPELL_SAVE_DC,
}

@Serializable
sealed interface RuleEffect {
    @Serializable
    @SerialName("modifier")
    data class Modifier(
        val statistic: String,
        val amount: Int,
        val type: ModifierType = ModifierType.UNTYPED,
    ) : RuleEffect

    @Serializable
    @SerialName("typed_modifier")
    data class TypedModifier(
        val statistic: StatisticId,
        val amount: ScalingRule,
        val type: ModifierType = ModifierType.UNTYPED,
    ) : RuleEffect

    @Serializable
    @SerialName("grant_action")
    data class GrantAction(val actionId: String) : RuleEffect

    @Serializable
    @SerialName("grant_speed")
    data class GrantSpeed(
        val mode: MovementMode,
        val feet: ScalingRule,
        val activation: FeatureActivationDefinition? = null,
    ) : RuleEffect

    @Serializable
    @SerialName("resource_delta")
    data class ResourceDelta(val resourceId: String, val amount: ScalingRule) : RuleEffect

    @Serializable
    @SerialName("set_roll_mode")
    data class SetRollMode(val rollKinds: Set<RollKind>, val mode: RollMode) : RuleEffect
}

@Serializable
sealed interface ScalingRule {
    @Serializable
    @SerialName("constant")
    data class Constant(val value: Int) : ScalingRule

    @Serializable
    @SerialName("total_level")
    data class TotalLevel(val multiplier: Int = 1, val offset: Int = 0) : ScalingRule

    @Serializable
    @SerialName("class_level")
    data class ClassLevel(val classId: String, val multiplier: Int = 1, val offset: Int = 0) : ScalingRule

    @Serializable
    @SerialName("ability_modifier")
    data class AbilityModifier(val ability: Ability) : ScalingRule

    @Serializable
    @SerialName("proficiency_bonus")
    data object ProficiencyBonus : ScalingRule

    @Serializable
    @SerialName("sum")
    data class Sum(val terms: List<ScalingRule>) : ScalingRule

    @Serializable
    @SerialName("step_table")
    data class StepTable(
        val levelSource: LevelSource,
        val steps: List<ScalingStep>,
    ) : ScalingRule
}

@Serializable
sealed interface LevelSource {
    @Serializable
    @SerialName("total")
    data object Total : LevelSource

    @Serializable
    @SerialName("class")
    data class Class(val classId: String) : LevelSource
}

@Serializable
data class ScalingStep(val minimumLevel: Int, val value: Int)

@Serializable
sealed interface RecoveryAmount {
    @Serializable
    @SerialName("full")
    data object Full : RecoveryAmount

    @Serializable
    @SerialName("fixed")
    data class Fixed(val amount: Int) : RecoveryAmount
}

@Serializable
data class RecoveryRule(
    val trigger: Recovery,
    val amount: RecoveryAmount,
    val predicate: RulePredicate = RulePredicate.Always,
)

@Serializable
enum class FeatureTrigger {
    MANUAL,
    BEFORE_ROLL,
    AFTER_ROLL,
    ON_HIT,
    ON_MISS,
    ON_DAMAGE,
    ON_FAILED_CHECK,
    ON_FAILED_SAVE,
    START_OF_TURN,
    END_OF_TURN,
    REACTION_TRIGGER,
}

@Serializable
enum class SpendTiming { ON_DECLARE, ON_HIT, ON_SUCCESS, ON_FAILURE, REFUND_ON_FAILURE }

@Serializable
data class ResourceSpendDefinition(
    val resourceId: String,
    val amount: ScalingRule = ScalingRule.Constant(1),
    val timing: SpendTiming = SpendTiming.ON_DECLARE,
)

@Serializable
data class ResourceDefinition(
    val id: String,
    val label: String,
    val maximum: ScalingRule,
    val recoveryRules: List<RecoveryRule>,
)

@Serializable
data class FeatureActivationDefinition(
    val id: String,
    val name: String,
    val trigger: FeatureTrigger = FeatureTrigger.MANUAL,
    val actionCost: ActionCost = ActionCost(),
    val resourceSpends: List<ResourceSpendDefinition> = emptyList(),
    val roll: RollFormula? = null,
    val effects: List<RuleEffect> = emptyList(),
    val prompts: List<ActionPrompt> = emptyList(),
)

@Serializable
data class FeatureDefinition(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    val predicate: RulePredicate = RulePredicate.Always,
    val groupId: String? = null,
    val resource: ResourceDefinition? = null,
    val activations: List<FeatureActivationDefinition> = emptyList(),
    val source: EntityRef? = null,
)

@Serializable
data class RollFormula(
    val dice: List<DiceExpression> = emptyList(),
    val modifier: ScalingRule = ScalingRule.Constant(0),
)

@Serializable
enum class ConditionStacking { BOOLEAN, VALUED, MULTIPLE_SOURCES }

@Serializable
data class ConditionDefinition(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    val stacking: ConditionStacking = ConditionStacking.BOOLEAN,
    val maximumIntensity: Int = 1,
    val iconKey: String,
    val effects: List<RuleEffect> = emptyList(),
    val derived: Boolean = false,
    val source: EntityRef? = null,
)

@Serializable
enum class ArmorCategory { LIGHT, MEDIUM, HEAVY, SHIELD, UNARMORED }

@Serializable
data class ArmorDefinition(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    val category: ArmorCategory,
    val baseArmorClass: Int,
    val dexterityCap: Int? = null,
    val strengthRequirement: Int? = null,
    val stealthDisadvantage: Boolean = false,
    val donSeconds: Int,
    val doffSeconds: Int,
    val requiresTraining: Boolean = true,
    /** Proficiency/training category checked while worn or wielded. */
    val trainingId: String? = null,
    val attunementRequired: Boolean = false,
    /** Portion of AC that is inactive until an item is attuned. */
    val magicBonus: Int = 0,
    val source: EntityRef? = null,
)

@Serializable
enum class FeatCategory { FIVE_E_2014, ORIGIN, GENERAL, FIGHTING_STYLE, EPIC_BOON, PF2E_ANCESTRY, PF2E_CLASS, PF2E_SKILL, PF2E_GENERAL, PF2E_ARCHETYPE }

@Serializable
data class FeatDefinition(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    val category: FeatCategory,
    val prerequisite: RulePredicate = RulePredicate.Always,
    val repeatable: Boolean = false,
    val recommendationTags: Set<String> = emptySet(),
    val source: EntityRef? = null,
)

@Serializable
data class FeatRecommendation(
    val featId: String,
    val score: Int,
    /** Stable, localizable reason keys; the UI decides whether to reveal them. */
    val reasonKeys: List<String> = emptyList(),
)

@Serializable
data class RuleDefinition(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    val predicate: RulePredicate = RulePredicate.Always,
    val effects: List<RuleEffect> = emptyList(),
    val action: AvailableAction? = null,
    val beginnerExplanation: String,
    val source: EntityRef? = null,
)

@Serializable
data class ClassDefinition(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    val hitDieSides: Int,
    val primaryAbilities: Set<Ability>,
    val savingThrowAbilities: Set<Ability>,
    val beginnerComplexity: Int,
    val featureIdsByLevel: Map<Int, List<String>> = emptyMap(),
    val multiclassPrerequisite: RulePredicate = RulePredicate.Always,
    val multiclassProficiencyIds: Set<String> = emptySet(),
)

@Serializable
data class SubclassDefinition(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    val classId: String,
    val selectionLevel: Int,
    val featureIdsByLevel: Map<Int, List<String>> = emptyMap(),
    val source: EntityRef? = null,
)

@Serializable
data class AncestryDefinition(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    val baseSpeedsFeet: Map<MovementMode, Int>,
    val languageIds: Set<String> = emptySet(),
    val featureIds: Set<String> = emptySet(),
)

@Serializable
data class ConversionPlan(
    val sourceCharacterId: String,
    val targetCharacterId: String,
    val targetRuleset: RulesetId,
    val preservedFields: Set<String>,
    val mappings: List<ConversionMapping>,
    val recommendations: List<ConversionRecommendation> = emptyList(),
    val warnings: List<String> = emptyList(),
    val unresolvedChoiceIds: Set<String> = emptySet(),
)

@Serializable
data class ConversionMapping(
    val sourceId: String,
    val targetId: String?,
    val kind: MappingKind,
    val explanation: String,
)

@Serializable
enum class MappingKind { PRESERVED, EXACT, REPLACED, REMOVED, RESELECT_REQUIRED }

@Serializable
data class ConversionRecommendation(
    val targetId: String,
    val label: String,
    val reason: String,
)

@Serializable
data class SearchDocument(
    val id: String,
    val ruleset: RulesetId,
    val locale: LocaleId,
    val title: String,
    val body: String,
    val aliases: Set<String> = emptySet(),
    val command: SearchCommand? = null,
    val source: EntityRef? = null,
)

@Serializable
sealed interface SearchCommand {
    @Serializable
    @SerialName("open")
    data class Open(val entityId: String) : SearchCommand

    @Serializable
    @SerialName("roll")
    data class Roll(val request: RollRequest) : SearchCommand

    @Serializable
    @SerialName("use_action")
    data class UseAction(val actionId: String) : SearchCommand

    @Serializable
    @SerialName("navigate")
    data class Navigate(val destination: String) : SearchCommand
}
