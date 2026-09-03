package app.dulliesanddungeons.ui

import app.dulliesanddungeons.data.LocalStateStore
import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CoinDenomination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ItemCatalogAndCreationTest {
    @Test
    fun languageSuggestionsReplaceOnlyTheActiveSegmentAndKeepCustomInputPossible() {
        val suggestions = languageSuggestions(
            input = "Common, el",
            options = listOf("Common", "Elvish", "Celestial", "Dwarvish"),
        )

        assertEquals(listOf("Elvish", "Celestial"), suggestions)
        assertEquals("Common, Elvish", replaceActiveLanguageSegment("Common, el", "Elvish"))
        assertEquals(emptyList(), languageSuggestions("Common, Elvish", listOf("Common", "Elvish")))
    }

    @Test
    fun catalogFiltersCompatibilityTypeRarityAndSearch() {
        val catalog = builtInKnownItemCatalog()

        val pf2Default = filterKnownItems(catalog, Ruleset.Pf2eRemaster, UiLanguage.English)
        val pf2WithIncompatible = filterKnownItems(
            catalog,
            Ruleset.Pf2eRemaster,
            UiLanguage.English,
            query = "plate",
            includeIncompatible = true,
            sort = KnownItemSort.Rarity,
        )
        val armorOnly = filterKnownItems(
            catalog,
            Ruleset.Fifth2024,
            UiLanguage.English,
            types = setOf(KnownItemType.Armor),
            rarities = setOf(ItemRarity.Mundane),
        )

        assertTrue(pf2Default.all { it.compatibleWith(Ruleset.Pf2eRemaster) })
        assertTrue(pf2WithIncompatible.any { it.name == "Plate Armor" && !it.compatibleWith(Ruleset.Pf2eRemaster) })
        assertTrue(armorOnly.isNotEmpty())
        assertTrue(armorOnly.all { it.type == KnownItemType.Armor && it.rarity == ItemRarity.Mundane })
    }

    @Test
    fun privateItemsWeaponsAndLanguagesBecomeTypedCatalogOptions() {
        val state = DndAppState(ItemTestStore())
        state.addPrivateEntry(PrivateEntryUi("moon-cloak", "Item", "Moon Cloak", "A silver travel cloak", "type=armor; ac=15; rarity=rare; ruleset=2024"))
        state.addPrivateEntry(PrivateEntryUi("star-blade", "Weapon", "Star Blade", "A luminous blade", "damage=1d8; damage type=radiant; ability=DEX; rarity=uncommon; ruleset=2024"))
        state.addPrivateEntry(PrivateEntryUi("old-spear", "Weapon", "Old Spear", "Its damage is unknown", "ruleset=2024"))
        state.addPrivateEntry(PrivateEntryUi("lunar", "Language", "Lunar", "Moonfolk speech", "ruleset=2024"))
        state.beginCreate()

        val catalog = state.knownItemCatalog()
        val cloak = catalog.single { it.name == "Moon Cloak" }
        val blade = catalog.single { it.name == "Star Blade" }
        val spear = catalog.single { it.name == "Old Spear" }

        assertEquals(KnownItemType.Armor, cloak.type)
        assertEquals(ItemRarity.Rare, cloak.rarity)
        assertEquals(15, cloak.equipment?.armorClass)
        assertTrue(blade.complete)
        assertEquals("radiant", blade.weapon?.damageType)
        assertFalse(spear.complete)
        assertTrue("Lunar" in state.creationLanguageOptions())
    }

    @Test
    fun knownCustomShieldAndUnarmoredChoicesDrivePreviewAndFinishedCharacter() {
        val state = DndAppState(ItemTestStore())
        state.beginCreate()
        state.creation.name = "Armor Tester"
        state.creation.className = "Fighter"
        state.creation.statMethod = StatMethod.Manual
        state.creation.manualAbilities.putAll(mapOf("STR" to 16, "DEX" to 14, "CON" to 14, "INT" to 10, "WIS" to 10, "CHA" to 8))

        val plate = state.knownItemCatalog().single { it.name == "Plate Armor" }
        state.selectCreationArmor(plate)
        assertEquals(18, state.creationPreview().armorClass)
        assertEquals("Plate Armor", state.creationPreview().startingArmor)

        state.setCustomCreationArmor(EquipmentUi("", "Tower Shield", EquipmentKind.ARMOR, shieldBonus = 3))
        assertEquals(15, state.creationPreview().armorClass)

        state.creation.startingArmorChoice = StartingArmorChoice.Unarmored
        assertEquals(12, state.creationPreview().armorClass)

        state.creation.startingArmorChoice = StartingArmorChoice.Known(plate.id)
        state.finishCreateWithRequiredProficiencies()
        val created = assertNotNull(state.selectedCharacter)
        val armor = created.resolvedEquipment.single { it.kind == EquipmentKind.ARMOR }
        assertEquals(18, created.armorClass)
        assertEquals("Plate Armor", armor.name)
        assertTrue(armor.worn)
    }

    @Test
    fun incompatibleArmorRemainsSelectableAndExplained() {
        val state = DndAppState(ItemTestStore())
        state.beginCreate()
        state.creation.ruleset = Ruleset.Pf2eRemaster
        val plate = state.knownItemCatalog().single { it.name == "Plate Armor" }

        state.selectCreationArmor(plate)

        assertNotNull(state.creationArmorAdvisory())
        assertTrue(state.creationPreview().armorClass > 0)
    }

    @Test
    fun officialCatalogContainsBothEditionWeaponAndMagicItemCoverage() {
        assertEquals(242, srdMagicItemCatalog.count { it.revision == SrdItemRevision.SRD_5_1 })
        assertEquals(258, srdMagicItemCatalog.count { it.revision == SrdItemRevision.SRD_5_2_1 })
        assertEquals(37, standardWeaponCatalog.count { Ruleset.Fifth2014 in it.supportedRulesets })
        assertEquals(38, standardWeaponCatalog.count { Ruleset.Fifth2024 in it.supportedRulesets })
        assertTrue(standardEquipmentCatalog.any { it.id == "padded-armor" })
        assertTrue(standardEquipmentCatalog.any { it.id == "hide-armor" })

        val cloak = builtInKnownItemCatalog().single {
            it.name == "Cloak of Protection" && it.compatibleWith(Ruleset.Fifth2024)
        }.equipment
        assertNotNull(cloak)
        assertEquals(2, cloak.effects.size)
        assertTrue(cloak.needsAttunement)

        val maul = builtInKnownItemCatalog().single {
            it.name == "Maul" && it.compatibleWith(Ruleset.Fifth2024)
        }.weapon
        assertNotNull(maul)
        assertEquals(Ability.CONSTITUTION, maul.savingThrows.single().ability)
        assertTrue(maul.savingThrows.single().difficultyClass.addProficiency)
    }

    @Test
    fun fighter2024StartingGearPackagesCanBeSelectedAndPersisted() {
        val state = DndAppState(ItemTestStore())
        state.beginCreate()
        state.creation.name = "Gear Tester"
        state.creation.className = "Fighter"
        state.creation.ruleset = Ruleset.Fifth2024
        state.creation.statMethod = StatMethod.Manual
        state.creation.manualAbilities.putAll(mapOf("STR" to 16, "DEX" to 14, "CON" to 14, "INT" to 10, "WIS" to 10, "CHA" to 8))

        val packages = state.creationGearPackages()
        assertEquals(listOf("fighter-a", "fighter-b", "fighter-gold"), packages.map { it.id })
        assertEquals(155, packages.single { it.id == "fighter-gold" }.goldPieces)
        state.selectCreationGearPackage("fighter-b")

        assertEquals(3, state.creation.startingWeapons.size)
        assertEquals("studded-leather", (state.creation.startingArmorChoice as StartingArmorChoice.Known).itemId.removePrefix("equipment:"))
        assertEquals(11, state.creation.startingGoldPieces)
        state.finishCreateWithRequiredProficiencies()

        val created = state.selectedCharacter!!
        assertTrue(created.weapons.any { it.name == "Longbow" })
        assertTrue(created.resolvedEquipment.any { it.name == "Studded Leather Armor" && it.worn })
        assertEquals(11, created.currency.balance(CoinDenomination.GOLD))
        assertFalse(created.resolvedEquipment.any { it.definitionId == "gold-pieces" })
    }

    @Test
    fun currencyCatalogSearchesNamesAliasesAndRulesetDenominations() {
        val catalog = builtInKnownItemCatalog()

        val gold = filterKnownItems(catalog, Ruleset.Fifth2024, UiLanguage.English, query = "Gold").single {
            it.currencyDenomination == CoinDenomination.GOLD
        }
        assertEquals(KnownItemType.Currency, gold.type)
        assertTrue(filterKnownItems(catalog, Ruleset.Fifth2024, UiLanguage.English, query = "GP").any { it.id == gold.id })
        assertTrue(filterKnownItems(catalog, Ruleset.Fifth2024, UiLanguage.German, query = "Silber").any {
            it.currencyDenomination == CoinDenomination.SILVER
        })
        assertTrue(filterKnownItems(catalog, Ruleset.Fifth2024, UiLanguage.English, query = "Electrum").any {
            it.currencyDenomination == CoinDenomination.ELECTRUM
        })
        assertFalse(filterKnownItems(catalog, Ruleset.Pf2eRemaster, UiLanguage.English, query = "Electrum").any {
            it.currencyDenomination == CoinDenomination.ELECTRUM
        })
    }
}

private class ItemTestStore : LocalStateStore {
    private var state: String? = null

    override fun readState(): String? = state

    override fun writeState(value: String) {
        state = value
    }
}
