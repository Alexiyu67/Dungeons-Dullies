package app.dulliesanddungeons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val navigateToStep: (CreationStep) -> Unit = { step -> draft.step = step.ordinal }
    LaunchedEffect(draft.step) {
        contentState.scrollToItem(0)
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

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = contentState,
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                when (currentStep) {
                    CreationStep.Rules -> RulesetStep(state)
                    CreationStep.Identity -> IdentityStep(state, onPickPortrait, onEditPortrait)
                    CreationStep.Build -> BuildStep(state)
                    CreationStep.LevelAndStats -> LevelAndStatsStep(state)
                    CreationStep.Details -> DetailsStep(state)
                    CreationStep.Gear -> GearStep(state)
                    CreationStep.Review -> ReviewStep(state, onEditPortrait)
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
                        (currentStep != CreationStep.Details || state.creationProficiencySelectionValid()) &&
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
    var selectedRolledAbility by remember(draft.statMethod, draft.rolledScores.toList()) { mutableStateOf<String?>(null) }
    LaunchedEffect(draft.statMethod) {
        if (draft.statMethod == StatMethod.Rolled && draft.rolledScores.isEmpty()) {
            state.rollCreationAbilityScores()
        }
    }
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
                    onClick = {
                        selectedRolledAbility = null
                        state.rollCreationAbilityScores()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    Icon(Icons.Rounded.Casino, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (draft.rolledScores.isEmpty()) state.t("Roll the six scores", "Sechs Werte würfeln") else state.t("Reroll all six", "Alle sechs neu würfeln"))
                }
                if (draft.rolledScores.isNotEmpty()) {
                    RolledAbilityAssignmentCard(
                        state = state,
                        selectedAbility = selectedRolledAbility,
                        onAbilitySelected = { selectedRolledAbility = it },
                    )
                }
            }
            StatMethod.StandardArray -> NumberChipPreview(if (draft.ruleset == Ruleset.Pf2eRemaster) listOf(18, 16, 14, 12, 10, 8) else listOf(15, 14, 13, 12, 10, 8))
            StatMethod.PointBuy -> NumberChipPreview(listOf(15, 15, 14, 10, 8, 8))
            StatMethod.Manual -> Unit
        }
        if (draft.statMethod != StatMethod.Rolled) {
            AbilityBreakdownList(state, editable = draft.statMethod == StatMethod.Manual)
        }

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
private fun RolledAbilityAssignmentCard(
    state: DndAppState,
    selectedAbility: String?,
    onAbilitySelected: (String?) -> Unit,
) {
    val breakdowns = state.creationAbilityBreakdowns()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(state.t("Assigned scores", "Verteilte Werte"), style = MaterialTheme.typography.titleSmall)
            if (selectedAbility != null) {
                Text(
                    state.t("Select another score to swap.", "Wähle einen zweiten Wert zum Tauschen."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            breakdowns.forEach { breakdown ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val abilityInfo = state.creationAbilityInfo(breakdown.ability)
                    TextButton(
                        onClick = { state.showInfo(abilityInfo.title, abilityInfo.body) },
                        modifier = Modifier.width(48.dp).height(48.dp).semantics {
                            contentDescription = state.t("Explain ${abilityInfo.title}", "${abilityInfo.title} erklären")
                        },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(breakdown.ability, fontWeight = FontWeight.Bold)
                    }
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val isSelected = selectedAbility == breakdown.ability
                        val scoreDescription = when {
                            isSelected -> state.t(
                                "${breakdown.ability} assigned score ${breakdown.baseScore}, selected. Tap again to cancel.",
                                "${breakdown.ability}, verteilter Wert ${breakdown.baseScore}, ausgewählt. Zum Abbrechen erneut tippen.",
                            )
                            selectedAbility != null -> state.t(
                                "${breakdown.ability} assigned score ${breakdown.baseScore}. Tap to swap with $selectedAbility.",
                                "${breakdown.ability}, verteilter Wert ${breakdown.baseScore}. Zum Tauschen mit $selectedAbility tippen.",
                            )
                            else -> state.t(
                                "${breakdown.ability} assigned score ${breakdown.baseScore}. Tap to select for swapping.",
                                "${breakdown.ability}, verteilter Wert ${breakdown.baseScore}. Zum Tauschen auswählen.",
                            )
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                when {
                                    selectedAbility == null -> onAbilitySelected(breakdown.ability)
                                    isSelected -> onAbilitySelected(null)
                                    else -> {
                                        state.swapCreationRolledScores(selectedAbility, breakdown.ability)
                                        onAbilitySelected(null)
                                    }
                                }
                            },
                            label = { Text(breakdown.baseScore.toString(), fontWeight = FontWeight.Bold) },
                            modifier = Modifier.height(48.dp).semantics(mergeDescendants = true) {
                                contentDescription = scoreDescription
                                selected = isSelected
                            },
                        )
                        AssistChip(
                            onClick = {
                                val info = state.creationModifierInfo(breakdown.ability)
                                state.showInfo(info.title, info.body)
                            },
                            label = { Text("Mod. ${creationSigned(breakdown.modifier)}") },
                            modifier = Modifier.height(48.dp).semantics(mergeDescendants = true) {
                                contentDescription = state.t(
                                    "${breakdown.ability} modifier ${creationSigned(breakdown.modifier)}. Tap for details.",
                                    "${breakdown.ability}, Modifikator ${creationSigned(breakdown.modifier)}. Für Details tippen.",
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary),
                        )
                        if (breakdown.ancestrySource != null && breakdown.finalScore != breakdown.baseScore) {
                            AssistChip(
                                onClick = {
                                    state.creationAncestryInfo(breakdown.ability)?.let { info -> state.showInfo(info.title, info.body) }
                                },
                                label = { Text(state.t("Total ${breakdown.finalScore}", "Gesamt ${breakdown.finalScore}")) },
                                modifier = Modifier.height(48.dp).semantics(mergeDescendants = true) {
                                    contentDescription = state.t(
                                        "${breakdown.ability} total ${breakdown.finalScore}. Tap for ancestry details.",
                                        "${breakdown.ability}, Gesamtwert ${breakdown.finalScore}. Für Abstammungsdetails tippen.",
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }
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
private fun DetailsStep(state: DndAppState) {
    val draft = state.creation
    val preview = state.creationPreview()
    val featLevels = if (draft.className == "Fighter") listOf(4, 6, 8, 12, 14, 16, 19) else listOf(4, 8, 12, 16, 19)
    val featChoices = featLevels.count { it <= draft.level }
    val featOptions = state.creationFeatOptions().sortedForPicker(state.language, FeatOptionUi::name, FeatOptionUi::id)
    val spellOptions = state.creationSpellOptions().sortedForPicker(state.language, SpellUi::name, SpellUi::id)
    val backgrounds = state.creationBackgroundOptions()
    val background = state.selectedCreationBackground()
    val rankSkillOptions = state.creationRankSkillOptions()
    var languageText by remember(draft.ruleset) { mutableStateOf(draft.languages.joinToString(", ")) }
    val suggestedLanguages = languageSuggestions(languageText, state.creationLanguageOptions())
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
        Text(state.t("Background", "Hintergrund"), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(backgrounds, key = { it.id }) { option ->
                FilterChip(
                    selected = draft.backgroundId == option.id,
                    onClick = { state.selectCreationBackground(option.id) },
                    label = { Text(option.name(state.language)) },
                )
            }
        }
        background?.let { selected ->
            val grantNames = selected.allGrantedSkillIds.mapNotNull { id ->
                rankSkillOptions.firstOrNull { it.id == id }?.name
            }
            if (grantNames.isNotEmpty()) {
                Text(
                    state.t("Grants: ${grantNames.joinToString()}", "Gewährt: ${grantNames.joinToString()}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected.customSkillCount > 0) {
                Text(
                    state.t(
                        "Background skills · ${draft.backgroundSkillIds.size}/${selected.customSkillCount}",
                        "Hintergrundfertigkeiten · ${draft.backgroundSkillIds.size}/${selected.customSkillCount}",
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                rankSkillOptions.forEach { skill ->
                    CreationSkillChoiceCard(
                        state = state,
                        skill = skill,
                        selected = skill.id in draft.backgroundSkillIds,
                        onClick = { state.toggleCreationBackgroundSkill(skill.id) },
                    )
                }
            }
        }
        val classDefinition = ProficiencyCatalog.classDefinition(draft.ruleset, draft.className)
        val fixedClassSkillNames = classDefinition.fixedSkillIds.mapNotNull { id ->
            rankSkillOptions.firstOrNull { it.id == id }?.name
        }
        if (fixedClassSkillNames.isNotEmpty()) {
            Text(
                state.t("Class grants: ${fixedClassSkillNames.joinToString()}", "Klasse gewährt: ${fixedClassSkillNames.joinToString()}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val classSkillCount = state.creationClassSkillCount()
        Text(
            state.t("Class skills · ${draft.classSkillIds.size}/$classSkillCount", "Klassenfertigkeiten · ${draft.classSkillIds.size}/$classSkillCount"),
            style = MaterialTheme.typography.titleMedium,
        )
        state.creationClassSkillOptions().forEach { skill ->
            CreationSkillChoiceCard(
                state = state,
                skill = skill,
                selected = skill.id in draft.classSkillIds,
                onClick = { state.toggleCreationClassSkill(skill.id) },
            )
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
        if (spellOptions.isNotEmpty()) {
            Text(state.t("Approved private spells", "Freigegebene private Zauber"), style = MaterialTheme.typography.titleMedium)
            spellOptions.forEach { spell ->
                FilterChip(
                    selected = spell.id.removePrefix("private-") in draft.selectedSpellIds,
                    onClick = {
                        val entryId = spell.id.removePrefix("private-")
                        if (entryId in draft.selectedSpellIds) draft.selectedSpellIds.remove(entryId) else draft.selectedSpellIds += entryId
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(spell.name)
                            CostChipRow(state, spell.activationCost.toCostTokens())
                        }
                    },
                )
            }
        }
        ExplanationCard(
            state.t("Nothing gets silently removed", "Nichts wird unbemerkt entfernt"),
            state.t("If an earlier choice changes, we show everything that needs review before saving.", "Wenn sich eine frühere Wahl ändert, zeigen wir vor dem Speichern alles, was geprüft werden muss."),
        )
    }
}

@Composable
private fun CreationSkillChoiceCard(
    state: DndAppState,
    skill: CreationSkillOptionUi,
    selected: Boolean,
    rankControl: Boolean = false,
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
                CreationGearRow(weapon.name, onRemove = { state.removeCreationWeapon(weapon.id) })
            }
            draft.startingEquipment.forEach { item ->
                CreationGearRow(item.name, onRemove = { state.removeCreationEquipment(item.id) })
            }
            if (draft.startingGoldPieces > 0) {
                Text("${draft.startingGoldPieces} GP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CreationGearRow(name: String, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, contentDescription = null) }
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
