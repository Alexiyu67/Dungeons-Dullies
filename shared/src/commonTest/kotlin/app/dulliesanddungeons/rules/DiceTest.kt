package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.RollMode
import app.dulliesanddungeons.domain.RollRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DiceTest {
    @Test
    fun advantageKeepsHigherD20AndAddsModifier() {
        val roller = DiceRoller(SequenceDiceSource(listOf(2, 16)))

        val result = roller.d20("Attack", modifier = 4, mode = RollMode.ADVANTAGE)

        assertEquals(listOf(3, 17), result.dice)
        assertEquals(listOf(17), result.keptDice)
        assertEquals(21, result.total)
    }

    @Test
    fun keepHighestSupportsRolledAbilityScores() {
        val roller = DiceRoller(SequenceDiceSource(listOf(0, 2, 4, 5)))

        val result = roller.roll(RollRequest("Ability", DiceExpression(4, 6, keepHighest = 3)))

        assertEquals(listOf(3, 5, 6), result.keptDice)
        assertEquals(14, result.total)
    }

    @Test
    fun notationParsesPositiveAndNegativeModifiers() {
        assertEquals(DiceExpression(2, 6, 3), DiceNotation.parse("2d6 + 3"))
        assertEquals(DiceExpression(1, 8, -2), DiceNotation.parse("1D8-2"))
        assertFailsWith<IllegalArgumentException> { DiceNotation.parse("d20") }
    }

    @Test
    fun seededSourceIsRepeatable() {
        val first = DeterministicDiceSource(42)
        val second = DeterministicDiceSource(42)

        assertEquals(List(20) { first.nextInt(20) }, List(20) { second.nextInt(20) })
    }
}
