package app.dulliesanddungeons.domain

import kotlinx.serialization.Serializable

@Serializable
enum class WeaponTrainingCategory { SIMPLE, MARTIAL, CUSTOM, NONE }

@Serializable
enum class WeaponCombatType { MELEE, RANGED, UNARMED }

/** Stable, rules-facing weapon data. Display text stays on the weapon record. */
@Serializable
data class WeaponClassification(
    val baseWeaponId: String = "",
    val training: WeaponTrainingCategory = WeaponTrainingCategory.CUSTOM,
    val combatType: WeaponCombatType = WeaponCombatType.MELEE,
    val propertyIds: Set<String> = emptySet(),
)

@Serializable
data class WeaponMatch(
    val baseWeaponIds: Set<String> = emptySet(),
    val excludedBaseWeaponIds: Set<String> = emptySet(),
    val training: Set<WeaponTrainingCategory> = emptySet(),
    val combatTypes: Set<WeaponCombatType> = emptySet(),
    val allPropertyIds: Set<String> = emptySet(),
    val anyPropertyIds: Set<String> = emptySet(),
    val excludedPropertyIds: Set<String> = emptySet(),
)

@Serializable
enum class DerivedAttackTrigger {
    ALWAYS,
    AFTER_ATTACK_ACTION,
    AFTER_ATTACK_WITH_MATCHING_WEAPON,
    AFTER_HIT_WITH_MATCHING_WEAPON,
    CREATURE_ENTERS_REACH,
    DAMAGED_BY_CREATURE_IN_REACH,
    ATTACKED_BY_CREATURE_IN_REACH,
    CREATURE_MISSES_WITH_MELEE_ATTACK,
    OTHER_CREATURE_NEAR_TARGET,
    WHILE_RAGING_AFTER_FIRST_TURN,
}

@Serializable
enum class DerivedAttackParent { SAME_WEAPON, DIFFERENT_WEAPON, UNARMED_STRIKE }

@Serializable
enum class DamageAbilityRule { INHERIT, OMIT_POSITIVE, NONE }

/**
 * Declarative feature or property grant that resolves into one or more attack options.
 * It intentionally contains no protected rules prose; private packs may supply their own labels and hints.
 */
@Serializable
data class DerivedAttackGrant(
    val id: String,
    val name: String,
    val supportedRulesets: Set<RulesetId> = emptySet(),
    val parent: DerivedAttackParent = DerivedAttackParent.SAME_WEAPON,
    val weaponMatch: WeaponMatch = WeaponMatch(),
    val triggerWeaponMatch: WeaponMatch = weaponMatch,
    val trigger: DerivedAttackTrigger = DerivedAttackTrigger.ALWAYS,
    val cost: ActionCost = ActionCost(),
    val damageDice: DiceExpression? = null,
    val damageType: String? = null,
    val damageAbilityRule: DamageAbilityRule = DamageAbilityRule.INHERIT,
    val attackCount: Int = 1,
    val maxUsesPerTurn: Int? = null,
    val requiresDifferentTriggerWeapon: Boolean = false,
    val inheritItemBonus: Boolean = true,
    val inheritReach: Boolean = true,
    val timingHint: String = "",
    val details: String = "",
)
