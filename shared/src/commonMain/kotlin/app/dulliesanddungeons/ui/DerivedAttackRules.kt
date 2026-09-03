package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.DamageAbilityRule
import app.dulliesanddungeons.domain.DerivedAttackGrant
import app.dulliesanddungeons.domain.DerivedAttackParent
import app.dulliesanddungeons.domain.DerivedAttackTrigger
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.TurnEvent
import app.dulliesanddungeons.domain.WeaponClassification
import app.dulliesanddungeons.domain.WeaponCombatType
import app.dulliesanddungeons.domain.WeaponMatch
import app.dulliesanddungeons.domain.WeaponTrainingCategory

/** A calculated attack view. It is deliberately never persisted as another weapon. */
data class ResolvedAttackOptionUi(
    val id: String,
    val parentWeaponId: String,
    val sourceId: String,
    val sourceName: String,
    val weapon: WeaponUi,
    val cost: ActionCost,
    val trigger: DerivedAttackTrigger,
    val triggerWeaponMatch: WeaponMatch,
    val attackCount: Int,
    val maxUsesPerTurn: Int?,
    val requiresDifferentTriggerWeapon: Boolean,
    val timingHint: String,
    val details: String,
) {
    val requiresTableConfirmation: Boolean
        get() = trigger in setOf(
            DerivedAttackTrigger.CREATURE_ENTERS_REACH,
            DerivedAttackTrigger.DAMAGED_BY_CREATURE_IN_REACH,
            DerivedAttackTrigger.ATTACKED_BY_CREATURE_IN_REACH,
            DerivedAttackTrigger.CREATURE_MISSES_WITH_MELEE_ATTACK,
            DerivedAttackTrigger.OTHER_CREATURE_NEAR_TARGET,
            DerivedAttackTrigger.WHILE_RAGING_AFTER_FIRST_TURN,
        )
}

internal fun WeaponUi.resolvedClassification(): WeaponClassification {
    val standard = standardWeaponCatalog.firstOrNull { it.id == classification.baseWeaponId || it.id == definitionId }
    val fallbackTraining = when {
        proficiencyId == "weapon:simple" -> WeaponTrainingCategory.SIMPLE
        proficiencyId == "weapon:martial" -> WeaponTrainingCategory.MARTIAL
        else -> WeaponTrainingCategory.CUSTOM
    }
    val fallbackCombatType = when {
        definitionId == "unarmed" || id == "unarmed" || properties.contains("unarmed", true) -> WeaponCombatType.UNARMED
        normalRangeFeet != null || properties.contains("ammunition", true) -> WeaponCombatType.RANGED
        else -> WeaponCombatType.MELEE
    }
    return classification.copy(
        baseWeaponId = classification.baseWeaponId.ifBlank { standard?.id ?: definitionId.ifBlank { id } },
        training = classification.training.takeUnless { it == WeaponTrainingCategory.CUSTOM }
            ?: standard?.classification?.training
            ?: fallbackTraining,
        combatType = when {
            fallbackCombatType == WeaponCombatType.UNARMED -> WeaponCombatType.UNARMED
            classification.baseWeaponId.isNotBlank() -> classification.combatType
            standard != null -> standard.classification.combatType
            else -> fallbackCombatType
        },
        propertyIds = classification.propertyIds.ifEmpty {
            normalizedWeaponPropertyIds(properties).ifEmpty { standard?.classification?.propertyIds.orEmpty() }
        },
    )
}

internal fun WeaponMatch.matches(weapon: WeaponUi): Boolean {
    val classification = weapon.resolvedClassification()
    if (classification.baseWeaponId in excludedBaseWeaponIds) return false
    if (baseWeaponIds.isNotEmpty() && classification.baseWeaponId !in baseWeaponIds) return false
    if (training.isNotEmpty() && classification.training !in training) return false
    if (combatTypes.isNotEmpty() && classification.combatType !in combatTypes) return false
    if (!classification.propertyIds.containsAll(allPropertyIds)) return false
    if (anyPropertyIds.isNotEmpty() && classification.propertyIds.intersect(anyPropertyIds).isEmpty()) return false
    if (classification.propertyIds.intersect(excludedPropertyIds).isNotEmpty()) return false
    return true
}

private data class GrantSource(val id: String, val name: String, val grant: DerivedAttackGrant)

internal fun DndAppState.derivedAttackOptions(
    character: CharacterUi,
    parentWeapon: WeaponUi,
): List<ResolvedAttackOptionUi> {
    if (character.ruleset == Ruleset.Pf2eRemaster) return emptyList()
    val sources = buildList {
        character.features.forEach { feature ->
            feature.attackGrants.forEach { grant -> add(GrantSource(feature.id, feature.name, grant)) }
        }
        privateEntries.filter { entry ->
            entry.id in character.featIds && character.features.none { it.id == "private-${entry.id}" && it.attackGrants.isNotEmpty() }
        }.forEach { entry ->
            entry.attackGrants.forEach { grant -> add(GrantSource("private-${entry.id}", entry.name, grant)) }
        }
        addAll(coreWeaponPropertyGrants(character))
        addAll(coreMonkGrants(character))
        addAll(coreExplicitFeatureGrants(character))
    }
    return sources.mapNotNull { source -> resolveAttackGrant(character, parentWeapon, source) }
        .distinctBy(ResolvedAttackOptionUi::id)
}

private fun coreExplicitFeatureGrants(character: CharacterUi): List<GrantSource> = buildList {
    character.features.filter { it.attackGrants.isEmpty() }.forEach { feature ->
        val key = "${feature.id} ${feature.name}".lowercase()
        when {
            "maneuver-riposte" in key || feature.name.equals("Riposte", true) -> add(
                GrantSource(
                    feature.id,
                    feature.name,
                    DerivedAttackGrant(
                        id = "riposte-attack",
                        name = "Riposte",
                        weaponMatch = WeaponMatch(combatTypes = setOf(WeaponCombatType.MELEE)),
                        trigger = DerivedAttackTrigger.CREATURE_MISSES_WITH_MELEE_ATTACK,
                        cost = ActionCost(reactions = 1, resources = mapOf((feature.resourceId ?: "superiority-dice") to feature.resourceCost)),
                        maxUsesPerTurn = 1,
                        timingHint = "Use after a creature misses you with a melee attack, if the maneuver's other conditions are met.",
                    ),
                )
            )
            feature.name.equals("Horde Breaker", true) -> add(
                GrantSource(
                    feature.id,
                    feature.name,
                    DerivedAttackGrant(
                        id = "horde-breaker-attack",
                        name = "Horde Breaker attack",
                        trigger = DerivedAttackTrigger.OTHER_CREATURE_NEAR_TARGET,
                        maxUsesPerTurn = 1,
                        timingHint = "Confirm that another creature is near the original target and within this weapon's range.",
                    ),
                )
            )
            feature.name.equals("Giant Killer", true) -> add(
                GrantSource(
                    feature.id,
                    feature.name,
                    DerivedAttackGrant(
                        id = "giant-killer-attack",
                        name = "Giant Killer attack",
                        trigger = DerivedAttackTrigger.ATTACKED_BY_CREATURE_IN_REACH,
                        cost = ActionCost(reactions = 1),
                        maxUsesPerTurn = 1,
                        timingHint = "Use immediately after an eligible Large or larger creature within 5 feet attacks you.",
                    ),
                )
            )
        }
    }
}

internal fun DndAppState.allAttackOptions(character: CharacterUi): List<ResolvedAttackOptionUi> =
    character.weapons.flatMap { derivedAttackOptions(character, it) }

private fun coreWeaponPropertyGrants(character: CharacterUi): List<GrantSource> {
    val eligibleLightWeapons = character.weapons.filter { weapon ->
        val classification = weapon.resolvedClassification()
        "light" in classification.propertyIds &&
            (character.ruleset == Ruleset.Fifth2024 || classification.combatType == WeaponCombatType.MELEE)
    }
    if (eligibleLightWeapons.size < 2) return emptyList()
    val hasTwoWeaponStyle = character.features.any {
        it.id.contains("two-weapon-fighting", true) || it.name.equals("Two-Weapon Fighting", true)
    } || character.featIds.any { it.contains("two-weapon-fighting", true) }
    val grant = DerivedAttackGrant(
        id = if (character.ruleset == Ruleset.Fifth2024) "light-extra-attack" else "two-weapon-attack",
        name = if (character.ruleset == Ruleset.Fifth2024) "Light extra attack" else "Two-weapon attack",
        parent = DerivedAttackParent.DIFFERENT_WEAPON,
        weaponMatch = WeaponMatch(
            combatTypes = if (character.ruleset == Ruleset.Fifth2014) setOf(WeaponCombatType.MELEE) else emptySet(),
            allPropertyIds = setOf("light"),
        ),
        triggerWeaponMatch = WeaponMatch(
            combatTypes = if (character.ruleset == Ruleset.Fifth2014) setOf(WeaponCombatType.MELEE) else emptySet(),
            allPropertyIds = setOf("light"),
        ),
        trigger = DerivedAttackTrigger.AFTER_ATTACK_WITH_MATCHING_WEAPON,
        cost = ActionCost(bonusActions = 1),
        damageAbilityRule = if (hasTwoWeaponStyle) DamageAbilityRule.INHERIT else DamageAbilityRule.OMIT_POSITIVE,
        maxUsesPerTurn = 1,
        requiresDifferentTriggerWeapon = true,
        timingHint = "Use after attacking with a different Light weapon.",
        details = "The attack uses this weapon's normal attack roll. Its damage modifier follows your Two-Weapon Fighting features.",
    )
    return listOf(GrantSource("weapon-property:light", "Light", grant))
}

private fun coreMonkGrants(character: CharacterUi): List<GrantSource> {
    val monkLevel = character.progression.count { it.className.equals("Monk", true) }.takeIf { it > 0 }
        ?: character.level.takeIf { character.className.equals("Monk", true) }
        ?: return emptyList()
    val monkWeaponIds = setOf(
        "club", "dagger", "handaxe", "javelin", "light-hammer", "mace", "quarterstaff", "sickle", "spear", "shortsword", "unarmed",
    ) + if (character.ruleset == Ruleset.Fifth2024) setOf("scimitar") else emptySet()
    val monkWeaponMatch = WeaponMatch(
        baseWeaponIds = monkWeaponIds,
        combatTypes = setOf(WeaponCombatType.MELEE, WeaponCombatType.UNARMED),
        excludedPropertyIds = setOf("heavy", "two-handed"),
    )
    val openHand = character.features.any { it.name.contains("Open Hand", true) }
    val martialArts = DerivedAttackGrant(
        id = "martial-arts-bonus-strike",
        name = "Martial Arts strike",
        parent = DerivedAttackParent.UNARMED_STRIKE,
        weaponMatch = monkWeaponMatch,
        triggerWeaponMatch = monkWeaponMatch,
        trigger = if (character.ruleset == Ruleset.Fifth2014) {
            DerivedAttackTrigger.AFTER_ATTACK_WITH_MATCHING_WEAPON
        } else {
            DerivedAttackTrigger.ALWAYS
        },
        cost = ActionCost(bonusActions = 1),
        damageDice = monkMartialArtsDie(character.ruleset, monkLevel),
        maxUsesPerTurn = 1,
        timingHint = if (character.ruleset == Ruleset.Fifth2014) {
            "Use after attacking with an eligible Monk weapon or Unarmed Strike."
        } else {
            "An Unarmed Strike made as a Bonus Action."
        },
    )
    val flurry = DerivedAttackGrant(
        id = "flurry-of-blows",
        name = "Flurry of Blows",
        parent = DerivedAttackParent.UNARMED_STRIKE,
        weaponMatch = monkWeaponMatch,
        triggerWeaponMatch = monkWeaponMatch,
        trigger = if (character.ruleset == Ruleset.Fifth2014) DerivedAttackTrigger.AFTER_ATTACK_ACTION else DerivedAttackTrigger.ALWAYS,
        cost = ActionCost(bonusActions = 1, resources = mapOf("focus-points" to 1)),
        damageDice = monkMartialArtsDie(character.ruleset, monkLevel),
        attackCount = if (character.ruleset == Ruleset.Fifth2024 && monkLevel >= 10) 3 else 2,
        maxUsesPerTurn = 1,
        timingHint = if (character.ruleset == Ruleset.Fifth2014) {
            "Use immediately after taking the Attack action."
        } else {
            "Spend 1 Focus Point and a Bonus Action."
        },
        details = if (openHand) "Open Hand Technique can modify hits from these attacks." else "",
    )
    return buildList {
        add(GrantSource("martial-arts", "Martial Arts", martialArts))
        if (monkLevel >= 2) add(GrantSource("flurry-of-blows", "Flurry of Blows", flurry))
    }
}

private fun monkMartialArtsDie(ruleset: Ruleset, monkLevel: Int): DiceExpression {
    val sides = if (ruleset == Ruleset.Fifth2014) when {
        monkLevel >= 17 -> 10
        monkLevel >= 11 -> 8
        monkLevel >= 5 -> 6
        else -> 4
    } else when {
        monkLevel >= 17 -> 12
        monkLevel >= 11 -> 10
        monkLevel >= 5 -> 8
        else -> 6
    }
    return DiceExpression(1, sides)
}

private fun DndAppState.resolveAttackGrant(
    character: CharacterUi,
    parentWeapon: WeaponUi,
    source: GrantSource,
): ResolvedAttackOptionUi? {
    val grant = source.grant
    val rulesetId = when (character.ruleset) {
        Ruleset.Fifth2014 -> RulesetId.FIFTH_EDITION_2014
        Ruleset.Fifth2024 -> RulesetId.FIFTH_EDITION_2024
        Ruleset.Pf2eRemaster -> RulesetId.PF2E_REMASTER
    }
    if (grant.supportedRulesets.isNotEmpty() && rulesetId !in grant.supportedRulesets) return null
    if (!grant.weaponMatch.matches(parentWeapon)) return null
    val resolvedParent = when (grant.parent) {
        DerivedAttackParent.SAME_WEAPON, DerivedAttackParent.DIFFERENT_WEAPON -> parentWeapon
        DerivedAttackParent.UNARMED_STRIKE -> character.weapons.firstOrNull {
            it.resolvedClassification().combatType == WeaponCombatType.UNARMED
        } ?: character.syntheticUnarmedStrike()
    }
    val abilityScore = character.abilities[resolvedParent.damageAbility ?: resolvedParent.ability] ?: 10
    val abilityModifier = kotlin.math.floor((abilityScore - 10) / 2.0).toInt()
    val inheritedItemBonus = if (grant.inheritItemBonus) resolvedParent.itemBonus else 0
    val modifier = when (grant.damageAbilityRule) {
        DamageAbilityRule.INHERIT -> abilityModifier + inheritedItemBonus
        DamageAbilityRule.OMIT_POSITIVE -> abilityModifier.coerceAtMost(0) + inheritedItemBonus
        DamageAbilityRule.NONE -> inheritedItemBonus
    }
    val damage = grant.damageDice?.let { dice ->
        formatDamage("${dice.count}d${dice.sides}", dice.modifier + modifier)
    } ?: when (grant.damageAbilityRule) {
        DamageAbilityRule.INHERIT -> resolvedParent.damage
        else -> replaceDamageModifier(resolvedParent.damage, modifier)
    }
    val attackBonus = if (grant.inheritItemBonus) resolvedParent.attackBonus
        else resolvedParent.attackBonus - resolvedParent.itemBonus
    val optionId = "${resolvedParent.id}::derived::${source.id}::${grant.id}"
    return ResolvedAttackOptionUi(
        id = optionId,
        parentWeaponId = parentWeapon.id,
        sourceId = source.id,
        sourceName = source.name,
        weapon = resolvedParent.copy(
            id = optionId,
            name = grant.name,
            attackBonus = attackBonus,
            damage = damage,
            damageType = grant.damageType ?: resolvedParent.damageType,
            reachFeet = if (grant.inheritReach) resolvedParent.reachFeet else 5,
        ),
        cost = grant.cost,
        trigger = grant.trigger,
        triggerWeaponMatch = grant.triggerWeaponMatch,
        attackCount = grant.attackCount.coerceAtLeast(1),
        maxUsesPerTurn = grant.maxUsesPerTurn,
        requiresDifferentTriggerWeapon = grant.requiresDifferentTriggerWeapon,
        timingHint = grant.timingHint,
        details = grant.details,
    )
}

private fun CharacterUi.syntheticUnarmedStrike(): WeaponUi {
    val strengthModifier = kotlin.math.floor(((abilities["STR"] ?: 10) - 10) / 2.0).toInt()
    val dexterityModifier = kotlin.math.floor(((abilities["DEX"] ?: 10) - 10) / 2.0).toInt()
    val useDexterity = className.equals("Monk", true) && dexterityModifier > strengthModifier
    val ability = if (useDexterity) "DEX" else "STR"
    val modifier = if (useDexterity) dexterityModifier else strengthModifier
    return WeaponUi(
        id = "unarmed",
        name = "Unarmed Strike",
        attackBonus = modifier + proficiency,
        damage = formatDamage("1d1", modifier),
        damageType = "Bludgeoning",
        properties = "Unarmed",
        ability = ability,
        damageAbility = ability,
        definitionId = "unarmed",
        classification = WeaponClassification(
            baseWeaponId = "unarmed",
            training = WeaponTrainingCategory.NONE,
            combatType = WeaponCombatType.UNARMED,
        ),
    )
}

private fun formatDamage(dice: String, modifier: Int): String = when {
    modifier > 0 -> "$dice + $modifier"
    modifier < 0 -> "$dice - ${-modifier}"
    else -> dice
}

private fun replaceDamageModifier(damage: String, modifier: Int): String {
    val dice = Regex("\\d+d\\d+", RegexOption.IGNORE_CASE).find(damage)?.value ?: damage.substringBefore(' ').trim()
    return formatDamage(dice, modifier)
}

internal fun ResolvedAttackOptionUi.isAvailableIn(session: TurnSession, character: CharacterUi): Boolean {
    val uses = session.events.count { (it as? TurnEvent.AttackMade)?.attackOptionId == id }
    val continuingMultiattack = uses % attackCount != 0
    if (!continuingMultiattack && !session.canPay(cost)) return false
    if (!continuingMultiattack && cost.resources.any { (resourceId, amount) ->
            (character.features.firstOrNull { it.id == resourceId }?.remaining ?: 0) < amount
        }
    ) return false
    if (maxUsesPerTurn != null && uses >= maxUsesPerTurn * attackCount) return false
    val triggeringAttacks = session.events.filterIsInstance<TurnEvent.AttackMade>().filter { event ->
        val triggerWeapon = character.weapons.firstOrNull { it.id == event.weaponId } ?: return@filter false
        triggerWeaponMatch.matches(triggerWeapon) && (!requiresDifferentTriggerWeapon || event.weaponId != parentWeaponId)
    }
    return when (trigger) {
        DerivedAttackTrigger.ALWAYS -> true
        DerivedAttackTrigger.AFTER_ATTACK_ACTION -> session.attackActionStarted
        DerivedAttackTrigger.AFTER_ATTACK_WITH_MATCHING_WEAPON -> triggeringAttacks.isNotEmpty()
        DerivedAttackTrigger.AFTER_HIT_WITH_MATCHING_WEAPON -> session.events.filterIsInstance<TurnEvent.AttackResolved>().any { event ->
            event.outcome != app.dulliesanddungeons.domain.AttackOutcomeRecord.MISS &&
                character.weapons.firstOrNull { it.id == event.weaponId }?.let(triggerWeaponMatch::matches) == true &&
                (!requiresDifferentTriggerWeapon || event.weaponId != parentWeaponId)
        }
        DerivedAttackTrigger.OTHER_CREATURE_NEAR_TARGET -> triggeringAttacks.isNotEmpty()
        else -> true
    }
}

internal fun ResolvedAttackOptionUi.unavailableReason(session: TurnSession, character: CharacterUi): String? = when {
    session.events.count { (it as? TurnEvent.AttackMade)?.attackOptionId == id } % attackCount == 0 && !session.canPay(cost) ->
        "Its action cost is no longer available this turn."
    session.events.count { (it as? TurnEvent.AttackMade)?.attackOptionId == id } % attackCount == 0 && cost.resources.any { (resourceId, amount) ->
        (character.features.firstOrNull { it.id == resourceId }?.remaining ?: 0) < amount
    } -> "Its resource cost is not available."
    maxUsesPerTurn != null && session.events.count { (it as? TurnEvent.AttackMade)?.attackOptionId == id } >= maxUsesPerTurn * attackCount ->
        "Already used this turn."
    !isAvailableIn(session, character) -> timingHint.ifBlank { "Complete the required attack first." }
    else -> null
}
