package app.dulliesanddungeons.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.dulliesanddungeons.domain.ActivityRecord
import app.dulliesanddungeons.domain.AttackOutcomeRecord
import app.dulliesanddungeons.domain.HitPointChangeKind
import app.dulliesanddungeons.domain.PlaySessionRecord
import app.dulliesanddungeons.domain.TurnEvent

@Composable
internal fun SessionHistoryDialog(state: DndAppState) {
    val character = state.selectedCharacter ?: return
    var renaming by remember { mutableStateOf<PlaySessionRecord?>(null) }
    var deleting by remember { mutableStateOf<PlaySessionRecord?>(null) }
    Dialog(
        onDismissRequest = { state.sessionHistoryOpen = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f).widthIn(max = 620.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(state.t("History", "Verlauf"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { state.sessionHistoryOpen = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = state.t("Close history", "Verlauf schließen"))
                        }
                    }
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        character.activePlaySession?.let { active ->
                            item(key = "current-${active.id}") {
                                SessionCard(
                                    state = state,
                                    session = active,
                                    title = state.t("Current session", "Aktuelle Sitzung"),
                                    current = true,
                                    onRename = {},
                                    onDelete = {},
                                )
                            }
                        }
                        if (character.savedPlaySessions.isNotEmpty()) {
                            item { Text(state.t("Saved sessions", "Gespeicherte Sitzungen"), style = MaterialTheme.typography.titleMedium) }
                            items(character.savedPlaySessions.sortedByDescending { it.savedAtEpochMillis ?: 0L }, key = { it.id }) { session ->
                                SessionCard(
                                    state = state,
                                    session = session,
                                    title = session.displayTitle(state),
                                    current = false,
                                    onRename = { renaming = session },
                                    onDelete = { deleting = session },
                                )
                            }
                        } else if (character.activePlaySession == null) {
                            item {
                                Text(
                                    state.t("Your first tracked action will start a session.", "Deine erste erfasste Aktion startet eine Sitzung."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    renaming?.let { session ->
        var title by remember(session.id) { mutableStateOf(session.displayTitle(state)) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(state.t("Rename session", "Sitzung umbenennen")) },
            text = { OutlinedTextField(value = title, onValueChange = { title = it.take(80) }, singleLine = true, label = { Text(state.t("Name", "Name")) }) },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(state.t("Cancel", "Abbrechen")) } },
            confirmButton = {
                Button(onClick = { state.renamePlaySession(session.id, title); renaming = null }, enabled = title.isNotBlank()) {
                    Text(state.t("Save", "Speichern"))
                }
            },
        )
    }
    deleting?.let { session ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(state.t("Delete session?", "Sitzung löschen?")) },
            text = { Text(session.displayTitle(state)) },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(state.t("Keep", "Behalten")) } },
            confirmButton = {
                Button(onClick = { state.deletePlaySession(session.id); deleting = null }) { Text(state.t("Delete", "Löschen")) }
            },
        )
    }
}

@Composable
internal fun SaveSessionDialog(state: DndAppState) {
    val session = state.currentPlaySession ?: return
    var title by remember(session.id) { mutableStateOf(session.displayTitle(state)) }
    AlertDialog(
        onDismissRequest = { state.sessionSaveOpen = false },
        title = { Text(state.t("Save session", "Sitzung speichern")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionStatsSummary(state, session.stats())
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text(state.t("Session name", "Sitzungsname")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        dismissButton = { TextButton(onClick = { state.sessionSaveOpen = false }) { Text(state.t("Cancel", "Abbrechen")) } },
        confirmButton = {
            Button(onClick = { state.savePlaySession(title) }, enabled = session.activities.isNotEmpty() && title.isNotBlank()) {
                Text(state.t("Save", "Speichern"))
            }
        },
    )
}

@Composable
private fun SessionCard(
    state: DndAppState,
    session: PlaySessionRecord,
    title: String,
    current: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember(session.id) { mutableStateOf(current) }
    var menuOpen by remember(session.id) { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (current) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (current) MaterialTheme.colorScheme.primary.copy(alpha = .45f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.t("${session.activities.size} events", "${session.activities.size} Ereignisse"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (current) {
                    TextButton(onClick = { state.sessionSaveOpen = true }, enabled = session.activities.isNotEmpty()) {
                        Text(state.t("Save", "Speichern"))
                    }
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = state.t("Session options", "Sitzungsoptionen")) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(state.t("Rename", "Umbenennen")) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = { menuOpen = false; onRename() },
                            )
                            DropdownMenuItem(
                                text = { Text(state.t("Delete", "Löschen")) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = if (expanded) state.t("Collapse", "Einklappen") else state.t("Expand", "Ausklappen"))
                }
            }
            SessionStatsSummary(state, session.stats())
            if (expanded) SessionTimeline(state, session)
        }
    }
}

@Composable
private fun SessionStatsSummary(state: DndAppState, stats: SessionStatsUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatCell(state.t("Turns", "Züge"), stats.turns.toString(), Icons.Rounded.PlayArrow, Modifier.weight(1f))
            StatCell(state.t("Damage", "Schaden"), stats.damageTaken.toString(), Icons.Rounded.Favorite, Modifier.weight(1f))
            StatCell(state.t("Healing", "Heilung"), stats.healingReceived.toString(), Icons.Rounded.Favorite, Modifier.weight(1f))
            StatCell(state.t("Downs", "K. o."), stats.timesDowned.toString(), Icons.Rounded.Favorite, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatCell(state.t("Moved", "Bewegt"), "${stats.distanceMoved} ft", Icons.AutoMirrored.Rounded.DirectionsRun, Modifier.weight(1f))
            StatCell(state.t("Attacks", "Angriffe"), stats.attacks.toString(), Icons.Rounded.SportsMma, Modifier.weight(1f))
            StatCell(state.t("Criticals", "Kritisch"), stats.criticals.toString(), Icons.Rounded.SportsMma, Modifier.weight(1f))
            StatCell(state.t("Rolls", "Würfe"), "${stats.rolls} · 20:${stats.naturalTwenties} · 1:${stats.naturalOnes}", Icons.Rounded.History, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SessionTimeline(state: DndAppState, session: PlaySessionRecord) {
    val segments = session.activities.sortedBy { it.sequence }.fold(mutableListOf<MutableList<ActivityRecord>>()) { groups, activity ->
        val current = groups.lastOrNull()
        if (current == null || current.first().turnNumber != activity.turnNumber) groups += mutableListOf(activity)
        else current += activity
        groups
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { activities ->
            val turn = activities.firstOrNull()?.turnNumber
            Text(
                turn?.let { state.t("Turn $it", "Zug $it") } ?: state.t("Between turns", "Zwischen den Zügen"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            activities.forEach { SessionActivityRow(state, it) }
        }
    }
}

@Composable
internal fun SessionActivityRow(state: DndAppState, activity: ActivityRecord) {
    val event = activity.turnEvent
    val roll = activity.roll ?: (event as? TurnEvent.RollRecorded)?.roll
    var details by remember(activity.id) { mutableStateOf(false) }
    val title = when (event) {
        is TurnEvent.ActionUsed -> when (event.actionId) {
            "dash" -> state.t("Dashed", "Gesprintet")
            "flight" -> state.t("Started flying", "Flug begonnen")
            "bonus-action" -> state.t("Bonus action", "Bonusaktion")
            else -> activity.label.replace('-', ' ').replaceFirstChar { it.uppercase() }
        }
        is TurnEvent.AttackMade -> state.t("Attacked with ${activity.label}", "Mit ${activity.label} angegriffen")
        is TurnEvent.AttackResolved -> when (event.outcome) {
            AttackOutcomeRecord.MISS -> state.t("${activity.label}: miss", "${activity.label}: verfehlt")
            AttackOutcomeRecord.HIT -> state.t("${activity.label}: hit", "${activity.label}: Treffer")
            AttackOutcomeRecord.CRITICAL -> state.t("${activity.label}: critical", "${activity.label}: kritisch")
        }
        is TurnEvent.HitPointsChanged -> if (event.kind == HitPointChangeKind.DAMAGE) {
            state.t("Took ${event.amount} damage · ${event.hitPointsAfter} HP", "${event.amount} Schaden · ${event.hitPointsAfter} TP")
        } else {
            state.t("Healed ${event.effectiveHitPointChange.coerceAtLeast(0)} HP", "${event.effectiveHitPointChange.coerceAtLeast(0)} TP geheilt")
        }
        is TurnEvent.Moved -> state.t("Moved ${event.distanceFeet} ft", "${event.distanceFeet} ft bewegt")
        is TurnEvent.MovementGranted -> state.t("Gained ${event.amountFeet} ft movement", "${event.amountFeet} ft Bewegung erhalten")
        is TurnEvent.RollRecorded -> state.t("${event.roll.request.label}: ${event.roll.total}", "${event.roll.request.label}: ${event.roll.total}")
        is TurnEvent.ConditionApplied -> state.t("Condition added: ${activity.label}", "Zustand hinzugefügt: ${activity.label}")
        is TurnEvent.ConditionRemoved -> state.t("Condition removed: ${activity.label}", "Zustand entfernt: ${activity.label}")
        is TurnEvent.FeatureStarted -> state.t("Used ${activity.label}", "${activity.label} genutzt")
        is TurnEvent.FeatureResolved -> state.t("Resolved ${activity.label}", "${activity.label} abgeschlossen")
        is TurnEvent.ResourceChanged -> state.t("${activity.label}: ${if (event.amount >= 0) "+" else ""}${event.amount}", "${activity.label}: ${if (event.amount >= 0) "+" else ""}${event.amount}")
        TurnEvent.TurnEnded -> state.t("Turn ended", "Zug beendet")
        null -> activity.label
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(7.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (roll != null) {
                TextButton(onClick = { details = !details }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                    Text(state.t("Details", "Details"), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (details && roll != null) {
            val dice = roll.dice.joinToString(" + ")
            val diceTotal = roll.keptDice.sum()
            val modifier = roll.total - diceTotal
            Text(
                buildString {
                    append(dice)
                    if (modifier > 0) append(" + $modifier")
                    if (modifier < 0) append(" − ${kotlin.math.abs(modifier)}")
                    append(" = ${roll.total}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 17.dp),
            )
        }
    }
}

private fun PlaySessionRecord.displayTitle(state: DndAppState): String =
    title.ifBlank { state.t("Session $ordinal", "Sitzung $ordinal") }
