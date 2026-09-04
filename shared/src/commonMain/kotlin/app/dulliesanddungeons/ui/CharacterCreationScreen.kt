package app.dulliesanddungeons.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal enum class CreationStep(
    val englishLabel: String,
    val germanLabel: String,
) {
    Rules("Rules", "Regeln"),
    Identity("Identity", "Identität"),
    Build("Build", "Konzept"),
    LevelAndStats("Level & stats", "Stufe & Werte"),
    Details("Details", "Details"),
    Gear("Gear", "Ausrüstung"),
    Review("Review", "Überblick"),
}

private val LAST_CREATION_STEP = CreationStep.entries.lastIndex

@Composable
internal fun CharacterCreationScreen(
    state: DndAppState,
    onPickPortrait: (PortraitPickTarget) -> Unit,
    onEditPortrait: (PortraitPickTarget) -> Unit,
) {
    val draft = state.creation
    val currentStep = CreationStep.entries[draft.step.coerceIn(0, LAST_CREATION_STEP)]
    val contentState = rememberLazyListState()
    val spellSelection = if (currentStep == CreationStep.Details) state.creationSpellSelection() else null
    var spellPickerOpen by remember { mutableStateOf(false) }
    var spellSectionVisible by remember { mutableStateOf(false) }
    var contentViewport by remember { mutableStateOf<Rect?>(null) }
    val navigateToStep: (CreationStep) -> Unit = { step -> draft.step = step.ordinal }
    LaunchedEffect(draft.step) {
        contentState.scrollToItem(0)
        spellSectionVisible = false
        if (currentStep != CreationStep.Details) spellPickerOpen = false
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (currentStep == CreationStep.Rules) state.screen = AppScreen.Characters
                else navigateToStep(CreationStep.entries[currentStep.ordinal - 1])
            }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = state.t("Go back", "Zurück"))
            }
            Column(Modifier.weight(1f)) {
                Text(state.t("Create your hero", "Held:in erstellen"), style = MaterialTheme.typography.titleLarge)
                Text(
                    state.t(
                        "Step ${currentStep.ordinal + 1} of ${CreationStep.entries.size} · ${currentStep.englishLabel}",
                        "Schritt ${currentStep.ordinal + 1} von ${CreationStep.entries.size} · ${currentStep.germanLabel}",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LanguageButton(state)
        }
        CreationProgressStepper(
            state = state,
            currentStep = currentStep,
            onStepSelected = navigateToStep,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { contentViewport = it.boundsInWindow() },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = contentState,
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp,
                    bottom = if (spellSelection?.options?.isNotEmpty() == true) 92.dp else 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    when (currentStep) {
                        CreationStep.Rules -> RulesetStep(state)
                        CreationStep.Identity -> IdentityStep(state, onPickPortrait, onEditPortrait)
                        CreationStep.Build -> BuildStep(state)
                        CreationStep.LevelAndStats -> LevelAndStatsStep(state)
                        CreationStep.Details -> DetailsStep(
                            state = state,
                            spellSelection = spellSelection,
                            viewport = contentViewport,
                            onSpellSectionVisibilityChanged = { spellSectionVisible = it },
                        )
                        CreationStep.Gear -> GearStep(state)
                        CreationStep.Review -> ReviewStep(state, onEditPortrait)
                    }
                }
            }

            if (spellSelection?.options?.isNotEmpty() == true && spellSectionVisible) {
                Button(
                    onClick = { spellPickerOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(52.dp)
                        .widthIn(min = 184.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text(state.t("Add spells", "Zauber hinzufügen"))
                }
            }
        }

        Surface(shadowElevation = 8.dp, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentStep != CreationStep.Rules) {
                    OutlinedButton(
                        onClick = { navigateToStep(CreationStep.entries[currentStep.ordinal - 1]) },
                        modifier = Modifier.height(52.dp),
                    ) {
                        Text(state.t("Back", "Zurück"))
                    }
                }
                Button(
                    onClick = {
                        if (currentStep != CreationStep.Review) {
                            navigateToStep(CreationStep.entries[currentStep.ordinal + 1])
                        } else {
                            state.finishCreate()
                        }
                    },
                    enabled = currentStep == CreationStep.Review ||
                        (currentStep != CreationStep.Identity || draft.name.isNotBlank()) &&
                        (currentStep != CreationStep.Details ||
                            state.creationProficiencySelectionValid() && state.creationSpellSelectionValid()) &&
                        (currentStep != CreationStep.Gear || state.creationGearSelectionValid()),
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (currentStep == CreationStep.Review) state.t("Create character", "Charakter erstellen") else state.t("Continue", "Weiter"))
                    Spacer(Modifier.width(6.dp))
                    Icon(if (currentStep == CreationStep.Review) Icons.Rounded.Check else Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        }
    }

    if (spellPickerOpen && spellSelection != null) {
        CreationSpellPicker(
            state = state,
            selection = spellSelection,
            onDismiss = { spellPickerOpen = false },
        )
    }
}

@Composable
private fun CreationProgressStepper(
    state: DndAppState,
    currentStep: CreationStep,
    onStepSelected: (CreationStep) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CreationStep.entries.forEach { step ->
            val isCurrent = step == currentStep
            val isEarlier = step.ordinal < currentStep.ordinal
            val label = state.t(step.englishLabel, step.germanLabel)
            val currentSuffix = if (isCurrent) state.t(", current", ", aktuell") else ""
            Surface(
                onClick = { onStepSelected(step) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = state.t(
                            "Step ${step.ordinal + 1} of ${CreationStep.entries.size}: $label$currentSuffix",
                            "Schritt ${step.ordinal + 1} von ${CreationStep.entries.size}: $label$currentSuffix",
                        )
                        role = Role.Button
                        selected = isCurrent
                    },
                color = androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (step != CreationStep.Rules) {
                        Surface(
                            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.5f).height(2.dp),
                            color = if (step.ordinal <= currentStep.ordinal) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ) {}
                    }
                    if (step != CreationStep.Review) {
                        Surface(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.5f).height(2.dp),
                            color = if (step.ordinal < currentStep.ordinal) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ) {}
                    }
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isEarlier -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = when {
                            isCurrent -> MaterialTheme.colorScheme.onPrimary
                            isEarlier -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("${step.ordinal + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIntro(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RulesetStep(state: DndAppState) {
    val draft = state.creation
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepIntro(
            state.t("Choose your rules", "Wähle deine Regeln"),
            state.t("Your choice controls every calculation, option and explanation for this character.", "Deine Wahl steuert alle Berechnungen, Optionen und Erklärungen für diesen Charakter."),
        )
        Ruleset.entries.forEach { ruleset ->
            OutlinedCard(
                onClick = {
                    state.selectCreationRuleset(ruleset)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (draft.ruleset == ruleset) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ruleset.longLabel, style = MaterialTheme.typography.titleMedium)
                            if (ruleset == Ruleset.Fifth2024) RulesetBadge(ruleset)
                        }
                        Text(
                            when (ruleset) {
                                Ruleset.Fifth2024 -> state.t("Recommended default · streamlined current rules", "Empfohlener Standard · aktuelle, vereinfachte Regeln")
                                Ruleset.Fifth2014 -> state.t("For tables using the original fifth-edition rules", "Für Runden mit den ursprünglichen Regeln der fünften Edition")
                                Ruleset.Pf2eRemaster -> state.t("Tactical three-action fantasy rules", "Taktische Fantasy-Regeln mit drei Aktionen")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (draft.ruleset == ruleset) Icon(Icons.Rounded.Check, contentDescription = state.t("Selected", "Ausgewählt"), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        ExplanationCard(
            state.t("You can change systems later", "Du kannst das System später wechseln"),
            state.t("Conversion always creates a separate copy. Your original stays untouched.", "Eine Konvertierung erstellt immer eine separate Kopie. Das Original bleibt unverändert."),
        )
    }
}

@Composable
private fun IdentityStep(
    state: DndAppState,
    onPickPortrait: (PortraitPickTarget) -> Unit,
    onEditPortrait: (PortraitPickTarget) -> Unit,
) {
    val draft = state.creation
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StepIntro(state.t("Meet your hero", "Lerne deine Heldin kennen"), state.t("Start with a name and a face. You can change both at any time.", "Beginne mit einem Namen und einem Gesicht. Beides lässt sich jederzeit ändern."))
        CharacterPortrait(
            draft.name.ifBlank { "?" },
            draft.name.hashCode(),
            Modifier.size(112.dp),
            draft.portraitBytes,
            onClick = draft.portraitBytes?.let { { onEditPortrait(PortraitPickTarget.Creation) } },
            clickLabel = state.t("Edit portrait crop", "Porträtzuschnitt bearbeiten"),
        )
        OutlinedButton(onClick = { onPickPortrait(PortraitPickTarget.Creation) }, modifier = Modifier.height(48.dp)) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (draft.portraitBytes == null) state.t("Choose picture", "Bild wählen")
                else state.t("Choose another", "Anderes wählen")
            )
        }
        OutlinedTextField(
            value = draft.name,
            onValueChange = { draft.name = it.take(40) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(state.t("Character name", "Charaktername")) },
            supportingText = { Text(state.t("This is the only required field on this step.", "Dies ist das einzige Pflichtfeld in diesem Schritt.")) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = draft.characterDescription,
            onValueChange = { draft.characterDescription = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(state.t("Character", "Charakter")) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = draft.motive,
            onValueChange = { draft.motive = it.take(500) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(state.t("Motive", "Motiv")) },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(16.dp),
        )
        AlignmentWheel(
            state = state,
            value = draft.alignment,
            onValueChange = { draft.alignment = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BuildStep(state: DndAppState) {
    val draft = state.creation
    var subclassPickerOpen by remember(draft.ruleset, draft.className) { mutableStateOf(false) }
    val ancestries = state.creationAncestryOptions().sortedForPicker(state.language, { it })
    val classes = state.creationClassOptions().sortedForPicker(state.language, { it })
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        StepIntro(state.t("What are they like?", "Wie sind sie?"), state.t("Any valid combination works. Recommendations explain fit; they never block your choice.", "Jede gültige Kombination funktioniert. Empfehlungen erklären die Passung, blockieren aber nie deine Wahl."))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft.useRecommendations, onCheckedChange = { draft.useRecommendations = it })
            Column {
                Text(state.t("Show beginner recommendations", "Empfehlungen für Einsteiger:innen zeigen"), style = MaterialTheme.typography.titleSmall)
                Text(state.t("Based on playstyle and complexity", "Basierend auf Spielstil und Komplexität"), style = MaterialTheme.typography.bodySmall)
            }
        }
        SelectionHeading(state.t("Ancestry / species", "Abstammung / Spezies"), draft.ancestry)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ancestries) { ancestry ->
                FilterChip(selected = draft.ancestry == ancestry, onClick = { draft.ancestry = ancestry }, label = { Text(ancestry) })
            }
        }
        if (draft.useRecommendations && draft.className == "Fighter") {
            ExplanationCard(
                state.t("A clear first adventure", "Ein klarer Einstieg"),
                state.t("Fighter is sturdy and direct, while still offering tactical choices as you level.", "Kämpfer:innen sind robust und direkt, bieten beim Aufstieg aber trotzdem taktische Möglichkeiten."),
            )
        }
        SelectionHeading(state.t("Class", "Klasse"), draft.className)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(classes) { className ->
                FilterChip(selected = draft.className == className, onClick = { state.selectCreationClass(className) }, label = { Text(className) })
            }
        }
        if (draft.ruleset != Ruleset.Pf2eRemaster) {
            SelectionHeading(
                state.t("Subclass", "Unterklasse"),
                draft.subclassName.ifBlank { state.t("Choose", "W\u00e4hlen") },
            )
            OutlinedCard(
                onClick = { subclassPickerOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            draft.subclassName.ifBlank { state.t("Choose a subclass", "Unterklasse w\u00e4hlen") },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val supporting = state.creationSubclassAdvisory()
                            ?: if (state.creationSubclassRequired()) state.t("Required at this level", "Auf dieser Stufe erforderlich")
                            else state.t("Optional until its normal selection level", "Bis zur regul\u00e4ren Wahlstufe optional")
                        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
            if (subclassPickerOpen) {
                SubclassPickerDialog(state = state, onDismiss = { subclassPickerOpen = false })
            }
        }
        TextButton(onClick = {
            state.showInfo(
                state.t("Missing an option?", "Fehlt eine Option?"),
                state.t(
                    "The built-in open rules are shown here. Approved private content and the custom subclass builder can add more options.",
                    "Hier werden die integrierten offenen Regeln angezeigt. Freigegebene private Inhalte und der Unterklassen-Editor k\u00f6nnen weitere Optionen hinzuf\u00fcgen.",
                ),
            )
        }) {
            Icon(Icons.Rounded.Info, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(state.t("About available content", "Über verfügbare Inhalte"))
        }
    }
}

@Composable
private fun SelectionHeading(label: String, selection: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(
            selection,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LevelAndStatsStep(state: DndAppState) {
    val draft = state.creation
    val methods = if (draft.ruleset == Ruleset.Pf2eRemaster) {
        listOf(StatMethod.StandardArray, StatMethod.Manual)
    } else {
        listOf(StatMethod.Rolled, StatMethod.StandardArray, StatMethod.PointBuy, StatMethod.Manual)
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        StepIntro(state.t("Choose your experience", "Wähle deine Erfahrung"), state.t("Start at any level. We will walk through every required choice in order.", "Starte auf jeder Stufe. Wir führen dich der Reihe nach durch jede nötige Wahl."))
        Text(state.t("Level ${draft.level}", "Stufe ${draft.level}"), style = MaterialTheme.typography.headlineMedium)
        Slider(
            value = draft.level.toFloat(),
            onValueChange = { state.setCreationLevel(it.roundToInt()) },
            valueRange = 1f..20f,
            steps = 18,
        )
        Text(state.t("Ability scores", "Attributswerte"), style = MaterialTheme.typography.titleMedium)
        methods.forEach { method ->
            OutlinedCard(
                onClick = {
                    draft.statMethod = method
                    if (method == StatMethod.Rolled && draft.rolledScores.isEmpty()) state.rollCreationAbilityScores()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = if (draft.statMethod == method) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface),
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (method == StatMethod.Rolled) Icon(Icons.Rounded.Casino, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    else Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (method) {
                                StatMethod.Rolled -> state.t("Roll for me · default", "Für mich würfeln · Standard")
                                StatMethod.StandardArray -> if (draft.ruleset == Ruleset.Pf2eRemaster) state.t("Recommended boosts", "Empfohlene Attributsboosts") else state.t("Standard array", "Standardwerte")
                                StatMethod.PointBuy -> state.t("Point buy", "Punktekauf")
                                StatMethod.Manual -> state.t("Enter manually", "Manuell eingeben")
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            when (method) {
                                StatMethod.Rolled -> state.t("Six sets of 4d6, drop the lowest; class-aware assignment", "Sechsmal 4W6, niedrigsten streichen; klassenbewusste Verteilung")
                                StatMethod.StandardArray -> if (draft.ruleset == Ruleset.Pf2eRemaster) "18, 16, 14, 12, 10, 8" else "15, 14, 13, 12, 10, 8"
                                StatMethod.PointBuy -> state.t("Spend 27 points with live guidance", "27 Punkte mit direkter Hilfe verteilen")
                                StatMethod.Manual -> state.t("For values rolled at your table", "Für am Spieltisch gewürfelte Werte")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (draft.statMethod == method) Icon(Icons.Rounded.Check, contentDescription = state.t("Selected", "Ausgewählt"))
                }
            }
        }
        when (draft.statMethod) {
            StatMethod.Rolled -> {
                OutlinedButton(
                    onClick = state::rollCreationAbilityScores,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Rounded.Casino, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (draft.rolledScores.isEmpty()) state.t("Roll the six scores", "Sechs Werte würfeln") else state.t("Reroll all six", "Alle sechs neu würfeln"))
                }
                if (draft.rolledScores.isNotEmpty()) NumberChipPreview(draft.rolledScores)
            }
            StatMethod.StandardArray -> NumberChipPreview(if (draft.ruleset == Ruleset.Pf2eRemaster) listOf(18, 16, 14, 12, 10, 8) else listOf(15, 14, 13, 12, 10, 8))
            StatMethod.PointBuy -> NumberChipPreview(listOf(15, 15, 14, 10, 8, 8))
            StatMethod.Manual -> Unit
        }
        AbilityBreakdownList(state, editable = draft.statMethod == StatMethod.Manual)

        Text(state.t("Hit Points", "Trefferpunkte"), style = MaterialTheme.typography.titleMedium)
        if (draft.ruleset == Ruleset.Pf2eRemaster) {
            Text(state.t("Ancestry HP is added once; class HP plus Constitution is added at every level.", "Abstammungs-TP werden einmal addiert; Klassen-TP plus Konstitution auf jeder Stufe."), style = MaterialTheme.typography.bodySmall)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HpMethod.entries) { method ->
                    FilterChip(
                        selected = draft.hpMethod == method,
                        onClick = { draft.hpMethod = method },
                        label = {
                            Text(
                                when (method) {
                                    HpMethod.Fixed -> state.t("Fixed", "Fest")
                                    HpMethod.Rolled -> state.t("Roll", "Würfeln")
                                    HpMethod.Manual -> state.t("Manual", "Manuell")
                                },
                            )
                        },
                    )
                }
            }
            when (draft.hpMethod) {
            HpMethod.Fixed -> Text(state.t("The class fixed value is used at each additional level.", "Für jede weitere Stufe wird der feste Klassenwert verwendet."), style = MaterialTheme.typography.bodySmall)
            HpMethod.Rolled -> {
                OutlinedButton(onClick = state::rollCreationHitPoints, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Casino, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(state.t("Roll ${draft.level - 1} Hit Dice", "${draft.level - 1} Trefferwürfel würfeln"))
                }
                if (draft.rolledHpGains.isNotEmpty()) {
                    NumberChipPreview(draft.rolledHpGains)
                    Text(
                        state.t("Dice total: ${draft.rolledHpGains.sum()}", "Würfelsumme: ${draft.rolledHpGains.sum()}"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HpMethod.Manual -> {
                Text(state.t("HP gained per additional level: ${draft.manualHpGain}", "TP pro weiterer Stufe: ${draft.manualHpGain}"), style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = draft.manualHpGain.toFloat(),
                    onValueChange = { draft.manualHpGain = it.roundToInt() },
                    valueRange = 1f..30f,
                    steps = 28,
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumberChipPreview(scores: List<Int>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        scores.forEach { score ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    score.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityBreakdownList(state: DndAppState, editable: Boolean) {
    val breakdowns = state.creationAbilityBreakdowns()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            if (editable) state.t("Enter each base score", "Jeden Basiswert eingeben")
            else state.t("Assigned scores", "Verteilte Werte"),
            style = MaterialTheme.typography.titleSmall,
        )
        breakdowns.forEach { breakdown ->
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val abilityInfo = state.creationAbilityInfo(breakdown.ability)
                    TextButton(
                        onClick = { state.showInfo(abilityInfo.title, abilityInfo.body) },
                        modifier = Modifier.width(54.dp).semantics {
                            contentDescription = state.t("Explain ${abilityInfo.title}", "${abilityInfo.title} erklären")
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(breakdown.ability, fontWeight = FontWeight.Bold)
                    }
                    if (editable) {
                        Slider(
                            value = breakdown.baseScore.toFloat(),
                            onValueChange = { state.creation.manualAbilities[breakdown.ability] = it.roundToInt() },
                            valueRange = 1f..20f,
                            steps = 18,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Text(
                        if (breakdown.ancestrySource == null) breakdown.finalScore.toString()
                        else "${breakdown.baseScore} → ${breakdown.finalScore}",
                        modifier = Modifier.width(72.dp),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(start = 54.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AssistChip(
                        onClick = {
                            val info = state.creationModifierInfo(breakdown.ability)
                            state.showInfo(info.title, info.body)
                        },
                        label = { Text(state.t("Modifier ${creationSigned(breakdown.modifier)}", "Modifikator ${creationSigned(breakdown.modifier)}")) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    breakdown.ancestrySource?.let { source ->
                        AssistChip(
                            onClick = {
                                state.creationAncestryInfo(breakdown.ability)?.let { info -> state.showInfo(info.title, info.body) }
                            },
                            label = { Text("${source.label} ${creationSigned(source.amount)}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }
            }
        }
        state.creationAncestryBonusAdvisory()?.let { advisory ->
            Text(advisory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.creationAbilityLimitAdvisory()?.let { advisory ->
            Text(advisory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun creationSigned(value: Int): String = if (value >= 0) "+$value" else value.toString()

@Composable
private fun DetailsStep(
    state: DndAppState,
    spellSelection: CreationSpellSelectionUi?,
    viewport: Rect?,
    onSpellSectionVisibilityChanged: (Boolean) -> Unit,
) {
    val draft = state.creation
    val preview = state.creationPreview()
    val featLevels = if (draft.className == "Fighter") listOf(4, 6, 8, 12, 14, 16, 19) else listOf(4, 8, 12, 16, 19)
    val featChoices = featLevels.count { it <= draft.level }
    val featOptions = state.creationFeatOptions().sortedForPicker(state.language, FeatOptionUi::name, FeatOptionUi::id)
    val backgrounds = state.creationBackgroundOptions().sortedForPicker(state.language, { it.name(state.language) }, BackgroundDefinitionUi::id)
    val background = state.selectedCreationBackground()
    val rankSkillOptions = state.creationRankSkillOptions()
    val filteredBackgrounds = filterCreationBackgrounds(backgrounds, draft.classSkillIds.toSet(), draft.backgroundId)
    var languageText by remember(draft.ruleset) { mutableStateOf(draft.languages.joinToString(", ")) }
    var backgroundListOpen by remember(draft.ruleset, draft.className) { mutableStateOf(false) }
    var classSkillLimitPulse by remember(draft.ruleset, draft.className) { mutableIntStateOf(0) }
    val suggestedLanguages = languageSuggestions(languageText, state.creationLanguageOptions())
    LaunchedEffect(spellSelection) {
        if (spellSelection == null) onSpellSectionVisibilityChanged(false)
    }
    DisposableEffect(Unit) {
        onDispose { onSpellSectionVisibilityChanged(false) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        StepIntro(state.t("We did the rules work", "Wir haben die Regelarbeit erledigt"), state.t("Here is the recommended foundation. Every value will have a tappable explanation on your sheet.", "Hier ist die empfohlene Grundlage. Jeder Wert erhält auf deinem Bogen eine antippbare Erklärung."))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReviewRow(state.t("Primary ability", "Hauptattribut"), "${preview.primaryAbility} · ${preview.primaryScore}")
                ReviewRow(state.t("Hit points", "Trefferpunkte"), preview.hitPoints.toString())
                ReviewRow(state.t("Armor Class", "Rüstungsklasse"), preview.armorClass.toString())
                ReviewRow(state.t("Languages", "Sprachen"), draft.languages.joinToString(", ").ifBlank { state.t("None", "Keine") })
            }
        }
        state.creationArmorAdvisory()?.let { advisory ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                Text(advisory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        val classDefinition = ProficiencyCatalog.classDefinition(draft.ruleset, draft.className)
        val classSkillCount = state.creationClassSkillCount()
        PulsingSelectionCount(
            text = state.t(
                "Class skills · ${draft.classSkillIds.size}/$classSkillCount",
                "Klassenfertigkeiten · ${draft.classSkillIds.size}/$classSkillCount",
            ),
            pulseSignal = classSkillLimitPulse,
        )
        classDefinition.requiredOneOfSkillGroups.forEach { group ->
            val names = group.mapNotNull { id -> rankSkillOptions.firstOrNull { it.id == id }?.name }
            Text(
                state.t("Include one of: ", "Wähle mindestens eine davon: ") + names.joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        classDefinition.fixedSkillIds.mapNotNull { id -> rankSkillOptions.firstOrNull { it.id == id } }.forEach { skill ->
            CreationSkillChoiceCard(
                state = state,
                skill = skill,
                selected = true,
                description = creationSkillDescription(state, skill.id),
                onClick = { showCreationSkillInfo(state, skill) },
            )
        }
        state.creationClassSkillOptions().forEach { skill ->
            val selected = skill.id in draft.classSkillIds
            CreationSkillChoiceCard(
                state = state,
                skill = skill,
                selected = selected,
                description = creationSkillDescription(state, skill.id),
                onClick = {
                    if (shouldPulseClassSkillLimit(selected, draft.classSkillIds.size, classSkillCount)) {
                        classSkillLimitPulse += 1
                    } else {
                        state.toggleCreationClassSkill(skill.id)
                    }
                },
            )
        }
        BackgroundSelector(
            state = state,
            selected = background,
            options = filteredBackgrounds,
            rankSkillOptions = rankSkillOptions,
            expanded = backgroundListOpen,
            onExpandedChange = { backgroundListOpen = it },
            onSelected = {
                state.selectCreationBackground(it)
                backgroundListOpen = false
            },
        )
        background?.takeIf { it.customSkillCount > 0 }?.let { selected ->
            Text(
                state.t(
                    "Background skills · ${draft.backgroundSkillIds.size}/${selected.customSkillCount}",
                    "Hintergrundfertigkeiten · ${draft.backgroundSkillIds.size}/${selected.customSkillCount}",
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            rankSkillOptions
                .filterNot { it.id in draft.classSkillIds || it.id in classDefinition.fixedSkillIds }
                .forEach { skill ->
                    CreationSkillChoiceCard(
                        state = state,
                        skill = skill,
                        selected = skill.id in draft.backgroundSkillIds,
                        description = creationSkillDescription(state, skill.id),
                        onClick = { state.toggleCreationBackgroundSkill(skill.id) },
                    )
                }
        }
        if (state.creationSkillIncreaseCount() > 0) {
            Text(
                state.t(
                    "Skill increases · ${state.creationSkillIncreaseCost()}/${state.creationSkillIncreaseCount()}",
                    "Fertigkeitssteigerungen · ${state.creationSkillIncreaseCost()}/${state.creationSkillIncreaseCount()}",
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            state.creationRankSkillOptions().forEach { skill ->
                CreationSkillChoiceCard(
                    state = state,
                    skill = skill,
                    selected = skill.id in draft.skillRankChoices,
                    rankControl = true,
                    onClick = { state.cycleCreationSkillRank(skill.id) },
                )
            }
        }
        draft.proficiencyAdvisory?.let { advisory ->
            Text(advisory, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        OutlinedTextField(
            value = languageText,
            onValueChange = { value ->
                languageText = value
                draft.languages.clear()
                draft.languages.addAll(value.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() })
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(state.t("Languages", "Sprachen")) },
            supportingText = { Text(state.t("Separate languages with commas.", "Sprachen mit Kommas trennen.")) },
        )
        if (suggestedLanguages.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    suggestedLanguages.forEach { suggestion ->
                        Surface(
                            onClick = {
                                languageText = replaceActiveLanguageSegment(languageText, suggestion)
                                draft.languages.clear()
                                draft.languages.addAll(languageText.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() })
                            },
                            color = androidx.compose.ui.graphics.Color.Transparent,
                        ) {
                            Text(suggestion, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        if (draft.ruleset != Ruleset.Pf2eRemaster && featChoices > 0) {
            Text(state.t("Feat choices · ${draft.selectedFeatIds.size}/$featChoices", "Talentwahlen · ${draft.selectedFeatIds.size}/$featChoices"), style = MaterialTheme.typography.titleMedium)
            featOptions.forEach { feat ->
                FeatSelectionCard(
                    state = state,
                    feat = feat,
                    selected = feat.id in draft.selectedFeatIds,
                    onClick = {
                        if (feat.id in draft.selectedFeatIds) {
                            draft.selectedFeatIds.remove(feat.id)
                            if (feat.id == "skilled") draft.featSkillIds.clear()
                        } else if (draft.selectedFeatIds.size < featChoices) {
                            draft.selectedFeatIds += feat.id
                        }
                    },
                )
            }
        }
        if (state.creationFeatSkillCount() > 0) {
            Text(
                state.t(
                    "Skilled feat · ${draft.featSkillIds.size}/${state.creationFeatSkillCount()}",
                    "Talent Geübt · ${draft.featSkillIds.size}/${state.creationFeatSkillCount()}",
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            state.creationFeatSkillOptions().forEach { skill ->
                CreationSkillChoiceCard(
                    state = state,
                    skill = skill,
                    selected = skill.id in draft.featSkillIds,
                    onClick = { state.toggleCreationFeatSkill(skill.id) },
                )
            }
        }
        if (draft.ruleset == Ruleset.Pf2eRemaster && featOptions.isNotEmpty()) {
            Text(state.t("Approved private feats", "Freigegebene private Talente"), style = MaterialTheme.typography.titleMedium)
            featOptions.forEach { feat ->
                FeatSelectionCard(
                    state = state,
                    feat = feat,
                    selected = feat.id in draft.selectedFeatIds,
                    onClick = { if (feat.id in draft.selectedFeatIds) draft.selectedFeatIds.remove(feat.id) else draft.selectedFeatIds += feat.id },
                )
            }
        }
        spellSelection?.let { selection ->
            CreationSpellSelectionSection(
                state = state,
                selection = selection,
                viewport = viewport,
                onVisibilityChanged = onSpellSectionVisibilityChanged,
            )
        }
        ExplanationCard(
            state.t("Nothing gets silently removed", "Nichts wird unbemerkt entfernt"),
            state.t("If an earlier choice changes, we show everything that needs review before saving.", "Wenn sich eine frühere Wahl ändert, zeigen wir vor dem Speichern alles, was geprüft werden muss."),
        )
    }
}

internal fun groupCreationSpells(spells: List<SpellUi>): List<Pair<Int, List<SpellUi>>> = spells
    .distinctBy { it.id }
    .groupBy { it.level }
    .toSortedMap()
    .map { (level, entries) -> level to entries.sortedWith(compareBy<SpellUi> { it.name.lowercase() }.thenBy { it.id }) }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreationSpellSelectionSection(
    state: DndAppState,
    selection: CreationSpellSelectionUi,
    viewport: Rect?,
    onVisibilityChanged: (Boolean) -> Unit,
) {
    val selectedSpells = (selection.fixed + selection.selected).distinctBy(SpellUi::id)
    val fixedIds = selection.fixed.mapTo(hashSetOf(), SpellUi::id)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val section = coordinates.boundsInWindow()
                val visible = viewport?.let { section.bottom > it.top && section.top < it.bottom } == true
                onVisibilityChanged(visible)
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(state.t("Spells", "Zauber"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(
                selectedSpells.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (selection.cantripLimit > 0) {
                SpellCountChip(
                    state.t("Cantrips", "Zaubertricks"),
                    selection.selected.count { it.level == 0 },
                    selection.cantripLimit,
                )
            }
            if (selection.leveledSpellLimit > 0) {
                SpellCountChip(
                    selection.leveledLabel,
                    selection.selected.count { it.level > 0 },
                    selection.leveledSpellLimit,
                )
            }
            selection.preparedLimit?.let { limit ->
                SpellLabelChip(state.t("Prepare $limit", "$limit vorbereiten"))
            }
        }
        if (selectedSpells.isEmpty()) {
            Text(
                state.t("No spells selected yet.", "Noch keine Zauber ausgewählt."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            groupCreationSpells(selectedSpells).forEach { (level, spells) ->
                Text(spellLevelLabel(state, level), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                spells.forEach { spell ->
                    SelectedCreationSpellCard(
                        state = state,
                        spell = spell,
                        fixed = spell.id in fixedIds,
                        onConfirmedRemove = { state.removeCreationSpell(spell.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellCountChip(label: String, selected: Int, limit: Int) {
    SpellLabelChip("$label $selected/$limit")
}

@Composable
private fun SpellLabelChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedCreationSpellCard(
    state: DndAppState,
    spell: SpellUi,
    fixed: Boolean,
    onConfirmedRemove: () -> Unit,
) {
    var confirmationOpen by remember(spell.id) { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.StartToEnd && !fixed) confirmationOpen = true
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !fixed,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(state.t("Remove", "Entfernen"), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) {
        SpellSummaryCard(
            state = state,
            spell = spell,
            fixed = fixed,
            modifier = Modifier.semantics {
                if (!fixed) {
                    customActions = listOf(
                        CustomAccessibilityAction(state.t("Remove ${spell.name}", "${spell.name} entfernen")) {
                            confirmationOpen = true
                            true
                        },
                    )
                }
            },
        )
    }
    if (confirmationOpen) {
        AlertDialog(
            onDismissRequest = { confirmationOpen = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            title = { Text(state.t("Remove ${spell.name}?", "${spell.name} entfernen?")) },
            text = { Text(state.t("This removes the spell from this character.", "Dieser Zauber wird von diesem Charakter entfernt.")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmationOpen = false
                    onConfirmedRemove()
                }) { Text(state.t("Remove", "Entfernen")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmationOpen = false }) { Text(state.t("Cancel", "Abbrechen")) }
            },
        )
    }
}

@Composable
private fun SpellSummaryCard(
    state: DndAppState,
    spell: SpellUi,
    fixed: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(spell.name, style = MaterialTheme.typography.titleSmall)
                    if (fixed) {
                        Text(
                            state.t("Granted", "Gewährt"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    "${spellLevelLabel(state, spell.level)} · ${state.t("Range", "Reichweite")}: ${spellRangeLabel(state, spell)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    state.t("Damage / utility", "Schaden / Nutzen") + ": " + spellShortEffect(state, spell),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { showCreationSpellInfo(state, spell) }) {
                Icon(Icons.Rounded.Info, contentDescription = state.t("About ${spell.name}", "Info zu ${spell.name}"))
            }
        }
    }
}

@Composable
private fun CreationSpellPicker(
    state: DndAppState,
    selection: CreationSpellSelectionUi,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val selectedIds = selection.selected.mapTo(hashSetOf(), SpellUi::id)
    val filtered = remember(search, selection.options) {
        val needle = search.trim()
        selection.options.filter { spell ->
            needle.isBlank() || spell.name.contains(needle, true) || spell.summary.contains(needle, true) ||
                spell.rulesText.effect.contains(needle, true) || spell.level.toString() == needle
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Surface(shadowElevation = 5.dp, tonalElevation = 2.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(state.t("Add spells", "Zauber hinzufügen"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                            TextButton(onClick = onDismiss) { Text(state.t("Done", "Fertig")) }
                        }
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            label = { Text(state.t("Search spells", "Zauber suchen")) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (selection.cantripLimit > 0) {
                                Text(
                                    state.t("Cantrips", "Zaubertricks") + " ${selection.selected.count { it.level == 0 }}/${selection.cantripLimit}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            if (selection.leveledSpellLimit > 0) {
                                Text(
                                    "${selection.leveledLabel} ${selection.selected.count { it.level > 0 }}/${selection.leveledSpellLimit}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                state.t("No matching spells.", "Keine passenden Zauber."),
                                modifier = Modifier.padding(vertical = 20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    groupCreationSpells(filtered).forEach { (level, spells) ->
                        item(key = "level-$level") {
                            Text(
                                spellLevelLabel(state, level),
                                modifier = Modifier.padding(top = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        items(spells, key = SpellUi::id) { spell ->
                            SpellPickerOptionCard(
                                state = state,
                                spell = spell,
                                selected = spell.id in selectedIds,
                                onToggle = { state.toggleCreationSpell(spell.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpellPickerOptionCard(
    state: DndAppState,
    spell: SpellUi,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    OutlinedCard(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = selected, onCheckedChange = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(spell.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    state.t("Range", "Reichweite") + ": ${spellRangeLabel(state, spell)} · " + spellShortEffect(state, spell),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { showCreationSpellInfo(state, spell) }) {
                Icon(Icons.Rounded.Info, contentDescription = state.t("About ${spell.name}", "Info zu ${spell.name}"))
            }
        }
    }
}

private fun spellLevelLabel(state: DndAppState, level: Int): String = if (level == 0) {
    state.t("Cantrips", "Zaubertricks")
} else if (state.creation.ruleset == Ruleset.Pf2eRemaster) {
    state.t("Rank $level", "Rang $level")
} else {
    state.t("Level $level", "Grad $level")
}

private fun spellRangeLabel(state: DndAppState, spell: SpellUi): String {
    spell.rulesText.range.takeIf(String::isNotBlank)?.let { return it }
    val range = Regex("(?i)\\b(self|touch|\\d+\\s*(?:ft|feet|foot|m|meter))\\b").find(spell.summary)?.value
    return range ?: state.t("Range not listed", "Reichweite nicht angegeben")
}

private fun spellShortEffect(state: DndAppState, spell: SpellUi): String {
    val effect = spell.castPreviews[spell.level.coerceAtLeast(1)]
        ?: spell.summary.takeIf(String::isNotBlank)
        ?: spell.rulesText.effect.takeIf(String::isNotBlank)
        ?: state.t("Utility spell", "Nutzzauber")
    val firstLine = effect.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val firstSentence = firstLine.substringBefore(". ").trim()
    return firstSentence.take(180).ifBlank { state.t("Utility spell", "Nutzzauber") }
}

private fun showCreationSpellInfo(state: DndAppState, spell: SpellUi) {
    val body = buildList {
        add("${spellLevelLabel(state, spell.level)} · ${state.t("Range", "Reichweite")}: ${spellRangeLabel(state, spell)}")
        add(state.t("Damage / utility", "Schaden / Nutzen") + ": " + spellShortEffect(state, spell))
        spell.rulesText.duration.takeIf(String::isNotBlank)?.let { add(state.t("Duration: $it", "Dauer: $it")) }
    }.joinToString("\n\n")
    state.showInfo(spell.name, body, spell.activationCost.toCostTokens())
}

internal fun shouldPulseClassSkillLimit(
    alreadySelected: Boolean,
    selectedCount: Int,
    selectionLimit: Int,
): Boolean = !alreadySelected && selectedCount >= selectionLimit

internal fun filterCreationBackgrounds(
    backgrounds: List<BackgroundDefinitionUi>,
    selectedClassSkillIds: Set<String>,
    selectedBackgroundId: String?,
): List<BackgroundDefinitionUi> = backgrounds.filter { background ->
    background.id == selectedBackgroundId || background.allGrantedSkillIds.none { it in selectedClassSkillIds }
}

@Composable
private fun PulsingSelectionCount(text: String, pulseSignal: Int) {
    var pulsing by remember { mutableStateOf(false) }
    LaunchedEffect(pulseSignal) {
        if (pulseSignal == 0) return@LaunchedEffect
        pulsing = true
        delay(170)
        pulsing = false
    }
    val color by animateColorAsState(
        targetValue = if (pulsing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(130),
    )
    val scale by animateFloatAsState(
        targetValue = if (pulsing) 1.1f else 1f,
        animationSpec = tween(130),
    )
    Text(
        text,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        style = MaterialTheme.typography.titleMedium,
        color = color,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundSelector(
    state: DndAppState,
    selected: BackgroundDefinitionUi?,
    options: List<BackgroundDefinitionUi>,
    rankSkillOptions: List<CreationSkillOptionUi>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (String) -> Unit,
) {
    Text(state.t("Background", "Hintergrund"), style = MaterialTheme.typography.titleMedium)
    OutlinedCard(
        onClick = { onExpandedChange(!expanded) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected == null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f),
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                selected?.name(state.language) ?: state.t("Choose a background", "Hintergrund wählen"),
                style = MaterialTheme.typography.titleMedium,
            )
            selected?.let { background ->
                BackgroundSkillChips(
                    state = state,
                    background = background,
                    rankSkillOptions = rankSkillOptions,
                    additionalSkillIds = state.creation.backgroundSkillIds.toSet(),
                )
            }
            Text(
                if (expanded) state.t("Close list", "Liste schließen") else state.t("Open background list", "Hintergrundliste öffnen"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (expanded) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)),
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (options.isEmpty()) {
                    Text(
                        state.t("No background fits the selected class skills.", "Kein Hintergrund passt zu den gewählten Klassenfertigkeiten."),
                        modifier = Modifier.padding(6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                options.forEach { option ->
                    OutlinedCard(
                        onClick = { onSelected(option.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (selected?.id == option.id) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(option.name(state.language), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                if (selected?.id == option.id) {
                                    Icon(Icons.Rounded.Check, contentDescription = state.t("Selected", "Ausgewählt"), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            BackgroundSkillChips(state, option, rankSkillOptions)
                            if (option.customSkillCount > 0) {
                                Text(
                                    state.t("Choose ${option.customSkillCount} skills", "${option.customSkillCount} Fertigkeiten wählen"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundSkillChips(
    state: DndAppState,
    background: BackgroundDefinitionUi,
    rankSkillOptions: List<CreationSkillOptionUi>,
    additionalSkillIds: Set<String> = emptySet(),
) {
    val skillIds = background.allGrantedSkillIds + additionalSkillIds
    if (skillIds.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        skillIds.forEach { skillId ->
            val known = rankSkillOptions.firstOrNull { it.id == skillId }
            val definition = ProficiencyCatalog.skill(state.creation.ruleset, skillId, background.id)
            val name = known?.name ?: definition?.name(state.language) ?: skillId.substringAfterLast(':')
            AssistChip(
                onClick = {
                    state.showInfo(
                        name,
                        buildString {
                            append(definition?.ability ?: known?.ability.orEmpty())
                            append("\n\n")
                            append(creationSkillDescription(state, skillId))
                        },
                    )
                },
                label = { Text(name) },
                leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
        }
    }
}

private fun showCreationSkillInfo(state: DndAppState, skill: CreationSkillOptionUi) {
    state.showInfo(skill.name, "${skill.ability}\n\n${creationSkillDescription(state, skill.id)}")
}

internal fun creationSkillDescription(state: DndAppState, skillId: String): String = when (skillId.substringAfter("skill:")) {
    "acrobatics" -> state.t("Balance, tumble, and stay on your feet.", "Balancieren, abrollen und auf den Beinen bleiben.")
    "animal-handling" -> state.t("Calm, guide, or understand an animal.", "Tiere beruhigen, führen oder verstehen.")
    "arcana" -> state.t("Recall lore about magic, spells, and planes.", "Wissen über Magie, Zauber und Ebenen abrufen.")
    "athletics" -> state.t("Climb, jump, swim, grapple, or shove.", "Klettern, springen, schwimmen, ringen oder stoßen.")
    "crafting" -> state.t("Create, repair, and identify crafted objects.", "Gegenstände herstellen, reparieren und identifizieren.")
    "deception" -> state.t("Mislead someone through words or disguise.", "Andere durch Worte oder Verkleidung täuschen.")
    "diplomacy", "persuasion" -> state.t("Influence others through reason and goodwill.", "Andere durch Vernunft und guten Willen beeinflussen.")
    "history" -> state.t("Recall people, places, events, and cultures of the past.", "Personen, Orte, Ereignisse und Kulturen der Vergangenheit kennen.")
    "insight" -> state.t("Read motives, emotions, and hidden intent.", "Motive, Gefühle und verborgene Absichten erkennen.")
    "intimidation" -> state.t("Pressure someone with threats or presence.", "Andere durch Drohungen oder Auftreten unter Druck setzen.")
    "investigation" -> state.t("Find clues and reason out how they connect.", "Hinweise finden und ihre Zusammenhänge erschließen.")
    "medicine" -> state.t("Assess injuries, stabilize, and provide treatment.", "Verletzungen beurteilen, stabilisieren und behandeln.")
    "nature" -> state.t("Recall lore about terrain, plants, animals, and weather.", "Wissen über Gelände, Pflanzen, Tiere und Wetter abrufen.")
    "occultism" -> state.t("Recall lore about occult magic and strange creatures.", "Wissen über okkulte Magie und fremdartige Wesen abrufen.")
    "perception" -> state.t("Notice creatures, objects, and subtle changes.", "Wesen, Gegenstände und feine Veränderungen bemerken.")
    "performance" -> state.t("Entertain or impress through a practiced art.", "Durch eine geübte Kunst unterhalten oder beeindrucken.")
    "religion" -> state.t("Recall lore about faiths, deities, and divine magic.", "Wissen über Glauben, Gottheiten und göttliche Magie abrufen.")
    "sleight-of-hand", "thievery" -> state.t("Handle small objects unnoticed and defeat simple mechanisms.", "Kleine Gegenstände unbemerkt handhaben und Mechanismen überwinden.")
    "society" -> state.t("Recall customs, institutions, and local history.", "Bräuche, Institutionen und lokale Geschichte kennen.")
    "stealth" -> state.t("Hide and move without being noticed.", "Verbergen und unbemerkt bewegen.")
    "survival" -> state.t("Navigate, track, forage, and endure the wilds.", "Navigieren, Spuren lesen, Nahrung finden und die Wildnis überstehen.")
    else -> if (skillId.startsWith("skill:lore:")) {
        state.t("Recall specialized knowledge about this subject.", "Spezialwissen zu diesem Thema abrufen.")
    } else {
        state.t("Apply this skill when its training is relevant.", "Diese Fertigkeit einsetzen, wenn ihre Übung relevant ist.")
    }
}

@Composable
private fun CreationSkillChoiceCard(
    state: DndAppState,
    skill: CreationSkillOptionUi,
    selected: Boolean,
    rankControl: Boolean = false,
    description: String? = null,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!rankControl) Checkbox(checked = selected, onCheckedChange = null)
            Column(Modifier.weight(1f)) {
                Text(skill.name, style = MaterialTheme.typography.titleSmall)
                Text(skill.ability, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            skill.rank?.let { rank ->
                Text(rank.displayName(state.language), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(creationSigned(skill.modifier), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GearStep(state: DndAppState) {
    val draft = state.creation
    val packages = state.creationGearPackages()
    val preview = state.creationPreview()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepIntro(
            state.t("Choose starting gear", "Startausrüstung wählen"),
            state.t("Pick a rules package, then adjust it if your table starts differently.", "Wähle ein Regelpaket und passe es bei Bedarf an eure Runde an."),
        )
        packages.forEach { option ->
            OutlinedCard(
                onClick = { state.selectCreationGearPackage(option.id) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (draft.selectedStartingGearPackageId == option.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(option.name, style = MaterialTheme.typography.titleMedium)
                    Text(option.summary(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        EditableReviewRow(
            state = state,
            label = state.t("Starting armor", "Startrüstung"),
            value = preview.startingArmor,
            onClick = { state.openItemBrowser(ItemBrowserTarget.StartingArmor) },
        )
        OutlinedButton(onClick = { state.openItemBrowser(ItemBrowserTarget.StartingGear) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(state.t("Customize gear", "Ausrüstung anpassen"))
        }
        if (draft.startingWeapons.isNotEmpty() || draft.startingEquipment.isNotEmpty()) {
            Text(state.t("Selected items", "Ausgewählte Gegenstände"), style = MaterialTheme.typography.titleSmall)
            draft.startingWeapons.forEach { weapon ->
                CreationGearRow(
                    state = state,
                    name = weapon.name,
                    quantity = weapon.quantity,
                    onQuantityChange = { state.updateCreationWeaponQuantity(weapon.id, it) },
                    onRemove = { state.removeCreationWeapon(weapon.id) },
                )
            }
            draft.startingEquipment.forEach { item ->
                CreationGearRow(
                    state = state,
                    name = item.name,
                    quantity = item.quantity,
                    onRemove = { state.removeCreationEquipment(item.id) },
                )
            }
            if (draft.startingGoldPieces > 0) {
                Text("${draft.startingGoldPieces} GP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CreationGearRow(
    state: DndAppState,
    name: String,
    quantity: Int,
    onQuantityChange: ((Int) -> Unit)? = null,
    onRemove: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (onQuantityChange != null) {
                IconButton(onClick = { onQuantityChange(quantity - 1) }, enabled = quantity > 1) {
                    Icon(Icons.Rounded.Remove, contentDescription = state.t("Decrease $name quantity", "Anzahl $name verringern"))
                }
                Text("×$quantity", style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                    Icon(Icons.Rounded.Add, contentDescription = state.t("Increase $name quantity", "Anzahl $name erhöhen"))
                }
            } else if (quantity > 1) {
                Text("×$quantity", style = MaterialTheme.typography.labelLarge)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = state.t("Remove $name", "$name entfernen"))
            }
        }
    }
}

@Composable
private fun ReviewStep(state: DndAppState, onEditPortrait: (PortraitPickTarget) -> Unit) {
    val draft = state.creation
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        StepIntro(state.t("Ready for adventure", "Bereit fürs Abenteuer"), state.t("Review the essentials. You can edit and level up from the character sheet.", "Prüfe das Wichtigste. Bearbeiten und Stufenaufstieg sind später vom Charakterbogen aus möglich."))
        Card(shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CharacterPortrait(
                        draft.name,
                        draft.name.hashCode(),
                        Modifier.size(64.dp),
                        draft.portraitBytes,
                        onClick = draft.portraitBytes?.let { { onEditPortrait(PortraitPickTarget.Creation) } },
                        clickLabel = state.t("Edit portrait crop", "Porträtzuschnitt bearbeiten"),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(draft.name, style = MaterialTheme.typography.titleLarge)
                        Text("${draft.ancestry} · ${draft.className} ${draft.level}", style = MaterialTheme.typography.bodyMedium)
                    }
                    RulesetBadge(draft.ruleset)
                }
                if (draft.characterDescription.isNotBlank()) ReviewProfileField(state.t("Character", "Charakter"), draft.characterDescription)
                if (draft.motive.isNotBlank()) ReviewProfileField(state.t("Motive", "Motiv"), draft.motive)
                if (draft.alignment.isNotBlank()) {
                    ReviewProfileField(
                        state.t("Alignment", "Gesinnung"),
                        alignmentDisplayName(draft.alignment, state.language),
                    )
                }
                ReviewInfoRow(
                    state = state,
                    label = state.t("Ability method", "Attributsmethode"),
                    value = draft.statMethod.name,
                    onInfo = {
                        state.showInfo(
                            state.t("Ability method", "Attributsmethode"),
                            state.creationAbilityMethodExplanation(),
                        )
                    },
                )
                ReviewRow(state.t("Ruleset", "Regelwerk"), draft.ruleset.longLabel)
                ReviewRow(
                    state.t("Starting gear", "Startausrüstung"),
                    state.creationGearPackages().firstOrNull { it.id == draft.selectedStartingGearPackageId }?.name
                        ?: state.t("Customized", "Angepasst"),
                    good = state.creationGearSelectionValid(),
                )
                ReviewRow(
                    state.t("Background", "Hintergrund"),
                    state.selectedCreationBackground()?.name(state.language) ?: state.t("Choose", "Wählen"),
                    good = state.selectedCreationBackground() != null,
                )
                if (draft.ruleset != Ruleset.Pf2eRemaster) {
                    ReviewRow(
                        state.t("Subclass", "Unterklasse"),
                        draft.subclassName.ifBlank { state.t("Not selected yet", "Noch offen") },
                        good = !state.creationSubclassRequired() || draft.subclassName.isNotBlank(),
                    )
                }
                ReviewRow(
                    state.t("Required choices", "Erforderliche Wahlen"),
                    if (state.creationSubclassSelectionValid() && state.creationProficiencySelectionValid()) {
                        state.t("Complete", "Vollständig")
                    } else state.t("Needs review", "Prüfung nötig"),
                    good = state.creationSubclassSelectionValid() && state.creationProficiencySelectionValid(),
                )
            }
        }
        ExplanationCard(state.t("The table stays in charge", "Der Spieltisch entscheidet"), state.t("The app calculates what it can and asks you or the DM whenever a ruling is needed.", "Die App berechnet, was sie kann, und fragt dich oder die Spielleitung, sobald eine Entscheidung nötig ist."))
    }
}

@Composable
private fun ReviewProfileField(label: String, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FeatSelectionCard(
    state: DndAppState,
    feat: FeatOptionUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(feat.name, style = MaterialTheme.typography.titleSmall)
                feat.recommendedReason?.let { reason ->
                    Text(
                        state.t("Recommended · $reason", "Empfohlen · $reason"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(
                onClick = {
                    val body = buildString {
                        append(feat.summary.ifBlank { state.t("No additional details supplied.", "Keine weiteren Details angegeben.") })
                        feat.recommendedReason?.let { reason ->
                            append("\n\n")
                            append(state.t("Why it may fit: $reason", "Warum es passen könnte: $reason"))
                        }
                    }
                    state.showInfo(feat.name, body)
                },
            ) {
                Icon(Icons.Rounded.Info, contentDescription = state.t("Explain ${feat.name}", "${feat.name} erklären"))
            }
        }
    }
}

@Composable
private fun EditableReviewRow(state: DndAppState, label: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent, shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Rounded.Edit, contentDescription = state.t("Edit $label", "$label bearbeiten"), modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ReviewInfoRow(state: DndAppState, label: String, value: String, onInfo: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onInfo, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Info, contentDescription = state.t("Explain $label", "$label erklären"), modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, good: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (good) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
