package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.ArmorCategory
import app.dulliesanddungeons.domain.ArmorDefinition
import app.dulliesanddungeons.domain.AttunementState
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.EquipmentLocation
import app.dulliesanddungeons.domain.EquipmentSlot

data class ArmorCalculation(
    val armorClass: Int,
    val armorId: String? = null,
    val shieldIds: List<String> = emptyList(),
    val speedPenaltyFeet: Int = 0,
    val stealthDisadvantage: Boolean = false,
    val untrainedItemIds: Set<String> = emptySet(),
    val inactiveMagicItemIds: Set<String> = emptySet(),
) {
    val trainingMissing: Boolean get() = untrainedItemIds.isNotEmpty()
}

object ArmorRules {
    fun fiveEdition(
        build: CharacterBuild,
        state: CharacterState,
        definitions: Map<String, ArmorDefinition>,
        armorTrainingIds: Set<String>,
        unarmoredBase: Int = 10,
    ): ArmorCalculation {
        require(build.ruleset.isFiveEdition)
        val dexterity = DerivedStatRules.abilityModifier(build.abilities.getOrDefault(Ability.DEXTERITY, 10))
        val wornArmor = state.equipment.filter {
            it.location == EquipmentLocation.WORN && it.slot == EquipmentSlot.ARMOR
        }
        require(wornArmor.size <= 1) { "Only one suit of armor can be worn" }
        val armorItem = wornArmor.singleOrNull()
        val armor = armorItem?.let { definitions[it.definitionId] }
        require(armor == null || armor.ruleset == build.ruleset) { "Armor definition uses another ruleset" }
        val armorIsInactive = armorItem != null && armor?.attunementRequired == true &&
            armorItem.attunement != AttunementState.ATTUNED
        val activeArmorBase = armor?.baseArmorClass?.minus(armor.magicBonus.takeIf { armorIsInactive } ?: 0)
        val base = if (armor == null || armor.category == ArmorCategory.UNARMORED) {
            unarmoredBase + dexterity
        } else {
            requireNotNull(activeArmorBase) + when {
                armor.dexterityCap == null -> dexterity
                else -> dexterity.coerceAtMost(armor.dexterityCap)
            }
        }
        val shields = state.equipment.filter {
            it.location == EquipmentLocation.WIELDED && it.slot == EquipmentSlot.SHIELD
        }
        require(shields.size <= 1) { "Only one shield can grant an Armor Class bonus" }
        require(shields.all { definitions[it.definitionId]?.ruleset == build.ruleset }) {
            "Shield definition is missing or uses another ruleset"
        }
        val untrainedItemIds = buildSet {
            if (armorItem != null && armor != null && armor.requiresTraining && armor.trainingId !in armorTrainingIds) {
                add(armorItem.id)
            }
            shields.mapNotNullTo(this) { item ->
                definitions[item.definitionId]
                    ?.takeIf { it.requiresTraining && it.trainingId !in armorTrainingIds }
                    ?.let { item.id }
            }
        }
        val shieldBonus = shields.sumOf { item ->
            val definition = definitions[item.definitionId] ?: return@sumOf 0
            val untrainedIn2024 = build.ruleset == app.dulliesanddungeons.domain.RulesetId.FIFTH_EDITION_2024 &&
                item.id in untrainedItemIds
            if (untrainedIn2024) return@sumOf 0
            val inactive = definition.attunementRequired && item.attunement != AttunementState.ATTUNED
            definition.baseArmorClass - (definition.magicBonus.takeIf { inactive } ?: 0)
        }
        val strength = build.abilities.getOrDefault(Ability.STRENGTH, 10)
        val speedPenalty = armor?.strengthRequirement?.takeIf { strength < it }?.let { 10 } ?: 0
        val inactiveMagic = (wornArmor + shields).filterTo(mutableSetOf()) { item ->
            definitions[item.definitionId]?.attunementRequired == true && item.attunement != AttunementState.ATTUNED
        }.mapTo(mutableSetOf()) { it.id }
        return ArmorCalculation(
            armorClass = base + shieldBonus,
            armorId = armorItem?.id,
            shieldIds = shields.map { it.id },
            speedPenaltyFeet = speedPenalty,
            stealthDisadvantage = armor?.stealthDisadvantage == true,
            untrainedItemIds = untrainedItemIds,
            inactiveMagicItemIds = inactiveMagic,
        )
    }

    fun canAttune(state: CharacterState, itemId: String, maximumAttuned: Int = 3): Boolean {
        val item = state.equipment.firstOrNull { it.id == itemId } ?: return false
        if (item.attunement != AttunementState.UNATTUNED) return false
        return state.equipment.count { it.attunement == AttunementState.ATTUNED } < maximumAttuned
    }
}
