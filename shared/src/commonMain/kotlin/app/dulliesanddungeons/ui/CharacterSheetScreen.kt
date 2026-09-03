package app.dulliesanddungeons.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import app.dulliesanddungeons.domain.ActiveConcentration
import app.dulliesanddungeons.domain.CharacterNote
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.RollMode
import app.dulliesanddungeons.domain.CombatContributionTiming
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
internal fun CharacterSheetScreen(
    state: DndAppState,
    onPickPortrait: (PortraitPickTarget) -> Unit,
    onEditPortrait: (PortraitPickTarget) -> Unit,
) {
    val character = state.selectedCharacter
    if (character == null) {
        state.screen = AppScreen.Characters
        return
    }
    val listState = rememberLazyListState()
    val activeAttackOption = state.currentSheetAttackOption()
    val activeWeapon = state.currentSheetAttackWeapon()
    var languagesEditorOpen by remember(character.id) { mutableStateOf(false) }
    var customFeatureEditorOpen by remember(character.id) { mutableStateOf(false) }
    var editingCustomFeature by remember(character.id) { mutableStateOf<FeatureUi?>(null) }
    var portraitViewerOpen by remember(character.id) { mutableStateOf(false) }
    var profileViewerOpen by remember(character.id) { mutableStateOf(false) }
    var noteEditorOpen by remember(character.id) { mutableStateOf(false) }
    var editingNote by remember(character.id) { mutableStateOf<CharacterNote?>(null) }
    var pendingNoteDeletion by remember(character.id) { mutableStateOf<CharacterNote?>(null) }
    var pendingQuickCastSpell by remember(character.id) { mutableStateOf<SpellUi?>(null) }
    var sorceryRecoveryOpen by remember(character.id) { mutableStateOf(false) }
    var longRestDialogOpen by remember(character.id) { mutableStateOf(false) }
    var characterDeletionRequested by remember(character.id) { mutableStateOf(false) }
    var diceRollerOpen by remember(character.id) { mutableStateOf(false) }
    var potentialDialogKind by remember(character.id) { mutableStateOf<PotentialDialogKind?>(null) }
    val attackPotential = CombatPotentialEngine.attacks(character, state.selectedConditions)
    val castPotential = CombatPotentialEngine.casts(character)
    val expandedSections = remember(character.id) { mutableStateMapOf<String, Boolean>() }
    fun sectionExpanded(key: String): Boolean = expandedSections[key] ?: (key != "rests")
    fun toggleSection(key: String) { expandedSections[key] = !sectionExpanded(key) }
    val groupedFeatures = character.features.groupBy(::featureFamily)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .paperTexture()
                .then(if (activeWeapon != null) Modifier.blur(8.dp) else Modifier),
        ) {
            SheetTopBar(
                state = state,
                character = character,
                onProfileClick = { profileViewerOpen = true },
                onEdit = state::beginEdit,
                onDelete = { characterDeletionRequested = true },
            )
            character.activeConcentration?.let { concentration ->
                ConcentrationBanner(state, concentration)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { SearchPrompt(state) }
                item {
                    HeroSummaryCard(
                        state = state,
                        character = character,
                        restsExpanded = sectionExpanded("rests"),
                        onRestsToggle = { toggleSection("rests") },
                        onLongRest = { longRestDialogOpen = true },
                        onProfileClick = { profileViewerOpen = true },
                        onPortraitClick = { hasPortrait ->
                            if (hasPortrait) portraitViewerOpen = true
                            else onPickPortrait(PortraitPickTarget.Character(character.id))
                        },
                    )
                }
                if (character.hp == 0 || character.isDead) item { DownedStatusCard(state, character) }
                item { TurnTrackerCard(state, character) }

                if (state.selectedConditions.isNotEmpty()) {
                    item {
                        CollapsibleSectionHeader(
                            state = state,
                            title = state.t("Active conditions", "Aktive Zustände"),
                            expanded = sectionExpanded("conditions"),
                            onToggle = { toggleSection("conditions") },
                        ) {
                            TextButton(onClick = { state.conditionsOpen = true }) { Text(state.t("Manage", "Verwalten")) }
                        }
                    }
                    if (sectionExpanded("conditions")) {
                        items(state.selectedConditions, key = { it.id.ifBlank { "condition-${it.name}-${it.source}-${it.characterId}" } }) { condition ->
                            ConditionRow(state, condition)
                        }
                    }
                }

                item { RollShortcuts(state, character, sectionExpanded("quick-rolls")) { toggleSection("quick-rolls") } }
                item { AbilityScores(state, character, sectionExpanded("abilities")) { toggleSection("abilities") } }
                item {
                    RollGrid(
                        state,
                        state.t("Saving throws", "Rettungswürfe"),
                        character.saves.map { saveAbbreviation(it.key) to it.value },
                        sectionExpanded("saving-throws"),
                        { toggleSection("saving-throws") },
                    ) { name ->
                        abilityFromUiName(name)?.let { ability ->
                            val calculation = CharacterStatEngine.savingThrow(character, ability, state.selectedConditions)
                            state.showInfo("${ability.displayName()} ${state.t("saving throw", "Rettungswurf")}", calculation.detailsText(state))
                        }
                    }
                }
                item { RollGrid(state, state.t("Skills", "Fertigkeiten"), character.skills.map { state.localizedSkillName(it.key) to it.value }, sectionExpanded("skills"), onToggle = { toggleSection("skills") }) }

                FeatureFamily.entries.filter { it != FeatureFamily.General }.forEach { family ->
                    val familyFeatures = groupedFeatures[family].orEmpty()
                    val sharedPool = when (family) {
                        FeatureFamily.Maneuvers -> familyFeatures.firstOrNull { it.id == "superiority-dice" }
                        FeatureFamily.Metamagic -> familyFeatures.firstOrNull { it.id == "sorcery-points" }
                        else -> null
                    }
                    val features = familyFeatures.filterNot { it.id == sharedPool?.id }
                    val sectionKey = "feature-${family.name}"
                    if (familyFeatures.isNotEmpty()) {
                        item(key = "feature-family-${family.name}") {
                            CollapsibleSectionHeader(
                                state,
                                featureFamilyLabel(state, family),
                                sectionExpanded(sectionKey),
                                { toggleSection(sectionKey) },
                            ) {
                                if (sharedPool != null) {
                                    SharedResourceStat(state, sharedPool)
                                }
                            }
                        }
                        if (sectionExpanded(sectionKey)) {
                            items(features, key = { "${family.name}-${it.id}" }) { feature ->
                                FeatureCard(state, feature, onEdit = {
                                    editingCustomFeature = feature
                                    customFeatureEditorOpen = true
                                })
                            }
                        }
                    }
                }
                item(key = "feature-family-general") {
                    val generalFeatures = groupedFeatures[FeatureFamily.General].orEmpty()
                    CollapsibleSectionHeader(
                        state,
                        featureFamilyLabel(state, FeatureFamily.General),
                        generalFeatures.isNotEmpty() && sectionExpanded("feature-General"),
                        { toggleSection("feature-General") },
                        enabled = generalFeatures.isNotEmpty(),
                    ) {
                        TextButton(onClick = { editingCustomFeature = null; customFeatureEditorOpen = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(state.t("Custom", "Eigenes"))
                        }
                    }
                }
                if (groupedFeatures[FeatureFamily.General].orEmpty().isNotEmpty() && sectionExpanded("feature-General")) {
                    items(groupedFeatures[FeatureFamily.General].orEmpty(), key = { "general-${it.id}" }) { feature ->
                        FeatureCard(state, feature, onEdit = {
                            editingCustomFeature = feature
                            customFeatureEditorOpen = true
                        })
                    }
                }

                if (character.canCastSpells) {
                    item {
                        CollapsibleSectionHeader(state, state.t("Spells", "Zauber"), sectionExpanded("spells"), { toggleSection("spells") }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (character.ruleset != Ruleset.Pf2eRemaster) {
                                    PotentialBadge(state, state.t("Casts", "Zauber"), castPotential) { potentialDialogKind = PotentialDialogKind.Casts }
                                }
                                IconButton(onClick = { state.beginEdit(section = EditorSection.Spells) }, modifier = Modifier.size(42.dp)) {
                                    Icon(Icons.Rounded.Edit, contentDescription = state.t("Edit spells", "Zauber bearbeiten"), Modifier.size(19.dp))
                                }
                            }
                        }
                    }
                    if (sectionExpanded("spells")) {
                        if (character.ruleset != Ruleset.Pf2eRemaster && character.fiveECasterLevel > 0) {
                            item { SpellSlotCounter(state, character, onRegain = { sorceryRecoveryOpen = true }) }
                        }
                        if (character.availableSpells.isEmpty()) {
                            item { ExplanationCard(state.t("Spellcasting available", "Zaubern verfügbar"), state.t("This character can cast spells, but none are currently prepared or granted.", "Dieser Charakter kann zaubern, aber derzeit ist kein Zauber vorbereitet oder gewährt.")) }
                        } else {
                            character.availableSpells.groupBy { it.level }.toSortedMap().forEach { (level, levelSpells) ->
                                item(key = "spell-level-$level") {
                                    Text(
                                        if (level == 0) state.t("Cantrips", "Zaubertricks") else state.t("Level $level", "Grad $level"),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                items(
                                    levelSpells.sortedForPicker(state.language, SpellUi::name, { "${it.sourceKind}-${it.sourceName}-${it.id}" }),
                                    key = { "${it.sourceKind}-${it.sourceName}-${it.id}" },
                                ) { spell ->
                                    SpellRow(state, spell) {
                                        val slotLevels = state.availableSpellSlotLevels(spell)
                                        when {
                                            spell.level == 0 -> state.castSpell(spell, session = null)
                                            slotLevels.size == 1 -> state.castSpell(spell, slotLevels.single(), session = null)
                                            slotLevels.size > 1 -> pendingQuickCastSpell = spell
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    CollapsibleSectionHeader(state, state.t("Weapons", "Waffen"), sectionExpanded("weapons"), { toggleSection("weapons") }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (character.ruleset != Ruleset.Pf2eRemaster) {
                                PotentialBadge(state, state.t("Attacks", "Angriffe"), attackPotential) { potentialDialogKind = PotentialDialogKind.Attacks }
                            }
                            TextButton(onClick = { state.openItemBrowser() }) {
                                Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(state.t("Add", "Hinzufügen"))
                            }
                        }
                    }
                }
                if (sectionExpanded("weapons")) {
                    items(character.weapons, key = { it.id }) { weapon -> WeaponRow(state, character, weapon) }
                }

                item {
                    CollapsibleSectionHeader(state, state.t("Equipment", "Ausrüstung"), sectionExpanded("equipment"), { toggleSection("equipment") }) {
                        TextButton(onClick = { state.openItemBrowser() }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(state.t("Add", "Hinzufügen"))
                        }
                    }
                }
                if (sectionExpanded("equipment")) {
                    item { CurrencyOverview(state, character) }
                    item { EquipmentCard(state, character) }
                }

                item {
                    CollapsibleSectionHeader(state, state.t("Languages", "Sprachen"), sectionExpanded("languages"), { toggleSection("languages") }) {
                        IconButton(onClick = { languagesEditorOpen = true }, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = state.t("Edit languages", "Sprachen bearbeiten"), Modifier.size(19.dp))
                        }
                    }
                }
                if (sectionExpanded("languages")) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(character.languages) { language ->
                                AssistChip(
                                    onClick = { state.showInfo(language, state.t("Spoken, read and written unless a rule says otherwise.", "Gesprochen, gelesen und geschrieben, sofern keine Regel etwas anderes sagt.")) },
                                    label = { Text(language) },
                                    leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = null, Modifier.size(18.dp)) },
                                )
                            }
                        }
                    }
                }

                item {
                    CollapsibleSectionHeader(state, state.t("Notes", "Notizen"), sectionExpanded("notes"), { toggleSection("notes") }) {
                        IconButton(
                            onClick = { editingNote = null; noteEditorOpen = true },
                            modifier = Modifier.size(42.dp),
                        ) { Icon(Icons.Rounded.Add, contentDescription = state.t("Add note", "Notiz hinzufügen")) }
                    }
                }
                if (sectionExpanded("notes")) {
                    if (character.notes.isEmpty()) {
                        item { EmptyNotesCard(state) }
                    } else {
                        items(character.notes, key = { "note-${it.id}" }) { note ->
                            NoteCard(
                                state = state,
                                note = note,
                                onEdit = { editingNote = note; noteEditorOpen = true },
                                onDelete = { pendingNoteDeletion = note },
                            )
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { state.conversionOpen = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(state.t("Convert ruleset", "Regelwerk konvertieren"))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = activeWeapon == null && !diceRollerOpen,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).zIndex(2f),
            enter = fadeIn() + scaleIn(initialScale = .72f),
            exit = fadeOut() + scaleOut(targetScale = .72f),
        ) {
            FloatingActionButton(
                onClick = { diceRollerOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    Icons.Rounded.Casino,
                    contentDescription = state.t("Open dice roller", "Würfelroller öffnen"),
                )
            }
        }

        AnimatedVisibility(
            visible = activeWeapon != null,
            modifier = Modifier.fillMaxSize().zIndex(3f),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(color = MaterialTheme.colorScheme.background.copy(alpha = .76f), modifier = Modifier.fillMaxSize()) {}
        }

        AnimatedVisibility(
            visible = activeWeapon != null,
            modifier = Modifier.align(Alignment.Center).padding(14.dp).widthIn(max = 560.dp).zIndex(4f),
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.96f),
        ) {
            activeWeapon?.let { weapon ->
                AttackCard(
                    state = state,
                    character = character,
                    weapon = weapon,
                    attackOption = activeAttackOption,
                    roll = state.sheetAttackRoll,
                    damage = state.sheetDamageRoll,
                    outcome = state.sheetAttackOutcome,
                    canRoll = state.canRollCurrentSheetAttack(),
                    onRoll = {
                        state.rollSheetAttack(RollMode.NORMAL)
                        state.dicePresentation = null
                    },
                    onDamage = {
                        state.rollSheetDamage()
                        state.dicePresentation = null
                    },
                    onOutcome = { outcome ->
                        state.resolveSheetAttack(outcome)
                        state.dicePresentation = null
                    },
                    onDismiss = state::closeSheetAttack,
                )
            }
        }
    }

    if (diceRollerOpen) {
        DiceRollerDialog(state = state, onDismiss = { diceRollerOpen = false })
    }

    if (characterDeletionRequested) {
        DeleteCharacterConfirmationDialog(
            state = state,
            character = character,
            onDismiss = { characterDeletionRequested = false },
            onConfirm = {
                state.deleteCharacter(character.id)
                characterDeletionRequested = false
            },
        )
    }

    if (languagesEditorOpen) {
        LanguagesEditorDialog(state, character) { languagesEditorOpen = false }
    }
    if (customFeatureEditorOpen) {
        CustomFeatureEditorDialog(state, editingCustomFeature) {
            customFeatureEditorOpen = false
            editingCustomFeature = null
        }
    }
    if (profileViewerOpen) {
        CharacterProfileDialog(
            state = state,
            character = character,
            onDismiss = { profileViewerOpen = false },
            onEdit = {
                profileViewerOpen = false
                state.beginEdit(section = EditorSection.Identity)
            },
        )
    }
    if (noteEditorOpen) {
        NoteEditorDialog(
            state = state,
            note = editingNote,
            onDismiss = { noteEditorOpen = false; editingNote = null },
            onDelete = { note ->
                noteEditorOpen = false
                editingNote = null
                pendingNoteDeletion = note
            },
        )
    }
    pendingNoteDeletion?.let { note ->
        ConfirmNoteDeletionDialog(
            state = state,
            note = note,
            onDismiss = { pendingNoteDeletion = null },
            onConfirm = {
                state.removeNote(note.id)
                pendingNoteDeletion = null
            },
        )
    }
    val portraitData = state.portraitBytes(character)
    if (portraitViewerOpen && portraitData != null) {
        PortraitViewerDialog(
            state = state,
            character = character,
            portraitData = portraitData,
            onDismiss = { portraitViewerOpen = false },
            onExchange = { onPickPortrait(PortraitPickTarget.Character(character.id)) },
            onEdit = { onEditPortrait(PortraitPickTarget.Character(character.id)) },
            onDelete = {
                if (state.deleteCharacterPortrait(character.id)) portraitViewerOpen = false
            },
        )
    }
    LaunchedEffect(portraitViewerOpen, portraitData) {
        if (portraitViewerOpen && portraitData == null) portraitViewerOpen = false
    }
    pendingQuickCastSpell?.let { spell ->
        QuickCastSlotDialog(
            state = state,
            spell = spell,
            onDismiss = { pendingQuickCastSpell = null },
            onCast = { level ->
                state.castSpell(spell, level, session = null)
                pendingQuickCastSpell = null
            },
        )
    }
    if (sorceryRecoveryOpen) {
        SorcerySlotRecoveryDialog(state, character) { sorceryRecoveryOpen = false }
    }
    if (longRestDialogOpen) {
        LongRestDialog(state, character) { longRestDialogOpen = false }
    }
    potentialDialogKind?.let { kind ->
        val potential = if (kind == PotentialDialogKind.Attacks) attackPotential else castPotential
        CombatPotentialDialog(state, kind, potential) { potentialDialogKind = null }
    }
}

private enum class PotentialDialogKind { Attacks, Casts }

@Composable
private fun PotentialBadge(
    state: DndAppState,
    label: String,
    potential: TurnPotentialUi,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics {
            role = Role.Button
            contentDescription = state.t(
                "$label per turn potential: ${potential.baseCount} normal plus ${potential.extraCount} extra. Show details.",
                "$label pro Zug: ${potential.baseCount} normal plus ${potential.extraCount} zusätzlich. Details anzeigen.",
            )
        },
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(13.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(potential.compactLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CombatPotentialDialog(
    state: DndAppState,
    kind: PotentialDialogKind,
    potential: TurnPotentialUi,
    onDismiss: () -> Unit,
) {
    val subject = if (kind == PotentialDialogKind.Attacks) state.t("Attack", "Angriffs") else state.t("Cast", "Zauber")
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (kind == PotentialDialogKind.Attacks) Icons.Rounded.SportsMma else Icons.Rounded.AutoAwesome, contentDescription = null) },
        title = { Text(state.t("$subject potential · ${potential.compactLabel}", "$subject-Potenzial · ${potential.compactLabel}")) },
        text = {
            Column(
                Modifier.heightIn(max = 570.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    state.t(
                        "Maximum for one of your turns with full resources, valid owned gear, and the listed setup.",
                        "Maximum für einen deiner Züge mit vollen Ressourcen, gültiger eigener Ausrüstung und dem aufgeführten Aufbau.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(state.t("BEST COMBINATION", "BESTE KOMBINATION"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                PotentialTreeRow(
                    connector = if (potential.bestSources.isEmpty()) "└─" else "├─",
                    label = when (potential.baseLabel) {
                        "Attack action" -> state.t("Attack action", "Angriffsaktion")
                        "Cast a Spell action" -> state.t("Cast a Spell action", "Zauber-wirken-Aktion")
                        "Magic action" -> state.t("Magic action", "Magieaktion")
                        else -> potential.baseLabel
                    },
                    count = potential.baseCount,
                    showPlus = false,
                )
                potential.bestSources.forEachIndexed { index, source ->
                    PotentialTreeRow(
                        connector = if (index == potential.bestSources.lastIndex) "└─" else "├─",
                        label = source.label,
                        count = source.count,
                        detail = potentialDetail(state, source),
                    )
                }
                if (potential.alternatives.isNotEmpty()) {
                    HorizontalDivider()
                    Text(state.t("ALTERNATIVES", "ALTERNATIVEN"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(
                        state.t("These use a conflicting action or require a different sequence.", "Diese benötigen eine kollidierende Aktion oder eine andere Reihenfolge."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    potential.alternatives.forEach { source ->
                        PotentialTreeRow("└─", source.label, source.count, potentialDetail(state, source))
                    }
                }
                if (potential.variableSources.isNotEmpty()) {
                    HorizontalDivider()
                    Text(state.t("DEPENDS ON TARGETS", "ABHÄNGIG VON ZIELEN"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    potential.variableSources.forEach { source ->
                        PotentialTreeRow("└─", source.label, null, potentialDetail(state, source))
                    }
                }
                HorizontalDivider()
                Text(
                    state.t(
                        "Reactions, save-only effects, summons, and attacks made by controlled creatures are not counted.",
                        "Reaktionen, reine Rettungswurfeffekte, Beschwörungen und Angriffe kontrollierter Kreaturen werden nicht gezählt.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(state.t("Done", "Fertig")) } },
    )
}

@Composable
private fun PotentialTreeRow(connector: String, label: String, count: Int?, detail: String = "", showPlus: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(connector, Modifier.width(28.dp), color = MaterialTheme.colorScheme.outline)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(count?.let { if (showPlus) "+$it" else "$it" } ?: "?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

private fun potentialDetail(state: DndAppState, source: PotentialSourceUi): String = buildList {
    add(when (source.timing) {
        CombatContributionTiming.ATTACK_ACTION -> state.t("within the Attack action", "innerhalb der Angriffsaktion")
        CombatContributionTiming.ACTION -> state.t("action", "Aktion")
        CombatContributionTiming.BONUS_ACTION -> state.t("Bonus Action", "Bonusaktion")
        CombatContributionTiming.EXTRA_ACTION -> state.t("additional action", "zusätzliche Aktion")
        CombatContributionTiming.TRIGGERED -> state.t("triggered", "ausgelöst")
    })
    if (source.resourceCost > 0 && source.resourceName.isNotBlank()) add("${source.resourceCost} ${source.resourceName}")
    if (source.requiresAttackAction) add(state.t("requires the Attack action", "benötigt die Angriffsaktion"))
    if (source.requiresActionCantripForAnotherCast) add(state.t("other casts must be action cantrips", "andere Zauber müssen Aktions-Zaubertricks sein"))
    if (source.requiresSetup) add(state.t("setup required", "Aufbau erforderlich"))
    if (source.requiresHit) add(state.t("requires a hit", "Treffer erforderlich"))
    if (source.requiresAdditionalTarget) add(state.t("additional target required", "zusätzliches Ziel erforderlich"))
    if (source.note.isNotBlank() && source.note != source.label) add(source.note)
}.joinToString(" · ")

@Composable
private fun ConcentrationBanner(state: DndAppState, concentration: ActiveConcentration) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f)
                    .clickable(onClick = state::showActiveConcentrationDetails)
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        state.t("CONCENTRATING", "KONZENTRATION"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        concentration.spellName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.t(
                            "${state.concentrationRemainingLabel(concentration)} remaining",
                            "${state.concentrationRemainingLabel(concentration)} verbleibend",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            IconButton(
                onClick = { state.endConcentration() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = state.t(
                        "End concentration on ${concentration.spellName}",
                        "Konzentration auf ${concentration.spellName} beenden",
                    ),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun PortraitViewerDialog(
    state: DndAppState,
    character: CharacterUi,
    portraitData: ByteArray,
    onDismiss: () -> Unit,
    onExchange: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember(character.id) { mutableStateOf(false) }
    val portrait = remember(portraitData) { runCatching { portraitData.decodeToImageBitmap() }.getOrNull() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = state.t("Close portrait", "Porträt schließen"), tint = Color.White)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    portrait?.let {
                        Image(
                            bitmap = it,
                            contentDescription = character.name,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text(state.t("Edit crop", "Zuschnitt bearbeiten"))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(onClick = onExchange, modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(7.dp))
                                Text(state.t("Replace", "Ersetzen"))
                            }
                            TextButton(
                                onClick = { confirmDelete = true },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null)
                                Spacer(Modifier.width(7.dp))
                                Text(state.t("Delete", "Löschen"))
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(state.t("Delete portrait?", "Porträt löschen?")) },
            text = { Text(state.t("The uploaded portrait will be removed from this character.", "Das hochgeladene Porträt wird von diesem Charakter entfernt.")) },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(state.t("Cancel", "Abbrechen")) }
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(state.t("Delete", "Löschen"), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

@Composable
private fun CharacterProfileDialog(
    state: DndAppState,
    character: CharacterUi,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(character.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileField(state.t("Character", "Charakter"), character.profile.characterDescription)
                ProfileField(state.t("Motive", "Motiv"), character.profile.motive)
                ProfileField(
                    state.t("Alignment", "Gesinnung"),
                    alignmentDisplayName(character.profile.alignment, state.language),
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("Close", "Schließen")) } },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(state.t("Edit", "Bearbeiten"))
            }
        },
    )
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NoteEditorDialog(
    state: DndAppState,
    note: CharacterNote?,
    onDismiss: () -> Unit,
    onDelete: (CharacterNote) -> Unit,
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var body by remember(note?.id) { mutableStateOf(note?.body.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) state.t("Add note", "Notiz hinzufügen") else state.t("Edit note", "Notiz bearbeiten")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(state.t("Title", "Titel")) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it.take(4_000) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(state.t("Note", "Notiz")) },
                    minLines = 4,
                    maxLines = 10,
                )
            }
        },
        dismissButton = {
            Row {
                if (note != null) {
                    TextButton(onClick = { onDelete(note) }) {
                        Text(state.t("Delete", "Löschen"), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(state.t("Cancel", "Abbrechen")) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (note == null) state.addNote(title, body) else state.updateNote(note.id, title, body)
                    onDismiss()
                },
                enabled = title.isNotBlank(),
            ) { Text(state.t("Save", "Speichern")) }
        },
    )
}

@Composable
private fun ConfirmNoteDeletionDialog(
    state: DndAppState,
    note: CharacterNote,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.t("Delete note?", "Notiz löschen?")) },
        text = { Text(note.title) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("Cancel", "Abbrechen")) } },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(state.t("Delete", "Löschen"), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Composable
private fun LanguagesEditorDialog(state: DndAppState, character: CharacterUi, onDismiss: () -> Unit) {
    val editableLanguages = character.languages.filterNot { it in character.lockedLanguages }
    var text by remember(character.id, character.languages) { mutableStateOf(editableLanguages.joinToString(", ")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Translate, contentDescription = null) },
        title = { Text(state.t("Edit languages", "Sprachen bearbeiten")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (character.lockedLanguages.isNotEmpty()) {
                    Text(state.t("From your rules: ${character.lockedLanguages.joinToString()}", "Aus deinen Regeln: ${character.lockedLanguages.joinToString()}"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(300) },
                    label = { Text(state.t("Other languages", "Weitere Sprachen")) },
                    supportingText = { Text(state.t("Separate languages with commas or new lines.", "Trenne Sprachen durch Kommas oder neue Zeilen.")) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("Cancel", "Abbrechen")) } },
        confirmButton = {
            Button(onClick = {
                state.updateLanguages(text.split(Regex("[,\\n]")).map(String::trim))
                onDismiss()
            }) { Text(state.t("Save", "Speichern")) }
        },
    )
}

@Composable
private fun CustomFeatureEditorDialog(state: DndAppState, feature: FeatureUi?, onDismiss: () -> Unit) {
    var name by remember(feature?.id) { mutableStateOf(feature?.name.orEmpty()) }
    var summary by remember(feature?.id) { mutableStateOf(feature?.summary.orEmpty()) }
    var notes by remember(feature?.id) { mutableStateOf(feature?.notes.orEmpty()) }
    var combatContributions by remember(feature?.id) { mutableStateOf(feature?.combatContributions.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Stars, contentDescription = null) },
        title = { Text(if (feature == null) state.t("Add custom feature", "Eigenes Merkmal hinzufügen") else state.t("Edit custom feature", "Eigenes Merkmal bearbeiten")) },
        text = {
            Column(Modifier.heightIn(max = 570.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it.take(80) }, label = { Text(state.t("Name", "Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(summary, { summary = it.take(300) }, label = { Text(state.t("Short effect", "Kurzer Effekt")) }, modifier = Modifier.fillMaxWidth())
                if (feature != null) OutlinedTextField(notes, { notes = it.take(500) }, label = { Text(state.t("Notes (optional)", "Notizen (optional)")) }, modifier = Modifier.fillMaxWidth())
                CombatContributionEditor(state, combatContributions) { combatContributions = it }
            }
        },
        dismissButton = {
            Row {
                if (feature != null) {
                    TextButton(onClick = { state.removeCustomFeature(feature.id); onDismiss() }) {
                        Text(state.t("Delete", "Löschen"), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(state.t("Cancel", "Abbrechen")) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (feature == null) state.addCustomFeature(name, summary, combatContributions)
                    else state.updateCustomFeature(feature.id, name, summary, notes, combatContributions)
                    onDismiss()
                },
                enabled = name.isNotBlank(),
            ) { Text(state.t("Save", "Speichern")) }
        },
    )
}

@Composable
private fun SheetTopBar(
    state: DndAppState,
    character: CharacterUi,
    onProfileClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { state.closeSheetAttack(); state.screen = AppScreen.Characters }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = state.t("All characters", "Alle Charaktere"))
        }
        Surface(
            onClick = onProfileClick,
            modifier = Modifier.weight(1f),
            color = Color.Transparent,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                character.name,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LanguageButton(state)
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = state.t("More options", "Weitere Optionen"))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(state.t("Edit character", "Charakter bearbeiten")) },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    onClick = { menuOpen = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text(state.t("Local content", "Lokale Inhalte")) },
                    leadingIcon = { Icon(Icons.Rounded.Backpack, contentDescription = null) },
                    onClick = { menuOpen = false; state.privateContentOpen = true },
                )
                DropdownMenuItem(
                    text = { Text(state.t("Credits", "Lizenzen")) },
                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        state.showInfo(
                            state.t("Credits", "Lizenzen"),
                            state.t(
                                "Alegreya and Alegreya Sans by Huerta Tipográfica (SIL Open Font License 1.1). Material Icons by Google (Apache License 2.0). Dice symbols are original in-app vector drawings.",
                                "Alegreya und Alegreya Sans von Huerta Tipográfica (SIL Open Font License 1.1). Material Icons von Google (Apache License 2.0). Würfelsymbole sind originale In-App-Vektorzeichnungen.",
                            ),
                        )
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            state.t("Delete character", "Charakter löschen"),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun SearchPrompt(state: DndAppState) {
    Surface(
        onClick = { state.searchOpen = true },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(9.dp))
            Text(state.t("Search character or rules", "Charakter oder Regeln suchen"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroSummaryCard(
    state: DndAppState,
    character: CharacterUi,
    restsExpanded: Boolean,
    onRestsToggle: () -> Unit,
    onLongRest: () -> Unit,
    onProfileClick: () -> Unit,
    onPortraitClick: (Boolean) -> Unit,
) {
    val portraitData = state.portraitBytes(character)
    val supportsInspiration = character.ruleset != Ruleset.Pf2eRemaster
    val inspirationActive = state.hasInspiration
    Card(
        onClick = onProfileClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(76.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                            .then(
                                if (supportsInspiration) {
                                    Modifier.combinedClickable(
                                        onClick = { onPortraitClick(portraitData != null) },
                                        onLongClick = { state.toggleInspiration() },
                                        onLongClickLabel = if (inspirationActive) {
                                            state.t("Remove Inspiration", "Inspiration entfernen")
                                        } else {
                                            state.t("Add Inspiration", "Inspiration hinzufügen")
                                        },
                                    )
                                } else {
                                    Modifier.clickable { onPortraitClick(portraitData != null) }
                                },
                            )
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = if (portraitData == null) {
                                    state.t("Upload portrait", "Porträt hochladen")
                                } else {
                                    state.t("View portrait", "Porträt ansehen")
                                }
                                if (supportsInspiration) {
                                    stateDescription = if (inspirationActive) {
                                        state.t("Inspiration active", "Inspiration aktiv")
                                    } else {
                                        state.t("No Inspiration", "Keine Inspiration")
                                    }
                                    customActions = listOf(
                                        CustomAccessibilityAction(
                                            if (inspirationActive) {
                                                state.t("Remove Inspiration", "Inspiration entfernen")
                                            } else {
                                                state.t("Add Inspiration", "Inspiration hinzufügen")
                                            },
                                        ) {
                                            state.toggleInspiration()
                                        },
                                    )
                                }
                            },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.Transparent,
                    ) {
                        CharacterPortrait(character.name, character.portraitSeed, Modifier.fillMaxSize(), portraitData)
                    }
                    if (inspirationActive) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).size(25.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.tertiary,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Stars,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(character.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f, fill = false))
                        if (character.isDead) {
                            Spacer(Modifier.width(7.dp))
                            Icon(
                                Icons.Rounded.Dangerous,
                                contentDescription = state.t("Dead", "Tot"),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("Level ${character.level} · ${character.ancestry} ${character.className}", "Stufe ${character.level} · ${character.ancestry} ${character.className}"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                    if (character.subclass != "—") {
                        Surface(
                            onClick = { state.showInfo(character.subclass, subclassInfo(state, character)) },
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(character.subclass, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(" · ${character.ruleset.shortLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.Info, contentDescription = state.t("Subclass details", "Unterklassen-Details"), Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Text(character.ruleset.shortLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { state.conditionsOpen = true }, modifier = Modifier.size(42.dp)) {
                        val hasHarmfulCondition = state.selectedConditions.any { !it.isInspiration() }
                        Icon(
                            Icons.Rounded.PsychologyAlt,
                            contentDescription = state.t("Conditions (${state.selectedConditions.size})", "Zustände (${state.selectedConditions.size})"),
                            tint = when {
                                hasHarmfulCondition -> MaterialTheme.colorScheme.error
                                inspirationActive -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    if (character.level < 20) {
                        IconButton(onClick = state::beginLevelUp, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = state.t("Level up", "Stufenaufstieg"))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryStat(
                    icon = { Icon(Icons.Rounded.Favorite, contentDescription = null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary) },
                    label = if (character.maxHpReduction > 0) {
                        state.t("HP · −${character.maxHpReduction} max", "TP · −${character.maxHpReduction} Max.")
                    } else {
                        state.t("HP", "TP")
                    },
                    value = "${character.hp}/${character.effectiveMaxHp}${if (character.temporaryHp > 0) " +${character.temporaryHp}" else ""}",
                    modifier = Modifier.weight(1f),
                    onClick = { state.hpAdjustOpen = true },
                )
                SummaryStat(
                    icon = { Icon(Icons.Rounded.Shield, contentDescription = null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary) },
                    label = "AC",
                    value = character.armorClass.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val calculation = CharacterStatEngine.armorClass(character, state.selectedConditions)
                        state.showInfo(state.t("Armor Class", "Rüstungsklasse"), calculation.detailsText(state))
                    },
                )
                SummaryStat(
                    icon = { Icon(if (character.flySpeedFeet != null) Icons.Rounded.Flight else Icons.Rounded.Bolt, contentDescription = null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary) },
                    label = if (character.flySpeedFeet != null) state.t("Walk / Fly", "Gehen / Flug") else state.t("Speed", "Beweg."),
                    value = character.effectiveFlySpeedFeet?.let { "${character.effectiveSpeedFeet} / $it" } ?: "${character.effectiveSpeedFeet} ft",
                    modifier = Modifier.weight(1f),
                    onClick = { state.showInfo(state.t("Speed", "Bewegungsrate"), speedInfo(state, character), speedCostTokens(character)) },
                )
            }
            LinearProgressIndicator(progress = { character.hp.coerceAtMost(character.effectiveMaxHp).toFloat() / character.effectiveMaxHp.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
            if (character.ruleset != Ruleset.Pf2eRemaster) {
                RestQuickActions(
                    state = state,
                    character = character,
                    expanded = restsExpanded,
                    onToggle = onRestsToggle,
                    onLongRest = onLongRest,
                )
            }
        }
    }
}

@Composable
private fun RestQuickActions(
    state: DndAppState,
    character: CharacterUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLongRest: () -> Unit,
) {
    val enabled = state.canTakeRest(character.id)
    val unavailableState = when {
        character.isDead || character.hp == 0 -> state.t("Unavailable at 0 HP", "Bei 0 TP nicht verfügbar")
        !character.hasPlayedSinceLongRest -> state.t("No play changes since the last Long Rest", "Keine Spieländerungen seit der letzten Langen Rast")
        else -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(32.dp)
                .clickable(onClick = onToggle)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    stateDescription = if (expanded) state.t("Expanded", "Ausgeklappt") else state.t("Collapsed", "Eingeklappt")
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.width(8.dp))
            Text(state.t("Rests", "Rasten"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { state.takeRest(Recovery.SHORT_REST) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f).height(52.dp).then(
                        unavailableState?.let { reason -> Modifier.semantics { stateDescription = reason } } ?: Modifier
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(Icons.Rounded.LocalCafe, contentDescription = null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(7.dp))
                    Text(state.t("Short Rest", "Kurze Rast"), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onLongRest,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).height(52.dp).then(
                        unavailableState?.let { reason -> Modifier.semantics { stateDescription = reason } } ?: Modifier
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(Icons.Rounded.NightsStay, contentDescription = null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(7.dp))
                    Text(state.t("Long Rest", "Lange Rast"), maxLines = 1)
                    if (character.ancestry.contains("elf", ignoreCase = true)) {
                        Spacer(Modifier.width(3.dp))
                        IconButton(
                            onClick = {
                                state.showInfo(
                                    state.t("Elven Trance", "Elfische Trance"),
                                    state.t(
                                        "An elf can complete a Long Rest in 4 hours by meditating in a trance, if the character's traits grant Trance.",
                                        "Ein Elf kann eine Lange Rast in 4 Stunden durch Trance abschließen, sofern seine Merkmale Trance gewähren.",
                                    ),
                                )
                            },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = state.t("Elven rest information", "Information zur Elfenrast"), Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LongRestDialog(
    state: DndAppState,
    character: CharacterUi,
    onDismiss: () -> Unit,
) {
    val rations = state.availableRations(character)
    var selectedRationId by remember(character.id, rations.map { it.id to it.quantity }) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.NightsStay, contentDescription = null) },
        title = { Text(state.t("Long Rest", "Lange Rast")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    state.t(
                        "Choose one ration to consume, or rest without one.",
                        "Wähle eine Ration zum Verbrauchen oder raste ohne Ration.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (rations.isEmpty()) {
                    Text(
                        state.t("No rations in equipment.", "Keine Rationen in der Ausrüstung."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    rations.forEach { ration ->
                        val selected = selectedRationId == ration.id
                        Surface(
                            onClick = { selectedRationId = ration.id.takeUnless { selected } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(ration.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                                Text("×${ration.quantity}", style = MaterialTheme.typography.labelMedium)
                                if (selected) {
                                    Spacer(Modifier.width(7.dp))
                                    Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, contentDescription = null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            state.t(
                                "Food and water are daily adventuring needs, not a formal Long Rest prerequisite. Skipping a ration does not add exhaustion automatically; track other food or water separately.",
                                "Nahrung und Wasser sind tägliche Reisebedürfnisse, aber keine formale Voraussetzung für eine Lange Rast. Ohne Ration wird nicht automatisch Erschöpfung hinzugefügt; andere Nahrung oder Wasser werden separat verwaltet.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("Cancel", "Abbrechen")) } },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = {
                        val id = selectedRationId ?: return@Button
                        if (state.takeRest(Recovery.LONG_REST, id)) onDismiss()
                    },
                    enabled = selectedRationId != null,
                ) { Text(state.t("Use ration & rest", "Ration nutzen & rasten")) }
                TextButton(onClick = { if (state.takeRest(Recovery.LONG_REST)) onDismiss() }) {
                    Text(state.t("Rest without ration", "Ohne Ration rasten"))
                }
            }
        },
    )
}

@Composable
private fun SummaryStat(icon: @Composable () -> Unit, label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun TurnTrackerCard(state: DndAppState, character: CharacterUi) {
    val playSession = character.activePlaySession
    val activities = playSession?.activities.orEmpty()
        .filter { it.turnNumber == playSession?.currentTurnNumber }
        .sortedBy { it.sequence }
    val legacyCount = (state.currentTurnRecordedCount(character.id) - activities.size).coerceAtLeast(0)
    var expanded by remember(playSession?.id, playSession?.currentTurnNumber) { mutableStateOf(false) }
    var confirmEmptyTurn by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val containerColor by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .38f) else Color.Transparent,
    )
    val containerPadding by animateDpAsState(if (expanded) 12.dp else 0.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        color = containerColor,
    ) {
        Column(Modifier.fillMaxWidth().padding(containerPadding), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { state.openTurn() }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(state.t("Turn", "Zug"))
                }
                IconButton(onClick = { state.sessionHistoryOpen = true }) {
                    Icon(Icons.Rounded.History, contentDescription = state.t("Session history", "Sitzungsverlauf"))
                }
                IconButton(onClick = { expanded = !expanded }, enabled = playSession != null) {
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) state.t("Collapse turn", "Zug einklappen") else state.t("Expand turn", "Zug ausklappen"),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded && playSession != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        Modifier.fillMaxWidth()
                            .heightIn(max = 144.dp)
                            .cleanVerticalScrollbar(scrollState)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (activities.isEmpty()) {
                            Text(state.t("No activity yet.", "Noch keine Aktivität."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            activities.forEach { activity ->
                                Text(
                                    "• ${activity.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (legacyCount > 0) {
                            Text(
                                state.t("• $legacyCount earlier actions", "• $legacyCount frühere Aktionen"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { state.sessionSaveOpen = true },
                            enabled = playSession?.activities?.isNotEmpty() == true,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text(state.t("Save session", "Sitzung speichern"), maxLines = 1) }
                        Button(
                            onClick = { if (!state.nextTurn()) confirmEmptyTurn = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text(state.t("Next turn", "Nächster Zug"), maxLines = 1) }
                    }
                }
            }
        }
    }
    if (confirmEmptyTurn) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTurn = false },
            title = { Text(state.t("Start an empty next turn?", "Leeren nächsten Zug starten?")) },
            text = { Text(state.t("Nothing has been recorded in this turn.", "In diesem Zug wurde nichts erfasst.")) },
            dismissButton = { TextButton(onClick = { confirmEmptyTurn = false }) { Text(state.t("Cancel", "Abbrechen")) } },
            confirmButton = {
                Button(onClick = { confirmEmptyTurn = false; state.nextTurn(confirmEmpty = true) }) {
                    Text(state.t("Next turn", "Nächster Zug"))
                }
            },
        )
    }
}

@Composable
private fun Modifier.cleanVerticalScrollbar(scrollState: androidx.compose.foundation.ScrollState): Modifier {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .28f)
    return drawWithContent {
        drawContent()
        if (scrollState.maxValue > 0) {
            val trackHeight = size.height
            val contentHeight = trackHeight + scrollState.maxValue
            val thumbHeight = (trackHeight * trackHeight / contentHeight).coerceAtLeast(24.dp.toPx())
            val availableTravel = (trackHeight - thumbHeight).coerceAtLeast(0f)
            val top = scrollState.value.toFloat() / scrollState.maxValue.toFloat() * availableTravel
            val width = 2.dp.toPx()
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - width, top),
                size = Size(width, thumbHeight),
                cornerRadius = CornerRadius(width, width),
            )
        }
    }
}

@Composable
private fun DownedStatusCard(state: DndAppState, character: CharacterUi) {
    val title = when {
        character.isDead -> state.t("Dead", "Tot")
        character.ruleset == Ruleset.Pf2eRemaster && character.dyingValue > 0 -> state.t("Dying ${character.dyingValue}", "Sterbend ${character.dyingValue}")
        character.isStable -> state.t("Stable at 0 HP", "Stabil bei 0 TP")
        else -> state.t("Down at 0 HP", "Bei 0 TP am Boden")
    }
    val bodyColor = if (character.isDead) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bodyColor),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (character.isDead) Icons.Rounded.Dangerous else Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = if (character.isDead) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(9.dp))
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            if (character.ruleset == Ruleset.Pf2eRemaster) {
                Pf2HealthTrack(state, character)
            } else if (!character.isDead && !character.isStable) {
                DeathSaveTrack(
                    state = state,
                    successes = character.deathSaveSuccesses,
                    failures = character.deathSaveFailures,
                )
            }
            when {
                character.isDead -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.t("Raise HP above 0 to revive this character.", "Erhöhe die TP über 0, um diesen Charakter wiederzubeleben."), style = MaterialTheme.typography.bodyMedium)
                    character.deathReason?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error) }
                }
                character.ruleset == Ruleset.Pf2eRemaster && character.dyingValue > 0 -> Button(
                    onClick = state::resolveDeathSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Icon(Icons.Rounded.Casino, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(state.t("Recovery Check", "Erholungswurf"))
                }
                character.ruleset == Ruleset.Pf2eRemaster && character.isStable -> Text(state.t("Dying is 0; no Recovery Check is needed.", "Sterbend ist 0; kein Erholungswurf ist nötig."), style = MaterialTheme.typography.bodyMedium)
                character.isStable -> Text(state.t("No more Death Saves are needed unless the character takes damage.", "Keine weiteren Todesrettungswürfe sind nötig, solange der Charakter keinen Schaden erleidet."), style = MaterialTheme.typography.bodyMedium)
                else -> Button(
                    onClick = state::resolveDeathSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Icon(Icons.Rounded.Casino, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(state.t("Roll Death Save", "Todesrettungswurf"))
                }
            }
        }
    }
}

@Composable
private fun Pf2HealthTrack(state: DndAppState, character: CharacterUi) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ValuedConditionMark(state.t("Dying", "Sterbend"), character.dyingValue, 4, Modifier.weight(1f))
        ValuedConditionMark(state.t("Wounded", "Verwundet"), character.woundedValue, 3, Modifier.weight(1f))
        ValuedConditionMark(state.t("Doomed", "Dem Tode geweiht"), character.doomedValue, 3, Modifier.weight(1f))
    }
}

@Composable
private fun ValuedConditionMark(label: String, value: Int, maximum: Int, modifier: Modifier) {
    Column(
        modifier.semantics(mergeDescendants = true) { stateDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(maximum) { index ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (index < value) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (index < value) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.outline),
                ) { Box(Modifier.size(15.dp)) }
            }
        }
        Text(value.toString(), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun DeathSaveTrack(state: DndAppState, successes: Int, failures: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DeathSaveMarks(state.t("Successes", "Erfolge"), successes, false, Modifier.weight(1f))
        DeathSaveMarks(state.t("Failures", "Fehlschläge"), failures, true, Modifier.weight(1f))
    }
}

@Composable
private fun DeathSaveMarks(label: String, count: Int, failed: Boolean, modifier: Modifier) {
    Column(
        modifier.semantics(mergeDescendants = true) { stateDescription = "$label: ${count.coerceIn(0, 3)} of 3" },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("$label ${count.coerceIn(0, 3)}/3", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                val active = index < count
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (active) {
                        if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (active) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.outline),
                ) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        if (active) Icon(
                            if (failed) Icons.Rounded.Close else Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = if (failed) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionRow(state: DndAppState, condition: ConditionUi) {
    val inspiration = condition.isInspiration()
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                state.removeCondition(condition)
                true
            } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = condition.removable,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Box(Modifier.fillMaxSize().padding(end = 18.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Rounded.Delete, contentDescription = state.t("Remove condition", "Zustand entfernen"), tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) {
        Card(
            modifier = Modifier.semantics {
                if (condition.removable) {
                    customActions = listOf(CustomAccessibilityAction(state.t("Remove ${condition.name}", "${condition.name} entfernen")) {
                        state.removeCondition(condition)
                        true
                    })
                }
            },
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (inspiration) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                },
            ),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 5.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(condition.name, style = MaterialTheme.typography.titleSmall)
                    Text("${condition.source} · ${condition.duration}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { state.showInfo(condition.name, condition.explanation) }) { Icon(Icons.Rounded.Info, contentDescription = state.t("Explain", "Erklären")) }
            }
        }
    }
}

@Composable
private fun CollapsibleSectionHeader(
    state: DndAppState,
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Surface(
            onClick = onToggle,
            enabled = enabled,
            modifier = Modifier.weight(1f).semantics {
                role = Role.Button
                stateDescription = if (expanded) state.t("Expanded", "Ausgeklappt") else state.t("Collapsed", "Eingeklappt")
            },
            color = Color.Transparent,
            shape = RoundedCornerShape(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
                )
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) state.t("Collapse $title", "$title einklappen") else state.t("Expand $title", "$title ausklappen"),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f),
                )
            }
        }
        action?.invoke()
    }
}

@Composable
private fun RollShortcuts(state: DndAppState, character: CharacterUi, expanded: Boolean, onToggle: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CollapsibleSectionHeader(state, state.t("Quick rolls", "Schnelle Würfe"), expanded, onToggle) {
            IconButton(onClick = { state.quickRollEditorOpen = true }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = state.t("Customize quick rolls", "Schnelle Würfe anpassen"), Modifier.size(19.dp))
            }
        }
        if (expanded) {
            val rolls = character.resolvedQuickRolls.take(12)
            val columns = when (rolls.size) {
                0, 1 -> 1
                2 -> 2
                3 -> 3
                else -> 2
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rolls.chunked(columns).forEach { rowRolls ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowRolls.forEach { quickRoll ->
                            QuickRollCard(state, character, quickRoll, Modifier.weight(1f))
                        }
                        if (rowRolls.size < columns && rolls.size <= 3) {
                            repeat(columns - rowRolls.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickRollCard(state: DndAppState, character: CharacterUi, quickRoll: QuickRollUi, modifier: Modifier) {
    val rollModifier = quickRollModifier(character, quickRoll)
    val icon = when (quickRoll.kind) {
        QuickRollKind.ABILITY -> Icons.Rounded.Stars
        QuickRollKind.SAVE -> Icons.Rounded.Shield
        QuickRollKind.SKILL -> Icons.Rounded.Search
        QuickRollKind.INITIATIVE -> Icons.Rounded.Bolt
        QuickRollKind.DEATH_SAVE -> Icons.Rounded.Favorite
        QuickRollKind.ATTACK -> Icons.Rounded.SportsMma
    }
    Surface(
        onClick = { state.executeQuickRoll(quickRoll) },
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(19.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.align(Alignment.TopStart)) {
                Text(quickRoll.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                rollModifier?.let { Text(signed(it), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
            }
            Icon(icon, contentDescription = null, Modifier.align(Alignment.BottomEnd).size(23.dp), tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AbilityScores(state: DndAppState, character: CharacterUi, expanded: Boolean, onToggle: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CollapsibleSectionHeader(state, state.t("Abilities", "Attribute"), expanded, onToggle)
        if (expanded) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(character.abilities.toList()) { (name, score) ->
                    val modifier = kotlin.math.floor((score - 10) / 2.0).toInt()
                    Surface(
                        modifier = Modifier.combinedClickable(
                            onClick = { state.roll(name, modifier, modifierLabel = name) },
                            onLongClick = { state.beginEdit(name) },
                            onLongClickLabel = state.t("Edit $name", "$name bearbeiten"),
                        ),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(Modifier.padding(horizontal = 17.dp, vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(signed(modifier), style = MaterialTheme.typography.titleLarge)
                            Text(score.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.cleanHorizontalScrollbar(listState: androidx.compose.foundation.lazy.LazyListState): Modifier {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .28f)
    return drawWithContent {
        drawContent()
        val layout = listState.layoutInfo
        val visible = layout.visibleItemsInfo
        val total = layout.totalItemsCount
        if (visible.isNotEmpty() && total > visible.size) {
            val thumbWidth = (size.width * visible.size / total.toFloat()).coerceAtLeast(28.dp.toPx())
            val maxIndex = (total - visible.size).coerceAtLeast(1)
            val progress = visible.first().index.coerceAtMost(maxIndex) / maxIndex.toFloat()
            val left = (size.width - thumbWidth) * progress
            drawRoundRect(
                color = color,
                topLeft = Offset(left, size.height - 3.dp.toPx()),
                size = Size(thumbWidth, 2.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun RollGrid(
    state: DndAppState,
    title: String,
    entries: List<Pair<String, Int>>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onInfo: ((String) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CollapsibleSectionHeader(state, title, expanded, onToggle)
        if (expanded) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    entries.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            pair.forEach { (name, modifier) ->
                                Surface(
                                    onClick = { state.roll(name, modifier, modifierLabel = name) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(13.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(signed(modifier), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                        if (onInfo != null) {
                                            IconButton(onClick = { onInfo(name) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Rounded.Info, contentDescription = state.t("Calculation details", "Berechnungsdetails"), modifier = Modifier.size(17.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(state: DndAppState, feature: FeatureUi, onEdit: () -> Unit) {
    val feedback = state.inlineFeatureFeedback?.takeIf { it.featureId == feature.id }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 5.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Stars, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(feature.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    if (feature.maximum != null && feature.resourceId == null) {
                        IconButton(onClick = { state.resetFeature(feature.id) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = state.t("Reset ${feature.name}", "${feature.name} zurücksetzen"), Modifier.size(17.dp))
                        }
                    }
                }
                if (feature.maximum != null && feature.resourceId == null) {
                    Text("${feature.remaining}/${feature.maximum} · ${recoveryLabel(state, feature.recovery)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (feature.toCostTokens().isNotEmpty()) {
                    CostChipRow(state, feature.toCostTokens(), available = (feature.remaining ?: 1) > 0)
                }
            }
            if (feature.isActivatable()) {
                FilledTonalButton(
                    onClick = { state.useFeature(feature.id, null) },
                    enabled = feature.remaining == null || feature.remaining >= feature.resourceCost,
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                ) {
                    Crossfade(targetState = feedback, label = "feature-feedback-${feature.id}") { activeFeedback ->
                        Text(
                            activeFeedback?.message
                                ?: if (feature.remaining == null || feature.remaining >= feature.resourceCost) state.t("Use", "Nutzen") else state.t("Used", "Leer"),
                        )
                    }
                }
            }
            if (feature.custom) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = state.t("Edit ${feature.name}", "${feature.name} bearbeiten"), Modifier.size(20.dp))
                }
            }
            IconButton(onClick = { state.showInfo(feature.name, feature.summary, feature.toCostTokens()) }) {
                Icon(Icons.Rounded.Info, contentDescription = state.t("Details", "Details"), Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SharedResourceStat(state: DndAppState, feature: FeatureUi) {
    val die = feature.resourceDieSides?.let { "d$it" }
        ?: Regex("d\\d+", RegexOption.IGNORE_CASE).find(feature.summary)?.value?.lowercase()
    Surface(
        onClick = { state.showInfo(feature.name, feature.summary) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (die != null) Icons.Rounded.Casino else Icons.Rounded.AutoAwesome, contentDescription = null, Modifier.size(17.dp))
            Text(listOfNotNull(die, "${feature.remaining}/${feature.maximum}").joinToString(" "), style = MaterialTheme.typography.labelLarge)
        }
    }
}

internal enum class FeatureFamily {
    Maneuvers,
    Metamagic,
    Invocations,
    ChannelDivinity,
    WildShape,
    Focus,
    WeaponMasteries,
    General,
}

internal fun featureFamily(feature: FeatureUi): FeatureFamily {
    if (feature.custom) return FeatureFamily.General
    val hint = "${feature.id} ${feature.name} ${feature.summary}".lowercase()
    return when {
        "maneuver" in hint || "superiority" in hint -> FeatureFamily.Maneuvers
        "metamagic" in hint || "sorcery point" in hint -> FeatureFamily.Metamagic
        "invocation" in hint -> FeatureFamily.Invocations
        "channel divinity" in hint || "channel-divinity" in hint -> FeatureFamily.ChannelDivinity
        "wild shape" in hint || "wild-shape" in hint -> FeatureFamily.WildShape
        "focus" in hint -> FeatureFamily.Focus
        "weapon mastery" in hint || "weapon-mastery" in hint || hint.startsWith("mastery ") -> FeatureFamily.WeaponMasteries
        else -> FeatureFamily.General
    }
}

internal fun featureFamilyLabel(state: DndAppState, family: FeatureFamily): String = when (family) {
    FeatureFamily.Maneuvers -> state.t("Maneuvers", "Manöver")
    FeatureFamily.Metamagic -> state.t("Metamagic", "Metamagie")
    FeatureFamily.Invocations -> state.t("Invocations", "Anrufungen")
    FeatureFamily.ChannelDivinity -> state.t("Channel Divinity", "Göttliche Macht kanalisieren")
    FeatureFamily.WildShape -> state.t("Wild Shape", "Tiergestalt")
    FeatureFamily.Focus -> state.t("Focus", "Fokus")
    FeatureFamily.WeaponMasteries -> state.t("Weapon Masteries", "Waffenmeisterschaften")
    FeatureFamily.General -> state.t("Features & resources", "Merkmale & Ressourcen")
}

@Composable
private fun SpellRow(state: DndAppState, spell: SpellUi, onQuickCast: () -> Unit) {
    val canQuickCast = spell.level == 0 || state.availableSpellSlotLevels(spell).isNotEmpty()
    val character = state.selectedCharacter
    val combatSummary = character?.let { spellCombatSummary(state, it, spell) }.orEmpty()
    Card(
        onClick = { state.showInfo(spell.name, spellDetails(state, spell), spell.activationCost.toCostTokens()) },
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(spell.name, style = MaterialTheme.typography.titleSmall)
                    CostChipRow(state, spell.activationCost.toCostTokens())
                }
                val level = if (spell.level == 0) state.t("Cantrip", "Zaubertrick") else state.t("Level ${spell.level}", "Grad ${spell.level}")
                val source = spell.sourceName.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                Text("$level$source", style = MaterialTheme.typography.labelMedium, color = if (spell.sourceKind == SpellSourceKind.ITEM) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(spell.summary, style = MaterialTheme.typography.bodySmall)
                if (combatSummary.isNotBlank()) {
                    Text(combatSummary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (spell.castPreviews.isNotEmpty()) {
                    val levels = state.availableSpellSlotLevels(spell).ifEmpty { spell.castPreviews.keys.sorted() }
                    Text(
                        state.t("Cast levels ${levels.first()}–${levels.last()}", "Wirkgrade ${levels.first()}–${levels.last()}"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onQuickCast,
                enabled = canQuickCast,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            ) { Text(state.t("Quick Cast", "Schnell wirken"), maxLines = 1) }
        }
    }
}

private fun spellCombatSummary(state: DndAppState, character: CharacterUi, spell: SpellUi): String = buildList {
    if (spell.spellAttack) {
        val bonus = CharacterStatEngine.spellAttackBonus(character, spell)
        add(state.t("Spell attack ${signed(bonus)}", "Zauberangriff ${signed(bonus)}"))
    }
    spell.savingThrows.forEach { prompt ->
        val dc = CharacterStatEngine.difficultyClass(character, prompt.difficultyClass, spell)
        add(state.t("${prompt.ability.shortName()} save DC $dc", "${prompt.ability.shortName()}-Rettung SG $dc"))
    }
}.joinToString(" · ")

private fun spellDetails(state: DndAppState, spell: SpellUi): String = buildString {
    append(spell.summary)
    state.selectedCharacter?.let { character ->
        val combat = spellCombatSummary(state, character, spell)
        if (combat.isNotBlank()) append("\n$combat")
    }
    if (spell.sourceName.isNotBlank()) append("\n${state.t("Source", "Quelle")}: ${spell.sourceName}")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpellSlotCounter(state: DndAppState, character: CharacterUi, onRegain: () -> Unit) {
    var expanded by remember(character.id) { mutableStateOf(true) }
    var editingMaximums by remember(character.id) { mutableStateOf(false) }
    var showTimingHint by remember(character.id) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val visibleSlots = if (editingMaximums) {
        (1..9).map { level ->
            character.resolvedSpellSlots.firstOrNull { it.level == level }
                ?: SpellSlotUi(level, 0, character.spellSlotMaximumOverrides[level] ?: state.rulesSpellSlotMaximum(level, character))
        }
    } else {
        character.resolvedSpellSlots
    }
    Surface(
        modifier = Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = {
                    editingMaximums = !editingMaximums
                    if (editingMaximums && state.shouldWarnAboutSpellSlotEdit(character)) showTimingHint = true
                },
            )
            .semantics {
                stateDescription = if (expanded) state.t("Expanded", "Ausgeklappt") else state.t("Collapsed", "Eingeklappt")
                customActions = listOf(
                    CustomAccessibilityAction(state.t("Edit spell slot maximums", "Maximale Zauberplätze bearbeiten")) {
                        editingMaximums = !editingMaximums
                        if (editingMaximums && state.shouldWarnAboutSpellSlotEdit(character)) showTimingHint = true
                        true
                    },
                )
            },
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.t("Spell slots", "Zauberplätze"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (editingMaximums) {
                    Text(state.t("Edit maximums", "Maxima bearbeiten"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                }
                if (character.isSorcerer) {
                    TextButton(onClick = onRegain, enabled = expanded) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(state.t("Regain", "Herstellen"))
                    }
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().cleanHorizontalScrollbar(listState).padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleSlots, key = { it.level }) { slot ->
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(
                                Modifier.widthIn(min = 104.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(state.t("Level ${slot.level}", "Grad ${slot.level}"), style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    repeat(slot.maximum) { index ->
                                        Surface(
                                            Modifier.width(3.dp).height(17.dp),
                                            shape = RoundedCornerShape(99.dp),
                                            color = if (index < slot.remaining) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        ) {}
                                    }
                                }
                                Text("${slot.remaining}/${slot.maximum}", style = MaterialTheme.typography.labelMedium)
                                if (editingMaximums) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { state.updateSpellSlotMaximum(slot.level, (slot.maximum - 1).coerceAtLeast(0)) },
                                            enabled = slot.maximum > 0,
                                            modifier = Modifier.size(34.dp),
                                        ) { Icon(Icons.Rounded.ArrowDownward, contentDescription = state.t("Decrease maximum", "Maximum verringern"), Modifier.size(17.dp)) }
                                        Text(slot.maximum.toString(), style = MaterialTheme.typography.titleSmall)
                                        IconButton(
                                            onClick = { state.updateSpellSlotMaximum(slot.level, (slot.maximum + 1).coerceAtMost(10)) },
                                            enabled = slot.maximum < 10,
                                            modifier = Modifier.size(34.dp),
                                        ) { Icon(Icons.Rounded.ArrowUpward, contentDescription = state.t("Increase maximum", "Maximum erhöhen"), Modifier.size(17.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showTimingHint) {
        AlertDialog(
            onDismissRequest = { showTimingHint = false },
            icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
            title = { Text(state.t("Spell slot maximums", "Maximale Zauberplätze")) },
            text = {
                Text(
                    state.t(
                        "These values usually change when you level up. You can still edit them for table rulings, imports, or corrections.",
                        "Diese Werte ändern sich normalerweise beim Stufenaufstieg. Für Hausregeln, Importe oder Korrekturen kannst du sie trotzdem bearbeiten.",
                    ),
                )
            },
            confirmButton = { TextButton(onClick = { showTimingHint = false }) { Text(state.t("Continue", "Weiter")) } },
        )
    }
}

@Composable
private fun QuickCastSlotDialog(
    state: DndAppState,
    spell: SpellUi,
    onDismiss: () -> Unit,
    onCast: (Int) -> Unit,
) {
    val levels = state.availableSpellSlotLevels(spell)
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val levelsWithPreview = levels
            Text(
                if (levelsWithPreview.isEmpty()) spell.name
                else state.t("${spell.name} · Levels ${levelsWithPreview.first()}–${levelsWithPreview.last()}", "${spell.name} · Grade ${levelsWithPreview.first()}–${levelsWithPreview.last()}"),
            )
        },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp)
                    .cleanVerticalScrollbar(scrollState)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(state.t("Choose a spell slot", "Zauberplatz wählen"))
                levels.forEach { level ->
                    FilledTonalButton(onClick = { onCast(level) }, modifier = Modifier.fillMaxWidth()) {
                        val preview = spell.castPreviews[level]
                        Text(
                            if (preview == null) state.t("Level $level slot", "Zauberplatz Grad $level")
                            else state.t("Level $level · $preview", "Grad $level · $preview"),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("Cancel", "Abbrechen")) } },
    )
}

@Composable
private fun SorcerySlotRecoveryDialog(state: DndAppState, character: CharacterUi, onDismiss: () -> Unit) {
    val points = character.features.firstOrNull { it.id == "sorcery-points" }?.remaining ?: 0
    val options = state.sorcerySpellSlotOptions(character)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.t("Regain a spell slot", "Zauberplatz wiederherstellen")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.t("$points Sorcery Points available", "$points Zaubereipunkte verfügbar"), style = MaterialTheme.typography.bodyMedium)
                if (options.isEmpty()) {
                    Text(state.t("All eligible spell slots are full.", "Alle verfügbaren Zauberplätze sind voll."))
                } else {
                    options.forEach { (slot, cost) ->
                        OutlinedButton(
                            onClick = { if (state.regainSpellSlotWithSorceryPoints(slot.level)) onDismiss() },
                            enabled = points >= cost,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(state.t("Level ${slot.level} · $cost points", "Grad ${slot.level} · $cost Punkte"))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(state.t("Close", "Schließen")) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun WeaponRow(state: DndAppState, character: CharacterUi, weapon: WeaponUi) {
    val derivedOptions = state.derivedAttackOptions(character, weapon)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> state.editWeapon(weapon)
                SwipeToDismissBoxValue.EndToStart -> state.removeWeapon(weapon.id)
                SwipeToDismissBoxValue.Settled -> Unit
            }
            value == SwipeToDismissBoxValue.EndToStart
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { InventorySwipeBackground(state, dismissState.targetValue) },
    ) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = { state.openSheetAttack(weapon.id) },
                onLongClick = { state.toggleWeaponEquipped(weapon.id) },
            )
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(state.t("Edit weapon", "Waffe bearbeiten")) { state.editWeapon(weapon); true },
                    CustomAccessibilityAction(state.t("Remove weapon", "Waffe entfernen")) { state.removeWeapon(weapon.id); true },
                    CustomAccessibilityAction(state.t("Toggle equipped", "Ausrüstung umschalten")) { state.toggleWeaponEquipped(weapon.id); true },
                )
            }
            .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(11.dp)) {
                Icon(Icons.Rounded.SportsMma, contentDescription = null, modifier = Modifier.padding(9.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(weapon.name, style = MaterialTheme.typography.titleSmall)
                Text("${signed(weapon.attackBonus)} · ${weapon.damage} ${weapon.damageType}", style = MaterialTheme.typography.bodyMedium)
                val detail = listOf("${weapon.reachFeet} ft", weapon.range, weapon.mastery, weapon.properties).filter { it.isNotBlank() }.joinToString(" · ")
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (weapon.equipped) Text(state.t("Equipped", "Ausgerüstet"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                if (weapon.needsAttunement) {
                    TextButton(onClick = { state.toggleWeaponAttunement(weapon.id) }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (weapon.attuned) state.t("Attuned", "Eingestimmt") else state.t("Needs attunement", "Einstimmung nötig"), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            IconButton(onClick = { state.showInfo(weapon.name, weaponDetails(state, weapon)) }) {
                Icon(Icons.Rounded.Info, contentDescription = state.t("Weapon details", "Waffendetails"))
            }
            CostChip(state, CostTokenUi(CostTokenKind.Attack))
        }
        derivedOptions.forEach { option ->
            HorizontalDivider(modifier = Modifier.padding(start = 54.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                onClick = { state.openSheetAttack(weapon.id, option.id) },
                modifier = Modifier.fillMaxWidth().padding(start = 38.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .28f),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(Modifier.width(2.dp).height(34.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                    Column(Modifier.weight(1f)) {
                        Text(
                            option.weapon.name + if (option.attackCount > 1) " ×${option.attackCount}" else "",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${signed(option.weapon.attackBonus)} · ${option.weapon.damage} ${option.weapon.damageType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val tokens = option.cost.toCostTokens().ifEmpty { listOf(CostTokenUi(CostTokenKind.Attack)) }
                    CostChipRow(state, tokens)
                }
            }
        }
        }
    }
    }
}

@Composable
private fun InventorySwipeBackground(state: DndAppState, target: SwipeToDismissBoxValue) {
    val editing = target == SwipeToDismissBoxValue.StartToEnd
    val removing = target == SwipeToDismissBoxValue.EndToStart
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = when {
            editing -> MaterialTheme.colorScheme.primaryContainer
            removing -> MaterialTheme.colorScheme.errorContainer
            else -> Color.Transparent
        },
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (editing) Arrangement.Start else Arrangement.End,
        ) {
            if (editing || removing) {
                Icon(
                    if (editing) Icons.Rounded.Edit else Icons.Rounded.Delete,
                    contentDescription = if (editing) state.t("Edit", "Bearbeiten") else state.t("Remove", "Entfernen"),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun EquipmentCard(state: DndAppState, character: CharacterUi) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth()) {
            character.resolvedEquipment.forEachIndexed { index, item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> state.editEquipment(item)
                            SwipeToDismissBoxValue.EndToStart -> state.removeEquipment(item.id)
                            SwipeToDismissBoxValue.Settled -> Unit
                        }
                        value == SwipeToDismissBoxValue.EndToStart
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { InventorySwipeBackground(state, dismissState.targetValue) },
                ) {
                Row(
                    Modifier.fillMaxWidth()
                        .combinedClickable(
                            onClick = { state.showInfo(item.name, equipmentDetails(state, character, item)) },
                            onLongClick = { state.toggleEquipmentEquipped(item.id) },
                        )
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(state.t("Edit item", "Gegenstand bearbeiten")) { state.editEquipment(item); true },
                                CustomAccessibilityAction(state.t("Remove item", "Gegenstand entfernen")) { state.removeEquipment(item.id); true },
                                CustomAccessibilityAction(state.t("Toggle equipped", "Ausrüstung umschalten")) { state.toggleEquipmentEquipped(item.id); true },
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.kind != EquipmentKind.ARMOR || item.worn) {
                        Icon(
                            if (item.kind == EquipmentKind.ARMOR) Icons.Rounded.Shield else Icons.Rounded.Backpack,
                            contentDescription = if (item.kind == EquipmentKind.ARMOR) state.t("Worn", "Getragen") else null,
                            tint = if (item.kind == EquipmentKind.ARMOR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name, style = MaterialTheme.typography.bodyLarge)
                        if (item.kind == EquipmentKind.ARMOR) {
                            Text(
                                buildString {
                                    append(state.t("Armor", "Rüstung"))
                                    item.armorClass?.let { append(" · AC $it") }
                                    if (item.shieldBonus != 0) append(" · ${signed(item.shieldBonus)} AC")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (item.details.isNotBlank()) Text(item.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (item.kind == EquipmentKind.ARMOR) {
                            TextButton(onClick = {
                                if (!item.worn && item.needsAttunement && !item.attuned) {
                                    state.toggleEquipmentAttunement(item.id)
                                    val attuned = state.selectedCharacter?.resolvedEquipment?.firstOrNull { it.id == item.id }?.attuned == true
                                    if (attuned) state.toggleEquipmentWorn(item.id)
                                } else state.toggleEquipmentWorn(item.id)
                            }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                Text(
                                    when {
                                        item.worn -> state.t("Take off", "Ablegen")
                                        item.needsAttunement && !item.attuned -> state.t("Attune & wear", "Einstimmen & tragen")
                                        else -> state.t("Wear", "Anlegen")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        if (item.needsAttunement) {
                            TextButton(onClick = { state.toggleEquipmentAttunement(item.id) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                Text(if (item.attuned) state.t("Attuned", "Eingestimmt") else state.t("Attune", "Einstimmen"), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Icon(Icons.Rounded.Info, contentDescription = state.t("Item details", "Gegenstandsdetails"), modifier = Modifier.size(20.dp))
                }
                }
                if (index < character.resolvedEquipment.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 14.dp))
            }
            if (character.resolvedEquipment.isEmpty()) {
                Text(state.t("No equipment", "Keine Ausrüstung"), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyNotesCard(state: DndAppState) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f))) {
        Text(
            state.t("No notes yet.", "Noch keine Notizen."),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(state: DndAppState, note: CharacterNote, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Box(Modifier.fillMaxSize().padding(end = 18.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Rounded.Delete, contentDescription = state.t("Delete note", "Notiz löschen"), tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) {
        Card(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().semantics {
                customActions = listOf(CustomAccessibilityAction(state.t("Delete ${note.title}", "${note.title} löschen")) {
                    onDelete()
                    true
                })
            },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(note.title, style = MaterialTheme.typography.titleSmall)
                if (note.body.isNotBlank()) {
                    Text(note.body, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun AttackCard(
    state: DndAppState,
    character: CharacterUi,
    weapon: WeaponUi,
    attackOption: ResolvedAttackOptionUi? = null,
    roll: AttackRollUi?,
    damage: DamageRollUi?,
    outcome: AttackOutcome,
    canRoll: Boolean,
    onRoll: () -> Unit,
    onDamage: () -> Unit,
    onOutcome: (AttackOutcome) -> Unit,
    onDismiss: () -> Unit,
) {
    var detailsExpanded by remember(weapon.id) { mutableStateOf(false) }
    var rollAnimationCycle by remember(weapon.id) { mutableStateOf(0) }
    var rollSettled by remember(weapon.id) { mutableStateOf(false) }
    var damageRollCycle by remember(weapon.id) { mutableStateOf(0) }
    val calculation = roll?.calculation ?: state.attackCalculation(character, weapon)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SportsMma, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            weapon.name + if ((attackOption?.attackCount ?: 1) > 1) " ×${attackOption?.attackCount}" else "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val tokens = attackOption?.cost?.toCostTokens().orEmpty().ifEmpty { listOf(CostTokenUi(CostTokenKind.Attack)) }
                        CostChipRow(state, tokens, available = canRoll)
                    }
                    Text(
                        "${weapon.damage} ${weapon.damageType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (attackOption != null && attackOption.timingHint.isNotBlank()) {
                        Text(
                            attackOption.timingHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                TextButton(
                    onClick = {
                        rollSettled = true
                        damageRollCycle++
                        onDamage()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = state.t("Roll damage", "Schaden würfeln")
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) { Text(state.t("Damage", "Schaden"), maxLines = 1) }
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = state.t("Close attack", "Angriff schließen")) }
            }

            Surface(
                onClick = { detailsExpanded = !detailsExpanded },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.semantics {
                    role = Role.Button
                    stateDescription = if (detailsExpanded) state.t("Expanded", "Ausgeklappt") else state.t("Collapsed", "Eingeklappt")
                },
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(state.t("Attack bonus", "Angriffsbonus"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        Text(signed(calculation.total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(7.dp))
                        Icon(
                            if (detailsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (detailsExpanded) {
                        if (attackOption != null) {
                            CalculationRow(state.t("Source", "Quelle"), attackOption.sourceName)
                            if (attackOption.details.isNotBlank()) {
                                Text(attackOption.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                        CalculationRow("d20", "1–20")
                        CalculationRow(calculation.abilityLabel, signed(calculation.ability))
                        CalculationRow(state.t("Proficiency", "Übung"), signed(calculation.proficiency))
                        CalculationRow(state.t("Item / other", "Gegenstand / sonst."), signed(calculation.item))
                        if (calculation.multipleAttackPenalty != 0) {
                            CalculationRow(state.t("Multiple attack penalty", "Mehrfachangriffsabzug"), signed(calculation.multipleAttackPenalty))
                        }
                        CalculationRow(state.t("Attack total", "Angriffsbonus"), signed(calculation.total), strong = true)
                    }
                }
            }

            if (roll != null) {
                AnimatedDiceRow(
                    sides = 20,
                    values = roll.dice,
                    kept = roll.kept,
                    animationKey = "${weapon.id}-$rollAnimationCycle",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onAnimationFinished = { rollSettled = true },
                )
                if (rollSettled) {
                    Text("${roll.total}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }

            if (roll != null && state.canUseInspirationForSheetAttack) {
                InspirationRerollHint(state, onClick = { state.rerollSheetAttackWithInspiration() })
            }

            AnimatedVisibility(
                visible = rollSettled && roll?.natural == 20 && outcome == AttackOutcome.Critical,
                enter = fadeIn() + scaleIn(initialScale = .92f),
                exit = fadeOut() + scaleOut(targetScale = .96f),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(state.t("Natural 20 · Critical hit", "Natürliche 20 · Kritischer Treffer"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(state.t("Damage dice rolled twice automatically.", "Schadenswürfel wurden automatisch zweimal gewürfelt."), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    rollSettled = false
                    rollAnimationCycle++
                    onRoll()
                },
                enabled = canRoll,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(13.dp),
            ) {
                Text(
                    if (roll == null) state.t("Roll attack", "Angriff würfeln")
                    else state.t("Roll again", "Erneut würfeln"),
                )
            }

            if (roll != null && rollSettled) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(AttackOutcome.Miss to state.t("Miss", "Verfehlt"), AttackOutcome.Hit to state.t("Hit", "Treffer"), AttackOutcome.Critical to state.t("Critical", "Kritisch")).forEach { (value, label) ->
                        val selected = outcome == value
                        if (selected) {
                            FilledTonalButton(onClick = { onOutcome(value) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 5.dp)) { Text(label, maxLines = 1) }
                        } else {
                            OutlinedButton(onClick = { onOutcome(value) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 5.dp)) { Text(label, maxLines = 1) }
                        }
                    }
                }
            }

            damage?.takeIf { rollSettled }?.let {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                            Text(if (it.critical) state.t("Critical damage", "Kritischer Schaden") else state.t("Damage", "Schaden"), style = MaterialTheme.typography.labelLarge)
                            Text("${it.dice.joinToString(" + ")}${if (it.modifier == 0) "" else " ${signed(it.modifier)}"} · ${it.damageType}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(it.total.toString(), style = MaterialTheme.typography.headlineMedium)
                        }
                        if (it.dice.isNotEmpty() && it.sides > 0) {
                            AnimatedDiceRow(it.sides, it.dice, animationKey = "damage-${weapon.id}-$damageRollCycle-${it.dice}-${it.total}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculationRow(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = if (strong) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodySmall, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
        Text(value, style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun quickRollModifier(character: CharacterUi, quickRoll: QuickRollUi): Int? = when (quickRoll.kind) {
    QuickRollKind.INITIATIVE -> character.initiative
    QuickRollKind.DEATH_SAVE -> null
    QuickRollKind.ABILITY -> kotlin.math.floor(((character.abilities[quickRoll.id] ?: 10) - 10) / 2.0).toInt()
    QuickRollKind.SAVE -> character.saves[quickRoll.id]
    QuickRollKind.SKILL -> character.skills[quickRoll.id]
    QuickRollKind.ATTACK -> character.weapons.firstOrNull { it.id == quickRoll.id }?.attackBonus
}

private fun saveAbbreviation(name: String): String = when {
    name.startsWith("Str", true) -> "STR"
    name.startsWith("Dex", true) -> "DEX"
    name.startsWith("Con", true) -> "CON"
    name.startsWith("Int", true) -> "INT"
    name.startsWith("Wis", true) -> "WIS"
    name.startsWith("Cha", true) -> "CHA"
    else -> name.take(3).uppercase()
}

private fun recoveryLabel(state: DndAppState, recovery: Recovery): String = when (recovery) {
    Recovery.TURN_START -> state.t("per turn", "pro Zug")
    Recovery.SHORT_REST -> state.t("short rest", "kurze Rast")
    Recovery.LONG_REST -> state.t("long rest", "lange Rast")
    Recovery.DAILY_PREPARATION -> state.t("daily", "täglich")
    Recovery.MANUAL -> state.t("manual", "manuell")
}

private fun subclassInfo(state: DndAppState, character: CharacterUi): String = when (character.subclass) {
    "Battle Master" -> state.t(
        "A tactical Fighter who spends Superiority Dice on maneuvers that add damage, movement, control, or protection. Available options and uses are listed under Maneuvers.",
        "Ein taktischer Kämpfer, der Überlegenheitswürfel für Manöver mit zusätzlichem Schaden, Bewegung, Kontrolle oder Schutz nutzt. Verfügbare Optionen und Einsätze stehen unter Manöver.",
    )
    else -> state.t(
        "${character.subclass} is this character's ${character.className} specialization. Its active features and resources are listed below.",
        "${character.subclass} ist die Spezialisierung dieses ${character.className}-Charakters. Aktive Merkmale und Ressourcen sind unten aufgeführt.",
    )
}

private fun speedInfo(state: DndAppState, character: CharacterUi): String {
    val flightFeatures = character.features.filter { feature ->
        val hint = "${feature.id} ${feature.name} ${feature.summary}".lowercase()
        "fly" in hint || "flight" in hint || "wing" in hint
    }
    return buildString {
        append(state.t("Walk ${character.effectiveSpeedFeet} ft", "Gehen ${character.effectiveSpeedFeet} ft"))
        character.effectiveFlySpeedFeet?.let { append(state.t("\nFly $it ft", "\nFlug $it ft")) }
        if (character.exhaustionLevel > 0) {
            append(state.t("\nExhaustion ${character.exhaustionLevel} is already applied.", "Erschöpfung ${character.exhaustionLevel} ist bereits eingerechnet."))
        }
        if (character.effectiveFlySpeedFeet != null) {
            append(state.t("\nChanging to Fly costs an Action.", "\nDer Wechsel zum Flug kostet eine Aktion."))
        }
        flightFeatures.filter { it.actionCost.hasCost }.forEach { feature ->
            val cost = when {
                feature.actionCost.reactions > 0 -> state.t("a Reaction", "eine Reaktion")
                feature.actionCost.bonusActions > 0 -> state.t("a Bonus Action", "eine Bonusaktion")
                feature.actionCost.actions > 0 || feature.actionCost.pf2eActions > 0 -> state.t("an Action", "eine Aktion")
                else -> state.t("a resource", "eine Ressource")
            }
            append(state.t("\nActivating ${feature.name} costs $cost.", "${feature.name} zu aktivieren kostet $cost."))
        }
    }
}

private fun speedCostTokens(character: CharacterUi): List<CostTokenUi> = buildList {
    if (character.effectiveFlySpeedFeet != null) add(CostTokenUi(CostTokenKind.Action))
    character.features.filter { feature ->
        val hint = "${feature.id} ${feature.name} ${feature.summary}".lowercase()
        ("fly" in hint || "flight" in hint || "wing" in hint) && feature.actionCost.hasCost
    }.flatMapTo(this) { it.actionCost.toCostTokens() }
}.distinct()

private fun StatCalculationUi.detailsText(state: DndAppState): String = buildString {
    append(state.t("Total", "Gesamt"))
    append(": ")
    append(total)
    sources.forEach { source ->
        append("\n")
        append(if (source.active) "• " else "○ ")
        append(source.label)
        source.amount?.let { append(" ${signed(it)}") }
        if (source.detail.isNotBlank()) {
            append(" — ")
            append(source.detail)
        }
    }
}

private fun weaponDetails(state: DndAppState, weapon: WeaponUi): String = buildString {
    append(state.t("Reach", "Reichweite"))
    append(" ${weapon.reachFeet} ft")
    if (weapon.range.isNotBlank()) append(" · ${weapon.range}")
    if (weapon.properties.isNotBlank()) append("\n${weapon.properties}")
    if (weapon.useCase.isNotBlank()) append("\n${weapon.useCase}")
    state.selectedCharacter?.let { character ->
        weapon.savingThrows.forEach { prompt ->
            append("\n${prompt.ability.displayName()} DC ${CharacterStatEngine.difficultyClass(character, prompt.difficultyClass)}")
        }
    }
}

private fun equipmentDetails(state: DndAppState, character: CharacterUi, item: EquipmentUi): String = buildString {
    if (item.details.isNotBlank()) append(item.details)
    if (item.useCase.isNotBlank()) {
        if (isNotEmpty()) append("\n")
        append(item.useCase)
    }
    item.savingThrows.forEach { prompt ->
        if (isNotEmpty()) append("\n")
        append("${prompt.ability.displayName()} DC ${CharacterStatEngine.difficultyClass(character, prompt.difficultyClass)}")
    }
    item.effects.forEach { effect ->
        if (isNotEmpty()) append("\n")
        append("${signed(effect.amount)} ${effect.statistic.name.replace('_', ' ').lowercase()}")
    }
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
