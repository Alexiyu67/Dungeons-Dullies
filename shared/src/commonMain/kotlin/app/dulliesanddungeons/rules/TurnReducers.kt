package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.PendingFeatureUse
import app.dulliesanddungeons.domain.PendingResourceSpend
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.SpendTiming
import app.dulliesanddungeons.domain.TurnCommand
import app.dulliesanddungeons.domain.TurnDraft
import app.dulliesanddungeons.domain.TurnEvent
import app.dulliesanddungeons.domain.TurnPhase
import app.dulliesanddungeons.domain.TurnRejection
import app.dulliesanddungeons.domain.TurnSection
import app.dulliesanddungeons.domain.TurnTransition

fun interface TurnReducer {
    fun reduce(state: TurnDraft, command: TurnCommand): TurnTransition
}

class FiveETurnReducer(private val edition: RulesetId) : TurnReducer {
    init {
        require(edition.isFiveEdition) { "FiveETurnReducer requires a fifth-edition ruleset" }
    }

    fun newTurn(
        speedsFeet: Map<MovementMode, Int>,
        attacksPerAction: Int = 1,
        resources: Map<String, Int> = emptyMap(),
        characterId: String = "",
        draftId: String = "",
    ): TurnDraft {
        require(speedsFeet.values.all { it >= 0 }) { "speeds cannot be negative" }
        require(attacksPerAction >= 1) { "attacksPerAction must be positive" }
        require(resources.values.all { it >= 0 }) { "resources cannot be negative" }
        return TurnDraft(
            draftId = draftId,
            characterId = characterId,
            ruleset = edition,
            actionsRemaining = 1,
            bonusActionsRemaining = 1,
            reactionsRemaining = 1,
            attacksRemaining = attacksPerAction,
            objectInteractionsRemaining = 1,
            speedsFeet = speedsFeet.filterValues { it > 0 },
            selectedMovementMode = when {
                speedsFeet.getOrDefault(MovementMode.FLY, 0) > 0 -> MovementMode.FLY
                speedsFeet.getOrDefault(MovementMode.WALK, 0) > 0 -> MovementMode.WALK
                else -> speedsFeet.entries.firstOrNull { it.value > 0 }?.key
            },
            resources = resources,
        )
    }

    override fun reduce(state: TurnDraft, command: TurnCommand): TurnTransition {
        state.ensureActive(edition)?.let { return it }
        return when (command) {
            is TurnCommand.UseAction -> useAction(state, command.actionId, command.cost)
            is TurnCommand.BeginFeature -> beginFeature(state, command.featureId, command.cost, command.spends)
            is TurnCommand.ResolveFeature -> resolveFeature(state, command.success, command.hit)
            is TurnCommand.Move -> move(state, command.mode, command.distanceFeet)
            is TurnCommand.GrantMovement -> grantMovement(state, command.sourceId, command.amountFeet, command.cost)
            is TurnCommand.RecordRoll -> state.commit(TurnEvent.RollRecorded(command.roll))
            is TurnCommand.ApplyCondition -> state.copy(
                projection = state.projection.copy(
                    addedConditionIds = state.projection.addedConditionIds + command.conditionId,
                ),
            ).commitFrom(state, TurnEvent.ConditionApplied(command.conditionId))
            is TurnCommand.Select -> TurnTransition(
                state,
                state.copy(
                    pendingSelection = command.selection,
                    currentSection = command.selection?.section ?: state.currentSection,
                ),
            )
            TurnCommand.Review -> TurnTransition(state, state.copy(phase = TurnPhase.REVIEW, currentSection = TurnSection.REVIEW))
            is TurnCommand.EndTurn -> end(state, command.confirmEmpty)
            TurnCommand.Discard -> TurnTransition(state, state.copy(phase = TurnPhase.DISCARDED))
        }
    }

    private fun useAction(state: TurnDraft, actionId: String, cost: ActionCost): TurnTransition {
        if (cost.pf2eActions != 0) return state.rejected(TurnRejection.WRONG_RULESET_COST)
        state.cannotPay(cost)?.let { return state.rejected(it) }
        return state.copyAfterPayment(cost).commitFrom(state, TurnEvent.ActionUsed(actionId, cost))
    }

    private fun move(state: TurnDraft, mode: MovementMode, distanceFeet: Int): TurnTransition {
        if (distanceFeet <= 0 || distanceFeet % 5 != 0) return state.rejected(TurnRejection.INVALID_MOVEMENT_INCREMENT)
        val speed = state.speedsFeet[mode] ?: return state.rejected(TurnRejection.MOVEMENT_MODE_UNAVAILABLE)
        if (state.movementSpentFeet + distanceFeet > speed + state.bonusMovementFeet) {
            return state.rejected(TurnRejection.MOVEMENT_EXHAUSTED)
        }
        return state.copy(
            movementSpentFeet = state.movementSpentFeet + distanceFeet,
            selectedMovementMode = mode,
        ).commitFrom(state, TurnEvent.Moved(mode, distanceFeet))
    }

    private fun grantMovement(state: TurnDraft, sourceId: String, amountFeet: Int, cost: ActionCost): TurnTransition {
        if (amountFeet <= 0 || amountFeet % 5 != 0) return state.rejected(TurnRejection.INVALID_MOVEMENT_INCREMENT)
        if (cost.pf2eActions != 0) return state.rejected(TurnRejection.WRONG_RULESET_COST)
        state.cannotPay(cost)?.let { return state.rejected(it) }
        return state.copyAfterPayment(cost).copy(
            bonusMovementFeet = state.bonusMovementFeet + amountFeet,
        ).commitFrom(state, TurnEvent.MovementGranted(sourceId, amountFeet, cost))
    }
}

class Pf2eTurnReducer : TurnReducer {
    fun newTurn(
        speedsFeet: Map<MovementMode, Int>,
        resources: Map<String, Int> = emptyMap(),
        characterId: String = "",
        draftId: String = "",
    ): TurnDraft {
        require(speedsFeet.values.all { it >= 0 }) { "speeds cannot be negative" }
        require(resources.values.all { it >= 0 }) { "resources cannot be negative" }
        return TurnDraft(
            draftId = draftId,
            characterId = characterId,
            ruleset = RulesetId.PF2E_REMASTER,
            reactionsRemaining = 1,
            pf2eActionsRemaining = 3,
            speedsFeet = speedsFeet.filterValues { it > 0 },
            selectedMovementMode = MovementMode.WALK.takeIf { speedsFeet.getOrDefault(it, 0) > 0 },
            resources = resources,
        )
    }

    override fun reduce(state: TurnDraft, command: TurnCommand): TurnTransition {
        state.ensureActive(RulesetId.PF2E_REMASTER)?.let { return it }
        return when (command) {
            is TurnCommand.UseAction -> useAction(state, command.actionId, command.cost)
            is TurnCommand.BeginFeature -> beginFeature(state, command.featureId, command.cost, command.spends)
            is TurnCommand.ResolveFeature -> resolveFeature(state, command.success, command.hit)
            is TurnCommand.Move -> move(state, command)
            is TurnCommand.GrantMovement -> state.rejected(TurnRejection.WRONG_RULESET_COST)
            is TurnCommand.RecordRoll -> state.commit(TurnEvent.RollRecorded(command.roll))
            is TurnCommand.ApplyCondition -> state.copy(
                projection = state.projection.copy(
                    addedConditionIds = state.projection.addedConditionIds + command.conditionId,
                ),
            ).commitFrom(state, TurnEvent.ConditionApplied(command.conditionId))
            is TurnCommand.Select -> TurnTransition(
                state,
                state.copy(
                    pendingSelection = command.selection,
                    currentSection = command.selection?.section ?: state.currentSection,
                ),
            )
            TurnCommand.Review -> TurnTransition(state, state.copy(phase = TurnPhase.REVIEW, currentSection = TurnSection.REVIEW))
            is TurnCommand.EndTurn -> end(state, command.confirmEmpty)
            TurnCommand.Discard -> TurnTransition(state, state.copy(phase = TurnPhase.DISCARDED))
        }
    }

    private fun useAction(state: TurnDraft, actionId: String, cost: ActionCost): TurnTransition {
        if (!cost.isPf2eCompatible()) return state.rejected(TurnRejection.WRONG_RULESET_COST)
        state.cannotPay(cost)?.let { return state.rejected(it) }
        return state.copyAfterPayment(cost).commitFrom(state, TurnEvent.ActionUsed(actionId, cost))
    }

    private fun move(state: TurnDraft, command: TurnCommand.Move): TurnTransition {
        if (command.distanceFeet <= 0 || command.distanceFeet % 5 != 0) {
            return state.rejected(TurnRejection.INVALID_MOVEMENT_INCREMENT)
        }
        val speed = state.speedsFeet[command.mode] ?: return state.rejected(TurnRejection.MOVEMENT_MODE_UNAVAILABLE)
        if (command.distanceFeet > speed) return state.rejected(TurnRejection.MOVEMENT_EXHAUSTED)
        val cost = ActionCost(pf2eActions = command.pf2eActionCost)
        state.cannotPay(cost)?.let { return state.rejected(it) }
        return state.copyAfterPayment(cost).copy(selectedMovementMode = command.mode)
            .commitFrom(state, TurnEvent.Moved(command.mode, command.distanceFeet))
    }
}

private fun beginFeature(
    state: TurnDraft,
    featureId: String,
    cost: ActionCost,
    spends: List<PendingResourceSpend>,
): TurnTransition {
    if (state.pendingFeature != null) return state.rejected(TurnRejection.PENDING_FEATURE_EXISTS)
    val declared = spends.filter { it.timing == SpendTiming.ON_DECLARE || it.timing == SpendTiming.REFUND_ON_FAILURE }
    val combinedCost = cost.copy(resources = mergeCosts(cost.resources, declared))
    state.cannotPay(combinedCost)?.let { return state.rejected(it) }
    val start = state.copyAfterPayment(combinedCost).copy(pendingFeature = PendingFeatureUse(featureId, spends))
    val events = buildList {
        add(TurnEvent.FeatureStarted(featureId, cost))
        declared.forEach { add(TurnEvent.ResourceChanged(it.resourceId, -it.amount)) }
    }
    return start.commitFrom(state, events)
}

private fun resolveFeature(state: TurnDraft, success: Boolean, hit: Boolean): TurnTransition {
    val pending = state.pendingFeature ?: return state.rejected(TurnRejection.NO_PENDING_FEATURE)
    val toCharge = pending.spends.filter {
        (it.timing == SpendTiming.ON_HIT && hit) ||
            (it.timing == SpendTiming.ON_SUCCESS && success) ||
            (it.timing == SpendTiming.ON_FAILURE && !success)
    }
    val chargeCost = ActionCost(resources = mergeCosts(emptyMap(), toCharge))
    state.cannotPay(chargeCost)?.let { return state.rejected(it) }
    val refunds = pending.spends.filter { it.timing == SpendTiming.REFUND_ON_FAILURE && !success }
    var resources = state.copyAfterPayment(chargeCost).resources.toMutableMap()
    refunds.forEach { resources[it.resourceId] = resources.getOrDefault(it.resourceId, 0) + it.amount }
    val refundDeltas = refunds.groupingBy { it.resourceId }.fold(0) { total, spend -> total + spend.amount }
    val events = buildList {
        toCharge.forEach { add(TurnEvent.ResourceChanged(it.resourceId, -it.amount)) }
        refunds.forEach { add(TurnEvent.ResourceChanged(it.resourceId, it.amount)) }
        add(TurnEvent.FeatureResolved(pending.featureId, success, hit))
    }
    val charged = state.copyAfterPayment(chargeCost)
    return charged.copy(
        resources = resources,
        pendingFeature = null,
        projection = charged.projection.copy(
            resourceDeltas = mergeDeltas(charged.projection.resourceDeltas, refundDeltas),
        ),
    ).commitFrom(state, events)
}

private fun mergeCosts(base: Map<String, Int>, spends: List<PendingResourceSpend>): Map<String, Int> =
    (base.keys + spends.map { it.resourceId }).associateWith { id ->
        base.getOrDefault(id, 0) + spends.filter { it.resourceId == id }.sumOf { it.amount }
    }

private fun end(state: TurnDraft, confirmEmpty: Boolean): TurnTransition {
    if (state.events.isEmpty() && !confirmEmpty) return state.rejected(TurnRejection.CONFIRM_EMPTY_TURN)
    if (state.pendingFeature != null) return state.rejected(TurnRejection.PENDING_FEATURE_EXISTS)
    return state.copy(phase = TurnPhase.ENDED, pendingSelection = null)
        .commitFrom(state, TurnEvent.TurnEnded)
}

private fun TurnDraft.ensureActive(expectedRuleset: RulesetId): TurnTransition? = when {
    ruleset != expectedRuleset -> rejected(TurnRejection.WRONG_RULESET_COST)
    phase == TurnPhase.ENDED || phase == TurnPhase.DISCARDED -> rejected(TurnRejection.TURN_ALREADY_ENDED)
    phase != TurnPhase.ACTIVE && phase != TurnPhase.REVIEW -> rejected(TurnRejection.TURN_NOT_ACTIVE)
    else -> null
}

private fun TurnDraft.commit(event: TurnEvent): TurnTransition = copy(events = events + event).let {
    TurnTransition(this, it, listOf(event))
}

private fun TurnDraft.commitFrom(previous: TurnDraft, event: TurnEvent): TurnTransition =
    commitFrom(previous, listOf(event))

private fun TurnDraft.commitFrom(previous: TurnDraft, newEvents: List<TurnEvent>): TurnTransition {
    val next = copy(events = previous.events + newEvents)
    return TurnTransition(previous, next, newEvents)
}

private fun TurnDraft.cannotPay(cost: ActionCost): TurnRejection? {
    if (cost.actions < 0 || cost.bonusActions < 0 || cost.reactions < 0 || cost.attacks < 0 ||
        cost.objectInteractions < 0 || cost.pf2eActions < 0 || cost.resources.values.any { it < 0 }
    ) return TurnRejection.WRONG_RULESET_COST
    if (actionsRemaining < cost.actions) return TurnRejection.INSUFFICIENT_ACTIONS
    if (bonusActionsRemaining < cost.bonusActions) return TurnRejection.INSUFFICIENT_BONUS_ACTIONS
    if (reactionsRemaining < cost.reactions) return TurnRejection.INSUFFICIENT_REACTIONS
    if (attacksRemaining < cost.attacks) return TurnRejection.INSUFFICIENT_ATTACKS
    if (objectInteractionsRemaining < cost.objectInteractions) return TurnRejection.INSUFFICIENT_OBJECT_INTERACTIONS
    if (pf2eActionsRemaining < cost.pf2eActions) return TurnRejection.INSUFFICIENT_PF2E_ACTIONS
    if (cost.resources.any { (id, amount) -> resources.getOrDefault(id, 0) < amount }) {
        return TurnRejection.INSUFFICIENT_RESOURCE
    }
    return null
}

private fun TurnDraft.copyAfterPayment(cost: ActionCost): TurnDraft = copy(
    actionsRemaining = actionsRemaining - cost.actions,
    bonusActionsRemaining = bonusActionsRemaining - cost.bonusActions,
    reactionsRemaining = reactionsRemaining - cost.reactions,
    attacksRemaining = attacksRemaining - cost.attacks,
    objectInteractionsRemaining = objectInteractionsRemaining - cost.objectInteractions,
    pf2eActionsRemaining = pf2eActionsRemaining - cost.pf2eActions,
    resources = resources.mapValues { (id, amount) -> amount - cost.resources.getOrDefault(id, 0) },
    projection = projection.copy(
        resourceDeltas = mergeDeltas(projection.resourceDeltas, cost.resources.mapValues { -it.value }),
    ),
)

private fun mergeDeltas(base: Map<String, Int>, added: Map<String, Int>): Map<String, Int> =
    (base.keys + added.keys).associateWith { id -> base.getOrDefault(id, 0) + added.getOrDefault(id, 0) }

private fun ActionCost.isPf2eCompatible(): Boolean =
    actions == 0 && bonusActions == 0 && attacks == 0 && objectInteractions == 0

private fun TurnDraft.rejected(reason: TurnRejection): TurnTransition =
    TurnTransition(previous = this, current = this, rejection = reason)
