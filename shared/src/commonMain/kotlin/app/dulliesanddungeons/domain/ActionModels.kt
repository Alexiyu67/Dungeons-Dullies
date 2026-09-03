package app.dulliesanddungeons.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AutomationLevel { AUTOMATIC, GUIDED, INFORMATIONAL }

@Serializable
enum class ActionKind { MOVE, ATTACK, SPELL, FEATURE, INTERACT, CONDITION, CUSTOM }

@Serializable
enum class MovementMode { WALK, FLY, SWIM, CLIMB, BURROW, STEP }

@Serializable
data class ActionCost(
    val actions: Int = 0,
    val bonusActions: Int = 0,
    val reactions: Int = 0,
    val attacks: Int = 0,
    val objectInteractions: Int = 0,
    val pf2eActions: Int = 0,
    val resources: Map<String, Int> = emptyMap(),
)

@Serializable
data class AvailableAction(
    val id: String,
    val name: String,
    val kind: ActionKind,
    val automation: AutomationLevel,
    val cost: ActionCost = ActionCost(),
    val enabled: Boolean = true,
    val disabledReason: String? = null,
    val explanation: String,
    val prompts: List<ActionPrompt> = emptyList(),
    val roll: RollRequest? = null,
)

@Serializable
sealed interface ActionPrompt {
    val id: String

    @Serializable
    @SerialName("confirmation")
    data class Confirmation(override val id: String, val message: String) : ActionPrompt

    @Serializable
    @SerialName("choice")
    data class Choice(override val id: String, val label: String, val options: List<String>) : ActionPrompt

    @Serializable
    @SerialName("number")
    data class Number(
        override val id: String,
        val label: String,
        val minimum: Int,
        val maximum: Int,
    ) : ActionPrompt
}

@Serializable
enum class RollMode { NORMAL, ADVANTAGE, DISADVANTAGE }

@Serializable
enum class RollKind {
    GENERIC,
    ABILITY_CHECK,
    ATTACK,
    SAVING_THROW,
    DEATH_SAVE,
    DAMAGE,
    HEALING,
    HIT_POINT_GAIN,
    INITIATIVE,
    PF2E_CHECK,
}

@Serializable
data class RollModifierPart(
    val label: String,
    val value: Int,
    val source: EntityRef? = null,
)

@Serializable
enum class Pf2eDegreeOfSuccess { CRITICAL_FAILURE, FAILURE, SUCCESS, CRITICAL_SUCCESS }

@Serializable
enum class FiveEAttackOutcome { MISS, HIT, CRITICAL_HIT }

@Serializable
data class DiceExpression(
    val count: Int,
    val sides: Int,
    val modifier: Int = 0,
    val keepHighest: Int? = null,
    val keepLowest: Int? = null,
)

@Serializable
data class RollTarget(
    val value: Int,
    val label: String,
)

@Serializable
data class RollRequest(
    val label: String,
    val expression: DiceExpression,
    val mode: RollMode = RollMode.NORMAL,
    val kind: RollKind = RollKind.GENERIC,
    val modifierParts: List<RollModifierPart> = emptyList(),
    val target: RollTarget? = null,
) {
    val totalModifier: Int get() = expression.modifier + modifierParts.sumOf { it.value }
}

@Serializable
sealed interface RollOutcome {
    @Serializable
    @SerialName("five_e_attack")
    data class FiveEAttack(val result: FiveEAttackOutcome) : RollOutcome

    @Serializable
    @SerialName("pf2e_degree")
    data class Pf2eDegree(val result: Pf2eDegreeOfSuccess) : RollOutcome

    @Serializable
    @SerialName("success")
    data class Success(val succeeded: Boolean, val target: Int) : RollOutcome

    @Serializable
    @SerialName("text")
    data class Text(val value: String) : RollOutcome
}

@Serializable
data class DiceRoll(
    val request: RollRequest,
    val dice: List<Int>,
    val keptDice: List<Int>,
    val total: Int,
    val outcome: RollOutcome? = null,
)

@Serializable
enum class TurnPhase { ACTIVE, REVIEW, ENDED, DISCARDED }

@Serializable
enum class TurnSection { OVERVIEW, MOVE, ATTACK, SPELL, OTHER, REVIEW, DOWNED }

@Serializable
data class PendingTurnSelection(
    val section: TurnSection,
    val entityId: String,
    val promptAnswers: Map<String, String> = emptyMap(),
)

@Serializable
data class PendingResourceSpend(
    val resourceId: String,
    val amount: Int,
    val timing: SpendTiming,
)

@Serializable
data class PendingFeatureUse(
    val featureId: String,
    val spends: List<PendingResourceSpend>,
)

@Serializable
data class ProjectedCharacterDelta(
    val hitPointDelta: Int = 0,
    val temporaryHitPointDelta: Int = 0,
    val resourceDeltas: Map<String, Int> = emptyMap(),
    val addedConditionIds: List<String> = emptyList(),
    val removedConditionInstanceIds: List<String> = emptyList(),
)

@Serializable
enum class TurnGuideFlag {
    FLYING,
    FLIGHT_ACTIVATION_PAID,
    DASH_ACTIVE,
    ACTION_USED,
    ATTACK_ACTION_STARTED,
    BONUS_ACTION_USED,
    REACTION_USED,
}

@Serializable
data class TurnGuideSelections(
    val weaponId: String? = null,
    val spellId: String? = null,
    val featureId: String? = null,
    val attackOptionId: String? = null,
)

/** Serializable, resumable per-character draft. It is reducer-owned and safe to discard. */
@Serializable
data class TurnDraft(
    val draftId: String = "",
    val characterId: String = "",
    val ruleset: RulesetId,
    val phase: TurnPhase = TurnPhase.ACTIVE,
    val currentSection: TurnSection = TurnSection.OVERVIEW,
    val actionsRemaining: Int = 0,
    val bonusActionsRemaining: Int = 0,
    val reactionsRemaining: Int = 0,
    val attacksRemaining: Int = 0,
    val objectInteractionsRemaining: Int = 0,
    val pf2eActionsRemaining: Int = 0,
    val pf2eAttacksMade: Int = 0,
    val speedsFeet: Map<MovementMode, Int> = emptyMap(),
    val movementSpentFeet: Int = 0,
    val bonusMovementFeet: Int = 0,
    val selectedMovementMode: MovementMode? = null,
    val requestedMovementFeet: Int = 0,
    val resources: Map<String, Int> = emptyMap(),
    val pendingSelection: PendingTurnSelection? = null,
    val pendingFeature: PendingFeatureUse? = null,
    val projection: ProjectedCharacterDelta = ProjectedCharacterDelta(),
    val selectedRollMode: RollMode = RollMode.NORMAL,
    val guideFlags: Set<TurnGuideFlag> = emptySet(),
    val guideSelections: TurnGuideSelections = TurnGuideSelections(),
    val recordedActivityCount: Int = 0,
    val completedGuideStepIds: Set<String> = emptySet(),
    val rollbackSnapshot: CharacterRollbackSnapshot? = null,
    val events: List<TurnEvent> = emptyList(),
) {
    val committedEvents: Int get() = events.size
}

@Serializable
sealed interface TurnCommand {
    @Serializable
    @SerialName("use_action")
    data class UseAction(val actionId: String, val cost: ActionCost) : TurnCommand

    @Serializable
    @SerialName("begin_feature")
    data class BeginFeature(
        val featureId: String,
        val cost: ActionCost,
        val spends: List<PendingResourceSpend> = emptyList(),
    ) : TurnCommand

    @Serializable
    @SerialName("resolve_feature")
    data class ResolveFeature(val success: Boolean, val hit: Boolean = success) : TurnCommand

    @Serializable
    @SerialName("move")
    data class Move(
        val mode: MovementMode,
        val distanceFeet: Int,
        val pf2eActionCost: Int = 1,
    ) : TurnCommand

    @Serializable
    @SerialName("grant_movement")
    data class GrantMovement(val sourceId: String, val amountFeet: Int, val cost: ActionCost) : TurnCommand

    @Serializable
    @SerialName("record_roll")
    data class RecordRoll(val roll: DiceRoll) : TurnCommand

    @Serializable
    @SerialName("apply_condition")
    data class ApplyCondition(val conditionId: String) : TurnCommand

    @Serializable
    @SerialName("select")
    data class Select(val selection: PendingTurnSelection?) : TurnCommand

    @Serializable
    @SerialName("review")
    data object Review : TurnCommand

    @Serializable
    @SerialName("end_turn")
    data class EndTurn(val confirmEmpty: Boolean = false) : TurnCommand

    @Serializable
    @SerialName("discard")
    data object Discard : TurnCommand
}

@Serializable
sealed interface TurnEvent {
    @Serializable
    @SerialName("action_used")
    data class ActionUsed(val actionId: String, val cost: ActionCost) : TurnEvent

    @Serializable
    @SerialName("feature_started")
    data class FeatureStarted(val featureId: String, val cost: ActionCost) : TurnEvent

    @Serializable
    @SerialName("feature_resolved")
    data class FeatureResolved(val featureId: String, val success: Boolean, val hit: Boolean) : TurnEvent

    @Serializable
    @SerialName("resource_changed")
    data class ResourceChanged(val resourceId: String, val amount: Int) : TurnEvent

    @Serializable
    @SerialName("moved")
    data class Moved(val mode: MovementMode, val distanceFeet: Int) : TurnEvent

    @Serializable
    @SerialName("movement_granted")
    data class MovementGranted(val sourceId: String, val amountFeet: Int, val cost: ActionCost) : TurnEvent

    @Serializable
    @SerialName("roll_recorded")
    data class RollRecorded(val roll: DiceRoll) : TurnEvent

    @Serializable
    @SerialName("condition_applied")
    data class ConditionApplied(val conditionId: String) : TurnEvent

    @Serializable
    @SerialName("condition_removed")
    data class ConditionRemoved(val conditionId: String) : TurnEvent

    @Serializable
    @SerialName("attack_made")
    data class AttackMade(
        val weaponId: String,
        val attackOptionId: String? = null,
        val sourceFeatureId: String? = null,
        val cost: ActionCost = ActionCost(),
    ) : TurnEvent

    @Serializable
    @SerialName("attack_resolved")
    data class AttackResolved(
        val weaponId: String,
        val outcome: AttackOutcomeRecord,
        val attackOptionId: String? = null,
        val sourceFeatureId: String? = null,
    ) : TurnEvent

    @Serializable
    @SerialName("hit_points_changed")
    data class HitPointsChanged(
        val kind: HitPointChangeKind,
        val amount: Int,
        val effectiveHitPointChange: Int,
        val hitPointsBefore: Int,
        val hitPointsAfter: Int,
        val temporaryHitPointsBefore: Int = 0,
        val temporaryHitPointsAfter: Int = 0,
        val critical: Boolean = false,
        val wentDown: Boolean = false,
    ) : TurnEvent

    @Serializable
    @SerialName("turn_ended")
    data object TurnEnded : TurnEvent
}

@Serializable
enum class AttackOutcomeRecord { MISS, HIT, CRITICAL }

@Serializable
enum class HitPointChangeKind { DAMAGE, HEALING }

@Serializable
enum class TurnRejection {
    TURN_ALREADY_ENDED,
    TURN_NOT_ACTIVE,
    WRONG_RULESET_COST,
    INSUFFICIENT_ACTIONS,
    INSUFFICIENT_BONUS_ACTIONS,
    INSUFFICIENT_REACTIONS,
    INSUFFICIENT_ATTACKS,
    INSUFFICIENT_OBJECT_INTERACTIONS,
    INSUFFICIENT_PF2E_ACTIONS,
    INSUFFICIENT_RESOURCE,
    MOVEMENT_MODE_UNAVAILABLE,
    INVALID_MOVEMENT_INCREMENT,
    MOVEMENT_EXHAUSTED,
    PENDING_FEATURE_EXISTS,
    NO_PENDING_FEATURE,
    CONFIRM_EMPTY_TURN,
}

@Serializable
data class TurnTransition(
    val previous: TurnDraft,
    val current: TurnDraft,
    val events: List<TurnEvent> = emptyList(),
    val rejection: TurnRejection? = null,
) {
    val accepted: Boolean get() = rejection == null
}
