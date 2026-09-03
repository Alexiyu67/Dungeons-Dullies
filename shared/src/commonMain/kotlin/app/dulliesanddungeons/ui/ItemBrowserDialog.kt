package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CoreModifier
import app.dulliesanddungeons.domain.CoreStatistic
import app.dulliesanddungeons.domain.DifficultyClass
import app.dulliesanddungeons.domain.EffectActivation
import app.dulliesanddungeons.domain.SavingThrowPrompt
import app.dulliesanddungeons.domain.WeaponClassification
import app.dulliesanddungeons.domain.WeaponCombatType

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.floor

private enum class BrowserPage { Catalog, CustomEquipment, CustomWeapon }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ItemBrowserDialog(state: DndAppState) {
    val target = state.itemBrowserTarget
    val ruleset = if (target != ItemBrowserTarget.Inventory) state.creation.ruleset else state.selectedCharacter?.ruleset ?: return
    val editingEquipment = state.itemBrowserEditingEquipment
    val editingWeapon = state.itemBrowserEditingWeapon
    var page by remember(target, editingEquipment?.id, editingWeapon?.id) {
        mutableStateOf(
            when {
                editingEquipment != null -> BrowserPage.CustomEquipment
                editingWeapon != null -> BrowserPage.CustomWeapon
                else -> BrowserPage.Catalog
            },
        )
    }
    var prefill by remember(target, editingEquipment?.id, editingWeapon?.id) { mutableStateOf<KnownItemUi?>(null) }
    var query by remember(target) { mutableStateOf("") }
    var filterOpen by remember(target) { mutableStateOf(false) }
    var customChoiceOpen by remember(target) { mutableStateOf(false) }
    var infoItem by remember(target) { mutableStateOf<KnownItemUi?>(null) }
    var quickWeaponItem by remember(target) { mutableStateOf<KnownItemUi?>(null) }
    var quickWeaponBonus by remember(target) { mutableStateOf(0) }
    var selectedTypes by remember(target) {
        mutableStateOf(if (target == ItemBrowserTarget.StartingArmor) setOf(KnownItemType.Armor) else emptySet())
    }
    var selectedRarities by remember(target) { mutableStateOf(emptySet<ItemRarity>()) }
    var selectedSources by remember(target) { mutableStateOf(emptySet<KnownItemSource>()) }
    var includeIncompatible by remember(target) { mutableStateOf(false) }
    var sort by remember(target) { mutableStateOf(KnownItemSort.Name) }
    var ascending by remember(target) { mutableStateOf(true) }

    Dialog(
        onDismissRequest = state::closeItemBrowser,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (page == BrowserPage.Catalog) state.closeItemBrowser() else {
                                page = BrowserPage.Catalog
                                prefill = null
                            }
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            if (page == BrowserPage.Catalog) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = state.t("Back", "Zurück"),
                        )
                    }
                    Text(
                        when {
                            page == BrowserPage.CustomWeapon -> state.t("Custom weapon", "Eigene Waffe")
                            page == BrowserPage.CustomEquipment && target == ItemBrowserTarget.StartingArmor -> state.t("Custom armor", "Eigene Rüstung")
                            page == BrowserPage.CustomEquipment -> state.t("Custom item", "Eigener Gegenstand")
                            target == ItemBrowserTarget.StartingArmor -> state.t("Choose starting armor", "Startrüstung wählen")
                            else -> state.t("Add items", "Gegenstände hinzufügen")
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (page == BrowserPage.Catalog) {
                        TextButton(onClick = {
                            if (target == ItemBrowserTarget.StartingArmor) page = BrowserPage.CustomEquipment
                            else customChoiceOpen = true
                        }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(18.dp))
                            Text(state.t("Custom", "Eigene"))
                        }
                    }
                }

                when (page) {
                    BrowserPage.Catalog -> {
                        Box(Modifier.fillMaxSize()) {
                            ItemCatalogPage(
                                state = state,
                                target = target,
                                ruleset = ruleset,
                                query = query,
                                onQueryChange = { query = it },
                                selectedTypes = selectedTypes,
                                selectedRarities = selectedRarities,
                                selectedSources = selectedSources,
                                includeIncompatible = includeIncompatible,
                                sort = sort,
                                ascending = ascending,
                                onIncomplete = { item ->
                                    prefill = item
                                    page = if (item.type == KnownItemType.Weapon) BrowserPage.CustomWeapon else BrowserPage.CustomEquipment
                                },
                                onAdd = { item ->
                                    if (item.type == KnownItemType.Weapon) {
                                        quickWeaponItem = item
                                        quickWeaponBonus = item.weapon?.itemBonus ?: 0
                                    } else if (target == ItemBrowserTarget.StartingArmor) {
                                        state.selectCreationArmor(item)
                                    } else if (target == ItemBrowserTarget.StartingGear) {
                                        if (state.addCreationGear(item)) {
                                            state.itemBrowserFeedback = state.t("Added ${item.name}", "${item.name} hinzugefügt")
                                        }
                                    } else {
                                        state.addKnownItem(item)
                                    }
                                },
                                onInfo = { infoItem = it },
                            )
                            FloatingActionButton(
                                onClick = { filterOpen = true },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = state.t("Filter and sort items", "Gegenstände filtern und sortieren"))
                            }
                        }
                    }
                    BrowserPage.CustomEquipment -> CustomEquipmentPage(
                        state = state,
                        target = target,
                        initial = prefill,
                        editing = editingEquipment,
                        onAdded = {
                            page = BrowserPage.Catalog
                            prefill = null
                        },
                    )
                    BrowserPage.CustomWeapon -> CustomWeaponPage(
                        state = state,
                        initial = prefill,
                        editing = editingWeapon,
                        onAdded = {
                            page = BrowserPage.Catalog
                            prefill = null
                        },
                    )
                }
            }
        }
    }

    if (filterOpen) {
        ItemFiltersSheet(
            state = state,
            target = target,
            selectedTypes = selectedTypes,
            onTypesChange = { selectedTypes = it },
            selectedRarities = selectedRarities,
            onRaritiesChange = { selectedRarities = it },
            selectedSources = selectedSources,
            onSourcesChange = { selectedSources = it },
            includeIncompatible = includeIncompatible,
            onIncludeIncompatibleChange = { includeIncompatible = it },
            sort = sort,
            onSortChange = { sort = it },
            ascending = ascending,
            onAscendingChange = { ascending = it },
            onDismiss = { filterOpen = false },
            onClear = {
                selectedTypes = if (target == ItemBrowserTarget.StartingArmor) setOf(KnownItemType.Armor) else emptySet()
                selectedRarities = emptySet()
                selectedSources = emptySet()
                includeIncompatible = false
                sort = KnownItemSort.Name
                ascending = true
            },
        )
    }

    if (customChoiceOpen) {
        ModalBottomSheet(onDismissRequest = { customChoiceOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.t("Create custom", "Eigenen Eintrag erstellen"), style = MaterialTheme.typography.titleLarge)
                Card(onClick = { customChoiceOpen = false; page = BrowserPage.CustomEquipment }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Backpack, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(state.t("Equipment or armor", "Ausrüstung oder Rüstung"), style = MaterialTheme.typography.titleSmall)
                    }
                }
                Card(onClick = { customChoiceOpen = false; page = BrowserPage.CustomWeapon }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SportsMma, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(state.t("Weapon", "Waffe"), style = MaterialTheme.typography.titleSmall)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    infoItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { infoItem = null }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(item.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${itemTypeLabel(state, item.type)} · ${rarityLabel(state, item.rarity)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.details.isNotBlank()) Text(item.details)
                item.equipment?.useCase?.takeIf(String::isNotBlank)?.let { Text(it) }
                item.weapon?.let { weapon ->
                    val reach = if ("reach" in weapon.properties.lowercase()) 10 else 5
                    Text(state.t("Reach $reach ft · ${weapon.properties}", "Reichweite $reach ft · ${weapon.properties}"))
                    Text(weapon.useCase.ifBlank { state.weaponUseCase(weapon) })
                }
                val savePrompts = item.equipment?.savingThrows ?: item.weapon?.savingThrows.orEmpty()
                if (savePrompts.isNotEmpty()) {
                    Text(
                        savePrompts.joinToString(" · ") { prompt ->
                            val formula = prompt.difficultyClass
                            val calculated = when {
                                formula.fixed != null -> formula.fixed
                                target == ItemBrowserTarget.StartingGear && formula.ability != null -> {
                                    val modifier = floor((state.creationAbilityScore(formula.ability.shortName()) - 10) / 2.0).toInt()
                                    formula.base + modifier + if (formula.addProficiency) state.creationTrainedProficiency() else 0
                                }
                                target != ItemBrowserTarget.StartingGear && state.selectedCharacter != null ->
                                    CharacterStatEngine.difficultyClass(state.selectedCharacter!!, formula)
                                else -> null
                            }
                            val dc = calculated?.let { "DC $it" } ?: state.t("calculated DC", "berechneter SG")
                            "${prompt.ability.displayName()} $dc"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedButton(
                    onClick = {
                        prefill = item
                        infoItem = null
                        page = if (item.type == KnownItemType.Weapon) BrowserPage.CustomWeapon else BrowserPage.CustomEquipment
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(state.t("Customize a copy", "Kopie anpassen")) }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    quickWeaponItem?.let { item ->
        val template = item.weapon ?: return@let
        ModalBottomSheet(onDismissRequest = { quickWeaponItem = null }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(item.name, style = MaterialTheme.typography.titleLarge)
                Text(state.t("Magic bonus", "Magiebonus"), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0..3).toList()) { bonus ->
                        FilterChip(
                            selected = quickWeaponBonus == bonus,
                            onClick = { quickWeaponBonus = bonus },
                            label = { Text(if (bonus == 0) "+0" else "+$bonus") },
                        )
                    }
                }
                Button(
                    onClick = {
                        if (target == ItemBrowserTarget.StartingGear) state.addCreationWeapon(template, quickWeaponBonus)
                        else state.addStandardWeapon(template, quickWeaponBonus)
                        state.itemBrowserFeedback = state.t(
                            "Added ${if (quickWeaponBonus > 0) "+$quickWeaponBonus " else ""}${template.name}",
                            "${if (quickWeaponBonus > 0) "+$quickWeaponBonus " else ""}${template.name} hinzugefügt",
                        )
                        quickWeaponItem = null
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(state.t("Add weapon", "Waffe hinzufügen")) }
                TextButton(
                    onClick = {
                        prefill = item.copy(weapon = template.copy(itemBonus = quickWeaponBonus))
                        quickWeaponItem = null
                        page = BrowserPage.CustomWeapon
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(state.t("Customize", "Anpassen")) }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ItemCatalogPage(
    state: DndAppState,
    target: ItemBrowserTarget,
    ruleset: Ruleset,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedTypes: Set<KnownItemType>,
    selectedRarities: Set<ItemRarity>,
    selectedSources: Set<KnownItemSource>,
    includeIncompatible: Boolean,
    sort: KnownItemSort,
    ascending: Boolean,
    onIncomplete: (KnownItemUi) -> Unit,
    onAdd: (KnownItemUi) -> Unit,
    onInfo: (KnownItemUi) -> Unit,
) {
    val catalog = state.knownItemCatalog().filter { item ->
        target == ItemBrowserTarget.Inventory || item.type != KnownItemType.Currency
    }
    val visible = filterKnownItems(
        items = catalog,
        ruleset = ruleset,
        language = state.language,
        query = query,
        types = selectedTypes,
        rarities = selectedRarities,
        sources = selectedSources,
        includeIncompatible = includeIncompatible,
        sort = sort,
        ascending = ascending,
    )
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {{
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = state.t("Clear search", "Suche leeren"))
                }
            }} else null,
            placeholder = { Text(state.t("Search all known items", "Alle bekannten Gegenstände durchsuchen")) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )
        state.itemBrowserFeedback?.let { feedback ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(feedback, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (target == ItemBrowserTarget.StartingArmor && (query.isBlank() || state.t("Unarmored", "Ungerüstet").contains(query, true))) {
                item("unarmored") {
                    ItemResultCard(
                        state = state,
                        name = state.t("Unarmored", "Ungerüstet"),
                        metadata = state.t("Use the character's unarmored calculation", "Rüstungslosen Wert des Charakters verwenden"),
                        details = "",
                        warning = null,
                        complete = true,
                        onClick = state::selectCreationUnarmored,
                        onInfo = null,
                    )
                }
            }
            if (visible.isEmpty()) {
                item("empty") {
                    Text(
                        state.t("No items match this search and filter.", "Keine Gegenstände passen zu Suche und Filter."),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(visible, key = KnownItemUi::id) { item ->
                val warning = state.itemCompatibilityHint(item, ruleset)
                ItemResultCard(
                    state = state,
                    name = item.currencyDenomination?.let { denomination ->
                        "${denomination.localizedName(state.language)} (${denomination.shortCode})"
                    } ?: item.name,
                    metadata = if (item.currencyDenomination != null) {
                        itemTypeLabel(state, item.type)
                    } else {
                        "${itemTypeLabel(state, item.type)} · ${rarityLabel(state, item.rarity)} · ${sourceLabel(state, item.source)}"
                    },
                    details = item.details,
                    warning = warning,
                    complete = item.complete,
                    onClick = { if (!item.complete) onIncomplete(item) else onAdd(item) },
                    onInfo = if (item.currencyDenomination == null) {{ onInfo(item) }} else null,
                )
            }
        }
    }
}

@Composable
private fun ItemResultCard(
    state: DndAppState,
    name: String,
    metadata: String,
    details: String,
    warning: String?,
    complete: Boolean,
    onClick: () -> Unit,
    onInfo: (() -> Unit)?,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (warning != null) Icons.Rounded.Warning else if (complete) Icons.Rounded.Backpack else Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (warning != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(metadata, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (warning != null) Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!complete) Text(state.t("Complete the missing details before adding.", "Ergänze vor dem Hinzufügen die fehlenden Angaben."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (onInfo != null) {
                IconButton(onClick = onInfo) {
                    Icon(Icons.Rounded.Info, contentDescription = state.t("Item details", "Gegenstandsdetails"))
                }
            }
            Icon(if (complete) Icons.Rounded.Add else Icons.Rounded.Edit, contentDescription = null, Modifier.size(20.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ItemFiltersSheet(
    state: DndAppState,
    target: ItemBrowserTarget,
    selectedTypes: Set<KnownItemType>,
    onTypesChange: (Set<KnownItemType>) -> Unit,
    selectedRarities: Set<ItemRarity>,
    onRaritiesChange: (Set<ItemRarity>) -> Unit,
    selectedSources: Set<KnownItemSource>,
    onSourcesChange: (Set<KnownItemSource>) -> Unit,
    includeIncompatible: Boolean,
    onIncludeIncompatibleChange: (Boolean) -> Unit,
    sort: KnownItemSort,
    onSortChange: (KnownItemSort) -> Unit,
    ascending: Boolean,
    onAscendingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.t("Filter and sort", "Filtern und sortieren"), style = MaterialTheme.typography.titleLarge)
            if (target != ItemBrowserTarget.StartingArmor) {
                Text(state.t("Type", "Typ"), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(KnownItemType.entries) { type ->
                        FilterChip(
                            selected = type in selectedTypes,
                            onClick = { onTypesChange(selectedTypes.toggle(type)) },
                            label = { Text(itemTypeLabel(state, type)) },
                        )
                    }
                }
            }
            Text(state.t("Rarity", "Seltenheit"), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(ItemRarity.entries) { rarity ->
                    FilterChip(
                        selected = rarity in selectedRarities,
                        onClick = { onRaritiesChange(selectedRarities.toggle(rarity)) },
                        label = { Text(rarityLabel(state, rarity)) },
                    )
                }
            }
            Text(state.t("Source", "Quelle"), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(KnownItemSource.entries) { source ->
                    FilterChip(
                        selected = source in selectedSources,
                        onClick = { onSourcesChange(selectedSources.toggle(source)) },
                        label = { Text(sourceLabel(state, source)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(includeIncompatible, onIncludeIncompatibleChange)
                Column {
                    Text(state.t("Include incompatible", "Inkompatible einblenden"), style = MaterialTheme.typography.titleSmall)
                    Text(state.t("They remain selectable and show why they may not fit.", "Sie bleiben wählbar und zeigen, warum sie eventuell nicht passen."), style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            Text(state.t("Sort by", "Sortieren nach"), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(KnownItemSort.entries) { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { onSortChange(option) },
                        label = { Text(sortLabel(state, option)) },
                    )
                }
            }
            OutlinedButton(onClick = { onAscendingChange(!ascending) }, modifier = Modifier.fillMaxWidth()) {
                Icon(if (ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, contentDescription = null)
                Spacer(Modifier.size(7.dp))
                Text(if (ascending) state.t("Ascending", "Aufsteigend") else state.t("Descending", "Absteigend"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text(state.t("Clear", "Zurücksetzen")) }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(state.t("Apply", "Anwenden")) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CustomEquipmentPage(
    state: DndAppState,
    target: ItemBrowserTarget,
    initial: KnownItemUi?,
    editing: EquipmentUi?,
    onAdded: () -> Unit,
) {
    val initialItem = editing ?: initial?.equipment
    var name by remember(initial?.id, editing?.id, target) { mutableStateOf(initialItem?.name.orEmpty()) }
    var details by remember(initial?.id, editing?.id, target) { mutableStateOf(initialItem?.details.orEmpty()) }
    var quantity by remember(initial?.id, editing?.id, target) { mutableStateOf((initialItem?.quantity ?: 1).toString()) }
    var kind by remember(initial?.id, editing?.id, target) { mutableStateOf(if (target == ItemBrowserTarget.StartingArmor) EquipmentKind.ARMOR else initialItem?.kind ?: EquipmentKind.GEAR) }
    var armorClass by remember(initial?.id, editing?.id, target) { mutableStateOf(initialItem?.armorClass?.toString().orEmpty()) }
    var shieldBonus by remember(initial?.id, editing?.id, target) { mutableStateOf(initialItem?.shieldBonus?.takeIf { it > 0 }?.toString().orEmpty()) }
    var attunement by remember(initial?.id, editing?.id, target) { mutableStateOf(initialItem?.needsAttunement == true) }
    var equipped by remember(initial?.id, editing?.id, target) { mutableStateOf(initialItem?.worn == true) }
    var acBonus by remember(initial?.id, editing?.id, target) {
        mutableStateOf(initialItem?.effects?.firstOrNull { it.statistic == CoreStatistic.ARMOR_CLASS }?.amount?.toString().orEmpty())
    }
    var saveBonus by remember(initial?.id, editing?.id, target) {
        mutableStateOf(initialItem?.effects?.firstOrNull { it.statistic == CoreStatistic.SAVING_THROW && it.ability == null }?.amount?.toString().orEmpty())
    }
    var detailsOpen by remember(initial?.id, editing?.id, target) { mutableStateOf(false) }
    var targetSaveAbility by remember(initial?.id, editing?.id, target) { mutableStateOf(initialItem?.savingThrows?.firstOrNull()?.ability) }
    var targetSaveDc by remember(initial?.id, editing?.id, target) { mutableStateOf(initialItem?.savingThrows?.firstOrNull()?.difficultyClass?.fixed?.toString().orEmpty()) }
    val armorValid = kind != EquipmentKind.ARMOR || initialItem != null ||
        armorClass.toIntOrNull() in 1..30 || shieldBonus.toIntOrNull() in 1..9

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        if (target != ItemBrowserTarget.StartingArmor) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(EquipmentKind.entries) { option ->
                    FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(equipmentKindLabel(state, option)) })
                }
            }
        }
        OutlinedTextField(name, { name = it.take(80) }, label = { Text(state.t("Name", "Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(details, { details = it.take(500) }, label = { Text(state.t("Useful details", "Nützliche Details")) }, modifier = Modifier.fillMaxWidth())
        if (target != ItemBrowserTarget.StartingArmor) {
            OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit).take(3) }, label = { Text(state.t("Quantity", "Menge")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    acBonus,
                    { acBonus = it.filter { char -> char.isDigit() || char == '-' }.take(2) },
                    label = { Text(state.t("AC bonus", "RK-Bonus")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    saveBonus,
                    { saveBonus = it.filter { char -> char.isDigit() || char == '-' }.take(2) },
                    label = { Text(state.t("All saves", "Alle Rettungswürfe")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (kind == EquipmentKind.ARMOR) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(armorClass, { armorClass = it.filter(Char::isDigit).take(2) }, label = { Text(state.t("Final AC", "Endgültige RK")) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(shieldBonus, { shieldBonus = it.filter(Char::isDigit).take(1) }, label = { Text(state.t("Shield bonus", "Schildbonus")) }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Text(state.t("Enter final AC for body armor, or a shield bonus for a shield.", "Gib für Körperrüstung die endgültige RK oder für einen Schild den Schildbonus ein."), style = MaterialTheme.typography.bodySmall)
        }
        if (target != ItemBrowserTarget.StartingArmor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(attunement, { attunement = it })
                Text(state.t("Needs attunement", "Benötigt Einstimmung"))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(equipped, { equipped = it })
                Text(state.t("Equipped", "Ausgerüstet"))
            }
            TextButton(onClick = { detailsOpen = !detailsOpen }) {
                Text(if (detailsOpen) state.t("Hide details", "Details ausblenden") else state.t("Details", "Details"))
            }
            if (detailsOpen) {
                Text(state.t("Saving throw caused by this item", "Rettungswurf dieses Gegenstands"), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item {
                        FilterChip(selected = targetSaveAbility == null, onClick = { targetSaveAbility = null }, label = { Text(state.t("None", "Keiner")) })
                    }
                    items(Ability.entries) { option ->
                        FilterChip(selected = targetSaveAbility == option, onClick = { targetSaveAbility = option }, label = { Text(option.shortName()) })
                    }
                }
                if (targetSaveAbility != null) {
                    OutlinedTextField(
                        targetSaveDc,
                        { targetSaveDc = it.filter(Char::isDigit).take(2) },
                        label = { Text("DC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Button(
            onClick = {
                val item = EquipmentUi(
                    id = initialItem?.id.orEmpty(),
                    name = name.trim(),
                    kind = kind,
                    quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    details = details.trim(),
                    needsAttunement = target != ItemBrowserTarget.StartingArmor && attunement,
                    attuned = initialItem?.attuned == true,
                    armorClass = armorClass.toIntOrNull()?.coerceIn(1, 30),
                    shieldBonus = shieldBonus.toIntOrNull()?.coerceIn(0, 9) ?: 0,
                    worn = target != ItemBrowserTarget.StartingArmor && equipped,
                    effects = buildList {
                        acBonus.toIntOrNull()?.takeIf { it != 0 }?.let { amount ->
                            add(CoreModifier(CoreStatistic.ARMOR_CLASS, amount, activation = if (attunement) EffectActivation.WORN_AND_ATTUNED else EffectActivation.WORN, label = "Custom AC bonus"))
                        }
                        saveBonus.toIntOrNull()?.takeIf { it != 0 }?.let { amount ->
                            add(CoreModifier(CoreStatistic.SAVING_THROW, amount, activation = if (attunement) EffectActivation.WORN_AND_ATTUNED else EffectActivation.WORN, label = "Custom saving throw bonus"))
                        }
                    },
                    savingThrows = targetSaveAbility?.let { ability ->
                        targetSaveDc.toIntOrNull()?.let { dc -> listOf(SavingThrowPrompt(ability, DifficultyClass(fixed = dc))) }
                    }.orEmpty(),
                    activeLocation = initialItem?.activeLocation ?: app.dulliesanddungeons.domain.EquipmentLocation.WORN,
                    useCase = initialItem?.useCase.orEmpty(),
                )
                if (target == ItemBrowserTarget.StartingArmor) {
                    state.setCustomCreationArmor(item)
                } else if (target == ItemBrowserTarget.StartingGear) {
                    if (item.kind == EquipmentKind.ARMOR) {
                        state.setCustomCreationArmor(item)
                    } else {
                        state.addCustomCreationEquipment(item)
                        state.itemBrowserFeedback = state.t("Added ${item.name}", "${item.name} hinzugefügt")
                        onAdded()
                    }
                } else if (editing != null) {
                    state.updateEquipment(item.copy(id = editing.id, definitionId = editing.definitionId))
                    state.closeItemBrowser()
                } else {
                    state.addEquipment(item)
                    state.itemBrowserFeedback = state.t("Added ${item.name}", "${item.name} hinzugefügt")
                    onAdded()
                }
            },
            enabled = name.isNotBlank() && armorValid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(
                when {
                    target == ItemBrowserTarget.StartingArmor -> state.t("Use this armor", "Diese Rüstung verwenden")
                    editing != null -> state.t("Save item", "Gegenstand speichern")
                    else -> state.t("Add item", "Gegenstand hinzufügen")
                },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CustomWeaponPage(state: DndAppState, initial: KnownItemUi?, editing: WeaponUi?, onAdded: () -> Unit) {
    val character = state.selectedCharacter
    if (character == null && state.itemBrowserTarget != ItemBrowserTarget.StartingGear) return
    val template = initial?.weapon
    val entry = initial?.privateEntry
    val importedDamage = template?.damage ?: entry?.formula?.let { Regex("\\b\\d+d\\d+(?:\\s*[+-]\\s*\\d+)?\\b").find(it)?.value }
    var name by remember(initial?.id, editing?.id) { mutableStateOf(editing?.name ?: template?.name ?: entry?.name.orEmpty()) }
    var ability by remember(initial?.id, editing?.id) { mutableStateOf(editing?.ability ?: template?.ability ?: entry?.formulaValue("ability")?.uppercase()?.takeIf { it in setOf("STR", "DEX") } ?: "STR") }
    var damage by remember(initial?.id, editing?.id) { mutableStateOf(editing?.damage?.substringBefore('·')?.trim() ?: importedDamage ?: "1d8") }
    var damageType by remember(initial?.id, editing?.id) { mutableStateOf(editing?.damageType ?: template?.damageType ?: entry?.formulaValue("damage[ _-]?type").orEmpty()) }
    var range by remember(initial?.id, editing?.id) { mutableStateOf(editing?.range ?: template?.range ?: entry?.formulaValue("range").orEmpty()) }
    var properties by remember(initial?.id, editing?.id) { mutableStateOf(editing?.properties ?: template?.properties ?: entry?.formulaValue("properties").orEmpty()) }
    var mastery by remember(initial?.id, editing?.id) { mutableStateOf(editing?.mastery ?: template?.mastery ?: entry?.formulaValue("mastery").orEmpty()) }
    var itemBonusText by remember(initial?.id, editing?.id) { mutableStateOf((editing?.itemBonus ?: template?.itemBonus ?: 0).toString()) }
    var attunement by remember(initial?.id, editing?.id) { mutableStateOf(editing?.needsAttunement ?: (template?.needsAttunement == true)) }
    var equipped by remember(initial?.id, editing?.id) { mutableStateOf(editing?.equipped == true) }
    var reachText by remember(initial?.id, editing?.id) {
        mutableStateOf((editing?.reachFeet ?: if (template?.properties?.contains("reach", true) == true) 10 else 5).toString())
    }
    var detailsOpen by remember(initial?.id, editing?.id) { mutableStateOf(false) }
    var targetSaveAbility by remember(initial?.id, editing?.id) {
        mutableStateOf((editing?.savingThrows ?: template?.savingThrows)?.firstOrNull()?.ability)
    }
    var targetSaveDc by remember(initial?.id, editing?.id) {
        mutableStateOf((editing?.savingThrows ?: template?.savingThrows)?.firstOrNull()?.difficultyClass?.fixed?.toString().orEmpty())
    }
    val inheritedSavingThrow = (editing?.savingThrows ?: template?.savingThrows)
        ?.firstOrNull { it.ability == targetSaveAbility }
    val abilityScore = if (state.itemBrowserTarget == ItemBrowserTarget.StartingGear) {
        state.creationAbilityScore(ability)
    } else {
        character?.abilities?.get(ability) ?: 10
    }
    val proficiencyBonus = if (state.itemBrowserTarget == ItemBrowserTarget.StartingGear) {
        state.creationTrainedProficiency()
    } else {
        character?.proficiency ?: 0
    }
    val abilityModifier = floor((abilityScore - 10) / 2.0).toInt()
    val itemBonus = itemBonusText.toIntOrNull()?.coerceIn(-5, 5) ?: 0
    val attackBonus = abilityModifier + proficiencyBonus + itemBonus
    val formulaValid = Regex("\\d+d\\d+(?:\\s*[+-]\\s*\\d+)?|\\d+").matches(damage.trim())
    val saveValid = targetSaveAbility == null || targetSaveDc.toIntOrNull() != null || inheritedSavingThrow != null
    val finalDamage = if (Regex("[+-]\\s*\\d+").containsMatchIn(damage)) damage.trim() else when {
        abilityModifier > 0 -> "${damage.trim()} + $abilityModifier"
        abilityModifier < 0 -> "${damage.trim()} - ${abs(abilityModifier)}"
        else -> damage.trim()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, { name = it.take(80) }, label = { Text(state.t("Weapon name", "Waffenname")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(listOf("STR", "DEX")) { option -> FilterChip(selected = ability == option, onClick = { ability = option }, label = { Text(option) }) }
        }
        OutlinedTextField(damage, { damage = it.take(24) }, label = { Text(state.t("Damage formula", "Schadensformel")) }, supportingText = { Text(if (formulaValid) finalDamage else state.t("Use a formula such as 1d8", "Nutze eine Formel wie 1d8")) }, isError = !formulaValid, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(damageType, { damageType = it.take(40) }, label = { Text(state.t("Damage type", "Schadensart")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(range, { range = it.take(40) }, label = { Text(state.t("Range (optional)", "Reichweite (optional)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(reachText, { reachText = it.filter(Char::isDigit).take(3) }, label = { Text(state.t("Reach (feet)", "Reichweite (Fuß)")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(properties, { properties = it.take(120) }, label = { Text(state.t("Properties", "Eigenschaften")) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(mastery, { mastery = it.take(40) }, label = { Text(state.t("Mastery", "Meisterschaft")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(itemBonusText, { itemBonusText = it.filter { character -> character.isDigit() || character == '-' }.take(2) }, label = { Text(state.t("Magic/item bonus", "Magie-/Gegenstandsbonus")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(13.dp)) {
            Text(state.t("Attack bonus ${browserSigned(attackBonus)}", "Angriffsbonus ${browserSigned(attackBonus)}"), Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(attunement, { attunement = it })
            Text(state.t("Needs attunement", "Benötigt Einstimmung"))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(equipped, { equipped = it })
            Text(state.t("Equipped", "Ausgerüstet"))
        }
        TextButton(onClick = { detailsOpen = !detailsOpen }) {
            Text(if (detailsOpen) state.t("Hide details", "Details ausblenden") else state.t("Details", "Details"))
        }
        if (detailsOpen) {
            Text(state.t("Saving throw caused by this weapon", "Rettungswurf dieser Waffe"), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { FilterChip(selected = targetSaveAbility == null, onClick = { targetSaveAbility = null }, label = { Text(state.t("None", "Keiner")) }) }
                items(Ability.entries) { option ->
                    FilterChip(selected = targetSaveAbility == option, onClick = { targetSaveAbility = option }, label = { Text(option.shortName()) })
                }
            }
            if (targetSaveAbility != null) {
                OutlinedTextField(targetSaveDc, { targetSaveDc = it.filter(Char::isDigit).take(2) }, label = { Text("DC") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        Button(
            onClick = {
                val weapon = WeaponUi(
                        id = editing?.id ?: template?.id.orEmpty(),
                        name = name.trim(),
                        attackBonus = attackBonus,
                        damage = finalDamage,
                        damageType = damageType.trim(),
                        properties = properties.trim(),
                        ability = ability,
                        itemBonus = itemBonus,
                        range = range.trim(),
                        mastery = mastery.trim(),
                        needsAttunement = attunement,
                        attuned = editing?.attuned == true,
                        custom = true,
                        damageAbility = ability,
                        reachFeet = reachText.toIntOrNull()?.coerceIn(0, 1_000) ?: 5,
                        normalRangeFeet = range.substringBefore('/').filter(Char::isDigit).toIntOrNull(),
                        longRangeFeet = range.substringAfter('/', "").filter(Char::isDigit).toIntOrNull(),
                        equipped = equipped,
                        savingThrows = targetSaveAbility?.let { saveAbility ->
                            val fixed = targetSaveDc.toIntOrNull()
                            inheritedSavingThrow?.takeIf { fixed == null }?.let(::listOf)
                                ?: fixed?.let { listOf(SavingThrowPrompt(saveAbility, DifficultyClass(fixed = it))) }
                        }.orEmpty(),
                        effects = editing?.effects.orEmpty(),
                        useCase = editing?.useCase.orEmpty(),
                        classification = editing?.classification ?: template?.classification ?: WeaponClassification(
                            baseWeaponId = editing?.definitionId ?: editing?.id ?: name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-'),
                            combatType = if (properties.contains("ammunition", true)) WeaponCombatType.RANGED else WeaponCombatType.MELEE,
                            propertyIds = normalizedWeaponPropertyIds(properties),
                        ),
                    )
                state.itemBrowserFeedback = state.t("Added ${name.trim()}", "${name.trim()} hinzugefügt")
                if (state.itemBrowserTarget == ItemBrowserTarget.StartingGear) {
                    state.addCustomCreationWeapon(weapon)
                    onAdded()
                } else if (editing != null) {
                    state.updateWeapon(weapon.copy(definitionId = editing.definitionId))
                    state.closeItemBrowser()
                } else {
                    state.addCustomWeapon(weapon)
                    onAdded()
                }
            },
            enabled = name.isNotBlank() && damageType.isNotBlank() && formulaValid && saveValid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (editing != null) state.t("Save weapon", "Waffe speichern") else state.t("Add weapon", "Waffe hinzufügen")) }
        Spacer(Modifier.height(20.dp))
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

private fun itemTypeLabel(state: DndAppState, type: KnownItemType): String = when (type) {
    KnownItemType.Weapon -> state.t("Weapon", "Waffe")
    KnownItemType.Armor -> state.t("Armor", "Rüstung")
    KnownItemType.Gear -> state.t("Gear", "Ausrüstung")
    KnownItemType.Tool -> state.t("Tool", "Werkzeug")
    KnownItemType.Consumable -> state.t("Consumable", "Verbrauch")
    KnownItemType.Rations -> state.t("Rations", "Rationen")
    KnownItemType.Currency -> state.t("Currency", "Währung")
}

private fun equipmentKindLabel(state: DndAppState, kind: EquipmentKind): String = itemTypeLabel(state, kind.toKnownItemType())

private fun rarityLabel(state: DndAppState, rarity: ItemRarity): String = when (rarity) {
    ItemRarity.Mundane -> state.t("Mundane", "Gewöhnlich")
    ItemRarity.Common -> state.t("Common", "Häufig")
    ItemRarity.Uncommon -> state.t("Uncommon", "Ungewöhnlich")
    ItemRarity.Rare -> state.t("Rare", "Selten")
    ItemRarity.VeryRare -> state.t("Very rare", "Sehr selten")
    ItemRarity.Legendary -> state.t("Legendary", "Legendär")
    ItemRarity.Artifact -> state.t("Artifact", "Artefakt")
    ItemRarity.Unique -> state.t("Unique", "Einzigartig")
    ItemRarity.Unspecified -> state.t("Unspecified", "Nicht angegeben")
}

private fun sourceLabel(state: DndAppState, source: KnownItemSource): String = when (source) {
    KnownItemSource.BuiltIn -> state.t("Built-in", "Integriert")
    KnownItemSource.Local -> state.t("Local", "Lokal")
}

private fun sortLabel(state: DndAppState, sort: KnownItemSort): String = when (sort) {
    KnownItemSort.Name -> state.t("Name", "Name")
    KnownItemSort.Type -> state.t("Type", "Typ")
    KnownItemSort.Rarity -> state.t("Rarity", "Seltenheit")
}

private fun browserSigned(value: Int): String = if (value >= 0) "+$value" else value.toString()
