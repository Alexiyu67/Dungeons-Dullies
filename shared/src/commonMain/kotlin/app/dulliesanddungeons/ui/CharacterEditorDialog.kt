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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterEditorDialog(state: DndAppState, onPickPortrait: (PortraitPickTarget) -> Unit) {
    val draft = state.editorDraft ?: return
    Dialog(
        onDismissRequest = state::cancelEdit,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(editorTitle(state, draft.section), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (draft.section != EditorSection.Hub) {
                                    Text(draft.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (draft.section == EditorSection.Hub) state.cancelEdit() else draft.section = EditorSection.Hub
                            }) {
                                Icon(
                                    if (draft.section == EditorSection.Hub) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = state.t("Back", "Zurück"),
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    EditorBottomBar(state, draft)
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).paperTexture(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    when (draft.section) {
                        EditorSection.Hub -> item { EditorHub(state, draft) }
                        EditorSection.Identity -> item { IdentityEditor(state, draft, onPickPortrait) }
                        EditorSection.Build -> item { BuildEditor(state, draft) }
                        EditorSection.Abilities -> item { AbilitiesEditor(state, draft) }
                        EditorSection.Combat -> item { CombatEditor(state, draft) }
                        EditorSection.Spells -> item { SpellsEditor(state, draft) }
                        EditorSection.Review -> item { ReviewEditor(state, draft) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorBottomBar(state: DndAppState, draft: CharacterEditorDraft) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (draft.section == EditorSection.Hub) {
                TextButton(onClick = state::cancelEdit) { Text(state.t("Cancel", "Abbrechen")) }
                Spacer(Modifier.weight(1f))
                Button(onClick = { draft.section = EditorSection.Review }, enabled = draft.isValid) {
                    Text(state.t("Review changes", "Änderungen prüfen"))
                }
            } else if (draft.section == EditorSection.Review) {
                TextButton(onClick = { draft.section = EditorSection.Hub }) { Text(state.t("Edit", "Bearbeiten")) }
                Spacer(Modifier.weight(1f))
                Button(onClick = { state.saveEdit() }, enabled = draft.isValid) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(state.t("Save character", "Charakter speichern"))
                }
            } else {
                Spacer(Modifier.weight(1f))
                Button(onClick = { draft.section = EditorSection.Hub }, enabled = draft.isValid) {
                    Text(state.t("Done", "Fertig"))
                }
            }
        }
    }
}

@Composable
private fun EditorHub(state: DndAppState, draft: CharacterEditorDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            state.t("Edit only what changed. Existing features, spells, weapons, and equipment stay intact.", "Ändere nur, was nötig ist. Merkmale, Zauber, Waffen und Ausrüstung bleiben erhalten."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EditorHubCard(Icons.Rounded.Person, state.t("Identity", "Identität"), draft.name, { draft.section = EditorSection.Identity })
        EditorHubCard(Icons.Rounded.Edit, state.t("Build", "Build"), "${draft.ancestry} · ${draft.className} ${draft.level}", { draft.section = EditorSection.Build })
        EditorHubCard(Icons.Rounded.SportsMma, state.t("Abilities", "Attribute"), editorAbilityOrder.joinToString(" · ") { "$it ${draft.abilities[it] ?: 10}" }, { draft.section = EditorSection.Abilities })
        EditorHubCard(Icons.Rounded.Shield, state.t("Combat", "Kampf"), "HP ${draft.maxHp} · AC ${draft.armorClass} · ${draft.speedFeet} ft", { draft.section = EditorSection.Combat })
        if (draft.original.canCastSpells || draft.className.equals("Wizard", true) || draft.className.equals("Sorcerer", true)) {
            EditorHubCard(
                Icons.Rounded.AutoAwesome,
                state.t("Spells", "Zauber"),
                state.t("${draft.spells.count { it.sourceKind == SpellSourceKind.CLASS }} class spells", "${draft.spells.count { it.sourceKind == SpellSourceKind.CLASS }} Klassenzauber"),
                { draft.section = EditorSection.Spells },
            )
        }
    }
}

@Composable
private fun EditorHubCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun IdentityEditor(state: DndAppState, draft: CharacterEditorDraft, onPickPortrait: (PortraitPickTarget) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CharacterPortrait(draft.name, draft.original.portraitSeed, Modifier.size(82.dp), draft.portraitBytes)
            Spacer(Modifier.width(14.dp))
            TextButton(onClick = { onPickPortrait(PortraitPickTarget.Editor) }) { Text(state.t("Change portrait", "Porträt ändern")) }
        }
        OutlinedTextField(
            value = draft.name,
            onValueChange = { draft.name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(state.t("Name", "Name")) },
            isError = draft.name.isBlank(),
            singleLine = true,
        )
        OutlinedTextField(
            value = draft.characterDescription,
            onValueChange = { draft.characterDescription = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(state.t("Character", "Charakter")) },
            singleLine = true,
        )
        OutlinedTextField(
            value = draft.motive,
            onValueChange = { draft.motive = it.take(500) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(state.t("Motive", "Motiv")) },
            minLines = 3,
            maxLines = 6,
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
private fun BuildEditor(state: DndAppState, draft: CharacterEditorDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        OutlinedTextField(draft.ancestry, { draft.ancestry = it }, Modifier.fillMaxWidth(), label = { Text(state.t("Ancestry", "Abstammung")) }, singleLine = true)
        OutlinedTextField(draft.className, { draft.className = it }, Modifier.fillMaxWidth(), label = { Text(state.t("Class", "Klasse")) }, singleLine = true)
        OutlinedTextField(draft.subclass, { draft.subclass = it }, Modifier.fillMaxWidth(), label = { Text(state.t("Subclass", "Unterklasse")) }, singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(state.t("Level", "Stufe"), style = MaterialTheme.typography.titleSmall)
            Text(draft.level.toString(), style = MaterialTheme.typography.titleLarge)
        }
        EditorNotice(state.t("Use Level Up on the character sheet to change level and record every required choice.", "Nutze den Stufenaufstieg auf dem Charakterbogen, damit alle nötigen Entscheidungen erfasst werden."))
        Text(state.t("Ruleset", "Regelwerk"), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Ruleset.entries.forEach { ruleset ->
                FilterChip(selected = draft.ruleset == ruleset, onClick = { draft.ruleset = ruleset }, label = { Text(ruleset.shortLabel) })
            }
        }
        if (draft.ruleset != draft.original.ruleset) {
            EditorNotice(state.t("Saving creates a converted copy. The original character remains unchanged.", "Beim Speichern wird eine konvertierte Kopie erstellt. Das Original bleibt unverändert."))
        }
        if (draft.level != draft.original.level || draft.className != draft.original.className) {
            EditorNotice(state.t("Linked numbers update; existing build content is preserved for review.", "Verknüpfte Werte werden aktualisiert; vorhandene Build-Inhalte bleiben zur Prüfung erhalten."))
        }
    }
}

@Composable
private fun AbilitiesEditor(state: DndAppState, draft: CharacterEditorDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        editorAbilityOrder.forEach { ability ->
            val score = draft.abilities[ability] ?: 10
            NumberStepper(ability, score, 1..30, supporting = state.t("Modifier ${signed(editorModifier(score))}", "Modifikator ${signed(editorModifier(score))}")) {
                draft.abilities[ability] = it
            }
        }
        val prof = if (draft.proficiencyManual) draft.proficiency else proficiencyForEditor(draft.level)
        val initiative = if (draft.initiativeManual) draft.initiative else editorModifier(draft.abilities["DEX"] ?: 10)
        EditorNotice(state.t("Preview: initiative ${signed(initiative)} · proficiency ${signed(prof)}. Linked saves, skills, and attacks update when saved.", "Vorschau: Initiative ${signed(initiative)} · Übungsbonus ${signed(prof)}. Verknüpfte Rettungswürfe, Fertigkeiten und Angriffe werden beim Speichern aktualisiert."))
    }
}

@Composable
private fun CombatEditor(state: DndAppState, draft: CharacterEditorDraft) {
    val shownInitiative = if (draft.initiativeManual) draft.initiative else editorModifier(draft.abilities["DEX"] ?: 10)
    val shownProficiency = if (draft.proficiencyManual) draft.proficiency else proficiencyForEditor(draft.level)
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        NumberStepper(state.t("Maximum HP", "Maximale TP"), draft.maxHp, 1..999) { draft.maxHp = it }
        NumberStepper(state.t("Armor Class", "Rüstungsklasse"), draft.armorClass, 1..99) { draft.armorClass = it }
        NumberStepper(state.t("Walking speed", "Gehbewegung"), draft.speedFeet, 0..250, "ft") { draft.speedFeet = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft.flySpeedFeet != null, onCheckedChange = { enabled -> draft.flySpeedFeet = if (enabled) (draft.flySpeedFeet ?: draft.speedFeet) else null })
            Text(state.t("Has flying speed", "Hat Flugbewegung"))
        }
        draft.flySpeedFeet?.let { speed -> NumberStepper(state.t("Flying speed", "Flugbewegung"), speed, 1..250, "ft") { draft.flySpeedFeet = it } }
        HorizontalDivider()
        NumberStepper(state.t("Initiative", "Initiative"), shownInitiative, -20..30, supporting = if (draft.initiativeManual) state.t("Manual override", "Manuelle Überschreibung") else state.t("Linked to Dexterity", "Mit Geschicklichkeit verknüpft")) {
            draft.initiative = it
            draft.initiativeManual = true
        }
        if (draft.initiativeManual && draft.original.derivation.initiative != null) {
            TextButton(onClick = { draft.initiativeManual = false }) { Text(state.t("Restore Dexterity link", "Geschicklichkeits-Verknüpfung wiederherstellen")) }
        }
        NumberStepper(state.t("Proficiency bonus", "Übungsbonus"), shownProficiency, 0..12, supporting = if (draft.proficiencyManual) state.t("Manual override", "Manuelle Überschreibung") else state.t("Linked to level", "Mit Stufe verknüpft")) {
            draft.proficiency = it
            draft.proficiencyManual = true
        }
        if (draft.proficiencyManual) {
            TextButton(onClick = { draft.proficiencyManual = false }) { Text(state.t("Restore level link", "Stufen-Verknüpfung wiederherstellen")) }
        }
    }
}

@Composable
private fun SpellsEditor(state: DndAppState, draft: CharacterEditorDraft) {
    val character = draft.original
    val catalogClassName = when {
        draft.className.equals("Wizard", true) || draft.className.equals("Sorcerer", true) -> draft.className
        character.progression.any { it.className.equals("Wizard", true) } -> "Wizard"
        character.progression.any { it.className.equals("Sorcerer", true) } -> "Sorcerer"
        else -> draft.className
    }
    val catalogCharacter = character.copy(className = catalogClassName, ruleset = draft.ruleset, progression = emptyList())
    val isWizard = catalogClassName.equals("Wizard", true)
    val catalog = state.editableSpellCatalog(catalogCharacter)
    var search by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf<Int?>(null) }
    var statusFilter by remember { mutableStateOf("all") }
    var sourceFilter by remember { mutableStateOf("all") }

    fun selectedSpell(option: SpellUi): SpellUi? = draft.spells.firstOrNull {
        it.sourceKind == SpellSourceKind.CLASS && (it.id == option.id || it.name.equals(option.name, ignoreCase = true))
    }

    val shown = catalog.asSequence()
        .filter { option -> search.isBlank() || option.name.contains(search.trim(), true) || option.summary.contains(search.trim(), true) }
        .filter { levelFilter == null || it.level == levelFilter }
        .filter { option ->
            val selected = selectedSpell(option)
            when (statusFilter) {
                "selected" -> selected != null
                "prepared" -> selected?.prepared == true || selected?.level == 0
                else -> true
            }
        }
        .filter { option ->
            when (sourceFilter) {
                "srd" -> option.sourceName.startsWith("SRD")
                "custom" -> !option.sourceName.startsWith("SRD")
                else -> true
            }
        }
        .groupBy { it.level }
        .toSortedMap()

    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        val grantedCount = character.availableSpells.count { it.sourceKind != SpellSourceKind.CLASS }
        if (grantedCount > 0) {
            EditorNotice(
                state.t(
                    "$grantedCount item or feature-granted spells remain read-only here.",
                    "$grantedCount durch Gegenstände oder Merkmale gewährte Zauber bleiben hier schreibgeschützt.",
                ),
            )
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            label = { Text(state.t("Search spells", "Zauber suchen")) },
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(selected = levelFilter == null, onClick = { levelFilter = null }, label = { Text(state.t("All levels", "Alle Grade")) })
            (0..9).forEach { level ->
                FilterChip(
                    selected = levelFilter == level,
                    onClick = { levelFilter = level },
                    label = { Text(if (level == 0) state.t("Cantrip", "Zaubertrick") else state.t("Level $level", "Grad $level")) },
                )
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(
                "all" to state.t("All", "Alle"),
                "selected" to if (isWizard) state.t("Spellbook", "Zauberbuch") else state.t("Known", "Bekannt"),
                "prepared" to state.t("Prepared", "Vorbereitet"),
            ).forEach { (id, label) ->
                FilterChip(selected = statusFilter == id, onClick = { statusFilter = id }, label = { Text(label) })
            }
            listOf("all" to state.t("Any source", "Alle Quellen"), "srd" to "SRD", "custom" to state.t("Custom", "Eigen"))
                .forEach { (id, label) ->
                    FilterChip(selected = sourceFilter == id, onClick = { sourceFilter = id }, label = { Text(label) })
                }
        }
        val classSpells = draft.spells.filter { it.sourceKind == SpellSourceKind.CLASS }
        val preparedCount = classSpells.count { it.prepared && it.level > 0 }
        val wizardSuggested = (character.level + editorModifier(character.abilities["INT"] ?: 10)).coerceAtLeast(1)
        EditorNotice(
            if (isWizard) {
                state.t(
                    "Spellbook ${classSpells.size} · prepared $preparedCount (usual maximum $wizardSuggested). Changes are allowed at any time; your table may normally limit when spells are added or prepared.",
                    "Zauberbuch ${classSpells.size} · vorbereitet $preparedCount (übliches Maximum $wizardSuggested). Änderungen sind jederzeit möglich; am Spieltisch kann der Zeitpunkt normalerweise begrenzt sein.",
                )
            } else {
                state.t(
                    "${classSpells.size} known spells. Changes are allowed at any time; Sorcerers normally learn or replace spells when leveling up.",
                    "${classSpells.size} bekannte Zauber. Änderungen sind jederzeit möglich; Zauberer lernen oder ersetzen Zauber normalerweise beim Stufenaufstieg.",
                )
            },
        )
        shown.forEach { (level, levelSpells) ->
            Text(
                if (level == 0) state.t("Cantrips", "Zaubertricks") else state.t("Level $level", "Grad $level"),
                style = MaterialTheme.typography.titleMedium,
            )
            levelSpells.sortedForPicker(state.language, SpellUi::name, SpellUi::id).forEach { option ->
                val selected = selectedSpell(option)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selected != null,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    draft.spells += option.copy(prepared = !isWizard || option.level == 0)
                                } else {
                                    draft.spells.removeAll { it.sourceKind == SpellSourceKind.CLASS && (it.id == option.id || it.name.equals(option.name, true)) }
                                }
                            },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(option.name, style = MaterialTheme.typography.labelLarge)
                            Text(option.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (option.castPreviews.isNotEmpty()) {
                                Text(
                                    option.castPreviews.entries.sortedBy { it.key }.joinToString(" · ") { "L${it.key} ${it.value}" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (isWizard && selected != null && option.level > 0) {
                            FilterChip(
                                selected = selected.prepared,
                                onClick = {
                                    val index = draft.spells.indexOf(selected)
                                    if (index >= 0) draft.spells[index] = selected.copy(prepared = !selected.prepared)
                                },
                                label = { Text(state.t("Prepared", "Vorbereitet")) },
                            )
                        }
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            Text(state.t("No spells match these filters.", "Keine Zauber entsprechen diesen Filtern."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewEditor(state: DndAppState, draft: CharacterEditorDraft) {
    val original = draft.original
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(state.t("Only changed fields are highlighted.", "Nur geänderte Felder sind hervorgehoben."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        ReviewChange(state.t("Name", "Name"), original.name, draft.name)
        ReviewChange(state.t("Character", "Charakter"), original.profile.characterDescription, draft.characterDescription)
        ReviewChange(state.t("Motive", "Motiv"), original.profile.motive, draft.motive)
        ReviewChange(
            state.t("Alignment", "Gesinnung"),
            alignmentDisplayName(original.profile.alignment, state.language),
            alignmentDisplayName(draft.alignment, state.language),
        )
        ReviewChange(state.t("Build", "Build"), original.buildLabel, "${draft.ancestry} ${draft.className} ${draft.level}")
        ReviewChange(state.t("Subclass", "Unterklasse"), original.subclass, draft.subclass)
        ReviewChange(state.t("Ruleset", "Regelwerk"), original.ruleset.shortLabel, draft.ruleset.shortLabel)
        ReviewChange("HP", original.maxHp.toString(), draft.maxHp.toString())
        ReviewChange("AC", original.armorClass.toString(), draft.armorClass.toString())
        ReviewChange(state.t("Movement", "Bewegung"), "${original.speedFeet}/${original.flySpeedFeet ?: "—"}", "${draft.speedFeet}/${draft.flySpeedFeet ?: "—"}")
        ReviewChange(state.t("Abilities", "Attribute"), editorAbilityOrder.joinToString(" / ") { (original.abilities[it] ?: 10).toString() }, editorAbilityOrder.joinToString(" / ") { (draft.abilities[it] ?: 10).toString() })
        ReviewChange(state.t("Class spells", "Klassenzauber"), original.spells.count { it.sourceKind == SpellSourceKind.CLASS }.toString(), draft.spells.count { it.sourceKind == SpellSourceKind.CLASS }.toString())
        if (draft.ruleset != original.ruleset) {
            EditorNotice(state.t("A new ${draft.ruleset.shortLabel} copy will be selected after save.", "Nach dem Speichern wird eine neue ${draft.ruleset.shortLabel}-Kopie ausgewählt."))
        }
    }
}

@Composable
private fun ReviewChange(label: String, before: String, after: String) {
    val changed = before != after
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (changed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (changed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(after, color = if (changed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditorNotice(text: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text(text, Modifier.fillMaxWidth().padding(13.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String = "",
    supporting: String = "",
    onValueChange: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                if (supporting.isNotBlank()) Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onValueChange((value - 1).coerceIn(range)) }, enabled = value > range.first) { Text("−") }
            Text("$value${if (suffix.isBlank()) "" else " $suffix"}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
            TextButton(onClick = { onValueChange((value + 1).coerceIn(range)) }, enabled = value < range.last) { Text("+") }
        }
    }
}

private fun editorTitle(state: DndAppState, section: EditorSection): String = when (section) {
    EditorSection.Hub -> state.t("Edit character", "Charakter bearbeiten")
    EditorSection.Identity -> state.t("Identity", "Identität")
    EditorSection.Build -> state.t("Build", "Build")
    EditorSection.Abilities -> state.t("Abilities", "Attribute")
    EditorSection.Combat -> state.t("Combat", "Kampf")
    EditorSection.Spells -> state.t("Spells", "Zauber")
    EditorSection.Review -> state.t("Review", "Prüfen")
}

private fun editorModifier(score: Int): Int = floor((score - 10) / 2.0).toInt()
private fun proficiencyForEditor(level: Int): Int = 2 + (level.coerceIn(1, 20) - 1) / 4
private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
private val editorAbilityOrder = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")
