package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Recovery
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PrivateContentRequirementUi(
    val id: String,
    val version: String,
)

@Serializable
data class InstalledPrivatePackUi(
    val id: String,
    val version: String,
    val requires: List<PrivateContentRequirementUi> = emptyList(),
)

@Serializable
data class PrivateContentDocument(
    val schemaVersion: Int,
    val id: String,
    val version: String,
    val ruleset: String,
    val locale: String,
    val requires: List<PrivateContentRequirementUi> = emptyList(),
    val entries: List<PrivateContentEntryUi>,
)

@Serializable
data class PrivateContentEntryUi(
    val id: String,
    val kind: String,
    val name: String,
    val summary: String = "",
    val aliases: List<String> = emptyList(),
    val mechanics: PrivateMechanicsUi = PrivateMechanicsUi(),
)

fun PrivateContentEntryUi.toPrivateEntry(
    packId: String,
    packVersion: String,
): PrivateEntryUi = PrivateEntryUi(
    id = id,
    kind = kind,
    name = name,
    summary = resolvedPrivateContentSummary(kind, summary, mechanics),
    sourceNote = "Imported data",
    aliases = aliases,
    mechanics = mechanics,
    sourcePackId = packId,
    sourcePackVersion = packVersion,
)

/**
 * Keeps authored rules summaries intact while replacing known import placeholders with an honest
 * mechanics-only synopsis. Missing effect text is called out explicitly rather than inferred from
 * a feature name.
 */
internal fun PrivateEntryUi.privateContentSummary(): String =
    resolvedPrivateContentSummary(kind, summary, mechanics)

private fun resolvedPrivateContentSummary(
    kind: String,
    summary: String,
    mechanics: PrivateMechanicsUi,
): String {
    val supplied = summary.trim()
    if (supplied.isNotEmpty() && !GENERIC_FEATURE_SUMMARY.matches(supplied)) return supplied
    if (kind.trim().lowercase() !in SUMMARY_FALLBACK_KINDS) return supplied

    val facts = buildList {
        mechanics.actionCost.toImportSummary()?.let(::add)
        mechanics.resource?.let { resource ->
            val uses = if (resource.maximum == 1) "1 use" else "${resource.maximum} uses"
            add("Tracks $uses; ${resource.recovery.importSummary()} recovery")
        }
    }
    val missingEffect = "Effect details were not supplied by the imported content pack."
    return if (facts.isEmpty()) missingEffect else facts.joinToString(". ", postfix = ". $missingEffect")
}

private fun ActionCost.toImportSummary(): String? {
    val timings = buildList {
        if (actions > 0) add(actions.countedLabel("Action"))
        if (bonusActions > 0) add(bonusActions.countedLabel("Bonus Action"))
        if (reactions > 0) add(reactions.countedLabel("Reaction"))
        if (attacks > 0) add(attacks.countedLabel("Attack"))
        if (objectInteractions > 0) add(objectInteractions.countedLabel("Object Interaction"))
        if (pf2eActions > 0) add(pf2eActions.countedLabel("PF2e Action"))
    }
    val resourceCosts = resources.entries
        .filter { it.value > 0 }
        .joinToString { (id, amount) -> "$amount ${id.toReadableImportName()}" }
        .takeIf(String::isNotEmpty)
    return buildList {
        if (timings.isNotEmpty()) add("Activation: ${timings.joinToString()}")
        resourceCosts?.let { add("Costs: $it") }
    }.joinToString(". ").takeIf(String::isNotEmpty)
}

private fun Int.countedLabel(label: String): String = if (this == 1) label else "$this ${label}s"

private fun String.toReadableImportName(): String = split('-', '_')
    .filter(String::isNotBlank)
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

private fun Recovery.importSummary(): String = when (this) {
    Recovery.TURN_START -> "Turn Start"
    Recovery.SHORT_REST -> "Short Rest"
    Recovery.LONG_REST -> "Long Rest"
    Recovery.DAILY_PREPARATION -> "Daily Preparation"
    Recovery.MANUAL -> "manual"
}

@Serializable
data class PrivateResourceMechanicsUi(
    val maximum: Int,
    val recovery: Recovery = Recovery.MANUAL,
)

@Serializable
data class PrivateSpellMechanicsUi(
    val level: Int,
    val school: String = "",
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val castingTime: String = "",
    val range: String = "",
    val components: String = "",
    val duration: String = "",
    val spellAttack: Boolean = false,
    val saveAbility: String? = null,
    val actionCost: ActionCost = ActionCost(actions = 1),
    val castPreviews: Map<Int, String> = emptyMap(),
)

@Serializable
data class PrivateItemMechanicsUi(
    val type: String = "gear",
    val damage: String? = null,
    val damageType: String? = null,
    val ability: String = "STR",
    val properties: String = "",
    val range: String = "",
    val mastery: String = "",
    val armorClass: Int? = null,
    val shieldBonus: Int = 0,
    val rarity: String = "mundane",
    val requiresAttunement: Boolean = false,
    val quantity: Int = 1,
)

/**
 * Compact player-side mechanics only. Conditional outcomes and target-facing resolution remain
 * table decisions; the app tracks unlocks, costs, resources, rests, and unconditional self stats.
 */
@Serializable
data class PrivateMechanicsUi(
    val parentClassId: String? = null,
    val parentSubclassId: String? = null,
    val parentSpeciesId: String? = null,
    val selectionLevel: Int? = null,
    val unlockLevel: Int? = null,
    val classIds: List<String> = emptyList(),
    val hitDie: Int? = null,
    val primaryAbility: String? = null,
    val caster: Boolean? = null,
    val grantedSkillIds: List<String> = emptyList(),
    val skillChoiceCount: Int = 0,
    val originFeatId: String? = null,
    val speedFeet: Int? = null,
    val actionCost: ActionCost = ActionCost(),
    val resource: PrivateResourceMechanicsUi? = null,
    val spell: PrivateSpellMechanicsUi? = null,
    val item: PrivateItemMechanicsUi? = null,
    val grantedSpellIds: List<String> = emptyList(),
    val grantAutomatically: Boolean = false,
)

private val privateContentJson = Json {
    ignoreUnknownKeys = false
    isLenient = false
    explicitNulls = false
}

fun decodePrivateContent(raw: String): PrivateContentDocument =
    privateContentJson.decodeFromString<PrivateContentDocument>(raw).also(::validatePrivateContent)

fun validatePrivateContent(document: PrivateContentDocument) {
    require(document.schemaVersion == 1) { "content-schema-version" }
    require(PRIVATE_ID.matches(document.id)) { "content-pack-id" }
    require(PRIVATE_VERSION.matches(document.version)) { "content-pack-version" }
    require(document.ruleset == "2024") { "content-ruleset" }
    require(document.locale == "en") { "content-locale" }
    require(document.entries.size in 1..MAX_PRIVATE_ENTRIES) { "content-entry-count" }
    require(document.requires.size <= 32) { "content-requirement-count" }
    document.requires.forEach { dependency ->
        require(PRIVATE_ID.matches(dependency.id) && PRIVATE_VERSION.matches(dependency.version)) { "content-requirement" }
        require(dependency.id != document.id) { "content-self-requirement" }
    }

    val entriesById = document.entries.associateBy(PrivateContentEntryUi::id)
    require(entriesById.size == document.entries.size) { "content-duplicate-entry" }
    document.entries.forEach { entry ->
        require(PRIVATE_ID.matches(entry.id)) { "content-entry-id" }
        require(entry.kind.lowercase() in PRIVATE_ENTRY_KINDS) { "content-entry-kind" }
        require(entry.name.isNotBlank() && entry.name.length <= 120) { "content-entry-name" }
        require(entry.summary.length <= 1_200) { "content-entry-text" }
        require(entry.aliases.size <= 24 && entry.aliases.all { it.isNotBlank() && it.length <= 120 }) { "content-entry-aliases" }
        val mechanics = entry.mechanics
        mechanics.parentClassId?.let { require(entriesById[it]?.kind.equals("class", true)) { "content-parent-class" } }
        mechanics.parentSubclassId?.let { require(entriesById[it]?.kind.equals("subclass", true)) { "content-parent-subclass" } }
        mechanics.parentSpeciesId?.let { require(entriesById[it]?.kind.equals("species", true)) { "content-parent-species" } }
        mechanics.originFeatId?.let { require(entriesById[it]?.kind.equals("feat", true)) { "content-origin-feat" } }
        mechanics.classIds.forEach { require(entriesById[it]?.kind.equals("class", true)) { "content-spell-class" } }
        mechanics.grantedSpellIds.forEach { require(entriesById[it]?.kind.equals("spell", true)) { "content-granted-spell" } }
        mechanics.selectionLevel?.let { require(it in 1..20) { "content-selection-level" } }
        mechanics.unlockLevel?.let { require(it in 1..20) { "content-unlock-level" } }
        mechanics.hitDie?.let { require(it in setOf(6, 8, 10, 12)) { "content-hit-die" } }
        mechanics.primaryAbility?.let { require(it in PRIVATE_ABILITIES) { "content-primary-ability" } }
        require(mechanics.skillChoiceCount in 0..18) { "content-skill-choice-count" }
        require(mechanics.grantedSkillIds.size <= 18 && mechanics.grantedSkillIds.all { it.length in 1..80 }) { "content-granted-skills" }
        mechanics.speedFeet?.let { require(it in 0..200) { "content-speed" } }
        mechanics.resource?.let { require(it.maximum in 1..999) { "content-resource-maximum" } }
        validateActionCost(mechanics.actionCost)
        mechanics.spell?.let { spell ->
            require(spell.level in 0..9) { "content-spell-level" }
            require(spell.saveAbility == null || spell.saveAbility in PRIVATE_ABILITIES) { "content-spell-save" }
            require(spell.castPreviews.keys.all { it in spell.level.coerceAtLeast(1)..9 }) { "content-spell-preview-level" }
            require(listOf(spell.school, spell.castingTime, spell.range, spell.components, spell.duration).all { it.length <= 120 }) { "content-spell-text" }
            validateActionCost(spell.actionCost)
        }
        mechanics.item?.let { item ->
            require(item.type in PRIVATE_ITEM_TYPES && item.quantity in 1..999) { "content-item" }
            require(item.ability in PRIVATE_ABILITIES) { "content-item-ability" }
            require(item.armorClass == null || item.armorClass in 1..30) { "content-item-armor-class" }
            require(item.shieldBonus in 0..9) { "content-item-shield" }
        }
    }
}

private fun validateActionCost(cost: ActionCost) {
    require(cost.actions in 0..10 && cost.bonusActions in 0..1 && cost.reactions in 0..1) { "content-action-cost" }
    require(cost.attacks in 0..10 && cost.objectInteractions in 0..10 && cost.pf2eActions in 0..3) { "content-action-cost" }
    require(cost.resources.all { (id, amount) -> PRIVATE_ID.matches(id) && amount in 1..20 }) { "content-action-resources" }
}

private val PRIVATE_ID = Regex("^[a-z0-9][a-z0-9._-]{1,79}$")
private val PRIVATE_VERSION = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(?:-[a-z0-9.-]+)?$")
private val GENERIC_FEATURE_SUMMARY = Regex(
    "^(?:.+?\\s+)?feature\\s+(?:unlocked|gained)\\s+at\\s+class\\s+level\\s+\\d+\\.?$",
    RegexOption.IGNORE_CASE,
)
private val SUMMARY_FALLBACK_KINDS = setOf(
    "feat", "feature", "class feature", "species feature", "racial feature", "trait", "resource",
)
private val PRIVATE_ABILITIES = setOf("STR", "DEX", "CON", "INT", "WIS", "CHA")
private val PRIVATE_ITEM_TYPES = setOf("weapon", "armor", "shield", "gear", "tool", "consumable", "rations", "mount", "vehicle", "magic-item")
private val PRIVATE_ENTRY_KINDS = setOf(
    "class", "subclass", "species", "background", "feat", "feature", "spell", "weapon",
    "armor", "item", "tool", "gear", "mount", "vehicle", "magic-item", "action", "condition",
)
private const val MAX_PRIVATE_ENTRIES = 5_000
