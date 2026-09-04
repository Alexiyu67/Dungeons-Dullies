package app.dulliesanddungeons.ui

import app.dulliesanddungeons.data.LocalStateStore
import app.dulliesanddungeons.data.PersistedAppState
import app.dulliesanddungeons.domain.ProficiencyRank
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProficiencyFlowTest {
    @Test
    fun creationRequiresBackgroundAndSkills() {
        val state = DndAppState(ProficiencyStore())
        val countBefore = state.characters.size
        state.beginCreate()
        state.creation.name = "Incomplete"

        state.finishCreate()

        assertEquals(countBefore, state.characters.size)
        assertFalse(state.creationProficiencySelectionValid())
        assertEquals("Complete proficiency choices", state.infoTitle)
        assertEquals(CreationStep.Details.ordinal, state.creation.step)
    }

    @Test
    fun creationGuidesReviewToTheEarliestIncompleteStep() {
        val state = DndAppState(ProficiencyStore())
        val countBefore = state.characters.size
        state.beginCreate()
        state.creation.step = CreationStep.Review.ordinal

        state.finishCreate()

        assertEquals(countBefore, state.characters.size)
        assertEquals(CreationStep.Identity.ordinal, state.creation.step)
        assertEquals("Add a name", state.infoTitle)

        state.creation.name = "Pathfinder"
        state.creation.step = CreationStep.Review.ordinal
        state.finishCreate()

        assertEquals(countBefore, state.characters.size)
        assertEquals(CreationStep.Details.ordinal, state.creation.step)
        assertEquals("Complete proficiency choices", state.infoTitle)

        state.completeRequiredCreationProficiencies()
        state.creation.step = CreationStep.Review.ordinal
        state.finishCreate()

        assertEquals(countBefore, state.characters.size)
        assertEquals(CreationStep.Gear.ordinal, state.creation.step)
        assertEquals("Choose starting gear", state.infoTitle)

        state.completeRequiredCreationGear()
        state.creation.step = CreationStep.Review.ordinal
        state.finishCreate()

        assertEquals(countBefore + 1, state.characters.size)
    }

    @Test
    fun fifthEditionSelectionsDriveEverySkillSaveWeaponAndRollSurfaceAndPersist() {
        val store = ProficiencyStore()
        val state = DndAppState(store)
        state.beginCreate()
        state.creation.name = "Mara"
        state.creation.statMethod = StatMethod.Manual
        state.creation.manualAbilities.putAll(
            mapOf("STR" to 16, "DEX" to 14, "CON" to 14, "INT" to 10, "WIS" to 12, "CHA" to 10),
        )
        state.selectCreationBackground("background:soldier")

        assertTrue(state.creationClassSkillOptions().none { it.id == "skill:athletics" })
        state.creationClassSkillOptions().take(2).forEach { state.toggleCreationClassSkill(it.id) }
        assertTrue(state.creationProficiencySelectionValid())
        state.completeRequiredCreationGear()
        state.finishCreate()

        val character = assertNotNull(state.selectedCharacter)
        assertEquals(18, character.skills.size)
        assertEquals(5, character.skills["Athletics"])
        assertEquals(2, character.skills["Intimidation"])
        assertEquals(5, character.saves["Strength"])
        assertEquals(4, character.saves["Constitution"])
        assertEquals(5, character.weapons.single().attackBonus)
        assertEquals(ProficiencyRank.TRAINED, character.proficiencyRanks["skill:athletics"])
        assertEquals(18, state.availableQuickRolls(character).count { it.kind == QuickRollKind.SKILL })
        assertTrue(state.search("Arcana").any { it.id == "skill-Arcana" })

        val restored = DndAppState(store).characters.single { it.name == "Mara" }
        assertEquals("background:soldier", restored.backgroundId)
        assertEquals(character.skills, restored.skills)
        assertEquals(character.proficiencyRanks, restored.proficiencyRanks)
    }

    @Test
    fun incompletePersistedSkillMapsAreCompletedWithoutLosingAuthoredSkills() {
        val source = assertNotNull(DndAppState(ProficiencyStore()).selectedCharacter)
        val document = source.toDocument()
        val athletics = document.sheet.combat.skills.getValue("Athletics")
        val incomplete = document.copy(
            sheet = document.sheet.copy(
                combat = document.sheet.combat.copy(
                    skills = linkedMapOf(
                        "Athletics" to athletics,
                        "Siege Lore" to athletics.copy(override = 6, storedValue = 6),
                    ),
                ),
            ),
        )
        val store = ProficiencyStore(Json.encodeToString(PersistedAppState(characters = listOf(incomplete))))

        val state = DndAppState(store)
        val restored = assertNotNull(state.selectedCharacter)

        assertEquals(ProficiencyCatalog.fiveESkills.map { it.englishName }, restored.skills.keys.take(18))
        assertEquals(19, restored.skills.size)
        assertEquals(8, restored.skills["Athletics"])
        assertEquals(6, restored.skills["Siege Lore"])
        assertTrue("Investigation" in restored.skills)
        assertTrue("Persuasion" in restored.skills)
        assertTrue(state.search("Persuasion").any { it.id == "skill-Persuasion" })

        state.toggleLanguage()
        val persisted = assertNotNull(DndAppState(store).selectedCharacter)
        assertEquals(restored.skills, persisted.skills)
    }

    @Test
    fun pf2eHigherLevelCreationAssignsAllRanksAndLore() {
        val state = DndAppState(ProficiencyStore())
        state.beginCreate()
        state.selectCreationRuleset(Ruleset.Pf2eRemaster)
        state.creation.name = "Tova"
        state.setCreationLevel(15)
        state.creation.statMethod = StatMethod.Manual
        state.creation.manualAbilities.putAll(
            mapOf("STR" to 18, "DEX" to 14, "CON" to 14, "INT" to 12, "WIS" to 12, "CHA" to 10),
        )
        state.completeRequiredCreationProficiencies()

        assertEquals(7, state.creationSkillIncreaseCount())
        assertEquals(7, state.creationSkillIncreaseCost())
        assertTrue(state.creationProficiencyRanks().values.any { it == ProficiencyRank.LEGENDARY })
        state.completeRequiredCreationGear()
        state.finishCreate()

        val character = assertNotNull(state.selectedCharacter)
        assertEquals(18, character.skills.size)
        assertTrue("Guild Lore" in character.skills)
        assertEquals(ProficiencyRank.TRAINED, character.proficiencyRanks["skill:lore:guild"])
        val legendary = character.proficiencyRanks.entries.first { it.value == ProficiencyRank.LEGENDARY }.key
        val skill = assertNotNull(ProficiencyCatalog.skill(Ruleset.Pf2eRemaster, legendary, character.backgroundId))
        val abilityModifier = ((character.abilities.getValue(skill.ability) - 10) / 2)
        assertEquals(abilityModifier + character.level + 8, character.skills[skill.englishName])
        assertEquals(ProficiencyRank.EXPERT, character.proficiencyRanks["weapon:martial"])
        assertEquals(4 + character.level + 4, character.weapons.single().attackBonus)
    }

    @Test
    fun pf2eLevelUpRequiresAndAppliesTheEarnedSkillIncrease() {
        val store = ProficiencyStore()
        val state = DndAppState(store)
        state.beginCreate()
        state.selectCreationRuleset(Ruleset.Pf2eRemaster)
        state.selectCreationClass("Rogue")
        state.creation.name = "Nim"
        state.completeRequiredCreationProficiencies()
        state.completeRequiredCreationGear()
        state.finishCreate()
        val before = assertNotNull(state.selectedCharacter)

        state.beginLevelUp()
        state.selectLevelUpFeat("tough")
        val choice = state.levelUpGuidedChoices().single { it.kind == GuidedLevelChoiceKind.PROFICIENCY }
        val option = choice.options.first()
        state.toggleLevelUpGuidedOption(choice.id, option.id)
        assertTrue(state.levelUpGuidedChoicesValid())
        assertTrue(state.applyLevelUp())

        val after = assertNotNull(state.selectedCharacter)
        assertEquals(2, after.level)
        assertEquals(option.proficiencyRank, after.proficiencyRanks[option.proficiencyId])
        assertTrue(after.skills.getValue(option.name) > before.skills.getValue(option.name))
        val restored = DndAppState(store).characters.single { it.name == "Nim" }
        assertEquals(after.skills, restored.skills)
        assertEquals(after.proficiencyRanks, restored.proficiencyRanks)
    }

    @Test
    fun skilledFeatLevelUpAddsThreeCalculatedProficiencies() {
        val state = DndAppState(ProficiencyStore())
        state.beginCreate()
        state.creation.name = "Iria"
        state.setCreationLevel(3)
        val subclass = state.creationSubclassOptions().first()
        state.selectCreationSubclass(subclass.id)
        state.completeRequiredCreationProficiencies()
        state.completeRequiredCreationGear()
        state.finishCreate()
        val previousRanks = assertNotNull(state.selectedCharacter).proficiencyRanks

        state.beginLevelUp()
        state.selectLevelUpFeat("skilled")
        val choice = state.levelUpGuidedChoices().single { it.id == "skilled-proficiencies" }
        choice.options.take(3).forEach { state.toggleLevelUpGuidedOption(choice.id, it.id) }
        assertTrue(state.applyLevelUp())

        val character = assertNotNull(state.selectedCharacter)
        val added = character.proficiencyRanks.keys - previousRanks.keys
        assertEquals(3, added.size)
        added.forEach { assertEquals(ProficiencyRank.TRAINED, character.proficiencyRanks[it]) }
    }

    @Test
    fun catalogsUseStableUniqueIdsAndBilingualLabels() {
        assertEquals(18, ProficiencyCatalog.fiveESkills.size)
        assertEquals(17, ProficiencyCatalog.pf2eSkills.size)
        Ruleset.entries.forEach { ruleset ->
            val skills = ProficiencyCatalog.skills(ruleset)
            assertEquals(skills.size, skills.map { it.id }.distinct().size)
            assertTrue(skills.all { it.id.startsWith("skill:") && it.englishName.isNotBlank() && it.germanName.isNotBlank() })
            val backgrounds = ProficiencyCatalog.backgrounds(ruleset)
            assertEquals(backgrounds.size, backgrounds.map { it.id }.distinct().size)
            assertTrue(backgrounds.all { it.id.startsWith("background:") && it.englishName.isNotBlank() && it.germanName.isNotBlank() })
        }
    }
}

private class ProficiencyStore(private var stored: String? = null) : LocalStateStore {
    override fun readState(): String? = stored
    override fun writeState(value: String) {
        stored = value
    }
}
