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

internal fun DndAppState.finishCreateWithRequiredProficiencies() {
    completeRequiredCreationProficiencies()
    finishCreate()
}
