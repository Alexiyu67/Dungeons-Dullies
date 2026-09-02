package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.DiceRoll
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.PendingResourceSpend
import app.dulliesanddungeons.domain.RollRequest
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.SpendTiming
import app.dulliesanddungeons.domain.TurnCommand
import app.dulliesanddungeons.domain.TurnPhase
import app.dulliesanddungeons.domain.TurnRejection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TurnReducersTest {
    @Test
    fun fifthEditionSharesDistanceAcrossMovementModes() {
        val reducer = FiveETurnReducer(RulesetId.FIFTH_EDITION_2024)
        val initial = reducer.newTurn(mapOf(MovementMode.WALK to 30, MovementMode.FLY to 40))
        assertEquals(MovementMode.FLY, initial.selectedMovementMode)

        val walked = reducer.reduce(initial, TurnCommand.Move(MovementMode.WALK, 20))
        val flew = reducer.reduce(walked.current, TurnCommand.Move(MovementMode.FLY, 20))
        val tooFar = reducer.reduce(flew.current, TurnCommand.Move(MovementMode.WALK, 5))

        assertTrue(walked.accepted)
        assertTrue(flew.accepted)
        assertFalse(tooFar.accepted)
        assertEquals(TurnRejection.MOVEMENT_EXHAUSTED, tooFar.rejection)
        assertEquals(flew.current, tooFar.current)
    }

    @Test
    fun fifthEditionRejectsOverspendingWithoutMutation() {
        val reducer = FiveETurnReducer(RulesetId.FIFTH_EDITION_2014)
        val initial = reducer.newTurn(mapOf(MovementMode.WALK to 30), resources = mapOf("superiority" to 1))
        val used = reducer.reduce(
            initial,
            TurnCommand.UseAction("maneuver", ActionCost(actions = 1, resources = mapOf("superiority" to 1))),
        )
        val rejected = reducer.reduce(used.current, TurnCommand.UseAction("dodge", ActionCost(actions = 1)))

        assertEquals(0, used.current.actionsRemaining)
        assertEquals(0, used.current.resources.getValue("superiority"))
        assertEquals(TurnRejection.INSUFFICIENT_ACTIONS, rejected.rejection)
        assertEquals(used.current, rejected.current)
    }

    @Test
    fun anEmptyTurnRequiresExplicitConfirmation() {
        val reducer = FiveETurnReducer(RulesetId.FIFTH_EDITION_2024)
        val initial = reducer.newTurn(mapOf(MovementMode.WALK to 30))

        val prompt = reducer.reduce(initial, TurnCommand.EndTurn())
        val ended = reducer.reduce(initial, TurnCommand.EndTurn(confirmEmpty = true))

        assertEquals(TurnRejection.CONFIRM_EMPTY_TURN, prompt.rejection)
        assertEquals(TurnPhase.ENDED, ended.current.phase)
    }

    @Test
    fun aRecordedRollCountsAsActivityForEndingTheTurn() {
        val reducer = FiveETurnReducer(RulesetId.FIFTH_EDITION_2024)
        val initial = reducer.newTurn(mapOf(MovementMode.WALK to 30))
        val request = RollRequest("Save", DiceExpression(1, 20, 2))
        val rolled = reducer.reduce(initial, TurnCommand.RecordRoll(DiceRoll(request, listOf(10), listOf(10), 12)))

        val ended = reducer.reduce(rolled.current, TurnCommand.EndTurn())

        assertTrue(ended.accepted)
        assertEquals(TurnPhase.ENDED, ended.current.phase)
    }

    @Test
    fun pf2eActivitiesConsumeExactlyThreeActions() {
        val reducer = Pf2eTurnReducer()
        val initial = reducer.newTurn(mapOf(MovementMode.WALK to 25))
        val twoAction = reducer.reduce(initial, TurnCommand.UseAction("activity", ActionCost(pf2eActions = 2)))
        val strike = reducer.reduce(twoAction.current, TurnCommand.UseAction("strike", ActionCost(pf2eActions = 1)))
        val fourth = reducer.reduce(strike.current, TurnCommand.UseAction("strike", ActionCost(pf2eActions = 1)))

        assertEquals(0, strike.current.pf2eActionsRemaining)
        assertEquals(TurnRejection.INSUFFICIENT_PF2E_ACTIONS, fourth.rejection)
    }

    @Test
    fun pf2eStrideConsumesAnActionButDoesNotCreateSharedMovementPool() {
        val reducer = Pf2eTurnReducer()
        val initial = reducer.newTurn(mapOf(MovementMode.WALK to 25))
        val first = reducer.reduce(initial, TurnCommand.Move(MovementMode.WALK, 25))
        val second = reducer.reduce(first.current, TurnCommand.Move(MovementMode.WALK, 25))

        assertTrue(second.accepted)
        assertEquals(1, second.current.pf2eActionsRemaining)
        assertEquals(0, second.current.movementSpentFeet)
    }

    @Test
    fun refundableFeatureSpendIsTransactionalAndProjected() {
        val reducer = FiveETurnReducer(RulesetId.FIFTH_EDITION_2024)
        val initial = reducer.newTurn(
            mapOf(MovementMode.WALK to 30),
            resources = mapOf("superiority" to 2),
        )
        val begun = reducer.reduce(
            initial,
            TurnCommand.BeginFeature(
                "precision_attack",
                ActionCost(),
                listOf(PendingResourceSpend("superiority", 1, SpendTiming.REFUND_ON_FAILURE)),
            ),
        )
        val failed = reducer.reduce(begun.current, TurnCommand.ResolveFeature(success = false))

        assertEquals(1, begun.current.resources.getValue("superiority"))
        assertEquals(-1, begun.current.projection.resourceDeltas.getValue("superiority"))
        assertEquals(2, failed.current.resources.getValue("superiority"))
        assertEquals(0, failed.current.projection.resourceDeltas.getValue("superiority"))
    }
}
