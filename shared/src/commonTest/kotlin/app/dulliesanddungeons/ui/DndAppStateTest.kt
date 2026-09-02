package app.dulliesanddungeons.ui

import app.dulliesanddungeons.data.LocalStateStore
import app.dulliesanddungeons.data.PersistedAppState
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.RollMode
import app.dulliesanddungeons.domain.RecoveryAmount
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DndAppStateTest {
    @Test
    fun charactersLanguageConditionsAndPortraitSurviveRestart() {
        val store = FakeStore()
        val state = DndAppState(store)
        state.beginCreate()
        state.creation.name = "Mira"
        state.creation.ancestry = "Elf"
        state.selectDraftPortrait(byteArrayOf(1, 2, 3, 4))
        state.finishCreate()
        state.toggleLanguage()
        state.addCondition("Prone")

        val restored = DndAppState(store)
        val character = restored.characters.single { it.name == "Mira" }

        assertEquals(UiLanguage.German, restored.language)
        assertEquals("Elf", character.ancestry)
        assertEquals("Prone", restored.conditions.single().name)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), assertNotNull(restored.portraitBytes(character)))
    }

    @Test
    fun profileAndTitledNotesCanBeCreatedEditedSearchedDeletedAndRestored() {
        val store = FakeStore()
        val state = DndAppState(store)
        state.beginCreate()
        state.creation.name = "Mira"
        state.creation.characterDescription = "Patient and curious"
        state.creation.motive = "Find the lost observatory"
        state.creation.alignment = "Chaotic good"
        state.finishCreate()

        assertEquals("Patient and curious", state.selectedCharacter?.profile?.characterDescription)
        assertEquals("Find the lost observatory", state.selectedCharacter?.profile?.motive)
        assertEquals("Chaotic good", state.selectedCharacter?.profile?.alignment)

        state.addNote("Session one", "Met the cartographer")
        val noteId = assertNotNull(state.selectedCharacter).notes.single().id
        state.updateNote(noteId, "Session 1", "Met the royal cartographer")
        assertTrue(state.search("royal cartographer").any { it.id == "note-$noteId" })

        val restored = DndAppState(store)
        val restoredCharacter = restored.characters.single { it.name == "Mira" }
        assertEquals("Patient and curious", restoredCharacter.profile.characterDescription)
        assertEquals("Session 1", restoredCharacter.notes.single().title)

        restored.openCharacter(restoredCharacter.id)
        restored.beginEdit(section = EditorSection.Identity)
        assertEquals(EditorSection.Identity, restored.editorDraft?.section)
        restored.editorDraft?.motive = "Protect the cartographer"
        assertTrue(restored.saveEdit())
        assertEquals("Protect the cartographer", DndAppState(store).characters.single { it.name == "Mira" }.profile.motive)

        restored.removeNote(noteId)
        assertTrue(DndAppState(store).characters.single { it.name == "Mira" }.notes.isEmpty())
    }

    @Test
    fun legacySingleNoteIsProjectedWithoutDataLoss() {
        val initial = assertNotNull(DndAppState(FakeStore()).selectedCharacter)
        val legacyDocument = initial.toDocument().copy(
            sheet = initial.toDocument().sheet.copy(
                notes = "Keeps a silver token.",
                noteEntries = emptyList(),
            ),
        )
        val payload = Json.encodeToString(PersistedAppState(characters = listOf(legacyDocument)))

        val restored = DndAppState(FakeStore(payload)).selectedCharacter

        assertEquals("General", restored?.notes?.single()?.title)
        assertEquals("Keeps a silver token.", restored?.notes?.single()?.body)
    }

    @Test
    fun sheetPortraitReplacementAndDeletionUseTheSelectedCharacter() {
        val store = FakeStore()
        val state = DndAppState(store)
        val character = assertNotNull(state.selectedCharacter)

        state.selectPortrait(PortraitPickTarget.Character(character.id), byteArrayOf(9, 8, 7))
        val portraitName = assertNotNull(state.selectedCharacter?.portraitFileName)
        assertContentEquals(byteArrayOf(9, 8, 7), assertNotNull(state.portraitBytes(assertNotNull(state.selectedCharacter))))

        assertTrue(state.deleteCharacterPortrait(character.id))
        assertNull(state.selectedCharacter?.portraitFileName)
        assertTrue(portraitName in store.deletedPortraits)
        assertNull(DndAppState(store).characters.first { it.id == character.id }.portraitFileName)
    }

    @Test
    fun deletingTheLastCharacterCleansOwnedStateAndPersistsAnEmptyRoster() {
        val source = assertNotNull(DndAppState(FakeStore()).selectedCharacter)
        val payload = Json.encodeToString(PersistedAppState(characters = listOf(source.toDocument())))
        val store = FakeStore(payload)
        val state = DndAppState(store)
        state.openCharacter(source.id)
        state.selectPortrait(PortraitPickTarget.Character(source.id), byteArrayOf(4, 3, 2, 1))
        val portraitName = assertNotNull(state.selectedCharacter?.portraitFileName)
        state.addCondition("Prone")
        state.openTurn()

        assertTrue(state.hasSavedTurnDraft(source.id))
        assertTrue(state.deleteCharacter(source.id))

        assertTrue(state.characters.isEmpty())
        assertTrue(state.conditions.none { it.characterId == source.id })
        assertFalse(state.hasSavedTurnDraft(source.id))
        assertNull(state.selectedCharacterId)
        assertEquals(AppScreen.Characters, state.screen)
        assertTrue(portraitName in store.deletedPortraits)

        val restored = DndAppState(store)
        assertTrue(restored.characters.isEmpty())
        assertNull(restored.selectedCharacterId)
    }

    @Test
    fun deletingAnUnknownCharacterDoesNothing() {
        val state = DndAppState(FakeStore())
        val before = state.characters.toList()

        assertFalse(state.deleteCharacter("missing-character"))
        assertEquals(before, state.characters)
    }

    @Test
    fun editorCanTargetACharacterFromTheOverview() {
        val state = DndAppState(FakeStore())
        val target = state.characters.first { it.id != state.selectedCharacterId }

        state.beginEdit(characterId = target.id)

        assertTrue(state.editorOpen)
        assertEquals(target.id, state.selectedCharacterId)
        assertEquals(target.id, state.editorDraft?.original?.id)
    }

    @Test
    fun levelUpCanBeOpenedFromSearchAndHitDieOverrideSurvivesRestart() {
        val store = FakeStore()
        val state = DndAppState(store)
        val searchResult = state.search("level up").single { it.id == "level-up" }

        state.handleSearchResult(searchResult)
        assertTrue(state.levelUpOpen)
        assertNotNull(state.levelUpDraft).apply {
            state.selectLevelUpHitDie(8)
            hpMethod = HpMethod.Fixed
        }
        val beforeMaxHp = assertNotNull(state.selectedCharacter).maxHp
        assertTrue(state.applyLevelUp())
        assertEquals(8, state.selectedCharacter?.hitDieOverrides?.get("Fighter"))
        assertEquals(8, assertNotNull(state.selectedCharacter).maxHp - beforeMaxHp)

        val restored = DndAppState(store)
        restored.beginLevelUp()
        assertEquals(8, restored.levelUpDraft?.hitDieSides)
    }

    @Test
    fun fifth2024IsPresentedAsDefault55e() {
        val state = DndAppState(FakeStore())

        assertEquals(Ruleset.Fifth2024, state.selectedCharacter?.ruleset)
        assertEquals("5.5e", Ruleset.Fifth2024.shortLabel)
        state.beginCreate()
        assertEquals(Ruleset.Fifth2024, state.creation.ruleset)
    }

    @Test
    fun pf2eCreationUsesAncestryAndClassHpLevelProficiencyAndExpertFighterWeapons() {
        val state = DndAppState(FakeStore())
        state.beginCreate()
        state.creation.ruleset = Ruleset.Pf2eRemaster
        state.creation.name = "Valeros"
        state.creation.ancestry = "Human"
        state.creation.className = "Fighter"
        state.creation.level = 1
        state.creation.statMethod = StatMethod.Manual
        state.creation.manualAbilities.putAll(
            mapOf("STR" to 18, "DEX" to 14, "CON" to 14, "INT" to 10, "WIS" to 10, "CHA" to 10),
        )

        val preview = state.creationPreview()
        state.finishCreate()
        val character = assertNotNull(state.selectedCharacter)

        assertEquals(20, character.maxHp)
        assertEquals(3, character.proficiency)
        assertEquals(18, character.armorClass)
        assertEquals("Scale Mail", character.resolvedEquipment.single { it.kind == EquipmentKind.ARMOR }.name)
        assertEquals(9, character.weapons.single().attackBonus)
        assertEquals("STR", character.weapons.single().ability)
        assertEquals(character.maxHp, preview.hitPoints)
        assertEquals(character.armorClass, preview.armorClass)
        assertEquals("STR", preview.primaryAbility)
        assertFalse(character.features.any { it.id == "second-wind" || it.id == "action-surge" })
    }

    @Test
    fun wizardStartsUnarmoredAndQuarterstaffUsesRecordedStrength() {
        val state = DndAppState(FakeStore())
        state.beginCreate()
        state.creation.name = "Ilyra"
        state.creation.className = "Wizard"
        state.creation.statMethod = StatMethod.Manual
        state.creation.manualAbilities.putAll(
            mapOf("STR" to 8, "DEX" to 14, "CON" to 12, "INT" to 18, "WIS" to 12, "CHA" to 10),
        )

        val preview = state.creationPreview()
        state.finishCreate()
        val character = assertNotNull(state.selectedCharacter)
        val staff = character.weapons.single()

        assertEquals(12, character.armorClass)
        assertFalse(character.resolvedEquipment.any { it.kind == EquipmentKind.ARMOR })
        assertEquals("Unarmored", preview.startingArmor)
        assertEquals("STR", staff.ability)
        assertEquals(1, staff.attackBonus)
        assertEquals("1d6 - 1", staff.damage)
        assertEquals("INT", preview.primaryAbility)
        assertEquals(18, preview.primaryScore)
    }

    @Test
    fun approvedTypedPrivateEntriesAreSelectableAndGenericEntriesStayInformational() {
        val state = DndAppState(FakeStore())
        state.addPrivateEntry(PrivateEntryUi("moonfolk", "species", "Moonfolk", "A lunar ancestry", "ruleset=2024"))
        state.addPrivateEntry(PrivateEntryUi("witchblade", "class", "Witchblade", "An arcane warrior", "hitDie=d8 primary ability=CHA caster ruleset=2024"))
        state.addPrivateEntry(PrivateEntryUi("star-touched", "feat", "Star Touched", "Read the night sky", "ruleset=2024"))
        state.addPrivateEntry(PrivateEntryUi("moon-step", "spell", "Moon Step", "Teleport through moonlight", "level=2 ruleset=2024"))
        state.addPrivateEntry(PrivateEntryUi("generic-page", "rule", "Imported page", "Unreviewed prose"))
        state.beginCreate()

        assertTrue("Moonfolk" in state.creationAncestryOptions())
        assertTrue("Witchblade" in state.creationClassOptions())
        assertTrue(state.creationFeatOptions().any { it.id == "star-touched" })
        assertTrue(state.creationSpellOptions().any { it.name == "Moon Step" })
        assertFalse(state.creationFeatOptions().any { it.name == "Imported page" })
        assertFalse(state.creationSpellOptions().any { it.name == "Imported page" })

        state.creation.name = "Selene"
        state.creation.ancestry = "Moonfolk"
        state.creation.className = "Witchblade"
        state.creation.statMethod = StatMethod.Manual
        state.creation.manualAbilities.putAll(
            mapOf("STR" to 12, "DEX" to 14, "CON" to 14, "INT" to 12, "WIS" to 10, "CHA" to 18),
        )
        state.creation.selectedFeatIds += "star-touched"
        state.creation.selectedSpellIds += "moon-step"
        state.finishCreate()
        val character = assertNotNull(state.selectedCharacter)

        assertEquals("Moonfolk", character.ancestry)
        assertEquals("Witchblade", character.className)
        assertTrue(character.features.any { it.name == "Star Touched" })
        assertTrue(character.features.any { it.name == "Witchblade" })
        assertTrue(character.availableSpells.any { it.name == "Moon Step" && it.level == 2 })
        assertFalse(character.features.any { it.name == "Imported page" })
        assertFalse(character.availableSpells.any { it.name == "Imported page" })
    }

    @Test
    fun seedDataCoversRequestedClassesAndUsesFourDefaultQuickRolls() {
        val state = DndAppState(FakeStore())

        assertEquals(setOf("Fighter", "Wizard", "Sorcerer", "Monk"), state.characters.map { it.className }.toSet())
        assertTrue(state.characters.all { it.resolvedQuickRolls.size == 4 })
    }

    @Test
    fun canonicalProjectionPreservesArmorConditionsAndSharedPools() {
        val fighter = DndAppState(FakeStore()).selectedCharacter!!
        val prone = ConditionUi("Prone", "DM", "Until removed", "Movement changes", fighter.id, "prone-1")

        val document = fighter.toDocument(listOf(prone))
        val restored = document.toCharacterUi()

        assertEquals(fighter.armorClass, restored.armorClass)
        assertEquals(fighter.unarmoredArmorClass, restored.unarmoredArmorClass)
        assertEquals(listOf("Prone"), document.toConditionUi().map { it.name })
        assertEquals(1, document.state.resources.count { it.id == "superiority-dice" })
        val secondWind = document.state.resources.single { it.id == "second-wind" }
        assertTrue(secondWind.recoveryRules.any {
            it.trigger == app.dulliesanddungeons.domain.Recovery.SHORT_REST && it.amount == RecoveryAmount.Fixed(1)
        })
        assertEquals(
            setOf("superiority-dice"),
            restored.features.filter { it.id.startsWith("maneuver-") }.mapNotNull { it.resourceId }.toSet(),
        )

        val authoredFormula = app.dulliesanddungeons.domain.DerivedStatisticFormula(
            ability = app.dulliesanddungeons.domain.Ability.STRENGTH,
            proficiencyMultiplier = 1,
        )
        val authoredDocument = document.copy(
            sheet = document.sheet.copy(
                combat = document.sheet.combat.copy(skills = mapOf("Athletics" to authoredFormula)),
            ),
        )
        assertEquals(8, authoredDocument.toCharacterUi().skills["Athletics"])
    }

    @Test
    fun canonicalTurnDraftSurvivesRestart() {
        val store = FakeStore()
        val state = DndAppState(store)
        state.openTurn()
        val session = state.turnSession!!
        assertTrue(session.commitDash())
        session.selectedWeaponId = "longbow"
        state.saveTurnDraft()

        val payload = Json.decodeFromString<PersistedAppState>(assertNotNull(store.raw()))
        assertNotNull(payload.characters.first().state.activeTurn)
        val restored = DndAppState(store)
        assertTrue(restored.hasSavedTurnDraft())
        restored.openTurn()
        assertTrue(restored.turnSession!!.dashActive)
        assertEquals("longbow", restored.turnSession!!.selectedWeaponId)
    }

    @Test
    fun playSessionTracksBetweenTurnAndTurnActivityAcrossRestart() {
        val store = FakeStore()
        val state = DndAppState(store)

        state.adjustHitPoints(4, damage = true)
        assertNull(state.currentPlaySession!!.activities.single().turnNumber)
        state.openTurn()
        val turn = assertNotNull(state.turnSession)
        turn.requestedMovement = 10
        turn.commitMovement()
        state.applyTurnDamage(6, critical = false, turn)
        state.adjustHitPoints(3, damage = false)
        state.closeTurnGuide()

        val stats = assertNotNull(state.currentPlaySession).stats()
        assertEquals(10, stats.damageTaken)
        assertEquals(3, stats.healingReceived)
        assertEquals(10, stats.distanceMoved)
        assertTrue(state.currentPlaySession!!.activities.any { it.turnNumber == 1 })

        val restored = DndAppState(store)
        val restoredStats = assertNotNull(restored.currentPlaySession).stats()
        assertEquals(stats, restoredStats)
        restored.openTurn()
        assertTrue(restored.nextTurn())
        assertEquals(2, restored.currentPlaySession!!.currentTurnNumber)
        assertEquals(1, restored.currentPlaySession!!.stats().turns)
    }

    @Test
    fun savingSessionIncludesNonEmptyTurnAndIgnoresFreshNextTurn() {
        val store = FakeStore()
        val state = DndAppState(store)
        state.openTurn()
        assertNotNull(state.turnSession).commitAction("dodge")
        assertTrue(state.nextTurn())

        state.savePlaySession("Game night")

        assertNull(state.currentPlaySession)
        assertFalse(state.hasSavedTurnDraft())
        val saved = state.selectedCharacter!!.savedPlaySessions.single()
        assertEquals("Game night", saved.title)
        assertEquals(1, saved.stats().turns)

        state.renamePlaySession(saved.id, "Dragon fight")
        val restored = DndAppState(store)
        assertEquals("Dragon fight", restored.selectedCharacter!!.savedPlaySessions.single().title)
        restored.deletePlaySession(saved.id)
        assertTrue(DndAppState(store).selectedCharacter!!.savedPlaySessions.isEmpty())
    }

    @Test
    fun sessionStatsCountIncomingDamageActualHealingAndRepeatedDowns() {
        val store = FakeStore()
        val source = DndAppState(FakeStore()).selectedCharacter!!.copy(hp = 5, temporaryHp = 3)
        store.writeState(Json.encodeToString(PersistedAppState(characters = listOf(source.toDocument()))))
        val state = DndAppState(store)

        state.adjustHitPoints(8, damage = true)
        state.adjustHitPoints(1, damage = false)
        state.adjustHitPoints(1, damage = true)

        val stats = assertNotNull(state.currentPlaySession).stats()
        assertEquals(9, stats.damageTaken)
        assertEquals(1, stats.healingReceived)
        assertEquals(2, stats.timesDowned)
    }

    @Test
    fun hpAdjustmentsClampAndPersist() {
        val store = FakeStore()
        val state = DndAppState(store)
        val maximum = assertNotNull(state.selectedCharacter).maxHp

        state.adjustHitPoints(10_000, damage = true)
        assertEquals(0, state.selectedCharacter?.hp)
        state.adjustHitPoints(10_000, damage = false)
        assertTrue(state.revivalConfirmationOpen)
        state.confirmRevival()
        assertEquals(maximum, state.selectedCharacter?.hp)

        assertEquals(maximum, DndAppState(store).selectedCharacter?.hp)
    }

    @Test
    fun shortRestRecoversOnlyShortRestResourcesAndPersists() {
        val store = FakeStore()
        val source = DndAppState(FakeStore()).selectedCharacter!!.copy(
            hp = 40,
            features = DndAppState(FakeStore()).selectedCharacter!!.features.map { feature ->
                if (feature.id in setOf("second-wind", "action-surge", "indomitable")) feature.copy(remaining = 0)
                else feature
            },
        )
        store.writeState(Json.encodeToString(PersistedAppState(characters = listOf(source.toDocument()))))
        val state = DndAppState(store)

        assertTrue(state.takeRest(Recovery.SHORT_REST))

        val rested = state.selectedCharacter!!
        assertEquals(40, rested.hp)
        assertEquals(1, rested.features.first { it.id == "second-wind" }.remaining)
        assertEquals(1, rested.features.first { it.id == "action-surge" }.remaining)
        assertEquals(0, rested.features.first { it.id == "indomitable" }.remaining)
        assertEquals(rested.features, DndAppState(store).selectedCharacter!!.features)
    }

    @Test
    fun longRestRestoresHealthResourcesAndOneExhaustionLevel() {
        val store = FakeStore()
        val original = DndAppState(FakeStore()).selectedCharacter!!
        val source = original.copy(
            hp = 12,
            temporaryHp = 5,
            exhaustionLevel = 2,
            features = original.features.map { feature ->
                if (feature.recovery == Recovery.SHORT_REST || feature.recovery == Recovery.LONG_REST) feature.copy(remaining = 0)
                else feature
            },
        )
        store.writeState(Json.encodeToString(PersistedAppState(characters = listOf(source.toDocument()))))
        val state = DndAppState(store)

        assertTrue(state.takeRest(Recovery.LONG_REST))

        val rested = state.selectedCharacter!!
        assertEquals(rested.effectiveMaxHp, rested.hp)
        assertEquals(0, rested.temporaryHp)
        assertEquals(1, rested.exhaustionLevel)
        assertTrue(rested.features.filter { it.recovery == Recovery.SHORT_REST || it.recovery == Recovery.LONG_REST }
            .all { it.remaining == it.maximum })
        val restored = DndAppState(store).selectedCharacter!!
        assertEquals(rested.hp, restored.hp)
        assertEquals(rested.features, restored.features)
    }

    @Test
    fun restsAreUnavailableForPf2eDownedCharactersAndUnfinishedTurns() {
        val pf2eState = DndAppState(FakeStore())
        pf2eState.characters[0] = pf2eState.selectedCharacter!!.copy(ruleset = Ruleset.Pf2eRemaster)
        assertFalse(pf2eState.canTakeRest())
        assertFalse(pf2eState.takeRest(Recovery.SHORT_REST))

        val downedState = DndAppState(FakeStore())
        downedState.adjustHitPoints(downedState.selectedCharacter!!.hp, damage = true)
        assertFalse(downedState.canTakeRest())
        assertFalse(downedState.takeRest(Recovery.LONG_REST))

        val turnState = DndAppState(FakeStore())
        turnState.openTurn()
        assertFalse(turnState.canTakeRest())
        assertFalse(turnState.takeRest(Recovery.SHORT_REST))
    }

    @Test
    fun quickRollSelectionIsLimitedToTwelveAndPersists() {
        val store = FakeStore()
        val state = DndAppState(store)
        val choices = state.availableQuickRolls().take(20)

        state.updateQuickRolls(choices)

        assertEquals(12, state.selectedCharacter?.quickRolls?.size)
        assertEquals(state.selectedCharacter?.quickRolls, DndAppState(store).selectedCharacter?.quickRolls)
    }

    @Test
    fun weaponsRouteOnlyToWeaponsAndAttunementIsRelevantOnlyWhenRequired() {
        val state = DndAppState(FakeStore())
        val beforeWeapons = assertNotNull(state.selectedCharacter).weapons.size
        val dagger = standardWeaponCatalog.first { it.id == "dagger" }

        state.addStandardWeapon(dagger)
        state.addEquipment(EquipmentUi("ring-test", "Test Ring", needsAttunement = true))
        val character = assertNotNull(state.selectedCharacter)

        assertEquals(beforeWeapons + 1, character.weapons.size)
        assertFalse(character.resolvedEquipment.any { it.name == "Dagger" })
        assertFalse(character.resolvedEquipment.first { it.id == "ring-test" }.attuned)
        state.toggleEquipmentAttunement("ring-test")
        assertTrue(state.selectedCharacter!!.resolvedEquipment.first { it.id == "ring-test" }.attuned)
    }

    @Test
    fun attackModesExposeDiceMathAndHitAutomaticallyRollsDamage() {
        val state = DndAppState(FakeStore())
        val weapon = assertNotNull(state.selectedCharacter).weapons.first()

        state.openSheetAttack(weapon.id)
        state.rollSheetAttack(RollMode.ADVANTAGE)
        val roll = assertNotNull(state.sheetAttackRoll)
        assertEquals(2, roll.dice.size)
        assertEquals(weapon.attackBonus, roll.calculation.total)
        assertNull(state.dicePresentation)

        state.resolveSheetAttack(AttackOutcome.Hit)
        assertNotNull(state.sheetDamageRoll)
        assertFalse(state.sheetDamageRoll!!.critical)
        assertNull(state.dicePresentation)
        state.resolveSheetAttack(AttackOutcome.Miss)
        assertNull(state.sheetDamageRoll)
    }

    @Test
    fun secondWindRespectsTheEffectiveMaximumHpFrom2014Exhaustion() {
        val fighter = DndAppState(FakeStore()).selectedCharacter!!.copy(
            ruleset = Ruleset.Fifth2014,
            hp = 49,
            maxHp = 100,
        )
        val payload = Json.encodeToString(PersistedAppState(characters = listOf(fighter.toDocument())))
        val state = DndAppState(FakeStore(payload))
        state.setExhaustion(4)

        assertTrue(state.useFeature("second-wind", session = null))

        assertEquals(50, state.selectedCharacter!!.effectiveMaxHp)
        assertEquals(50, state.selectedCharacter!!.hp)
    }

    @Test
    fun actionSurgeSpendsItsPoolAndGrantsOneRealExtraAction() {
        val state = DndAppState(FakeStore())
        state.openTurn()
        val session = assertNotNull(state.turnSession)

        assertTrue(state.useFeature("action-surge", session))
        assertEquals(1, session.extraActionsRemaining)
        assertEquals(0, state.selectedCharacter!!.features.first { it.id == "action-surge" }.remaining)

        session.commitAction()
        assertEquals(1, session.extraActionsRemaining)
        session.commitAction()
        assertEquals(0, session.extraActionsRemaining)
        assertFalse(session.canUseAction)
        assertFalse(state.useFeature("action-surge", session))
    }

    @Test
    fun incomingTurnDamageUsesTemporaryHpWithoutSpendingTurnCosts() {
        val source = DndAppState(FakeStore()).selectedCharacter!!.copy(hp = 20, temporaryHp = 5)
        val state = DndAppState(FakeStore(Json.encodeToString(PersistedAppState(characters = listOf(source.toDocument())))))
        state.openTurn()
        val session = assertNotNull(state.turnSession)

        state.applyTurnDamage(0, critical = false, session)
        state.applyTurnDamage(10_000, critical = false, session)
        assertEquals(20, state.selectedCharacter!!.hp)
        assertEquals(0, session.eventCount)

        val updated = assertNotNull(state.applyTurnDamage(8, critical = false, session))

        assertEquals(17, updated.hp)
        assertEquals(0, updated.temporaryHp)
        assertFalse(updated.stopsTurnGuide)
        assertFalse(session.actionUsed)
        assertFalse(session.bonusActionUsed)
        assertFalse(session.reactionUsed)
        assertEquals(0, session.movementUsed)
        assertEquals(1, session.eventCount)
    }

    @Test
    fun incomingTurnDamageAtZeroStopsTheGuideAndDiscardRestoresTheBaseline() {
        val store = FakeStore()
        val source = DndAppState(FakeStore()).selectedCharacter!!.copy(hp = 6, temporaryHp = 0)
        store.writeState(Json.encodeToString(PersistedAppState(characters = listOf(source.toDocument()))))
        val state = DndAppState(store)
        state.openTurn()
        val session = assertNotNull(state.turnSession)

        val downed = assertNotNull(state.applyTurnDamage(6, critical = false, session))
        assertEquals(0, downed.hp)
        assertTrue(downed.isDowned)
        assertTrue(downed.stopsTurnGuide)
        assertFalse(session.actionUsed)
        state.closeTurnGuide()

        val resumed = DndAppState(store)
        resumed.openTurn()
        assertTrue(resumed.selectedCharacter!!.stopsTurnGuide)
        resumed.discardTurn()
        assertEquals(6, resumed.selectedCharacter!!.hp)
        assertFalse(resumed.selectedCharacter!!.stopsTurnGuide)
    }

    @Test
    fun endingTurnKeepsIncomingDamage() {
        val store = FakeStore()
        val source = DndAppState(FakeStore()).selectedCharacter!!.copy(hp = 20, temporaryHp = 0)
        store.writeState(Json.encodeToString(PersistedAppState(characters = listOf(source.toDocument()))))
        val state = DndAppState(store)
        state.openTurn()
        state.applyTurnDamage(7, critical = false, assertNotNull(state.turnSession))
        state.finishTurn()

        val restored = DndAppState(store)
        assertEquals(13, restored.selectedCharacter!!.hp)
        assertFalse(restored.hasSavedTurnDraft())
    }

    @Test
    fun pf2eCriticalIncomingDamageUsesTheCriticalDyingIncrease() {
        val base = DndAppState(FakeStore()).selectedCharacter!!.copy(
            ruleset = Ruleset.Pf2eRemaster,
            hp = 3,
            maxHp = 30,
            temporaryHp = 0,
            woundedValue = 1,
            dyingValue = 0,
            isDead = false,
            deathReason = null,
        )
        val state = DndAppState(FakeStore(Json.encodeToString(PersistedAppState(characters = listOf(base.toDocument())))))
        state.openTurn()

        val updated = assertNotNull(state.applyTurnDamage(3, critical = true, assertNotNull(state.turnSession)))

        assertEquals(0, updated.hp)
        assertEquals(3, updated.dyingValue)
        assertTrue(updated.stopsTurnGuide)
        assertFalse(updated.isDead)
    }

    @Test
    fun structuredCostsPersistAndSpendTheCorrectTurnResource() {
        val wizard = DndAppState(FakeStore()).characters.first { it.className == "Wizard" }
        val restoredWizard = wizard.toDocument().toCharacterUi()
        val shield = restoredWizard.availableSpells.first { it.id == "shield" }
        assertEquals(ActionCost(reactions = 1), shield.activationCost)

        val state = DndAppState(FakeStore(Json.encodeToString(PersistedAppState(characters = listOf(restoredWizard.toDocument())))))
        state.openTurn()
        val session = assertNotNull(state.turnSession)
        assertTrue(session.commitCost(shield.activationCost))
        assertTrue(session.reactionUsed)
        assertFalse(session.actionUsed)
        assertFalse(session.bonusActionUsed)

        val fighter = DndAppState(FakeStore()).characters.first { it.className == "Fighter" }.toDocument().toCharacterUi()
        assertEquals(1, fighter.features.first { it.id == "second-wind" }.actionCost.bonusActions)
        assertEquals(1, fighter.features.first { it.id == "maneuver-riposte" }.actionCost.reactions)
    }

    @Test
    fun pf2eTurnAttacksApplyAndPersistTheMultipleAttackPenalty() {
        val store = FakeStore()
        val state = DndAppState(store)
        state.convert(Ruleset.Pf2eRemaster)
        state.openTurn()
        val session = assertNotNull(state.turnSession)
        val weapon = state.selectedCharacter!!.weapons.first()

        state.rollAttack(weapon, session)
        assertEquals(0, state.turnSession!!.lastAttackDetails!!.calculation.multipleAttackPenalty)
        session.finishAttackResolution()
        state.rollAttack(weapon, session)
        assertEquals(-5, state.turnSession!!.lastAttackDetails!!.calculation.multipleAttackPenalty)
        assertEquals(2, session.pf2AttacksMade)
        state.saveTurnDraft()
        val characterId = state.selectedCharacter!!.id

        val restored = DndAppState(store)
        restored.openCharacter(characterId)
        restored.openTurn()
        assertEquals(2, restored.turnSession!!.pf2AttacksMade)
    }

    @Test
    fun suggestedTurnIsDeterministicShortAndHasNoTargetAssumptions() {
        val state = DndAppState(FakeStore())
        state.openTurn()
        val first = state.suggestedTurnSteps()
        val second = state.suggestedTurnSteps()
        assertEquals(first, second)

        assertTrue(first.size in 3..5)
        assertTrue(first.any { it.section == TurnSection.Move })
        assertTrue(first.any { it.weaponId != null })
        assertTrue(first.all { it.assumptionCount == 0 })
    }

    @Test
    fun itemSpellRequiresAttunementAndKeepsItsSource() {
        val store = FakeStore()
        val current = DndAppState(store).selectedCharacter!!
        val ringSpell = SpellUi("misty-step", "Misty Step", 2, "Teleport 30 ft")
        val withRing = current.copy(
            equipmentItems = current.resolvedEquipment + EquipmentUi(
                id = "ring-of-steps",
                name = "Ring of Steps",
                needsAttunement = true,
                grantedSpells = listOf(ringSpell),
            ),
        )
        val payload = Json.encodeToString(PersistedAppState(characters = listOf(withRing.toDocument())))
        val state = DndAppState(FakeStore(payload))

        assertFalse(state.selectedCharacter!!.canCastSpells)
        state.toggleEquipmentAttunement("ring-of-steps")
        val available = state.selectedCharacter!!.availableSpells.single()
        assertEquals(SpellSourceKind.ITEM, available.sourceKind)
        assertEquals("Ring of Steps", available.sourceName)
    }

    @Test
    fun editorIsAtomicAndRecalculatesOnlyLinkedValues() {
        val state = DndAppState(FakeStore())
        val original = state.selectedCharacter!!
        state.beginEdit("DEX")
        val draft = assertNotNull(state.editorDraft)
        draft.abilities["DEX"] = 20
        draft.maxHp = 50

        assertEquals(original.abilities["DEX"], state.selectedCharacter!!.abilities["DEX"])
        assertTrue(state.saveEdit())

        val edited = state.selectedCharacter!!
        assertEquals(20, edited.abilities["DEX"])
        assertEquals(5, edited.initiative)
        assertEquals(original.skills, edited.skills)
        assertEquals(9, edited.weapons.first { it.id == "longbow" }.attackBonus)
        assertEquals(original.armorClass, edited.armorClass)
        assertEquals(50, edited.hp)
    }

    @Test
    fun changingRulesetFromEditorCreatesCopyAndPreservesOriginal() {
        val state = DndAppState(FakeStore())
        val original = state.selectedCharacter!!
        state.beginEdit()
        state.editorDraft!!.ruleset = Ruleset.Pf2eRemaster

        assertTrue(state.saveEdit())

        assertEquals(5, state.characters.size)
        assertEquals(original, state.characters.first { it.id == original.id })
        assertEquals(Ruleset.Pf2eRemaster, state.selectedCharacter!!.ruleset)
        assertEquals(original.id, state.selectedCharacter!!.sourceCharacterId)
    }

    @Test
    fun levelUpRequiresAsiOrFeatAndAppliesAbilityScoresInsteadOfFakeFeatId() {
        val source = DndAppState(FakeStore()).selectedCharacter!!
        val levelThree = source.copy(
            level = 3,
            proficiency = 2,
            progression = source.progression.take(3),
        )
        val payload = Json.encodeToString(PersistedAppState(characters = listOf(levelThree.toDocument())))
        val state = DndAppState(FakeStore(payload))

        state.beginLevelUp()
        assertTrue(state.levelUpFeatAvailable())
        assertFalse(state.applyLevelUp())

        state.selectLevelUpFeat("ability-score-improvement")
        state.setLevelUpAbilityIncrease("STR", 2)
        assertTrue(state.levelUpFeatSelectionValid())
        assertTrue(state.applyLevelUp())

        val updated = state.selectedCharacter!!
        assertEquals(20, updated.abilities["STR"])
        assertFalse("ability-score-improvement" in updated.featIds)
        assertNull(updated.progression.last().featId)
    }

    @Test
    fun levelUpRequiresAndStoresSubclassChoiceAtTheClassThreshold() {
        val source = DndAppState(FakeStore()).characters.first { it.className == "Monk" }
        val levelTwo = source.copy(
            level = 2,
            subclass = "—",
            progression = source.progression.take(2),
            features = source.features.filter { it.id == "focus-points" },
        )
        val payload = Json.encodeToString(PersistedAppState(characters = listOf(levelTwo.toDocument())))
        val state = DndAppState(FakeStore(payload))
        state.beginLevelUp()

        assertFalse(state.levelUpGuidedChoicesValid())
        val subclass = state.levelUpGuidedChoices().single { it.kind == GuidedLevelChoiceKind.SUBCLASS }
        state.toggleLevelUpGuidedOption(subclass.id, subclass.options.single().id)

        assertTrue(state.levelUpGuidedChoicesValid())
        assertTrue(state.applyLevelUp())
        assertEquals("Warrior of the Open Hand", state.selectedCharacter!!.subclass)
    }

    @Test
    fun seedClassesReceiveConcreteLevelUnlocksAndRequiredSpellChoices() {
        data class Expectation(val characterId: String, val featureId: String, val spellCount: Int)
        val expectations = listOf(
            Expectation("seed-fighter-10", "extra-attack", 0),
            Expectation("seed-wizard-5", "potent-cantrip", 2),
            Expectation("seed-sorcerer-5", "elemental-affinity", 1),
            Expectation("seed-monk-5", "empowered-strikes", 0),
        )

        expectations.forEach { expectation ->
            val initial = DndAppState(FakeStore()).characters.first { it.id == expectation.characterId }
            val payload = Json.encodeToString(PersistedAppState(characters = listOf(initial.toDocument())))
            val state = DndAppState(FakeStore(payload))
            state.beginLevelUp()
            var guard = 0
            while (!state.levelUpGuidedChoicesValid() && guard++ < 5) {
                state.levelUpGuidedChoices().forEach { choice ->
                    val selected = state.levelUpDraft!!.guidedSelections[choice.id].orEmpty()
                    choice.options.filterNot { it.id in selected }
                        .take(choice.chooseCount - selected.size)
                        .forEach { state.toggleLevelUpGuidedOption(choice.id, it.id) }
                }
            }

            assertTrue(state.levelUpGuidedChoicesValid(), expectation.characterId)
            assertTrue(state.applyLevelUp(), expectation.characterId)
            val updated = state.selectedCharacter!!
            assertTrue(updated.features.any { it.id == expectation.featureId }, expectation.characterId)
            assertEquals(initial.spells.size + expectation.spellCount, updated.spells.size, expectation.characterId)
            if (expectation.characterId == "seed-fighter-10") {
                assertTrue(updated.features.first { it.id == "extra-attack" }.summary.contains("3"))
            }
            if (expectation.characterId == "seed-monk-5") {
                assertTrue(updated.features.any { it.id == "wholeness-of-body" })
            }
            if (expectation.characterId == "seed-sorcerer-5") {
                assertEquals(6, updated.features.first { it.id == "sorcery-points" }.maximum)
                assertEquals(2, updated.features.count { it.id.startsWith("metamagic-") })
            }
        }
    }

    @Test
    fun wizardQuickCastSpendsTheChosenSlotAndLongRestRestoresIt() {
        val store = FakeStore()
        val state = DndAppState(store)
        state.openCharacter("seed-wizard-5")
        val wizard = assertNotNull(state.selectedCharacter)
        assertEquals(listOf(4, 3, 2), wizard.resolvedSpellSlots.map { it.maximum })

        val cantrip = wizard.availableSpells.first { it.level == 0 }
        val beforeCantrip = wizard.resolvedSpellSlots
        assertTrue(state.castSpell(cantrip, session = null))
        assertEquals(beforeCantrip, state.selectedCharacter!!.resolvedSpellSlots)

        val magicMissile = state.selectedCharacter!!.availableSpells.first { it.id == "magic-missile" }
        assertEquals(listOf(1, 2, 3), state.availableSpellSlotLevels(magicMissile))
        assertTrue(state.castSpell(magicMissile, slotLevel = 2, session = null))
        assertEquals(2, state.selectedCharacter!!.resolvedSpellSlots.first { it.level == 2 }.remaining)

        val restored = DndAppState(store)
        restored.openCharacter("seed-wizard-5")
        assertEquals(2, restored.selectedCharacter!!.resolvedSpellSlots.first { it.level == 2 }.remaining)
        assertTrue(restored.takeRest(Recovery.LONG_REST))
        assertTrue(restored.selectedCharacter!!.resolvedSpellSlots.all { it.remaining == it.maximum })
    }

    @Test
    fun sorcererCanConvertPointsIntoAnExpendedSpellSlot() {
        val state = DndAppState(FakeStore())
        state.openCharacter("seed-sorcerer-5")
        val spell = state.selectedCharacter!!.availableSpells.first { it.id == "burning-hands" }
        assertTrue(state.castSpell(spell, slotLevel = 1, session = null))
        val pointsBefore = state.selectedCharacter!!.features.first { it.id == "sorcery-points" }.remaining!!

        assertTrue(state.regainSpellSlotWithSorceryPoints(1))

        val sorcerer = state.selectedCharacter!!
        assertEquals(sorcerer.resolvedSpellSlots.first { it.level == 1 }.maximum, sorcerer.resolvedSpellSlots.first { it.level == 1 }.remaining)
        assertEquals(pointsBefore - 2, sorcerer.features.first { it.id == "sorcery-points" }.remaining)
        assertFalse(state.regainSpellSlotWithSorceryPoints(1))
    }

    @Test
    fun quickRollsExcludeWeaponsAndBlankSearchSuggestsPlayerWikiCards() {
        val state = DndAppState(FakeStore())
        val weapon = state.selectedCharacter!!.weapons.first()
        state.updateQuickRolls(
            listOf(
                QuickRollUi(QuickRollKind.ATTACK, weapon.id, weapon.name),
                QuickRollUi(QuickRollKind.ABILITY, "WIS", "WIS"),
            ),
        )

        assertTrue(state.selectedCharacter!!.quickRolls.none { it.kind == QuickRollKind.ATTACK })
        assertTrue(state.availableQuickRolls().none { it.kind == QuickRollKind.ATTACK })
        assertTrue(state.search("").any { it.id == "rule-maneuvers" })
        assertEquals("rule-superiority-dice", state.search("superiority dice").first().id)
    }

    @Test
    fun recordedPlayEnablesRestAndLongRestCanConsumeOneRation() {
        val store = FakeStore()
        val state = DndAppState(store)
        assertFalse(state.canTakeRest())
        state.addEquipment(EquipmentUi("travel-ration", "Travel ration", EquipmentKind.RATIONS))

        state.roll("Perception", 3)

        assertTrue(state.canTakeRest())
        assertEquals("travel-ration", state.availableRations().single().id)
        assertTrue(state.takeRest(Recovery.LONG_REST, "travel-ration"))
        assertFalse(state.selectedCharacter!!.hasPlayedSinceLongRest)
        assertTrue(state.availableRations().isEmpty())

        val restored = DndAppState(store)
        assertFalse(restored.selectedCharacter!!.hasPlayedSinceLongRest)
        assertTrue(restored.availableRations().isEmpty())
    }

    @Test
    fun maneuverQuickUseSpendsAndRollsTheSharedSuperiorityDie() {
        val state = DndAppState(FakeStore())
        val before = state.selectedCharacter!!.features.first { it.id == "superiority-dice" }.remaining!!

        assertTrue(state.useFeature("maneuver-trip-attack", session = null))

        val fighter = state.selectedCharacter!!
        assertEquals(before - 1, fighter.features.first { it.id == "superiority-dice" }.remaining)
        assertEquals(before - 1, fighter.features.first { it.id == "maneuver-trip-attack" }.remaining)
        assertTrue(assertNotNull(state.inlineFeatureFeedback).rolledValue in 1..10)
    }

    @Test
    fun spellSlotMaximumOverridePreservesSpentSlotsWhenShrunkAndRestored() {
        val store = FakeStore()
        val state = DndAppState(store)
        state.openCharacter("seed-wizard-5")
        val spell = state.selectedCharacter!!.availableSpells.first { it.level == 1 }
        assertTrue(state.castSpell(spell, 1, session = null))
        assertTrue(state.castSpell(spell, 1, session = null))

        assertTrue(state.updateSpellSlotMaximum(1, 1))
        assertEquals(0, state.selectedCharacter!!.resolvedSpellSlots.first { it.level == 1 }.remaining)
        val restarted = DndAppState(store)
        restarted.openCharacter("seed-wizard-5")
        assertTrue(restarted.updateSpellSlotMaximum(1, restarted.rulesSpellSlotMaximum(1)))

        val restored = restarted.selectedCharacter!!.resolvedSpellSlots.first { it.level == 1 }
        assertEquals(2, restored.remaining)
        assertEquals(4, restored.maximum)
    }

    @Test
    fun obsoleteSchemaIsRejectedInsteadOfMigrated() {
        val obsolete = DndAppState(FakeStore()).selectedCharacter!!.copy(name = "Must not load")
        val payload = Json { encodeDefaults = false }.encodeToString(
            PersistedAppState(schemaVersion = 1, characters = listOf(obsolete.toDocument()))
        )

        val restored = DndAppState(FakeStore(payload))

        assertFalse(restored.characters.any { it.name == "Must not load" })
        assertEquals(4, restored.characters.size)
    }
}

private class FakeStore(initialState: String? = null) : LocalStateStore {
    private var state: String? = initialState
    private val portraits = mutableMapOf<String, ByteArray>()
    val deletedPortraits = mutableSetOf<String>()

    override fun readState(): String? = state

    override fun writeState(value: String) {
        state = value
    }

    override fun writePortrait(characterId: String, bytes: ByteArray): String {
        val name = "$characterId.jpg"
        portraits[name] = bytes
        return name
    }

    override fun readPortrait(fileName: String): ByteArray? = portraits[fileName]

    override fun deletePortrait(fileName: String): Boolean {
        deletedPortraits += fileName
        portraits.remove(fileName)
        return true
    }

    fun raw(): String? = state
}
