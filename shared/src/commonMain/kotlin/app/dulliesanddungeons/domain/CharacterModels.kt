package app.dulliesanddungeons.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RulesetId {
    FIFTH_EDITION_2014,
    FIFTH_EDITION_2024,
    PF2E_REMASTER,
    ;

    val isFiveEdition: Boolean
        get() = this == FIFTH_EDITION_2014 || this == FIFTH_EDITION_2024
}

@Serializable
enum class LocaleId { EN, DE }

@Serializable
enum class Ability { STRENGTH, DEXTERITY, CONSTITUTION, INTELLIGENCE, WISDOM, CHARISMA }

/** A missing entry means untrained. This keeps legacy proficiency ID sets readable. */
@Serializable
enum class ProficiencyRank(val rankBonus: Int) {
    TRAINED(2),
    EXPERT(4),
    MASTER(6),
    LEGENDARY(8),
}

@Serializable
data class ContentVersionRef(val packId: String, val version: String)

@Serializable
data class EntityRef(val packId: String, val packVersion: String, val entityId: String)

@Serializable
data class ContentSource(
    val title: String,
    val license: String,
    val attribution: String,
    val page: String? = null,
)

@Serializable
enum class ContentDistribution { BUNDLED_OPEN, USER_IMPORTED_PRIVATE }

@Serializable
data class ContentPackManifest(
    val id: String,
    val version: String,
    val ruleset: RulesetId,
    val locales: Set<LocaleId>,
    val dependencies: List<ContentVersionRef> = emptyList(),
    val sources: List<ContentSource>,
    val distribution: ContentDistribution = ContentDistribution.BUNDLED_OPEN,
    val containsRestrictedMaterial: Boolean = false,
) {
    init {
        require(!containsRestrictedMaterial || distribution == ContentDistribution.USER_IMPORTED_PRIVATE) {
            "Restricted rules text may only exist in a private, user-imported content pack"
        }
    }
}

@Serializable
data class ChoiceProvenance(
    val source: EntityRef? = null,
    val selectedAtLevel: Int? = null,
    val isCustom: Boolean = false,
    val note: String? = null,
)

@Serializable
data class ChoiceSelection(
    val id: String,
    val provenance: ChoiceProvenance = ChoiceProvenance(),
)

@Serializable
enum class CustomEntityKind {
    ANCESTRY,
    HERITAGE,
    BACKGROUND,
    CLASS,
    SUBCLASS,
    FEAT,
    PROFICIENCY,
    LANGUAGE,
    SPELL,
    FEATURE,
    WEAPON,
    EQUIPMENT,
    OTHER,
}

@Serializable
data class CustomEntitySnapshot(
    val id: String,
    val kind: CustomEntityKind,
    val name: String,
    val details: String = "",
)

data class EntityDisplayText(
    val name: String,
    val details: String = "",
)

fun interface EntityDisplayResolver {
    fun resolve(reference: EntityRef): EntityDisplayText?
}

@Serializable
data class ClassLevel(
    val classId: String,
    val levels: Int,
    val subclassId: String? = null,
)

/** Ruleset-owned build payload. Shared UI code projects this instead of persisting display models. */
@Serializable
sealed interface RulesetBuildData {
    val level: Int
    val abilities: Map<Ability, Int>
    val proficiencyRanks: Map<String, ProficiencyRank>
}

@Serializable
@SerialName("five_e")
data class FiveEBuildData(
    val ancestryId: String,
    val backgroundId: String,
    val classes: List<ClassLevel>,
    override val abilities: Map<Ability, Int>,
    val heritageId: String? = null,
    val feats: List<ChoiceSelection> = emptyList(),
    val proficiencyIds: Set<String> = emptySet(),
    override val proficiencyRanks: Map<String, ProficiencyRank> = emptyMap(),
    val languages: List<ChoiceSelection> = emptyList(),
    val knownSpells: List<ChoiceSelection> = emptyList(),
    val preparedSpellIds: Set<String> = emptySet(),
    val features: List<ChoiceSelection> = emptyList(),
    val optionalRules: FiveEOptionalRules = FiveEOptionalRules(),
    /** Character-local overrides keyed by class ID. Values are used for later levels of that class. */
    val classHitDieOverrides: Map<String, Int> = emptyMap(),
) : RulesetBuildData {
    override val level: Int get() = classes.sumOf(ClassLevel::levels)

    init {
        require(classes.map { it.classId }.distinct().size == classes.size) { "A class may appear only once" }
    }
}

@Serializable
data class FiveEOptionalRules(
    val multiclassingEnabled: Boolean = true,
    val featsEnabled: Boolean = true,
)

@Serializable
enum class Pf2eFeatCategory { ANCESTRY, CLASS, SKILL, GENERAL, ARCHETYPE, BONUS }

@Serializable
data class Pf2eFeatSelection(
    val category: Pf2eFeatCategory,
    val selection: ChoiceSelection,
    val selectedAtLevel: Int,
)

@Serializable
@SerialName("pf2e_remastered")
data class Pf2eBuildData(
    val ancestryId: String,
    val heritageId: String,
    val backgroundId: String,
    val classId: String,
    override val level: Int,
    override val abilities: Map<Ability, Int>,
    val featSelections: List<Pf2eFeatSelection> = emptyList(),
    val proficiencyIds: Set<String> = emptySet(),
    override val proficiencyRanks: Map<String, ProficiencyRank> = emptyMap(),
    val languages: List<ChoiceSelection> = emptyList(),
    val knownSpells: List<ChoiceSelection> = emptyList(),
    val preparedSpellIds: Set<String> = emptySet(),
    val features: List<ChoiceSelection> = emptyList(),
) : RulesetBuildData

@Serializable
data class CharacterBuild(
    val id: String,
    val name: String,
    val ruleset: RulesetId,
    /** Set when this document was produced from a non-destructive ruleset conversion. */
    val sourceCharacterId: String? = null,
    val locale: LocaleId = LocaleId.EN,
    val contentVersions: List<ContentVersionRef> = emptyList(),
    /** Rendered 512 px square used by lists, sheets, and the full-screen viewer. */
    val portraitFileName: String? = null,
    /** Orientation-corrected private source retained for non-destructive re-cropping. */
    val portraitSourceFileName: String? = null,
    val portraitCrop: PortraitCrop? = null,
    /** Exact pack references for public/private entities selected by this build. */
    val entityReferences: Map<String, EntityRef> = emptyMap(),
    /** Only user-authored/local text is snapshotted; licensed pack prose stays in the pack. */
    val customEntities: Map<String, CustomEntitySnapshot> = emptyMap(),
    val rules: RulesetBuildData,
) {
    init {
        require(
            (ruleset.isFiveEdition && rules is FiveEBuildData) ||
                (ruleset == RulesetId.PF2E_REMASTER && rules is Pf2eBuildData),
        ) { "Ruleset ID and rules payload must agree" }
    }

    val level: Int get() = rules.level
    val abilities: Map<Ability, Int> get() = rules.abilities
    val ancestryId: String get() = when (val value = rules) {
        is FiveEBuildData -> value.ancestryId
        is Pf2eBuildData -> value.ancestryId
    }
    val heritageId: String? get() = when (val value = rules) {
        is FiveEBuildData -> value.heritageId
        is Pf2eBuildData -> value.heritageId
    }
    val backgroundId: String get() = when (val value = rules) {
        is FiveEBuildData -> value.backgroundId
        is Pf2eBuildData -> value.backgroundId
    }
    val classes: List<ClassLevel> get() = when (val value = rules) {
        is FiveEBuildData -> value.classes
        is Pf2eBuildData -> value.classId.takeIf(String::isNotBlank)?.let { listOf(ClassLevel(it, value.level)) }.orEmpty()
    }
    val featIds: Set<String> get() = when (val value = rules) {
        is FiveEBuildData -> value.feats.mapTo(mutableSetOf()) { it.id }
        is Pf2eBuildData -> value.featSelections.mapTo(mutableSetOf()) { it.selection.id }
    }
    val proficiencyIds: Set<String> get() = when (val value = rules) {
        is FiveEBuildData -> value.proficiencyIds + value.proficiencyRanks.keys
        is Pf2eBuildData -> value.proficiencyIds + value.proficiencyRanks.keys
    }
    val proficiencyRanks: Map<String, ProficiencyRank> get() = rules.proficiencyRanks
    val languageIds: Set<String> get() = when (val value = rules) {
        is FiveEBuildData -> value.languages.mapTo(mutableSetOf()) { it.id }
        is Pf2eBuildData -> value.languages.mapTo(mutableSetOf()) { it.id }
    }
    val knownSpellIds: Set<String> get() = when (val value = rules) {
        is FiveEBuildData -> value.knownSpells.mapTo(mutableSetOf()) { it.id }
        is Pf2eBuildData -> value.knownSpells.mapTo(mutableSetOf()) { it.id }
    }
    val preparedSpellIds: Set<String> get() = when (val value = rules) {
        is FiveEBuildData -> value.preparedSpellIds
        is Pf2eBuildData -> value.preparedSpellIds
    }
    val featureIds: Set<String> get() = when (val value = rules) {
        is FiveEBuildData -> value.features.mapTo(mutableSetOf()) { it.id }
        is Pf2eBuildData -> value.features.mapTo(mutableSetOf()) { it.id }
    }
    val choiceProvenance: Map<String, ChoiceProvenance> get() = when (val value = rules) {
        is FiveEBuildData -> value.feats + value.languages + value.knownSpells + value.features
        is Pf2eBuildData -> value.featSelections.map { it.selection } + value.languages + value.knownSpells + value.features
    }.associate { it.id to it.provenance }

    fun displayText(entityId: String, resolver: EntityDisplayResolver): EntityDisplayText? {
        customEntities[entityId]?.let { return EntityDisplayText(it.name, it.details) }
        return entityReferences[entityId]?.let(resolver::resolve)
    }
}

@Serializable
data class PortraitCrop(
    val rotationQuarterTurns: Int = 0,
    val centerXFraction: Float = 0.5f,
    val centerYFraction: Float = 0.5f,
    val sizeFractionOfShortEdge: Float = 1f,
)

@Serializable
enum class HitPointGainMethod { FIRST_LEVEL_MAXIMUM, ROLLED, FIXED, MANUAL_OVERRIDE }

@Serializable
data class HitPointGainRecord(
    val method: HitPointGainMethod,
    val dieSides: Int,
    val dieFace: Int? = null,
    val constitutionModifier: Int,
    val totalGain: Int,
)

@Serializable
data class LevelProgressionEntry(
    val characterLevel: Int,
    val classId: String,
    val classLevel: Int,
    val hitPoints: HitPointGainRecord,
    val abilityScoreChanges: Map<Ability, Int> = emptyMap(),
    val selections: List<ChoiceSelection> = emptyList(),
    val importedOrManual: Boolean = false,
    val applyHitPointGainToCurrent: Boolean = true,
)

@Serializable
data class ProgressionLedger(
    /** Imported characters can establish a baseline without fabricating historical choices. */
    val baselineLevel: Int = 0,
    val baselineClassLevels: Map<String, Int> = emptyMap(),
    val entries: List<LevelProgressionEntry> = emptyList(),
)

@Serializable
enum class LevelUpStep { CLASS, HIT_POINTS, ABILITY_OR_FEAT, FEATURES, SPELLS, REVIEW }

@Serializable
data class LevelUpDraft(
    val characterId: String,
    val ruleset: RulesetId,
    val fromLevel: Int,
    val targetRules: RulesetBuildData,
    val step: LevelUpStep = LevelUpStep.CLASS,
    val hitPointGain: HitPointGainRecord? = null,
    val abilityScoreChanges: Map<Ability, Int> = emptyMap(),
    val selections: List<ChoiceSelection> = emptyList(),
    val completedSteps: Set<LevelUpStep> = emptySet(),
)

@Serializable
data class DerivedStatisticFormula(
    val ability: Ability? = null,
    val proficiencyMultiplier: Int = 0,
    /** Stable save/skill/category ID whose rank supplies the proficiency modifier. */
    val proficiencyId: String? = null,
    val base: Int = 0,
    val itemBonus: Int = 0,
    val override: Int? = null,
    /** Last projected total, retained independently from whether the formula is automatic. */
    val storedValue: Int? = null,
)

@Serializable
sealed interface ArmorClassMethod {
    @Serializable
    @SerialName("automatic")
    data object Automatic : ArmorClassMethod

    @Serializable
    @SerialName("formula")
    data class Formula(
        val base: Int = 10,
        val ability: Ability? = Ability.DEXTERITY,
        val abilityCap: Int? = null,
        val flatBonus: Int = 0,
    ) : ArmorClassMethod

    @Serializable
    @SerialName("manual")
    data class Manual(val value: Int) : ArmorClassMethod
}

@Serializable
data class CombatProfile(
    val baseSpeedsFeet: Map<MovementMode, Int> = mapOf(MovementMode.WALK to 30),
    val armorClassMethod: ArmorClassMethod = ArmorClassMethod.Automatic,
    val unarmoredArmorClass: Int? = null,
    val proficiencyBonusOverride: Int? = null,
    val storedProficiencyBonus: Int? = null,
    val criticalHitThreshold: Int = 20,
    val initiativeRollMode: RollMode = RollMode.NORMAL,
    val initiative: DerivedStatisticFormula = DerivedStatisticFormula(Ability.DEXTERITY),
    val savingThrows: Map<Ability, DerivedStatisticFormula> = emptyMap(),
    val skills: Map<String, DerivedStatisticFormula> = emptyMap(),
)

@Serializable
data class WeaponRecord(
    val id: String,
    val name: String,
    val attackAbility: Ability,
    val damageAbility: Ability? = attackAbility,
    val damage: DiceExpression,
    val damageType: String,
    val proficient: Boolean = true,
    /** Stable weapon category or specific training ID used by automatic attack calculations. */
    val proficiencyId: String? = null,
    val itemBonus: Int = 0,
    val attackBonusOverride: Int? = null,
    val damageModifierOverride: Int? = null,
    val abilityModifierOverride: Int? = null,
    val storedAttackBonus: Int? = null,
    val damageFormula: String? = null,
    val range: String = "",
    val properties: Set<String> = emptySet(),
    val masteryId: String? = null,
    val attunement: AttunementState = AttunementState.NOT_REQUIRED,
    val custom: Boolean = false,
    val source: EntityRef? = null,
)

@Serializable
enum class SpellSourceKind { CLASS, FEATURE, ITEM, CUSTOM }

@Serializable
data class SpellRecord(
    val id: String,
    val name: String,
    val level: Int,
    val prepared: Boolean = true,
    val sourceKind: SpellSourceKind = SpellSourceKind.CLASS,
    val sourceId: String? = null,
    val sourceName: String = "",
    val summary: String = "",
    val activationCost: ActionCost = ActionCost(actions = 1),
    /** Compact, localized cast-level preview keyed by the slot level used. */
    val castPreviews: Map<Int, String> = emptyMap(),
    val source: EntityRef? = null,
)

@Serializable
data class FeatureRecord(
    val id: String,
    val name: String,
    val summary: String = "",
    val resourceId: String? = null,
    /** Number of units removed from [resourceId] when the feature is used. */
    val resourceCost: Int = 1,
    val activationIds: List<String> = emptyList(),
    val actionCost: ActionCost = ActionCost(),
    val effectKey: String = "resource_only",
    val custom: Boolean = false,
    val notes: String = "",
    val turnGuideEligible: Boolean = true,
    val source: EntityRef? = null,
)

@Serializable
data class LanguageRecord(
    val id: String,
    val name: String,
    val custom: Boolean = false,
    val locked: Boolean = false,
    val source: EntityRef? = null,
)

@Serializable
data class CharacterNote(
    val id: String,
    val title: String,
    val body: String = "",
)

@Serializable
data class CharacterProfile(
    val characterDescription: String = "",
    val motive: String = "",
    val alignment: String = "",
)

@Serializable
data class CharacterSheetData(
    val portraitSeed: Int = 0,
    /** Legacy single-note field retained so existing schema-v2 app state still decodes. */
    val notes: String = "",
    val noteEntries: List<CharacterNote> = emptyList(),
    val profile: CharacterProfile = CharacterProfile(),
    /** Spellcasting remains visible even before the first spell is selected. */
    val spellcastingSourceIds: Set<String> = emptySet(),
    val combat: CombatProfile = CombatProfile(),
    val weapons: List<WeaponRecord> = emptyList(),
    val spells: List<SpellRecord> = emptyList(),
    val features: List<FeatureRecord> = emptyList(),
    val languages: List<LanguageRecord> = emptyList(),
)

@Serializable
data class CharacterDocument(
    val schemaVersion: Int = 1,
    val build: CharacterBuild,
    val state: CharacterState,
    val progression: ProgressionLedger = ProgressionLedger(),
    val sheet: CharacterSheetData = CharacterSheetData(),
)

@Serializable
enum class Recovery { TURN_START, SHORT_REST, LONG_REST, DAILY_PREPARATION, MANUAL }

@Serializable
data class ResourcePool(
    val id: String,
    val label: String,
    val current: Int,
    val maximum: Int,
    val recoveryRules: List<RecoveryRule> = emptyList(),
    /** Die represented by one unit of this pool, when the pool contains dice. */
    val dieSides: Int? = null,
)

@Serializable
data class EffectSource(val label: String? = null, val entity: EntityRef? = null)

@Serializable
data class ActiveCondition(
    /** Definition ID; multiple instances may share it when different sources remain active. */
    val id: String,
    val instanceId: String = id,
    val source: EffectSource? = null,
    val displayName: String? = null,
    val intensity: Int = 1,
    val remainingRounds: Int? = null,
    val expiresAt: ExpiryTrigger = ExpiryTrigger.MANUAL,
    val saveToEnd: RollRequest? = null,
    val requiresConcentration: Boolean = false,
    val requiresSustain: Boolean = false,
    val removable: Boolean = true,
    val durationLabel: String? = null,
    val note: String? = null,
)

@Serializable
enum class ExpiryTrigger { START_OF_TURN, END_OF_TURN, SAVE_ENDS, CONCENTRATION_ENDS, MANUAL }

@Serializable
enum class EquipmentLocation { CARRIED, WORN, WIELDED, HELD, STOWED }

@Serializable
enum class EquipmentSlot { ARMOR, SHIELD, MAIN_HAND, OFF_HAND, BOTH_HANDS, OTHER }

@Serializable
enum class AttunementState { NOT_REQUIRED, UNATTUNED, ATTUNED, INVESTED }

@Serializable
enum class EquipmentCategory { GEAR, ARMOR, TOOL, CONSUMABLE, RATIONS }

@Serializable
data class EquipmentItem(
    /** Stable inventory-instance ID; several instances can reference the same definition. */
    val id: String,
    val definitionId: String = id,
    val name: String,
    val category: EquipmentCategory = EquipmentCategory.GEAR,
    val quantity: Int = 1,
    val location: EquipmentLocation = EquipmentLocation.CARRIED,
    val slot: EquipmentSlot = EquipmentSlot.OTHER,
    val attunement: AttunementState = AttunementState.NOT_REQUIRED,
    val ammunition: Int? = null,
    val charges: ResourcePool? = null,
    val weightOrBulk: String? = null,
    val details: String = "",
    val armorClass: Int? = null,
    val shieldBonus: Int = 0,
    val grantedSpells: List<SpellRecord> = emptyList(),
    val customProperties: Map<String, String> = emptyMap(),
) {
    val equipped: Boolean get() = location == EquipmentLocation.WORN ||
        location == EquipmentLocation.WIELDED ||
        location == EquipmentLocation.HELD
    val attunedOrInvested: Boolean get() = attunement == AttunementState.ATTUNED ||
        attunement == AttunementState.INVESTED
}

@Serializable
enum class HealthStatus { ALIVE, DOWNED, STABLE, DEAD }

@Serializable
enum class DeathReason { DEATH_SAVE_FAILURES, MASSIVE_DAMAGE, EXHAUSTION, MAXIMUM_HIT_POINTS_ZERO, MANUAL }

@Serializable
sealed interface RulesetHealthState

@Serializable
@SerialName("five_e")
data class FiveEHealthState(
    val deathSaveSuccesses: Int = 0,
    val deathSaveFailures: Int = 0,
    val stable: Boolean = false,
    val exhaustionLevel: Int = 0,
    val deathReason: DeathReason? = null,
    val deathNote: String? = null,
) : RulesetHealthState

@Serializable
@SerialName("pf2e")
data class Pf2eHealthState(
    val dying: Int = 0,
    val wounded: Int = 0,
    val doomed: Int = 0,
    val heroPoints: Int = 0,
    val dead: Boolean = false,
) : RulesetHealthState

@Serializable
enum class QuickRollKind { INITIATIVE, PERCEPTION, ABILITY_CHECK, SKILL, SAVING_THROW, ATTACK, CUSTOM }

@Serializable
data class QuickRollShortcut(
    val kind: QuickRollKind,
    val entityId: String,
    val ability: Ability? = null,
    val label: String = entityId,
)

@Serializable
data class ActivityRecord(
    val id: String,
    val sequence: Long,
    val label: String,
    val turnNumber: Int? = null,
    val turnEvent: TurnEvent? = null,
    val roll: DiceRoll? = null,
    val correctsActivityId: String? = null,
)

@Serializable
data class PlaySessionRecord(
    val id: String,
    val ordinal: Int,
    val title: String = "",
    val startedAtEpochMillis: Long,
    val savedAtEpochMillis: Long? = null,
    val currentTurnNumber: Int = 1,
    val activities: List<ActivityRecord> = emptyList(),
)

/** Transaction rollback payload without recursive draft/history fields. */
@Serializable
data class CharacterRollbackSnapshot(
    val build: CharacterBuild,
    val progression: ProgressionLedger,
    val sheet: CharacterSheetData,
    val currentHitPoints: Int,
    val maximumHitPoints: Int,
    val temporaryHitPoints: Int,
    val health: RulesetHealthState,
    val resources: List<ResourcePool>,
    val conditions: List<ActiveCondition>,
    val equipment: List<EquipmentItem>,
    val quickRolls: List<QuickRollShortcut>,
    val spellSlotMaximumOverrides: Map<Int, Int> = emptyMap(),
    val spellSlotSpentCounts: Map<Int, Int> = emptyMap(),
    val hasPlayedSinceLongRest: Boolean = false,
    val maximumHitPointReduction: Int = 0,
)

@Serializable
data class CharacterState(
    val characterId: String,
    val currentHitPoints: Int,
    /** Base maximum before temporary effects such as 2014 Exhaustion are applied. */
    val maximumHitPoints: Int,
    val temporaryHitPoints: Int = 0,
    /** Temporary reduction applied to the base maximum before other ruleset modifiers. */
    val maximumHitPointReduction: Int = 0,
    val health: RulesetHealthState = FiveEHealthState(),
    val resources: List<ResourcePool> = emptyList(),
    val conditions: List<ActiveCondition> = emptyList(),
    val equipment: List<EquipmentItem> = emptyList(),
    val quickRolls: List<QuickRollShortcut> = defaultQuickRolls(),
    val activeTurn: TurnDraft? = null,
    val activeLevelUp: LevelUpDraft? = null,
    val activityHistory: List<ActivityRecord> = emptyList(),
    val activePlaySession: PlaySessionRecord? = null,
    val savedPlaySessions: List<PlaySessionRecord> = emptyList(),
    /** Manual replacements for rules-derived spell-slot maxima, keyed by slot level. */
    val spellSlotMaximumOverrides: Map<Int, Int> = emptyMap(),
    /** Spent slots retained separately so lowering a maximum never erases expended-slot history. */
    val spellSlotSpentCounts: Map<Int, Int> = emptyMap(),
    /** UX bookkeeping: a Long Rest is useful after at least one play activity. */
    val hasPlayedSinceLongRest: Boolean = false,
)

fun defaultQuickRolls(): List<QuickRollShortcut> = listOf(
    QuickRollShortcut(QuickRollKind.INITIATIVE, "initiative", Ability.DEXTERITY),
    QuickRollShortcut(QuickRollKind.PERCEPTION, "perception", Ability.WISDOM),
    QuickRollShortcut(QuickRollKind.SAVING_THROW, "save:dexterity", Ability.DEXTERITY),
    QuickRollShortcut(QuickRollKind.SAVING_THROW, "save:constitution", Ability.CONSTITUTION),
)

@Serializable
data class ValidationIssue(
    val code: String,
    val message: String,
    val path: String? = null,
    val severity: Severity = Severity.ERROR,
)

@Serializable
enum class Severity { INFO, WARNING, ERROR }

@Serializable
data class DerivedStats(
    val armorClass: Int,
    val proficiencyBonus: Int,
    val initiative: Int,
    val savingThrows: Map<Ability, Int>,
    val speedsFeet: Map<MovementMode, Int>,
    val effectiveMaximumHitPoints: Int? = null,
    val healthStatus: HealthStatus = HealthStatus.ALIVE,
    val explanations: Map<String, List<CalculationPart>> = emptyMap(),
)

@Serializable
data class CalculationPart(
    val label: String,
    val value: Int,
    val source: String? = null,
)
