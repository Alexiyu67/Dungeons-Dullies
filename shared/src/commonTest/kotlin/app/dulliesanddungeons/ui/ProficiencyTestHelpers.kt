package app.dulliesanddungeons.ui

internal fun DndAppState.completeRequiredCreationProficiencies() {
    if (selectedCreationBackground() == null) {
        selectCreationBackground(creationBackgroundOptions().first().id)
    }
    while (creation.backgroundSkillIds.size < creationBackgroundSkillCount()) {
        val next = creationRankSkillOptions().first { it.id !in creation.backgroundSkillIds }
        toggleCreationBackgroundSkill(next.id)
    }
    while (creation.classSkillIds.size < creationClassSkillCount()) {
        val next = creationClassSkillOptions().first { it.id !in creation.classSkillIds }
        toggleCreationClassSkill(next.id)
    }
    while (creation.featSkillIds.size < creationFeatSkillCount()) {
        val next = creationFeatSkillOptions().first { it.id !in creation.featSkillIds }
        toggleCreationFeatSkill(next.id)
    }
    var guard = 0
    while (creationSkillIncreaseCost() < creationSkillIncreaseCount() && guard++ < 100) {
        val before = creationSkillIncreaseCost()
        creationRankSkillOptions().firstOrNull { skill ->
            cycleCreationSkillRank(skill.id)
            creationSkillIncreaseCost() > before
        } ?: error("No legal skill rank increase is available")
    }
    check(creationProficiencySelectionValid())
}

internal fun DndAppState.completeRequiredCreationGear() {
    if (creationGearSelectionValid()) return
    val armorId = when (creation.ruleset) {
        Ruleset.Pf2eRemaster -> when (creation.className) {
            "Champion" -> "pf2e-half-plate"
            "Fighter" -> "pf2e-scale-mail"
            "Cleric" -> "pf2e-chain-shirt"
            "Rogue", "Ranger", "Bard", "Druid", "Alchemist" -> "pf2e-leather-armor"
            else -> null
        }
        else -> when (creation.className) {
            "Fighter", "Paladin" -> "chain-mail"
            "Cleric", "Ranger" -> "scale-mail"
            "Rogue", "Bard", "Druid", "Warlock" -> "leather-armor"
            else -> null
        }
    }
    creation.startingArmorChoice = armorId
        ?.let { StartingArmorChoice.Known("equipment:$it") }
        ?: StartingArmorChoice.Unarmored
    check(creationGearSelectionValid())
}

internal fun DndAppState.completeRequiredCreationSpells() {
    val selection = creationSpellSelection() ?: return
    val selectedIds = selection.selected.mapTo(mutableSetOf()) { it.id }
    selection.options.filter { it.level == 0 }
        .filterNot { it.id in selectedIds }
        .take(selection.cantripLimit - selection.selected.count { it.level == 0 })
        .forEach { toggleCreationSpell(it.id) }
    selection.options.filter { it.level > 0 }
        .filterNot { it.id in selectedIds }
        .take(selection.leveledSpellLimit - selection.selected.count { it.level > 0 })
        .forEach { toggleCreationSpell(it.id) }
    check(creationSpellSelectionValid())
}

internal fun DndAppState.finishCreateWithRequiredProficiencies() {
    completeRequiredCreationProficiencies()
    completeRequiredCreationSpells()
    completeRequiredCreationGear()
    finishCreate()
}
