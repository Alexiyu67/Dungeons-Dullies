package app.dulliesanddungeons.domain

import kotlinx.serialization.Serializable

/** What a structured local rule contributes to the character's best possible turn. */
@Serializable
enum class CombatContributionType {
    /** Replaces the normal number of attacks made by one Attack action when higher. */
    ATTACK_ACTION_COUNT,
    /** Adds attack rolls outside the normal attacks of the primary Attack action. */
    EXTRA_ATTACKS,
    /** Adds spell casts outside the character's primary cast. */
    EXTRA_CASTS,
}

@Serializable
enum class CombatContributionTiming {
    ATTACK_ACTION,
    ACTION,
    BONUS_ACTION,
    EXTRA_ACTION,
    TRIGGERED,
}

/**
 * Rules-safe metadata for private/local content. Descriptive text is never parsed for automation:
 * only a contribution that the user explicitly reviews can change the displayed potential.
 */
@Serializable
data class CombatContribution(
    val type: CombatContributionType,
    val count: Int = 1,
    val timing: CombatContributionTiming = CombatContributionTiming.BONUS_ACTION,
    val variable: Boolean = false,
    val requiresAttackAction: Boolean = false,
    val requiresActionCantripForAnotherCast: Boolean = false,
    val requiresSetup: Boolean = false,
    val setupUsesConcentration: Boolean = false,
    val castsSpellThisTurn: Boolean = false,
    val requiresHit: Boolean = false,
    val requiresAdditionalTarget: Boolean = false,
    val requiredWeaponProperties: Set<String> = emptySet(),
    val resourceName: String = "",
    val resourceCost: Int = 0,
    val note: String = "",
)
