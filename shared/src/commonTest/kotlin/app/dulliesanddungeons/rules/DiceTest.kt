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
    fun mixedPoolRollsEachSelectedDieAndCalculatesTotal() {
        val roller = DiceRoller(SequenceDiceSource(listOf(0, 5, 19)))

        val result = roller.rollPool(linkedMapOf(20 to 1, 6 to 2, 4 to 0))

        assertEquals(3, result.diceCount)
        assertEquals("2d6 + d20", result.notation)
        assertEquals(listOf(6, 20), result.groups.map { it.sides })
        assertEquals(listOf(1, 6), result.groups[0].values)
        assertEquals(7, result.groups[0].subtotal)
        assertEquals(listOf(20), result.groups[1].values)
        assertEquals(27, result.total)
    }

    @Test
    fun mixedPoolRejectsEmptyNegativeAndOversizedSelections() {
        val roller = DiceRoller(SequenceDiceSource(emptyList()))

        assertFailsWith<IllegalArgumentException> { roller.rollPool(emptyMap()) }
        assertFailsWith<IllegalArgumentException> { roller.rollPool(mapOf(6 to -1)) }
        assertFailsWith<IllegalArgumentException> { roller.rollPool(mapOf(6 to 101)) }
    }

    @Test
    fun mixedPoolKeepsEveryValueWithinItsDieRange() {
        val result = DiceRoller(DeterministicDiceSource(93)).rollPool(
            mapOf(4 to 8, 6 to 8, 8 to 8, 10 to 8, 12 to 8, 20 to 8),
        )

        result.groups.forEach { group ->
            group.values.forEach { value ->
                kotlin.test.assertTrue(value in 1..group.sides)
            }
        }
    }

    @Test
    fun seededSourceIsRepeatable() {
        val first = DeterministicDiceSource(42)
        val second = DeterministicDiceSource(42)

        assertEquals(List(20) { first.nextInt(20) }, List(20) { second.nextInt(20) })
    }
}
