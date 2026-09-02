package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.ActiveCondition
import app.dulliesanddungeons.domain.ArmorClassMethod
import app.dulliesanddungeons.domain.AttunementState
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterDocument
import app.dulliesanddungeons.domain.CharacterNote
import app.dulliesanddungeons.domain.CharacterSheetData
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.CharacterRollbackSnapshot
import app.dulliesanddungeons.domain.ChoiceSelection
import app.dulliesanddungeons.domain.ClassLevel
import app.dulliesanddungeons.domain.CombatProfile
import app.dulliesanddungeons.domain.CustomEntityKind
import app.dulliesanddungeons.domain.CustomEntitySnapshot
import app.dulliesanddungeons.domain.DeathReason
import app.dulliesanddungeons.domain.DerivedStatisticFormula
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.EffectSource
import app.dulliesanddungeons.domain.EquipmentCategory
import app.dulliesanddungeons.domain.EquipmentItem
import app.dulliesanddungeons.domain.EquipmentLocation
import app.dulliesanddungeons.domain.EquipmentSlot
import app.dulliesanddungeons.domain.ExpiryTrigger
import app.dulliesanddungeons.domain.FeatureRecord
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.FiveEHealthState
import app.dulliesanddungeons.domain.HitPointGainMethod
import app.dulliesanddungeons.domain.HitPointGainRecord
import app.dulliesanddungeons.domain.LanguageRecord
import app.dulliesanddungeons.domain.LevelProgressionEntry
import app.dulliesanddungeons.domain.LocaleId
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.Pf2eBuildData
import app.dulliesanddungeons.domain.Pf2eFeatCategory
import app.dulliesanddungeons.domain.Pf2eFeatSelection
import app.dulliesanddungeons.domain.Pf2eHealthState
import app.dulliesanddungeons.domain.ProficiencyRank
import app.dulliesanddungeons.domain.ProgressionLedger
import app.dulliesanddungeons.domain.QuickRollShortcut
import app.dulliesanddungeons.domain.RecoveryAmount
import app.dulliesanddungeons.domain.RecoveryRule
import app.dulliesanddungeons.domain.ResourcePool
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.RollMode
import app.dulliesanddungeons.domain.SpellRecord
import app.dulliesanddungeons.domain.SpellSourceKind as DomainSpellSourceKind
import app.dulliesanddungeons.domain.QuickRollKind as DomainQuickRollKind
import app.dulliesanddungeons.domain.WeaponRecord
import app.dulliesanddungeons.domain.TurnDraft
import app.dulliesanddungeons.domain.TurnGuideFlag
import app.dulliesanddungeons.domain.TurnGuideSelections
import app.dulliesanddungeons.domain.TurnSection as DomainTurnSection
import app.dulliesanddungeons.rules.DiceNotation
import app.dulliesanddungeons.rules.FifthEditionFeatureRules

internal fun CharacterUi.toDocument(characterConditions: List<ConditionUi> = emptyList()): CharacterDocument {
    val rulesetId = ruleset.toDomain()
    val storedAbilities = baseAbilities.ifEmpty { abilities }
    val abilitiesByType = storedAbilities.mapNotNull { (key, value) -> ability(key)?.let { it to value } }.toMap()
    val classNames = progression.map { it.className }.ifEmpty { List(level) { className } }
    val classIdsByName = classNames.distinct().associateWith { "class:${slug(it)}" }
    val classes = classNames.groupingBy { it }.eachCount().map { (name, levels) ->
        val selectedSubclassId = subclassIdsByClass[name]
            ?: if (name == className && subclass.isNotBlank() && subclass != "—") "subclass:${slug(subclass)}" else null
        ClassLevel(
            classId = classIdsByName.getValue(name),
            levels = levels,
            subclassId = selectedSubclassId,
        )
    }
    val ancestryId = "ancestry:${slug(ancestry)}"
    val backgroundId = this.backgroundId.takeIf { it.startsWith("background:") } ?: "background:${slug(backgroundName)}"
    val languageRecords = languages.map { name ->
        LanguageRecord("language:${slug(name)}", name, custom = true, locked = name in lockedLanguages)
    }
    val customEntities = buildMap {
        put(ancestryId, CustomEntitySnapshot(ancestryId, CustomEntityKind.ANCESTRY, ancestry))
        put(backgroundId, CustomEntitySnapshot(backgroundId, CustomEntityKind.BACKGROUND, backgroundName))
        classes.forEach { value ->
            val name = classIdsByName.entries.first { it.value == value.classId }.key
            put(value.classId, CustomEntitySnapshot(value.classId, CustomEntityKind.CLASS, name))
        }
        classes.forEach { classLevel ->
            classLevel.subclassId?.let { id ->
                val classDisplayName = classIdsByName.entries.first { it.value == classLevel.classId }.key
                val subclassDisplayName = subclassNamesByClass[classDisplayName]
                    ?: if (classDisplayName == className) subclass else displayId(id.substringAfter(':'))
                put(id, CustomEntitySnapshot(id, CustomEntityKind.SUBCLASS, subclassDisplayName))
            }
        }
        featIds.forEach { id -> put(id, CustomEntitySnapshot(id, CustomEntityKind.FEAT, displayId(id))) }
        languageRecords.forEach { put(it.id, CustomEntitySnapshot(it.id, CustomEntityKind.LANGUAGE, it.name)) }
        spells.forEach { put(it.id, CustomEntitySnapshot(it.id, CustomEntityKind.SPELL, it.name, it.summary)) }
        features.forEach { put(it.id, CustomEntitySnapshot(it.id, CustomEntityKind.FEATURE, it.name, it.summary)) }
    }
    val rulePayload = if (rulesetId.isFiveEdition) {
        FiveEBuildData(
            ancestryId = ancestryId,
            backgroundId = backgroundId,
            classes = classes,
            abilities = abilitiesByType,
            feats = featIds.map(::ChoiceSelection),
            proficiencyIds = proficiencyIds.ifEmpty { proficiencyRanks.keys },
            proficiencyRanks = proficiencyRanks,
            languages = languageRecords.map { ChoiceSelection(it.id) },
            knownSpells = spells.map { ChoiceSelection(it.id) },
            preparedSpellIds = spells.filter { it.prepared }.mapTo(mutableSetOf()) { it.id },
            features = features.map { ChoiceSelection(it.id) },
            classHitDieOverrides = hitDieOverrides.mapNotNull { (className, sides) ->
                classIdsByName[className]?.let { it to sides }
            }.toMap(),
        )
    } else {
        val primary = classes.firstOrNull() ?: ClassLevel("class:${slug(className)}", level)
        Pf2eBuildData(
            ancestryId = ancestryId,
            heritageId = "heritage:local",
            backgroundId = backgroundId,
            classId = primary.classId,
            level = level,
            abilities = abilitiesByType,
            featSelections = featIds.map { Pf2eFeatSelection(Pf2eFeatCategory.BONUS, ChoiceSelection(it), level) },
            proficiencyIds = proficiencyIds.ifEmpty { proficiencyRanks.keys },
            proficiencyRanks = proficiencyRanks,
            languages = languageRecords.map { ChoiceSelection(it.id) },
            knownSpells = spells.map { ChoiceSelection(it.id) },
            preparedSpellIds = spells.filter { it.prepared }.mapTo(mutableSetOf()) { it.id },
            features = features.map { ChoiceSelection(it.id) },
        )
    }
    val build = CharacterBuild(
        id = id,
        name = name,
        ruleset = rulesetId,
        sourceCharacterId = sourceCharacterId,
        locale = LocaleId.EN,
        portraitFileName = portraitFileName,
        portraitSourceFileName = portraitSourceFileName,
        portraitCrop = portraitCrop,
        customEntities = customEntities,
        rules = rulePayload,
    )
    val health = if (rulesetId.isFiveEdition) {
        FiveEHealthState(
            deathSaveSuccesses = deathSaveSuccesses,
            deathSaveFailures = deathSaveFailures,
            stable = isStable,
            exhaustionLevel = exhaustionLevel,
            deathReason = if (isDead) deathReason.toDomainDeathReason() ?: DeathReason.MANUAL else null,
            deathNote = deathReason,
        )
    } else {
        Pf2eHealthState(dyingValue, woundedValue, doomedValue, dead = isDead)
    }
    val resourceCandidates = features.mapNotNull { feature ->
        val current = feature.remaining ?: return@mapNotNull null
        val maximum = feature.maximum ?: return@mapNotNull null
        ResourcePool(
            id = feature.resourceId ?: feature.id,
            label = feature.name,
            current = current,
            maximum = maximum,
            recoveryRules = if (feature.id == "second-wind" && rulesetId.isFiveEdition) {
                requireNotNull(FifthEditionFeatureRules.secondWind(rulesetId).resource).recoveryRules
            } else {
                listOf(RecoveryRule(feature.recovery, RecoveryAmount.Full))
            },
            dieSides = feature.resourceDieSides,
        )
    } + resolvedSpellSlots.map { slot ->
        ResourcePool(
            id = "spell-slot-${slot.level}",
            label = "Level ${slot.level} spell slots",
            current = slot.remaining,
            maximum = slot.maximum,
            recoveryRules = listOf(RecoveryRule(app.dulliesanddungeons.domain.Recovery.LONG_REST, RecoveryAmount.Full)),
        )
    }
    val resources = resourceCandidates.groupBy { it.id }.map { (id, candidates) ->
        require(candidates.map { it.current to it.maximum }.distinct().size == 1) {
            "Shared resource $id has inconsistent counts"
        }
        candidates.first()
    }
    val state = CharacterState(
        characterId = id,
        currentHitPoints = hp,
        maximumHitPoints = maxHp,
        temporaryHitPoints = temporaryHp,
        health = health,
        resources = resources,
        conditions = characterConditions.map { it.toDomain() },
        equipment = equipmentItems.map { it.toDomain() },
        quickRolls = resolvedQuickRolls.map { it.toDomain() },
        activePlaySession = activePlaySession,
        savedPlaySessions = savedPlaySessions,
        spellSlotMaximumOverrides = spellSlotMaximumOverrides,
        spellSlotSpentCounts = spellSlots.associate { it.level to (it.maximum - it.remaining).coerceAtLeast(0) }.filterValues { it > 0 },
        hasPlayedSinceLongRest = hasPlayedSinceLongRest,
    )
    val combat = CombatProfile(
        baseSpeedsFeet = buildMap {
            put(MovementMode.WALK, baseSpeedFeet ?: speedFeet)
            (baseFlySpeedFeet ?: flySpeedFeet)?.let { put(MovementMode.FLY, it) }
        },
        armorClassMethod = ArmorClassMethod.Manual(baseArmorClass ?: armorClass),
        unarmoredArmorClass = unarmoredArmorClass,
        proficiencyBonusOverride = proficiency.takeUnless { derivation.proficiencyFromLevel },
        storedProficiencyBonus = proficiency,
        criticalHitThreshold = criticalHitThreshold,
        initiativeRollMode = initiativeRollMode,
        initiative = derivation.initiative.toDomainFormula(initiative),
        savingThrows = saves.mapNotNull { (name, value) ->
            ability(name)?.let { it to derivation.saves[name].toDomainFormula(baseSaves[name] ?: value) }
        }.toMap(),
        skills = skills.mapValues { (name, value) -> derivation.skills[name].toDomainFormula(value) },
        storedBaseArmorClass = baseArmorClass ?: armorClass,
        storedBaseSavingThrows = baseSaves.mapNotNull { (name, value) -> ability(name)?.let { it to value } }.toMap(),
        passiveArmorClassBonus = passiveArmorClassBonus,
    )
    val featureRecords = features.map { feature ->
        FeatureRecord(
            id = feature.id,
            name = feature.name,
            summary = feature.summary,
            resourceId = feature.resourceId ?: feature.id.takeIf { feature.remaining != null && feature.maximum != null },
            resourceCost = feature.resourceCost,
            actionCost = feature.actionCost,
            effectKey = feature.effect.name.lowercase(),
            custom = feature.custom,
            notes = feature.notes,
            turnGuideEligible = feature.turnGuideEligible,
            effects = feature.effects,
        )
    }
    return CharacterDocument(
        build = build,
        state = state,
        progression = progression.toDomainProgression(this, classIdsByName),
        sheet = CharacterSheetData(
            portraitSeed = portraitSeed,
            notes = notes.joinToString("\n\n") { note ->
                listOf(note.title, note.body).filter(String::isNotBlank).joinToString("\n")
            },
            noteEntries = notes,
            profile = profile,
            spellcastingSourceIds = if (hasSpellcastingCapability) setOf("class") else emptySet(),
            combat = combat,
            weapons = weapons.map { it.toDomain() },
            spells = spells.map { it.toDomain() },
            features = featureRecords,
            languages = languageRecords,
        ),
    )
}

internal fun CharacterDocument.toCharacterUi(): CharacterUi {
    val combat = sheet.combat
    val rulesetUi = build.ruleset.toUi()
    val abilitiesUi = Ability.entries.associate { it.shortName to build.abilities.getOrDefault(it, 10) }
    val primaryClass = build.classes.firstOrNull()
    val className = primaryClass?.classId?.let(::displayName) ?: "Adventurer"
    val subclass = primaryClass?.subclassId?.let(::displayName) ?: "—"
    val subclassIdsByClass = build.classes.mapNotNull { classLevel ->
        classLevel.subclassId?.let { displayName(classLevel.classId) to it }
    }.toMap()
    val subclassNamesByClass = build.classes.mapNotNull { classLevel ->
        classLevel.subclassId?.let { displayName(classLevel.classId) to displayName(it) }
    }.toMap()
    val health = state.health
    val isDead = when (health) {
        is FiveEHealthState -> health.deathReason != null || health.exhaustionLevel >= 6
        is Pf2eHealthState -> health.dead
    }
    val resources = state.resources.associateBy { it.id }
    val armorClass = when (val method = combat.armorClassMethod) {
        ArmorClassMethod.Automatic -> combat.unarmoredArmorClass ?: 10 + modifier(build.abilities.getOrDefault(Ability.DEXTERITY, 10))
        is ArmorClassMethod.Manual -> method.value
        is ArmorClassMethod.Formula -> method.base + method.flatBonus + (method.ability?.let {
            modifier(build.abilities.getOrDefault(it, 10)).let { value -> method.abilityCap?.let(value::coerceAtMost) ?: value }
        } ?: 0)
    }
    val proficiency = combat.proficiencyBonusOverride ?: if (build.ruleset.isFiveEdition) {
        2 + (build.level - 1) / 4
    } else build.level + 2
    val resolvedProficiency = proficiency.takeIf { it > 0 } ?: combat.storedProficiencyBonus ?: 0
    val initiative = combat.initiative.value(build, resolvedProficiency, storedFallback = 0)
    val progressionUi = progression.entries.map { entry ->
        LevelProgressionUi(
            level = entry.characterLevel,
            className = displayName(entry.classId),
            hitPointGain = entry.hitPoints.totalGain,
            hpMethod = entry.hitPoints.method.toUi(),
            featId = entry.selections.firstOrNull()?.id,
            hitDieSides = entry.hitPoints.dieSides,
        )
    }
    val featureUi = sheet.features.map { feature ->
        val pool = feature.resourceId?.let(resources::get)
        FeatureUi(
            id = feature.id,
            name = feature.name,
            summary = feature.summary,
            remaining = pool?.current,
            maximum = pool?.maximum,
            recovery = pool?.recoveryRules?.firstOrNull()?.trigger ?: app.dulliesanddungeons.domain.Recovery.MANUAL,
            effect = FeatureEffect.entries.firstOrNull { it.name.equals(feature.effectKey, true) } ?: FeatureEffect.RESOURCE_ONLY,
            actionCost = feature.actionCost,
            custom = feature.custom,
            notes = feature.notes,
            resourceId = feature.resourceId?.takeIf { it != feature.id },
            resourceCost = feature.resourceCost,
            resourceDieSides = pool?.dieSides,
            turnGuideEligible = feature.turnGuideEligible,
            effects = feature.effects,
        )
    }
    val raw = CharacterUi(
        id = build.id,
        name = build.name,
        ruleset = rulesetUi,
        level = build.level,
        ancestry = displayName(build.ancestryId),
        className = className,
        subclass = subclass,
        subclassIdsByClass = subclassIdsByClass,
        subclassNamesByClass = subclassNamesByClass,
        hp = state.currentHitPoints,
        maxHp = state.maximumHitPoints,
        temporaryHp = state.temporaryHitPoints,
        deathSaveSuccesses = (health as? FiveEHealthState)?.deathSaveSuccesses ?: 0,
        deathSaveFailures = (health as? FiveEHealthState)?.deathSaveFailures ?: 0,
        isStable = (health as? FiveEHealthState)?.stable == true,
        isDead = isDead,
        deathReason = (health as? FiveEHealthState)?.let { it.deathNote ?: it.deathReason?.displayName },
        exhaustionLevel = (health as? FiveEHealthState)?.exhaustionLevel ?: 0,
        dyingValue = (health as? Pf2eHealthState)?.dying ?: 0,
        woundedValue = (health as? Pf2eHealthState)?.wounded ?: 0,
        doomedValue = (health as? Pf2eHealthState)?.doomed ?: 0,
        armorClass = armorClass,
        unarmoredArmorClass = combat.unarmoredArmorClass ?: armorClass,
        speedFeet = combat.baseSpeedsFeet[MovementMode.WALK] ?: 0,
        flySpeedFeet = combat.baseSpeedsFeet[MovementMode.FLY],
        initiative = initiative,
        proficiency = resolvedProficiency,
        criticalHitThreshold = combat.criticalHitThreshold,
        initiativeRollMode = combat.initiativeRollMode,
        portraitSeed = sheet.portraitSeed,
        portraitFileName = build.portraitFileName,
        portraitSourceFileName = build.portraitSourceFileName,
        portraitCrop = build.portraitCrop,
        abilities = abilitiesUi,
        skills = combat.skills.mapValues { it.value.value(build, resolvedProficiency, 0) },
        saves = combat.savingThrows.map { (ability, formula) -> ability.displayName to formula.value(build, resolvedProficiency, 0) }.toMap(),
        languages = sheet.languages.map { it.name },
        lockedLanguages = sheet.languages.filter { it.locked }.map { it.name },
        weapons = sheet.weapons.map { it.toUi(build, resolvedProficiency) },
        spells = sheet.spells.map { it.toUi() },
        spellSlots = (1..9).mapNotNull { level ->
            val pool = resources["spell-slot-$level"]
            val spent = state.spellSlotSpentCounts[level] ?: pool?.let { (it.maximum - it.current).coerceAtLeast(0) } ?: 0
            if (pool == null && spent == 0) return@mapNotNull null
            val storageMaximum = maxOf(pool?.maximum ?: 0, spent)
            SpellSlotUi(level, (storageMaximum - spent).coerceAtLeast(0), storageMaximum)
        },
        spellSlotMaximumOverrides = state.spellSlotMaximumOverrides,
        features = featureUi,
        equipmentItems = state.equipment.map { it.toUi() },
        quickRolls = state.quickRolls.map { it.toUi() },
        progression = progressionUi,
        hitDieOverrides = (build.rules as? FiveEBuildData)?.classHitDieOverrides.orEmpty()
            .mapKeys { (classId, _) -> displayName(classId) },
        featIds = build.featIds.toList(),
        notes = sheet.noteEntries.takeIf { it.isNotEmpty() }
            ?: sheet.notes.takeIf(String::isNotBlank)?.let { listOf(CharacterNote("legacy-notes", "General", it)) }.orEmpty(),
        profile = sheet.profile,
        sourceCharacterId = build.sourceCharacterId,
        hasSpellcastingCapability = sheet.spellcastingSourceIds.isNotEmpty(),
        derivation = CharacterDerivationUi(
            proficiencyFromLevel = combat.proficiencyBonusOverride == null,
            initiative = combat.initiative.toUiFormula(),
            saves = combat.savingThrows.mapNotNull { (ability, formula) ->
                formula.toUiFormula()?.let { ability.displayName to it }
            }.toMap(),
            skills = combat.skills.mapNotNull { (name, formula) -> formula.toUiFormula()?.let { name to it } }.toMap(),
        ),
        backgroundId = build.backgroundId,
        backgroundName = displayName(build.backgroundId),
        proficiencyIds = build.proficiencyIds,
        proficiencyRanks = build.proficiencyRanks,
        activePlaySession = state.activePlaySession,
        savedPlaySessions = state.savedPlaySessions,
        hasPlayedSinceLongRest = state.hasPlayedSinceLongRest ||
            state.currentHitPoints < state.maximumHitPoints ||
            state.resources.any { pool ->
                pool.current < pool.maximum && pool.recoveryRules.any { it.trigger == app.dulliesanddungeons.domain.Recovery.SHORT_REST || it.trigger == app.dulliesanddungeons.domain.Recovery.LONG_REST }
            },
        baseAbilities = abilitiesUi,
        baseSaves = combat.storedBaseSavingThrows.takeIf { it.isNotEmpty() }
            ?.mapKeys { (ability, _) -> ability.displayName }
            ?: combat.savingThrows.map { (ability, formula) ->
                ability.displayName to formula.value(build, resolvedProficiency, 0)
            }.toMap(),
        baseArmorClass = combat.storedBaseArmorClass ?: armorClass,
        baseSpeedFeet = combat.baseSpeedsFeet[MovementMode.WALK] ?: 0,
        baseFlySpeedFeet = combat.baseSpeedsFeet[MovementMode.FLY],
        passiveArmorClassBonus = combat.passiveArmorClassBonus,
    )
    return CharacterStatEngine.resolve(raw)
}

internal fun CharacterDocument.toConditionUi(): List<ConditionUi> = state.conditions.map { condition ->
    ConditionUi(
        name = condition.displayName ?: displayId(condition.id),
        source = condition.source?.label.orEmpty(),
        duration = condition.durationLabel ?: when (condition.expiresAt) {
            ExpiryTrigger.MANUAL -> "Until removed"
            else -> displayId(condition.expiresAt.name)
        },
        explanation = condition.note.orEmpty(),
        characterId = build.id,
        id = condition.instanceId,
        level = condition.intensity,
        removable = condition.removable,
        effects = condition.effects,
    )
}

internal fun TurnSessionSnapshotUi.toDomainTurnDraft(
    current: CharacterDocument,
    characterConditions: List<ConditionUi>,
): TurnDraft {
    val flags = buildSet {
        if (flying) add(TurnGuideFlag.FLYING)
        if (flightActivationPaid) add(TurnGuideFlag.FLIGHT_ACTIVATION_PAID)
        if (dashActive) add(TurnGuideFlag.DASH_ACTIVE)
        if (actionUsed) add(TurnGuideFlag.ACTION_USED)
        if (attackActionStarted) add(TurnGuideFlag.ATTACK_ACTION_STARTED)
        if (bonusActionUsed) add(TurnGuideFlag.BONUS_ACTION_USED)
        if (reactionUsed) add(TurnGuideFlag.REACTION_USED)
    }
    val baselineDocument = baseline.toDocument(characterConditions)
    return TurnDraft(
        draftId = "turn-$characterId",
        characterId = characterId,
        ruleset = current.build.ruleset,
        currentSection = selectedSection.toDomain(),
        actionsRemaining = if (current.build.ruleset.isFiveEdition) (if (actionUsed) 0 else 1) + extraActionsRemaining else 0,
        bonusActionsRemaining = if (current.build.ruleset.isFiveEdition && !bonusActionUsed) 1 else 0,
        reactionsRemaining = if (!reactionUsed) 1 else 0,
        attacksRemaining = attacksRemaining,
        objectInteractionsRemaining = 1,
        pf2eActionsRemaining = pf2ActionsRemaining,
        pf2eAttacksMade = pf2AttacksMade,
        speedsFeet = current.sheet.combat.baseSpeedsFeet,
        movementSpentFeet = movementUsed,
        bonusMovementFeet = if (dashActive) current.sheet.combat.baseSpeedsFeet[MovementMode.WALK] ?: 0 else 0,
        selectedMovementMode = if (flying) MovementMode.FLY else MovementMode.WALK,
        requestedMovementFeet = requestedMovement,
        resources = current.state.resources.associate { it.id to it.current },
        selectedRollMode = when {
            advantage && !disadvantage -> RollMode.ADVANTAGE
            disadvantage && !advantage -> RollMode.DISADVANTAGE
            else -> RollMode.NORMAL
        },
        guideFlags = flags,
        guideSelections = TurnGuideSelections(selectedWeaponId, selectedSpellId, selectedFeatureId),
        recordedActivityCount = eventCount,
        events = events,
        completedGuideStepIds = completedSuggestionIds.toSet(),
        rollbackSnapshot = baselineDocument.toRollbackSnapshot(),
    )
}

internal fun CharacterDocument.toTurnSessionSnapshotUi(): TurnSessionSnapshotUi? {
    val turn = state.activeTurn ?: return null
    val actionUsed = TurnGuideFlag.ACTION_USED in turn.guideFlags
    val baseline = turn.rollbackSnapshot?.toDocument()?.toCharacterUi() ?: copy(
        state = state.copy(activeTurn = null),
    ).toCharacterUi()
    return TurnSessionSnapshotUi(
        characterId = build.id,
        baseline = baseline,
        selectedSection = turn.currentSection.toUi(),
        movementUsed = turn.movementSpentFeet,
        flying = TurnGuideFlag.FLYING in turn.guideFlags,
        requestedMovement = turn.requestedMovementFeet,
        dashActive = TurnGuideFlag.DASH_ACTIVE in turn.guideFlags,
        flightActivationPaid = TurnGuideFlag.FLIGHT_ACTIVATION_PAID in turn.guideFlags,
        actionUsed = actionUsed,
        attackActionStarted = TurnGuideFlag.ATTACK_ACTION_STARTED in turn.guideFlags,
        attacksRemaining = turn.attacksRemaining,
        extraActionsRemaining = (turn.actionsRemaining - if (actionUsed) 0 else 1).coerceAtLeast(0),
        pf2ActionsRemaining = turn.pf2eActionsRemaining,
        pf2AttacksMade = turn.pf2eAttacksMade,
        bonusActionUsed = TurnGuideFlag.BONUS_ACTION_USED in turn.guideFlags,
        reactionUsed = TurnGuideFlag.REACTION_USED in turn.guideFlags,
        selectedWeaponId = turn.guideSelections.weaponId,
        selectedSpellId = turn.guideSelections.spellId,
        selectedFeatureId = turn.guideSelections.featureId,
        advantage = turn.selectedRollMode == RollMode.ADVANTAGE,
        disadvantage = turn.selectedRollMode == RollMode.DISADVANTAGE,
        eventCount = turn.recordedActivityCount,
        events = turn.events,
        completedSuggestionIds = turn.completedGuideStepIds.toList(),
    )
}

private fun CharacterDocument.toRollbackSnapshot() = CharacterRollbackSnapshot(
    build = build,
    progression = progression,
    sheet = sheet,
    currentHitPoints = state.currentHitPoints,
    maximumHitPoints = state.maximumHitPoints,
    temporaryHitPoints = state.temporaryHitPoints,
    health = state.health,
    resources = state.resources,
    conditions = state.conditions,
    equipment = state.equipment,
    quickRolls = state.quickRolls,
    spellSlotMaximumOverrides = state.spellSlotMaximumOverrides,
    spellSlotSpentCounts = state.spellSlotSpentCounts,
    hasPlayedSinceLongRest = state.hasPlayedSinceLongRest,
)

private fun CharacterRollbackSnapshot.toDocument() = CharacterDocument(
    build = build,
    progression = progression,
    sheet = sheet,
    state = CharacterState(
        characterId = build.id,
        currentHitPoints = currentHitPoints,
        maximumHitPoints = maximumHitPoints,
        temporaryHitPoints = temporaryHitPoints,
        health = health,
        resources = resources,
        conditions = conditions,
        equipment = equipment,
        quickRolls = quickRolls,
        spellSlotMaximumOverrides = spellSlotMaximumOverrides,
        spellSlotSpentCounts = spellSlotSpentCounts,
        hasPlayedSinceLongRest = hasPlayedSinceLongRest,
    ),
)

private fun TurnSection.toDomain() = when (this) {
    TurnSection.Overview -> DomainTurnSection.OVERVIEW
    TurnSection.Move -> DomainTurnSection.MOVE
    TurnSection.Attack -> DomainTurnSection.ATTACK
    TurnSection.Spell -> DomainTurnSection.SPELL
    TurnSection.Other -> DomainTurnSection.OTHER
}

private fun DomainTurnSection.toUi() = when (this) {
    DomainTurnSection.OVERVIEW, DomainTurnSection.REVIEW, DomainTurnSection.DOWNED -> TurnSection.Overview
    DomainTurnSection.MOVE -> TurnSection.Move
    DomainTurnSection.ATTACK -> TurnSection.Attack
    DomainTurnSection.SPELL -> TurnSection.Spell
    DomainTurnSection.OTHER -> TurnSection.Other
}

private fun ConditionUi.toDomain() = ActiveCondition(
    id = if (name == "Exhaustion") "exhaustion" else slug(name),
    instanceId = id.ifBlank { "condition-$characterId-${slug(name)}" },
    source = EffectSource(source),
    displayName = name,
    intensity = level,
    expiresAt = ExpiryTrigger.MANUAL,
    removable = removable,
    durationLabel = duration,
    note = explanation,
    effects = effects,
)

private fun EquipmentUi.toDomain() = EquipmentItem(
    id = id,
    definitionId = definitionId,
    name = name,
    category = when (kind) {
        EquipmentKind.GEAR -> EquipmentCategory.GEAR
        EquipmentKind.ARMOR -> EquipmentCategory.ARMOR
        EquipmentKind.TOOL -> EquipmentCategory.TOOL
        EquipmentKind.CONSUMABLE -> EquipmentCategory.CONSUMABLE
        EquipmentKind.RATIONS -> EquipmentCategory.RATIONS
    },
    quantity = quantity,
    location = if (worn) activeLocation else EquipmentLocation.CARRIED,
    slot = when {
        shieldBonus > 0 -> EquipmentSlot.SHIELD
        kind == EquipmentKind.ARMOR -> EquipmentSlot.ARMOR
        else -> EquipmentSlot.OTHER
    },
    attunement = when {
        !needsAttunement -> AttunementState.NOT_REQUIRED
        attuned -> AttunementState.ATTUNED
        else -> AttunementState.UNATTUNED
    },
    details = details,
    armorClass = armorClass,
    shieldBonus = shieldBonus,
    grantedSpells = grantedSpells.map { it.toDomain() },
    activeLocation = activeLocation,
    effects = effects,
    savingThrows = savingThrows,
    useCase = useCase,
)

private fun EquipmentItem.toUi() = EquipmentUi(
    id = id,
    name = name,
    kind = when {
        category == EquipmentCategory.CONSUMABLE && definitionId == "rations" -> EquipmentKind.RATIONS
        else -> when (category) {
        EquipmentCategory.GEAR -> EquipmentKind.GEAR
        EquipmentCategory.ARMOR -> EquipmentKind.ARMOR
        EquipmentCategory.TOOL -> EquipmentKind.TOOL
        EquipmentCategory.CONSUMABLE -> EquipmentKind.CONSUMABLE
        EquipmentCategory.RATIONS -> EquipmentKind.RATIONS
        }
    },
    quantity = quantity,
    details = details,
    needsAttunement = attunement != AttunementState.NOT_REQUIRED,
    attuned = attunement == AttunementState.ATTUNED || attunement == AttunementState.INVESTED,
    worn = equipped,
    armorClass = armorClass,
    shieldBonus = shieldBonus,
    grantedSpells = grantedSpells.map { it.toUi() },
    definitionId = definitionId,
    activeLocation = activeLocation,
    effects = effects,
    savingThrows = savingThrows,
    useCase = useCase,
)

private fun WeaponUi.toDomain(): WeaponRecord {
    val expression = runCatching { DiceNotation.parse(damage.substringBefore('·').trim()) }
        .getOrElse { DiceExpression(1, 20) }
    return WeaponRecord(
        id = id,
        name = name,
        attackAbility = requireNotNull(ability(ability)),
        damageAbility = damageAbility?.let(::ability),
        damage = expression,
        damageType = damageType,
        proficient = proficient,
        proficiencyId = proficiencyId,
        itemBonus = itemBonus,
        attackBonusOverride = attackBonusOverride,
        abilityModifierOverride = abilityModifierOverride,
        storedAttackBonus = attackBonus,
        damageFormula = damage,
        range = range,
        properties = properties.split(',').map(String::trim).filter(String::isNotEmpty).toSet(),
        masteryId = mastery.takeIf(String::isNotBlank),
        attunement = when {
            !needsAttunement -> AttunementState.NOT_REQUIRED
            attuned -> AttunementState.ATTUNED
            else -> AttunementState.UNATTUNED
        },
        custom = custom,
        definitionId = definitionId,
        reachFeet = reachFeet,
        normalRangeFeet = normalRangeFeet,
        longRangeFeet = longRangeFeet,
        equipped = equipped,
        effects = effects,
        savingThrows = savingThrows,
        useCase = useCase,
    )
}

private fun WeaponRecord.toUi(build: CharacterBuild, proficiency: Int): WeaponUi {
    val ability = abilityModifierOverride ?: modifier(build.abilities.getOrDefault(attackAbility, 10))
    val automatic = ability + proficiencyFor(build, proficiencyId, proficiency, proficient) + itemBonus
    return WeaponUi(
    id = id,
    name = name,
    attackBonus = attackBonusOverride ?: automatic.takeIf { proficiencyId != null || proficient } ?: storedAttackBonus ?: automatic,
    damage = damageFormula ?: damage.format(),
    damageType = damageType,
    properties = properties.joinToString(", "),
    ability = attackAbility.shortName,
    proficient = proficient,
    proficiencyId = proficiencyId,
    itemBonus = itemBonus,
    abilityModifierOverride = abilityModifierOverride,
    attackBonusOverride = attackBonusOverride,
    range = range,
    mastery = masteryId.orEmpty(),
    needsAttunement = attunement != AttunementState.NOT_REQUIRED,
    attuned = attunement == AttunementState.ATTUNED || attunement == AttunementState.INVESTED,
    custom = custom,
    damageAbility = damageAbility?.shortName,
    definitionId = definitionId,
    reachFeet = reachFeet,
    normalRangeFeet = normalRangeFeet,
    longRangeFeet = longRangeFeet,
    equipped = equipped,
    effects = effects,
    savingThrows = savingThrows,
    useCase = useCase,
)
}

private fun SpellUi.toDomain() = SpellRecord(
    id = id,
    name = name,
    level = level,
    prepared = prepared,
    sourceKind = when (sourceKind) {
        SpellSourceKind.CLASS -> DomainSpellSourceKind.CLASS
        SpellSourceKind.FEATURE -> DomainSpellSourceKind.FEATURE
        SpellSourceKind.ITEM -> DomainSpellSourceKind.ITEM
    },
    sourceName = sourceName,
    summary = summary,
    activationCost = activationCost,
    castPreviews = castPreviews,
    savingThrows = savingThrows,
    spellAttack = spellAttack,
    spellcastingAbility = spellcastingAbility,
)

private fun SpellRecord.toUi() = SpellUi(
    id = id,
    name = name,
    level = level,
    summary = summary,
    prepared = prepared,
    sourceKind = when (sourceKind) {
        DomainSpellSourceKind.CLASS, DomainSpellSourceKind.CUSTOM -> SpellSourceKind.CLASS
        DomainSpellSourceKind.FEATURE -> SpellSourceKind.FEATURE
        DomainSpellSourceKind.ITEM -> SpellSourceKind.ITEM
    },
    sourceName = sourceName,
    activationCost = activationCost,
    castPreviews = castPreviews,
    savingThrows = savingThrows,
    spellAttack = spellAttack,
    spellcastingAbility = spellcastingAbility,
)

private fun QuickRollUi.toDomain() = QuickRollShortcut(
    kind = when (kind) {
        QuickRollKind.ABILITY -> DomainQuickRollKind.ABILITY_CHECK
        QuickRollKind.SAVE -> DomainQuickRollKind.SAVING_THROW
        QuickRollKind.SKILL -> if (id.equals("perception", true)) DomainQuickRollKind.PERCEPTION else DomainQuickRollKind.SKILL
        QuickRollKind.INITIATIVE -> DomainQuickRollKind.INITIATIVE
        QuickRollKind.DEATH_SAVE -> DomainQuickRollKind.CUSTOM
        QuickRollKind.ATTACK -> DomainQuickRollKind.ATTACK
    },
    entityId = id,
    label = label,
)

private fun QuickRollShortcut.toUi() = QuickRollUi(
    kind = when (kind) {
        DomainQuickRollKind.ABILITY_CHECK -> QuickRollKind.ABILITY
        DomainQuickRollKind.SAVING_THROW -> QuickRollKind.SAVE
        DomainQuickRollKind.PERCEPTION, DomainQuickRollKind.SKILL -> QuickRollKind.SKILL
        DomainQuickRollKind.INITIATIVE -> QuickRollKind.INITIATIVE
        DomainQuickRollKind.ATTACK -> QuickRollKind.ATTACK
        DomainQuickRollKind.CUSTOM -> if (entityId == "death-save") QuickRollKind.DEATH_SAVE else QuickRollKind.ABILITY
    },
    id = entityId,
    label = label,
)

private fun List<LevelProgressionUi>.toDomainProgression(
    character: CharacterUi,
    classIdsByName: Map<String, String>,
): ProgressionLedger {
    if (isEmpty()) {
        return ProgressionLedger(
            baselineLevel = character.level,
            baselineClassLevels = mapOf("class:${slug(character.className)}" to character.level),
        )
    }
    val classCounts = mutableMapOf<String, Int>()
    val constitution = modifier(character.abilities["CON"] ?: 10)
    return ProgressionLedger(entries = map { progress ->
        val classId = classIdsByName[progress.className] ?: "class:${slug(progress.className)}"
        val classLevel = classCounts.getOrDefault(classId, 0) + 1
        classCounts[classId] = classLevel
        val method = progress.hpMethod.toDomain()
        val dieSides = progress.hitDieSides ?: hitDie(progress.className)
        LevelProgressionEntry(
            characterLevel = progress.level,
            classId = classId,
            classLevel = classLevel,
            hitPoints = HitPointGainRecord(
                method = method,
                dieSides = dieSides,
                dieFace = if (method == HitPointGainMethod.ROLLED) (progress.hitPointGain - constitution).coerceIn(1, dieSides) else null,
                constitutionModifier = constitution,
                totalGain = progress.hitPointGain,
            ),
            selections = progress.featId?.let { listOf(ChoiceSelection(it)) }.orEmpty(),
            importedOrManual = progress.hpMethod == HpMethod.Manual,
        )
    })
}

private fun DerivedModifierFormulaUi?.toDomainFormula(stored: Int): DerivedStatisticFormula = if (this == null) {
    DerivedStatisticFormula(override = stored, storedValue = stored)
} else {
    DerivedStatisticFormula(
        ability = ability(ability),
        proficiencyMultiplier = proficiencyMultiplier,
        proficiencyId = proficiencyId,
        base = base,
        storedValue = stored,
    )
}

private fun DerivedStatisticFormula.toUiFormula(): DerivedModifierFormulaUi? =
    ability?.takeIf { override == null }?.let { DerivedModifierFormulaUi(it.shortName, proficiencyMultiplier, base, proficiencyId) }

private fun DerivedStatisticFormula.value(build: CharacterBuild, proficiency: Int, storedFallback: Int): Int {
    override?.let { return it }
    val sourceAbility = ability ?: return storedValue ?: storedFallback
    return base + itemBonus + modifier(build.abilities.getOrDefault(sourceAbility, 10)) +
        if (proficiencyId != null) proficiencyFor(build, proficiencyId, proficiency) else proficiencyMultiplier * proficiency
}

private fun proficiencyFor(
    build: CharacterBuild,
    proficiencyId: String?,
    fiveEProficiency: Int,
    legacyProficient: Boolean = false,
): Int {
    val id = proficiencyId ?: return if (legacyProficient) fiveEProficiency else 0
    val rank = build.proficiencyRanks[id] ?: ProficiencyRank.TRAINED.takeIf { id in build.proficiencyIds }
    return when {
        rank == null -> 0
        build.ruleset.isFiveEdition -> fiveEProficiency
        else -> build.level + rank.rankBonus
    }
}

private fun HpMethod.toDomain() = when (this) {
    HpMethod.Fixed -> HitPointGainMethod.FIXED
    HpMethod.Rolled -> HitPointGainMethod.ROLLED
    HpMethod.Manual -> HitPointGainMethod.MANUAL_OVERRIDE
}

private fun HitPointGainMethod.toUi() = when (this) {
    HitPointGainMethod.FIRST_LEVEL_MAXIMUM, HitPointGainMethod.FIXED -> HpMethod.Fixed
    HitPointGainMethod.ROLLED -> HpMethod.Rolled
    HitPointGainMethod.MANUAL_OVERRIDE -> HpMethod.Manual
}

private fun Ruleset.toDomain() = when (this) {
    Ruleset.Fifth2024 -> RulesetId.FIFTH_EDITION_2024
    Ruleset.Fifth2014 -> RulesetId.FIFTH_EDITION_2014
    Ruleset.Pf2eRemaster -> RulesetId.PF2E_REMASTER
}

private fun RulesetId.toUi() = when (this) {
    RulesetId.FIFTH_EDITION_2024 -> Ruleset.Fifth2024
    RulesetId.FIFTH_EDITION_2014 -> Ruleset.Fifth2014
    RulesetId.PF2E_REMASTER -> Ruleset.Pf2eRemaster
}

private fun String?.toDomainDeathReason(): DeathReason? = when (this) {
    "Failed death saves" -> DeathReason.DEATH_SAVE_FAILURES
    "Massive damage" -> DeathReason.MASSIVE_DAMAGE
    "Exhaustion" -> DeathReason.EXHAUSTION
    else -> null
}

private val DeathReason.displayName: String get() = when (this) {
    DeathReason.DEATH_SAVE_FAILURES -> "Failed death saves"
    DeathReason.MASSIVE_DAMAGE -> "Massive damage"
    DeathReason.EXHAUSTION -> "Exhaustion"
    DeathReason.MAXIMUM_HIT_POINTS_ZERO -> "Maximum Hit Points are zero"
    DeathReason.MANUAL -> "Marked dead"
}

private fun CharacterDocument.displayName(id: String): String = build.customEntities[id]?.name ?: displayId(id.substringAfter(':'))

private val Ability.shortName: String get() = name.take(3)
private val Ability.displayName: String get() = name.lowercase().replaceFirstChar { it.uppercase() }

private fun ability(value: String): Ability? = when (value.uppercase().take(3)) {
    "STR" -> Ability.STRENGTH
    "DEX" -> Ability.DEXTERITY
    "CON" -> Ability.CONSTITUTION
    "INT" -> Ability.INTELLIGENCE
    "WIS" -> Ability.WISDOM
    "CHA" -> Ability.CHARISMA
    else -> null
}

private fun DiceExpression.format(): String = buildString {
    append(count).append('d').append(sides)
    if (modifier > 0) append(" + ").append(modifier)
    if (modifier < 0) append(" - ").append(-modifier)
}

private fun modifier(score: Int): Int = score / 2 - 5

private fun hitDie(className: String): Int = when (className) {
    "Barbarian" -> 12
    "Fighter", "Paladin", "Ranger" -> 10
    "Sorcerer", "Wizard" -> 6
    else -> 8
}

private fun slug(value: String): String = value.lowercase().map { if (it.isLetterOrDigit()) it else '-' }
    .joinToString("").replace(Regex("-+"), "-").trim('-').ifBlank { "entry" }

private fun displayId(value: String): String = value.replace('-', ' ').replace('_', ' ')
    .split(' ').filter(String::isNotBlank).joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
