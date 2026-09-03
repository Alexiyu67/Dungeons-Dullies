package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.DerivedAttackGrant
import app.dulliesanddungeons.domain.DerivedAttackTrigger
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.WeaponCombatType
import app.dulliesanddungeons.domain.WeaponMatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DerivedAttackRulesTest {
    @Test
    fun everyStandardWeaponHasStructuredClassification() {
        standardWeaponCatalog.forEach { weapon ->
            assertEquals(weapon.id, weapon.classification.baseWeaponId)
            assertTrue(weapon.classification.propertyIds == normalizedWeaponPropertyIds(weapon.properties))
        }
    }

    @Test
    fun rulesetVariantsPreserveTheEditionSpecificLanceAndTridentData() {
        val lance = standardWeaponCatalog.first { it.id == "lance" }
        val trident = standardWeaponCatalog.first { it.id == "trident" }

        assertEquals("1d12", lance.forRuleset(Ruleset.Fifth2014).damage)
        assertFalse("heavy" in lance.forRuleset(Ruleset.Fifth2014).classification.propertyIds)
        assertEquals("1d10", lance.forRuleset(Ruleset.Fifth2024).damage)
        assertTrue("heavy" in lance.forRuleset(Ruleset.Fifth2024).classification.propertyIds)
        assertEquals("1d6", trident.forRuleset(Ruleset.Fifth2014).damage)
        assertEquals("1d8", trident.forRuleset(Ruleset.Fifth2024).damage)
        assertEquals("", trident.forRuleset(Ruleset.Fifth2014).mastery)
    }

    @Test
    fun reviewed2014PoleStrikeResolvesOnlyForItsListedWeapons() {
        val state = DndAppState()
        val grant = poleStrike2014()
        val character = assertNotNull(state.selectedCharacter).copy(
            ruleset = Ruleset.Fifth2014,
            abilities = mapOf("STR" to 18, "DEX" to 12, "CON" to 10, "INT" to 10, "WIS" to 10, "CHA" to 10),
            features = listOf(FeatureUi("private-polearm", "Polearm feature", "", attackGrants = listOf(grant))),
            weapons = listOf("glaive", "halberd", "quarterstaff", "spear", "pike").map(::weapon),
        )

        val resolvedIds = character.weapons.filter { state.derivedAttackOptions(character, it).isNotEmpty() }
            .map { it.definitionId }.toSet()
        assertEquals(setOf("glaive", "halberd", "quarterstaff", "spear"), resolvedIds)
        val option = state.derivedAttackOptions(character, character.weapons.first { it.definitionId == "glaive" }).single()
        assertEquals("1d4 + 4", option.weapon.damage)
        assertEquals("Bludgeoning", option.weapon.damageType)
        assertEquals(1, option.cost.bonusActions)
    }

    @Test
    fun anExistingCharacterCanResolveAGrantFromItsInstalledPrivateFeat() {
        val state = DndAppState()
        state.addPrivateEntry(PrivateEntryUi("polearm-feat", "feat", "Polearm feature", attackGrants = listOf(poleStrike2014())))
        val character = assertNotNull(state.selectedCharacter).copy(
            ruleset = Ruleset.Fifth2014,
            featIds = listOf("polearm-feat"),
            features = emptyList(),
            weapons = listOf(weapon("quarterstaff")),
        )

        assertEquals("Pole strike", state.derivedAttackOptions(character, character.weapons.single()).single().weapon.name)
    }

    @Test
    fun reviewed2024HeavyReachMatchIncludesPikeButNotWhip() {
        val state = DndAppState()
        val grant = DerivedAttackGrant(
            id = "pole-strike",
            name = "Pole strike",
            weaponMatch = WeaponMatch(
                baseWeaponIds = setOf("quarterstaff", "spear", "glaive", "halberd", "lance", "pike"),
            ),
            trigger = DerivedAttackTrigger.AFTER_ATTACK_WITH_MATCHING_WEAPON,
            cost = ActionCost(bonusActions = 1),
            damageDice = DiceExpression(1, 4),
            damageType = "Bludgeoning",
        )
        val character = assertNotNull(state.selectedCharacter).copy(
            ruleset = Ruleset.Fifth2024,
            features = listOf(FeatureUi("private-polearm", "Polearm feature", "", attackGrants = listOf(grant))),
            weapons = listOf("pike", "whip").map(::weapon),
        )

        assertTrue(state.derivedAttackOptions(character, character.weapons.first { it.definitionId == "pike" }).isNotEmpty())
        assertTrue(state.derivedAttackOptions(character, character.weapons.first { it.definitionId == "whip" }).isEmpty())
    }

    @Test
    fun lightPropertyCreatesAChildAttackAndTracksItsPrerequisite() {
        val state = DndAppState()
        val character = assertNotNull(state.selectedCharacter).copy(
            ruleset = Ruleset.Fifth2024,
            features = emptyList(),
            weapons = listOf(weapon("dagger"), weapon("scimitar")),
        )
        val daggerOption = state.derivedAttackOptions(character, character.weapons.first()).single()
        val session = TurnSession(character)

        assertFalse(daggerOption.isAvailableIn(session, character))
        session.commitAttack(character.weapons.last().id)
        assertTrue(daggerOption.isAvailableIn(session, character))
    }

    @Test
    fun monkFlurryUsesTheMartialArtsDieAndOpenHandDetails() {
        val state = DndAppState()
        val character = assertNotNull(state.selectedCharacter).copy(
            ruleset = Ruleset.Fifth2024,
            className = "Monk",
            level = 10,
            progression = emptyList(),
            features = listOf(FeatureUi("focus-points", "Focus Points", "", 10, 10), FeatureUi("open-hand", "Open Hand Technique", "")),
            weapons = listOf(weapon("quarterstaff")),
        )
        val options = state.derivedAttackOptions(character, character.weapons.single())
        val flurry = options.first { it.sourceId == "flurry-of-blows" }

        assertEquals(3, flurry.attackCount)
        assertTrue(flurry.weapon.damage.startsWith("1d8"))
        assertTrue(flurry.details.contains("Open Hand"))
        assertEquals(1, flurry.cost.resources["focus-points"])
    }

    @Test
    fun classificationAndAttackGrantsSurviveCharacterProjection() {
        val state = DndAppState()
        val grant = poleStrike2014()
        val original = assertNotNull(state.selectedCharacter).copy(
            ruleset = Ruleset.Fifth2014,
            weapons = listOf(weapon("glaive")),
            features = listOf(FeatureUi("private-polearm", "Polearm feature", "", attackGrants = listOf(grant))),
        )

        val restored = original.toDocument().toCharacterUi()

        assertEquals("glaive", restored.weapons.single().classification.baseWeaponId)
        assertEquals(grant, restored.features.single().attackGrants.single())
    }

    @Test
    fun selectedDerivedAttackSurvivesTurnDraftProjection() {
        val character = assertNotNull(DndAppState().selectedCharacter)
        val snapshot = TurnSessionSnapshotUi(
            characterId = character.id,
            baseline = character,
            selectedWeaponId = character.weapons.first().id,
            selectedAttackOptionId = "derived-option",
        )
        val draft = snapshot.toDomainTurnDraft(character.toDocument(), emptyList())
        val restored = character.toDocument().copy(state = character.toDocument().state.copy(activeTurn = draft)).toTurnSessionSnapshotUi()

        assertEquals("derived-option", restored?.selectedAttackOptionId)
    }

    private fun poleStrike2014() = DerivedAttackGrant(
        id = "pole-strike",
        name = "Pole strike",
        weaponMatch = WeaponMatch(baseWeaponIds = setOf("glaive", "halberd", "quarterstaff", "spear")),
        trigger = DerivedAttackTrigger.AFTER_ATTACK_WITH_MATCHING_WEAPON,
        cost = ActionCost(bonusActions = 1),
        damageDice = DiceExpression(1, 4),
        damageType = "Bludgeoning",
    )

    private fun weapon(id: String): WeaponUi {
        val template = standardWeaponCatalog.first { it.id == id }
        return WeaponUi(
            id = "owned-$id",
            name = template.name,
            attackBonus = 6,
            damage = "${template.damage} + 4",
            damageType = template.damageType,
            properties = template.properties,
            ability = template.ability,
            itemBonus = 0,
            damageAbility = template.ability,
            definitionId = id,
            reachFeet = if ("reach" in template.classification.propertyIds) 10 else 5,
            classification = template.classification.copy(combatType = template.classification.combatType.takeUnless { id == "unarmed" } ?: WeaponCombatType.UNARMED),
        )
    }
}
