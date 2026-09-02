package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.DeathReason
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.DiceRoll
import app.dulliesanddungeons.domain.FiveEHealthState
import app.dulliesanddungeons.domain.HealthStatus
import app.dulliesanddungeons.domain.Pf2eDegreeOfSuccess
import app.dulliesanddungeons.domain.Pf2eHealthState
import app.dulliesanddungeons.domain.RollKind
import app.dulliesanddungeons.domain.RollRequest
import app.dulliesanddungeons.domain.RulesetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HealthRulesTest {
    @Test
    fun exhaustionUsesTheSelectedFifthEditionRules() {
        val state = state(maximum = 41, exhaustion = 4)

        assertEquals(20, FiveEHealthRules.effectiveMaximumHitPoints(state, RulesetId.FIFTH_EDITION_2014))
        assertEquals(15, FiveEHealthRules.effectiveSpeedFeet(30, RulesetId.FIFTH_EDITION_2014, 4))
        assertEquals(10, FiveEHealthRules.effectiveSpeedFeet(30, RulesetId.FIFTH_EDITION_2024, 4))
        assertEquals(-8, FiveEHealthRules.exhaustionModifier(RulesetId.FIFTH_EDITION_2024, 4, RollKind.ATTACK))
        assertEquals(0, FiveEHealthRules.exhaustionModifier(RulesetId.FIFTH_EDITION_2024, 4, RollKind.DAMAGE))
    }

    @Test
    fun maximumHitPointReductionPrecedesThe2014ExhaustionModifier() {
        val reduced = state(maximum = 41, exhaustion = 4).copy(
            currentHitPoints = 10,
            maximumHitPointReduction = 9,
        )

        assertEquals(16, FiveEHealthRules.effectiveMaximumHitPoints(reduced, RulesetId.FIFTH_EDITION_2014))
        assertEquals(32, FiveEHealthRules.effectiveMaximumHitPoints(reduced, RulesetId.FIFTH_EDITION_2024))
        assertEquals(HealthStatus.ALIVE, FiveEHealthRules.status(reduced, RulesetId.FIFTH_EDITION_2014))

        val fullyReduced = reduced.copy(maximumHitPointReduction = 41)
        assertEquals(HealthStatus.DEAD, FiveEHealthRules.status(fullyReduced, RulesetId.FIFTH_EDITION_2024))
    }

    @Test
    fun healingAndMassiveDamageUseTheReducedMaximum() {
        val reduced = state(maximum = 20).copy(
            currentHitPoints = 10,
            maximumHitPointReduction = 7,
        )

        val healed = FiveEHealthRules.heal(reduced, RulesetId.FIFTH_EDITION_2024, amount = 100)
        assertEquals(13, healed.state.currentHitPoints)

        val damaged = FiveEHealthRules.applyDamage(
            reduced.copy(currentHitPoints = 5),
            RulesetId.FIFTH_EDITION_2024,
            amount = 18,
        )
        assertTrue(damaged.died)
    }

    @Test
    fun naturalOneAddsTwoFailuresAndNaturalTwentyRestoresOneHitPoint() {
        val failed = FiveEHealthRules.resolveDeathSave(
            state(),
            RulesetId.FIFTH_EDITION_2024,
            deathSave(natural = 1, total = -1),
        )
        assertEquals(2, (failed.state.health as FiveEHealthState).deathSaveFailures)

        val recovered = FiveEHealthRules.resolveDeathSave(
            failed.state,
            RulesetId.FIFTH_EDITION_2024,
            deathSave(natural = 20, total = 18),
        )
        assertEquals(1, recovered.state.currentHitPoints)
        assertTrue(recovered.regainedHitPoint)
        assertEquals(HealthStatus.ALIVE, FiveEHealthRules.status(recovered.state, RulesetId.FIFTH_EDITION_2024))
    }

    @Test
    fun thirdSuccessStabilizesAndResetsDeathSaveCounters() {
        val before = state(health = FiveEHealthState(deathSaveSuccesses = 2, deathSaveFailures = 1))
        val result = FiveEHealthRules.resolveDeathSave(
            before,
            RulesetId.FIFTH_EDITION_2014,
            deathSave(natural = 12, total = 12),
        )

        val health = result.state.health as FiveEHealthState
        assertTrue(result.becameStable)
        assertTrue(health.stable)
        assertEquals(0, health.deathSaveSuccesses)
        assertEquals(0, health.deathSaveFailures)
    }

    @Test
    fun criticalDamageAtZeroAddsTwoFailures() {
        val result = FiveEHealthRules.applyDamage(
            state(),
            RulesetId.FIFTH_EDITION_2024,
            amount = 3,
            criticalHit = true,
        )

        assertEquals(2, result.deathSaveFailuresAdded)
        assertEquals(2, (result.state.health as FiveEHealthState).deathSaveFailures)
        assertFalse(result.died)
    }

    @Test
    fun hitPointEditingNeedsExplicitOverrideAfterFailedDeathSaves() {
        val dead = state(health = FiveEHealthState(deathSaveFailures = 3, deathReason = DeathReason.DEATH_SAVE_FAILURES))

        assertTrue(FiveEHealthRules.heal(dead, RulesetId.FIFTH_EDITION_2024, 5).requiresRevivalOverride)
        val revived = FiveEHealthRules.heal(
            dead,
            RulesetId.FIFTH_EDITION_2024,
            amount = 5,
            allowManualRevival = true,
        )
        assertEquals(5, revived.state.currentHitPoints)
        assertEquals(null, (revived.state.health as FiveEHealthState).deathReason)
    }

    @Test
    fun pf2eSuccessfulRecoveryRemovesDyingAndAddsWounded() {
        val result = Pf2eHealthRules.resolveRecoveryCheck(
            Pf2eHealthState(dying = 1, wounded = 1),
            Pf2eDegreeOfSuccess.SUCCESS,
        )

        assertTrue(result.lostDying)
        assertEquals(0, result.health.dying)
        assertEquals(2, result.health.wounded)
    }

    private fun state(
        maximum: Int = 20,
        exhaustion: Int = 0,
        health: FiveEHealthState = FiveEHealthState(exhaustionLevel = exhaustion),
    ) = CharacterState("hero", currentHitPoints = 0, maximumHitPoints = maximum, health = health)

    private fun deathSave(natural: Int, total: Int): DiceRoll {
        val request = RollRequest("Death save", DiceExpression(1, 20), kind = RollKind.DEATH_SAVE)
        return DiceRoll(request, listOf(natural), listOf(natural), total)
    }
}
