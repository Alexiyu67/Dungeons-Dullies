package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.ArmorCategory
import app.dulliesanddungeons.domain.ArmorDefinition
import app.dulliesanddungeons.domain.AttunementState
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.ClassLevel
import app.dulliesanddungeons.domain.EquipmentItem
import app.dulliesanddungeons.domain.EquipmentLocation
import app.dulliesanddungeons.domain.EquipmentSlot
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.RulesetId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArmorRulesTest {
    @Test
    fun mediumArmorCapsDexterityAndUntrained2024ShieldGivesNoBonus() {
        val build = build(RulesetId.FIFTH_EDITION_2024)
        val state = state(
            EquipmentItem("armor-1", "half_plate", "Half Plate", location = EquipmentLocation.WORN, slot = EquipmentSlot.ARMOR),
            EquipmentItem("shield-1", "shield", "Shield", location = EquipmentLocation.WIELDED, slot = EquipmentSlot.SHIELD),
        )

        val result = ArmorRules.fiveEdition(build, state, definitions(build.ruleset), setOf("armor:medium"))

        assertEquals(17, result.armorClass)
        assertEquals(setOf("shield-1"), result.untrainedItemIds)
        assertTrue(result.stealthDisadvantage)
    }

    @Test
    fun untrained2014ShieldStillProvidesAcButReportsTrainingProblem() {
        val build = build(RulesetId.FIFTH_EDITION_2014)
        val state = state(
            EquipmentItem("armor-1", "half_plate", "Half Plate", location = EquipmentLocation.WORN, slot = EquipmentSlot.ARMOR),
            EquipmentItem("shield-1", "shield", "Shield", location = EquipmentLocation.WIELDED, slot = EquipmentSlot.SHIELD),
        )

        val result = ArmorRules.fiveEdition(build, state, definitions(build.ruleset), setOf("armor:medium"))

        assertEquals(19, result.armorClass)
        assertTrue(result.trainingMissing)
    }

    @Test
    fun unattunedMagicBonusIsInactiveUntilAttuned() {
        val build = build(RulesetId.FIFTH_EDITION_2024)
        val unattuned = state(
            EquipmentItem(
                "shield-1",
                "magic_shield",
                "+1 Shield",
                location = EquipmentLocation.WIELDED,
                slot = EquipmentSlot.SHIELD,
                attunement = AttunementState.UNATTUNED,
            ),
        )

        val result = ArmorRules.fiveEdition(build, unattuned, definitions(build.ruleset), setOf("armor:shield"))

        assertEquals(15, result.armorClass)
        assertEquals(setOf("shield-1"), result.inactiveMagicItemIds)
        assertTrue(ArmorRules.canAttune(unattuned, "shield-1"))
        assertFalse(ArmorRules.canAttune(unattuned, "missing"))
    }

    private fun build(ruleset: RulesetId) = CharacterBuild(
        id = "hero",
        name = "Hero",
        ruleset = ruleset,
        rules = FiveEBuildData(
            ancestryId = "human",
            backgroundId = "guard",
            classes = listOf(ClassLevel("fighter", 5)),
            abilities = Ability.entries.associateWith { if (it == Ability.DEXTERITY) 16 else 10 },
        ),
    )

    private fun state(vararg equipment: EquipmentItem) = CharacterState(
        characterId = "hero",
        currentHitPoints = 30,
        maximumHitPoints = 30,
        equipment = equipment.toList(),
    )

    private fun definitions(ruleset: RulesetId) = listOf(
        ArmorDefinition(
            "half_plate",
            "Half Plate",
            ruleset,
            ArmorCategory.MEDIUM,
            baseArmorClass = 15,
            dexterityCap = 2,
            stealthDisadvantage = true,
            donSeconds = 300,
            doffSeconds = 60,
            trainingId = "armor:medium",
        ),
        ArmorDefinition(
            "shield",
            "Shield",
            ruleset,
            ArmorCategory.SHIELD,
            baseArmorClass = 2,
            donSeconds = 6,
            doffSeconds = 6,
            trainingId = "armor:shield",
        ),
        ArmorDefinition(
            "magic_shield",
            "+1 Shield",
            ruleset,
            ArmorCategory.SHIELD,
            baseArmorClass = 3,
            donSeconds = 6,
            doffSeconds = 6,
            trainingId = "armor:shield",
            attunementRequired = true,
            magicBonus = 1,
        ),
    ).associateBy { it.id }
}
