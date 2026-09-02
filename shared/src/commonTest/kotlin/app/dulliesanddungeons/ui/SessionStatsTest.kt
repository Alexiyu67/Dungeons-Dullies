package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ActivityRecord
import app.dulliesanddungeons.domain.AttackOutcomeRecord
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.DiceRoll
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.PlaySessionRecord
import app.dulliesanddungeons.domain.RollRequest
import app.dulliesanddungeons.domain.TurnEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionStatsTest {
    @Test
    fun combatStatsAreDerivedFromTypedActivities() {
        val naturalTwenty = DiceRoll(
            request = RollRequest("Longsword", DiceExpression(1, 20, 5)),
            dice = listOf(20),
            keptDice = listOf(20),
            total = 25,
        )
        val events = listOf(
            TurnEvent.Moved(MovementMode.WALK, 25),
            TurnEvent.AttackMade("longsword"),
            TurnEvent.RollRecorded(naturalTwenty),
            TurnEvent.AttackResolved("longsword", AttackOutcomeRecord.CRITICAL),
            TurnEvent.TurnEnded,
        )
        val session = PlaySessionRecord(
            id = "session-1",
            ordinal = 1,
            startedAtEpochMillis = 1,
            activities = events.mapIndexed { index, event ->
                ActivityRecord(
                    id = "activity-$index",
                    sequence = index.toLong(),
                    label = "event",
                    turnNumber = 1,
                    turnEvent = event,
                )
            },
        )

        val stats = session.stats()

        assertEquals(1, stats.turns)
        assertEquals(25, stats.distanceMoved)
        assertEquals(1, stats.attacks)
        assertEquals(1, stats.criticals)
        assertEquals(1, stats.rolls)
        assertEquals(1, stats.naturalTwenties)
        assertEquals(0, stats.naturalOnes)
    }
}
