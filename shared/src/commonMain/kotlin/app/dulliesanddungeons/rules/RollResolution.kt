package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.FiveEAttackOutcome
import app.dulliesanddungeons.domain.Pf2eDegreeOfSuccess

object RollResolution {
    fun fiveEAttack(
        total: Int,
        targetArmorClass: Int,
        naturalD20: Int,
        criticalThreshold: Int = 20,
    ): FiveEAttackOutcome {
        require(naturalD20 in 1..20)
        require(criticalThreshold in 1..20)
        return when {
            naturalD20 == 1 -> FiveEAttackOutcome.MISS
            naturalD20 >= criticalThreshold -> FiveEAttackOutcome.CRITICAL_HIT
            total >= targetArmorClass -> FiveEAttackOutcome.HIT
            else -> FiveEAttackOutcome.MISS
        }
    }

    fun pf2eDegree(total: Int, difficultyClass: Int, naturalD20: Int? = null): Pf2eDegreeOfSuccess {
        naturalD20?.let { require(it in 1..20) }
        val base = when {
            total >= difficultyClass + 10 -> Pf2eDegreeOfSuccess.CRITICAL_SUCCESS
            total >= difficultyClass -> Pf2eDegreeOfSuccess.SUCCESS
            total <= difficultyClass - 10 -> Pf2eDegreeOfSuccess.CRITICAL_FAILURE
            else -> Pf2eDegreeOfSuccess.FAILURE
        }
        val shift = when (naturalD20) {
            20 -> 1
            1 -> -1
            else -> 0
        }
        val shifted = (base.ordinal + shift).coerceIn(
            Pf2eDegreeOfSuccess.CRITICAL_FAILURE.ordinal,
            Pf2eDegreeOfSuccess.CRITICAL_SUCCESS.ordinal,
        )
        return Pf2eDegreeOfSuccess.entries[shifted]
    }
}
