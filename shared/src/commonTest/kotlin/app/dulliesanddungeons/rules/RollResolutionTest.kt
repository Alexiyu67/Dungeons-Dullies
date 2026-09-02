package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.FiveEAttackOutcome
import app.dulliesanddungeons.domain.Pf2eDegreeOfSuccess
import kotlin.test.Test
import kotlin.test.assertEquals

class RollResolutionTest {
    @Test
    fun fifthEditionNaturalOneAndCriticalOverrideTotals() {
        assertEquals(FiveEAttackOutcome.MISS, RollResolution.fiveEAttack(30, 10, naturalD20 = 1))
        assertEquals(FiveEAttackOutcome.CRITICAL_HIT, RollResolution.fiveEAttack(12, 25, naturalD20 = 20))
        assertEquals(FiveEAttackOutcome.HIT, RollResolution.fiveEAttack(17, 17, naturalD20 = 12))
    }

    @Test
    fun pf2eNaturalResultsShiftDegreeByOneStep() {
        assertEquals(Pf2eDegreeOfSuccess.SUCCESS, RollResolution.pf2eDegree(9, 10, naturalD20 = 20))
        assertEquals(Pf2eDegreeOfSuccess.FAILURE, RollResolution.pf2eDegree(10, 10, naturalD20 = 1))
        assertEquals(Pf2eDegreeOfSuccess.CRITICAL_SUCCESS, RollResolution.pf2eDegree(20, 10, naturalD20 = 20))
    }
}
