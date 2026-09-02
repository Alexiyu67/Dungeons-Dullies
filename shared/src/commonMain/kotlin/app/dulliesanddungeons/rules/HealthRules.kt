package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.DeathReason
import app.dulliesanddungeons.domain.DiceRoll
import app.dulliesanddungeons.domain.FiveEHealthState
import app.dulliesanddungeons.domain.HealthStatus
import app.dulliesanddungeons.domain.Pf2eDegreeOfSuccess
import app.dulliesanddungeons.domain.Pf2eHealthState
import app.dulliesanddungeons.domain.RollKind
import app.dulliesanddungeons.domain.RollMode
import app.dulliesanddungeons.domain.RulesetId

data class DeathSaveResolution(
    val state: CharacterState,
    val succeeded: Boolean?,
    val naturalD20: Int,
    val becameStable: Boolean = false,
    val regainedHitPoint: Boolean = false,
    val died: Boolean = false,
)

data class DamageResolution(
    val state: CharacterState,
    val absorbedByTemporaryHitPoints: Int,
    val hitPointsLost: Int,
    val deathSaveFailuresAdded: Int = 0,
    val died: Boolean = false,
)

data class HealingResolution(
    val state: CharacterState,
    val hitPointsRestored: Int,
    val requiresRevivalOverride: Boolean = false,
)

fun CharacterState.effectiveMaximumHitPoints(ruleset: RulesetId): Int {
    val reducedMaximum = (maximumHitPoints - maximumHitPointReduction).coerceAtLeast(0)
    val exhaustion = (health as? FiveEHealthState)?.exhaustionLevel ?: 0
    return when {
        ruleset == RulesetId.FIFTH_EDITION_2014 && exhaustion >= 4 -> reducedMaximum / 2
        else -> reducedMaximum
    }
}

object FiveEHealthRules {
    fun status(state: CharacterState, ruleset: RulesetId): HealthStatus {
        require(ruleset.isFiveEdition)
        val health = state.health as? FiveEHealthState ?: return HealthStatus.ALIVE
        return when {
            health.deathReason != null || health.exhaustionLevel >= 6 ||
                state.effectiveMaximumHitPoints(ruleset) <= 0 -> HealthStatus.DEAD
            state.currentHitPoints > 0 -> HealthStatus.ALIVE
            health.stable -> HealthStatus.STABLE
            else -> HealthStatus.DOWNED
        }
    }

    fun effectiveMaximumHitPoints(state: CharacterState, ruleset: RulesetId): Int {
        require(ruleset.isFiveEdition)
        return state.effectiveMaximumHitPoints(ruleset)
    }

    fun effectiveSpeedFeet(baseSpeedFeet: Int, ruleset: RulesetId, exhaustionLevel: Int): Int {
        require(ruleset.isFiveEdition)
        require(baseSpeedFeet >= 0)
        require(exhaustionLevel in 0..6)
        return when (ruleset) {
            RulesetId.FIFTH_EDITION_2014 -> when {
                exhaustionLevel >= 5 -> 0
                exhaustionLevel >= 2 -> baseSpeedFeet / 2
                else -> baseSpeedFeet
            }
            RulesetId.FIFTH_EDITION_2024 -> baseSpeedFeet - 5 * exhaustionLevel
            RulesetId.PF2E_REMASTER -> error("unreachable")
        }.coerceAtLeast(0)
    }

    fun exhaustionModifier(ruleset: RulesetId, exhaustionLevel: Int, kind: RollKind): Int {
        require(ruleset.isFiveEdition)
        if (ruleset != RulesetId.FIFTH_EDITION_2024 || kind !in d20Tests) return 0
        return -2 * exhaustionLevel.coerceIn(0, 6)
    }

    fun exhaustionRollMode(ruleset: RulesetId, exhaustionLevel: Int, kind: RollKind): RollMode {
        require(ruleset.isFiveEdition)
        if (ruleset != RulesetId.FIFTH_EDITION_2014) return RollMode.NORMAL
        return when {
            exhaustionLevel >= 3 && kind in setOf(RollKind.ATTACK, RollKind.SAVING_THROW, RollKind.DEATH_SAVE) ->
                RollMode.DISADVANTAGE
            exhaustionLevel >= 1 && kind == RollKind.ABILITY_CHECK -> RollMode.DISADVANTAGE
            else -> RollMode.NORMAL
        }
    }

    fun resolveDeathSave(state: CharacterState, ruleset: RulesetId, roll: DiceRoll): DeathSaveResolution {
        require(ruleset.isFiveEdition)
        require(status(state, ruleset) == HealthStatus.DOWNED) { "Only a downed character makes death saves" }
        val health = state.health as? FiveEHealthState ?: FiveEHealthState()
        require(health.deathReason == null && !health.stable) { "Dead or stable characters do not make death saves" }
        val natural = roll.keptDice.singleOrNull() ?: error("A death save must keep exactly one d20")
        require(natural in 1..20)
        if (natural == 20) {
            val updated = state.copy(
                currentHitPoints = 1.coerceAtMost(effectiveMaximumHitPoints(state, ruleset)),
                health = health.copy(deathSaveSuccesses = 0, deathSaveFailures = 0, stable = false),
            )
            return DeathSaveResolution(updated, true, natural, regainedHitPoint = true)
        }

        var successes = health.deathSaveSuccesses
        var failures = health.deathSaveFailures
        val succeeded = natural != 1 && roll.total >= 10
        if (succeeded) successes++ else failures += if (natural == 1) 2 else 1
        failures = failures.coerceAtMost(3)
        if (failures >= 3) {
            val updated = state.copy(
                health = health.copy(
                    deathSaveSuccesses = successes.coerceAtMost(2),
                    deathSaveFailures = 3,
                    stable = false,
                    deathReason = DeathReason.DEATH_SAVE_FAILURES,
                ),
            )
            return DeathSaveResolution(updated, false, natural, died = true)
        }
        if (successes >= 3) {
            val updated = state.copy(
                health = health.copy(deathSaveSuccesses = 0, deathSaveFailures = 0, stable = true),
            )
            return DeathSaveResolution(updated, true, natural, becameStable = true)
        }
        return DeathSaveResolution(
            state.copy(health = health.copy(deathSaveSuccesses = successes, deathSaveFailures = failures)),
            succeeded,
            natural,
        )
    }

    fun applyDamage(
        state: CharacterState,
        ruleset: RulesetId,
        amount: Int,
        criticalHit: Boolean = false,
    ): DamageResolution {
        require(ruleset.isFiveEdition)
        require(amount >= 0)
        if (amount == 0) return DamageResolution(state, 0, 0)
        val health = state.health as? FiveEHealthState ?: FiveEHealthState()
        if (status(state, ruleset) == HealthStatus.DEAD) return DamageResolution(state, 0, 0, died = true)

        val absorbed = amount.coerceAtMost(state.temporaryHitPoints)
        val remaining = amount - absorbed
        if (remaining == 0) {
            return DamageResolution(state.copy(temporaryHitPoints = state.temporaryHitPoints - absorbed), absorbed, 0)
        }
        val effectiveMaximum = effectiveMaximumHitPoints(state, ruleset)
        if (state.currentHitPoints > 0) {
            val lost = remaining.coerceAtMost(state.currentHitPoints)
            val leftover = remaining - lost
            val died = leftover >= effectiveMaximum && effectiveMaximum > 0
            val updatedHealth = health.copy(
                stable = false,
                deathReason = if (died) DeathReason.MASSIVE_DAMAGE else null,
            )
            return DamageResolution(
                state.copy(
                    currentHitPoints = state.currentHitPoints - lost,
                    temporaryHitPoints = state.temporaryHitPoints - absorbed,
                    health = updatedHealth,
                ),
                absorbed,
                lost,
                died = died,
            )
        }

        if (remaining >= effectiveMaximum && effectiveMaximum > 0) {
            return DamageResolution(
                state.copy(
                    temporaryHitPoints = state.temporaryHitPoints - absorbed,
                    health = health.copy(stable = false, deathReason = DeathReason.MASSIVE_DAMAGE),
                ),
                absorbed,
                0,
                died = true,
            )
        }
        val failuresAdded = if (criticalHit) 2 else 1
        val failures = (health.deathSaveFailures + failuresAdded).coerceAtMost(3)
        val died = failures >= 3
        return DamageResolution(
            state.copy(
                temporaryHitPoints = state.temporaryHitPoints - absorbed,
                health = health.copy(
                    deathSaveFailures = failures,
                    stable = false,
                    deathReason = DeathReason.DEATH_SAVE_FAILURES.takeIf { died },
                ),
            ),
            absorbed,
            0,
            failuresAdded.coerceAtMost(3 - health.deathSaveFailures),
            died,
        )
    }

    fun heal(
        state: CharacterState,
        ruleset: RulesetId,
        amount: Int,
        allowManualRevival: Boolean = false,
    ): HealingResolution {
        require(ruleset.isFiveEdition)
        require(amount >= 0)
        val health = state.health as? FiveEHealthState ?: FiveEHealthState()
        val dead = status(state, ruleset) == HealthStatus.DEAD
        if (dead && (!allowManualRevival || health.exhaustionLevel >= 6)) {
            return HealingResolution(state, 0, requiresRevivalOverride = true)
        }
        val effectiveMaximum = effectiveMaximumHitPoints(state, ruleset)
        val next = (state.currentHitPoints + amount).coerceAtMost(effectiveMaximum)
        val restored = next - state.currentHitPoints
        val updatedHealth = if (next > 0) {
            health.copy(
                deathSaveSuccesses = 0,
                deathSaveFailures = 0,
                stable = false,
                deathReason = null,
                deathNote = null,
            )
        } else health
        return HealingResolution(state.copy(currentHitPoints = next, health = updatedHealth), restored)
    }

    fun stabilize(state: CharacterState): CharacterState {
        require(state.currentHitPoints == 0)
        val health = state.health as? FiveEHealthState ?: FiveEHealthState()
        require(health.deathReason == null)
        return state.copy(
            health = health.copy(deathSaveSuccesses = 0, deathSaveFailures = 0, stable = true),
        )
    }

    private val d20Tests = setOf(
        RollKind.ABILITY_CHECK,
        RollKind.ATTACK,
        RollKind.SAVING_THROW,
        RollKind.DEATH_SAVE,
        RollKind.INITIATIVE,
    )
}

data class Pf2eRecoveryResolution(
    val health: Pf2eHealthState,
    val recoveryDc: Int,
    val died: Boolean,
    val lostDying: Boolean,
)

object Pf2eHealthRules {
    fun recoveryDc(health: Pf2eHealthState): Int = 10 + health.dying

    fun deathThreshold(health: Pf2eHealthState): Int = (4 - health.doomed).coerceAtLeast(1)

    fun resolveRecoveryCheck(health: Pf2eHealthState, degree: Pf2eDegreeOfSuccess): Pf2eRecoveryResolution {
        require(health.dying > 0 && !health.dead)
        val dc = recoveryDc(health)
        val delta = when (degree) {
            Pf2eDegreeOfSuccess.CRITICAL_SUCCESS -> -2
            Pf2eDegreeOfSuccess.SUCCESS -> -1
            Pf2eDegreeOfSuccess.FAILURE -> 1
            Pf2eDegreeOfSuccess.CRITICAL_FAILURE -> 2
        }
        val dying = (health.dying + delta).coerceAtLeast(0)
        val died = dying >= deathThreshold(health)
        val lostDying = dying == 0
        val updated = health.copy(
            dying = dying,
            wounded = if (lostDying) health.wounded + 1 else health.wounded,
            dead = died,
        )
        return Pf2eRecoveryResolution(updated, dc, died, lostDying)
    }

    fun healedWhileDying(health: Pf2eHealthState): Pf2eHealthState {
        if (health.dying == 0 || health.dead) return health
        return health.copy(dying = 0, wounded = health.wounded + 1)
    }
}
