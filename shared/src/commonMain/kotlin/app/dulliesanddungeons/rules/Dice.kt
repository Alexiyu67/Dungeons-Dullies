package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.DiceRoll
import app.dulliesanddungeons.domain.RollMode
import app.dulliesanddungeons.domain.RollRequest

/** A platform implementation can back this with a cryptographically secure random source. */
fun interface DiceSource {
    fun nextInt(bound: Int): Int
}

/** Stable across platforms and runs; intended for tests, previews, and replay fixtures. */
class DeterministicDiceSource(seed: Long) : DiceSource {
    private var state: Long = if (seed == 0L) -7046029254386353131L else seed

    override fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        var value = state
        value = value xor (value shl 13)
        value = value xor (value ushr 7)
        value = value xor (value shl 17)
        state = value
        return ((value ushr 1) % bound.toLong()).toInt()
    }
}

class SequenceDiceSource(values: Iterable<Int>) : DiceSource {
    private val iterator = values.iterator()

    override fun nextInt(bound: Int): Int {
        check(iterator.hasNext()) { "No deterministic dice values remain" }
        val value = iterator.next()
        require(value in 0 until bound) { "Sequence value $value is outside 0 until $bound" }
        return value
    }
}

internal data class DicePoolGroupRoll(
    val sides: Int,
    val values: List<Int>,
) {
    val subtotal: Int get() = values.sum()
}

internal data class DicePoolRoll(
    val groups: List<DicePoolGroupRoll>,
) {
    val diceCount: Int get() = groups.sumOf { it.values.size }
    val total: Int get() = groups.sumOf(DicePoolGroupRoll::subtotal)
    val notation: String
        get() = groups.joinToString(" + ") { group ->
            if (group.values.size == 1) "d${group.sides}" else "${group.values.size}d${group.sides}"
        }
}

class DiceRoller(private val source: DiceSource) {
    fun roll(request: RollRequest): DiceRoll {
        val expression = request.expression
        require(expression.count in 1..100) { "dice count must be between 1 and 100" }
        require(expression.sides in 2..1000) { "die sides must be between 2 and 1000" }
        require(expression.keepHighest == null || expression.keepLowest == null) {
            "keepHighest and keepLowest cannot both be specified"
        }
        require(request.mode == RollMode.NORMAL || expression.count == 1) {
            "advantage and disadvantage require a single die expression"
        }

        val modeMultiplier = if (expression.count == 1 && request.mode != RollMode.NORMAL) 2 else 1
        val dice = List(expression.count * modeMultiplier) { source.nextInt(expression.sides) + 1 }
        val keepCount = expression.keepHighest ?: expression.keepLowest
        require(keepCount == null || keepCount in 1..dice.size) { "kept dice count must be within the rolled dice count" }
        val keptDice = when {
            request.mode == RollMode.ADVANTAGE && expression.count == 1 -> listOf(dice.max())
            request.mode == RollMode.DISADVANTAGE && expression.count == 1 -> listOf(dice.min())
            expression.keepHighest != null -> dice.keepSelected(expression.keepHighest, descending = true)
            expression.keepLowest != null -> dice.keepSelected(expression.keepLowest, descending = false)
            else -> dice
        }
        return DiceRoll(
            request = request,
            dice = dice,
            keptDice = keptDice,
            total = keptDice.sum() + request.totalModifier,
        )
    }

    fun d20(label: String, modifier: Int = 0, mode: RollMode = RollMode.NORMAL): DiceRoll =
        roll(RollRequest(label, DiceExpression(1, 20, modifier), mode))

    internal fun rollPool(countsBySides: Map<Int, Int>, maxDice: Int = 100): DicePoolRoll {
        require(maxDice > 0) { "maxDice must be positive" }
        require(countsBySides.values.all { it >= 0 }) { "dice counts cannot be negative" }

        val selected = countsBySides
            .filterValues { it > 0 }
            .toSortedMap()
        val diceCount = selected.values.sum()
        require(diceCount in 1..maxDice) { "dice pool must contain between 1 and $maxDice dice" }

        return DicePoolRoll(
            groups = selected.map { (sides, count) ->
                val rolled = roll(RollRequest("Dice pool", DiceExpression(count, sides)))
                DicePoolGroupRoll(sides = sides, values = rolled.dice)
            },
        )
    }
}

private fun List<Int>.keepSelected(count: Int, descending: Boolean): List<Int> {
    val keptIndices = withIndex()
        .let { indexed -> if (descending) indexed.sortedByDescending { it.value } else indexed.sortedBy { it.value } }
        .take(count)
        .mapTo(mutableSetOf()) { it.index }
    return filterIndexed { index, _ -> index in keptIndices }
}

object DiceNotation {
    private val notation = Regex("^\\s*(\\d+)[dD](\\d+)(?:\\s*([+-])\\s*(\\d+))?\\s*$")

    fun parse(value: String): DiceExpression {
        val match = notation.matchEntire(value)
            ?: throw IllegalArgumentException("Expected dice notation such as 2d6+3")
        val count = match.groupValues[1].toInt()
        val sides = match.groupValues[2].toInt()
        val magnitude = match.groupValues[4].takeIf(String::isNotEmpty)?.toInt() ?: 0
        val modifier = if (match.groupValues[3] == "-") -magnitude else magnitude
        return DiceExpression(count = count, sides = sides, modifier = modifier)
    }
}
