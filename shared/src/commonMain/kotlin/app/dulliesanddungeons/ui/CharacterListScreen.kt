package app.dulliesanddungeons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun CharacterListScreen(state: DndAppState) {
    var pendingDeletion by remember { mutableStateOf<CharacterUi?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dullies & Dungeons", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(state.t("Your heroes", "Deine Held:innen"), style = MaterialTheme.typography.headlineLarge)
                }
                LanguageButton(state)
            }
        }

        item {
            Button(
                onClick = state::beginCreate,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(state.t("Create a character", "Charakter erstellen"))
            }
        }

        item {
            Spacer(Modifier.height(2.dp))
            Text(
                state.t("CHARACTERS · ${state.characters.size}", "CHARAKTERE · ${state.characters.size}"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }

        if (state.characters.isEmpty()) {
            item { EmptyCharacters(state) }
        } else {
            items(state.characters, key = { it.id }) { character ->
                CharacterCard(character, state, onDeleteRequest = { pendingDeletion = character })
            }
        }
    }

    pendingDeletion?.let { character ->
        DeleteCharacterConfirmationDialog(
            state = state,
            character = character,
            onDismiss = { pendingDeletion = null },
            onConfirm = {
                state.deleteCharacter(character.id)
                pendingDeletion = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterCard(
    character: CharacterUi,
    state: DndAppState,
    onDeleteRequest: () -> Unit,
) {
    val currentCharacterId by rememberUpdatedState(character.id)
    val currentDeleteRequest by rememberUpdatedState(onDeleteRequest)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> state.beginEdit(characterId = currentCharacterId)
                SwipeToDismissBoxValue.EndToStart -> currentDeleteRequest()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val editing = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                color = if (editing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentAlignment = if (editing) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    Icon(
                        if (editing) Icons.Rounded.Edit else Icons.Rounded.Delete,
                        contentDescription = if (editing) {
                            state.t("Edit ${character.name}", "${character.name} bearbeiten")
                        } else {
                            state.t("Delete ${character.name}", "${character.name} löschen")
                        },
                        tint = if (editing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    ) {
        Card(
            onClick = { state.openCharacter(character.id) },
            modifier = Modifier.fillMaxWidth().semantics {
                customActions = listOf(
                    CustomAccessibilityAction(state.t("Edit ${character.name}", "${character.name} bearbeiten")) {
                        state.beginEdit(characterId = character.id)
                        true
                    },
                    CustomAccessibilityAction(state.t("Delete ${character.name}", "${character.name} löschen")) {
                        onDeleteRequest()
                        true
                    },
                )
            },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CharacterPortrait(character.name, character.portraitSeed, Modifier.size(64.dp), state.portraitBytes(character))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(character.name, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        if (character.isDead) {
                            Text(
                                "☠",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics { contentDescription = state.t("Dead", "Tot") },
                            )
                        }
                    }
                    Text(
                        state.t(
                            "Level ${character.level} ${character.ancestry} ${character.className}",
                            "Stufe ${character.level} · ${character.ancestry} · ${character.className}",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RulesetBadge(character.ruleset)
                }
                FilledTonalIconButton(onClick = { state.openCharacter(character.id) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = state.t("Open ${character.name}", "${character.name} öffnen"))
                }
            }
        }
    }
}

@Composable
private fun EmptyCharacters(state: DndAppState) {
    Box(Modifier.fillMaxWidth().padding(vertical = 44.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.outline)
            Text(state.t("Your first adventure starts here", "Dein erstes Abenteuer beginnt hier"), style = MaterialTheme.typography.titleMedium)
            Text(state.t("We will explain every choice as you go.", "Wir erklären dir jede Entscheidung unterwegs."), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
