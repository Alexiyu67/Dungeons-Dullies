package app.dulliesanddungeons.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.RollMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TurnAssistantDialog(state: DndAppState) {
    val character = state.selectedCharacter ?: return
    val session = state.turnSession ?: return
    var overviewSelection by remember(session) { mutableStateOf<TurnSection?>(null) }
    var dashSelected by remember(session) { mutableStateOf(false) }
    var postMoveChooserOpen by remember(session) { mutableStateOf(false) }
    var endReviewOpen by remember(session) { mutableStateOf(false) }
    var discardReviewOpen by remember(session) { mutableStateOf(false) }
    var selectedOtherAction by remember(session) { mutableStateOf<String?>(null) }
    var selectedSpellSlotLevel by remember(session) { mutableStateOf<Int?>(null) }
    val turnStopped = character.stopsTurnGuide
    val selectedSpell = character.availableSpells.firstOrNull { it.id == session.selectedSpellId }
    LaunchedEffect(selectedSpell?.id, character.spellSlots) {
        selectedSpellSlotLevel = selectedSpell?.takeIf { it.level > 0 }
            ?.let { state.availableSpellSlotLevels(it, character).firstOrNull() }
    }
    val continueEnabled = !turnStopped && when (session.selectedSection) {
        TurnSection.Overview -> overviewSelection != null
        TurnSection.Move -> {
            val actionCost = (if (dashSelected) 1 else 0) + (if (session.requiresFlightAction) 1 else 0)
            session.requestedMovement > 0 && (session.remainingMovement > 0 || dashSelected) && session.standardActionsAvailable >= actionCost
        }
        TurnSection.Attack -> session.lastAttackDetails != null && session.attackOutcome != AttackOutcome.Pending
        TurnSection.Spell -> selectedSpell?.let { state.canCastSpell(it, selectedSpellSlotLevel, session) } == true
        TurnSection.Other -> selectedOtherAction != null
    }
    fun continueTurn() {
        when (session.selectedSection) {
            TurnSection.Overview -> overviewSelection?.let { session.selectedSection = it }
            TurnSection.Move -> {
                if (!session.commitFlightActivation()) return
                if (dashSelected && session.ruleset != Ruleset.Pf2eRemaster) {
                    if (!session.commitDash()) return
                }
                session.commitMovement()
                session.markSuggestionComplete("move")
                dashSelected = false
                postMoveChooserOpen = true
            }
            TurnSection.Attack -> {
                if (session.canAttack) {
                    session.lastAttackDetails = null
                    session.lastAttackRoll = null
                    session.lastDamageDetails = null
                    session.lastDamageRoll = null
                    session.attackOutcome = AttackOutcome.Pending
                } else session.selectedSection = TurnSection.Overview
            }
            TurnSection.Spell -> {
                selectedSpell?.takeIf { state.castSpell(it, selectedSpellSlotLevel, session) }?.let {
                    session.markSuggestionComplete("spell")
                    session.selectedSection = TurnSection.Overview
                }
            }
            TurnSection.Other -> {
                if (session.canUseAction) session.commitAction(selectedOtherAction ?: "action")
                selectedOtherAction = null
                session.selectedSection = TurnSection.Overview
            }
        }
    }
    Dialog(
        onDismissRequest = state::closeTurnGuide,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(state.t("Turn", "Zug"))
                                Text(character.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        actions = {
                            if (!turnStopped) {
                                IconButton(onClick = { session.suggestionsVisible = !session.suggestionsVisible }) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = state.t("Suggested turn", "Vorgeschlagener Zug"), tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            IconButton(onClick = state::closeTurnGuide) {
                                Icon(Icons.Rounded.Close, contentDescription = state.t("Close and resume later", "Schließen und später fortsetzen"))
                            }
                        },
                    )
                },
                bottomBar = {
                    if (turnStopped) TurnStoppedBottomBar(state) { if (!state.nextTurn()) endReviewOpen = true }
                    else TurnGuideBottomBar(
                            state = state,
                            character = character,
                            session = session,
                            continueEnabled = continueEnabled,
                            onContinue = ::continueTurn,
                            onEndTurn = { if (!state.nextTurn()) endReviewOpen = true },
                        )
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).paperTexture(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { TurnResourceBar(state, session) }
                    if (turnStopped) {
                        item { TurnStoppedPanel(state, character) }
                    } else {
                        if (session.suggestionsVisible) item { SuggestedTurnPanel(state, character, session) }
                        item {
                            when (session.selectedSection) {
                                TurnSection.Overview -> TurnOverview(state, character, session, overviewSelection) { overviewSelection = it }
                                TurnSection.Move -> MoveTurnSection(state, character, session, dashSelected) { dashSelected = it }
                                TurnSection.Attack -> AttackTurnSection(state, character, session)
                                TurnSection.Spell -> SpellTurnSection(
                                    state,
                                    character,
                                    session,
                                    selectedSpellSlotLevel,
                                    onSelectSlot = { selectedSpellSlotLevel = it },
                                )
                                TurnSection.Other -> OtherTurnSection(state, character, session, selectedOtherAction) { selectedOtherAction = it }
                            }
                        }
                    }
                }
            }
        }
    }

    if (postMoveChooserOpen && !turnStopped) {
        AlertDialog(
            onDismissRequest = { postMoveChooserOpen = false },
            title = { Text(state.t("What comes next?", "Was kommt als Nächstes?")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { postMoveChooserOpen = false; session.selectedSection = TurnSection.Attack },
                        enabled = character.weapons.isNotEmpty() && session.canAttack,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(state.t("Simple attack", "Einfacher Angriff")) }
                    if (character.canCastSpells) {
                        OutlinedButton(
                            onClick = { postMoveChooserOpen = false; session.selectedSection = TurnSection.Spell },
                            enabled = character.availableSpells.any { session.canPay(it.activationCost) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(state.t("Spell", "Zauber")) }
                    }
                    OutlinedButton(
                        onClick = { postMoveChooserOpen = false; session.selectedSection = TurnSection.Other },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(state.t("Other", "Anderes")) }
                }
            },
            confirmButton = { TextButton(onClick = { postMoveChooserOpen = false }) { Text(state.t("Not yet", "Noch nicht")) } },
        )
    }

    if (endReviewOpen) {
        AlertDialog(
            onDismissRequest = { endReviewOpen = false },
            title = { Text(state.t("Start the next turn?", "Nächsten Zug starten?")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(state.t("Recorded steps: ${session.eventCount}", "Erfasste Schritte: ${session.eventCount}"))
                    Text(state.t("Movement remaining: ${session.remainingMovement} ft", "Verbleibende Bewegung: ${session.remainingMovement} ft"))
                    if (session.ruleset == Ruleset.Pf2eRemaster) Text(state.t("Actions remaining: ${session.pf2ActionsRemaining}", "Verbleibende Aktionen: ${session.pf2ActionsRemaining}"))
                    else Text(state.t("Action available: ${if (session.canUseAction) "Yes" else "No"}", "Aktion verfügbar: ${if (session.canUseAction) "Ja" else "Nein"}"))
                    Text(state.t("This turn is empty. You can still advance.", "Dieser Zug ist leer. Du kannst trotzdem weitergehen."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = { endReviewOpen = false; discardReviewOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(state.t("Discard turn draft", "Zugentwurf verwerfen")) }
                }
            },
            dismissButton = { TextButton(onClick = { endReviewOpen = false }) { Text(state.t("Go back", "Zurück")) } },
            confirmButton = {
                Button(onClick = {
                    endReviewOpen = false
                    state.nextTurn(confirmEmpty = true)
                }) { Text(state.t("Next turn", "Nächster Zug")) }
            },
        )
    }

    if (discardReviewOpen) {
        AlertDialog(
            onDismissRequest = { discardReviewOpen = false },
            title = { Text(state.t("Discard this turn?", "Diesen Zug verwerfen?")) },
            text = { Text(state.t("Recorded movement, attacks, and choices in this guide will be cleared.", "Erfasste Bewegung, Angriffe und Entscheidungen in dieser Führung werden gelöscht.")) },
            dismissButton = { TextButton(onClick = { discardReviewOpen = false }) { Text(state.t("Keep", "Behalten")) } },
            confirmButton = {
                Button(onClick = {
                    discardReviewOpen = false
                    state.discardTurn()
                }) { Text(state.t("Discard", "Verwerfen")) }
            },
        )
    }
}

@Composable
private fun TurnGuideBottomBar(
    state: DndAppState,
    character: CharacterUi,
    session: TurnSession,
    continueEnabled: Boolean,
    onContinue: () -> Unit,
    onEndTurn: () -> Unit,
) {
    Surface(tonalElevation = 5.dp, shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            TurnSectionRail(state, character, session)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEndTurn, modifier = Modifier.weight(1f).height(50.dp)) { Text(state.t("Next turn", "Nächster Zug")) }
                Button(onClick = onContinue, enabled = continueEnabled, modifier = Modifier.weight(1f).height(50.dp)) { Text(state.t("Continue", "Weiter")) }
            }
        }
    }
}

@Composable
private fun TurnStoppedBottomBar(state: DndAppState, onEndTurn: () -> Unit) {
    Surface(tonalElevation = 5.dp, shadowElevation = 6.dp) {
        Button(
            onClick = onEndTurn,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(14.dp),
        ) { Text(state.t("Next turn", "Nächster Zug")) }
    }
}

@Composable
private fun TurnStoppedPanel(state: DndAppState, character: CharacterUi) {
    val dead = character.isDead
    val title = when {
        dead -> state.t("Dead", "Tot")
        character.isStable -> state.t("Stable at 0 HP", "Stabil bei 0 TP")
        character.ruleset == Ruleset.Pf2eRemaster -> state.t("Dying ${character.dyingValue}", "Sterbend ${character.dyingValue}")
        else -> state.t("Down at 0 HP", "Bei 0 TP am Boden")
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (dead) Icons.Rounded.Dangerous else Icons.Rounded.HeartBroken,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(74.dp),
        )
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.error)
        Text(state.t("This turn is stopped.", "Dieser Zug ist gestoppt."), style = MaterialTheme.typography.titleMedium)
        if (dead) {
            character.deathReason?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (character.ruleset == Ruleset.Pf2eRemaster) {
            val threshold = (4 - character.doomedValue).coerceAtLeast(1)
            Spacer(Modifier.height(6.dp))
            ValuePips(state.t("Dying", "Sterbend"), character.dyingValue, threshold, MaterialTheme.colorScheme.error)
            ValuePips(state.t("Wounded", "Verwundet"), character.woundedValue, 3, MaterialTheme.colorScheme.tertiary)
            ValuePips(state.t("Doomed", "Dem Tode geweiht"), character.doomedValue, 3, MaterialTheme.colorScheme.secondary)
            Text(
                state.t("Recovery checks begin on the character's next turn.", "Erholungswürfe beginnen im nächsten Zug des Charakters."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.height(6.dp))
            ValuePips(state.t("Successes", "Erfolge"), character.deathSaveSuccesses, 3, MaterialTheme.colorScheme.primary)
            ValuePips(state.t("Failures", "Fehlschläge"), character.deathSaveFailures, 3, MaterialTheme.colorScheme.error)
            Text(
                if (character.isStable) state.t("No death saves are needed while stable.", "Solange der Charakter stabil ist, sind keine Todesrettungswürfe nötig.")
                else state.t("Death saves begin on the character's next turn.", "Todesrettungswürfe beginnen im nächsten Zug des Charakters."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ValuePips(label: String, value: Int, maximum: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = color)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(maximum.coerceAtLeast(1)) { index ->
                val filled = index < value
                Surface(
                    modifier = Modifier.size(29.dp),
                    shape = CircleShape,
                    color = if (filled) color else androidx.compose.ui.graphics.Color.Transparent,
                    border = BorderStroke(2.dp, color),
                ) {
                    if (filled) Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TurnSectionRail(state: DndAppState, character: CharacterUi, session: TurnSession) {
    val sections = buildList {
        add(TurnSection.Overview)
        add(TurnSection.Move)
        add(TurnSection.Attack)
        if (character.canCastSpells) add(TurnSection.Spell)
        add(TurnSection.Other)
    }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            sections.forEach { section ->
                val selected = session.selectedSection == section
                Surface(
                    onClick = { session.selectedSection = section },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(sectionIcon(section), contentDescription = null, Modifier.size(19.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sectionLabel(state, section), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
}

@Composable
private fun TurnResourceBar(state: DndAppState, session: TurnSession) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CostChip(
            state,
            CostTokenUi(CostTokenKind.Movement, labelOverride = state.t("Move ${session.remainingMovement}/${session.maxMovement} ft", "Beweg. ${session.remainingMovement}/${session.maxMovement} ft")),
            Modifier.weight(1f),
            available = session.remainingMovement > 0,
            fillWidth = true,
        )
        if (session.ruleset == Ruleset.Pf2eRemaster) {
            CostChip(state, CostTokenUi(CostTokenKind.Action, labelOverride = state.t("Actions ${session.pf2ActionsRemaining}", "Aktionen ${session.pf2ActionsRemaining}")), Modifier.weight(1f), session.pf2ActionsRemaining > 0, fillWidth = true)
        } else {
            CostChip(state, CostTokenUi(CostTokenKind.Action, labelOverride = state.t("Action ${if (session.canUseAction) 1 else 0}", "Aktion ${if (session.canUseAction) 1 else 0}")), Modifier.weight(1f), session.canUseAction, fillWidth = true)
            CostChip(state, CostTokenUi(CostTokenKind.BonusAction, labelOverride = state.t("Bonus ${if (session.bonusActionUsed) 0 else 1}", "Bonus ${if (session.bonusActionUsed) 0 else 1}")), Modifier.weight(1f), !session.bonusActionUsed, fillWidth = true)
        }
        CostChip(state, CostTokenUi(CostTokenKind.Reaction, labelOverride = state.t("Reaction ${if (session.reactionUsed) 0 else 1}", "Reaktion ${if (session.reactionUsed) 0 else 1}")), Modifier.weight(1f), !session.reactionUsed, fillWidth = true)
    }
}

@Composable
private fun TurnOverview(
    state: DndAppState,
    character: CharacterUi,
    session: TurnSession,
    selection: TurnSection?,
    onSelect: (TurnSection) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(state.t("What do you want to do?", "Was möchtest du tun?"), style = MaterialTheme.typography.headlineMedium)
        IntentCard(state, Icons.AutoMirrored.Rounded.DirectionsRun, state.t("Move", "Bewegen"), state.t("${session.remainingMovement} of ${session.maxMovement} ft available", "${session.remainingMovement} von ${session.maxMovement} ft verfügbar"), listOf(CostTokenUi(CostTokenKind.Movement)), session.remainingMovement > 0, selection == TurnSection.Move) { onSelect(TurnSection.Move) }
        IntentCard(state, Icons.Rounded.SportsMma, state.t("Attack", "Angriff"), character.weapons.firstOrNull()?.let { "${it.name} · ${signed(it.attackBonus)}" } ?: state.t("No attack configured", "Kein Angriff eingerichtet"), listOf(CostTokenUi(CostTokenKind.Action), CostTokenUi(CostTokenKind.Attack)), character.weapons.isNotEmpty() && session.canAttack, selection == TurnSection.Attack) { onSelect(TurnSection.Attack) }
        if (character.canCastSpells) {
            val spell = character.availableSpells.firstOrNull()
            IntentCard(state, Icons.Rounded.AutoAwesome, state.t("Spell", "Zauber"), spell?.let { spellSourceLabel(state, it) } ?: state.t("No spell currently available", "Derzeit kein Zauber verfügbar"), spell?.activationCost?.toCostTokens().orEmpty(), spell?.let { session.canPay(it.activationCost) } == true, selection == TurnSection.Spell) { onSelect(TurnSection.Spell) }
        }
        IntentCard(state, Icons.Rounded.MoreHoriz, state.t("Other", "Anderes"), state.t("Features and standard actions", "Merkmale und Standardaktionen"), listOf(CostTokenUi(CostTokenKind.Action)), session.canUseAction || character.features.isNotEmpty(), selection == TurnSection.Other) { onSelect(TurnSection.Other) }
    }
}

@Composable
private fun SuggestedTurnPanel(state: DndAppState, character: CharacterUi, session: TurnSession) {
    val steps = state.suggestedTurnSteps(character, session)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(9.dp))
                Text(state.t("Suggested turn", "Vorgeschlagener Zug"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { session.suggestionsVisible = false }, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = state.t("Hide guide", "Guide ausblenden"), Modifier.size(19.dp))
                }
            }
            steps.forEachIndexed { index, step ->
                val completed = step.id in session.completedSuggestionIds
                Surface(
                    onClick = { routeSuggestion(session, step) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (completed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(999.dp), color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer) {
                            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                                if (completed) Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                else Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(step.title, style = MaterialTheme.typography.titleSmall)
                                val feature = step.featureId?.let { id -> character.features.firstOrNull { it.id == id } }
                                val tokens = when {
                                    step.section == TurnSection.Move -> listOf(CostTokenUi(CostTokenKind.Movement))
                                    feature != null -> feature.toCostTokens()
                                    else -> step.cost.toCostTokens()
                                }
                                if (tokens.isNotEmpty()) CostChipRow(state, tokens, available = !completed)
                            }
                            Text(step.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun routeSuggestion(session: TurnSession, step: SuggestedTurnStepUi) {
    session.selectedSection = step.section
    step.weaponId?.let { session.selectedWeaponId = it }
    step.featureId?.let { session.selectedFeatureId = it }
}

@Composable
private fun IntentCard(state: DndAppState, icon: ImageVector, title: String, subtitle: String, costs: List<CostTokenUi>, enabled: Boolean, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (costs.isNotEmpty()) CostChipRow(state = state, tokens = costs, available = enabled)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MoveTurnSection(
    state: DndAppState,
    character: CharacterUi,
    session: TurnSession,
    dashSelected: Boolean,
    onDashSelected: (Boolean) -> Unit,
) {
    val selectableMovement = session.remainingMovement + if (dashSelected && session.ruleset != Ruleset.Pf2eRemaster) session.maxMovement else 0
    val dashApplied = session.dashActive || dashSelected
    var damageExpanded by remember(session) { mutableStateOf(false) }
    var damageText by remember(session) { mutableStateOf("1") }
    var criticalDamage by remember(session) { mutableStateOf(false) }
    var damageFeedback by remember(session) { mutableStateOf<String?>(null) }
    val damageAmount = damageText.toIntOrNull()?.coerceIn(0, 9_999) ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.t("Move", "Bewegen"), style = MaterialTheme.typography.headlineMedium)
        if (character.flySpeedFeet != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = !session.flying, onClick = { session.selectFlying(false) }, label = { Text(state.t("Walk", "Gehen")) })
                FilterChip(selected = session.flying, onClick = { session.selectFlying(true) }, leadingIcon = { Icon(Icons.Rounded.Flight, contentDescription = null, Modifier.size(18.dp)) }, label = { Text(state.t("Fly", "Flug")) })
                CostChip(state, CostTokenUi(CostTokenKind.Action), available = !session.flightActivationPaid)
            }
            if (session.requiresFlightAction) Text(state.t("Changing to Fly uses your Action.", "Der Wechsel zum Flug verbraucht deine Aktion."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        if (session.ruleset == Ruleset.Pf2eRemaster) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(state.t("Stride", "Laufen"), style = MaterialTheme.typography.titleSmall)
                        CostChip(state, CostTokenUi(CostTokenKind.Action), available = session.pf2ActionsRemaining > 0)
                    }
                    Text(state.t("Uses one action to move up to your Speed.", "Verbraucht eine Aktion für Bewegung bis zu deiner Bewegungsrate."), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Surface(
                onClick = { if (!session.dashActive && session.canUseAction) onDashSelected(!dashSelected) },
                color = if (dashApplied) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, if (dashApplied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(state.t("Dash", "Sprinten"), style = MaterialTheme.typography.titleSmall)
                            CostChip(state, CostTokenUi(CostTokenKind.Action), available = session.canUseAction || session.dashActive)
                        }
                        Text(state.t("Uses your Action and adds your Speed again—normally doubling movement.", "Verbraucht deine Aktion und fügt deine Bewegungsrate erneut hinzu—normalerweise verdoppelt sich die Bewegung."), style = MaterialTheme.typography.bodySmall)
                    }
                    FilterChip(
                        selected = dashApplied,
                        onClick = { onDashSelected(!dashSelected) },
                        enabled = !session.dashActive && session.canUseAction,
                        label = { Text(if (session.dashActive) state.t("Active", "Aktiv") else if (dashSelected) state.t("On", "An") else state.t("Off", "Aus")) },
                    )
                }
            }
        }
        Text("${session.requestedMovement.coerceAtMost(selectableMovement)} ft", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(state.t("$selectableMovement ft available", "$selectableMovement ft verfügbar"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = session.requestedMovement.coerceIn(0, selectableMovement.coerceAtLeast(5)).toFloat(),
            onValueChange = { session.requestedMovement = (it.roundToInt() / 5) * 5 },
            valueRange = 0f..selectableMovement.coerceAtLeast(5).toFloat(),
            steps = (selectableMovement / 5 - 1).coerceAtLeast(0),
            enabled = selectableMovement > 0,
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { damageExpanded = !damageExpanded },
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.HeartBroken, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(state.t("Take damage", "Schaden nehmen"), style = MaterialTheme.typography.titleSmall)
                            Text(
                                damageFeedback ?: state.t("e.g. from an opportunity attack", "z. B. durch einen Gelegenheitsangriff"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (damageFeedback == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            )
                        }
                        Icon(
                            if (damageExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (damageExpanded) state.t("Collapse damage", "Schaden einklappen") else state.t("Expand damage", "Schaden ausklappen"),
                        )
                    }
                }
                if (damageExpanded) {
                    Column(Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp, bottom = 13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.t("Damage", "Schaden"), style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { damageText = (damageAmount.coerceAtLeast(1) - 1).coerceAtLeast(1).toString() },
                                enabled = damageAmount > 1,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(48.dp),
                            ) { Icon(Icons.Rounded.Remove, contentDescription = state.t("Decrease damage", "Schaden verringern")) }
                            OutlinedTextField(
                                value = damageText,
                                onValueChange = { damageText = it.filter(Char::isDigit).take(4) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text(state.t("Amount", "Menge")) },
                            )
                            OutlinedButton(
                                onClick = { damageText = (damageAmount.coerceAtLeast(0) + 1).coerceAtMost(9_999).toString() },
                                enabled = damageAmount < 9_999,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(48.dp),
                            ) { Icon(Icons.Rounded.Add, contentDescription = state.t("Increase damage", "Schaden erhöhen")) }
                        }
                        if (session.ruleset == Ruleset.Pf2eRemaster) {
                            FilterChip(
                                selected = criticalDamage,
                                onClick = { criticalDamage = !criticalDamage },
                                label = { Text(state.t("Critical hit", "Kritischer Treffer")) },
                                leadingIcon = if (criticalDamage) {{ Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(17.dp)) }} else null,
                            )
                        }
                        Button(
                            onClick = {
                                state.applyTurnDamage(damageAmount, criticalDamage, session)?.let { updated ->
                                    damageFeedback = state.t(
                                        "$damageAmount damage · ${updated.hp}/${updated.effectiveMaxHp} HP",
                                        "$damageAmount Schaden · ${updated.hp}/${updated.effectiveMaxHp} TP",
                                    )
                                    damageExpanded = false
                                    damageText = "1"
                                    criticalDamage = false
                                }
                            },
                            enabled = damageAmount in 1..9_999,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(13.dp),
                        ) { Text(state.t("Apply $damageAmount damage", "$damageAmount Schaden anwenden")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttackTurnSection(state: DndAppState, character: CharacterUi, session: TurnSession) {
    val selected = character.weapons.firstOrNull { it.id == session.selectedWeaponId } ?: character.weapons.firstOrNull()
    var weaponMenuOpen by remember(session) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        if (selected == null) {
            ExplanationCard(state.t("No attack", "Kein Angriff"), state.t("Add a weapon, natural attack, or unarmed strike first.", "Füge zuerst eine Waffe, einen natürlichen oder waffenlosen Angriff hinzu."))
            return@Column
        }
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { weaponMenuOpen = true },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(selected.name, style = MaterialTheme.typography.labelLarge)
                        CostChip(state, CostTokenUi(CostTokenKind.Attack), available = session.canAttack)
                    }
                    Text("${signed(selected.attackBonus)} · ${selected.damage} ${selected.damageType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = state.t("Choose weapon", "Waffe wählen"))
            }
            DropdownMenu(expanded = weaponMenuOpen, onDismissRequest = { weaponMenuOpen = false }) {
                character.weapons.forEach { weapon ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(weapon.name, style = MaterialTheme.typography.labelLarge)
                                Text("${signed(weapon.attackBonus)} · ${weapon.damage} ${weapon.damageType}", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        leadingIcon = if (weapon.id == selected.id) {{ Icon(Icons.Rounded.Check, contentDescription = null) }} else null,
                        onClick = {
                            weaponMenuOpen = false
                            session.selectedWeaponId = weapon.id
                            session.lastAttackDetails = null
                            session.lastAttackRoll = null
                            session.lastDamageDetails = null
                            session.lastDamageRoll = null
                            session.attackOutcome = AttackOutcome.Pending
                        },
                    )
                }
            }
        }
        AttackCard(
            state = state,
            character = character,
            weapon = selected,
            roll = session.lastAttackDetails,
            damage = session.lastDamageDetails,
            outcome = session.attackOutcome,
            canRoll = session.canAttack,
            onRoll = {
                state.rollAttack(selected, session, RollMode.NORMAL)
                state.dicePresentation = null
                session.markSuggestionComplete("attack")
            },
            onOutcome = { outcome ->
                session.attackOutcome = outcome
                state.recordAttackOutcome(selected, outcome, session)
                when (outcome) {
                    AttackOutcome.Hit -> state.rollDamage(selected, session, false)
                    AttackOutcome.Critical -> state.rollDamage(selected, session, true)
                    AttackOutcome.Miss -> {
                        session.lastDamageDetails = null
                        session.lastDamageRoll = null
                        session.finishAttackResolution()
                    }
                    AttackOutcome.Pending -> { session.lastDamageDetails = null; session.lastDamageRoll = null }
                }
                state.dicePresentation = null
            },
            onDismiss = { session.selectedSection = TurnSection.Overview },
        )
    }
}

@Composable
private fun SpellTurnSection(
    state: DndAppState,
    character: CharacterUi,
    session: TurnSession,
    selectedSlotLevel: Int?,
    onSelectSlot: (Int?) -> Unit,
) {
    val spells = character.availableSpells
    val selectedSpell = spells.firstOrNull { it.id == session.selectedSpellId }
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(state.t("Cast a spell", "Zauber wirken"), style = MaterialTheme.typography.headlineMedium)
        if (character.resolvedSpellSlots.isNotEmpty()) {
            Text(
                character.resolvedSpellSlots.joinToString("  ·  ") { "L${it.level} ${it.remaining}/${it.maximum}" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (spells.isEmpty()) {
            ExplanationCard(state.t("No available spells", "Keine verfügbaren Zauber"), state.t("Spellcasting is available, but no prepared or attuned item spell can be used now.", "Zaubern ist verfügbar, aber derzeit kann kein vorbereiteter oder eingestimmter Gegenstandszauber genutzt werden."))
        } else {
            spells.forEach { spell ->
                val slotLevels = state.availableSpellSlotLevels(spell, character)
                val available = session.canPay(spell.activationCost) && (spell.level == 0 || slotLevels.isNotEmpty())
                Card(
                    onClick = {
                        session.selectedSpellId = spell.id
                        onSelectSlot(if (spell.level == 0) null else slotLevels.firstOrNull())
                    },
                    enabled = available,
                    colors = CardDefaults.cardColors(containerColor = if (session.selectedSpellId == spell.id) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(17.dp),
                    border = BorderStroke(1.dp, if (session.selectedSpellId == spell.id) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(spell.name, style = MaterialTheme.typography.titleSmall)
                                CostChipRow(state, spell.activationCost.toCostTokens(), available = available)
                            }
                            Text(spellSourceLabel(state, spell), style = MaterialTheme.typography.labelSmall, color = if (spell.sourceKind == SpellSourceKind.ITEM) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(spell.summary, style = MaterialTheme.typography.bodySmall)
                        }
                        if (session.selectedSpellId == spell.id) Icon(Icons.Rounded.Check, contentDescription = state.t("Selected", "Ausgewählt"))
                    }
                }
            }
            selectedSpell?.takeIf { it.level > 0 }?.let { spell ->
                val slotLevels = state.availableSpellSlotLevels(spell, character)
                if (slotLevels.isNotEmpty()) {
                    Text(state.t("Spell slot", "Zauberplatz"), style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(slotLevels) { level ->
                            FilterChip(
                                selected = selectedSlotLevel == level,
                                onClick = { onSelectSlot(level) },
                                label = { Text(state.t("Level $level", "Grad $level")) },
                            )
                        }
                    }
                }
            }
            Text(state.t("Continue to use the selected spell.", "Weiter, um den gewählten Zauber zu nutzen."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OtherTurnSection(
    state: DndAppState,
    character: CharacterUi,
    session: TurnSession,
    selectedAction: String?,
    onSelectAction: (String) -> Unit,
) {
    val actions = listOf(
        Triple(state.t("Disengage", "Lösen"), state.t("Avoid opportunity attacks", "Gelegenheitsangriffe vermeiden"), listOf(CostTokenUi(CostTokenKind.Action))),
        Triple(state.t("Dodge", "Ausweichen"), state.t("Defend until next turn", "Bis zum nächsten Zug verteidigen"), listOf(CostTokenUi(CostTokenKind.Action))),
        Triple(state.t("Help", "Helfen"), state.t("Assist an ally", "Verbündeten helfen"), listOf(CostTokenUi(CostTokenKind.Action))),
        Triple(state.t("Use an object", "Gegenstand nutzen"), state.t("Interact with the scene", "Mit der Szene interagieren"), listOf(CostTokenUi(CostTokenKind.Action), CostTokenUi(CostTokenKind.ObjectInteraction))),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(state.t("Features & other", "Merkmale & anderes"), style = MaterialTheme.typography.headlineMedium)
        val featureGroups = character.features.groupBy(::featureFamily)
        FeatureFamily.entries.filter { it != FeatureFamily.General }.forEach { family ->
            featureGroups[family].orEmpty().takeIf { it.isNotEmpty() }?.let { familyFeatures ->
                val sharedPool = when (family) {
                    FeatureFamily.Maneuvers -> familyFeatures.firstOrNull { it.id == "superiority-dice" }
                    FeatureFamily.Metamagic -> familyFeatures.firstOrNull { it.id == "sorcery-points" }
                    else -> null
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(featureFamilyLabel(state, family), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    if (sharedPool != null) {
                        val die = sharedPool.resourceDieSides?.let { "d$it" }
                            ?: Regex("d\\d+", RegexOption.IGNORE_CASE).find(sharedPool.summary)?.value?.lowercase()
                        Text(listOfNotNull(die, "${sharedPool.remaining}/${sharedPool.maximum}").joinToString(" "), style = MaterialTheme.typography.labelLarge)
                    }
                }
                familyFeatures.filterNot { it.id == sharedPool?.id }
                    .forEach { feature -> TurnFeatureCard(state, session, feature) }
            }
        }
        featureGroups[FeatureFamily.General].orEmpty().filter(FeatureUi::isActivatable).takeIf { it.isNotEmpty() }?.let { generalFeatures ->
            Text(featureFamilyLabel(state, FeatureFamily.General), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            generalFeatures.forEach { feature -> TurnFeatureCard(state, session, feature) }
        }
        if (character.features.none { it.isActivatable() || featureFamily(it) != FeatureFamily.General }) {
            Text(state.t("No usable class features are available this turn.", "In diesem Zug sind keine nutzbaren Klassenmerkmale verfügbar."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        actions.forEach { (title, subtitle, costs) ->
            val selected = selectedAction == title
            Card(onClick = { onSelectAction(title) }, enabled = session.canUseAction, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant), border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (title.contains("Dodge") || title.contains("Ausweichen")) Icons.Rounded.Shield else Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall)
                            CostChipRow(state, costs, available = session.canUseAction)
                        }
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun TurnFeatureCard(state: DndAppState, session: TurnSession, feature: FeatureUi) {
    val feedback = state.inlineFeatureFeedback?.takeIf { it.featureId == feature.id }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(start = 13.dp, end = 5.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(feature.name, style = MaterialTheme.typography.titleSmall)
                if (feature.remaining != null && feature.resourceId == null) {
                    Text("${feature.remaining}/${feature.maximum} · ${recoveryLabel(state, feature.recovery)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(feature.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                if (feature.toCostTokens().isNotEmpty()) CostChipRow(state, feature.toCostTokens(), available = session.canPay(feature.actionCost) && (feature.remaining ?: 1) > 0)
            }
            if (feature.isActivatable()) {
                FilledTonalButton(
                    onClick = { if (state.useFeature(feature.id, session)) session.markSuggestionComplete("feature") },
                    enabled = (feature.remaining == null || feature.remaining >= feature.resourceCost) && session.canPay(feature.actionCost),
                    contentPadding = PaddingValues(horizontal = 11.dp),
                ) { Text(feedback?.message ?: state.t("Use", "Nutzen")) }
            }
            IconButton(onClick = { state.showInfo(feature.name, feature.summary, feature.toCostTokens()) }) {
                Icon(Icons.Rounded.ViewAgenda, contentDescription = state.t("Details for ${feature.name}", "Details zu ${feature.name}"), Modifier.size(19.dp))
            }
        }
    }
}

private fun sectionIcon(section: TurnSection): ImageVector = when (section) {
    TurnSection.Overview -> Icons.Rounded.ViewAgenda
    TurnSection.Move -> Icons.AutoMirrored.Rounded.DirectionsRun
    TurnSection.Attack -> Icons.Rounded.SportsMma
    TurnSection.Spell -> Icons.Rounded.AutoAwesome
    TurnSection.Other -> Icons.Rounded.MoreHoriz
}

private fun sectionLabel(state: DndAppState, section: TurnSection): String = when (section) {
    TurnSection.Overview -> state.t("Turn", "Zug")
    TurnSection.Move -> state.t("Move", "Bewegen")
    TurnSection.Attack -> state.t("Attack", "Angriff")
    TurnSection.Spell -> state.t("Spell", "Zauber")
    TurnSection.Other -> state.t("Other", "Anderes")
}

private fun spellSourceLabel(state: DndAppState, spell: SpellUi): String {
    val level = if (spell.level == 0) state.t("Cantrip", "Zaubertrick") else state.t("Level ${spell.level}", "Grad ${spell.level}")
    return spell.sourceName.takeIf { it.isNotBlank() }?.let { "$level · $it" } ?: level
}

private fun recoveryLabel(state: DndAppState, recovery: Recovery): String = when (recovery) {
    Recovery.TURN_START -> state.t("per turn", "pro Zug")
    Recovery.SHORT_REST -> state.t("short rest", "kurze Rast")
    Recovery.LONG_REST -> state.t("long rest", "lange Rast")
    Recovery.DAILY_PREPARATION -> state.t("daily", "täglich")
    Recovery.MANUAL -> state.t("manual", "manuell")
}

private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
