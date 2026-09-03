package app.dulliesanddungeons.ui

import app.dulliesanddungeons.data.LocalStateStore
import app.dulliesanddungeons.data.PersistedAppState
import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CoreModifier
import app.dulliesanddungeons.domain.CoreStatistic
import app.dulliesanddungeons.domain.EffectActivation
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharacterStatEngineTest {
    @Test
    fun cloakOfProtectionAppliesOnlyWhileWornAndAttunedAndSurvivesRoundTrip() {
        val state = DndAppState(EffectTestStore())
        val starting = state.selectedCharacter!!
        val startingAc = starting.armorClass
        val startingSaves = starting.saves
        val cloak = state.knownItemCatalog().single {
            it.name == "Cloak of Protection" && it.compatibleWith(starting.ruleset)
        }

        assertTrue(state.addKnownItem(cloak))
        val added = state.selectedCharacter!!.resolvedEquipment.single { it.name == "Cloak of Protection" }
        assertEquals(startingAc, state.selectedCharacter!!.armorClass)

        state.toggleEquipmentEquipped(added.id)
        assertEquals(startingAc, state.selectedCharacter!!.armorClass)
        state.toggleEquipmentAttunement(added.id)

        val active = state.selectedCharacter!!
        assertEquals(startingAc + 1, active.armorClass)
        startingSaves.forEach { (ability, value) -> assertEquals(value + 1, active.saves[ability]) }
        val breakdown = CharacterStatEngine.armorClass(active)
        assertEquals(active.armorClass, breakdown.total)
        assertTrue(breakdown.sources.any { it.label == "Cloak of Protection" && it.active && it.amount == 1 })

        val restored = active.toDocument().toCharacterUi()
        assertEquals(active.armorClass, restored.armorClass)
        assertEquals(active.saves, restored.saves)
    }

    @Test
    fun customCarriedBonusesAndMagicWeaponBonusAreIncludedInEveryDerivedValue() {
        val state = DndAppState(EffectTestStore())
        val base = state.selectedCharacter!!
        state.addEquipment(
            EquipmentUi(
                id = "custom-ward",
                name = "Custom Ward",
                effects = listOf(
                    CoreModifier(CoreStatistic.ARMOR_CLASS, 1, activation = EffectActivation.CARRIED),
                    CoreModifier(CoreStatistic.SAVING_THROW, 1, activation = EffectActivation.CARRIED),
                ),
            ),
        )
        assertEquals(base.armorClass + 1, state.selectedCharacter!!.armorClass)
        base.saves.forEach { (ability, value) -> assertEquals(value + 1, state.selectedCharacter!!.saves[ability]) }

        val glaive = standardWeaponCatalog.single { it.id == "glaive" }
        state.addStandardWeapon(glaive, magicBonus = 1)
        val weapon = state.selectedCharacter!!.weapons.last()
        assertEquals("+1 Glaive", weapon.name)
        assertEquals(10, weapon.reachFeet)
        assertEquals(9, weapon.attackBonus)
        assertTrue(weapon.damage.endsWith("+ 5"))
    }

    @Test
    fun spellCatalogExposesConcreteCurrentDcAndAttackBonus() {
        val state = DndAppState(EffectTestStore())
        state.openCharacter("seed-wizard-5")
        val wizard = state.selectedCharacter!!
        val fireball = state.editableSpellCatalog().single { it.id == "spell.fireball" }
        val fireBolt = state.editableSpellCatalog().single { it.id == "spell.fire-bolt" }

        assertEquals(listOf(Ability.DEXTERITY), fireball.savingThrows.map { it.ability })
        assertEquals(15, CharacterStatEngine.difficultyClass(wizard, fireball.savingThrows.single().difficultyClass, fireball))
        assertTrue(fireBolt.spellAttack)
        assertEquals(7, CharacterStatEngine.spellAttackBonus(wizard, fireBolt))
    }

    @Test
    fun inventoryRemovalCanBeUndoneAtItsOriginalPosition() {
        val state = DndAppState(EffectTestStore())
        val before = state.selectedCharacter!!.weapons
        val removed = before.first()

        state.removeWeapon(removed.id)
        assertFalse(state.selectedCharacter!!.weapons.any { it.id == removed.id })
        assertTrue(state.inventoryFeedbackCanUndo)
        state.undoInventoryRemoval()

        assertEquals(before.map { it.id }, state.selectedCharacter!!.weapons.map { it.id })
        assertFalse(state.inventoryFeedbackCanUndo)
    }

    @Test
    fun featureAndConditionEffectsJoinEquipmentInTheSameSourcePipeline() {
        val character = DndAppState(EffectTestStore()).selectedCharacter!!
        val withFeature = character.copy(
            features = character.features + FeatureUi(
                id = "blessing",
                name = "Blessing",
                summary = "",
                effects = listOf(CoreModifier(CoreStatistic.SAVING_THROW, 2, ability = Ability.WISDOM)),
            ),
        )
        val condition = ConditionUi(
            name = "Guarded",
            source = "Test",
            duration = "",
            explanation = "",
            characterId = character.id,
            id = "guarded",
            effects = listOf(CoreModifier(CoreStatistic.ARMOR_CLASS, 1)),
        )

        val resolved = CharacterStatEngine.resolve(withFeature, listOf(condition))
        assertEquals(character.saves.getValue("Wisdom") + 2, resolved.saves.getValue("Wisdom"))
        assertEquals(character.armorClass + 1, resolved.armorClass)
        assertTrue(CharacterStatEngine.savingThrow(resolved, Ability.WISDOM, listOf(condition)).sources.any { it.label == "Blessing" })
    }

    @Test
    fun unsupportedSchemaStartsFreshAndNextWriteUsesCurrentSchema() {
        val original = DndAppState(EffectTestStore()).selectedCharacter!!.copy(name = "Migrated hero")
        val payload = Json.encodeToString(
            PersistedAppState(schemaVersion = 2, characters = listOf(original.toDocument())),
        )
        val store = EffectTestStore(payload)
        val restored = DndAppState(store)
        assertFalse(restored.characters.any { it.name == "Migrated hero" })

        restored.addEquipment(EquipmentUi("migration-token", "Migration Token"))
        val persisted = Json.decodeFromString<PersistedAppState>(store.storedValue!!)
        assertEquals(5, persisted.schemaVersion)
    }
}

private class EffectTestStore(initialValue: String? = null) : LocalStateStore {
    private var value: String? = initialValue
    val storedValue: String? get() = value

    override fun readState(): String? = value

    override fun writeState(value: String) {
        this.value = value
    }
}
