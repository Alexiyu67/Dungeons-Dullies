package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CoreModifier
import app.dulliesanddungeons.domain.CoreStatistic
import app.dulliesanddungeons.domain.DifficultyClass
import app.dulliesanddungeons.domain.EffectActivation
import app.dulliesanddungeons.domain.EquipmentLocation
import app.dulliesanddungeons.domain.ModifierOperation
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.ProficiencyRank
import kotlin.math.floor

data class StatSourceUi(
    val label: String,
    val detail: String = "",
    val amount: Int? = null,
    val active: Boolean = true,
)

data class StatCalculationUi(
    val total: Int,
    val sources: List<StatSourceUi>,
)

private data class SourcedModifier(
    val sourceKey: String,
    val sourceName: String,
    val modifier: CoreModifier,
    val active: Boolean,
    val inactiveReason: String = "",
)

internal object CharacterStatEngine {
    fun resolve(character: CharacterUi, conditions: List<ConditionUi> = emptyList()): CharacterUi {
        val baseAbilities = character.baseAbilities.ifEmpty { character.abilities }
        val allModifiers = modifiers(character, conditions)
        val abilities = Ability.entries.associate { ability ->
            val key = ability.shortName()
            key to apply(baseAbilities[key] ?: character.abilities[key] ?: 10, allModifiers.activeFor(CoreStatistic.ABILITY_SCORE, ability))
        }
        val baseSaves = calculateBaseSaves(character, abilities)
        val saves = Ability.entries.associate { ability ->
            ability.displayName() to apply(
                baseSaves[ability.displayName()] ?: abilityModifier(abilities.getValue(ability.shortName())),
                allModifiers.activeFor(CoreStatistic.SAVING_THROW, ability),
            )
        }
        val baseArmorClass = character.baseArmorClass ?: character.armorClass
        val armorClass = apply(baseArmorClass, allModifiers.activeFor(CoreStatistic.ARMOR_CLASS))
        val baseWalk = character.baseSpeedFeet ?: character.speedFeet
        val baseFly = character.baseFlySpeedFeet ?: character.flySpeedFeet
        val speedFeet = apply(baseWalk, allModifiers.activeFor(CoreStatistic.SPEED, movementMode = MovementMode.WALK)).coerceAtLeast(0)
        val flySpeedFeet = baseFly?.let {
            apply(it, allModifiers.activeFor(CoreStatistic.SPEED, movementMode = MovementMode.FLY)).coerceAtLeast(0)
        }
        val weapons = character.weapons.map { weapon ->
            val ability = abilityModifier(abilities[weapon.ability] ?: 10)
            val proficiency = proficiencyModifier(character, weapon.proficiencyId, weapon.proficient)
            val own = ability + proficiency + weapon.itemBonus
            val attack = apply(weapon.attackBonusOverride ?: own, allModifiers.activeFor(CoreStatistic.ATTACK_ROLL))
            val damageModifier = apply(0, allModifiers.activeFor(CoreStatistic.DAMAGE_ROLL))
            weapon.copy(
                attackBonus = attack,
                damage = weapon.damageAbility?.let { key ->
                    replaceFormulaModifier(weapon.damage, abilityModifier(abilities[key] ?: 10) + weapon.itemBonus + damageModifier)
                }
                    ?: weapon.damage,
            )
        }
        return character.copy(
            abilities = abilities,
            saves = saves,
            armorClass = armorClass,
            speedFeet = speedFeet,
            flySpeedFeet = flySpeedFeet,
            weapons = weapons,
            baseAbilities = baseAbilities,
            baseSaves = baseSaves,
            baseArmorClass = baseArmorClass,
            baseSpeedFeet = baseWalk,
            baseFlySpeedFeet = baseFly,
        )
    }

    fun armorClass(character: CharacterUi, conditions: List<ConditionUi> = emptyList()): StatCalculationUi {
        val resolved = resolve(character, conditions)
        val modifiers = modifiers(resolved, conditions)
        val bodyArmor = resolved.equipmentItems.firstOrNull { it.worn && it.armorClass != null }
        val shields = resolved.equipmentItems.filter { it.worn && it.shieldBonus != 0 && (!it.needsAttunement || it.attuned) }
        val base = resolved.baseArmorClass ?: resolved.armorClass
        val bodyValue = bodyArmor?.armorClass ?: resolved.unarmoredArmorClass
        val expectedBase = bodyValue + shields.sumOf(EquipmentUi::shieldBonus) + if (bodyArmor != null) resolved.passiveArmorClassBonus else 0
        val baseAdjustment = base - expectedBase
        return StatCalculationUi(
            total = resolved.armorClass,
            sources = buildList {
                add(StatSourceUi(bodyArmor?.name ?: "Unarmored", if (bodyArmor == null) "Base Armor Class" else "Worn armor", bodyValue))
                shields.forEach { add(StatSourceUi(it.name, "Equipped shield", it.shieldBonus)) }
                if (bodyArmor != null && resolved.passiveArmorClassBonus != 0) {
                    add(StatSourceUi("Class and subclass", "Passive Armor Class bonus", resolved.passiveArmorClassBonus))
                }
                if (baseAdjustment != 0) add(StatSourceUi("Manual adjustment", "Stored base correction", baseAdjustment))
                addModifierSources(modifiers, CoreStatistic.ARMOR_CLASS)
            },
        )
    }

    fun savingThrow(character: CharacterUi, ability: Ability, conditions: List<ConditionUi> = emptyList()): StatCalculationUi {
        val resolved = resolve(character, conditions)
        val modifiers = modifiers(resolved, conditions)
        val formula = resolved.derivation.saves[ability.displayName()]
        val abilityAmount = abilityModifier(resolved.abilities[ability.shortName()] ?: 10)
        val proficiency = formula?.proficiencyId?.let { proficiencyModifier(resolved, it, legacy = false) }
            ?: (resolved.baseSaves[ability.displayName()] ?: 0) - abilityAmount
        val baseExtra = formula?.base ?: 0
        return StatCalculationUi(
            total = resolved.saves[ability.displayName()] ?: abilityAmount + proficiency + baseExtra,
            sources = buildList {
                add(StatSourceUi(ability.displayName(), "Ability modifier", abilityAmount))
                if (proficiency != 0) add(StatSourceUi("Proficiency", formula?.proficiencyId.orEmpty(), proficiency))
                if (baseExtra != 0) add(StatSourceUi("Class, subclass, or manual", "Base adjustment", baseExtra))
                addModifierSources(modifiers, CoreStatistic.SAVING_THROW, ability)
            },
        )
    }

    fun spellAttackBonus(character: CharacterUi, spell: SpellUi? = null, conditions: List<ConditionUi> = emptyList()): Int {
        val resolved = resolve(character, conditions)
        val ability = spell?.spellcastingAbility ?: defaultSpellcastingAbility(resolved)
        val base = abilityModifier(resolved.abilities[ability.shortName()] ?: 10) + resolved.proficiency
        return apply(base, modifiers(resolved, conditions).activeFor(CoreStatistic.SPELL_ATTACK))
    }

    fun spellSaveDc(character: CharacterUi, spell: SpellUi? = null, conditions: List<ConditionUi> = emptyList()): Int {
        val resolved = resolve(character, conditions)
        val ability = spell?.spellcastingAbility ?: defaultSpellcastingAbility(resolved)
        val base = 8 + abilityModifier(resolved.abilities[ability.shortName()] ?: 10) + resolved.proficiency
        return apply(base, modifiers(resolved, conditions).activeFor(CoreStatistic.SPELL_SAVE_DC))
    }

    fun difficultyClass(character: CharacterUi, dc: DifficultyClass, spell: SpellUi? = null): Int {
        dc.fixed?.let { return it }
        val ability = when {
            dc.useSpellcasting -> spell?.spellcastingAbility ?: defaultSpellcastingAbility(character)
            else -> dc.ability
        }
        return dc.base + (ability?.let { abilityModifier(character.abilities[it.shortName()] ?: 10) } ?: 0) +
            if (dc.addProficiency || dc.useSpellcasting) character.proficiency else 0
    }

    private fun calculateBaseSaves(character: CharacterUi, abilities: Map<String, Int>): Map<String, Int> = Ability.entries.associate { ability ->
        val name = ability.displayName()
        val formula = character.derivation.saves[name]
        val fallback = character.baseSaves[name] ?: character.saves[name] ?: abilityModifier(abilities[ability.shortName()] ?: 10)
        val total = if (formula == null) fallback else {
            abilityModifier(abilities[formula.ability] ?: 10) + formula.base + when {
                formula.proficiencyId != null -> proficiencyModifier(character, formula.proficiencyId, legacy = false)
                else -> formula.proficiencyMultiplier * character.proficiency
            }
        }
        name to total
    }

    private fun modifiers(character: CharacterUi, conditions: List<ConditionUi>): List<SourcedModifier> {
        val raw = buildList {
            character.equipmentItems.forEach { item ->
                item.effects.forEach { effect ->
                    val active = equipmentEffectActive(item, effect.activation)
                    add(SourcedModifier("equipment:${item.definitionId}", item.name, effect, active, equipmentInactiveReason(item, effect.activation)))
                }
            }
            character.weapons.forEach { weapon ->
                weapon.effects.forEach { effect ->
                    val active = weaponEffectActive(weapon, effect.activation)
                    add(SourcedModifier("weapon:${weapon.definitionId}", weapon.name, effect, active, weaponInactiveReason(weapon, effect.activation)))
                }
            }
            character.features.forEach { feature ->
                feature.effects.forEach { add(SourcedModifier("feature:${feature.id}", feature.name, it, true)) }
            }
            conditions.filter { it.characterId.isBlank() || it.characterId == character.id }.forEach { condition ->
                condition.effects.forEach { add(SourcedModifier("condition:${condition.id}", condition.name, it, true)) }
            }
        }
        // Identical named effects from duplicate copies do not stack; different sources still can.
        return raw.groupBy { sourced ->
            listOf(
                sourced.sourceKey,
                sourced.modifier.statistic.name,
                sourced.modifier.ability?.name.orEmpty(),
                sourced.modifier.movementMode?.name.orEmpty(),
                sourced.modifier.operation.name,
            ).joinToString(":")
        }.values.map { candidates ->
            candidates.maxByOrNull { candidate -> if (candidate.active) candidate.modifier.amount else Int.MIN_VALUE } ?: candidates.first()
        }
    }

    private fun List<SourcedModifier>.activeFor(
        statistic: CoreStatistic,
        ability: Ability? = null,
        movementMode: MovementMode? = null,
    ): List<CoreModifier> = filter { sourced ->
        sourced.active && sourced.modifier.statistic == statistic &&
            (sourced.modifier.ability == null || sourced.modifier.ability == ability) &&
            (sourced.modifier.movementMode == null || sourced.modifier.movementMode == movementMode)
    }.map(SourcedModifier::modifier)

    private fun MutableList<StatSourceUi>.addModifierSources(
        modifiers: List<SourcedModifier>,
        statistic: CoreStatistic,
        ability: Ability? = null,
    ) {
        modifiers.filter { sourced ->
            sourced.modifier.statistic == statistic && (sourced.modifier.ability == null || sourced.modifier.ability == ability)
        }.forEach { sourced ->
            add(
                StatSourceUi(
                    label = sourced.sourceName,
                    detail = sourced.modifier.label.ifBlank { if (sourced.active) "Active effect" else sourced.inactiveReason },
                    amount = sourced.modifier.amount.takeIf { sourced.active },
                    active = sourced.active,
                ),
            )
        }
    }

    private fun equipmentEffectActive(item: EquipmentUi, activation: EffectActivation): Boolean {
        val equipped = item.worn
        val attuned = !item.needsAttunement || item.attuned
        return when (activation) {
            EffectActivation.ALWAYS, EffectActivation.CARRIED -> true
            EffectActivation.EQUIPPED -> equipped
            EffectActivation.WORN -> equipped && item.activeLocation == EquipmentLocation.WORN
            EffectActivation.HELD -> equipped && item.activeLocation == EquipmentLocation.HELD
            EffectActivation.WIELDED -> equipped && item.activeLocation == EquipmentLocation.WIELDED
            EffectActivation.ATTUNED -> attuned
            EffectActivation.CARRIED_AND_ATTUNED -> attuned
            EffectActivation.EQUIPPED_AND_ATTUNED -> equipped && attuned
            EffectActivation.WORN_AND_ATTUNED -> equipped && attuned && item.activeLocation == EquipmentLocation.WORN
            EffectActivation.HELD_AND_ATTUNED -> equipped && attuned && item.activeLocation == EquipmentLocation.HELD
            EffectActivation.WIELDED_AND_ATTUNED -> equipped && attuned && item.activeLocation == EquipmentLocation.WIELDED
        }
    }

    private fun weaponEffectActive(weapon: WeaponUi, activation: EffectActivation): Boolean {
        val attuned = !weapon.needsAttunement || weapon.attuned
        return when (activation) {
            EffectActivation.ALWAYS, EffectActivation.CARRIED -> true
            EffectActivation.ATTUNED, EffectActivation.CARRIED_AND_ATTUNED -> attuned
            EffectActivation.EQUIPPED, EffectActivation.WORN, EffectActivation.HELD, EffectActivation.WIELDED -> weapon.equipped
            EffectActivation.EQUIPPED_AND_ATTUNED, EffectActivation.WORN_AND_ATTUNED,
            EffectActivation.HELD_AND_ATTUNED, EffectActivation.WIELDED_AND_ATTUNED -> weapon.equipped && attuned
        }
    }

    private fun equipmentInactiveReason(item: EquipmentUi, activation: EffectActivation): String = when {
        item.needsAttunement && !item.attuned && activation.name.contains("ATTUNED") -> "Needs attunement"
        !item.worn && activation !in setOf(EffectActivation.ALWAYS, EffectActivation.CARRIED, EffectActivation.ATTUNED, EffectActivation.CARRIED_AND_ATTUNED) -> "Not equipped"
        else -> "Inactive"
    }

    private fun weaponInactiveReason(weapon: WeaponUi, activation: EffectActivation): String = when {
        weapon.needsAttunement && !weapon.attuned && activation.name.contains("ATTUNED") -> "Needs attunement"
        !weapon.equipped && activation !in setOf(EffectActivation.ALWAYS, EffectActivation.CARRIED, EffectActivation.ATTUNED, EffectActivation.CARRIED_AND_ATTUNED) -> "Not equipped"
        else -> "Inactive"
    }

    private fun apply(base: Int, modifiers: List<CoreModifier>): Int {
        val minimum = modifiers.filter { it.operation == ModifierOperation.MINIMUM }.maxOfOrNull(CoreModifier::amount)
        val set = modifiers.filter { it.operation == ModifierOperation.SET }.maxOfOrNull(CoreModifier::amount)
        val starting = when {
            set != null -> set
            minimum != null -> maxOf(base, minimum)
            else -> base
        }
        return starting + modifiers.filter { it.operation == ModifierOperation.ADD }.sumOf(CoreModifier::amount)
    }

    private fun proficiencyModifier(character: CharacterUi, id: String?, legacy: Boolean): Int {
        val rank = id?.let { proficiencyId ->
            character.proficiencyRanks[proficiencyId] ?: ProficiencyRank.TRAINED.takeIf { proficiencyId in character.proficiencyIds }
        }
        return when {
            id == null -> if (legacy) character.proficiency else 0
            rank == null -> 0
            character.ruleset != Ruleset.Pf2eRemaster -> character.proficiency
            else -> character.level + rank.rankBonus
        }
    }

    private fun defaultSpellcastingAbility(character: CharacterUi): Ability = when (character.className.lowercase()) {
        "wizard", "artificer" -> Ability.INTELLIGENCE
        "cleric", "druid", "ranger", "monk" -> Ability.WISDOM
        else -> Ability.CHARISMA
    }

    private fun abilityModifier(score: Int): Int = floor((score - 10) / 2.0).toInt()

    private fun replaceFormulaModifier(formula: String, modifier: Int): String {
        val base = formula.substringBefore('·').trim().replace(Regex("\\s*[+-]\\s*\\d+\\s*$"), "")
        return when {
            modifier > 0 -> "$base + $modifier"
            modifier < 0 -> "$base - ${-modifier}"
            else -> base
        }
    }
}

internal fun Ability.shortName(): String = when (this) {
    Ability.STRENGTH -> "STR"
    Ability.DEXTERITY -> "DEX"
    Ability.CONSTITUTION -> "CON"
    Ability.INTELLIGENCE -> "INT"
    Ability.WISDOM -> "WIS"
    Ability.CHARISMA -> "CHA"
}

internal fun Ability.displayName(): String = name.lowercase().replaceFirstChar { it.uppercase() }

internal fun abilityFromUiName(value: String): Ability? = when (value.trim().uppercase().take(3)) {
    "STR" -> Ability.STRENGTH
    "DEX" -> Ability.DEXTERITY
    "CON" -> Ability.CONSTITUTION
    "INT" -> Ability.INTELLIGENCE
    "WIS" -> Ability.WISDOM
    "CHA" -> Ability.CHARISMA
    else -> null
}
