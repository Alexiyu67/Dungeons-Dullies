package app.dulliesanddungeons.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.dulliesanddungeons.data.LocalStateStore
import app.dulliesanddungeons.data.PersistedAppState
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.ActiveConcentration
import app.dulliesanddungeons.domain.ActivityRecord
import app.dulliesanddungeons.domain.AttackOutcomeRecord
import app.dulliesanddungeons.domain.CharacterNote
import app.dulliesanddungeons.domain.CharacterProfile
import app.dulliesanddungeons.domain.CoinDenomination
import app.dulliesanddungeons.domain.CombatContribution
import app.dulliesanddungeons.domain.CurrencyPurse
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.DiceRoll
import app.dulliesanddungeons.domain.DerivedAttackGrant
import app.dulliesanddungeons.domain.CoreModifier
import app.dulliesanddungeons.domain.DifficultyClass
import app.dulliesanddungeons.domain.EquipmentLocation
import app.dulliesanddungeons.domain.HitPointChangeKind
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.PlaySessionRecord
import app.dulliesanddungeons.domain.PortraitCrop
import app.dulliesanddungeons.domain.ProficiencyRank
import app.dulliesanddungeons.domain.Recovery
import app.dulliesanddungeons.domain.RollMode
import app.dulliesanddungeons.domain.RollRequest
import app.dulliesanddungeons.domain.SavingThrowPrompt
import app.dulliesanddungeons.domain.SpellRulesText
import app.dulliesanddungeons.domain.TurnEvent
import app.dulliesanddungeons.domain.WeaponClassification
import app.dulliesanddungeons.domain.WeaponTrainingCategory
import app.dulliesanddungeons.rules.DiceNotation
import app.dulliesanddungeons.rules.DicePoolRoll
import app.dulliesanddungeons.rules.DiceRoller
import app.dulliesanddungeons.rules.DiceSource
import app.dulliesanddungeons.rules.DerivedStatRules
import app.dulliesanddungeons.rules.CharacterDocumentValidator
import app.dulliesanddungeons.rules.SrdSpellCatalog
import app.dulliesanddungeons.rules.SrdSpellCombatCatalog
import app.dulliesanddungeons.rules.SrdSpellDetailsCatalog
import app.dulliesanddungeons.rules.SrdSpellClass
import app.dulliesanddungeons.rules.SrdSpellRevision
import app.dulliesanddungeons.rules.SrdWikiCatalog
import app.dulliesanddungeons.rules.SrdWikiKind
import app.dulliesanddungeons.rules.SrdWikiRevision
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
enum class UiLanguage { English, German }

@Serializable
enum class Ruleset(val shortLabel: String, val longLabel: String) {
    Fifth2024("5.5e", "Fifth edition · 2024 rules"),
    Fifth2014("5E 2014", "Fifth edition · 2014"),
    Pf2eRemaster("PF2e", "Pathfinder 2e Remaster"),
}

enum class AppScreen { Characters, CreateCharacter, CharacterSheet }

sealed interface PortraitPickTarget {
    data object Creation : PortraitPickTarget
    data object Editor : PortraitPickTarget
    data class Character(val characterId: String) : PortraitPickTarget
}

data class PortraitEditResult(
    val sourceBytes: ByteArray,
    val portraitBytes: ByteArray,
    val crop: PortraitCrop,
)

data class PortraitEditSource(
    val sourceBytes: ByteArray,
    val crop: PortraitCrop,
    val usesRenderedFallback: Boolean = false,
)

private data class StoredPortraitAssets(
    val displayFileName: String,
    val sourceFileName: String,
    val crop: PortraitCrop,
)

enum class StatMethod { Rolled, StandardArray, PointBuy, Manual }

@Serializable
enum class HpMethod { Fixed, Rolled, Manual }

internal val supportedClassHitDice = listOf(6, 8, 10, 12)

@Serializable
enum class QuickRollKind { ABILITY, SAVE, SKILL, INITIATIVE, DEATH_SAVE, ATTACK }

@Serializable
data class QuickRollUi(
    val kind: QuickRollKind,
    val id: String,
    val label: String,
)

@Serializable
enum class FeatureEffect { RESOURCE_ONLY, EXTRA_ACTION, SECOND_WIND, REROLL_SAVE, OPEN_HAND }

@Serializable
enum class EquipmentKind { GEAR, ARMOR, TOOL, CONSUMABLE, RATIONS }

@Serializable
data class LevelProgressionUi(
    val level: Int,
    val className: String,
    val hitPointGain: Int,
    val hpMethod: HpMethod,
    val featId: String? = null,
    val hitDieSides: Int? = null,
)

data class FeatOptionUi(
    val id: String,
    val name: String,
    val summary: String,
    val recommendedReason: String? = null,
)

data class CreationPreviewUi(
    val abilities: Map<String, Int>,
    val primaryAbility: String,
    val primaryScore: Int,
    val hitPoints: Int,
    val armorClass: Int,
    val startingArmor: String,
)

data class CreationSpellSelectionUi(
    val options: List<SpellUi>,
    val selected: List<SpellUi>,
    val fixed: List<SpellUi> = emptyList(),
    val cantripLimit: Int = 0,
    val leveledSpellLimit: Int = 0,
    val preparedLimit: Int? = null,
    val leveledLabel: String = "Spells",
)

data class CreationAbilitySourceUi(
    val label: String,
    val amount: Int,
    val detail: String,
)

data class CreationAbilityBreakdownUi(
    val ability: String,
    val baseScore: Int,
    val ancestrySource: CreationAbilitySourceUi?,
    val finalScore: Int,
    val modifier: Int,
)

data class CreationInfoUi(
    val title: String,
    val body: String,
)

private data class CreationAncestryBonuses(
    val amounts: Map<String, Int> = emptyMap(),
    val detail: String = "",
    val invalidPrivateFormula: Boolean = false,
)

private val creationAbilityOrder = listOf("STR", "DEX", "CON", "INT", "WIS", "CHA")

private val fifth2014AncestryBonuses = mapOf(
    "Human" to creationAbilityOrder.associateWith { 1 },
    "Dwarf" to mapOf("CON" to 2),
    "Elf" to mapOf("DEX" to 2),
    "Halfling" to mapOf("DEX" to 2),
    "Dragonborn" to mapOf("STR" to 2, "CHA" to 1),
    "Gnome" to mapOf("INT" to 2),
    "Tiefling" to mapOf("INT" to 1, "CHA" to 2),
)

sealed interface StartingArmorChoice {
    data object Recommended : StartingArmorChoice
    data object Unarmored : StartingArmorChoice
    data class Known(val itemId: String) : StartingArmorChoice
    data class Custom(val item: EquipmentUi) : StartingArmorChoice
}

@Serializable
data class EquipmentUi(
    val id: String,
    val name: String,
    val kind: EquipmentKind = EquipmentKind.GEAR,
    val quantity: Int = 1,
    val details: String = "",
    val needsAttunement: Boolean = false,
    val attuned: Boolean = false,
    val worn: Boolean = false,
    val armorClass: Int? = null,
    val shieldBonus: Int = 0,
    val grantedSpells: List<SpellUi> = emptyList(),
    val definitionId: String = id,
    val activeLocation: EquipmentLocation = EquipmentLocation.WORN,
    val effects: List<CoreModifier> = emptyList(),
    val savingThrows: List<SavingThrowPrompt> = emptyList(),
    val useCase: String = "",
    val combatContributions: List<CombatContribution> = emptyList(),
)

@Serializable
data class PrivateEntryUi(
    val id: String,
    val kind: String,
    val name: String,
    val summary: String = "",
    val formula: String = "",
    val sourceNote: String = "Local manual entry",
    val subclass: SubclassMechanicsUi? = null,
    val attackGrants: List<DerivedAttackGrant> = emptyList(),
    val combatContributions: List<CombatContribution> = emptyList(),
    val aliases: List<String> = emptyList(),
    val mechanics: PrivateMechanicsUi = PrivateMechanicsUi(),
    val sourcePackId: String? = null,
    val sourcePackVersion: String? = null,
)

@Serializable
data class PendingImportUi(
    val packId: String,
    val version: String,
    val requires: List<PrivateContentRequirementUi> = emptyList(),
    val sourcePath: String,
    val candidates: List<PrivateEntryUi> = emptyList(),
    val error: String? = null,
)

@Serializable
data class WeaponUi(
    val id: String,
    val name: String,
    val attackBonus: Int,
    val damage: String,
    val damageType: String,
    val properties: String,
    val ability: String = "STR",
    val proficient: Boolean = true,
    val proficiencyId: String? = null,
    val itemBonus: Int = 0,
    val abilityModifierOverride: Int? = null,
    val attackBonusOverride: Int? = null,
    val range: String = "",
    val mastery: String = "",
    val needsAttunement: Boolean = false,
    val attuned: Boolean = false,
    val custom: Boolean = false,
    val damageAbility: String? = null,
    val definitionId: String = id,
    val reachFeet: Int = 5,
    val normalRangeFeet: Int? = null,
    val longRangeFeet: Int? = null,
    val equipped: Boolean = false,
    val effects: List<CoreModifier> = emptyList(),
    val savingThrows: List<SavingThrowPrompt> = emptyList(),
    val useCase: String = "",
    val classification: WeaponClassification = WeaponClassification(),
    val combatContributions: List<CombatContribution> = emptyList(),
    /** Number of interchangeable copies represented by this single combat entry. */
    val quantity: Int = 1,
)

@Serializable
enum class SpellSourceKind { CLASS, FEATURE, ITEM }

@Serializable
data class SpellUi(
    val id: String,
    val name: String,
    val level: Int,
    val summary: String,
    val prepared: Boolean = true,
    val sourceKind: SpellSourceKind = SpellSourceKind.CLASS,
    val sourceName: String = "",
    val activationCost: ActionCost = ActionCost(actions = 1),
    val castPreviews: Map<Int, String> = emptyMap(),
    val savingThrows: List<SavingThrowPrompt> = emptyList(),
    val spellAttack: Boolean = false,
    val spellcastingAbility: Ability? = null,
    val rulesText: SpellRulesText = SpellRulesText(),
    val combatContributions: List<CombatContribution> = emptyList(),
)

@Serializable
data class SpellSlotUi(
    val level: Int,
    val remaining: Int,
    val maximum: Int,
)

@Serializable
data class DerivedModifierFormulaUi(
    val ability: String,
    val proficiencyMultiplier: Int = 0,
    val base: Int = 0,
    val proficiencyId: String? = null,
)

@Serializable
data class CharacterDerivationUi(
    val proficiencyFromLevel: Boolean = false,
    val initiative: DerivedModifierFormulaUi? = null,
    val saves: Map<String, DerivedModifierFormulaUi> = emptyMap(),
    val skills: Map<String, DerivedModifierFormulaUi> = emptyMap(),
)

@Serializable
data class FeatureUi(
    val id: String,
    val name: String,
    val summary: String,
    val remaining: Int? = null,
    val maximum: Int? = null,
    val recovery: Recovery = Recovery.MANUAL,
    val effect: FeatureEffect = FeatureEffect.RESOURCE_ONLY,
    val actionCost: ActionCost = ActionCost(),
    val custom: Boolean = false,
    val notes: String = "",
    /** ID of the shared pool spent by this option, when the feature is not its own pool. */
    val resourceId: String? = null,
    val resourceCost: Int = 1,
    val resourceDieSides: Int? = null,
    /** Passive, reaction-only, and table-facing features stay on the sheet without cluttering suggestions. */
    val turnGuideEligible: Boolean = true,
    val effects: List<CoreModifier> = emptyList(),
    val attackGrants: List<DerivedAttackGrant> = emptyList(),
    val combatContributions: List<CombatContribution> = emptyList(),
)

internal fun FeatureUi.isActivatable(): Boolean = remaining != null || actionCost != ActionCost()

@Serializable
data class CharacterUi(
    val id: String,
    val name: String,
    val ruleset: Ruleset,
    val level: Int,
    val ancestry: String,
    val className: String,
    val subclass: String,
    /** Stable subclass IDs and display names keyed by class name; supports multiclass characters. */
    val subclassIdsByClass: Map<String, String> = emptyMap(),
    val subclassNamesByClass: Map<String, String> = emptyMap(),
    val hp: Int,
    val maxHp: Int,
    val temporaryHp: Int = 0,
    val maxHpReduction: Int = 0,
    val deathSaveSuccesses: Int = 0,
    val deathSaveFailures: Int = 0,
    val isStable: Boolean = false,
    val isDead: Boolean = false,
    val deathReason: String? = null,
    val exhaustionLevel: Int = 0,
    val dyingValue: Int = 0,
    val woundedValue: Int = 0,
    val doomedValue: Int = 0,
    val armorClass: Int,
    /** Stable AC when no body armor or shield is active; equipment is always recalculated from this. */
    val unarmoredArmorClass: Int = armorClass,
    val speedFeet: Int,
    val flySpeedFeet: Int? = null,
    val initiative: Int,
    val proficiency: Int,
    val criticalHitThreshold: Int = 20,
    val initiativeRollMode: RollMode = RollMode.NORMAL,
    val portraitSeed: Int,
    val portraitFileName: String? = null,
    val portraitSourceFileName: String? = null,
    val portraitCrop: PortraitCrop? = null,
    val abilities: Map<String, Int>,
    val skills: Map<String, Int>,
    val saves: Map<String, Int>,
    val languages: List<String>,
    val lockedLanguages: List<String> = emptyList(),
    val weapons: List<WeaponUi>,
    val spells: List<SpellUi>,
    val spellSlots: List<SpellSlotUi> = emptyList(),
    val spellSlotMaximumOverrides: Map<Int, Int> = emptyMap(),
    val features: List<FeatureUi>,
    val equipmentItems: List<EquipmentUi> = emptyList(),
    val currency: CurrencyPurse = CurrencyPurse(),
    val quickRolls: List<QuickRollUi> = emptyList(),
    val progression: List<LevelProgressionUi> = emptyList(),
    val hitDieOverrides: Map<String, Int> = emptyMap(),
    val featIds: List<String> = emptyList(),
    val notes: List<CharacterNote> = emptyList(),
    val profile: CharacterProfile = CharacterProfile(),
    val sourceCharacterId: String? = null,
    val hasSpellcastingCapability: Boolean = false,
    val derivation: CharacterDerivationUi = CharacterDerivationUi(),
    val backgroundId: String = "background:local",
    val backgroundName: String = "Local",
    /** Stable proficiency IDs are retained for backwards compatibility with older documents. */
    val proficiencyIds: Set<String> = emptySet(),
    val proficiencyRanks: Map<String, ProficiencyRank> = emptyMap(),
    val activePlaySession: PlaySessionRecord? = null,
    val savedPlaySessions: List<PlaySessionRecord> = emptyList(),
    val hasPlayedSinceLongRest: Boolean = false,
    val activeConcentration: ActiveConcentration? = null,
    /** Stable values before active item/weapon/condition effects are applied. */
    val baseAbilities: Map<String, Int> = emptyMap(),
    val baseSaves: Map<String, Int> = emptyMap(),
    val baseArmorClass: Int? = null,
    val baseSpeedFeet: Int? = null,
    val baseFlySpeedFeet: Int? = null,
    val passiveArmorClassBonus: Int = 0,
    val privateContentVersions: List<InstalledPrivatePackUi> = emptyList(),
) {
    val buildLabel: String get() = "$ancestry $className $level"
    val classLevelLabel: String
        get() = progression.groupingBy { it.className }.eachCount().entries.joinToString(" / ") { "${it.key} ${it.value}" }
            .ifBlank { "$className $level" }
    fun effectiveMaxHp(maximumReduction: Int): Int {
        val reducedMaximum = (maxHp - maximumReduction).coerceAtLeast(0)
        return if (ruleset == Ruleset.Fifth2014 && exhaustionLevel >= 4) reducedMaximum / 2 else reducedMaximum
    }
    val effectiveMaxHp: Int get() = effectiveMaxHp(maxHpReduction)
    val effectiveSpeedFeet: Int
        get() = when (ruleset) {
            Ruleset.Fifth2024 -> (speedFeet - exhaustionLevel * 5).coerceAtLeast(0)
            Ruleset.Fifth2014 -> when {
                exhaustionLevel >= 5 -> 0
                exhaustionLevel >= 2 -> speedFeet / 2
                else -> speedFeet
            }
            Ruleset.Pf2eRemaster -> speedFeet
        }
    val effectiveFlySpeedFeet: Int?
        get() = flySpeedFeet?.let { speed ->
            when (ruleset) {
                Ruleset.Fifth2024 -> (speed - exhaustionLevel * 5).coerceAtLeast(0)
                Ruleset.Fifth2014 -> when {
                    exhaustionLevel >= 5 -> 0
                    exhaustionLevel >= 2 -> speed / 2
                    else -> speed
                }
                Ruleset.Pf2eRemaster -> speed
            }
        }
    val isDowned: Boolean get() = hp == 0 && !isDead && !isStable
    val stopsTurnGuide: Boolean get() = hp == 0 || isDead
    val resolvedEquipment: List<EquipmentUi> get() = equipmentItems

    val resolvedQuickRolls: List<QuickRollUi>
        get() = quickRolls.filterNot { it.kind == QuickRollKind.ATTACK }.takeIf { it.isNotEmpty() } ?: listOf(
            QuickRollUi(QuickRollKind.INITIATIVE, "initiative", "Initiative"),
            QuickRollUi(QuickRollKind.SKILL, "Perception", "Perception"),
            QuickRollUi(QuickRollKind.SAVE, "Dexterity", "DEX Save"),
            QuickRollUi(QuickRollKind.SAVE, "Constitution", "CON Save"),
        )

    val availableSpells: List<SpellUi>
        get() = (spells.filter { it.sourceKind != SpellSourceKind.CLASS || it.prepared || it.level == 0 } + resolvedEquipment.flatMap { item ->
            if (!item.needsAttunement || item.attuned) item.grantedSpells.map { spell ->
                spell.copy(sourceKind = SpellSourceKind.ITEM, sourceName = item.name)
            } else emptyList()
        }).distinctBy { "${it.sourceKind}:${it.sourceName}:${it.id}" }

    val canCastSpells: Boolean
        get() = hasSpellcastingCapability || availableSpells.isNotEmpty()

    val fiveECasterLevel: Int
        get() {
            if (ruleset == Ruleset.Pf2eRemaster) return 0
            val progressionLevel = progression.count { it.className.equals("Wizard", true) || it.className.equals("Sorcerer", true) }
            return progressionLevel.takeIf { it > 0 }
                ?: level.takeIf { className.equals("Wizard", true) || className.equals("Sorcerer", true) }
                ?: 0
        }

    val resolvedSpellSlots: List<SpellSlotUi>
        get() {
            val derived = DerivedStatRules.fiveESpellSlots(fiveECasterLevel)
            return (1..9).mapNotNull { level ->
                val maximum = spellSlotMaximumOverrides[level] ?: derived.getOrElse(level - 1) { 0 }
                if (maximum <= 0) return@mapNotNull null
                val stored = spellSlots.firstOrNull { it.level == level }
                val spent = stored?.let { (it.maximum - it.remaining).coerceAtLeast(0) } ?: 0
                SpellSlotUi(level, (maximum - spent).coerceIn(0, maximum), maximum)
            }
        }

    val isSorcerer: Boolean
        get() = className.equals("Sorcerer", true) || progression.any { it.className.equals("Sorcerer", true) }
}

data class AttackCalculationUi(
    val ability: Int,
    val proficiency: Int,
    val item: Int,
    val multipleAttackPenalty: Int = 0,
    val total: Int,
    val abilityLabel: String,
)

data class AttackRollUi(
    val dice: List<Int>,
    val kept: Int,
    val mode: RollMode,
    val total: Int,
    val natural: Int,
    val calculation: AttackCalculationUi,
)

data class DamageRollUi(
    val dice: List<Int>,
    val sides: Int,
    val modifier: Int,
    val total: Int,
    val critical: Boolean,
    val damageType: String,
)

data class DicePresentationUi(
    val id: Int,
    val label: String,
    val sides: Int,
    val dice: List<Int>,
    val kept: Int? = null,
    val total: Int,
    val calculation: String = "",
    val context: String = "",
    val modifierLabel: String = "",
    val inspirationEligible: Boolean = false,
)

data class InlineFeatureFeedbackUi(
    val id: Int,
    val featureId: String,
    val rolledValue: Int? = null,
    val message: String,
)

data class SuggestedTurnStepUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: TurnSection,
    val weaponId: String? = null,
    val featureId: String? = null,
    val cost: ActionCost = ActionCost(),
    val assumptionCount: Int = 0,
    val shortRestCost: Int = 0,
    val longRestCost: Int = 0,
)

@Serializable
data class ConditionUi(
    val name: String,
    val source: String,
    val duration: String,
    val explanation: String,
    val characterId: String = "",
    val id: String = "",
    val level: Int = 1,
    val removable: Boolean = true,
    val effects: List<CoreModifier> = emptyList(),
)

internal fun ConditionUi.isInspiration(): Boolean =
    name.equals("Inspiration", ignoreCase = true) || id.substringAfterLast('-').equals("inspiration", ignoreCase = true)

enum class SearchResultKind { Roll, Action, Rule, Note, Navigate, Currency }

data class SearchResultUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: SearchResultKind,
    val actionLabel: String,
    val modifier: Int = 0,
    val cost: ActionCost = ActionCost(),
    val resourceLabel: String? = null,
    val searchTerms: List<String> = emptyList(),
    val detailBody: String = "",
    val denomination: CoinDenomination? = null,
)

@Serializable
enum class TurnSection { Overview, Move, Attack, Spell, Other }
enum class AttackOutcome { Pending, Miss, Hit, Critical }

@Serializable
data class TurnSessionSnapshotUi(
    val characterId: String,
    val baseline: CharacterUi,
    val selectedSection: TurnSection = TurnSection.Overview,
    val movementUsed: Int = 0,
    val flying: Boolean = false,
    val requestedMovement: Int = 0,
    val dashActive: Boolean = false,
    val flightActivationPaid: Boolean = false,
    val actionUsed: Boolean = false,
    val attackActionStarted: Boolean = false,
    val attacksRemaining: Int = 0,
    val extraActionsRemaining: Int = 0,
    val pf2ActionsRemaining: Int = 0,
    val pf2AttacksMade: Int = 0,
    val bonusActionUsed: Boolean = false,
    val reactionUsed: Boolean = false,
    val selectedWeaponId: String? = null,
    val selectedAttackOptionId: String? = null,
    val selectedSpellId: String? = null,
    val selectedFeatureId: String? = null,
    val advantage: Boolean = false,
    val disadvantage: Boolean = false,
    val eventCount: Int = 0,
    val events: List<TurnEvent> = emptyList(),
    val completedSuggestionIds: List<String> = emptyList(),
)

data class SessionStatsUi(
    val turns: Int = 0,
    val damageTaken: Int = 0,
    val healingReceived: Int = 0,
    val timesDowned: Int = 0,
    val distanceMoved: Int = 0,
    val attacks: Int = 0,
    val criticals: Int = 0,
    val rolls: Int = 0,
    val naturalTwenties: Int = 0,
    val naturalOnes: Int = 0,
)

fun PlaySessionRecord.stats(): SessionStatsUi {
    var result = SessionStatsUi()
    activities.forEach { activity ->
        when (val event = activity.turnEvent) {
            is TurnEvent.HitPointsChanged -> result = result.copy(
                damageTaken = result.damageTaken + if (event.kind == HitPointChangeKind.DAMAGE) event.amount else 0,
                healingReceived = result.healingReceived + if (event.kind == HitPointChangeKind.HEALING) event.effectiveHitPointChange.coerceAtLeast(0) else 0,
                timesDowned = result.timesDowned + if (event.wentDown) 1 else 0,
            )
            is TurnEvent.Moved -> result = result.copy(distanceMoved = result.distanceMoved + event.distanceFeet)
            is TurnEvent.AttackMade -> result = result.copy(attacks = result.attacks + 1)
            is TurnEvent.AttackResolved -> if (event.outcome == AttackOutcomeRecord.CRITICAL) {
                result = result.copy(criticals = result.criticals + 1)
            }
            TurnEvent.TurnEnded -> result = result.copy(turns = result.turns + 1)
            else -> Unit
        }
        val roll = activity.roll ?: (activity.turnEvent as? TurnEvent.RollRecorded)?.roll
        if (roll != null) {
            val kept = roll.keptDice.singleOrNull()
            result = result.copy(
                rolls = result.rolls + 1,
                naturalTwenties = result.naturalTwenties + if (roll.request.expression.sides == 20 && kept == 20) 1 else 0,
                naturalOnes = result.naturalOnes + if (roll.request.expression.sides == 20 && kept == 1) 1 else 0,
            )
        }
    }
    return result
}

class TurnSession(
    character: CharacterUi,
    private val restored: TurnSessionSnapshotUi? = null,
    private val onEvent: (TurnEvent, String?) -> Unit = { _, _ -> },
) {
    val characterId = character.id
    val baseline = restored?.baseline ?: character
    val ruleset = character.ruleset
    private val walkSpeed = character.effectiveSpeedFeet
    private val flySpeed = character.effectiveFlySpeedFeet
    private val attacksPerAction = CombatPotentialEngine.attacksPerAction(character)
    var selectedSection by mutableStateOf(restored?.selectedSection ?: TurnSection.Overview)
    var movementUsed by mutableIntStateOf(restored?.movementUsed ?: 0)
    var flying by mutableStateOf(restored?.flying ?: false)
    var flightActivationPaid by mutableStateOf(restored?.flightActivationPaid ?: false)
    var requestedMovement by mutableIntStateOf(restored?.requestedMovement ?: walkSpeed.coerceAtMost(30))
    var dashActive by mutableStateOf(restored?.dashActive ?: false)
    var actionUsed by mutableStateOf(restored?.actionUsed ?: false)
    var attackActionStarted by mutableStateOf(restored?.attackActionStarted ?: false)
    var attacksRemaining by mutableIntStateOf(restored?.attacksRemaining ?: attacksPerAction)
    var extraActionsRemaining by mutableIntStateOf(restored?.extraActionsRemaining ?: 0)
    var pf2ActionsRemaining by mutableIntStateOf(restored?.pf2ActionsRemaining ?: if (ruleset == Ruleset.Pf2eRemaster) 3 else 0)
    var pf2AttacksMade by mutableIntStateOf(restored?.pf2AttacksMade ?: 0)
    var bonusActionUsed by mutableStateOf(restored?.bonusActionUsed ?: false)
    var reactionUsed by mutableStateOf(restored?.reactionUsed ?: false)
    var selectedWeaponId by mutableStateOf(restored?.selectedWeaponId ?: character.weapons.firstOrNull()?.id)
    var selectedAttackOptionId by mutableStateOf(restored?.selectedAttackOptionId)
    var selectedSpellId by mutableStateOf(restored?.selectedSpellId ?: character.availableSpells.firstOrNull()?.id)
    var selectedFeatureId by mutableStateOf(restored?.selectedFeatureId)
    var advantage by mutableStateOf(restored?.advantage ?: false)
    var disadvantage by mutableStateOf(restored?.disadvantage ?: false)
    var lastAttackRoll by mutableStateOf<String?>(null)
    var lastAttackDetails by mutableStateOf<AttackRollUi?>(null)
    var attackOutcome by mutableStateOf(AttackOutcome.Pending)
    var unresolvedAttackCommitted by mutableStateOf(false)
    var lastDamageRoll by mutableStateOf<String?>(null)
    var lastDamageDetails by mutableStateOf<DamageRollUi?>(null)
    val events = mutableStateListOf<TurnEvent>().apply { addAll(restored?.events.orEmpty()) }
    var legacyEventCount by mutableIntStateOf(restored?.eventCount ?: 0)
    var suggestionsVisible by mutableStateOf(false)
    val completedSuggestionIds = mutableStateListOf<String>().apply { addAll(restored?.completedSuggestionIds.orEmpty()) }

    val maxMovement: Int get() = if (flying) flySpeed ?: walkSpeed else walkSpeed
    val remainingMovement: Int
        get() = if (ruleset == Ruleset.Pf2eRemaster) {
            if (pf2ActionsRemaining > 0) maxMovement else 0
        } else ((maxMovement * if (dashActive) 2 else 1) - movementUsed).coerceAtLeast(0)
    val canUseAction: Boolean
        get() = if (ruleset == Ruleset.Pf2eRemaster) pf2ActionsRemaining > 0 else !actionUsed || extraActionsRemaining > 0
    val standardActionsAvailable: Int
        get() = if (ruleset == Ruleset.Pf2eRemaster) pf2ActionsRemaining else (if (actionUsed) 0 else 1) + extraActionsRemaining
    val requiresFlightAction: Boolean
        get() = ruleset != Ruleset.Pf2eRemaster && flying && !flightActivationPaid
    val canAttack: Boolean
        get() = if (ruleset == Ruleset.Pf2eRemaster) {
            pf2ActionsRemaining > 0
        } else {
            (attackActionStarted && attacksRemaining > 0) || !actionUsed || extraActionsRemaining > 0
        }
    val eventCount: Int get() = maxOf(legacyEventCount, events.size)
    val hasCommittedEvent: Boolean get() = eventCount > 0

    fun record(event: TurnEvent, label: String? = null) {
        events += event
        legacyEventCount = maxOf(legacyEventCount, events.size)
        onEvent(event, label)
    }

    fun canPay(cost: ActionCost): Boolean {
        val reactionAvailable = cost.reactions == 0 || (!reactionUsed && cost.reactions == 1)
        if (!reactionAvailable) return false
        return if (ruleset == Ruleset.Pf2eRemaster) {
            val required = cost.pf2eActions + cost.actions + cost.bonusActions
            pf2ActionsRemaining >= required
        } else {
            standardActionsAvailable >= cost.actions &&
                (cost.bonusActions == 0 || (!bonusActionUsed && cost.bonusActions == 1))
        }
    }

    fun commitCost(cost: ActionCost, actionId: String = "action"): Boolean {
        if (!canPay(cost)) return false
        if (ruleset == Ruleset.Pf2eRemaster) {
            pf2ActionsRemaining -= cost.pf2eActions + cost.actions + cost.bonusActions
        } else {
            repeat(cost.actions) {
                if (!actionUsed) actionUsed = true else extraActionsRemaining--
            }
            if (cost.bonusActions > 0) bonusActionUsed = true
        }
        if (cost.reactions > 0) reactionUsed = true
        if (cost.actions > 0 || cost.bonusActions > 0 || cost.reactions > 0 || cost.pf2eActions > 0) {
            record(TurnEvent.ActionUsed(actionId, cost))
        }
        return true
    }

    fun commitMovement() {
        val used = requestedMovement.coerceIn(0, remainingMovement)
        if (used > 0) {
            movementUsed += used
            if (ruleset == Ruleset.Pf2eRemaster) pf2ActionsRemaining--
            requestedMovement = (remainingMovement).coerceAtMost(5)
            record(TurnEvent.Moved(if (flying) MovementMode.FLY else MovementMode.WALK, used))
        }
    }

    fun commitAction(actionId: String = "action") {
        if (ruleset == Ruleset.Pf2eRemaster) {
            if (pf2ActionsRemaining <= 0) return
            pf2ActionsRemaining--
        } else {
            if (!actionUsed) actionUsed = true
            else if (extraActionsRemaining > 0) extraActionsRemaining--
            else return
            attacksRemaining = 0
            attackActionStarted = false
        }
        record(TurnEvent.ActionUsed(actionId, if (ruleset == Ruleset.Pf2eRemaster) ActionCost(pf2eActions = 1) else ActionCost(actions = 1)))
    }

    fun commitDash(): Boolean {
        if (ruleset == Ruleset.Pf2eRemaster || dashActive || !canUseAction) return false
        commitAction("dash")
        dashActive = true
        return true
    }

    fun selectFlying(value: Boolean) {
        if (flySpeed == null) return
        if (!value && flying) flightActivationPaid = false
        flying = value
        requestedMovement = requestedMovement.coerceAtMost(maxMovement)
    }

    fun commitFlightActivation(): Boolean {
        if (!requiresFlightAction) return true
        if (!canUseAction) return false
        commitAction("flight")
        flightActivationPaid = true
        return true
    }

    fun commitAttack(weaponId: String) {
        if (!canAttack) return
        if (ruleset == Ruleset.Pf2eRemaster) {
            pf2ActionsRemaining--
            pf2AttacksMade++
        } else {
            if (!attackActionStarted || attacksRemaining <= 0) {
                if (!actionUsed) actionUsed = true
                else if (extraActionsRemaining > 0) extraActionsRemaining--
                else return
                attackActionStarted = true
                attacksRemaining = attacksPerAction
            }
            attacksRemaining = (attacksRemaining - 1).coerceAtLeast(0)
        }
        record(TurnEvent.AttackMade(weaponId))
    }

    fun commitDerivedAttack(option: ResolvedAttackOptionUi): Boolean {
        val priorRolls = events.count { (it as? TurnEvent.AttackMade)?.attackOptionId == option.id }
        val continuingMultiattack = priorRolls % option.attackCount != 0
        val paidCost = if (continuingMultiattack) ActionCost() else option.cost
        if (!commitCost(paidCost, option.id)) return false
        record(TurnEvent.AttackMade(option.parentWeaponId, option.id, option.sourceId, paidCost))
        return true
    }

    fun grantExtraAction() {
        if (ruleset == Ruleset.Pf2eRemaster) pf2ActionsRemaining++ else extraActionsRemaining++
        record(TurnEvent.ActionUsed("extra-action-granted", ActionCost()))
    }

    fun commitBonusAction(): Boolean {
        if (ruleset == Ruleset.Pf2eRemaster) {
            if (pf2ActionsRemaining <= 0) return false
            pf2ActionsRemaining--
        } else {
            if (bonusActionUsed) return false
            bonusActionUsed = true
        }
        record(TurnEvent.ActionUsed("bonus-action", if (ruleset == Ruleset.Pf2eRemaster) ActionCost(pf2eActions = 1) else ActionCost(bonusActions = 1)))
        return true
    }

    fun markSuggestionComplete(id: String) {
        if (id !in completedSuggestionIds) completedSuggestionIds += id
    }

    fun snapshot(): TurnSessionSnapshotUi = TurnSessionSnapshotUi(
        characterId = characterId,
        baseline = baseline,
        selectedSection = selectedSection,
        movementUsed = movementUsed,
        flying = flying,
        requestedMovement = requestedMovement,
        dashActive = dashActive,
        flightActivationPaid = flightActivationPaid,
        actionUsed = actionUsed,
        attackActionStarted = attackActionStarted,
        attacksRemaining = attacksRemaining,
        extraActionsRemaining = extraActionsRemaining,
        pf2ActionsRemaining = pf2ActionsRemaining,
        pf2AttacksMade = pf2AttacksMade,
        bonusActionUsed = bonusActionUsed,
        reactionUsed = reactionUsed,
        selectedWeaponId = selectedWeaponId,
        selectedAttackOptionId = selectedAttackOptionId,
        selectedSpellId = selectedSpellId,
        selectedFeatureId = selectedFeatureId,
        advantage = advantage,
        disadvantage = disadvantage,
        eventCount = eventCount,
        events = events.toList(),
        completedSuggestionIds = completedSuggestionIds.toList(),
    )

    fun finishAttackResolution() {
        unresolvedAttackCommitted = false
    }
}

class CreationDraft {
    var step by mutableIntStateOf(0)
    var name by mutableStateOf("")
    var characterDescription by mutableStateOf("")
    var motive by mutableStateOf("")
    var alignment by mutableStateOf("")
    var ruleset by mutableStateOf(Ruleset.Fifth2024)
    var level by mutableIntStateOf(1)
    var ancestry by mutableStateOf("Human")
    var className by mutableStateOf("Fighter")
    var backgroundId by mutableStateOf<String?>(null)
    val backgroundSkillIds = mutableStateListOf<String>()
    val classSkillIds = mutableStateListOf<String>()
    val featSkillIds = mutableStateListOf<String>()
    val skillRankChoices = mutableStateMapOf<String, ProficiencyRank>()
    var proficiencyAdvisory by mutableStateOf<String?>(null)
    var subclassId by mutableStateOf<String?>(null)
    var subclassName by mutableStateOf("")
    var subclassAdvisory by mutableStateOf<String?>(null)
    var statMethod by mutableStateOf(StatMethod.Rolled)
    var useRecommendations by mutableStateOf(true)
    var hpMethod by mutableStateOf(HpMethod.Fixed)
    var manualHpGain by mutableIntStateOf(1)
    val rolledHpGains = mutableStateListOf<Int>()
    val selectedFeatIds = mutableStateListOf<String>()
    val selectedSpellIds = mutableStateListOf<String>()
    val languages = mutableStateListOf("Common")
    var startingArmorChoice by mutableStateOf<StartingArmorChoice>(StartingArmorChoice.Recommended)
    var selectedStartingGearPackageId by mutableStateOf<String?>(null)
    val startingEquipment = mutableStateListOf<EquipmentUi>()
    val startingWeapons = mutableStateListOf<WeaponUi>()
    var startingGoldPieces by mutableIntStateOf(0)
    var portraitBytes by mutableStateOf<ByteArray?>(null)
    var portraitSourceBytes by mutableStateOf<ByteArray?>(null)
    var portraitCrop by mutableStateOf<PortraitCrop?>(null)
    val rolledScores = mutableStateListOf<Int>()
    val rolledAbilityAssignments = mutableStateMapOf<String, Int>()
    var rolledAssignmentEdited by mutableStateOf(false)
    val manualAbilities = mutableStateMapOf(
        "STR" to 10, "DEX" to 10, "CON" to 10, "INT" to 10, "WIS" to 10, "CHA" to 10,
    )

    fun reset() {
        step = 0
        name = ""
        characterDescription = ""
        motive = ""
        alignment = ""
        ruleset = Ruleset.Fifth2024
        level = 1
        ancestry = "Human"
        className = "Fighter"
        backgroundId = null
        backgroundSkillIds.clear()
        classSkillIds.clear()
        featSkillIds.clear()
        skillRankChoices.clear()
        proficiencyAdvisory = null
        subclassId = null
        subclassName = ""
        subclassAdvisory = null
        statMethod = StatMethod.Rolled
        useRecommendations = true
        hpMethod = HpMethod.Fixed
        manualHpGain = 1
        rolledHpGains.clear()
        selectedFeatIds.clear()
        selectedSpellIds.clear()
        languages.clear()
        languages += "Common"
        startingArmorChoice = StartingArmorChoice.Recommended
        selectedStartingGearPackageId = null
        startingEquipment.clear()
        startingWeapons.clear()
        startingGoldPieces = 0
        portraitBytes = null
        portraitSourceBytes = null
        portraitCrop = null
        rolledScores.clear()
        rolledAbilityAssignments.clear()
        rolledAssignmentEdited = false
        manualAbilities.keys.toList().forEach { manualAbilities[it] = 10 }
    }
}

class LevelUpDraft(character: CharacterUi, initialHitDieSides: Int) {
    val characterId = character.id
    val fromLevel = character.level
    val toLevel = character.level + 1
    var step by mutableIntStateOf(0)
    var className by mutableStateOf(character.className)
    var hitDieSides by mutableIntStateOf(initialHitDieSides)
    var hpMethod by mutableStateOf(HpMethod.Fixed)
    var rolledHitDie by mutableStateOf<Int?>(null)
    var manualHitDie by mutableIntStateOf(1)
    var healByIncrease by mutableStateOf(false)
    var selectedFeatId by mutableStateOf<String?>(null)
    val abilityIncreases = mutableStateMapOf<String, Int>()
    val guidedSelections = mutableStateMapOf<String, Set<String>>()
}

enum class EditorSection { Hub, Identity, Build, Abilities, Combat, Spells, Review }

class CharacterEditorDraft(character: CharacterUi, portrait: ByteArray?, portraitSource: PortraitEditSource?) {
    val original = character
    var section by mutableStateOf(EditorSection.Hub)
    var name by mutableStateOf(character.name)
    var characterDescription by mutableStateOf(character.profile.characterDescription)
    var motive by mutableStateOf(character.profile.motive)
    var alignment by mutableStateOf(character.profile.alignment)
    var ruleset by mutableStateOf(character.ruleset)
    var level by mutableIntStateOf(character.level)
    var ancestry by mutableStateOf(character.ancestry)
    var className by mutableStateOf(character.className)
    var subclass by mutableStateOf(character.subclass)
    var maxHp by mutableIntStateOf(character.maxHp)
    var armorClass by mutableIntStateOf(character.baseArmorClass ?: character.armorClass)
    var speedFeet by mutableIntStateOf(character.baseSpeedFeet ?: character.speedFeet)
    var flySpeedFeet by mutableStateOf(character.baseFlySpeedFeet ?: character.flySpeedFeet)
    var initiative by mutableIntStateOf(character.initiative)
    var proficiency by mutableIntStateOf(character.proficiency)
    var initiativeManual by mutableStateOf(character.derivation.initiative == null)
    var proficiencyManual by mutableStateOf(!character.derivation.proficiencyFromLevel)
    var portraitBytes by mutableStateOf(portrait)
    var portraitSourceBytes by mutableStateOf(portraitSource?.sourceBytes)
    var portraitCrop by mutableStateOf(portraitSource?.crop)
    var portraitChanged by mutableStateOf(false)
    var linkPrivateContent by mutableStateOf(false)
    val abilities = mutableStateMapOf<String, Int>().apply { putAll(character.baseAbilities.ifEmpty { character.abilities }) }
    val spells = mutableStateListOf<SpellUi>().apply { addAll(character.spells) }

    val isValid: Boolean
        get() = name.isNotBlank() && level in 1..20 && maxHp > 0 && armorClass > 0 &&
            speedFeet >= 0 && abilities.values.all { it in 1..30 }
}

private sealed interface InventoryRemovalUi {
    val characterId: String

    data class Equipment(
        override val characterId: String,
        val index: Int,
        val item: EquipmentUi,
    ) : InventoryRemovalUi

    data class Weapon(
        override val characterId: String,
        val index: Int,
        val item: WeaponUi,
    ) : InventoryRemovalUi
}

/**
 * Presentation contract for the vertical slice. Repositories and the declarative rules engine can
 * implement the same events and replace this class without changing screen composables.
 */
@OptIn(ExperimentalTime::class)
class DndAppState(
    private val storage: LocalStateStore = LocalStateStore.None,
    initialStateJson: String? = storage.readState(),
) {
    private val json = Json { encodeDefaults = true }
    private val dice = DiceRoller(DiceSource { bound -> Random.nextInt(bound) })
    private val restored = runCatching {
        initialStateJson?.let { json.decodeFromString<PersistedAppState>(it) }
    }.getOrNull()?.takeIf { persisted ->
        persisted.schemaVersion == 5 && persisted.characters.all {
            CharacterDocumentValidator.validate(it).isEmpty()
        }
    }

    var language by mutableStateOf(restored?.language ?: UiLanguage.English)
        private set
    var screen by mutableStateOf(AppScreen.Characters)
    val characters = mutableStateListOf<CharacterUi>().apply {
        addAll((restored?.characters?.map { it.toCharacterUi() } ?: seedCharacters()).map(::withCompleteSkills))
    }
    var selectedCharacterId by mutableStateOf<String?>(null)
    val conditions = mutableStateListOf<ConditionUi>().apply {
        addAll(restored?.conditions.orEmpty())
        addAll(restored?.characters.orEmpty().flatMap { it.toConditionUi() })
    }
    val privateEntries = mutableStateListOf<PrivateEntryUi>().apply { addAll(restored?.privateEntries.orEmpty()) }
    val pendingImports = mutableStateListOf<PendingImportUi>().apply { addAll(restored?.pendingImports.orEmpty()) }
    val installedPrivatePacks = mutableStateListOf<InstalledPrivatePackUi>().apply {
        addAll(restored?.installedPrivatePacks.orEmpty())
    }
    private val savedTurnDrafts = mutableStateMapOf<String, TurnSessionSnapshotUi>().apply {
        restored?.characters.orEmpty().mapNotNull { it.toTurnSessionSnapshotUi() }.forEach { put(it.characterId, it) }
    }
    val creation = CreationDraft()
    var searchOpen by mutableStateOf(false)
    var conditionsOpen by mutableStateOf(false)
    var hpAdjustOpen by mutableStateOf(false)
    var quickRollEditorOpen by mutableStateOf(false)
    var equipmentAddOpen by mutableStateOf(false)
    var itemBrowserTarget by mutableStateOf(ItemBrowserTarget.Inventory)
    var currencyAdjustmentDenomination by mutableStateOf<CoinDenomination?>(null)
        private set
    var itemBrowserFeedback by mutableStateOf<String?>(null)
    var itemBrowserEditingEquipment by mutableStateOf<EquipmentUi?>(null)
        private set
    var itemBrowserEditingWeapon by mutableStateOf<WeaponUi?>(null)
        private set
    var inventoryFeedback by mutableStateOf<String?>(null)
        private set
    private var pendingInventoryRemoval: InventoryRemovalUi? = null
    val inventoryFeedbackCanUndo: Boolean get() = pendingInventoryRemoval != null
    var privateContentOpen by mutableStateOf(false)
    var turnOpen by mutableStateOf(false)
    var sessionHistoryOpen by mutableStateOf(false)
    var sessionSaveOpen by mutableStateOf(false)
    var conversionOpen by mutableStateOf(false)
    var editorOpen by mutableStateOf(false)
    var editorDraft by mutableStateOf<CharacterEditorDraft?>(null)
    var levelUpOpen by mutableStateOf(false)
    var levelUpDraft by mutableStateOf<LevelUpDraft?>(null)
    var infoTitle by mutableStateOf<String?>(null)
    var infoBody by mutableStateOf("")
    internal var infoCosts by mutableStateOf<List<CostTokenUi>>(emptyList())
    var lastRoll by mutableStateOf<String?>(null)
    var turnSession by mutableStateOf<TurnSession?>(null)
    var preselectedTurnSection by mutableStateOf<TurnSection?>(null)
    var sheetAttackWeaponId by mutableStateOf<String?>(null)
    var sheetAttackOptionId by mutableStateOf<String?>(null)
    var sheetAttackRoll by mutableStateOf<AttackRollUi?>(null)
    var sheetAttackOutcome by mutableStateOf(AttackOutcome.Pending)
    var sheetDamageRoll by mutableStateOf<DamageRollUi?>(null)
    var dicePresentation by mutableStateOf<DicePresentationUi?>(null)
    private var dicePresentationId = 0
    private var sheetAttackResourcesCommitted by mutableStateOf(false)
    private var lastRollAction: (() -> Unit)? = null
    var inlineFeatureFeedback by mutableStateOf<InlineFeatureFeedbackUi?>(null)
        private set
    private var inlineFeatureFeedbackId = 0
    var recentlyLevelledCharacterId by mutableStateOf<String?>(null)
        private set
    var revivalConfirmationOpen by mutableStateOf(false)
    private var pendingRevivalHp by mutableIntStateOf(0)

    val selectedCharacter: CharacterUi?
        get() = characters.firstOrNull { it.id == selectedCharacterId }
    val selectedConditions: List<ConditionUi>
        get() = conditions.filter { it.characterId.isBlank() || it.characterId == selectedCharacterId }
    val hasInspiration: Boolean
        get() = selectedCharacter?.ruleset != Ruleset.Pf2eRemaster && conditions.any {
            it.characterId == selectedCharacterId && it.isInspiration()
        }
    val canUseInspirationForCurrentRoll: Boolean
        get() = hasInspiration && dicePresentation?.inspirationEligible == true && lastRollAction != null
    val canUseInspirationForSheetAttack: Boolean
        get() = hasInspiration && sheetAttackRoll != null && selectedCharacter?.ruleset != Ruleset.Pf2eRemaster
    val currentPlaySession: PlaySessionRecord?
        get() = selectedCharacter?.activePlaySession
    fun hasSavedTurnDraft(characterId: String = selectedCharacterId.orEmpty()): Boolean =
        turnSession?.characterId == characterId || characterId in savedTurnDrafts
    fun currentTurnRecordedCount(characterId: String = selectedCharacterId.orEmpty()): Int =
        turnSession?.takeIf { it.characterId == characterId }?.eventCount ?: savedTurnDrafts[characterId]?.eventCount ?: 0

    private fun newPlaySession(character: CharacterUi): PlaySessionRecord {
        val ordinal = (character.savedPlaySessions.maxOfOrNull { it.ordinal } ?: 0) + 1
        val now = Clock.System.now().toEpochMilliseconds()
        return PlaySessionRecord(
            id = "session-${character.id}-$now-$ordinal",
            ordinal = ordinal,
            startedAtEpochMillis = now,
        )
    }

    private fun ensureActivePlaySession(): PlaySessionRecord? {
        val character = selectedCharacter ?: return null
        character.activePlaySession?.let { return it }
        val created = newPlaySession(character)
        updateSelectedCharacter { it.copy(activePlaySession = created) }
        return created
    }

    private fun eventLabel(event: TurnEvent): String = when (event) {
        is TurnEvent.ActionUsed -> when {
            event.actionId.startsWith("spell:") -> selectedCharacter?.availableSpells?.firstOrNull { it.id == event.actionId.removePrefix("spell:") }?.name ?: event.actionId.removePrefix("spell:")
            else -> selectedCharacter?.features?.firstOrNull { it.id == event.actionId }?.name ?: event.actionId
        }
        is TurnEvent.AttackMade -> selectedCharacter?.weapons?.firstOrNull { it.id == event.weaponId }?.name ?: event.weaponId
        is TurnEvent.AttackResolved -> selectedCharacter?.weapons?.firstOrNull { it.id == event.weaponId }?.name ?: event.weaponId
        is TurnEvent.ConditionApplied -> selectedConditions.firstOrNull { it.id == event.conditionId }?.name ?: event.conditionId
        is TurnEvent.ConditionRemoved -> event.conditionId
        is TurnEvent.FeatureResolved -> selectedCharacter?.features?.firstOrNull { it.id == event.featureId }?.name ?: event.featureId
        is TurnEvent.FeatureStarted -> selectedCharacter?.features?.firstOrNull { it.id == event.featureId }?.name ?: event.featureId
        is TurnEvent.HitPointsChanged -> if (event.kind == HitPointChangeKind.DAMAGE) "Damage" else "Healing"
        is TurnEvent.Moved -> event.mode.name
        is TurnEvent.MovementGranted -> event.sourceId
        is TurnEvent.ResourceChanged -> event.resourceId
        is TurnEvent.RollRecorded -> event.roll.request.label
        TurnEvent.TurnEnded -> "Turn"
    }

    private fun recordActivity(event: TurnEvent, label: String = eventLabel(event), roll: DiceRoll? = null) {
        val character = selectedCharacter ?: return
        inlineFeatureFeedback = null
        val active = character.activePlaySession ?: newPlaySession(character)
        val nextSequence = (active.activities.maxOfOrNull { it.sequence } ?: 0L) + 1L
        val belongsToTurn = turnSession?.characterId == character.id || character.id in savedTurnDrafts
        val activity = ActivityRecord(
            id = "${active.id}-activity-$nextSequence",
            sequence = nextSequence,
            label = label,
            turnNumber = active.currentTurnNumber.takeIf { belongsToTurn },
            turnEvent = event,
            roll = roll,
        )
        updateSelectedCharacter { current ->
            current.copy(
                activePlaySession = active.copy(activities = active.activities + activity),
                hasPlayedSinceLongRest = true,
            )
        }
    }

    private fun recordEvent(event: TurnEvent, label: String = eventLabel(event), session: TurnSession? = turnSession) {
        val activeTurn = session?.takeIf { it.characterId == selectedCharacterId }
        if (activeTurn != null) activeTurn.record(event, label) else recordActivity(event, label)
    }

    private fun hitPointEvent(
        before: CharacterUi,
        after: CharacterUi,
        kind: HitPointChangeKind,
        amount: Int,
        critical: Boolean = false,
    ): TurnEvent.HitPointsChanged = TurnEvent.HitPointsChanged(
        kind = kind,
        amount = amount,
        effectiveHitPointChange = after.hp - before.hp,
        hitPointsBefore = before.hp,
        hitPointsAfter = after.hp,
        temporaryHitPointsBefore = before.temporaryHp,
        temporaryHitPointsAfter = after.temporaryHp,
        critical = critical,
        wentDown = before.hp > 0 && after.hp == 0,
    )

    init {
        characters.indices.forEach { index ->
            val character = characters[index]
            characters[index] = CharacterStatEngine.resolve(
                character,
                conditions.filter { it.characterId.isBlank() || it.characterId == character.id },
            )
        }
        selectedCharacterId = characters.firstOrNull()?.id
    }

    fun t(english: String, german: String): String =
        if (language == UiLanguage.English) english else german

    fun toggleLanguage() {
        language = if (language == UiLanguage.English) UiLanguage.German else UiLanguage.English
        persist()
    }

    fun openCharacter(id: String) {
        turnSession?.takeIf { it.characterId != id }?.let {
            saveTurnDraft()
            turnSession = null
        }
        selectedCharacterId = id
        screen = AppScreen.CharacterSheet
    }

    fun beginCreate() {
        creation.reset()
        screen = AppScreen.CreateCharacter
    }

    fun finishCreate() {
        if (creation.name.isBlank()) {
            creation.step = CreationStep.Identity.ordinal
            showInfo(
                t("Add a name", "Namen hinzufügen"),
                t("Enter a name for your hero.", "Gib deinem Charakter einen Namen."),
            )
            return
        }
        if (!creationSubclassSelectionValid()) {
            creation.step = CreationStep.Build.ordinal
            showInfo(
                t("Choose a subclass", "W\u00e4hle eine Unterklasse"),
                t("A subclass is required at this class level.", "Auf dieser Klassenstufe ist eine Unterklasse erforderlich."),
            )
            return
        }
        if (!creationProficiencySelectionValid()) {
            creation.step = CreationStep.Details.ordinal
            showInfo(
                t("Complete proficiency choices", "Fertigkeitswahlen abschließen"),
                t("Choose a background and complete every required skill choice.", "Wähle einen Hintergrund und schließe alle erforderlichen Fertigkeitswahlen ab."),
            )
            return
        }
        if (!creationSpellSelectionValid()) {
            creation.step = CreationStep.Details.ordinal
            showInfo(
                t("Complete spell choices", "Zauberwahlen abschließen"),
                t(
                    "Choose the required number of legal cantrips and spells for this build.",
                    "Wähle die erforderliche Anzahl legaler Zaubertricks und Zauber für diesen Build.",
                ),
            )
            return
        }
        if (!creationGearSelectionValid()) {
            creation.step = CreationStep.Gear.ordinal
            showInfo(
                t("Choose starting gear", "Startausrüstung wählen"),
                t(
                    "Choose a starting gear package or customize the starting equipment.",
                    "Wähle ein Startausrüstungspaket oder passe die Startausrüstung an.",
                ),
            )
            return
        }
        val id = "character-${characters.size + 1}-${Random.nextInt(10_000)}"
        val caster = CreationSpellRules.limits(
            creation.ruleset,
            creation.className,
            creation.level,
            abilityScoresForDraft(),
        ) != null || privateEntryForName("class", creation.className)?.mechanics?.caster == true
        val portraitResult = creation.portraitBytes?.let { portraitBytes ->
            PortraitEditResult(
                sourceBytes = creation.portraitSourceBytes ?: portraitBytes,
                portraitBytes = portraitBytes,
                crop = creation.portraitCrop ?: PortraitCrop(),
            )
        }
        val portraitAssets = portraitResult?.let { writePortraitAssets(id, it) }
        val portraitSaveFailed = portraitResult != null && portraitAssets == null
        val abilities = abilityScoresForDraft()
        val constitutionModifier = abilityModifier(abilities.getValue("CON"))
        val dexterityModifier = abilityModifier(abilities.getValue("DEX"))
        val hpGains = creationHitPointGains(hitDieFor(creation.className), constitutionModifier)
        val proficiency = if (creation.ruleset == Ruleset.Pf2eRemaster) creation.level + 2 else proficiencyForLevel(creation.level)
        val proficiencyRanks = creationProficiencyRanks()
        val subclassOption = selectedCreationSubclass()
        val subclassStats = subclassOption?.resolveStats(creation.level, proficiency) ?: ResolvedSubclassStatsUi()
        val hitPoints = (hpGains.sum() + subclassStats.hitPointBonus).coerceAtLeast(creation.level)
        val saveNames = linkedMapOf("Strength" to "STR", "Dexterity" to "DEX", "Constitution" to "CON", "Intelligence" to "INT", "Wisdom" to "WIS", "Charisma" to "CHA")
        val saves = saveNames.mapValues { (_, key) ->
            abilityModifier(abilities.getValue(key)) +
                proficiencyModifier(creation.ruleset, creation.level, proficiencyRanks[saveProficiencyId(key)]) +
                subclassStats.savingThrowBonus
        }
        val classUnarmoredArmorClass = if (creation.ruleset == Ruleset.Pf2eRemaster) {
            10 + dexterityModifier + proficiencyModifier(
                creation.ruleset,
                creation.level,
                proficiencyRanks["armor:unarmored"],
            )
        } else when (creation.className) {
            "Barbarian" -> 10 + dexterityModifier + constitutionModifier
            "Monk" -> 10 + dexterityModifier + abilityModifier(abilities.getValue("WIS"))
            else -> 10 + dexterityModifier
        }
        val subclassUnarmoredArmorClass = when (subclassStats.armorFormula) {
            SubclassArmorFormulaUi.NONE -> classUnarmoredArmorClass
            SubclassArmorFormulaUi.DEXTERITY_13 -> 13 + dexterityModifier
            SubclassArmorFormulaUi.DEXTERITY_AND_CHARISMA_10 ->
                10 + dexterityModifier + abilityModifier(abilities.getValue("CHA"))
        }
        val unarmoredArmorClass = subclassUnarmoredArmorClass + subclassStats.armorClassBonus
        val armorProficiency = proficiencyModifier(
            creation.ruleset,
            creation.level,
            proficiencyRanks[creationArmorProficiencyId()],
        )
        val startingArmor = resolvedCreationArmor(dexterityModifier, armorProficiency)
        val armorClass = (startingArmor?.armorClass?.plus(subclassStats.armorClassBonus) ?: unarmoredArmorClass) +
            (startingArmor?.shieldBonus ?: 0)
        val selectedGearWeapons = if (
            creation.selectedStartingGearPackageId != null || creation.startingWeapons.isNotEmpty() || creation.startingEquipment.isNotEmpty()
        ) creation.startingWeapons.toList() else creationWeapons(abilities, proficiencyRanks)
        val weapons = selectedGearWeapons.map { weapon ->
            weapon.copy(
                attackBonus = weapon.attackBonus + subclassStats.attackBonus,
                itemBonus = weapon.itemBonus + subclassStats.attackBonus,
            )
        }
        val features = buildList {
            if (creation.className == "Monk" && creation.ruleset != Ruleset.Pf2eRemaster) {
                add(FeatureUi("martial-arts", "Martial Arts", "Use martial training with Monk weapons and Unarmed Strikes."))
                if (creation.level >= 2) {
                    add(FeatureUi(
                        "focus-points",
                        if (creation.ruleset == Ruleset.Fifth2014) "Ki Points" else "Focus Points",
                        "${creation.level} points fuel Monk techniques.",
                        creation.level,
                        creation.level,
                        Recovery.SHORT_REST,
                    ))
                }
            }
            if (creation.className == "Fighter" && creation.ruleset != Ruleset.Pf2eRemaster) {
                val uses = secondWindUses(creation.ruleset, creation.level)
                add(FeatureUi("second-wind", "Second Wind", "Regain 1d10 + Fighter level (${creation.level}) HP.", uses, uses, Recovery.SHORT_REST, FeatureEffect.SECOND_WIND, ActionCost(bonusActions = 1)))
            }
            if (creation.className == "Fighter" && creation.ruleset != Ruleset.Pf2eRemaster && creation.level >= 2) add(FeatureUi("action-surge", "Action Surge", "Take one additional action this turn.", 1, 1, Recovery.SHORT_REST, FeatureEffect.EXTRA_ACTION))
            if (creation.className == "Fighter" && creation.ruleset != Ruleset.Pf2eRemaster && creation.level >= 5) add(FeatureUi("extra-attack", "Extra Attack", "Attack twice when you take the Attack action."))
            if (creation.className == "Fighter" && creation.ruleset != Ruleset.Pf2eRemaster && creation.level >= 9) {
                val uses = when {
                    creation.level >= 17 -> 3
                    creation.level >= 13 -> 2
                    else -> 1
                }
                add(FeatureUi("indomitable", "Indomitable", "Reroll a failed saving throw and use the new result.", uses, uses, Recovery.LONG_REST, FeatureEffect.REROLL_SAVE))
            }
            creation.selectedFeatIds.mapNotNull(::privateEntryById).forEach { feat ->
                add(feat.toPrivateFeature().copy(id = "private-${feat.id}"))
            }
            privateEntryForName("class", creation.className)?.let { classEntry ->
                add(classEntry.toPrivateFeature().copy(id = "private-class-${classEntry.id}"))
            }
            addAll(automaticPrivateFeatures(
                creation.className,
                creation.ancestry,
                subclassOption?.let { privateEntryForName("subclass", it.name)?.id ?: it.id },
                creation.level,
            ))
            subclassOption?.let { addAll(it.resolveFeatures(creation.level, proficiency, abilities = abilities)) }
            if (isEmpty()) add(FeatureUi("class-feature", creation.className, "Your level ${creation.level} class features are filtered by the selected content pack."))
        }.distinctBy { it.name.lowercase() }
        val created = CharacterStatEngine.resolve(CharacterUi(
            id = id,
            name = creation.name.trim().ifEmpty { t("Unnamed hero", "Namenlose Heldin") },
            ruleset = creation.ruleset,
            level = creation.level,
            ancestry = creation.ancestry,
            className = creation.className,
            subclass = subclassOption?.name ?: "—",
            subclassIdsByClass = subclassOption?.let { mapOf(creation.className to it.id) }.orEmpty(),
            subclassNamesByClass = subclassOption?.let { mapOf(creation.className to it.name) }.orEmpty(),
            hp = hitPoints,
            maxHp = hitPoints,
            armorClass = armorClass,
            unarmoredArmorClass = unarmoredArmorClass,
            speedFeet = speedFor(creation.ruleset, creation.ancestry) + subclassStats.speedBonusFeet,
            flySpeedFeet = if (creation.ancestry == "Aarakocra") 50 else null,
            initiative = (if (creation.ruleset == Ruleset.Pf2eRemaster) {
                abilityModifier(abilities.getValue("WIS")) + proficiencyModifier(
                    creation.ruleset,
                    creation.level,
                    proficiencyRanks["skill:perception"],
                )
            } else dexterityModifier) + subclassStats.initiativeBonus,
            proficiency = proficiency,
            criticalHitThreshold = subclassStats.criticalThreshold,
            initiativeRollMode = subclassStats.initiativeRollMode,
            portraitSeed = characters.size + 2,
            portraitFileName = portraitAssets?.displayFileName,
            portraitSourceFileName = portraitAssets?.sourceFileName,
            portraitCrop = portraitAssets?.crop,
            abilities = abilities,
            skills = ProficiencyCatalog.skills(creation.ruleset, creation.backgroundId).associate { skill ->
                skill.englishName to (abilityModifier(abilities.getValue(skill.ability)) +
                    proficiencyModifier(creation.ruleset, creation.level, proficiencyRanks[skill.id]))
            },
            saves = saves,
            languages = creation.languages.toList().ifEmpty { listOf("Common") },
            weapons = weapons,
            spells = resolvedCreationSpells(),
            features = features,
            equipmentItems = listOfNotNull(startingArmor) + if (
                creation.selectedStartingGearPackageId != null || creation.startingEquipment.isNotEmpty()
            ) creation.startingEquipment else listOf(
                EquipmentUi("explorers-pack", "Explorer's pack"),
                EquipmentUi("bedroll", "Bedroll"),
                EquipmentUi("hempen-rope", "Rope (50 ft)"),
                EquipmentUi("torch", "Torch", EquipmentKind.CONSUMABLE, quantity = 10),
            ),
            currency = CurrencyPurse(gold = creation.startingGoldPieces.coerceAtLeast(0)),
            profile = CharacterProfile(
                characterDescription = creation.characterDescription.trim(),
                motive = creation.motive.trim(),
                alignment = creation.alignment.trim(),
            ),
            hasSpellcastingCapability = caster,
            progression = hpGains.mapIndexed { index, gain ->
                LevelProgressionUi(index + 1, creation.className, gain, if (index == 0) HpMethod.Fixed else creation.hpMethod)
            },
            featIds = creation.selectedFeatIds.toList(),
            derivation = CharacterDerivationUi(
                proficiencyFromLevel = creation.ruleset != Ruleset.Pf2eRemaster,
                initiative = if (creation.ruleset == Ruleset.Pf2eRemaster) {
                    DerivedModifierFormulaUi("WIS", base = subclassStats.initiativeBonus, proficiencyId = "skill:perception")
                } else DerivedModifierFormulaUi("DEX", base = subclassStats.initiativeBonus),
                saves = saveNames.mapValues { (_, ability) ->
                    DerivedModifierFormulaUi(ability, base = subclassStats.savingThrowBonus, proficiencyId = saveProficiencyId(ability))
                },
                skills = ProficiencyCatalog.skills(creation.ruleset, creation.backgroundId).associate { skill ->
                    skill.englishName to DerivedModifierFormulaUi(skill.ability, proficiencyId = skill.id)
                },
            ),
            backgroundId = requireNotNull(creation.backgroundId),
            backgroundName = requireNotNull(selectedCreationBackground()).name(language),
            proficiencyIds = proficiencyRanks.keys,
            proficiencyRanks = proficiencyRanks,
            baseAbilities = abilities,
            baseSaves = saves,
            baseArmorClass = armorClass,
            baseSpeedFeet = speedFor(creation.ruleset, creation.ancestry) + subclassStats.speedBonusFeet,
            baseFlySpeedFeet = if (creation.ancestry == "Aarakocra") 50 else null,
            passiveArmorClassBonus = subclassStats.armorClassBonus,
            privateContentVersions = privateContentVersionsUsedBy(
                className = creation.className,
                ancestry = creation.ancestry,
                subclassId = subclassOption?.let { privateEntryForName("subclass", it.name)?.id ?: it.id },
                selectedEntryIds = creation.selectedFeatIds + creation.selectedSpellIds,
            ),
        ))
        val completedCharacter = withCompleteSkills(created)
        characters += completedCharacter
        if (!persist()) {
            characters.remove(completedCharacter)
            deletePortraitFileIfUnused(portraitAssets?.displayFileName)
            deletePortraitFileIfUnused(portraitAssets?.sourceFileName)
            showInfo(
                t("Character not saved", "Charakter nicht gespeichert"),
                t("Your character could not be stored. Please try again.", "Dein Charakter konnte nicht gespeichert werden. Versuche es erneut."),
            )
            return
        }
        openCharacter(completedCharacter.id)
        if (portraitSaveFailed) {
            showInfo(
                t("Portrait not saved", "Porträt nicht gespeichert"),
                t("The character was created without the selected portrait.", "Der Charakter wurde ohne das ausgewählte Porträt erstellt."),
            )
        }
    }

    fun selectDraftPortrait(bytes: ByteArray) {
        selectPortrait(PortraitPickTarget.Creation, PortraitEditResult(bytes, bytes, PortraitCrop()))
    }

    fun selectPortrait(target: PortraitPickTarget, bytes: ByteArray): Boolean =
        selectPortrait(target, PortraitEditResult(bytes, bytes, PortraitCrop()))

    fun selectPortrait(target: PortraitPickTarget, result: PortraitEditResult): Boolean =
        when (target) {
            PortraitPickTarget.Creation -> {
                creation.portraitBytes = result.portraitBytes
                creation.portraitSourceBytes = result.sourceBytes
                creation.portraitCrop = result.crop
                true
            }
            PortraitPickTarget.Editor -> editorDraft?.let { draft ->
                draft.portraitBytes = result.portraitBytes
                draft.portraitSourceBytes = result.sourceBytes
                draft.portraitCrop = result.crop
                draft.portraitChanged = true
                true
            } ?: false
            is PortraitPickTarget.Character -> replaceCharacterPortrait(target.characterId, result)
        }

    fun portraitEditSource(target: PortraitPickTarget): PortraitEditSource? = when (target) {
        PortraitPickTarget.Creation -> creation.portraitBytes?.let { display ->
            PortraitEditSource(
                sourceBytes = creation.portraitSourceBytes ?: display,
                crop = creation.portraitCrop ?: PortraitCrop(),
                usesRenderedFallback = creation.portraitSourceBytes == null,
            )
        }
        PortraitPickTarget.Editor -> editorDraft?.portraitBytes?.let { display ->
            PortraitEditSource(
                sourceBytes = editorDraft?.portraitSourceBytes ?: display,
                crop = editorDraft?.portraitCrop ?: PortraitCrop(),
                usesRenderedFallback = editorDraft?.portraitSourceBytes == null,
            )
        }
        is PortraitPickTarget.Character -> characters.firstOrNull { it.id == target.characterId }?.let(::portraitEditSource)
    }

    private fun portraitEditSource(character: CharacterUi): PortraitEditSource? {
        val display = portraitBytes(character) ?: return null
        val source = character.portraitSourceFileName?.let(storage::readPortrait)
        return PortraitEditSource(
            sourceBytes = source ?: display,
            crop = character.portraitCrop ?: PortraitCrop(),
            usesRenderedFallback = source == null,
        )
    }

    private fun replaceCharacterPortrait(characterId: String, result: PortraitEditResult): Boolean {
        val index = characters.indexOfFirst { it.id == characterId }
        if (index < 0) return false
        val original = characters[index]
        val assets = writePortraitAssets(characterId, result) ?: return false
        characters[index] = original.copy(
            portraitFileName = assets.displayFileName,
            portraitSourceFileName = assets.sourceFileName,
            portraitCrop = assets.crop,
        )
        if (!persist()) {
            characters[index] = original
            deletePortraitFileIfUnused(assets.displayFileName)
            deletePortraitFileIfUnused(assets.sourceFileName)
            return false
        }
        deletePortraitFileIfUnused(original.portraitFileName)
        deletePortraitFileIfUnused(original.portraitSourceFileName)
        return true
    }

    fun deleteCharacterPortrait(characterId: String = selectedCharacterId.orEmpty()): Boolean {
        val index = characters.indexOfFirst { it.id == characterId }
        if (index < 0) return false
        val original = characters[index]
        val oldDisplayFileName = original.portraitFileName
        val oldSourceFileName = original.portraitSourceFileName
        if (oldDisplayFileName == null && oldSourceFileName == null) return false
        characters[index] = characters[index].copy(
            portraitFileName = null,
            portraitSourceFileName = null,
            portraitCrop = null,
        )
        if (!persist()) {
            characters[index] = original
            return false
        }
        deletePortraitFileIfUnused(oldDisplayFileName)
        deletePortraitFileIfUnused(oldSourceFileName)
        return true
    }

    fun deleteCharacter(characterId: String): Boolean {
        val index = characters.indexOfFirst { it.id == characterId }
        if (index < 0) return false
        val character = characters[index]

        conditions.removeAll { it.characterId == characterId }
        savedTurnDrafts.remove(characterId)
        if (turnSession?.characterId == characterId) {
            turnSession = null
            turnOpen = false
            sessionSaveOpen = false
        }
        if (editorDraft?.original?.id == characterId) cancelEdit()
        if (levelUpDraft?.characterId == characterId) cancelLevelUp()
        if (recentlyLevelledCharacterId == characterId) recentlyLevelledCharacterId = null

        characters.removeAt(index)
        if (selectedCharacterId == characterId) {
            selectedCharacterId = characters.firstOrNull()?.id
            closeSheetAttack()
            conditionsOpen = false
            hpAdjustOpen = false
            quickRollEditorOpen = false
            equipmentAddOpen = false
            turnOpen = false
            sessionHistoryOpen = false
            sessionSaveOpen = false
            conversionOpen = false
            revivalConfirmationOpen = false
            preselectedTurnSection = null
            if (screen == AppScreen.CharacterSheet) screen = AppScreen.Characters
        }

        persist()
        deletePortraitFileIfUnused(character.portraitFileName)
        deletePortraitFileIfUnused(character.portraitSourceFileName)
        return true
    }

    private fun writePortraitAssets(characterId: String, result: PortraitEditResult): StoredPortraitAssets? {
        val displayFileName = storage.writePortrait(characterId, result.portraitBytes) ?: return null
        val sourceFileName = storage.writePortraitSource(characterId, result.sourceBytes)
        if (sourceFileName == null) {
            deletePortraitFileIfUnused(displayFileName)
            return null
        }
        return StoredPortraitAssets(displayFileName, sourceFileName, result.crop)
    }

    private fun deletePortraitFileIfUnused(fileName: String?) {
        val candidate = fileName ?: return
        val used = characters.any {
            it.portraitFileName == candidate || it.portraitSourceFileName == candidate
        }
        if (!used) storage.deletePortrait(candidate)
    }

    private fun CharacterUi.withRebasedEditorValues(edited: CharacterUi): CharacterUi {
        val missingHp = (effectiveMaxHp - hp).coerceAtLeast(0)
        val rebased = copy(
            name = edited.name,
            profile = edited.profile,
            level = edited.level,
            ancestry = edited.ancestry,
            className = edited.className,
            subclass = edited.subclass,
            maxHp = edited.maxHp,
            maxHpReduction = edited.maxHpReduction,
            armorClass = edited.armorClass,
            unarmoredArmorClass = edited.unarmoredArmorClass,
            speedFeet = edited.speedFeet,
            flySpeedFeet = edited.flySpeedFeet,
            initiative = edited.initiative,
            proficiency = edited.proficiency,
            abilities = edited.abilities,
            saves = edited.saves,
            skills = edited.skills,
            weapons = edited.weapons,
            spells = edited.spells,
            derivation = edited.derivation,
            baseAbilities = edited.baseAbilities,
            baseSaves = edited.baseSaves,
            baseArmorClass = edited.baseArmorClass,
            baseSpeedFeet = edited.baseSpeedFeet,
            baseFlySpeedFeet = edited.baseFlySpeedFeet,
            passiveArmorClassBonus = edited.passiveArmorClassBonus,
            portraitFileName = edited.portraitFileName,
            portraitSourceFileName = edited.portraitSourceFileName,
            portraitCrop = edited.portraitCrop,
        )
        val maximumDeath = rebased.ruleset != Ruleset.Pf2eRemaster && rebased.effectiveMaxHp == 0
        return rebased.copy(
            hp = (rebased.effectiveMaxHp - missingHp).coerceIn(0, rebased.effectiveMaxHp),
            isDead = maximumDeath || isDead,
            deathReason = if (maximumDeath) "Maximum Hit Points are zero" else deathReason,
        )
    }

    private fun createTurnSession(character: CharacterUi, restored: TurnSessionSnapshotUi?): TurnSession =
        TurnSession(character, restored, onEvent = { event, label ->
            recordActivity(event, label ?: eventLabel(event))
        })

    private fun rebaseTurnAfterCharacterEdit(edited: CharacterUi) {
        val liveSession = turnSession?.takeIf { it.characterId == edited.id }
        val snapshot = liveSession?.snapshot() ?: savedTurnDrafts[edited.id] ?: return
        val rebasedSnapshot = snapshot.copy(
            baseline = snapshot.baseline.withRebasedEditorValues(edited),
        )
        savedTurnDrafts[edited.id] = rebasedSnapshot
        if (liveSession != null) turnSession = createTurnSession(edited, rebasedSnapshot)
    }

    private fun parkLiveTurn(characterId: String) {
        val session = turnSession?.takeIf { it.characterId == characterId } ?: return
        savedTurnDrafts[characterId] = session.snapshot()
        turnSession = null
        turnOpen = false
    }

    fun beginEdit(
        focusAbility: String? = null,
        section: EditorSection? = null,
        characterId: String? = null,
    ) {
        val character = characters.firstOrNull { it.id == (characterId ?: selectedCharacterId) } ?: return
        if (turnSession?.characterId != null && turnSession?.characterId != character.id) {
            saveTurnDraft()
            turnSession = null
        }
        turnOpen = false
        selectedCharacterId = character.id
        editorDraft = CharacterEditorDraft(
            character = character,
            portrait = portraitBytes(character),
            portraitSource = portraitEditSource(character),
        ).also { draft ->
            draft.section = section ?: if (focusAbility != null) EditorSection.Abilities else EditorSection.Hub
        }
        editorOpen = true
    }

    fun cancelEdit() {
        editorOpen = false
        editorDraft = null
    }

    fun matchingPrivateContentCount(draft: CharacterEditorDraft): Int = matchingPrivateEntries(draft).size

    fun linkPrivateContent(draft: CharacterEditorDraft) {
        if (matchingPrivateEntries(draft).isNotEmpty()) draft.linkPrivateContent = true
    }

    private fun matchingPrivateEntries(draft: CharacterEditorDraft): List<PrivateEntryUi> {
        if (draft.ruleset != Ruleset.Fifth2024) return emptyList()
        val classEntry = privateEntryForName("class", draft.className, draft.ruleset)
        val speciesEntry = privateEntryForName("ancestry", draft.ancestry, draft.ruleset)
        val subclassEntry = approvedPrivateEntries("subclass", draft.ruleset).firstOrNull { entry ->
            entry.name.equals(draft.subclass, true) && entry.mechanics.parentClassId == classEntry?.id
        }
        val parents = setOfNotNull(classEntry?.id, speciesEntry?.id, subclassEntry?.id)
        val children = privateEntries.filter { entry ->
            entry.mechanics.parentClassId in parents ||
                entry.mechanics.parentSpeciesId in parents ||
                entry.mechanics.parentSubclassId in parents
        }.filter { (it.mechanics.unlockLevel ?: 1) <= draft.level }
        return (listOfNotNull(classEntry, speciesEntry, subclassEntry) + children).distinctBy { it.id }
    }

    private fun applyPrivateContent(character: CharacterUi, draft: CharacterEditorDraft): CharacterUi {
        val matching = matchingPrivateEntries(draft)
        if (matching.isEmpty()) return character
        val classEntry = matching.firstOrNull { it.normalizedKind() == "class" }
        val speciesEntry = matching.firstOrNull { it.normalizedKind() == "ancestry" }
        val subclassEntry = matching.firstOrNull { it.normalizedKind() == "subclass" }
        val linkedFeatures = matching.filter { it.normalizedKind() in setOf("class", "ancestry", "subclass", "feature", "feat") }
            .map { entry -> entry.toPrivateFeature().copy(id = "private-linked-${entry.id}") }
        val grantedSpellIds = matching.flatMap { it.mechanics.grantedSpellIds }.toSet()
        val linkedSpells = grantedSpellIds.mapNotNull(::privateEntryById).mapNotNull(::privateSpell)
        val usedPackIds = (matching + grantedSpellIds.mapNotNull(::privateEntryById)).mapNotNull { it.sourcePackId }.toSet()
        val usedPacks = installedPrivatePacks.filter { it.id in usedPackIds }
        return character.copy(
            subclassIdsByClass = if (subclassEntry == null) character.subclassIdsByClass else character.subclassIdsByClass + (character.className to subclassEntry.id),
            subclassNamesByClass = if (subclassEntry == null) character.subclassNamesByClass else character.subclassNamesByClass + (character.className to subclassEntry.name),
            speedFeet = speciesEntry?.mechanics?.speedFeet ?: character.speedFeet,
            baseSpeedFeet = speciesEntry?.mechanics?.speedFeet ?: character.baseSpeedFeet,
            hitDieOverrides = classEntry?.mechanics?.hitDie?.let { character.hitDieOverrides + (character.className to it) } ?: character.hitDieOverrides,
            hasSpellcastingCapability = classEntry?.mechanics?.caster ?: character.hasSpellcastingCapability,
            features = (character.features.filterNot { it.id.startsWith("private-linked-") } + linkedFeatures).distinctBy { it.id },
            spells = (character.spells + linkedSpells).distinctBy { it.id },
            privateContentVersions = (character.privateContentVersions.filterNot { previous -> usedPacks.any { it.id == previous.id } } + usedPacks),
        )
    }

    fun saveEdit(): Boolean {
        val draft = editorDraft ?: return false
        if (!draft.isValid) return false
        val original = draft.original
        val proficiency = if (draft.proficiencyManual) draft.proficiency else {
            if (draft.ruleset == Ruleset.Pf2eRemaster) draft.level + 2 else proficiencyForLevel(draft.level)
        }
        val newAbilities = linkedMapOf<String, Int>().apply {
            listOf("STR", "DEX", "CON", "INT", "WIS", "CHA").forEach { ability ->
                put(ability, draft.abilities[ability] ?: 10)
            }
        }
        val initiative = if (draft.initiativeManual) draft.initiative else {
            original.derivation.initiative?.resolve(newAbilities, proficiency, draft.ruleset, draft.level, original.proficiencyRanks, original.proficiencyIds) ?: draft.initiative
        }
        val saves = original.saves.mapValues { (name, value) ->
            original.derivation.saves[name]?.resolve(newAbilities, proficiency, draft.ruleset, draft.level, original.proficiencyRanks, original.proficiencyIds) ?: value
        }
        val skills = original.skills.mapValues { (name, value) ->
            original.derivation.skills[name]?.resolve(newAbilities, proficiency, draft.ruleset, draft.level, original.proficiencyRanks, original.proficiencyIds) ?: value
        }
        val missingHp = (original.effectiveMaxHp - original.hp).coerceAtLeast(0)
        val updatedReduction = original.maxHpReduction.coerceAtMost(draft.maxHp)
        val updatedEffectiveMaximum = original.copy(
            ruleset = draft.ruleset,
            maxHp = draft.maxHp,
            maxHpReduction = updatedReduction,
        ).effectiveMaxHp
        val maximumDeath = draft.ruleset != Ruleset.Pf2eRemaster && updatedEffectiveMaximum == 0
        val base = original.copy(
            name = draft.name.trim(),
            profile = CharacterProfile(
                characterDescription = draft.characterDescription.trim(),
                motive = draft.motive.trim(),
                alignment = draft.alignment.trim(),
            ),
            level = draft.level,
            ancestry = draft.ancestry.trim().ifBlank { original.ancestry },
            className = draft.className.trim().ifBlank { original.className },
            subclass = draft.subclass.trim().ifBlank { "—" },
            hp = (updatedEffectiveMaximum - missingHp).coerceIn(0, updatedEffectiveMaximum),
            maxHp = draft.maxHp,
            maxHpReduction = updatedReduction,
            isDead = maximumDeath || original.isDead,
            deathReason = if (maximumDeath) "Maximum Hit Points are zero" else original.deathReason,
            armorClass = draft.armorClass,
            unarmoredArmorClass = draft.armorClass,
            speedFeet = draft.speedFeet,
            flySpeedFeet = draft.flySpeedFeet?.takeIf { it > 0 },
            initiative = initiative,
            proficiency = proficiency,
            abilities = newAbilities,
            saves = saves,
            skills = skills,
            weapons = original.weapons.map { weapon ->
                recalculateWeapon(
                    weapon,
                    newAbilities,
                    proficiency,
                    draft.ruleset,
                    draft.level,
                    original.proficiencyRanks,
                    original.proficiencyIds,
                )
            },
            spells = draft.spells.toList(),
            derivation = original.derivation.copy(
                proficiencyFromLevel = !draft.proficiencyManual,
                initiative = if (draft.initiativeManual) null else original.derivation.initiative,
            ),
            baseAbilities = newAbilities,
            baseSaves = saves,
            baseArmorClass = draft.armorClass,
            baseSpeedFeet = draft.speedFeet,
            baseFlySpeedFeet = draft.flySpeedFeet?.takeIf { it > 0 },
        ).let { if (draft.linkPrivateContent) applyPrivateContent(it, draft) else it }
        val existingIndex = characters.indexOfFirst { it.id == original.id }
        if (existingIndex < 0) return false
        val charactersBeforeEdit = characters.toList()
        val selectedCharacterBeforeEdit = selectedCharacterId
        val turnSessionBeforeEdit = turnSession
        val savedTurnDraftsBeforeEdit = savedTurnDrafts.toMap()
        val turnOpenBeforeEdit = turnOpen

        val portraitResult = if (draft.portraitChanged) {
            val portraitBytes = draft.portraitBytes ?: return false
            PortraitEditResult(
                sourceBytes = draft.portraitSourceBytes ?: portraitBytes,
                portraitBytes = portraitBytes,
                crop = draft.portraitCrop ?: PortraitCrop(),
            )
        } else null
        val replacedPortraitFiles = mutableSetOf<String>()
        val newPortraitFiles = mutableSetOf<String>()
        if (draft.ruleset == original.ruleset) {
            val assets = portraitResult?.let { writePortraitAssets(original.id, it) }
            if (portraitResult != null && assets == null) {
                showInfo(
                    t("Portrait not saved", "Porträt nicht gespeichert"),
                    t("The previous portrait is still intact. Please try again.", "Das vorherige Porträt ist weiterhin vorhanden. Versuche es erneut."),
                )
                return false
            }
            val edited = withCompleteSkills(
                CharacterStatEngine.resolve(
                    if (assets == null) base else base.copy(
                        portraitFileName = assets.displayFileName,
                        portraitSourceFileName = assets.sourceFileName,
                        portraitCrop = assets.crop,
                    ),
                    conditions.filter { it.characterId.isBlank() || it.characterId == original.id },
                ),
            )
            characters[existingIndex] = edited
            if (assets != null) {
                newPortraitFiles += assets.displayFileName
                newPortraitFiles += assets.sourceFileName
                original.portraitFileName?.let(replacedPortraitFiles::add)
                original.portraitSourceFileName?.let(replacedPortraitFiles::add)
            }
            selectedCharacterId = original.id
            rebaseTurnAfterCharacterEdit(edited)
        } else {
            val convertedId = "conversion-${characters.size + 1}-${Random.nextInt(10_000)}"
            val assets = portraitResult?.let { writePortraitAssets(convertedId, it) }
            if (portraitResult != null && assets == null) {
                showInfo(
                    t("Portrait not saved", "Porträt nicht gespeichert"),
                    t("The conversion was not created. Please try again.", "Die Konvertierung wurde nicht erstellt. Versuche es erneut."),
                )
                return false
            }
            val convertedBase = base.copy(
                id = convertedId,
                ruleset = draft.ruleset,
                sourceCharacterId = original.id,
                notes = base.notes + CharacterNote(
                    id = uniqueId("ruleset-conversion", base.notes.map { it.id }),
                    title = t("Ruleset conversion", "Regelwerk-Konvertierung"),
                    body = t(
                        "Edited as a ${draft.ruleset.shortLabel} copy; build content was preserved for review.",
                        "Als ${draft.ruleset.shortLabel}-Kopie bearbeitet; Build-Inhalte wurden zur Prüfung beibehalten.",
                    ),
                ),
            )
            val converted = CharacterStatEngine.resolve(if (assets == null) convertedBase else convertedBase.copy(
                portraitFileName = assets.displayFileName,
                portraitSourceFileName = assets.sourceFileName,
                portraitCrop = assets.crop,
            ), conditions.filter { it.characterId.isBlank() || it.characterId == convertedId })
            if (assets != null) {
                newPortraitFiles += assets.displayFileName
                newPortraitFiles += assets.sourceFileName
            }
            characters += withCompleteSkills(converted)
            selectedCharacterId = converted.id
            parkLiveTurn(original.id)
        }
        if (!persist()) {
            characters.clear()
            characters.addAll(charactersBeforeEdit)
            selectedCharacterId = selectedCharacterBeforeEdit
            turnSession = turnSessionBeforeEdit
            savedTurnDrafts.clear()
            savedTurnDrafts.putAll(savedTurnDraftsBeforeEdit)
            turnOpen = turnOpenBeforeEdit
            newPortraitFiles.forEach(::deletePortraitFileIfUnused)
            showInfo(
                t("Character not saved", "Charakter nicht gespeichert"),
                t("Your changes could not be stored. Please try again.", "Deine Änderungen konnten nicht gespeichert werden. Versuche es erneut."),
            )
            return false
        }
        replacedPortraitFiles.forEach(::deletePortraitFileIfUnused)
        editorOpen = false
        editorDraft = null
        return true
    }

    fun rollCreationAbilityScores() {
        creation.rolledScores.clear()
        repeat(6) {
            creation.rolledScores += dice.roll(
                RollRequest("Ability", DiceExpression(count = 4, sides = 6, keepHighest = 3))
            ).total
        }
        assignRolledScoresForCurrentClass()
    }

    fun swapCreationRolledScores(firstAbility: String, secondAbility: String): Boolean {
        if (
            creation.statMethod != StatMethod.Rolled ||
            firstAbility !in creationAbilityOrder ||
            secondAbility !in creationAbilityOrder ||
            firstAbility == secondAbility
        ) return false
        ensureRolledAbilityAssignments()
        val firstScore = creation.rolledAbilityAssignments[firstAbility] ?: return false
        val secondScore = creation.rolledAbilityAssignments[secondAbility] ?: return false
        creation.rolledAbilityAssignments[firstAbility] = secondScore
        creation.rolledAbilityAssignments[secondAbility] = firstScore
        creation.rolledAssignmentEdited = true
        return true
    }

    fun setCreationLevel(level: Int) {
        val safeLevel = level.coerceIn(1, 20)
        if (safeLevel == creation.level) return
        creation.level = safeLevel
        trimCreationSpellSelection()
        creation.subclassAdvisory = creationSubclassAdvisory()
        creation.rolledHpGains.clear()
        if (creation.skillRankChoices.isNotEmpty()) {
            creation.skillRankChoices.clear()
            creation.proficiencyAdvisory = t(
                "Skill increases were cleared for the new level.",
                "Fertigkeitssteigerungen wurden für die neue Stufe zurückgesetzt.",
            )
        }
    }

    fun selectCreationRuleset(ruleset: Ruleset) {
        if (ruleset == creation.ruleset) return
        creation.ruleset = ruleset
        creation.ancestry = "Human"
        creation.className = "Fighter"
        creation.subclassId = null
        creation.subclassName = ""
        creation.subclassAdvisory = null
        creation.statMethod = if (ruleset == Ruleset.Pf2eRemaster) StatMethod.StandardArray else StatMethod.Rolled
        creation.hpMethod = HpMethod.Fixed
        creation.rolledScores.clear()
        creation.rolledAbilityAssignments.clear()
        creation.rolledAssignmentEdited = false
        creation.rolledHpGains.clear()
        creation.selectedFeatIds.clear()
        creation.selectedSpellIds.clear()
        creation.backgroundId = null
        creation.backgroundSkillIds.clear()
        creation.classSkillIds.clear()
        creation.featSkillIds.clear()
        creation.skillRankChoices.clear()
        clearCreationStartingGear()
        creation.proficiencyAdvisory = t(
            "Choose a background and skills for the new ruleset.",
            "Wähle Hintergrund und Fertigkeiten für das neue Regelwerk.",
        )
    }

    fun selectCreationClass(className: String) {
        if (className == creation.className) return
        creation.className = className
        creation.subclassId = null
        creation.subclassName = ""
        creation.subclassAdvisory = null
        creation.rolledHpGains.clear()
        creation.selectedSpellIds.clear()
        if (
            creation.statMethod == StatMethod.Rolled &&
            creation.rolledScores.size == creationAbilityOrder.size &&
            !creation.rolledAssignmentEdited
        ) {
            assignRolledScoresForCurrentClass()
        }
        creation.classSkillIds.clear()
        creation.featSkillIds.clear()
        creation.skillRankChoices.clear()
        clearCreationStartingGear()
        creation.proficiencyAdvisory = t(
            "Choose the skills granted by the new class.",
            "Wähle die Fertigkeiten der neuen Klasse.",
        )
    }

    internal fun creationBackgroundOptions(): List<BackgroundDefinitionUi> = (
        ProficiencyCatalog.backgrounds(creation.ruleset) + approvedPrivateEntries("background").map { entry ->
            BackgroundDefinitionUi(
                id = entry.id,
                englishName = entry.name,
                germanName = entry.name,
                grantedSkillIds = entry.mechanics.grantedSkillIds.map(ProficiencyCatalog::normalizeSkillId).toSet(),
                customSkillCount = entry.mechanics.skillChoiceCount,
            )
        }
    ).distinctBy { it.id }

    internal fun selectedCreationBackground(): BackgroundDefinitionUi? =
        creationBackgroundOptions().firstOrNull { it.id == creation.backgroundId }

    fun selectCreationBackground(backgroundId: String) {
        val selected = creationBackgroundOptions().firstOrNull { it.id == backgroundId } ?: return
        if (creation.backgroundId == selected.id) return
        val hadChoices = creation.backgroundId != null || creation.classSkillIds.isNotEmpty() || creation.skillRankChoices.isNotEmpty()
        creation.backgroundId = selected.id
        creation.backgroundSkillIds.clear()
        val newlyGranted = selected.allGrantedSkillIds
        creation.classSkillIds.removeAll { it in newlyGranted }
        creation.featSkillIds.removeAll { it in newlyGranted }
        creation.skillRankChoices.clear()
        creation.proficiencyAdvisory = if (hadChoices) t(
            "Selections that overlapped the new background were cleared.",
            "Wahlen, die sich mit dem neuen Hintergrund überschnitten, wurden entfernt.",
        ) else null
    }

    fun creationBackgroundSkillCount(): Int = selectedCreationBackground()?.customSkillCount ?: 0

    fun toggleCreationBackgroundSkill(skillId: String) {
        val count = creationBackgroundSkillCount()
        if (count == 0 || ProficiencyCatalog.fiveESkills.none { it.id == skillId }) return
        when {
            skillId in creation.backgroundSkillIds -> creation.backgroundSkillIds.remove(skillId)
            creation.backgroundSkillIds.size < count -> {
                creation.backgroundSkillIds += skillId
                creation.classSkillIds.remove(skillId)
                creation.featSkillIds.remove(skillId)
            }
        }
        creation.skillRankChoices.clear()
    }

    fun creationClassSkillCount(): Int {
        val definition = ProficiencyCatalog.classDefinition(creation.ruleset, creation.className)
        if (creation.ruleset != Ruleset.Pf2eRemaster) return definition.classSkillCount
        val intelligenceModifier = abilityModifier(abilityScoresForDraft()["INT"] ?: 10)
        val overlapReplacements = definition.fixedSkillIds.count { it in creationBackgroundGrantedSkillIds() }
        return (definition.pf2AdditionalSkills + intelligenceModifier).coerceAtLeast(0) + overlapReplacements
    }

    internal fun creationClassSkillOptions(): List<CreationSkillOptionUi> {
        val definition = ProficiencyCatalog.classDefinition(creation.ruleset, creation.className)
        val unavailable = creationBackgroundGrantedSkillIds() + creation.backgroundSkillIds + definition.fixedSkillIds
        return ProficiencyCatalog.skills(creation.ruleset, creation.backgroundId)
            .filter { it.id in definition.classSkillIds && it.id !in unavailable }
            .map { skill -> creationSkillOption(skill, creationBaseProficiencyRanks()) }
    }

    fun toggleCreationClassSkill(skillId: String) {
        if (creationClassSkillOptions().none { it.id == skillId }) return
        val count = creationClassSkillCount()
        when {
            skillId in creation.classSkillIds -> creation.classSkillIds.remove(skillId)
            creation.classSkillIds.size < count -> creation.classSkillIds += skillId
        }
        creation.skillRankChoices.clear()
    }

    fun creationFeatSkillCount(): Int = if (creation.ruleset != Ruleset.Pf2eRemaster && "skilled" in creation.selectedFeatIds) 3 else 0

    internal fun creationFeatSkillOptions(): List<CreationSkillOptionUi> {
        val unavailable = creationBackgroundGrantedSkillIds() + creation.backgroundSkillIds + creation.classSkillIds
        return ProficiencyCatalog.fiveESkills.filterNot { it.id in unavailable }
            .map { skill -> creationSkillOption(skill, creationBaseProficiencyRanks()) }
    }

    fun toggleCreationFeatSkill(skillId: String) {
        if (creationFeatSkillOptions().none { it.id == skillId }) return
        val count = creationFeatSkillCount()
        when {
            skillId in creation.featSkillIds -> creation.featSkillIds.remove(skillId)
            creation.featSkillIds.size < count -> creation.featSkillIds += skillId
        }
    }

    fun creationSkillIncreaseCount(): Int = if (creation.ruleset != Ruleset.Pf2eRemaster) 0 else {
        if (creation.className == "Rogue") (creation.level - 1).coerceAtLeast(0)
        else ((creation.level - 1) / 2).coerceAtLeast(0)
    }

    fun creationSkillIncreaseCost(): Int {
        val base = creationBaseProficiencyRanks()
        return creation.skillRankChoices.entries.sumOf { (id, rank) ->
            (rank.rankIndex() - (base[id]?.rankIndex() ?: 0)).coerceAtLeast(0)
        }
    }

    internal fun creationRankSkillOptions(): List<CreationSkillOptionUi> =
        ProficiencyCatalog.skills(creation.ruleset, creation.backgroundId).map { skill ->
            creationSkillOption(skill, creationProficiencyRanks())
        }

    fun cycleCreationSkillRank(skillId: String) {
        if (creation.ruleset != Ruleset.Pf2eRemaster) return
        val baseRanks = creationBaseProficiencyRanks()
        val base = baseRanks[skillId]
        val current = creation.skillRankChoices[skillId] ?: base
        val next = when (current) {
            null -> ProficiencyRank.TRAINED
            ProficiencyRank.TRAINED -> ProficiencyRank.EXPERT
            ProficiencyRank.EXPERT -> ProficiencyRank.MASTER.takeIf { creation.level >= 7 }
            ProficiencyRank.MASTER -> ProficiencyRank.LEGENDARY.takeIf { creation.level >= 15 }
            ProficiencyRank.LEGENDARY -> null
        }
        if (next == null || next.rankIndex() <= (base?.rankIndex() ?: 0)) {
            creation.skillRankChoices.remove(skillId)
            return
        }
        val oldCost = creationSkillIncreaseCost()
        val oldRank = creation.skillRankChoices[skillId]
        creation.skillRankChoices[skillId] = next
        if (creationSkillIncreaseCost() > creationSkillIncreaseCount()) {
            if (oldRank == null) creation.skillRankChoices.remove(skillId) else creation.skillRankChoices[skillId] = oldRank
        } else if (creationSkillIncreaseCost() == oldCost && oldRank == next) {
            creation.skillRankChoices.remove(skillId)
        }
    }

    fun creationProficiencyRanks(): Map<String, ProficiencyRank> =
        creationBaseProficiencyRanks() + creation.skillRankChoices

    fun creationProficiencySelectionValid(): Boolean {
        val background = selectedCreationBackground() ?: return false
        if (creation.backgroundSkillIds.size != background.customSkillCount) return false
        if (creation.backgroundSkillIds.any { id -> ProficiencyCatalog.fiveESkills.none { it.id == id } }) return false
        val definition = ProficiencyCatalog.classDefinition(creation.ruleset, creation.className)
        val unavailable = creationBackgroundGrantedSkillIds() + creation.backgroundSkillIds + definition.fixedSkillIds
        if (creation.classSkillIds.size != creationClassSkillCount()) return false
        if (creation.classSkillIds.any { it !in definition.classSkillIds || it in unavailable }) return false
        if (definition.requiredOneOfSkillGroups.any { group -> creation.classSkillIds.none { it in group } }) return false
        if (creation.featSkillIds.size != creationFeatSkillCount()) return false
        if (creation.featSkillIds.any { it in creationBackgroundGrantedSkillIds() || it in creation.backgroundSkillIds || it in creation.classSkillIds }) return false
        return creationSkillIncreaseCost() == creationSkillIncreaseCount()
    }

    fun creationBackgroundGrantedSkillIds(): Set<String> =
        selectedCreationBackground()?.allGrantedSkillIds.orEmpty()

    fun localizedSkillName(nameOrId: String): String =
        ProficiencyCatalog.skillByDisplayName(selectedCharacter?.ruleset ?: creation.ruleset, nameOrId)?.name(language) ?: nameOrId

    private fun creationBaseProficiencyRanks(): Map<String, ProficiencyRank> {
        val definition = ProficiencyCatalog.classDefinition(creation.ruleset, creation.className)
        return buildMap {
            putAll(definition.automaticRanks)
            (creationBackgroundGrantedSkillIds() + creation.backgroundSkillIds + definition.fixedSkillIds +
                creation.classSkillIds + creation.featSkillIds).forEach { put(it, ProficiencyRank.TRAINED) }
        }
    }

    private fun creationSkillOption(
        skill: SkillDefinitionUi,
        ranks: Map<String, ProficiencyRank>,
    ): CreationSkillOptionUi {
        val rank = ranks[skill.id]
        val modifier = abilityModifier(abilityScoresForDraft()[skill.ability] ?: 10) +
            proficiencyModifier(creation.ruleset, creation.level, rank)
        return CreationSkillOptionUi(skill.id, skill.name(language), skill.ability, modifier, rank, skill.id in creationBackgroundGrantedSkillIds())
    }

    fun rollCreationHitPoints() {
        if (creation.ruleset == Ruleset.Pf2eRemaster) return
        val sides = hitDieFor(creation.className)
        creation.rolledHpGains.clear()
        repeat((creation.level - 1).coerceAtLeast(0)) {
            creation.rolledHpGains += dice.roll(RollRequest("Level HP", DiceExpression(1, sides))).total
        }
    }

    fun creationAbilityMethodExplanation(): String = when (creation.statMethod) {
        StatMethod.Rolled -> t(
            "Six scores are rolled with 4d6, dropping the lowest die each time. The totals are sorted from highest to lowest and assigned using ${creation.className}'s ability priority. Rerolling replaces all six scores.",
            "Sechs Werte werden mit 4W6 gewürfelt; jeweils der niedrigste Würfel entfällt. Die Ergebnisse werden absteigend sortiert und nach der Attributspriorität von ${creation.className} verteilt. Neu würfeln ersetzt alle sechs Werte.",
        )
        StatMethod.StandardArray -> if (creation.ruleset == Ruleset.Pf2eRemaster) {
            t(
                "The fixed scores 18, 16, 14, 12, 10, and 8 are assigned using ${creation.className}'s ability priority.",
                "Die festen Werte 18, 16, 14, 12, 10 und 8 werden nach der Attributspriorität von ${creation.className} verteilt.",
            )
        } else {
            t(
                "The fixed scores 15, 14, 13, 12, 10, and 8 are assigned using ${creation.className}'s ability priority.",
                "Die festen Werte 15, 14, 13, 12, 10 und 8 werden nach der Attributspriorität von ${creation.className} verteilt.",
            )
        }
        StatMethod.PointBuy -> t(
            "The 27-point preset 15, 15, 14, 10, 8, and 8 is assigned using ${creation.className}'s ability priority.",
            "Die 27-Punkte-Vorgabe 15, 15, 14, 10, 8 und 8 wird nach der Attributspriorität von ${creation.className} verteilt.",
        )
        StatMethod.Manual -> t(
            "Each entered score is used exactly as shown. The app does not reorder manual values.",
            "Jeder eingegebene Wert wird genau wie angezeigt verwendet. Manuelle Werte werden von der App nicht neu angeordnet.",
        )
    }

    fun creationAncestryOptions(): List<String> {
        val builtIn = if (creation.ruleset == Ruleset.Pf2eRemaster) {
            listOf("Human", "Elf", "Dwarf", "Goblin", "Orc", "Gnome", "Halfling", "Leshy")
        } else {
            listOf("Human", "Elf", "Dwarf", "Halfling", "Dragonborn", "Gnome", "Orc", "Tiefling", "Aasimar", "Goliath", "Aarakocra")
        }
        return (builtIn + approvedPrivateEntries("ancestry").map { it.name }).distinct()
    }

    fun creationClassOptions(): List<String> {
        val builtIn = if (creation.ruleset == Ruleset.Pf2eRemaster) {
            listOf("Fighter", "Wizard", "Cleric", "Rogue", "Ranger", "Bard", "Druid", "Champion", "Sorcerer", "Alchemist", "Barbarian", "Monk")
        } else {
            listOf("Fighter", "Wizard", "Cleric", "Rogue", "Ranger", "Bard", "Druid", "Paladin", "Sorcerer", "Warlock", "Barbarian", "Monk")
        }
        return (builtIn + approvedPrivateEntries("class").map { it.name }).distinct()
    }

    fun creationSubclassOptions(): List<SubclassOptionUi> {
        return subclassOptions(creation.ruleset, creation.className)
    }

    private fun subclassOptions(ruleset: Ruleset, className: String): List<SubclassOptionUi> {
        if (ruleset == Ruleset.Pf2eRemaster) return emptyList()
        val builtIn = BuiltInSubclassCatalog.forClass(ruleset, className)
        val local = approvedPrivateEntries("subclass", ruleset).mapNotNull { entry ->
            val parentClass = entry.mechanics.parentClassId?.let(::privateEntryById)?.name
            val generated = parentClass?.let {
                SubclassMechanicsUi(
                    parentClassName = it,
                    ruleset = Ruleset.Fifth2024,
                    selectionLevel = entry.mechanics.selectionLevel ?: 3,
                    features = privateEntries.filter { child ->
                        child.mechanics.parentSubclassId == entry.id && child.normalizedKind() == "feature"
                    }
                        .map { child ->
                            SubclassFeatureGrantUi(
                                minimumClassLevel = child.mechanics.unlockLevel ?: 1,
                                feature = child.toPrivateFeature(),
                            )
                        },
                    spells = privateEntries.filter { child -> child.mechanics.parentSubclassId == entry.id && child.normalizedKind() == "spell" }
                        .mapNotNull { child -> privateSpell(child)?.let { SubclassSpellGrantUi(child.mechanics.unlockLevel ?: it.level.coerceAtLeast(1), it) } },
                )
            }
            (entry.subclass ?: generated)
                ?.takeIf { it.ruleset == ruleset && it.parentClassName.equals(className, true) }
                ?.let { mechanics ->
                    SubclassOptionUi(
                        id = entry.id,
                        name = entry.name,
                        summaryEnglish = entry.privateContentSummary(),
                        summaryGerman = entry.privateContentSummary(),
                        mechanics = mechanics,
                        local = true,
                        sourceNote = entry.sourceNote,
                    )
                }
        }
        return (builtIn + local).distinctBy { it.id }.sortedBy { it.name }
    }

    private fun subclassArmorClassBonus(character: CharacterUi): Int {
        val classLevels = character.progression.groupingBy(LevelProgressionUi::className).eachCount()
        return character.subclassIdsByClass.entries.sumOf { (className, subclassId) ->
            val option = subclassOptions(character.ruleset, className).firstOrNull {
                it.id == subclassId || it.name.equals(character.subclassNamesByClass[className], true)
            }
            option?.resolveStats(classLevels[className] ?: 0, character.proficiency)?.armorClassBonus ?: 0
        }
    }

    fun selectedCreationSubclass(): SubclassOptionUi? =
        creation.subclassId?.let { id -> creationSubclassOptions().firstOrNull { it.id == id } }

    fun selectCreationSubclass(subclassId: String?) {
        if (subclassId == null) {
            creation.subclassId = null
            creation.subclassName = ""
            creation.subclassAdvisory = null
            return
        }
        val selected = creationSubclassOptions().firstOrNull { it.id == subclassId } ?: return
        creation.subclassId = selected.id
        creation.subclassName = selected.name
        creation.subclassAdvisory = creationSubclassAdvisory(selected)
    }

    fun addCustomSubclass(name: String, summary: String, mechanics: SubclassMechanicsUi) {
        val cleanName = name.trim()
        if (cleanName.isEmpty() || mechanics.parentClassName != creation.className || mechanics.ruleset != creation.ruleset) return
        val id = uniqueId("custom-subclass-${slug(cleanName)}", privateEntries.map { it.id })
        privateEntries += PrivateEntryUi(
            id = id,
            kind = "Subclass",
            name = cleanName,
            summary = summary.trim(),
            formula = "ruleset=${creation.ruleset.name}",
            sourceNote = "Local custom subclass",
            subclass = mechanics,
        )
        persist()
        selectCreationSubclass(id)
    }

    fun creationSubclassRequired(): Boolean {
        if (creation.ruleset == Ruleset.Pf2eRemaster) return false
        val selectionLevel = selectedCreationSubclass()?.mechanics?.selectionLevel
            ?: defaultSubclassSelectionLevel(creation.ruleset, creation.className)
        return creation.level >= selectionLevel
    }

    fun creationSubclassSelectionValid(): Boolean = !creationSubclassRequired() || selectedCreationSubclass() != null

    fun creationSubclassAdvisory(option: SubclassOptionUi? = selectedCreationSubclass()): String? {
        val selected = option ?: return null
        return if (creation.level < selected.mechanics.selectionLevel) {
            t(
                "Selected now; its mechanics start at ${creation.className} level ${selected.mechanics.selectionLevel}.",
                "Jetzt gew\u00e4hlt; die Regeln gelten ab Stufe ${selected.mechanics.selectionLevel} in ${creation.className}.",
            )
        } else null
    }

    private fun defaultSubclassSelectionLevel(ruleset: Ruleset, className: String): Int = when (ruleset) {
        Ruleset.Fifth2024 -> 3
        Ruleset.Fifth2014 -> when (className) {
            "Cleric", "Sorcerer", "Warlock" -> 1
            "Druid", "Wizard" -> 2
            else -> 3
        }
        Ruleset.Pf2eRemaster -> Int.MAX_VALUE
    }

    fun creationFeatOptions(): List<FeatOptionUi> {
        val builtIn = if (creation.ruleset == Ruleset.Pf2eRemaster) emptyList() else listOf(
            FeatOptionUi("ability-score-improvement", "Ability Score Improvement", "Increase your key ability scores.", "Reliable for every build"),
            FeatOptionUi("tough", "Tough", "Gain additional Hit Points as you level.", "Helpful for a front-line character"),
            FeatOptionUi("alert", "Alert", "Improve initiative and awareness.", "Helpful when acting early matters"),
            FeatOptionUi("skilled", "Skilled", "Gain additional skill proficiencies.", "Flexible outside combat"),
        )
        return builtIn + approvedPrivateFeatOptions()
    }

    fun approvedPrivateFeatOptions(character: CharacterUi? = null): List<FeatOptionUi> = approvedPrivateEntries(
        "feat",
        character?.ruleset ?: creation.ruleset,
    ).map { entry ->
        FeatOptionUi(entry.id, entry.name, entry.privateContentSummary(), t("Approved private content", "Freigegebener privater Inhalt"))
    }

    /** Legacy catalog accessor retained for local-content management and tests. */
    fun creationSpellOptions(): List<SpellUi> = approvedPrivateSpellOptions(null)

    internal fun localizedCreationSpellLabel(label: String): String = when (label) {
        "Known spells" -> t("Known spells", "Bekannte Zauber")
        "Prepared spells" -> t("Prepared spells", "Vorbereitete Zauber")
        "Spellbook" -> t("Spellbook", "Zauberbuch")
        "Granted spells" -> t("Granted spells", "Gewährte Zauber")
        else -> label
    }

    fun creationSpellSelection(): CreationSpellSelectionUi? {
        val limits = CreationSpellRules.limits(
            creation.ruleset,
            creation.className,
            creation.level,
            abilityScoresForDraft(),
        )
        val classEntryId = privateEntryForName("class", creation.className)?.id
        val builtIn = limits?.let { rule ->
            srdSpellCatalog(
                ruleset = creation.ruleset,
                spellClasses = setOf(rule.spellClass),
                maximumLevel = rule.maxSpellLevel,
            )
        }.orEmpty()
        val privateOptions = approvedPrivateEntries("spell")
            .filter { limits != null }
            .filter { entry -> classEntryId != null && classEntryId in entry.mechanics.classIds }
            .mapNotNull(::privateSpell)
            .filter { spell -> limits == null || spell.level <= limits.maxSpellLevel }
        val options = (builtIn + privateOptions).distinctBy { it.id }
            .sortedWith(compareBy<SpellUi> { it.level }.thenBy { it.name.lowercase() }.thenBy { it.id })
        val selected = creation.selectedSpellIds.mapNotNull { selectedId ->
            options.firstOrNull { spellSelectionKey(it) == selectedId || it.id == selectedId }
        }.distinctBy { it.id }
        val fixed = creationGrantedSpells()
        if (limits == null && options.isEmpty() && fixed.isEmpty()) return null
        return CreationSpellSelectionUi(
            options = options,
            selected = selected,
            fixed = fixed,
            cantripLimit = limits?.cantripLimit ?: 0,
            leveledSpellLimit = limits?.leveledSpellLimit ?: 0,
            preparedLimit = limits?.preparedLimit,
            leveledLabel = localizedCreationSpellLabel(limits?.leveledLabel ?: "Granted spells"),
        )
    }

    fun toggleCreationSpell(spellId: String): Boolean {
        val selection = creationSpellSelection() ?: return false
        val spell = selection.options.firstOrNull { it.id == spellId || spellSelectionKey(it) == spellId } ?: return false
        val key = spellSelectionKey(spell)
        if (key in creation.selectedSpellIds) {
            creation.selectedSpellIds.remove(key)
            return true
        }
        val selectedAtLevel = selection.selected.count { (it.level == 0) == (spell.level == 0) }
        val limit = if (spell.level == 0) selection.cantripLimit else selection.leveledSpellLimit
        if (selectedAtLevel >= limit) return false
        creation.selectedSpellIds += key
        return true
    }

    fun removeCreationSpell(spellId: String) {
        val selection = creationSpellSelection() ?: return
        val spell = selection.selected.firstOrNull { it.id == spellId || spellSelectionKey(it) == spellId } ?: return
        creation.selectedSpellIds.remove(spellSelectionKey(spell))
    }

    fun creationSpellSelectionValid(): Boolean {
        val selection = creationSpellSelection() ?: return true
        if (selection.options.isEmpty()) return true
        return selection.selected.count { it.level == 0 } == selection.cantripLimit &&
            selection.selected.count { it.level > 0 } == selection.leveledSpellLimit
    }

    private fun trimCreationSpellSelection() {
        val selection = creationSpellSelection() ?: run {
            creation.selectedSpellIds.clear()
            return
        }
        val allowedCantrips = selection.selected.filter { it.level == 0 }.take(selection.cantripLimit)
        val allowedLeveled = selection.selected.filter { it.level > 0 }.take(selection.leveledSpellLimit)
        creation.selectedSpellIds.clear()
        creation.selectedSpellIds += (allowedCantrips + allowedLeveled).map(::spellSelectionKey)
    }

    private fun spellSelectionKey(spell: SpellUi): String = spell.id.removePrefix("private-")

    private fun resolvedCreationSpells(): List<SpellUi> {
        val selection = creationSpellSelection() ?: return (
            creationGrantedSpells() + creation.selectedSpellIds.mapNotNull(::privateSpellById)
        ).distinctBy { it.id }
        var preparedLeveled = 0
        val selected = selection.selected.map { spell ->
            val prepared = when {
                spell.level == 0 -> true
                selection.preparedLimit == null -> true
                preparedLeveled < selection.preparedLimit -> {
                    preparedLeveled += 1
                    true
                }
                else -> false
            }
            spell.copy(prepared = prepared, sourceKind = SpellSourceKind.CLASS, sourceName = creation.className)
        }
        return (selection.fixed + selected).distinctBy { it.id }
    }

    fun creationGearPackages(): List<StartingGearPackageUi> = startingGearPackages(creation.ruleset, creation.className)

    internal fun creationAbilityScore(ability: String): Int = abilityScoresForDraft()[ability] ?: 10

    internal fun creationTrainedProficiency(): Int = proficiencyModifier(
        creation.ruleset,
        creation.level,
        ProficiencyRank.TRAINED,
    )

    fun creationGearSelectionValid(): Boolean = creation.selectedStartingGearPackageId != null ||
        creation.startingEquipment.isNotEmpty() || creation.startingWeapons.isNotEmpty() ||
        creation.startingArmorChoice != StartingArmorChoice.Recommended

    fun selectCreationGearPackage(packageId: String) {
        val selected = creationGearPackages().firstOrNull { it.id == packageId } ?: return
        clearCreationStartingGear()
        creation.selectedStartingGearPackageId = selected.id
        creation.startingGoldPieces = selected.goldPieces
        if (selected.goldOnly) return
        selected.armorId?.let { armorId -> creation.startingArmorChoice = StartingArmorChoice.Known("equipment:$armorId") }
        selected.equipmentIds.forEachIndexed { index, equipmentId ->
            standardEquipmentCatalog.firstOrNull { it.id == equipmentId }?.let { template ->
                creation.startingEquipment += template.copy(id = "starting-$equipmentId-$index", definitionId = equipmentId)
            }
        }
        val abilities = abilityScoresForDraft()
        val ranks = creationProficiencyRanks()
        selected.weaponIds.withQuantities().forEachIndexed { index, (weaponId, quantity) ->
            standardWeaponCatalog.firstOrNull { it.id == weaponId }?.let { template ->
                creation.startingWeapons += creationWeapon(template.forRuleset(creation.ruleset), abilities, ranks, index)
                    .copy(quantity = quantity)
            }
        }
    }

    fun addCreationGear(item: KnownItemUi): Boolean {
        if (!item.complete) return false
        val equipment = item.equipment
        if (equipment != null) {
            if (equipment.kind == EquipmentKind.ARMOR) {
                creation.selectedStartingGearPackageId = null
                creation.startingArmorChoice = StartingArmorChoice.Known(item.id)
                return true
            }
            creation.selectedStartingGearPackageId = null
            creation.startingEquipment += equipment.copy(
                id = uniqueId(equipment.id.ifBlank { slug(equipment.name) }, creation.startingEquipment.map { it.id }),
                worn = false,
                attuned = false,
            )
            return true
        }
        val weapon = item.weapon ?: return false
        return addCreationWeapon(weapon, weapon.itemBonus)
    }

    fun addCreationWeapon(template: StandardWeaponTemplate, magicBonus: Int): Boolean {
        creation.selectedStartingGearPackageId = null
        creation.startingWeapons += creationWeapon(
            template.forRuleset(creation.ruleset).copy(itemBonus = magicBonus),
            abilityScoresForDraft(),
            creationProficiencyRanks(),
            creation.startingWeapons.size,
        )
        return true
    }

    fun removeCreationEquipment(itemId: String) {
        creation.selectedStartingGearPackageId = null
        creation.startingEquipment.removeAll { it.id == itemId }
    }

    fun addCustomCreationEquipment(item: EquipmentUi) {
        creation.selectedStartingGearPackageId = null
        creation.startingEquipment += item.copy(
            id = uniqueId(item.id.ifBlank { slug(item.name) }, creation.startingEquipment.map { it.id }),
        )
    }

    fun addCustomCreationWeapon(item: WeaponUi) {
        creation.selectedStartingGearPackageId = null
        creation.startingWeapons += item.copy(
            id = uniqueId(item.id.ifBlank { slug(item.name) }, creation.startingWeapons.map { it.id }),
            custom = true,
        )
    }

    fun removeCreationWeapon(itemId: String) {
        creation.selectedStartingGearPackageId = null
        creation.startingWeapons.removeAll { it.id == itemId }
    }

    fun updateCreationWeaponQuantity(itemId: String, quantity: Int) {
        val index = creation.startingWeapons.indexOfFirst { it.id == itemId }
        if (index < 0) return
        creation.selectedStartingGearPackageId = null
        creation.startingWeapons[index] = creation.startingWeapons[index].copy(quantity = quantity.coerceIn(1, 999))
    }

    private fun clearCreationStartingGear() {
        creation.selectedStartingGearPackageId = null
        creation.startingEquipment.clear()
        creation.startingWeapons.clear()
        creation.startingGoldPieces = 0
        creation.startingArmorChoice = StartingArmorChoice.Recommended
    }

    fun creationLanguageOptions(): List<String> {
        val builtIn = standardLanguageCatalog
            .filter { creation.ruleset in it.rulesets }
            .map { it.label(language) }
        val local = approvedPrivateEntries("language").map { it.name }
        return (builtIn + local)
            .distinctBy { it.trim().lowercase() }
            .sortedForPicker(language, { it })
    }

    fun knownItemCatalog(): List<KnownItemUi> = (builtInKnownItemCatalog() + privateEntries.mapNotNull(::privateKnownItem))
        .distinctBy(KnownItemUi::id)

    fun openItemBrowser(target: ItemBrowserTarget = ItemBrowserTarget.Inventory) {
        itemBrowserTarget = target
        itemBrowserFeedback = null
        itemBrowserEditingEquipment = null
        itemBrowserEditingWeapon = null
        equipmentAddOpen = true
    }

    fun editEquipment(item: EquipmentUi) {
        itemBrowserTarget = ItemBrowserTarget.Inventory
        itemBrowserEditingEquipment = item
        itemBrowserEditingWeapon = null
        itemBrowserFeedback = null
        equipmentAddOpen = true
    }

    fun editWeapon(item: WeaponUi) {
        itemBrowserTarget = ItemBrowserTarget.Inventory
        itemBrowserEditingEquipment = null
        itemBrowserEditingWeapon = item
        itemBrowserFeedback = null
        equipmentAddOpen = true
    }

    fun closeItemBrowser() {
        equipmentAddOpen = false
        itemBrowserFeedback = null
        itemBrowserEditingEquipment = null
        itemBrowserEditingWeapon = null
    }

    fun openCurrencyAdjustment(denomination: CoinDenomination) {
        if (selectedCharacter == null) return
        closeItemBrowser()
        searchOpen = false
        currencyAdjustmentDenomination = denomination
    }

    fun closeCurrencyAdjustment() {
        currencyAdjustmentDenomination = null
    }

    fun adjustCurrency(denomination: CoinDenomination, delta: Int): Boolean {
        if (delta == 0) return false
        val current = selectedCharacter ?: return false
        val resultingBalance = current.currency.balance(denomination).toLong() + delta.toLong()
        if (resultingBalance !in 0..Int.MAX_VALUE.toLong()) return false
        updateSelectedCharacter { character ->
            character.copy(currency = character.currency.withBalance(denomination, resultingBalance.toInt()))
        }
        return true
    }

    fun selectCreationArmor(item: KnownItemUi) {
        if (item.type != KnownItemType.Armor) return
        creation.selectedStartingGearPackageId = null
        creation.startingArmorChoice = StartingArmorChoice.Known(item.id)
        closeItemBrowser()
    }

    fun selectCreationUnarmored() {
        creation.selectedStartingGearPackageId = null
        creation.startingArmorChoice = StartingArmorChoice.Unarmored
        closeItemBrowser()
    }

    fun setCustomCreationArmor(item: EquipmentUi) {
        if (item.kind != EquipmentKind.ARMOR) return
        creation.selectedStartingGearPackageId = null
        creation.startingArmorChoice = StartingArmorChoice.Custom(item.copy(id = "starting-armor", worn = true))
        closeItemBrowser()
    }

    fun creationArmorAdvisory(): String? {
        val choice = creation.startingArmorChoice as? StartingArmorChoice.Known ?: return null
        val item = knownItemCatalog().firstOrNull { it.id == choice.itemId }
            ?: return t("This armor is no longer available; the class recommendation will be used.", "Diese Rüstung ist nicht mehr verfügbar; die Klassenempfehlung wird verwendet.")
        itemCompatibilityHint(item, creation.ruleset)?.let { return it }
        val proficiencyId = creationArmorProficiencyId()
        if (proficiencyId !in creationProficiencyRanks()) {
            return t(
                "${creation.className} is not trained with this armor. It remains allowed, but its proficiency bonus is not added.",
                "${creation.className} ist mit dieser Rüstung nicht geübt. Sie bleibt erlaubt, aber der Übungsbonus wird nicht addiert.",
            )
        }
        return null
    }

    fun itemCompatibilityHint(item: KnownItemUi, ruleset: Ruleset): String? {
        if (item.compatibleWith(ruleset)) return null
        if (item.supportedRulesets.isEmpty()) {
            return t("The ruleset marker is not recognized. You can still use this item.", "Die Regelwerksangabe wurde nicht erkannt. Du kannst diesen Gegenstand trotzdem verwenden.")
        }
        val expected = item.supportedRulesets.joinToString { it.shortLabel }
        return t(
            "Designed for $expected, not ${ruleset.shortLabel}. You can still use it.",
            "Für $expected statt ${ruleset.shortLabel} gedacht. Du kannst den Gegenstand trotzdem verwenden.",
        )
    }

    fun addKnownItem(item: KnownItemUi): Boolean {
        if (!item.complete) return false
        val addedName = when (item.type) {
            KnownItemType.Currency -> {
                val denomination = item.currencyDenomination ?: return false
                openCurrencyAdjustment(denomination)
                return true
            }
            KnownItemType.Weapon -> {
                val weapon = item.weapon ?: return false
                addStandardWeapon(weapon)
                weapon.name
            }
            else -> {
                val equipment = resolveKnownEquipmentForCharacter(item) ?: return false
                addEquipment(equipment.copy(worn = false, attuned = false))
                equipment.name
            }
        }
        itemBrowserFeedback = t("Added $addedName", "$addedName hinzugefügt")
        return true
    }

    fun editableSpellCatalog(character: CharacterUi? = selectedCharacter): List<SpellUi> {
        val active = character ?: return emptyList()
        if (active.ruleset == Ruleset.Pf2eRemaster) return approvedPrivateSpellOptions(active)
        val builtIn = srdSpellCatalog(active.ruleset, SrdSpellClass.entries.toSet(), maximumLevel = 9)
        return (builtIn + approvedPrivateSpellOptions(active)).distinctBy { it.id }
    }

    private fun srdSpellCatalog(
        ruleset: Ruleset,
        spellClasses: Set<SrdSpellClass>,
        maximumLevel: Int,
    ): List<SpellUi> {
        if (ruleset == Ruleset.Pf2eRemaster) return emptyList()
        val revision = if (ruleset == Ruleset.Fifth2014) SrdSpellRevision.SRD_5_1 else SrdSpellRevision.SRD_5_2_1
        val source = if (revision == SrdSpellRevision.SRD_5_1) "SRD 5.1" else "SRD 5.2.1"
        return SrdSpellCatalog.entries.filter { entry ->
            entry.revision == revision && entry.level <= maximumLevel && entry.classes.any { it in spellClasses }
        }.map { entry ->
            val text = if (language == UiLanguage.German) entry.de else entry.en
            val combat = SrdSpellCombatCatalog.find(revision, entry.id)
            val details = SrdSpellDetailsCatalog.find(revision, entry.id)
            SpellUi(
                id = entry.id,
                name = text.name,
                level = entry.level,
                summary = text.summary,
                prepared = true,
                sourceKind = SpellSourceKind.CLASS,
                sourceName = source,
                activationCost = CombatPotentialEngine.standardSpellActivationCost(entry.id),
                castPreviews = entry.castPreviews.associate { preview ->
                    preview.slotLevel to if (language == UiLanguage.German) preview.de else preview.en
                },
                savingThrows = combat.savingThrowAbilities.map { ability ->
                    SavingThrowPrompt(Ability.valueOf(ability), DifficultyClass(useSpellcasting = true))
                },
                spellAttack = combat.spellAttack,
                rulesText = SpellRulesText(
                    school = entry.school,
                    concentration = entry.concentration,
                    ritual = entry.ritual,
                    castingTime = details?.castingTime.orEmpty(),
                    range = details?.range.orEmpty(),
                    components = details?.components.orEmpty(),
                    duration = details?.duration.orEmpty(),
                    effect = details?.effect.orEmpty(),
                    durationRounds = details?.durationRounds,
                    source = details?.source ?: source,
                ),
            )
        }
    }

    fun approvedPrivateSpellOptions(character: CharacterUi?): List<SpellUi> = approvedPrivateEntries(
        "spell",
        character?.ruleset ?: creation.ruleset,
    )
        .filter { entry ->
            character == null || entry.mechanics.classIds.isEmpty() || entry.mechanics.classIds.any { classId ->
                val className = privateEntryById(classId)?.name ?: return@any false
                character.className.equals(className, true) || character.progression.any { it.className.equals(className, true) }
            }
        }
        .mapNotNull(::privateSpell)

    fun creationPreview(): CreationPreviewUi {
        val abilities = abilityScoresForDraft()
        val constitution = abilityModifier(abilities.getValue("CON"))
        val dexterity = abilityModifier(abilities.getValue("DEX"))
        val proficiency = if (creation.ruleset == Ruleset.Pf2eRemaster) creation.level + 2 else proficiencyForLevel(creation.level)
        val ranks = creationProficiencyRanks()
        val subclassStats = selectedCreationSubclass()?.resolveStats(creation.level, proficiency) ?: ResolvedSubclassStatsUi()
        val classUnarmored = if (creation.ruleset == Ruleset.Pf2eRemaster) {
            10 + dexterity + proficiencyModifier(creation.ruleset, creation.level, ranks["armor:unarmored"])
        } else when (creation.className) {
            "Barbarian" -> 10 + dexterity + constitution
            "Monk" -> 10 + dexterity + abilityModifier(abilities.getValue("WIS"))
            else -> 10 + dexterity
        }
        val unarmored = (when (subclassStats.armorFormula) {
            SubclassArmorFormulaUi.NONE -> classUnarmored
            SubclassArmorFormulaUi.DEXTERITY_13 -> 13 + dexterity
            SubclassArmorFormulaUi.DEXTERITY_AND_CHARISMA_10 ->
                10 + dexterity + abilityModifier(abilities.getValue("CHA"))
        }) + subclassStats.armorClassBonus
        val armorProficiency = if (creation.ruleset == Ruleset.Pf2eRemaster) {
            proficiencyModifier(creation.ruleset, creation.level, ranks[creationArmorProficiencyId()])
        } else proficiency
        val armor = resolvedCreationArmor(dexterity, armorProficiency)
        val primary = primaryAbilityFor(creation.className)
        return CreationPreviewUi(
            abilities = abilities,
            primaryAbility = primary,
            primaryScore = abilities.getValue(primary),
            hitPoints = (creationHitPointGains(hitDieFor(creation.className), constitution).sum() + subclassStats.hitPointBonus)
                .coerceAtLeast(creation.level),
            armorClass = (armor?.armorClass?.plus(subclassStats.armorClassBonus) ?: unarmored) + (armor?.shieldBonus ?: 0),
            startingArmor = armor?.name ?: t("Unarmored", "Unge­rüstet"),
        )
    }

    fun creationAbilityBreakdowns(): List<CreationAbilityBreakdownUi> {
        val baseScores = baseAbilityScoresForDraft()
        val ancestryBonuses = creationAncestryBonuses()
        return creationAbilityOrder.map { ability ->
            val baseScore = baseScores.getValue(ability)
            val ancestryBonus = ancestryBonuses.amounts[ability] ?: 0
            val finalScore = baseScore + ancestryBonus
            CreationAbilityBreakdownUi(
                ability = ability,
                baseScore = baseScore,
                ancestrySource = ancestryBonus.takeIf { it != 0 }?.let {
                    CreationAbilitySourceUi(
                        label = creation.ancestry,
                        amount = it,
                        detail = ancestryBonuses.detail,
                    )
                },
                finalScore = finalScore,
                modifier = abilityModifier(finalScore),
            )
        }
    }

    fun creationAbilityInfo(ability: String): CreationInfoUi {
        val breakdown = creationAbilityBreakdown(ability)
        val name = creationAbilityName(ability)
        val current = breakdown.ancestrySource?.let { source ->
            t(
                "Base ${breakdown.baseScore} + ${signed(source.amount)} from ${source.label} = final ${breakdown.finalScore}. Modifier ${signed(breakdown.modifier)}.",
                "Basis ${breakdown.baseScore} + ${signed(source.amount)} durch ${source.label} = endgültig ${breakdown.finalScore}. Modifikator ${signed(breakdown.modifier)}.",
            )
        } ?: t(
            "Current score ${breakdown.finalScore}. Modifier ${signed(breakdown.modifier)}.",
            "Aktueller Wert ${breakdown.finalScore}. Modifikator ${signed(breakdown.modifier)}.",
        )
        val recommendation = creationAbilityRecommendation(ability)
        return CreationInfoUi(
            title = "$name ($ability)",
            body = listOfNotNull(creationAbilityDescription(ability), current, recommendation).joinToString("\n\n"),
        )
    }

    fun creationModifierInfo(ability: String): CreationInfoUi {
        val breakdown = creationAbilityBreakdown(ability)
        val name = creationAbilityName(ability)
        val ancestryCalculation = breakdown.ancestrySource?.let { source ->
            t(
                "Its final score includes ${signed(source.amount)} from ${source.label}: ${breakdown.baseScore} + ${source.amount} = ${breakdown.finalScore}.",
                "Der endgültige Wert enthält ${signed(source.amount)} durch ${source.label}: ${breakdown.baseScore} + ${source.amount} = ${breakdown.finalScore}.",
            )
        }
        val formula = t(
            "This modifier comes from the final $name score: (${breakdown.finalScore} - 10) ÷ 2, rounded down = ${signed(breakdown.modifier)}.",
            "Dieser Modifikator stammt vom endgültigen $name-Wert: (${breakdown.finalScore} - 10) ÷ 2, abgerundet = ${signed(breakdown.modifier)}.",
        )
        return CreationInfoUi(
            title = t("$name modifier ${signed(breakdown.modifier)}", "$name-Modifikator ${signed(breakdown.modifier)}"),
            body = listOfNotNull(formula, ancestryCalculation).joinToString("\n\n"),
        )
    }

    fun creationAncestryInfo(ability: String): CreationInfoUi? {
        val breakdown = creationAbilityBreakdown(ability)
        val source = breakdown.ancestrySource ?: return null
        val name = creationAbilityName(ability)
        val detail = source.detail.takeIf { it.isNotBlank() }?.let { "\n\n${t("Source", "Quelle")}: $it" }.orEmpty()
        return CreationInfoUi(
            title = t("${source.label} ancestry ${signed(source.amount)}", "${source.label}-Abstammung ${signed(source.amount)}"),
            body = t(
                "This ${signed(source.amount)} comes from ${source.label} ancestry for $name under ${creation.ruleset.shortLabel}. Base ${breakdown.baseScore} + ${source.amount} = final ${breakdown.finalScore}.",
                "Diese ${signed(source.amount)} stammen bei $name aus der Abstammung ${source.label} nach ${creation.ruleset.shortLabel}. Basis ${breakdown.baseScore} + ${source.amount} = endgültig ${breakdown.finalScore}.",
            ) + detail,
        )
    }

    fun creationAncestryBonusAdvisory(): String? {
        val privateEntry = privateEntryForName("ancestry", creation.ancestry)
        val bonuses = creationAncestryBonuses()
        if (bonuses.invalidPrivateFormula) {
            return t(
                "${creation.ancestry}'s local ability bonus formula is invalid, so no ancestry bonus is applied.",
                "Die lokale Attributsbonus-Formel für ${creation.ancestry} ist ungültig; deshalb wird kein Abstammungsbonus angewendet.",
            )
        }
        if (creation.ruleset == Ruleset.Fifth2014 && bonuses.amounts.isEmpty()) {
            return if (privateEntry != null) {
                t(
                    "No reviewed ability bonus is defined for ${creation.ancestry}; no ancestry adjustment is applied.",
                    "Für ${creation.ancestry} ist kein geprüfter Attributsbonus definiert; es wird kein Abstammungsbonus angewendet.",
                )
            } else {
                t(
                    "No audited SRD 5.1 ability bonus is available for ${creation.ancestry}; no ancestry adjustment is applied.",
                    "Für ${creation.ancestry} ist kein geprüfter Attributsbonus aus dem SRD 5.1 verfügbar; es wird kein Abstammungsbonus angewendet.",
                )
            }
        }
        return null
    }

    fun creationAbilityLimitAdvisory(): String? = creationAbilityBreakdowns()
        .filter { it.finalScore > 20 }
        .takeIf { it.isNotEmpty() }
        ?.joinToString { "${it.ability} ${it.finalScore}" }
        ?.let { scores ->
            t(
                "Final scores above the usual creation limit of 20: $scores. The values remain allowed for table rulings and custom rules.",
                "Endgültige Werte über dem üblichen Erschaffungslimit von 20: $scores. Die Werte bleiben für Spieltischentscheidungen und eigene Regeln erlaubt.",
            )
        }

    fun beginLevelUp() {
        val character = selectedCharacter ?: return
        if (character.level >= 20 || turnOpen) return
        levelUpDraft = LevelUpDraft(character, levelUpHitDieFor(character, character.className))
        levelUpOpen = true
    }

    fun cancelLevelUp() {
        levelUpOpen = false
        levelUpDraft = null
    }

    fun rollLevelUpHitDie() {
        val draft = levelUpDraft ?: return
        draft.rolledHitDie = dice.roll(RollRequest("Level ${draft.toLevel} HP", DiceExpression(1, draft.hitDieSides))).total
    }

    fun levelUpFeatOptions(): List<FeatOptionUi> {
        val character = selectedCharacter ?: return emptyList()
        val dexterity = character.abilities["DEX"] ?: 10
        val constitution = character.abilities["CON"] ?: 10
        val intelligence = character.abilities["INT"] ?: 10
        val spellcaster = character.canCastSpells
        return (listOf(
            FeatOptionUi("ability-score-improvement", "Ability Score Improvement", "Increase your key ability scores.", "Reliable for every build"),
            FeatOptionUi("tough", "Tough", "Gain additional Hit Points as you level.", if (constitution < 16) "Useful with your current Constitution" else null),
            FeatOptionUi("alert", "Alert", "Improve initiative and avoid being caught off guard.", if (dexterity < 16) "Helps you act earlier" else null),
            FeatOptionUi("skilled", "Skilled", "Gain additional skill proficiencies.", "Flexible outside combat"),
            FeatOptionUi("magic-initiate", "Magic Initiate", "Learn a small amount of spellcasting.", if (!spellcaster || intelligence >= 14) "Adds magical options" else null),
        ) + approvedPrivateFeatOptions(character)).distinctBy(FeatOptionUi::id)
            .filter { it.id == "ability-score-improvement" || it.id !in character.featIds }
            .sortedWith(compareByDescending<FeatOptionUi> { it.recommendedReason != null }.thenBy { it.name })
    }

    fun selectLevelUpClass(className: String) {
        val draft = levelUpDraft ?: return
        if (className !in levelUpClassOptions()) return
        if (draft.className == className) return
        draft.className = className
        draft.selectedFeatId = null
        draft.abilityIncreases.clear()
        draft.guidedSelections.clear()
        draft.rolledHitDie = null
        selectedCharacter?.let { draft.hitDieSides = levelUpHitDieFor(it, className) }
    }

    fun selectLevelUpHitDie(sides: Int) {
        if (sides !in supportedClassHitDice) return
        val draft = levelUpDraft ?: return
        if (draft.hitDieSides == sides) return
        draft.hitDieSides = sides
        draft.rolledHitDie = null
    }

    private fun levelUpHitDieFor(character: CharacterUi, className: String): Int =
        character.hitDieOverrides[className] ?: hitDieFor(className, character.ruleset)

    fun levelUpClassOptions(character: CharacterUi? = selectedCharacter): List<String> {
        val activeCharacter = character ?: return emptyList()
        if (activeCharacter.ruleset == Ruleset.Pf2eRemaster) return listOf(activeCharacter.className)
        val privateClasses = approvedPrivateEntries("class", activeCharacter.ruleset).map { it.name }.toSet()
        val classes = (listOf("Barbarian", "Bard", "Cleric", "Druid", "Fighter", "Monk", "Paladin", "Ranger", "Rogue", "Sorcerer", "Warlock", "Wizard") + privateClasses).distinct()
        val canLeaveCurrent = meetsMulticlassPrerequisite(activeCharacter, activeCharacter.className)
        return classes.filter { candidate ->
            candidate == activeCharacter.className || (canLeaveCurrent && (candidate in privateClasses || meetsMulticlassPrerequisite(activeCharacter, candidate)))
        }
    }

    private fun meetsMulticlassPrerequisite(character: CharacterUi, className: String): Boolean {
        fun score(ability: String) = character.abilities[ability] ?: 10
        return when (className) {
            "Barbarian" -> score("STR") >= 13
            "Bard", "Sorcerer", "Warlock" -> score("CHA") >= 13
            "Cleric", "Druid" -> score("WIS") >= 13
            "Fighter" -> score("STR") >= 13 || score("DEX") >= 13
            "Monk" -> score("DEX") >= 13 && score("WIS") >= 13
            "Paladin" -> score("STR") >= 13 && score("CHA") >= 13
            "Ranger" -> score("DEX") >= 13 && score("WIS") >= 13
            "Rogue" -> score("DEX") >= 13
            "Wizard" -> score("INT") >= 13
            else -> false
        }
    }

    fun levelUpFeatAvailable(draft: LevelUpDraft? = levelUpDraft): Boolean {
        val activeDraft = draft ?: return false
        val character = selectedCharacter ?: return false
        if (character.ruleset == Ruleset.Pf2eRemaster) return true
        val newClassLevel = character.progression.count { it.className == activeDraft.className } + 1
        return if (activeDraft.className == "Fighter") newClassLevel in setOf(4, 6, 8, 12, 14, 16, 19)
        else newClassLevel in setOf(4, 8, 12, 16, 19)
    }

    fun selectLevelUpFeat(featId: String) {
        val draft = levelUpDraft ?: return
        if (featId !in levelUpFeatOptions().map(FeatOptionUi::id)) return
        draft.selectedFeatId = featId
        if (featId != "ability-score-improvement") draft.abilityIncreases.clear()
    }

    fun setLevelUpAbilityIncrease(ability: String, increase: Int) {
        val draft = levelUpDraft ?: return
        val character = selectedCharacter ?: return
        if (draft.selectedFeatId != "ability-score-improvement" || ability !in character.abilities) return
        val safe = increase.coerceIn(0, 2)
        if ((character.abilities[ability] ?: 10) + safe > 20) return
        val otherTotal = draft.abilityIncreases.filterKeys { it != ability }.values.sum()
        if (otherTotal + safe > 2) return
        if (safe == 0) draft.abilityIncreases.remove(ability) else draft.abilityIncreases[ability] = safe
    }

    fun cycleLevelUpAbilityIncrease(ability: String) {
        val current = levelUpDraft?.abilityIncreases?.get(ability) ?: 0
        val next = if (current >= 2) 0 else current + 1
        setLevelUpAbilityIncrease(ability, next)
        if (levelUpDraft?.abilityIncreases?.get(ability) == current && current > 0) {
            setLevelUpAbilityIncrease(ability, 0)
        }
    }

    fun levelUpFeatSelectionValid(draft: LevelUpDraft? = levelUpDraft): Boolean {
        val activeDraft = draft ?: return false
        if (!levelUpFeatAvailable(activeDraft)) return true
        val selected = activeDraft.selectedFeatId ?: return false
        if (selected !in levelUpFeatOptions().map(FeatOptionUi::id)) return false
        if (selected != "ability-score-improvement") return true
        val character = selectedCharacter ?: return false
        return activeDraft.abilityIncreases.values.sum() == 2 &&
            activeDraft.abilityIncreases.all { (ability, increase) ->
                increase in 1..2 && (character.abilities[ability] ?: 10) + increase <= 20
            }
    }

    internal fun levelUpGuidedChoices(draft: LevelUpDraft? = levelUpDraft): List<GuidedLevelChoiceUi> {
        val activeDraft = draft ?: return emptyList()
        val character = selectedCharacter ?: return emptyList()
        if (character.ruleset == Ruleset.Pf2eRemaster) {
            val increaseDue = activeDraft.toLevel >= 2 &&
                (activeDraft.className == "Rogue" || activeDraft.toLevel >= 3 && activeDraft.toLevel % 2 == 1)
            if (!increaseDue) return emptyList()
            val options = character.skills.keys.mapNotNull { name ->
                val skill = ProficiencyCatalog.skillByDisplayName(character.ruleset, name) ?: return@mapNotNull null
                if (skill.id == "skill:perception") return@mapNotNull null
                val current = proficiencyRank(skill.id, character.proficiencyRanks, character.proficiencyIds)
                val next = when (current) {
                    null -> ProficiencyRank.TRAINED
                    ProficiencyRank.TRAINED -> ProficiencyRank.EXPERT
                    ProficiencyRank.EXPERT -> ProficiencyRank.MASTER.takeIf { activeDraft.toLevel >= 7 }
                    ProficiencyRank.MASTER -> ProficiencyRank.LEGENDARY.takeIf { activeDraft.toLevel >= 15 }
                    ProficiencyRank.LEGENDARY -> null
                } ?: return@mapNotNull null
                GuidedLevelOptionUi(
                    id = "${skill.id}:${next.name.lowercase()}",
                    name = skill.name(language),
                    summary = "${current?.displayName(language) ?: t("Untrained", "Ungeübt")} → ${next.displayName(language)}",
                    proficiencyId = skill.id,
                    proficiencyRank = next,
                )
            }
            return if (options.isEmpty()) emptyList() else listOf(
                GuidedLevelChoiceUi(
                    id = "pf2e-skill-increase-${activeDraft.toLevel}",
                    title = t("Choose a skill increase", "Wähle eine Fertigkeitssteigerung"),
                    kind = GuidedLevelChoiceKind.PROFICIENCY,
                    chooseCount = 1,
                    options = options,
                ),
            )
        }
        val definition = guidedClassDefinitions[activeDraft.className]
        val classLevel = character.progression.count { it.className == activeDraft.className } + 1
        val choices = mutableListOf<GuidedLevelChoiceUi>()
        val subclassOptions = subclassOptions(character.ruleset, activeDraft.className)
        val existingSubclassId = character.subclassIdsByClass[activeDraft.className]
        val existingSubclass = character.subclassNamesByClass[activeDraft.className]
            ?: subclassOptions.firstOrNull { it.id == existingSubclassId }?.name
            ?: character.subclass.takeIf { activeDraft.className == character.className && it.isConcreteSubclass() }
        val subclassLevel = defaultSubclassSelectionLevel(character.ruleset, activeDraft.className)
        if (classLevel >= subclassLevel && existingSubclass == null) {
            choices += GuidedLevelChoiceUi(
                id = "${activeDraft.className.lowercase()}-subclass",
                title = "Choose ${activeDraft.className} subclass",
                kind = GuidedLevelChoiceKind.SUBCLASS,
                chooseCount = 1,
                options = subclassOptions.map { option ->
                    GuidedLevelOptionUi(
                        id = option.id,
                        name = option.name,
                        summary = option.summary(language),
                        subclassId = option.id,
                        subclassName = option.name,
                    )
                },
            )
        }
        val selectedSubclass = choices.firstOrNull { it.kind == GuidedLevelChoiceKind.SUBCLASS }
            ?.let { choice ->
                val selectedId = activeDraft.guidedSelections[choice.id]?.singleOrNull()
                choice.options.firstOrNull { it.id == selectedId }?.subclassName
            }
        val effectiveSubclass = selectedSubclass ?: existingSubclass
        val hasManeuvers = character.features.any { it.id.startsWith("maneuver-") }
        if (activeDraft.className == "Fighter" && classLevel >= 3 && effectiveSubclass == "Battle Master" && !hasManeuvers) {
            choices += GuidedLevelChoiceUi(
                id = "fighter-maneuvers",
                title = "Choose three maneuvers",
                kind = GuidedLevelChoiceKind.CLASS_OPTION,
                chooseCount = 3,
                options = battleMasterManeuverOptions,
            )
        }
        val hasMetamagicOptions = character.features.any { it.id.startsWith("metamagic-") }
        if (activeDraft.className == "Sorcerer" && classLevel >= 3 && !hasMetamagicOptions) {
            choices += GuidedLevelChoiceUi(
                id = "sorcerer-metamagic",
                title = "Choose two Metamagic options",
                kind = GuidedLevelChoiceKind.CLASS_OPTION,
                chooseCount = 2,
                options = metamagicOptions,
            )
        }
        if (definition != null && definition.spellsLearnedEachLevel > 0) {
            val maximumSpellLevel = ((classLevel + 1) / 2).coerceIn(1, 9)
            val knownIds = character.spells.map(SpellUi::id).toSet()
            val options = (definition.spellOptions + approvedPrivateSpellOptions(character))
                .distinctBy(SpellUi::id)
                .filter { it.level <= maximumSpellLevel && it.id !in knownIds }
                .map { spell -> GuidedLevelOptionUi(spell.id, spell.name, spell.summary, spell = spell) }
            if (options.isNotEmpty()) {
                choices += GuidedLevelChoiceUi(
                    id = "${activeDraft.className.lowercase()}-spells-$classLevel",
                    title = if (definition.spellsLearnedEachLevel == 1) "Choose a spell" else "Choose ${definition.spellsLearnedEachLevel} spells",
                    kind = GuidedLevelChoiceKind.SPELL,
                    chooseCount = definition.spellsLearnedEachLevel.coerceAtMost(options.size),
                    options = options,
                )
            }
        }
        if (activeDraft.selectedFeatId == "skilled") {
            val options = ProficiencyCatalog.fiveESkills
                .filterNot { it.id in character.proficiencyRanks || it.id in character.proficiencyIds }
                .map { skill ->
                    GuidedLevelOptionUi(
                        id = "${skill.id}:trained",
                        name = skill.name(language),
                        summary = t("Become proficient", "Fertigkeit erlernen"),
                        proficiencyId = skill.id,
                        proficiencyRank = ProficiencyRank.TRAINED,
                    )
                }
            if (options.isNotEmpty()) {
                choices += GuidedLevelChoiceUi(
                    id = "skilled-proficiencies",
                    title = t("Choose three skill proficiencies", "Wähle drei Fertigkeiten"),
                    kind = GuidedLevelChoiceKind.PROFICIENCY,
                    chooseCount = 3.coerceAtMost(options.size),
                    options = options,
                )
            }
        }
        return choices
    }

    fun toggleLevelUpGuidedOption(choiceId: String, optionId: String) {
        val draft = levelUpDraft ?: return
        val choice = levelUpGuidedChoices(draft).firstOrNull { it.id == choiceId } ?: return
        if (choice.options.none { it.id == optionId }) return
        val current = draft.guidedSelections[choiceId].orEmpty()
        val updated = when {
            optionId in current -> current - optionId
            choice.chooseCount == 1 -> setOf(optionId)
            current.size < choice.chooseCount -> current + optionId
            else -> current
        }
        if (updated.isEmpty()) draft.guidedSelections.remove(choiceId) else draft.guidedSelections[choiceId] = updated
        val validChoiceIds = levelUpGuidedChoices(draft).map(GuidedLevelChoiceUi::id).toSet()
        draft.guidedSelections.keys.toList().filterNot { it in validChoiceIds }.forEach(draft.guidedSelections::remove)
    }

    fun levelUpGuidedChoicesValid(draft: LevelUpDraft? = levelUpDraft): Boolean {
        val activeDraft = draft ?: return false
        return levelUpGuidedChoices(activeDraft).all { choice ->
            val selections = activeDraft.guidedSelections[choice.id].orEmpty()
            selections.size == choice.chooseCount && selections.all { selected -> choice.options.any { it.id == selected } }
        }
    }

    fun applyLevelUp(): Boolean {
        val draft = levelUpDraft ?: return false
        val character = selectedCharacter?.takeIf { it.id == draft.characterId } ?: return false
        if (draft.toLevel > 20) return false
        if (!levelUpFeatSelectionValid(draft) || !levelUpGuidedChoicesValid(draft)) return false
        val newAbilities = character.baseAbilities.ifEmpty { character.abilities }.toMutableMap().apply {
            if (draft.selectedFeatId == "ability-score-improvement") {
                draft.abilityIncreases.forEach { (ability, increase) ->
                    this[ability] = ((this[ability] ?: 10) + increase).coerceAtMost(20)
                }
            }
        }
        val oldConstitution = abilityModifier(character.abilities["CON"] ?: 10)
        val newConstitution = abilityModifier(newAbilities["CON"] ?: 10)
        val sides = draft.hitDieSides
        val levelGain = when (draft.hpMethod) {
            HpMethod.Fixed -> sides / 2 + 1 + oldConstitution
            HpMethod.Rolled -> (draft.rolledHitDie ?: return false) + oldConstitution
            HpMethod.Manual -> draft.manualHitDie
        }.coerceAtLeast(1)
        val newLevel = draft.toLevel
        val chosenFeat = draft.selectedFeatId?.takeUnless { it == "ability-score-improvement" }
        val constitutionIncrease = (newConstitution - oldConstitution).coerceAtLeast(0) * newLevel
        val toughIncrease = when {
            chosenFeat == "tough" -> newLevel * 2
            "tough" in character.featIds -> 2
            else -> 0
        }
        var gain = levelGain + constitutionIncrease + toughIncrease
        val guidedChoices = levelUpGuidedChoices(draft)
        val selectedOptions = guidedChoices.flatMap { choice ->
            val selectedIds = draft.guidedSelections[choice.id].orEmpty()
            choice.options.filter { it.id in selectedIds }
        }
        val updatedProficiencyRanks = buildMap {
            character.proficiencyIds.forEach { put(it, ProficiencyRank.TRAINED) }
            putAll(character.proficiencyRanks)
            selectedOptions.forEach { option ->
                val proficiencyId = option.proficiencyId
                val rank = option.proficiencyRank
                if (proficiencyId != null && rank != null) put(proficiencyId, rank)
            }
            if (character.ruleset != Ruleset.Pf2eRemaster && draft.className != character.className) {
                ProficiencyCatalog.classDefinition(character.ruleset, draft.className).automaticRanks
                    .filterKeys { it.startsWith("weapon:") || it.startsWith("armor:") }
                    .forEach { (id, rank) -> put(id, rank) }
            }
        }
        val chosenSubclassId = selectedOptions.firstNotNullOfOrNull(GuidedLevelOptionUi::subclassId)
        val chosenSubclass = selectedOptions.firstNotNullOfOrNull(GuidedLevelOptionUi::subclassName)
        val updatedSubclassIds = character.subclassIdsByClass.toMutableMap().apply {
            chosenSubclassId?.let { put(draft.className, it) }
        }
        val updatedSubclassNames = character.subclassNamesByClass.toMutableMap().apply {
            chosenSubclass?.let { put(draft.className, it) }
        }
        val newSubclass = if (draft.className == character.className) chosenSubclass ?: character.subclass else character.subclass
        val activeSubclassId = updatedSubclassIds[draft.className]
        val activeSubclassName = updatedSubclassNames[draft.className]
        val activeSubclass = subclassOptions(character.ruleset, draft.className).firstOrNull {
            it.id == activeSubclassId || it.name.equals(activeSubclassName, true)
        }
        val newProficiency = if (character.ruleset == Ruleset.Pf2eRemaster) newLevel + 2 else proficiencyForLevel(newLevel)
        val oldClassLevel = character.progression.count { it.className == draft.className }
        val newClassLevel = oldClassLevel + 1
        val oldSubclassStats = activeSubclass?.resolveStats(oldClassLevel, character.proficiency) ?: ResolvedSubclassStatsUi()
        val newSubclassStats = activeSubclass?.resolveStats(newClassLevel, newProficiency) ?: ResolvedSubclassStatsUi()
        gain += newSubclassStats.hitPointBonus - oldSubclassStats.hitPointBonus
        val newMax = character.maxHp + gain
        val dexterity = abilityModifier(newAbilities["DEX"] ?: 10)
        val charisma = abilityModifier(newAbilities["CHA"] ?: 10)
        fun subclassArmorFormula(formula: SubclassArmorFormulaUi): Int = when (formula) {
            SubclassArmorFormulaUi.NONE -> 10 + dexterity
            SubclassArmorFormulaUi.DEXTERITY_13 -> 13 + dexterity
            SubclassArmorFormulaUi.DEXTERITY_AND_CHARISMA_10 -> 10 + dexterity + charisma
        }
        val armorBonusIncrease = newSubclassStats.armorClassBonus - oldSubclassStats.armorClassBonus
        val unarmoredIncrease = armorBonusIncrease +
            subclassArmorFormula(newSubclassStats.armorFormula) - subclassArmorFormula(oldSubclassStats.armorFormula)
        val wornBodyArmor = character.equipmentItems.firstOrNull { it.worn && it.armorClass != null }
        val wearingBodyArmor = wornBodyArmor != null
        val pf2eBodyArmorIncrease = if (
            character.ruleset == Ruleset.Pf2eRemaster &&
            wornBodyArmor?.let(::armorProficiencyId) in updatedProficiencyRanks
        ) 1 else 0
        val pf2eUnarmoredIncrease = if (
            character.ruleset == Ruleset.Pf2eRemaster && "armor:unarmored" in updatedProficiencyRanks
        ) 1 else 0
        val selectedFeatures = buildList {
            addAll(selectedOptions.mapNotNull(GuidedLevelOptionUi::feature))
            activeSubclass?.let {
                addAll(it.resolveFeatures(newClassLevel, proficiencyForLevel(newLevel), character.features, newAbilities))
            }
            chosenFeat?.let(::privateEntryById)?.let { feat ->
                add(feat.toPrivateFeature().copy(id = "private-${feat.id}"))
            }
            privateEntryForName("class", draft.className, character.ruleset)?.let { classEntry ->
                add(classEntry.toPrivateFeature().copy(id = "private-class-${classEntry.id}"))
            }
        }
        val selectedSpells = selectedOptions.mapNotNull(GuidedLevelOptionUi::spell) + activeSubclass?.resolveSpells(newClassLevel).orEmpty()
        val initiativeIncrease = newSubclassStats.initiativeBonus - oldSubclassStats.initiativeBonus
        val skillDerivations = buildMap {
            character.skills.forEach { (name, _) ->
                val existing = character.derivation.skills[name]
                val derived = ProficiencyCatalog.skillByDisplayName(character.ruleset, name)?.let { skill ->
                    DerivedModifierFormulaUi(skill.ability, proficiencyId = skill.id)
                }
                (existing ?: derived)?.let { put(name, it) }
            }
        }
        val saveDerivations = character.saves.mapValues { (name, _) ->
            character.derivation.saves[name] ?: DerivedModifierFormulaUi(
                saveAbbreviation(name),
                proficiencyId = saveProficiencyId(saveAbbreviation(name)),
            )
        }
        val newInitiative = (character.derivation.initiative?.resolve(
            newAbilities,
            newProficiency,
            character.ruleset,
            newLevel,
            updatedProficiencyRanks,
            character.proficiencyIds,
        ) ?: character.initiative) + initiativeIncrease
        val savingThrowIncrease = newSubclassStats.savingThrowBonus - oldSubclassStats.savingThrowBonus
        val newSaves = character.saves.mapValues { (name, value) ->
            (saveDerivations[name]?.resolve(newAbilities, newProficiency, character.ruleset, newLevel, updatedProficiencyRanks, character.proficiencyIds) ?: value) + savingThrowIncrease
        }
        val newSkills = character.skills.mapValues { (name, value) ->
            skillDerivations[name]?.resolve(newAbilities, newProficiency, character.ruleset, newLevel, updatedProficiencyRanks, character.proficiencyIds) ?: value
        }
        val canonicalHitDie = hitDieFor(draft.className, character.ruleset)
        val updatedHitDieOverrides = character.hitDieOverrides.toMutableMap().apply {
            if (draft.hitDieSides == canonicalHitDie) remove(draft.className)
            else put(draft.className, draft.hitDieSides)
        }
        val updatedReduction = character.maxHpReduction.coerceAtMost(newMax)
        val updatedEffectiveMaximum = character.copy(
            maxHp = newMax,
            maxHpReduction = updatedReduction,
        ).effectiveMaxHp
        val updated = character.copy(
            level = newLevel,
            hp = if (draft.healByIncrease) {
                (character.hp + gain).coerceAtMost(updatedEffectiveMaximum)
            } else {
                character.hp.coerceAtMost(updatedEffectiveMaximum)
            },
            maxHp = newMax,
            maxHpReduction = updatedReduction,
            armorClass = (character.baseArmorClass ?: character.armorClass) + if (wearingBodyArmor) {
                armorBonusIncrease + pf2eBodyArmorIncrease
            } else unarmoredIncrease + pf2eUnarmoredIncrease,
            unarmoredArmorClass = character.unarmoredArmorClass + unarmoredIncrease + pf2eUnarmoredIncrease,
            subclass = newSubclass,
            subclassIdsByClass = updatedSubclassIds,
            subclassNamesByClass = updatedSubclassNames,
            proficiency = newProficiency,
            initiative = newInitiative,
            initiativeRollMode = if (newSubclassStats.initiativeRollMode != RollMode.NORMAL) newSubclassStats.initiativeRollMode else character.initiativeRollMode,
            criticalHitThreshold = minOf(character.criticalHitThreshold, newSubclassStats.criticalThreshold),
            speedFeet = (character.baseSpeedFeet ?: character.speedFeet) + (newSubclassStats.speedBonusFeet - oldSubclassStats.speedBonusFeet),
            abilities = newAbilities,
            saves = newSaves,
            skills = newSkills,
            weapons = character.weapons.map { weapon ->
                recalculateWeapon(
                    weapon.copy(itemBonus = weapon.itemBonus + (newSubclassStats.attackBonus - oldSubclassStats.attackBonus)),
                    newAbilities,
                    newProficiency,
                    character.ruleset,
                    newLevel,
                    updatedProficiencyRanks,
                    character.proficiencyIds,
                )
            },
            progression = character.progression + LevelProgressionUi(
                newLevel,
                draft.className,
                gain,
                draft.hpMethod,
                chosenFeat,
                draft.hitDieSides,
            ),
            hitDieOverrides = updatedHitDieOverrides,
            featIds = if (chosenFeat == null) character.featIds else (character.featIds + chosenFeat).distinct(),
            proficiencyIds = updatedProficiencyRanks.keys,
            proficiencyRanks = updatedProficiencyRanks,
            spells = (character.spells + selectedSpells).distinctBy(SpellUi::id),
            features = refreshedClassFeatures(character, draft.className, newSubclass, selectedFeatures),
            hasSpellcastingCapability = character.hasSpellcastingCapability || draft.className in setOf("Wizard", "Sorcerer"),
            derivation = character.derivation.copy(
                initiative = character.derivation.initiative?.let { it.copy(base = it.base + initiativeIncrease) },
                saves = saveDerivations.mapValues { (_, formula) ->
                    formula.copy(base = formula.base + savingThrowIncrease)
                },
                skills = skillDerivations,
            ),
            baseAbilities = newAbilities,
            baseSaves = newSaves,
            baseArmorClass = (character.baseArmorClass ?: character.armorClass) + if (wearingBodyArmor) {
                armorBonusIncrease + pf2eBodyArmorIncrease
            } else unarmoredIncrease + pf2eUnarmoredIncrease,
            baseSpeedFeet = (character.baseSpeedFeet ?: character.speedFeet) +
                (newSubclassStats.speedBonusFeet - oldSubclassStats.speedBonusFeet),
            passiveArmorClassBonus = character.passiveArmorClassBonus + armorBonusIncrease,
        )
        updateSelectedCharacter { updated }
        recentlyLevelledCharacterId = updated.id
        levelUpOpen = false
        levelUpDraft = null
        lastRoll = t("Level $newLevel applied · +$gain maximum HP", "Stufe $newLevel angewendet · +$gain maximale TP")
        return true
    }

    private fun creationHitPointGains(hitDie: Int, constitutionModifier: Int): List<Int> {
        if (creation.ruleset == Ruleset.Pf2eRemaster) {
            val perLevel = (pf2ClassHitPoints(creation.className) + constitutionModifier).coerceAtLeast(1)
            return List(creation.level) { levelIndex ->
                perLevel + if (levelIndex == 0) pf2AncestryHitPoints(creation.ancestry) else 0
            }
        }
        if (creation.level <= 1) return listOf((hitDie + constitutionModifier).coerceAtLeast(1))
        val later = when (creation.hpMethod) {
            HpMethod.Fixed -> List(creation.level - 1) { (hitDie / 2 + 1 + constitutionModifier).coerceAtLeast(1) }
            HpMethod.Manual -> List(creation.level - 1) { creation.manualHpGain.coerceAtLeast(1) }
            HpMethod.Rolled -> {
                while (creation.rolledHpGains.size < creation.level - 1) {
                    creation.rolledHpGains += dice.roll(RollRequest("Level HP", DiceExpression(1, hitDie))).total
                }
                creation.rolledHpGains.take(creation.level - 1).map { (it + constitutionModifier).coerceAtLeast(1) }
            }
        }
        return listOf((hitDie + constitutionModifier).coerceAtLeast(1)) + later
    }

    private fun refreshedClassFeatures(
        character: CharacterUi,
        addedClass: String,
        subclass: String,
        selectedFeatures: List<FeatureUi>,
    ): List<FeatureUi> {
        val classLevels = character.progression.groupingBy(LevelProgressionUi::className).eachCount().toMutableMap().apply {
            this[addedClass] = (this[addedClass] ?: 0) + 1
        }
        val retained = character.features.filterNot { it.id in guidedAutomaticFeatureIds }.toMutableList()
        fun remainingAfterMaximumChange(id: String, maximum: Int): Int {
            val old = character.features.firstOrNull { it.id == id } ?: return maximum
            val spent = ((old.maximum ?: maximum) - (old.remaining ?: maximum)).coerceAtLeast(0)
            return (maximum - spent).coerceIn(0, maximum)
        }
        fun add(feature: FeatureUi) {
            retained.removeAll { it.id == feature.id }
            retained += feature
        }

        guidedClassDefinitions.values.forEach { definition ->
            val classLevel = classLevels[definition.className] ?: return@forEach
            definition.featureUnlocks
                .filter { it.minimumLevel <= classLevel }
                .filter { unlock -> unlock.subclass == null || unlock.subclass.equals(subclass, ignoreCase = true) }
                .filterNot { it.id in setOf("second-wind", "action-surge", "extra-attack", "indomitable", "sorcery-points", "focus-points") }
                .forEach { unlock ->
                    val old = character.features.firstOrNull { it.id == unlock.id }
                    add(
                        FeatureUi(
                            id = unlock.id,
                            name = unlock.name,
                            summary = unlock.summary,
                            remaining = old?.remaining,
                            maximum = old?.maximum,
                            recovery = unlock.recovery,
                            effect = old?.effect ?: FeatureEffect.RESOURCE_ONLY,
                            actionCost = old?.actionCost ?: ActionCost(),
                        )
                    )
                }
        }

        val fighterLevel = classLevels["Fighter"] ?: 0
        if (fighterLevel > 0) {
            val secondWindMaximum = secondWindUses(character.ruleset, fighterLevel)
            add(
                FeatureUi(
                    "second-wind",
                    "Second Wind",
                    "Regain 1d10 + Fighter level ($fighterLevel) HP.",
                    remainingAfterMaximumChange("second-wind", secondWindMaximum),
                    secondWindMaximum,
                    Recovery.SHORT_REST,
                    FeatureEffect.SECOND_WIND,
                    ActionCost(bonusActions = 1),
                )
            )
            if (fighterLevel >= 2) {
                val maximum = if (fighterLevel >= 17) 2 else 1
                add(FeatureUi("action-surge", "Action Surge", "Take one additional action this turn.", remainingAfterMaximumChange("action-surge", maximum), maximum, Recovery.SHORT_REST, FeatureEffect.EXTRA_ACTION))
            }
            if (fighterLevel >= 9) {
                val uses = when {
                    fighterLevel >= 17 -> 3
                    fighterLevel >= 13 -> 2
                    else -> 1
                }
                add(FeatureUi("indomitable", "Indomitable", "Reroll a failed saving throw.", remainingAfterMaximumChange("indomitable", uses), uses, Recovery.LONG_REST, FeatureEffect.REROLL_SAVE))
            }
            if (fighterLevel >= 3 && subclass.equals("Battle Master", ignoreCase = true)) {
                val dice = if (fighterLevel >= 7) 5 else 4
                val die = when {
                    fighterLevel >= 18 -> "d12"
                    fighterLevel >= 10 -> "d10"
                    else -> "d8"
                }
                add(
                    FeatureUi(
                        "superiority-dice",
                        "Superiority Dice",
                        "$dice $die dice fuel your Battle Master maneuvers.",
                        remainingAfterMaximumChange("superiority-dice", dice),
                        dice,
                        Recovery.SHORT_REST,
                        resourceDieSides = die.removePrefix("d").toInt(),
                    )
                )
            }
        }

        val sorcererLevel = classLevels["Sorcerer"] ?: 0
        if (sorcererLevel >= 2) {
            add(
                FeatureUi(
                    "sorcery-points",
                    "Sorcery Points",
                    "$sorcererLevel points fuel Sorcerer class options.",
                    remainingAfterMaximumChange("sorcery-points", sorcererLevel),
                    sorcererLevel,
                    Recovery.LONG_REST,
                )
            )
        }

        val monkLevel = classLevels["Monk"] ?: 0
        if (monkLevel >= 2) {
            add(
                FeatureUi(
                    "focus-points",
                    if (character.ruleset == Ruleset.Fifth2014) "Ki Points" else "Focus Points",
                    "$monkLevel points fuel Monk techniques.",
                    remainingAfterMaximumChange("focus-points", monkLevel),
                    monkLevel,
                    Recovery.SHORT_REST,
                )
            )
        }

        if (fighterLevel >= 5 || monkLevel >= 5) {
            val attacks = when {
                fighterLevel >= 20 -> 4
                fighterLevel >= 11 -> 3
                else -> 2
            }
            add(FeatureUi("extra-attack", "Extra Attack", "Attack $attacks times when you take the Attack action."))
        }

        selectedFeatures.forEach(::add)
        return retained.distinctBy(FeatureUi::id)
    }

    private fun hitDieFor(className: String, ruleset: Ruleset = creation.ruleset): Int = privateEntryForName("class", className, ruleset)?.mechanics?.hitDie
        ?: privateEntryForName("class", className, ruleset)?.formula
        ?.let { Regex("(?:hit[- ]?die\\s*[:=]?\\s*)?d(6|8|10|12)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1)?.toIntOrNull() }
        ?: when (className) {
        "Barbarian" -> 12
        "Fighter", "Paladin", "Champion", "Ranger", "Monk" -> 10
        "Sorcerer", "Wizard" -> 6
        else -> 8
    }

    private fun pf2ClassHitPoints(className: String): Int = privateEntryForName("class", className)?.formula
        ?.let { Regex("(?:hp|hit points)(?:\\s+per\\s+level)?\\s*[:=]?\\s*(6|8|10|12)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1)?.toIntOrNull() }
        ?: when (className) {
            "Barbarian" -> 12
            "Fighter", "Champion", "Ranger", "Monk" -> 10
            "Wizard", "Sorcerer" -> 6
            else -> 8
        }

    private fun pf2AncestryHitPoints(ancestry: String): Int = when (ancestry) {
        "Elf", "Goblin", "Halfling" -> 6
        "Dwarf", "Orc" -> 10
        else -> 8
    }

    private fun primaryAbilityFor(className: String): String {
        privateEntryForName("class", className)?.mechanics?.primaryAbility?.let { return it }
        privateEntryForName("class", className)?.formula?.let { formula ->
            Regex("(?:key|primary)\\s*ability\\s*[:=]?\\s*(STR|DEX|CON|INT|WIS|CHA)", RegexOption.IGNORE_CASE)
                .find(formula)?.groupValues?.get(1)?.uppercase()?.let { return it }
        }
        return when (className) {
            "Wizard", "Alchemist" -> "INT"
            "Cleric", "Druid" -> "WIS"
            "Bard", "Champion", "Paladin", "Sorcerer", "Warlock" -> "CHA"
            "Rogue", "Monk", "Ranger" -> "DEX"
            else -> "STR"
        }
    }

    private fun isCasterClass(ruleset: Ruleset, className: String): Boolean = privateEntryForName("class", className, ruleset)
        ?.mechanics?.caster ?: if (ruleset == Ruleset.Pf2eRemaster) {
        className in setOf("Wizard", "Cleric", "Druid", "Bard", "Sorcerer")
    } else {
        className in setOf("Wizard", "Cleric", "Druid", "Bard", "Sorcerer", "Warlock", "Paladin", "Ranger")
    }

    private fun resolvedCreationArmor(dexterityModifier: Int, proficiency: Int): EquipmentUi? = when (val choice = creation.startingArmorChoice) {
        StartingArmorChoice.Recommended -> startingArmorFor(creation.ruleset, creation.className, dexterityModifier, proficiency)
        StartingArmorChoice.Unarmored -> null
        is StartingArmorChoice.Known -> knownItemCatalog()
            .firstOrNull { it.id == choice.itemId && it.type == KnownItemType.Armor }
            ?.let { resolveKnownEquipment(it, dexterityModifier, proficiency)?.let { armor ->
                armor.copy(id = "starting-armor", worn = true, attuned = armor.needsAttunement)
            } }
            ?: startingArmorFor(creation.ruleset, creation.className, dexterityModifier, proficiency)
        is StartingArmorChoice.Custom -> choice.item.copy(id = "starting-armor", worn = true)
    }

    internal fun resolveKnownEquipmentForCharacter(item: KnownItemUi): EquipmentUi? {
        val character = selectedCharacter ?: return null
        val dexterityModifier = abilityModifier(character.abilities["DEX"] ?: 10)
        return resolveKnownEquipment(item, dexterityModifier, character.proficiency)
    }

    private fun resolveKnownEquipment(item: KnownItemUi, dexterityModifier: Int, proficiency: Int): EquipmentUi? {
        val equipment = item.equipment ?: return null
        val armorClass = when (equipment.id) {
            "leather-armor", "padded-armor" -> 11 + dexterityModifier
            "studded-leather" -> 12 + dexterityModifier
            "hide-armor" -> 12 + dexterityModifier.coerceAtMost(2)
            "chain-shirt" -> 13 + dexterityModifier.coerceAtMost(2)
            "scale-mail", "breastplate" -> 14 + dexterityModifier.coerceAtMost(2)
            "half-plate" -> 15 + dexterityModifier.coerceAtMost(2)
            "ring-mail" -> 14
            "chain-mail" -> 16
            "splint-armor" -> 17
            "plate-armor" -> 18
            "pf2e-leather-armor" -> 10 + proficiency + 1 + dexterityModifier.coerceAtMost(4)
            "pf2e-chain-shirt" -> 10 + proficiency + 2 + dexterityModifier.coerceAtMost(3)
            "pf2e-scale-mail" -> 10 + proficiency + 3 + dexterityModifier.coerceAtMost(2)
            "pf2e-half-plate" -> 10 + proficiency + 5 + dexterityModifier.coerceAtMost(1)
            else -> equipment.armorClass
        }
        return equipment.copy(armorClass = armorClass)
    }

    private fun startingArmorFor(
        ruleset: Ruleset,
        className: String,
        dexterityModifier: Int,
        proficiency: Int,
    ): EquipmentUi? {
        if (ruleset == Ruleset.Pf2eRemaster) {
            val (name, itemBonus, dexterityCap) = when (className) {
                "Champion" -> Triple("Half Plate", 5, 1)
                "Fighter" -> Triple("Scale Mail", 3, 2)
                "Cleric" -> Triple("Chain Shirt", 2, 3)
                "Rogue", "Ranger", "Bard", "Druid", "Alchemist" -> Triple("Leather Armor", 1, 4)
                else -> return null
            }
            return EquipmentUi(
                id = "starting-armor",
                name = name,
                kind = EquipmentKind.ARMOR,
                details = "PF2e item bonus +$itemBonus · Dexterity cap +$dexterityCap",
                worn = true,
                armorClass = 10 + proficiency + itemBonus + dexterityModifier.coerceAtMost(dexterityCap),
            )
        }
        val (name, armorClass) = when (className) {
            "Fighter", "Paladin" -> "Chain Mail" to 16
            "Cleric", "Ranger" -> "Scale Mail" to (14 + dexterityModifier.coerceAtMost(2))
            "Rogue", "Bard", "Druid", "Warlock" -> "Leather Armor" to (11 + dexterityModifier)
            else -> return null
        }
        return EquipmentUi("starting-armor", name, EquipmentKind.ARMOR, worn = true, armorClass = armorClass)
    }

    private fun creationWeapons(
        abilities: Map<String, Int>,
        proficiencyRanks: Map<String, ProficiencyRank>,
    ): List<WeaponUi> {
        val staffClass = creation.className in setOf("Wizard", "Sorcerer", "Cleric", "Druid")
        val dexterityClass = creation.className in setOf("Rogue", "Monk", "Ranger", "Bard", "Alchemist")
        val ability = if (staffClass) "STR" else if (dexterityClass) "DEX" else "STR"
        val abilityModifier = abilityModifier(abilities.getValue(ability))
        fun proficiencyId(categoryId: String, weaponId: String): String =
            categoryId.takeIf { it in proficiencyRanks } ?: "weapon:$weaponId"
        fun weapon(id: String, name: String, damage: String, damageType: String, properties: String, categoryId: String): WeaponUi {
            val trainingId = proficiencyId(categoryId, id)
            val weaponProficiency = proficiencyModifier(creation.ruleset, creation.level, proficiencyRanks[trainingId])
            return WeaponUi(
                id,
                name,
                abilityModifier + weaponProficiency,
                withAbilityDamage(damage, abilityModifier),
                damageType,
                properties,
                ability = ability,
                proficient = trainingId in proficiencyRanks,
                proficiencyId = trainingId,
                damageAbility = ability,
            )
        }
        return when {
            staffClass -> listOf(
                weapon("quarterstaff", "Quarterstaff", "1d6", "Bludgeoning", "Versatile (1d8)", "weapon:simple"),
            )
            dexterityClass -> listOf(
                weapon("shortsword", "Shortsword", "1d6", "Piercing", "Finesse", "weapon:martial"),
            )
            else -> listOf(
                weapon("longsword", "Longsword", "1d8", "Slashing", "Versatile (1d10)", "weapon:martial"),
            )
        }
    }

    private fun creationArmorProficiencyId(): String = when (val choice = creation.startingArmorChoice) {
        is StartingArmorChoice.Known -> when (choice.itemId.removePrefix("equipment:")) {
            "pf2e-half-plate", "half-plate", "ring-mail", "chain-mail", "splint-armor", "plate-armor" -> "armor:heavy"
            "pf2e-scale-mail", "scale-mail", "breastplate" -> "armor:medium"
            "pf2e-leather-armor", "pf2e-chain-shirt", "leather-armor", "studded-leather", "chain-shirt" -> "armor:light"
            else -> "armor:unarmored"
        }
        StartingArmorChoice.Unarmored -> "armor:unarmored"
        is StartingArmorChoice.Custom -> "armor:unarmored"
        StartingArmorChoice.Recommended -> when (creation.className) {
            "Champion" -> "armor:heavy"
            "Fighter" -> "armor:medium"
            "Cleric", "Rogue", "Ranger", "Bard", "Druid", "Alchemist" -> "armor:light"
            else -> "armor:unarmored"
        }
    }

    private fun armorProficiencyId(item: EquipmentUi): String = when {
        item.name.contains("half plate", true) || item.name.contains("plate armor", true) ||
            item.name.contains("chain mail", true) || item.name.contains("splint", true) ||
            item.name.contains("ring mail", true) -> "armor:heavy"
        item.name.contains("scale mail", true) || item.name.contains("breastplate", true) -> "armor:medium"
        else -> "armor:light"
    }

    private fun speedFor(ruleset: Ruleset, ancestry: String): Int = privateEntryForName("ancestry", ancestry, ruleset)
        ?.mechanics?.speedFeet ?: if (ruleset == Ruleset.Pf2eRemaster) {
        when (ancestry) {
            "Dwarf" -> 20
            "Elf" -> 30
            else -> 25
        }
    } else 30

    private fun approvedPrivateEntries(kind: String, ruleset: Ruleset = creation.ruleset): List<PrivateEntryUi> = privateEntries
        .filter { it.normalizedKind() == kind }
        .filter { it.appliesTo(ruleset) }
        .sortedBy { it.name }

    private fun privateEntryById(id: String): PrivateEntryUi? = privateEntries.firstOrNull { it.id == id }

    private fun privateEntryForName(kind: String, name: String, ruleset: Ruleset = creation.ruleset): PrivateEntryUi? = privateEntries.firstOrNull {
        it.normalizedKind() == kind && it.name.equals(name, ignoreCase = true) && it.appliesTo(ruleset)
    }

    private fun privateSpellById(id: String): SpellUi? = privateEntryById(id)
        ?.takeIf { it.normalizedKind() == "spell" && it.appliesTo(creation.ruleset) }
        ?.let(::privateSpell)

    private fun creationGrantedSpells(): List<SpellUi> {
        val ancestryEntry = privateEntryForName("ancestry", creation.ancestry)
        val classEntry = privateEntryForName("class", creation.className)
        val subclassEntry = selectedCreationSubclass()?.let { selected ->
            privateEntryForName("subclass", selected.name) ?: privateEntryById(selected.id)
        }
        val privateGranted = listOfNotNull(ancestryEntry, classEntry, subclassEntry)
            .flatMap { owner -> owner.mechanics.grantedSpellIds.map { owner to it } }
            .distinctBy { it.second }
            .mapNotNull { (owner, id) -> privateSpellById(id)?.copy(sourceKind = SpellSourceKind.FEATURE, sourceName = owner.name) }
        val ancestrySpellIds = when (creation.ruleset) {
            Ruleset.Fifth2014 -> when (creation.ancestry) {
                "Tiefling" -> listOf("spell.thaumaturgy")
                else -> emptyList()
            }
            Ruleset.Fifth2024 -> when (creation.ancestry) {
                "Aasimar" -> listOf("spell.light")
                "Tiefling" -> listOf("spell.thaumaturgy")
                else -> emptyList()
            }
            Ruleset.Pf2eRemaster -> emptyList()
        }
        val ancestryGranted = ancestrySpellIds.mapNotNull { id ->
            srdSpellCatalog(creation.ruleset, SrdSpellClass.entries.toSet(), maximumLevel = 0)
                .firstOrNull { it.id == id }
                ?.copy(sourceKind = SpellSourceKind.FEATURE, sourceName = creation.ancestry)
        }
        val subclassGranted = selectedCreationSubclass()?.resolveSpells(creation.level).orEmpty()
            .map { it.copy(sourceKind = SpellSourceKind.FEATURE, sourceName = creation.subclassName) }
        val classGrantedIds = if (creation.ruleset == Ruleset.Fifth2024) when (creation.className) {
            "Druid" -> listOf("spell.speak-with-animals")
            "Ranger" -> listOf("spell.hunters-mark")
            else -> emptyList()
        } else emptyList()
        val classGranted = classGrantedIds.mapNotNull { id ->
            srdSpellCatalog(creation.ruleset, SrdSpellClass.entries.toSet(), maximumLevel = 9)
                .firstOrNull { it.id == id }
                ?.copy(sourceKind = SpellSourceKind.FEATURE, sourceName = creation.className)
        }
        return (ancestryGranted + classGranted + privateGranted + subclassGranted).distinctBy { it.id }
    }

    private fun privateSpell(entry: PrivateEntryUi): SpellUi? {
        if (entry.normalizedKind() != "spell") return null
        val typed = entry.mechanics.spell
        val level = typed?.level ?: Regex("(?:level|rank)\\s*[:=]?\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(entry.formula)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 10) ?: 0
        val saveAbility = typed?.saveAbility?.let { value ->
            when (value) {
                "STR" -> Ability.STRENGTH
                "DEX" -> Ability.DEXTERITY
                "CON" -> Ability.CONSTITUTION
                "INT" -> Ability.INTELLIGENCE
                "WIS" -> Ability.WISDOM
                "CHA" -> Ability.CHARISMA
                else -> null
            }
        }
        return SpellUi(
            "private-${entry.id}",
            entry.name,
            level,
            entry.summary,
            sourceName = entry.sourceNote,
            activationCost = typed?.actionCost ?: entry.structuredSpellCost(creation.ruleset),
            castPreviews = typed?.castPreviews.orEmpty(),
            savingThrows = saveAbility?.let { listOf(SavingThrowPrompt(it, DifficultyClass(useSpellcasting = true))) }.orEmpty(),
            spellAttack = typed?.spellAttack == true,
            rulesText = typed?.let {
                SpellRulesText(
                    school = it.school,
                    concentration = it.concentration,
                    ritual = it.ritual,
                    castingTime = it.castingTime,
                    range = it.range,
                    components = it.components,
                    duration = it.duration,
                    source = entry.sourceNote,
                )
            } ?: SpellRulesText(),
            combatContributions = entry.combatContributions,
        )
    }

    private fun PrivateEntryUi.structuredSpellCost(ruleset: Ruleset): ActionCost {
        val marker = Regex("(?:activation|cost)\\s*[:=]\\s*(reaction|bonus[ -]?action|(?:[123]\\s*)?actions?)", RegexOption.IGNORE_CASE)
            .find(formula)?.groupValues?.get(1)?.lowercase()?.replace('-', ' ')
        return when {
            marker == "reaction" -> ActionCost(reactions = 1)
            marker == "bonus action" -> ActionCost(bonusActions = 1)
            ruleset == Ruleset.Pf2eRemaster -> ActionCost(pf2eActions = marker?.firstOrNull()?.digitToIntOrNull() ?: 2)
            else -> ActionCost(actions = 1)
        }
    }

    private fun PrivateEntryUi.normalizedKind(): String = normalizedPrivateKind()

    private fun PrivateEntryUi.appliesTo(ruleset: Ruleset): Boolean {
        return ruleset in privateRulesets()
    }

    private fun secondWindUses(ruleset: Ruleset, fighterLevel: Int): Int = when (ruleset) {
        Ruleset.Fifth2014 -> 1
        Ruleset.Fifth2024 -> when {
            fighterLevel >= 10 -> 4
            fighterLevel >= 4 -> 3
            else -> 2
        }
        Ruleset.Pf2eRemaster -> 0
    }

    fun portraitBytes(character: CharacterUi): ByteArray? =
        character.portraitFileName?.let(storage::readPortrait)

    private fun withCompleteSkills(character: CharacterUi): CharacterUi {
        val definitions = ProficiencyCatalog.skills(character.ruleset, character.backgroundId)
        val sourceKeys = (character.skills.keys + character.derivation.skills.keys).toList()
        val consumedKeys = mutableSetOf<String>()
        val completedSkills = linkedMapOf<String, Int>()
        val completedDerivations = linkedMapOf<String, DerivedModifierFormulaUi>()

        definitions.forEach { skill ->
            val sourceKey = sourceKeys.firstOrNull { key ->
                key !in consumedKeys && ProficiencyCatalog.skillByDisplayName(character.ruleset, key)?.id == skill.id
            }
            if (sourceKey != null) consumedKeys += sourceKey
            val existingValue = sourceKey?.let(character.skills::get)
            val existingFormula = sourceKey?.let(character.derivation.skills::get)
            val formula = existingFormula ?: DerivedModifierFormulaUi(skill.ability, proficiencyId = skill.id)
            completedSkills[skill.englishName] = existingValue ?: formula.resolve(
                character.abilities,
                character.proficiency,
                character.ruleset,
                character.level,
                character.proficiencyRanks,
                character.proficiencyIds,
            )
            if (existingFormula != null || existingValue == null) {
                completedDerivations[skill.englishName] = formula
            }
        }

        character.skills.forEach { (name, value) ->
            if (name !in consumedKeys) completedSkills[name] = value
        }
        character.derivation.skills.forEach { (name, formula) ->
            if (name !in consumedKeys) completedDerivations[name] = formula
        }
        return character.copy(
            skills = completedSkills,
            derivation = character.derivation.copy(skills = completedDerivations),
        )
    }

    private fun updateSelectedCharacter(transform: (CharacterUi) -> CharacterUi): CharacterUi? {
        val characterIndex = characters.indexOfFirst { it.id == selectedCharacterId }
        if (characterIndex < 0) return null
        val transformed = transform(characters[characterIndex])
        val updated = withCompleteSkills(
            CharacterStatEngine.resolve(
                transformed,
                conditions.filter { it.characterId.isBlank() || it.characterId == transformed.id },
            ),
        )
        characters[characterIndex] = updated
        persist()
        return updated
    }

    fun adjustHitPoints(amount: Int, damage: Boolean) {
        val safeAmount = amount.coerceAtLeast(0)
        if (safeAmount == 0) return
        val current = selectedCharacter ?: return
        if (!damage && current.isDead) {
            if (current.effectiveMaxHp == 0) {
                showInfo(
                    t("Maximum HP is zero", "Maximale TP sind null"),
                    t("Remove the maximum HP reduction before restoring HP.", "Entferne zuerst die Max.-TP-Senkung."),
                )
                return
            }
            pendingRevivalHp = safeAmount.coerceAtMost(current.effectiveMaxHp).coerceAtLeast(1)
            revivalConfirmationOpen = true
            return
        }
        val updated = (if (damage) applyDamage(safeAmount, critical = false) else updateSelectedCharacter { character ->
            val newHp = (character.hp + safeAmount).coerceAtMost(character.effectiveMaxHp)
            val leftPf2Dying = character.ruleset == Ruleset.Pf2eRemaster && character.dyingValue > 0 && newHp > 0
            character.copy(
                hp = newHp,
                deathSaveSuccesses = if (newHp > 0) 0 else character.deathSaveSuccesses,
                deathSaveFailures = if (newHp > 0) 0 else character.deathSaveFailures,
                isStable = if (newHp > 0) false else character.isStable,
                isDead = if (newHp > 0) false else character.isDead,
                deathReason = if (newHp > 0) null else character.deathReason,
                dyingValue = if (newHp > 0) 0 else character.dyingValue,
                woundedValue = if (leftPf2Dying) character.woundedValue + 1 else character.woundedValue,
            )
        }) ?: return
        recordEvent(
            hitPointEvent(
                before = current,
                after = updated,
                kind = if (damage) HitPointChangeKind.DAMAGE else HitPointChangeKind.HEALING,
                amount = safeAmount,
            ),
        )
        lastRoll = if (damage) {
            t("$safeAmount damage · ${updated.hp}/${updated.effectiveMaxHp} HP", "$safeAmount Schaden · ${updated.hp}/${updated.effectiveMaxHp} TP")
        } else {
            t("$safeAmount healed · ${updated.hp}/${updated.effectiveMaxHp} HP", "$safeAmount geheilt · ${updated.hp}/${updated.effectiveMaxHp} TP")
        }
    }

    fun setHitPoints(target: Int) {
        val character = selectedCharacter ?: return
        val safeTarget = target.coerceIn(0, character.effectiveMaxHp)
        when {
            safeTarget > character.hp -> adjustHitPoints(safeTarget - character.hp, damage = false)
            safeTarget < character.hp -> adjustHitPoints(character.hp - safeTarget, damage = true)
        }
    }

    fun setTemporaryHitPoints(target: Int) {
        val character = selectedCharacter ?: return
        val safeTarget = target.coerceIn(0, 9_999)
        val delta = safeTarget - character.temporaryHp
        if (delta == 0) return
        updateSelectedCharacter { it.copy(temporaryHp = safeTarget) }
        recordEvent(
            TurnEvent.ResourceChanged("temporary-hit-points", delta),
            t("Temporary HP", "Temporäre TP"),
        )
        lastRoll = t("Temporary HP set to $safeTarget", "Temporäre TP auf $safeTarget gesetzt")
    }

    fun setMaximumHitPointReduction(target: Int) {
        val character = selectedCharacter ?: return
        val safeTarget = target.coerceIn(0, character.maxHp)
        val delta = safeTarget - character.maxHpReduction
        if (delta == 0) return
        val updated = updateSelectedCharacter { current ->
            val effectiveMaximum = current.effectiveMaxHp(safeTarget)
            val maximumDeath = current.ruleset != Ruleset.Pf2eRemaster && effectiveMaximum == 0
            current.copy(
                hp = current.hp.coerceAtMost(effectiveMaximum),
                maxHpReduction = safeTarget,
                isDead = maximumDeath || current.isDead,
                deathReason = if (maximumDeath) "Maximum Hit Points are zero" else current.deathReason,
            )
        } ?: return
        recordEvent(
            TurnEvent.ResourceChanged("maximum-hit-point-reduction", delta),
            t("Max HP reduction", "Max.-TP-Senkung"),
        )
        lastRoll = t(
            "Max HP reduction set to $safeTarget · ${updated.hp}/${updated.effectiveMaxHp} HP",
            "Max.-TP-Senkung auf $safeTarget gesetzt · ${updated.hp}/${updated.effectiveMaxHp} TP",
        )
        if (updated.stopsTurnGuide) endConcentration()
    }

    fun applyDamage(amount: Int, critical: Boolean): CharacterUi? {
        val safeAmount = amount.coerceAtLeast(0)
        if (safeAmount == 0) return selectedCharacter
        val updated = updateSelectedCharacter { character ->
            if (character.isDead) return@updateSelectedCharacter character
            val afterTemporary = (safeAmount - character.temporaryHp).coerceAtLeast(0)
            val newTemporary = (character.temporaryHp - safeAmount).coerceAtLeast(0)
            if (afterTemporary == 0) return@updateSelectedCharacter character.copy(temporaryHp = newTemporary)
            if (character.ruleset == Ruleset.Pf2eRemaster) {
                val newHp = (character.hp - afterTemporary).coerceAtLeast(0)
                if (newHp > 0) return@updateSelectedCharacter character.copy(hp = newHp, temporaryHp = newTemporary)
                val dyingIncrease = if (critical) 2 else 1
                val dying = if (character.hp > 0) dyingIncrease + character.woundedValue else character.dyingValue + dyingIncrease
                val threshold = (4 - character.doomedValue).coerceAtLeast(1)
                return@updateSelectedCharacter character.copy(
                    hp = 0,
                    temporaryHp = newTemporary,
                    dyingValue = dying,
                    isStable = false,
                    isDead = dying >= threshold,
                    deathReason = if (dying >= threshold) "Dying $dying" else null,
                )
            }
            if (character.hp == 0) {
                val failures = character.deathSaveFailures + if (critical) 2 else 1
                return@updateSelectedCharacter character.copy(
                    temporaryHp = newTemporary,
                    deathSaveFailures = failures.coerceAtMost(3),
                    isStable = false,
                    isDead = failures >= 3,
                    deathReason = if (failures >= 3) "Failed death saves" else character.deathReason,
                )
            }
            val newHp = (character.hp - afterTemporary).coerceAtLeast(0)
            val overflow = (afterTemporary - character.hp).coerceAtLeast(0)
            val instantDeath = newHp == 0 && overflow >= character.effectiveMaxHp
            character.copy(
                hp = newHp,
                temporaryHp = newTemporary,
                deathSaveSuccesses = if (newHp == 0) 0 else character.deathSaveSuccesses,
                deathSaveFailures = if (newHp == 0) 0 else character.deathSaveFailures,
                isStable = false,
                isDead = instantDeath,
                deathReason = if (instantDeath) "Massive damage" else null,
            )
        }
        if (updated?.stopsTurnGuide == true) {
            endConcentration()
            return selectedCharacter
        }
        return updated
    }

    fun applyTurnDamage(amount: Int, critical: Boolean, session: TurnSession): CharacterUi? {
        val character = selectedCharacter ?: return null
        if (amount !in 1..9_999 || character.id != session.characterId || character.isDead) return character
        val safeAmount = amount
        val updated = applyDamage(safeAmount, critical) ?: return null
        session.record(hitPointEvent(character, updated, HitPointChangeKind.DAMAGE, safeAmount, critical))
        lastRoll = t(
            "$safeAmount damage · ${updated.hp}/${updated.effectiveMaxHp} HP",
            "$safeAmount Schaden · ${updated.hp}/${updated.effectiveMaxHp} TP",
        )
        return updated
    }

    fun resolveDeathSave() {
        val character = selectedCharacter ?: return
        if (!character.isDowned) return
        if (character.ruleset == Ruleset.Pf2eRemaster) {
            resolvePf2eRecovery(character)
            return
        }
        val modifier = if (character.ruleset == Ruleset.Fifth2024) -2 * character.exhaustionLevel else 0
        val mode = if (character.ruleset == Ruleset.Fifth2014 && character.exhaustionLevel >= 3) RollMode.DISADVANTAGE else RollMode.NORMAL
        val rolled = dice.d20("Death Save", modifier, mode)
        val natural = rolled.keptDice.single()
        val total = rolled.total
        val formula = buildString {
            append("d20 $natural")
            if (modifier != 0) append(" ${signed(modifier)} Exhaustion")
            append(" = $total")
        }
        val outcome = when {
            natural == 20 -> t("Natural 20 · 1 HP regained", "Natürliche 20 · 1 TP wiedererlangt")
            natural == 1 -> t("Failed · counts as 2 failures", "Fehlgeschlagen · zählt als 2 Fehlschläge")
            total >= 10 -> t("Successful Death Save", "Todesrettungswurf erfolgreich")
            else -> t("Failed Death Save", "Todesrettungswurf fehlgeschlagen")
        }
        recordEvent(TurnEvent.RollRecorded(rolled), "Death Save")
        dicePresentation = DicePresentationUi(
            ++dicePresentationId,
            "Death Save",
            20,
            rolled.dice,
            natural,
            total,
            formula,
            "DC 10 · $outcome",
            inspirationEligible = true,
        )
        lastRollAction = {
            updateSelectedCharacter { current ->
                current.copy(
                    hp = character.hp,
                    deathSaveSuccesses = character.deathSaveSuccesses,
                    deathSaveFailures = character.deathSaveFailures,
                    isStable = character.isStable,
                    isDead = character.isDead,
                    deathReason = character.deathReason,
                )
            }
            resolveDeathSave()
        }
        val updated = updateSelectedCharacter { current ->
            when {
                natural == 20 -> current.copy(
                    hp = 1,
                    deathSaveSuccesses = 0,
                    deathSaveFailures = 0,
                    isStable = false,
                    isDead = false,
                    deathReason = null,
                    dyingValue = 0,
                )
                natural == 1 -> deathSaveFailure(current, 2)
                total >= 10 -> {
                    val successes = current.deathSaveSuccesses + 1
                    if (successes >= 3) current.copy(deathSaveSuccesses = 0, deathSaveFailures = 0, isStable = true)
                    else current.copy(deathSaveSuccesses = successes)
                }
                else -> deathSaveFailure(current, 1)
            }
        }
        if (natural == 20 && updated != null) {
            recordEvent(hitPointEvent(character, updated, HitPointChangeKind.HEALING, 1), "Death Save")
        }
    }

    private fun resolvePf2eRecovery(character: CharacterUi) {
        val dc = 10 + character.dyingValue
        val rolled = dice.d20("Recovery Check")
        recordEvent(TurnEvent.RollRecorded(rolled), "Recovery Check")
        val natural = rolled.keptDice.single()
        var degree = when {
            rolled.total >= dc + 10 -> 2
            rolled.total >= dc -> 1
            rolled.total <= dc - 10 -> -2
            else -> -1
        }
        if (natural == 20) degree++
        if (natural == 1) degree--
        degree = degree.coerceIn(-2, 2)
        val change = when (degree) {
            2 -> -2
            1 -> -1
            -1 -> 1
            else -> 2
        }
        val outcome = when (degree) {
            2 -> t("Critical Success", "Kritischer Erfolg")
            1 -> t("Success", "Erfolg")
            -1 -> t("Failure", "Fehlschlag")
            else -> t("Critical Failure", "Kritischer Fehlschlag")
        }
        val newDying = (character.dyingValue + change).coerceAtLeast(0)
        val threshold = (4 - character.doomedValue).coerceAtLeast(1)
        dicePresentation = DicePresentationUi(
            ++dicePresentationId,
            "Recovery Check",
            20,
            rolled.dice,
            natural,
            rolled.total,
            "d20 $natural = ${rolled.total}",
            "DC $dc · $outcome",
        )
        lastRollAction = {
            updateSelectedCharacter { current ->
                current.copy(
                    dyingValue = character.dyingValue,
                    woundedValue = character.woundedValue,
                    isStable = character.isStable,
                    isDead = character.isDead,
                    deathReason = character.deathReason,
                )
            }
            resolveDeathSave()
        }
        updateSelectedCharacter { current ->
            current.copy(
                dyingValue = newDying,
                woundedValue = if (newDying == 0) current.woundedValue + 1 else current.woundedValue,
                isStable = newDying == 0,
                isDead = newDying >= threshold,
                deathReason = if (newDying >= threshold) "Dying $newDying" else null,
            )
        }
    }

    private fun deathSaveFailure(character: CharacterUi, amount: Int): CharacterUi {
        val failures = character.deathSaveFailures + amount
        return character.copy(
            deathSaveFailures = failures.coerceAtMost(3),
            isDead = failures >= 3,
            isStable = false,
            deathReason = if (failures >= 3) "Failed death saves" else character.deathReason,
        )
    }

    fun confirmRevival() {
        val target = pendingRevivalHp
        val character = selectedCharacter
        if (character != null && target > 0 && character.effectiveMaxHp > 0 && !(character.deathReason == "Exhaustion" && character.exhaustionLevel >= 6)) {
            val updated = updateSelectedCharacter {
                it.copy(
                    hp = target.coerceAtMost(it.effectiveMaxHp),
                    deathSaveSuccesses = 0,
                    deathSaveFailures = 0,
                    isStable = false,
                    isDead = false,
                    deathReason = null,
                    dyingValue = 0,
                )
            }
            updated?.let { recordEvent(hitPointEvent(character, it, HitPointChangeKind.HEALING, target)) }
        }
        pendingRevivalHp = 0
        revivalConfirmationOpen = false
    }

    fun cancelRevival() {
        pendingRevivalHp = 0
        revivalConfirmationOpen = false
    }

    fun useFeature(featureId: String, session: TurnSession? = turnSession): Boolean {
        val character = selectedCharacter ?: return false
        val feature = character.features.firstOrNull { it.id == featureId } ?: return false
        val pool = feature.resourceId?.let { poolId -> character.features.firstOrNull { it.id == poolId } } ?: feature
        val remaining = pool.remaining
        if (remaining == null && !feature.isActivatable()) return false
        val resourceCost = feature.resourceCost.coerceAtLeast(1)
        if (remaining != null && remaining < resourceCost) return false

        if (session != null) {
            val costAccepted = session.commitCost(feature.actionCost, feature.id)
            if (!costAccepted) {
                lastRoll = t("That turn resource is already used.", "Diese Zugressource ist bereits verbraucht.")
                return false
            }
        }

        if (remaining != null) {
            updateSelectedCharacter { current ->
                current.copy(features = current.features.map {
                    if (it.id == pool.id || it.resourceId == pool.id) it.copy(remaining = remaining - resourceCost) else it
                })
            }
        }
        recordEvent(TurnEvent.FeatureStarted(feature.id, feature.actionCost), feature.name, session)
        if (remaining != null) recordEvent(TurnEvent.ResourceChanged(pool.id, -resourceCost), pool.name, session)

        val resourceDieSides = pool.resourceDieSides
            ?: Regex("d(\\d+)", RegexOption.IGNORE_CASE).find(pool.summary)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val resourceRoll = resourceDieSides?.takeIf { feature.resourceId != null }?.let { sides ->
            dice.roll(RollRequest(feature.name, DiceExpression(1, sides)))
                .also { recordEvent(TurnEvent.RollRecorded(it), feature.name, session) }
        }

        when (feature.effect) {
            FeatureEffect.EXTRA_ACTION -> session?.grantExtraAction()
            FeatureEffect.SECOND_WIND -> {
                val fighterLevel = character.progression.count { it.className == "Fighter" }.takeIf { it > 0 }
                    ?: character.level.takeIf { character.className == "Fighter" }
                    ?: 0
                val before = selectedCharacter ?: character
                val rolled = dice.roll(RollRequest(feature.name, DiceExpression(1, 10, fighterLevel)))
                val healing = rolled.total
                recordEvent(TurnEvent.RollRecorded(rolled), feature.name, session)
                val healed = updateSelectedCharacter { it.copy(hp = (it.hp + healing).coerceAtMost(it.effectiveMaxHp)) }
                healed?.let { recordEvent(hitPointEvent(before, it, HitPointChangeKind.HEALING, healing), feature.name, session) }
                lastRoll = t("${feature.name}: $healing healed · ${healed?.hp}/${healed?.effectiveMaxHp} HP", "${feature.name}: $healing geheilt · ${healed?.hp}/${healed?.effectiveMaxHp} TP")
            }
            else -> lastRoll = if (remaining == null) {
                t("${feature.name} used", "${feature.name} genutzt")
            } else {
                t("${feature.name} used · ${remaining - 1} remaining", "${feature.name} genutzt · noch ${remaining - 1}")
            }
        }
        if (feature.effect != FeatureEffect.SECOND_WIND) {
            lastRoll = if (remaining == null) {
                t("${feature.name} used", "${feature.name} genutzt")
            } else {
                t(
                    "${feature.name} used · ${remaining - resourceCost} remaining",
                    "${feature.name} genutzt · noch ${remaining - resourceCost}",
                )
            }
        }
        inlineFeatureFeedback = InlineFeatureFeedbackUi(
            id = ++inlineFeatureFeedbackId,
            featureId = feature.id,
            rolledValue = resourceRoll?.total,
            message = resourceRoll?.let { "d${resourceDieSides}: ${it.total}" } ?: t("Used", "Genutzt"),
        )
        return true
    }

    fun clearInlineFeatureFeedback() {
        inlineFeatureFeedback = null
    }

    fun availableSpellSlotLevels(spell: SpellUi, character: CharacterUi? = selectedCharacter): List<Int> {
        if (spell.level == 0) return emptyList()
        return character?.resolvedSpellSlots.orEmpty()
            .filter { it.level >= spell.level && it.remaining > 0 }
            .map { it.level }
    }

    fun rulesSpellSlotMaximum(level: Int, character: CharacterUi? = selectedCharacter): Int {
        if (level !in 1..9) return 0
        val active = character ?: return 0
        return DerivedStatRules.fiveESpellSlots(active.fiveECasterLevel).getOrElse(level - 1) { 0 }
    }

    fun updateSpellSlotMaximum(level: Int, maximum: Int): Boolean {
        if (level !in 1..9 || maximum !in 0..10) return false
        val character = selectedCharacter ?: return false
        val current = character.resolvedSpellSlots.firstOrNull { it.level == level }
        val stored = character.spellSlots.firstOrNull { it.level == level }
        val oldMaximum = current?.maximum ?: stored?.maximum ?: rulesSpellSlotMaximum(level, character)
        val oldRemaining = current?.remaining ?: stored?.remaining ?: oldMaximum
        val spent = ((stored?.maximum ?: oldMaximum) - (stored?.remaining ?: oldRemaining)).coerceAtLeast(0)
        val ruleMaximum = rulesSpellSlotMaximum(level, character)
        val overrides = character.spellSlotMaximumOverrides.toMutableMap().apply {
            if (maximum == ruleMaximum) remove(level) else put(level, maximum)
        }
        val storageMaximum = maximum.coerceAtLeast(spent)
        val slot = SpellSlotUi(level, (maximum - spent).coerceIn(0, maximum), storageMaximum)
        updateSelectedCharacter { currentCharacter ->
            currentCharacter.copy(
                spellSlotMaximumOverrides = overrides,
                spellSlots = (currentCharacter.spellSlots.filterNot { it.level == level } + slot).sortedBy { it.level },
            )
        }
        return true
    }

    fun shouldWarnAboutSpellSlotEdit(character: CharacterUi? = selectedCharacter): Boolean =
        character != null && recentlyLevelledCharacterId != character.id

    fun canCastSpell(
        spell: SpellUi,
        slotLevel: Int? = null,
        session: TurnSession? = turnSession,
    ): Boolean {
        val character = selectedCharacter ?: return false
        if (spell !in character.availableSpells) return false
        if (session != null && !session.canPay(spell.activationCost)) return false
        if (spell.level == 0) return slotLevel == null
        return slotLevel != null && slotLevel in availableSpellSlotLevels(spell, character)
    }

    private fun resolvedSpellRulesText(spell: SpellUi, ruleset: Ruleset): SpellRulesText {
        if (ruleset == Ruleset.Pf2eRemaster) return spell.rulesText
        val revision = if (ruleset == Ruleset.Fifth2014) SrdSpellRevision.SRD_5_1 else SrdSpellRevision.SRD_5_2_1
        val stableId = if (spell.id.startsWith("spell.")) spell.id else "spell.${spell.id.replace('_', '-')}"
        val entry = SrdSpellCatalog.find(revision, stableId)
            ?: SrdSpellCatalog.entries.firstOrNull { candidate ->
                candidate.revision == revision && candidate.en.name.equals(spell.name, ignoreCase = true)
            }
            ?: return spell.rulesText
        val details = SrdSpellDetailsCatalog.find(revision, entry.id)
        return SpellRulesText(
            school = spell.rulesText.school.ifBlank { entry.school },
            concentration = spell.rulesText.concentration || entry.concentration,
            ritual = spell.rulesText.ritual || entry.ritual,
            castingTime = spell.rulesText.castingTime.ifBlank { details?.castingTime.orEmpty() },
            range = spell.rulesText.range.ifBlank { details?.range.orEmpty() },
            components = spell.rulesText.components.ifBlank { details?.components.orEmpty() },
            duration = spell.rulesText.duration.ifBlank { details?.duration.orEmpty() },
            effect = spell.rulesText.effect.ifBlank { details?.effect.orEmpty() },
            durationRounds = spell.rulesText.durationRounds ?: details?.durationRounds,
            source = spell.rulesText.source.ifBlank { details?.source.orEmpty() },
        )
    }

    private fun beginConcentration(spell: SpellUi, rulesText: SpellRulesText) {
        val character = selectedCharacter ?: return
        val previous = character.activeConcentration
        val active = ActiveConcentration(
            spellId = spell.id,
            spellName = spell.name,
            spellLevel = spell.level,
            rulesText = rulesText,
            remainingRounds = rulesText.durationRounds,
        )
        updateSelectedCharacter { it.copy(activeConcentration = active) }
        previous?.let {
            recordEvent(TurnEvent.ConditionRemoved("concentration:${it.spellId}"), it.spellName)
        }
        recordEvent(TurnEvent.ConditionApplied("concentration:${spell.id}"), spell.name)
    }

    fun endConcentration(): Boolean {
        val active = selectedCharacter?.activeConcentration ?: return false
        updateSelectedCharacter { it.copy(activeConcentration = null) }
        recordEvent(TurnEvent.ConditionRemoved("concentration:${active.spellId}"), active.spellName)
        return true
    }

    fun concentrationRemainingLabel(active: ActiveConcentration): String {
        val rounds = active.remainingRounds ?: return active.rulesText.duration
        val seconds = rounds * 6
        val hours = seconds / 3_600
        val minutes = seconds % 3_600 / 60
        val remainder = seconds % 60
        return if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:${remainder.toString().padStart(2, '0')}"
        } else {
            "$minutes:${remainder.toString().padStart(2, '0')}"
        }
    }

    fun concentrationSaveDc(damage: Int, ruleset: Ruleset = selectedCharacter?.ruleset ?: Ruleset.Fifth2024): Int {
        val dc = maxOf(10, damage.coerceAtLeast(0) / 2)
        return if (ruleset == Ruleset.Fifth2024) dc.coerceAtMost(30) else dc
    }

    fun showActiveConcentrationDetails() {
        val active = selectedCharacter?.activeConcentration ?: return
        val rules = active.rulesText
        val classification = buildList {
            add(if (active.spellLevel == 0) t("Cantrip", "Zaubertrick") else t("Level ${active.spellLevel}", "Grad ${active.spellLevel}"))
            rules.school.takeIf(String::isNotBlank)?.let { add(localizedSpellSchool(it)) }
            if (rules.ritual) add("Ritual")
            add(t("Concentration", "Konzentration"))
        }.joinToString(" · ")
        val fields = buildList {
            rules.castingTime.takeIf(String::isNotBlank)?.let { add("${t("Casting time", "Zeitaufwand")}: $it") }
            rules.range.takeIf(String::isNotBlank)?.let { add("${t("Range", "Reichweite")}: $it") }
            rules.components.takeIf(String::isNotBlank)?.let { add("${t("Components", "Komponenten")}: $it") }
            rules.duration.takeIf(String::isNotBlank)?.let { add("${t("Duration", "Dauer")}: $it") }
            active.remainingRounds?.let { add("${t("Remaining", "Verbleibend")}: ${concentrationRemainingLabel(active)}") }
        }.joinToString("\n")
        val body = buildList {
            add(classification)
            if (fields.isNotBlank()) add(fields)
            rules.effect.takeIf(String::isNotBlank)?.let(::add)
            rules.source.takeIf(String::isNotBlank)?.let { add(t("Source: $it · Spell Descriptions", "Quelle: $it · Zauberbeschreibungen")) }
        }.joinToString("\n\n")
        showInfo(active.spellName, body)
    }

    private fun advanceConcentration(character: CharacterUi): CharacterUi {
        val active = character.activeConcentration ?: return character
        val remaining = active.remainingRounds ?: return character
        return character.copy(
            activeConcentration = if (remaining <= 1) null else active.copy(remainingRounds = remaining - 1),
        )
    }

    fun castSpell(
        spell: SpellUi,
        slotLevel: Int? = null,
        session: TurnSession? = turnSession,
        actionCostAlreadyCommitted: Boolean = false,
    ): Boolean {
        val character = selectedCharacter ?: return false
        if (spell !in character.availableSpells) return false
        val rulesText = resolvedSpellRulesText(spell, character.ruleset)
        val resolvedSlotLevel = if (spell.level == 0) null else slotLevel
        if (spell.level > 0 && resolvedSlotLevel !in availableSpellSlotLevels(spell, character)) return false
        if (!actionCostAlreadyCommitted) {
            if (session != null) {
                if (!session.commitCost(spell.activationCost, "spell:${spell.id}")) return false
            } else {
                recordEvent(TurnEvent.ActionUsed("spell:${spell.id}", spell.activationCost), spell.name, null)
            }
        }
        if (resolvedSlotLevel != null) {
            updateSelectedCharacter { current ->
                current.copy(spellSlots = current.resolvedSpellSlots.map { slot ->
                    if (slot.level == resolvedSlotLevel) slot.copy(remaining = slot.remaining - 1) else slot
                })
            }
            recordEvent(TurnEvent.ResourceChanged("spell-slot-$resolvedSlotLevel", -1), spell.name, session)
        }
        lastRoll = if (resolvedSlotLevel == null) {
            t("${spell.name} cast", "${spell.name} gewirkt")
        } else {
            t("${spell.name} cast with a level $resolvedSlotLevel slot", "${spell.name} mit Zauberplatz Grad $resolvedSlotLevel gewirkt")
        }
        if (rulesText.concentration) beginConcentration(spell, rulesText)
        return true
    }

    fun sorcerySpellSlotOptions(character: CharacterUi? = selectedCharacter): List<Pair<SpellSlotUi, Int>> {
        if (character?.isSorcerer != true) return emptyList()
        val costs = mapOf(1 to 2, 2 to 3, 3 to 5, 4 to 6, 5 to 7)
        return character.resolvedSpellSlots.mapNotNull { slot ->
            costs[slot.level]?.takeIf { slot.remaining < slot.maximum }?.let { slot to it }
        }
    }

    fun regainSpellSlotWithSorceryPoints(level: Int): Boolean {
        val character = selectedCharacter ?: return false
        if (!character.isSorcerer) return false
        val option = sorcerySpellSlotOptions(character).firstOrNull { it.first.level == level } ?: return false
        val cost = option.second
        val pool = character.features.firstOrNull { it.id == "sorcery-points" } ?: return false
        val remainingPoints = pool.remaining ?: return false
        if (remainingPoints < cost) return false
        updateSelectedCharacter { current ->
            current.copy(
                features = current.features.map { feature ->
                    if (feature.id == pool.id || feature.resourceId == pool.id) feature.copy(remaining = remainingPoints - cost) else feature
                },
                spellSlots = current.resolvedSpellSlots.map { slot ->
                    if (slot.level == level) slot.copy(remaining = (slot.remaining + 1).coerceAtMost(slot.maximum)) else slot
                },
            )
        }
        recordEvent(TurnEvent.ResourceChanged(pool.id, -cost), pool.name)
        recordEvent(TurnEvent.ResourceChanged("spell-slot-$level", 1), t("Spell slot", "Zauberplatz"))
        lastRoll = t("Regained one level $level spell slot", "Einen Zauberplatz Grad $level wiederhergestellt")
        return true
    }

    fun resetFeature(featureId: String) {
        val character = selectedCharacter ?: return
        val feature = character.features.firstOrNull { it.id == featureId } ?: return
        val pool = feature.resourceId?.let { poolId -> character.features.firstOrNull { it.id == poolId } } ?: feature
        val maximum = pool.maximum ?: return
        updateSelectedCharacter { current ->
            current.copy(features = current.features.map {
                if (it.id == pool.id || it.resourceId == pool.id) it.copy(remaining = maximum) else it
            })
        }
        lastRoll = t("${feature.name} reset", "${feature.name} zurückgesetzt")
    }

    private fun recoveredFeatures(character: CharacterUi, recovery: Recovery): List<FeatureUi> =
        character.features.map { feature ->
            when {
                feature.id == "second-wind" && character.ruleset == Ruleset.Fifth2024 && recovery == Recovery.LONG_REST && feature.maximum != null ->
                    feature.copy(remaining = feature.maximum)
                feature.id == "second-wind" && character.ruleset == Ruleset.Fifth2024 && recovery == Recovery.SHORT_REST && feature.maximum != null ->
                    feature.copy(remaining = ((feature.remaining ?: 0) + 1).coerceAtMost(feature.maximum))
                recovery == Recovery.LONG_REST && feature.recovery == Recovery.SHORT_REST && feature.maximum != null ->
                    feature.copy(remaining = feature.maximum)
                feature.recovery == recovery && feature.maximum != null -> feature.copy(remaining = feature.maximum)
                else -> feature
            }
        }

    fun recoverFeatures(recovery: Recovery) {
        val character = selectedCharacter ?: return
        val recovered = recoveredFeatures(character, recovery)
        if (recovered == character.features) return
        updateSelectedCharacter { current -> current.copy(features = recoveredFeatures(current, recovery)) }
    }

    fun canTakeRest(characterId: String = selectedCharacterId.orEmpty()): Boolean {
        val character = characters.firstOrNull { it.id == characterId } ?: return false
        return character.ruleset != Ruleset.Pf2eRemaster &&
            character.hp > 0 &&
            !character.isDead &&
            character.hasPlayedSinceLongRest
    }

    fun availableRations(character: CharacterUi? = selectedCharacter): List<EquipmentUi> =
        character?.resolvedEquipment.orEmpty()
            .filter { it.kind == EquipmentKind.RATIONS && it.quantity > 0 }
            .sortedForPicker(language, EquipmentUi::name, EquipmentUi::id)

    fun takeRest(recovery: Recovery, rationItemId: String? = null): Boolean {
        if (recovery != Recovery.SHORT_REST && recovery != Recovery.LONG_REST) return false
        val before = selectedCharacter ?: return false
        if (!canTakeRest(before.id)) return false
        val ration = rationItemId?.let { id ->
            before.resolvedEquipment.firstOrNull { it.id == id && it.kind == EquipmentKind.RATIONS && it.quantity > 0 }
                ?: return false
        }
        if (recovery != Recovery.LONG_REST && ration != null) return false
        val updated = updateSelectedCharacter { current ->
            val equipmentAfterRation = if (ration == null) {
                current.equipmentItems
            } else {
                current.equipmentItems.mapNotNull { item ->
                    if (item.id != ration.id) item else item.copy(quantity = item.quantity - 1).takeIf { it.quantity > 0 }
                }
            }
            val withRecoveredFeatures = current.copy(
                features = recoveredFeatures(current, recovery),
                equipmentItems = equipmentAfterRation,
                hasPlayedSinceLongRest = recovery != Recovery.LONG_REST,
            )
            if (recovery == Recovery.SHORT_REST) {
                withRecoveredFeatures
            } else {
                val withReducedExhaustion = withRecoveredFeatures.copy(
                    exhaustionLevel = (withRecoveredFeatures.exhaustionLevel - 1).coerceAtLeast(0),
                    spellSlots = withRecoveredFeatures.resolvedSpellSlots.map { it.copy(remaining = it.maximum) },
                    maxHpReduction = 0,
                    activeConcentration = null,
                )
                withReducedExhaustion.copy(
                    hp = withReducedExhaustion.effectiveMaxHp,
                    temporaryHp = 0,
                    deathSaveSuccesses = 0,
                    deathSaveFailures = 0,
                    isStable = false,
                    deathReason = null,
                )
            }
        } ?: return false
        val restoredHitPoints = updated.hp - before.hp
        if (restoredHitPoints > 0) {
            recordEvent(
                hitPointEvent(before, updated, HitPointChangeKind.HEALING, restoredHitPoints),
                t("Long Rest", "Lange Rast"),
            )
        }
        ration?.let {
            recordEvent(TurnEvent.ResourceChanged("ration:${it.id}", -1), it.name)
        }
        if (recovery == Recovery.LONG_REST) {
            updateSelectedCharacter { it.copy(hasPlayedSinceLongRest = false) }
        }
        lastRoll = when (recovery) {
            Recovery.SHORT_REST -> t("Short Rest completed", "Kurze Rast abgeschlossen")
            Recovery.LONG_REST -> t(
                "Long Rest completed · ${updated.hp}/${updated.effectiveMaxHp} HP",
                "Lange Rast abgeschlossen · ${updated.hp}/${updated.effectiveMaxHp} TP",
            )
            else -> error("Unsupported rest recovery")
        }
        return true
    }

    fun openTurn(section: TurnSection? = null) {
        var character = selectedCharacter ?: return
        ensureActivePlaySession()
        character = selectedCharacter ?: return
        turnSession?.takeIf { it.characterId == character.id }?.let { existing ->
            section?.let { existing.selectedSection = it }
            turnOpen = true
            return
        }
        val restoredDraft = savedTurnDrafts[character.id]
        if (restoredDraft == null) recoverFeatures(Recovery.TURN_START)
        val current = selectedCharacter ?: return
        turnSession = createTurnSession(current, restoredDraft).also { session ->
            section?.let { session.selectedSection = it }
        }
        turnOpen = true
    }

    fun closeTurnGuide() {
        saveTurnDraft()
        turnOpen = false
    }

    fun saveTurnDraft() {
        val session = turnSession ?: return
        savedTurnDrafts[session.characterId] = session.snapshot()
        persist()
    }

    fun finishTurn() {
        val session = turnSession ?: return
        if (session.hasCommittedEvent && session.events.lastOrNull() != TurnEvent.TurnEnded) {
            session.record(TurnEvent.TurnEnded)
        }
        savedTurnDrafts.remove(session.characterId)
        turnSession = null
        turnOpen = false
        updateSelectedCharacter { character ->
            val active = character.activePlaySession ?: return@updateSelectedCharacter character
            advanceConcentration(
                character.copy(activePlaySession = active.copy(currentTurnNumber = active.currentTurnNumber + 1)),
            )
        }
        persist()
    }

    fun nextTurn(confirmEmpty: Boolean = false): Boolean {
        val session = turnSession ?: run {
            val hadDraft = hasSavedTurnDraft()
            openTurn()
            return if (hadDraft) nextTurn(confirmEmpty) else true
        }
        if (!session.hasCommittedEvent && !confirmEmpty) return false
        if (session.events.lastOrNull() != TurnEvent.TurnEnded) session.record(TurnEvent.TurnEnded)
        savedTurnDrafts.remove(session.characterId)
        turnSession = null
        updateSelectedCharacter { character ->
            val active = character.activePlaySession ?: return@updateSelectedCharacter character
            advanceConcentration(
                character.copy(activePlaySession = active.copy(currentTurnNumber = active.currentTurnNumber + 1)),
            )
        }
        turnOpen = false
        persist()
        openTurn()
        return true
    }

    fun savePlaySession(title: String) {
        val character = selectedCharacter ?: return
        val currentTurn = turnSession?.takeIf { it.characterId == character.id }
        if (currentTurn?.hasCommittedEvent == true && currentTurn.events.lastOrNull() != TurnEvent.TurnEnded) {
            currentTurn.record(TurnEvent.TurnEnded)
        }
        val refreshed = selectedCharacter ?: return
        val active = refreshed.activePlaySession ?: return
        if (active.activities.isEmpty()) return
        val saved = active.copy(
            title = title.trim().ifBlank { t("Session ${active.ordinal}", "Sitzung ${active.ordinal}") },
            savedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
        )
        savedTurnDrafts.remove(character.id)
        turnSession = null
        turnOpen = false
        sessionSaveOpen = false
        updateSelectedCharacter { it.copy(activePlaySession = null, savedPlaySessions = it.savedPlaySessions + saved) }
        persist()
    }

    fun renamePlaySession(sessionId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        updateSelectedCharacter { character ->
            character.copy(savedPlaySessions = character.savedPlaySessions.map { session ->
                if (session.id == sessionId) session.copy(title = trimmed) else session
            })
        }
    }

    fun deletePlaySession(sessionId: String) {
        updateSelectedCharacter { character ->
            character.copy(savedPlaySessions = character.savedPlaySessions.filterNot { it.id == sessionId })
        }
    }

    fun discardTurn() {
        val session = turnSession ?: return
        val index = characters.indexOfFirst { it.id == session.characterId }
        if (index >= 0) {
            val current = characters[index]
            val active = current.activePlaySession?.let { playSession ->
                playSession.copy(activities = playSession.activities.filterNot { it.turnNumber == playSession.currentTurnNumber })
            }
            characters[index] = session.baseline.copy(
                activePlaySession = active,
                savedPlaySessions = current.savedPlaySessions,
            )
        }
        savedTurnDrafts.remove(session.characterId)
        turnSession = null
        turnOpen = false
        persist()
    }

    fun roll(
        label: String,
        modifier: Int,
        sides: Int = 20,
        mode: RollMode = RollMode.NORMAL,
        modifierLabel: String = "",
    ): String {
        val rolledDice = if (sides == 20) dice.d20(label, modifier, mode) else null
        val rolled = rolledDice ?: dice.roll(RollRequest(label, DiceExpression(1, sides, modifier)))
        val die = rolledDice?.keptDice?.single() ?: rolled.dice.single()
        val sign = if (modifier >= 0) "+" else "−"
        val result = "$label: ${rolled.total}  (d$sides $die $sign ${kotlin.math.abs(modifier)})"
        dicePresentation = DicePresentationUi(
            ++dicePresentationId,
            label,
            sides,
            rolled.dice,
            die,
            rolled.total,
            "d$sides $die ${signed(modifier)} = ${rolled.total}",
            modifierLabel = modifierLabel,
            inspirationEligible = sides == 20 && selectedCharacter?.ruleset != Ruleset.Pf2eRemaster,
        )
        recordEvent(TurnEvent.RollRecorded(rolled))
        lastRoll = result
        lastRollAction = { roll(label, modifier, sides, mode, modifierLabel) }
        return result
    }

    internal fun rollDicePool(countsBySides: Map<Int, Int>): DicePoolRoll =
        dice.rollPool(countsBySides)

    fun rerollDicePresentation() {
        lastRollAction?.invoke()
    }

    fun rerollDicePresentationWithInspiration(): Boolean {
        val reroll = lastRollAction ?: return false
        if (!canUseInspirationForCurrentRoll || !consumeInspiration()) return false
        reroll()
        return true
    }

    fun attackCalculation(character: CharacterUi, weapon: WeaponUi, multipleAttackPenalty: Int = 0): AttackCalculationUi {
        val ability = weapon.abilityModifierOverride ?: abilityModifier(character.abilities[weapon.ability] ?: 10)
        val rank = weapon.proficiencyId?.let { id -> proficiencyRank(id, character.proficiencyRanks, character.proficiencyIds) }
        val proficiency = if (weapon.proficiencyId != null) {
            proficiencyModifier(character.ruleset, character.level, rank)
        } else if (weapon.proficient) character.proficiency else 0
        val exhaustion = if (character.ruleset == Ruleset.Fifth2024) -2 * character.exhaustionLevel else 0
        val total = (weapon.attackBonusOverride ?: weapon.attackBonus) + exhaustion + multipleAttackPenalty
        return AttackCalculationUi(
            ability = ability,
            proficiency = proficiency,
            item = total - ability - proficiency - multipleAttackPenalty,
            multipleAttackPenalty = multipleAttackPenalty,
            total = total,
            abilityLabel = weapon.ability,
        )
    }

    fun openSheetAttack(weaponId: String, attackOptionId: String? = null) {
        if (selectedCharacter?.weapons?.none { it.id == weaponId } != false) return
        if (attackOptionId != null && selectedCharacter?.let { character ->
                derivedAttackOptions(character, character.weapons.first { it.id == weaponId }).none { it.id == attackOptionId }
            } != false
        ) return
        sheetAttackWeaponId = weaponId
        sheetAttackOptionId = attackOptionId
        sheetAttackRoll = null
        sheetAttackOutcome = AttackOutcome.Pending
        sheetDamageRoll = null
        sheetAttackResourcesCommitted = false
    }

    fun closeSheetAttack() {
        sheetAttackWeaponId = null
        sheetAttackOptionId = null
        sheetAttackRoll = null
        sheetAttackOutcome = AttackOutcome.Pending
        sheetDamageRoll = null
        sheetAttackResourcesCommitted = false
    }

    internal fun currentSheetAttackOption(): ResolvedAttackOptionUi? {
        val character = selectedCharacter ?: return null
        val parent = character.weapons.firstOrNull { it.id == sheetAttackWeaponId } ?: return null
        return derivedAttackOptions(character, parent).firstOrNull { it.id == sheetAttackOptionId }
    }

    internal fun currentSheetAttackWeapon(): WeaponUi? {
        val character = selectedCharacter ?: return null
        return currentSheetAttackOption()?.weapon ?: character.weapons.firstOrNull { it.id == sheetAttackWeaponId }
    }

    internal fun canRollCurrentSheetAttack(): Boolean {
        val option = currentSheetAttackOption() ?: return currentSheetAttackWeapon() != null
        if (sheetAttackResourcesCommitted) return true
        val character = selectedCharacter ?: return false
        return option.cost.resources.all { (resourceId, amount) ->
            (character.features.firstOrNull { it.id == resourceId }?.remaining ?: 0) >= amount
        }
    }

    private fun consumeSheetAttackResources(option: ResolvedAttackOptionUi?): Boolean {
        if (option == null || sheetAttackResourcesCommitted || option.cost.resources.isEmpty()) return true
        val character = selectedCharacter ?: return false
        val pools = option.cost.resources.mapNotNull { (resourceId, amount) ->
            character.features.firstOrNull { it.id == resourceId && it.remaining != null }
                ?.let { Triple(resourceId, amount, it) }
        }
        if (pools.size != option.cost.resources.size || pools.any { (_, amount, feature) -> (feature.remaining ?: 0) < amount }) {
            lastRoll = t("Not enough resources for ${option.weapon.name}.", "Nicht genügend Ressourcen für ${option.weapon.name}.")
            return false
        }
        updateSelectedCharacter { current ->
            current.copy(features = current.features.map { feature ->
                val amount = option.cost.resources[feature.id]
                if (amount != null && feature.remaining != null) feature.copy(remaining = (feature.remaining - amount).coerceAtLeast(0)) else feature
            })
        }
        sheetAttackResourcesCommitted = true
        return true
    }

    fun rollSheetAttack(mode: RollMode) {
        val character = selectedCharacter ?: return
        val option = currentSheetAttackOption()
        if (!consumeSheetAttackResources(option)) return
        val weapon = option?.weapon ?: character.weapons.firstOrNull { it.id == sheetAttackWeaponId } ?: return
        val details = performAttack(character, weapon, mode)
        sheetAttackRoll = details
        val naturalCritical = character.ruleset != Ruleset.Pf2eRemaster && details.natural >= character.criticalHitThreshold
        sheetAttackOutcome = if (naturalCritical) AttackOutcome.Critical else AttackOutcome.Pending
        sheetDamageRoll = if (naturalCritical) performDamage(weapon, critical = true) else null
        val eventWeaponId = option?.parentWeaponId ?: weapon.id
        recordEvent(TurnEvent.AttackMade(eventWeaponId, option?.id, option?.sourceId, option?.cost ?: ActionCost(attacks = 1)))
        recordEvent(TurnEvent.RollRecorded(details.toDiceRoll(weapon.name)))
        if (naturalCritical) {
            recordEvent(TurnEvent.AttackResolved(eventWeaponId, AttackOutcomeRecord.CRITICAL, option?.id, option?.sourceId))
            sheetDamageRoll?.takeIf { it.dice.isNotEmpty() }?.let { recordEvent(TurnEvent.RollRecorded(it.toDiceRoll(weapon.name))) }
        }
    }

    fun rerollSheetAttackWithInspiration(): Boolean {
        val character = selectedCharacter ?: return false
        val weapon = currentSheetAttackWeapon() ?: return false
        val previous = sheetAttackRoll ?: return false
        if (!canUseInspirationForSheetAttack || !consumeInspiration()) return false

        val details = performAttack(character, weapon, previous.mode)
        sheetAttackRoll = details
        val naturalCritical = details.natural >= character.criticalHitThreshold
        sheetAttackOutcome = if (naturalCritical) AttackOutcome.Critical else AttackOutcome.Pending
        sheetDamageRoll = if (naturalCritical) performDamage(weapon, critical = true) else null
        recordEvent(TurnEvent.RollRecorded(details.toDiceRoll(weapon.name)))
        if (naturalCritical) {
            val option = currentSheetAttackOption()
            recordEvent(TurnEvent.AttackResolved(option?.parentWeaponId ?: weapon.id, AttackOutcomeRecord.CRITICAL, option?.id, option?.sourceId))
            sheetDamageRoll?.takeIf { it.dice.isNotEmpty() }?.let { recordEvent(TurnEvent.RollRecorded(it.toDiceRoll(weapon.name))) }
        }
        return true
    }

    fun rollSheetDamage() {
        val weapon = currentSheetAttackWeapon() ?: return
        sheetDamageRoll = performDamage(weapon, critical = false).also { details ->
            if (details.dice.isNotEmpty()) recordEvent(TurnEvent.RollRecorded(details.toDiceRoll(weapon.name)))
        }
    }

    fun resolveSheetAttack(outcome: AttackOutcome) {
        val weapon = currentSheetAttackWeapon() ?: return
        val option = currentSheetAttackOption()
        sheetAttackOutcome = outcome
        if (outcome != AttackOutcome.Pending) recordEvent(TurnEvent.AttackResolved(option?.parentWeaponId ?: weapon.id, outcome.toRecord(), option?.id, option?.sourceId))
        sheetDamageRoll = when (outcome) {
            AttackOutcome.Hit -> performDamage(weapon, critical = false).also { if (it.dice.isNotEmpty()) recordEvent(TurnEvent.RollRecorded(it.toDiceRoll(weapon.name))) }
            AttackOutcome.Critical -> performDamage(weapon, critical = true).also { if (it.dice.isNotEmpty()) recordEvent(TurnEvent.RollRecorded(it.toDiceRoll(weapon.name))) }
            else -> null
        }
    }

    private fun consumeDerivedAttackResources(option: ResolvedAttackOptionUi, session: TurnSession): Boolean {
        val priorRolls = session.events.count { (it as? TurnEvent.AttackMade)?.attackOptionId == option.id }
        if (priorRolls % option.attackCount != 0 || option.cost.resources.isEmpty()) return true
        val character = selectedCharacter ?: return false
        val pools = option.cost.resources.mapNotNull { (resourceId, amount) ->
            character.features.firstOrNull { it.id == resourceId && it.remaining != null }?.let { Triple(resourceId, amount, it) }
        }
        if (pools.size != option.cost.resources.size || pools.any { (_, amount, feature) -> (feature.remaining ?: 0) < amount }) {
            lastRoll = t("Not enough resources for ${option.weapon.name}.", "Nicht genügend Ressourcen für ${option.weapon.name}.")
            return false
        }
        updateSelectedCharacter { current ->
            current.copy(features = current.features.map { feature ->
                val amount = option.cost.resources[feature.id]
                if (amount != null && feature.remaining != null) feature.copy(remaining = (feature.remaining - amount).coerceAtLeast(0)) else feature
            })
        }
        return true
    }

    fun rollAttack(
        weapon: WeaponUi,
        session: TurnSession,
        requestedMode: RollMode? = null,
        attackOption: ResolvedAttackOptionUi? = null,
    ) {
        val mode = requestedMode ?: when {
            session.advantage && !session.disadvantage -> RollMode.ADVANTAGE
            session.disadvantage && !session.advantage -> RollMode.DISADVANTAGE
            else -> RollMode.NORMAL
        }
        val character = selectedCharacter ?: return
        if (attackOption != null && !attackOption.isAvailableIn(session, character)) {
            lastRoll = attackOption.unavailableReason(session, character)
            return
        }
        val attackNumber = if (character.ruleset == Ruleset.Pf2eRemaster) {
            if (session.unresolvedAttackCommitted) session.pf2AttacksMade.coerceAtLeast(1) else session.pf2AttacksMade + 1
        } else 1
        val multipleAttackPenalty = if (character.ruleset == Ruleset.Pf2eRemaster) {
            DerivedStatRules.pf2eMultipleAttackPenalty(
                attackNumber = attackNumber,
                agile = weapon.properties.contains("agile", ignoreCase = true),
            )
        } else 0
        val details = performAttack(character, weapon, mode, multipleAttackPenalty)
        val qualifier = when {
            mode == RollMode.ADVANTAGE -> " advantage [${details.dice.joinToString()}]"
            mode == RollMode.DISADVANTAGE -> " disadvantage [${details.dice.joinToString()}]"
            else -> ""
        }
        val needsCommit = !session.unresolvedAttackCommitted
        session.lastAttackDetails = details
        dicePresentation = DicePresentationUi(
            ++dicePresentationId,
            weapon.name,
            20,
            details.dice,
            details.kept,
            details.total,
            attackFormula(details),
            inspirationEligible = character.ruleset != Ruleset.Pf2eRemaster,
        )
        session.lastAttackRoll = "${details.total} · d20 ${details.kept} ${signed(details.calculation.total)}$qualifier"
        session.lastDamageDetails = null
        session.lastDamageRoll = null
        val naturalCritical = character.ruleset != Ruleset.Pf2eRemaster && details.natural >= character.criticalHitThreshold
        session.attackOutcome = if (naturalCritical) AttackOutcome.Critical else AttackOutcome.Pending
        if (needsCommit) {
            if (attackOption == null) {
                session.commitAttack(weapon.id)
            } else {
                if (!consumeDerivedAttackResources(attackOption, session) || !session.commitDerivedAttack(attackOption)) return
            }
            session.unresolvedAttackCommitted = true
        }
        if (details.dice.isNotEmpty()) session.record(TurnEvent.RollRecorded(details.toDiceRoll(weapon.name)))
        if (naturalCritical) {
            recordAttackOutcome(weapon, AttackOutcome.Critical, session, attackOption)
            rollDamage(weapon, session, critical = true)
        }
        lastRollAction = { rollAttack(weapon, session, mode, attackOption) }
    }

    fun recordAttackOutcome(
        weapon: WeaponUi,
        outcome: AttackOutcome,
        session: TurnSession,
        attackOption: ResolvedAttackOptionUi? = null,
    ) {
        if (outcome != AttackOutcome.Pending) session.record(
            TurnEvent.AttackResolved(
                attackOption?.parentWeaponId ?: weapon.id,
                outcome.toRecord(),
                attackOption?.id,
                attackOption?.sourceId,
            )
        )
    }

    fun rollDamage(weapon: WeaponUi, session: TurnSession, critical: Boolean) {
        rollSessionDamage(weapon, session, critical, resolvesAttack = true)
    }

    fun rollDirectDamage(weapon: WeaponUi, session: TurnSession) {
        rollSessionDamage(weapon, session, critical = false, resolvesAttack = false)
    }

    private fun rollSessionDamage(weapon: WeaponUi, session: TurnSession, critical: Boolean, resolvesAttack: Boolean) {
        val details = performDamage(weapon, critical)
        session.lastDamageDetails = details
        if (details.sides > 0) {
            dicePresentation = DicePresentationUi(++dicePresentationId, weapon.name, details.sides, details.dice, null, details.total, damageFormula(details))
        }
        val modifierText = when {
            details.modifier > 0 -> " + ${details.modifier}"
            details.modifier < 0 -> " − ${kotlin.math.abs(details.modifier)}"
            else -> ""
        }
        session.lastDamageRoll = "${details.total} ${details.damageType.lowercase()} · ${details.dice.joinToString(" + ")}$modifierText"
        if (details.dice.isNotEmpty()) session.record(TurnEvent.RollRecorded(details.toDiceRoll(weapon.name)))
        if (resolvesAttack) session.unresolvedAttackCommitted = false
        lastRollAction = {
            if (resolvesAttack) rollDamage(weapon, session, critical)
            else rollDirectDamage(weapon, session)
        }
    }

    private fun AttackRollUi.toDiceRoll(label: String): DiceRoll = DiceRoll(
        request = RollRequest(label, DiceExpression(1, 20, calculation.total), mode),
        dice = dice,
        keptDice = listOf(kept),
        total = total,
    )

    private fun DamageRollUi.toDiceRoll(label: String): DiceRoll = DiceRoll(
        request = RollRequest(label, DiceExpression(dice.size.coerceAtLeast(1), sides.coerceAtLeast(1), modifier)),
        dice = dice,
        keptDice = dice,
        total = total,
    )

    private fun AttackOutcome.toRecord(): AttackOutcomeRecord = when (this) {
        AttackOutcome.Miss -> AttackOutcomeRecord.MISS
        AttackOutcome.Hit -> AttackOutcomeRecord.HIT
        AttackOutcome.Critical -> AttackOutcomeRecord.CRITICAL
        AttackOutcome.Pending -> AttackOutcomeRecord.MISS
    }

    private fun performAttack(character: CharacterUi, weapon: WeaponUi, mode: RollMode, multipleAttackPenalty: Int = 0): AttackRollUi {
        val calculation = attackCalculation(character, weapon, multipleAttackPenalty)
        val effectiveMode = if (character.ruleset == Ruleset.Fifth2014 && character.exhaustionLevel >= 3) {
            when (mode) {
                RollMode.ADVANTAGE -> RollMode.NORMAL
                else -> RollMode.DISADVANTAGE
            }
        } else mode
        val rolled = dice.d20(weapon.name, calculation.total, effectiveMode)
        val kept = rolled.keptDice.single()
        return AttackRollUi(
            dice = rolled.dice,
            kept = kept,
            mode = effectiveMode,
            total = rolled.total,
            natural = kept,
            calculation = calculation,
        )
    }

    private fun performDamage(weapon: WeaponUi, critical: Boolean): DamageRollUi {
        Regex("^\\s*(\\d+)(?:\\s*([+-])\\s*(\\d+))?\\s*$").matchEntire(weapon.damage)?.let { match ->
            val base = match.groupValues[1].toInt()
            val extra = match.groupValues[3].toIntOrNull() ?: 0
            val flat = base + if (match.groupValues[2] == "-") -extra else extra
            return DamageRollUi(
                dice = emptyList(),
                sides = 0,
                modifier = flat,
                total = flat,
                critical = critical,
                damageType = weapon.damageType,
            )
        }
        val parsed = runCatching { DiceNotation.parse(weapon.damage) }.getOrElse { DiceExpression(1, 8) }
        val expression = if (critical) parsed.copy(count = parsed.count * 2) else parsed
        val rolled = dice.roll(RollRequest(weapon.name, expression))
        return DamageRollUi(
            dice = rolled.dice,
            sides = expression.sides,
            modifier = expression.modifier,
            total = rolled.total,
            critical = critical,
            damageType = weapon.damageType,
        )
    }

    private fun attackFormula(roll: AttackRollUi): String = buildString {
        if (roll.dice.size > 1) append("[${roll.dice.joinToString()}] → ")
        append("d20 ${roll.kept} + ${roll.calculation.abilityLabel} ${signed(roll.calculation.ability)}")
        if (roll.calculation.proficiency != 0) append(" + proficiency ${roll.calculation.proficiency}")
        if (roll.calculation.item != 0) append(" ${signed(roll.calculation.item)} item")
        if (roll.calculation.multipleAttackPenalty != 0) append(" ${signed(roll.calculation.multipleAttackPenalty)} multiple attack penalty")
        append(" = ${roll.total}")
    }

    private fun damageFormula(roll: DamageRollUi): String = buildString {
        append(roll.dice.joinToString(" + ").ifBlank { "${roll.modifier}" })
        if (roll.dice.isNotEmpty() && roll.modifier != 0) append(" ${signed(roll.modifier)}")
        append(" = ${roll.total} ${roll.damageType}")
        if (roll.critical) append(" · critical")
    }

    fun availableQuickRolls(character: CharacterUi? = selectedCharacter): List<QuickRollUi> = if (character == null) emptyList() else buildList {
        add(QuickRollUi(QuickRollKind.INITIATIVE, "initiative", t("Initiative", "Initiative")))
        add(QuickRollUi(QuickRollKind.DEATH_SAVE, "death-save", t("Death save", "Todesrettungswurf")))
        character.abilities.keys.forEach { ability -> add(QuickRollUi(QuickRollKind.ABILITY, ability, ability)) }
        character.saves.forEach { (name, _) -> add(QuickRollUi(QuickRollKind.SAVE, name, saveAbbreviation(name))) }
        character.skills.forEach { (name, _) -> add(QuickRollUi(QuickRollKind.SKILL, name, localizedSkillName(name))) }
    }

    fun executeQuickRoll(quickRoll: QuickRollUi) {
        val character = selectedCharacter ?: return
        val exhaustionPenalty = if (character.ruleset == Ruleset.Fifth2024) -2 * character.exhaustionLevel else 0
        fun modeFor(check: Boolean): RollMode =
            if (check && character.ruleset == Ruleset.Fifth2014 && character.exhaustionLevel >= 1) RollMode.DISADVANTAGE
            else if (!check && character.ruleset == Ruleset.Fifth2014 && character.exhaustionLevel >= 3) RollMode.DISADVANTAGE
            else RollMode.NORMAL
        when (quickRoll.kind) {
            QuickRollKind.INITIATIVE -> {
                val exhaustionMode = modeFor(check = true)
                val mode = when {
                    exhaustionMode == RollMode.DISADVANTAGE && character.initiativeRollMode == RollMode.ADVANTAGE -> RollMode.NORMAL
                    exhaustionMode == RollMode.DISADVANTAGE -> RollMode.DISADVANTAGE
                    else -> character.initiativeRollMode
                }
                roll(t("Initiative", "Initiative"), character.initiative + exhaustionPenalty, mode = mode, modifierLabel = "DEX")
            }
            QuickRollKind.DEATH_SAVE -> if (character.isDowned) resolveDeathSave() else Unit
            QuickRollKind.ABILITY -> roll(quickRoll.label, abilityModifier(character.abilities[quickRoll.id] ?: 10) + exhaustionPenalty, mode = modeFor(check = true), modifierLabel = quickRoll.id)
            QuickRollKind.SAVE -> roll(quickRoll.label, (character.saves[quickRoll.id] ?: 0) + exhaustionPenalty, mode = modeFor(check = false), modifierLabel = saveAbbreviation(quickRoll.id))
            QuickRollKind.SKILL -> roll(quickRoll.label, (character.skills[quickRoll.id] ?: 0) + exhaustionPenalty, mode = modeFor(check = true), modifierLabel = quickRoll.label)
            QuickRollKind.ATTACK -> openSheetAttack(quickRoll.id)
        }
    }

    fun updateQuickRolls(quickRolls: List<QuickRollUi>) {
        updateSelectedCharacter {
            it.copy(quickRolls = quickRolls.filterNot { roll -> roll.kind == QuickRollKind.ATTACK }
                .distinctBy { roll -> roll.kind to roll.id }
                .take(12))
        }
    }

    fun addStandardWeapon(template: StandardWeaponTemplate, magicBonus: Int = template.itemBonus) {
        val character = selectedCharacter ?: return
        val resolvedTemplate = template.forRuleset(character.ruleset)
        val abilityBonus = abilityModifier(character.abilities[resolvedTemplate.ability] ?: 10)
        val categoryId = when (resolvedTemplate.classification.training) {
            WeaponTrainingCategory.SIMPLE -> "weapon:simple"
            WeaponTrainingCategory.MARTIAL -> "weapon:martial"
            else -> "weapon:${resolvedTemplate.id}"
        }
        val specificId = "weapon:${resolvedTemplate.id}"
        val proficiencyId = categoryId.takeIf { it in character.proficiencyRanks || it in character.proficiencyIds } ?: specificId
        val rank = proficiencyRank(proficiencyId, character.proficiencyRanks, character.proficiencyIds)
        val proficiency = proficiencyModifier(character.ruleset, character.level, rank)
        val weapon = WeaponUi(
            id = uniqueId(resolvedTemplate.id, character.weapons.map { it.id }),
            name = if (magicBonus > 0) "+$magicBonus ${resolvedTemplate.name}" else resolvedTemplate.name,
            attackBonus = abilityBonus + proficiency + magicBonus,
            damage = withAbilityDamage(resolvedTemplate.damage, abilityBonus + magicBonus),
            damageType = resolvedTemplate.damageType,
            properties = resolvedTemplate.properties,
            ability = resolvedTemplate.ability,
            proficient = rank != null,
            proficiencyId = proficiencyId,
            range = resolvedTemplate.range,
            mastery = resolvedTemplate.mastery,
            itemBonus = magicBonus,
            needsAttunement = resolvedTemplate.needsAttunement,
            custom = resolvedTemplate.custom,
            damageAbility = resolvedTemplate.ability,
            definitionId = resolvedTemplate.id,
            reachFeet = if ("reach" in resolvedTemplate.classification.propertyIds) 10 else 5,
            normalRangeFeet = resolvedTemplate.range.substringBefore('/').filter(Char::isDigit).toIntOrNull(),
            longRangeFeet = resolvedTemplate.range.substringAfter('/', "").filter(Char::isDigit).toIntOrNull(),
            savingThrows = standardWeaponSavingThrows(resolvedTemplate, character.ruleset),
            useCase = resolvedTemplate.useCase.ifBlank { weaponUseCase(resolvedTemplate) },
            classification = resolvedTemplate.classification,
            combatContributions = resolvedTemplate.combatContributions,
        )
        updateSelectedCharacter { it.copy(weapons = it.weapons + weapon) }
    }

    fun addCustomWeapon(weapon: WeaponUi) {
        val character = selectedCharacter ?: return
        val unique = weapon.copy(id = uniqueId(weapon.id.ifBlank { slug(weapon.name) }, character.weapons.map { it.id }), custom = true)
        updateSelectedCharacter { it.copy(weapons = it.weapons + unique) }
    }

    fun addEquipment(item: EquipmentUi) {
        val character = selectedCharacter ?: return
        val existing = character.resolvedEquipment.firstOrNull {
            it.definitionId == item.definitionId && !it.needsAttunement && it.effects.isEmpty() && item.effects.isEmpty()
        }
        val updatedItems = if (existing != null) {
            character.resolvedEquipment.map { if (it.id == existing.id) it.copy(quantity = it.quantity + item.quantity) else it }
        } else {
            character.resolvedEquipment + item.copy(id = uniqueId(item.id.ifBlank { slug(item.name) }, character.resolvedEquipment.map { it.id }))
        }
        updateSelectedCharacter { it.copy(equipmentItems = updatedItems) }
    }

    fun toggleEquipmentWorn(itemId: String) = toggleEquipmentEquipped(itemId)

    fun toggleEquipmentEquipped(itemId: String) {
        updateSelectedCharacter { character ->
            val selected = character.resolvedEquipment.firstOrNull { it.id == itemId } ?: return@updateSelectedCharacter character
            val wear = !selected.worn
            val updatedItems = character.resolvedEquipment.map { item ->
                when {
                    item.id == itemId -> item.copy(worn = wear)
                    wear && selected.kind == EquipmentKind.ARMOR && selected.shieldBonus == 0 &&
                        item.kind == EquipmentKind.ARMOR && item.shieldBonus == 0 -> item.copy(worn = false)
                    else -> item
                }
            }
            val armor = updatedItems.firstOrNull { it.worn && it.armorClass != null }
            val shield = updatedItems.filter { it.worn }.sumOf { it.shieldBonus }
            character.copy(
                equipmentItems = updatedItems,
                baseArmorClass = (armor?.armorClass?.plus(character.passiveArmorClassBonus)
                    ?: character.unarmoredArmorClass) + shield,
            )
        }
    }

    private fun creationWeapon(
        template: StandardWeaponTemplate,
        abilities: Map<String, Int>,
        proficiencyRanks: Map<String, ProficiencyRank>,
        index: Int,
    ): WeaponUi {
        val resolvedTemplate = template.forRuleset(creation.ruleset)
        val categoryId = when (resolvedTemplate.classification.training) {
            WeaponTrainingCategory.SIMPLE -> "weapon:simple"
            WeaponTrainingCategory.MARTIAL -> "weapon:martial"
            else -> "weapon:${resolvedTemplate.id}"
        }
        val specificId = "weapon:${resolvedTemplate.id}"
        val proficiencyId = categoryId.takeIf { it in proficiencyRanks } ?: specificId
        val trained = proficiencyId in proficiencyRanks
        val proficiency = proficiencyModifier(creation.ruleset, creation.level, proficiencyRanks[proficiencyId])
        val abilityBonus = abilityModifier(abilities[resolvedTemplate.ability] ?: 10)
        return WeaponUi(
            id = "starting-${resolvedTemplate.id}-$index",
            definitionId = resolvedTemplate.id,
            name = if (resolvedTemplate.itemBonus > 0) "+${resolvedTemplate.itemBonus} ${resolvedTemplate.name}" else resolvedTemplate.name,
            attackBonus = abilityBonus + proficiency + resolvedTemplate.itemBonus,
            damage = withAbilityDamage(resolvedTemplate.damage, abilityBonus + resolvedTemplate.itemBonus),
            damageType = resolvedTemplate.damageType,
            properties = resolvedTemplate.properties,
            ability = resolvedTemplate.ability,
            proficient = trained,
            proficiencyId = proficiencyId,
            itemBonus = resolvedTemplate.itemBonus,
            range = resolvedTemplate.range,
            mastery = resolvedTemplate.mastery,
            damageAbility = resolvedTemplate.ability,
            reachFeet = if ("reach" in resolvedTemplate.classification.propertyIds) 10 else 5,
            normalRangeFeet = resolvedTemplate.range.substringBefore('/').filter(Char::isDigit).toIntOrNull(),
            longRangeFeet = resolvedTemplate.range.substringAfter('/', "").filter(Char::isDigit).toIntOrNull(),
            savingThrows = standardWeaponSavingThrows(resolvedTemplate, creation.ruleset),
            useCase = resolvedTemplate.useCase.ifBlank { weaponUseCase(resolvedTemplate) },
            classification = resolvedTemplate.classification,
            combatContributions = resolvedTemplate.combatContributions,
        )
    }

    fun toggleEquipmentAttunement(itemId: String) {
        val current = selectedCharacter ?: return
        val selected = current.resolvedEquipment.firstOrNull { it.id == itemId } ?: return
        val attunedCount = current.resolvedEquipment.count { it.attuned } + current.weapons.count { it.attuned }
        if (selected.needsAttunement && !selected.attuned && attunedCount >= 3) {
            inventoryFeedback = t(
                "Attuned. Your character now exceeds the usual limit of 3.",
                "Eingestimmt. Dein Charakter überschreitet jetzt das übliche Limit von 3.",
            )
        }
        updateSelectedCharacter { character ->
            character.copy(
                equipmentItems = character.resolvedEquipment.map { item ->
                    if (item.id == itemId && item.needsAttunement) item.copy(attuned = !item.attuned) else item
                },
            )
        }
    }

    fun toggleWeaponAttunement(weaponId: String) {
        val current = selectedCharacter ?: return
        val selected = current.weapons.firstOrNull { it.id == weaponId } ?: return
        val attunedCount = current.resolvedEquipment.count { it.attuned } + current.weapons.count { it.attuned }
        if (selected.needsAttunement && !selected.attuned && attunedCount >= 3) {
            inventoryFeedback = t(
                "Attuned. Your character now exceeds the usual limit of 3.",
                "Eingestimmt. Dein Charakter überschreitet jetzt das übliche Limit von 3.",
            )
        }
        updateSelectedCharacter { character ->
            character.copy(weapons = character.weapons.map { weapon ->
                if (weapon.id == weaponId && weapon.needsAttunement) weapon.copy(attuned = !weapon.attuned) else weapon
            })
        }
    }

    fun toggleWeaponEquipped(weaponId: String) {
        updateSelectedCharacter { character ->
            character.copy(weapons = character.weapons.map { weapon ->
                if (weapon.id == weaponId) weapon.copy(equipped = !weapon.equipped) else weapon
            })
        }
    }

    fun updateEquipment(item: EquipmentUi) {
        updateSelectedCharacter { character ->
            character.copy(equipmentItems = character.resolvedEquipment.map { current ->
                if (current.id == item.id) item else current
            })
        }
    }

    fun updateWeapon(item: WeaponUi) {
        updateSelectedCharacter { character ->
            character.copy(weapons = character.weapons.map { current -> if (current.id == item.id) item else current })
        }
    }

    fun removeEquipment(itemId: String) {
        val character = selectedCharacter ?: return
        val index = character.resolvedEquipment.indexOfFirst { it.id == itemId }
        if (index < 0) return
        val item = character.resolvedEquipment[index]
        pendingInventoryRemoval = InventoryRemovalUi.Equipment(character.id, index, item)
        updateSelectedCharacter { it.copy(equipmentItems = it.resolvedEquipment.filterNot { entry -> entry.id == itemId }) }
        inventoryFeedback = t("Removed ${item.name}", "${item.name} entfernt")
    }

    fun removeWeapon(weaponId: String) {
        val character = selectedCharacter ?: return
        val index = character.weapons.indexOfFirst { it.id == weaponId }
        if (index < 0) return
        val item = character.weapons[index]
        pendingInventoryRemoval = InventoryRemovalUi.Weapon(character.id, index, item)
        updateSelectedCharacter { it.copy(weapons = it.weapons.filterNot { entry -> entry.id == weaponId }) }
        inventoryFeedback = t("Removed ${item.name}", "${item.name} entfernt")
    }

    fun undoInventoryRemoval() {
        val removal = pendingInventoryRemoval ?: return
        val index = characters.indexOfFirst { it.id == removal.characterId }
        if (index < 0) return
        val character = characters[index]
        val restored = when (removal) {
            is InventoryRemovalUi.Equipment -> character.copy(
                equipmentItems = character.resolvedEquipment.toMutableList().apply {
                    add(removal.index.coerceIn(0, size), removal.item)
                },
            )
            is InventoryRemovalUi.Weapon -> character.copy(
                weapons = character.weapons.toMutableList().apply {
                    add(removal.index.coerceIn(0, size), removal.item)
                },
            )
        }
        characters[index] = CharacterStatEngine.resolve(
            restored,
            conditions.filter { it.characterId.isBlank() || it.characterId == restored.id },
        )
        pendingInventoryRemoval = null
        inventoryFeedback = null
        persist()
    }

    fun clearInventoryFeedback() {
        inventoryFeedback = null
        pendingInventoryRemoval = null
    }

    fun updateLanguages(values: List<String>) {
        updateSelectedCharacter { character ->
            val normalized = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            character.copy(languages = (character.lockedLanguages + normalized).distinct())
        }
    }

    fun addNote(title: String, body: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        updateSelectedCharacter { character ->
            val note = CharacterNote(
                id = uniqueId("note-${slug(cleanTitle)}", character.notes.map { it.id }),
                title = cleanTitle,
                body = body.trim(),
            )
            character.copy(notes = character.notes + note)
        }
    }

    fun updateNote(noteId: String, title: String, body: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        updateSelectedCharacter { character ->
            character.copy(notes = character.notes.map { note ->
                if (note.id == noteId) note.copy(title = cleanTitle, body = body.trim()) else note
            })
        }
    }

    fun removeNote(noteId: String) {
        updateSelectedCharacter { character -> character.copy(notes = character.notes.filterNot { it.id == noteId }) }
    }

    fun addCustomFeature(name: String, summary: String, combatContributions: List<CombatContribution> = emptyList()) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        updateSelectedCharacter { character ->
            val id = uniqueId(slug(cleanName), character.features.map { it.id })
            character.copy(features = character.features + FeatureUi(id, cleanName, summary.trim(), custom = true, combatContributions = combatContributions))
        }
    }

    fun updateCustomFeature(
        featureId: String,
        name: String,
        summary: String,
        notes: String = "",
        combatContributions: List<CombatContribution>? = null,
    ) {
        updateSelectedCharacter { character ->
            character.copy(features = character.features.map { feature ->
                if (feature.id == featureId && feature.custom) feature.copy(
                    name = name.trim().ifBlank { feature.name },
                    summary = summary.trim(),
                    notes = notes.trim(),
                    combatContributions = combatContributions ?: feature.combatContributions,
                ) else feature
            })
        }
    }

    fun removeCustomFeature(featureId: String) {
        updateSelectedCharacter { character ->
            character.copy(features = character.features.filterNot { it.id == featureId && it.custom })
        }
    }

    fun addPrivateEntry(entry: PrivateEntryUi) {
        privateEntries += entry.copy(id = uniqueId(entry.id.ifBlank { slug(entry.name) }, privateEntries.map { it.id }))
        persist()
    }

    fun registerPendingImport(imported: PendingImportUi) {
        pendingImports.removeAll { it.packId == imported.packId }
        pendingImports += imported
        privateContentOpen = true
        persist()
    }

    fun missingPrivateRequirements(packId: String): List<PrivateContentRequirementUi> {
        val pending = pendingImports.firstOrNull { it.packId == packId } ?: return emptyList()
        return pending.requires.filter { requirement ->
            installedPrivatePacks.none { it.id == requirement.id && it.version == requirement.version }
        }
    }

    private fun PrivateEntryUi.toPrivateFeature(): FeatureUi {
        val resource = mechanics.resource
        return FeatureUi(
            id = "private-$id",
            name = name,
            summary = privateContentSummary(),
            remaining = resource?.maximum,
            maximum = resource?.maximum,
            recovery = resource?.recovery ?: Recovery.MANUAL,
            actionCost = mechanics.actionCost,
            custom = true,
            notes = sourceNote,
            turnGuideEligible = resource != null || mechanics.actionCost != ActionCost(),
            attackGrants = attackGrants,
            combatContributions = combatContributions,
        )
    }

    private fun automaticPrivateFeatures(
        className: String,
        ancestry: String,
        subclassId: String?,
        level: Int,
    ): List<FeatureUi> {
        val classId = privateEntryForName("class", className)?.id
        val speciesId = privateEntryForName("ancestry", ancestry)?.id
        return privateEntries.filter { entry ->
            entry.normalizedKind() == "feature" &&
                entry.mechanics.grantAutomatically &&
                (entry.mechanics.unlockLevel ?: 1) <= level &&
                (
                    (classId != null && entry.mechanics.parentClassId == classId) ||
                        (speciesId != null && entry.mechanics.parentSpeciesId == speciesId) ||
                        (subclassId != null && entry.mechanics.parentSubclassId == subclassId)
                    )
        }.map { it.toPrivateFeature() }
    }

    private fun privateContentVersionsUsedBy(
        className: String,
        ancestry: String,
        subclassId: String?,
        selectedEntryIds: Collection<String>,
    ): List<InstalledPrivatePackUi> {
        val packIds = buildSet {
            privateEntryForName("class", className)?.sourcePackId?.let(::add)
            privateEntryForName("ancestry", ancestry)?.sourcePackId?.let(::add)
            subclassId?.let(::privateEntryById)?.sourcePackId?.let(::add)
            selectedEntryIds.mapNotNull(::privateEntryById).mapNotNullTo(this) { it.sourcePackId }
        }
        return installedPrivatePacks.filter { it.id in packIds }
    }

    fun approvePendingImport(packId: String): Boolean {
        val pending = pendingImports.firstOrNull { it.packId == packId } ?: return false
        if (pending.error != null || pending.candidates.isEmpty() || missingPrivateRequirements(packId).isNotEmpty()) return false
        privateEntries.removeAll { it.sourcePackId == packId }
        privateEntries += pending.candidates
        installedPrivatePacks.removeAll { it.id == packId }
        installedPrivatePacks += InstalledPrivatePackUi(packId, pending.version, pending.requires)
        pendingImports.remove(pending)
        persist()
        return true
    }

    fun discardPendingImport(packId: String) {
        pendingImports.removeAll { it.packId == packId }
        persist()
    }

    fun suggestedTurnSteps(
        character: CharacterUi? = selectedCharacter,
        session: TurnSession? = turnSession,
    ): List<SuggestedTurnStepUi> = if (character == null) emptyList() else SuggestedTurnPlanner.build(character, session)

    private fun saveAbbreviation(name: String): String = when {
        name.startsWith("Str", true) -> "STR"
        name.startsWith("Dex", true) -> "DEX"
        name.startsWith("Con", true) -> "CON"
        name.startsWith("Int", true) -> "INT"
        name.startsWith("Wis", true) -> "WIS"
        name.startsWith("Cha", true) -> "CHA"
        else -> name.take(3).uppercase()
    }

    private fun withAbilityDamage(formula: String, modifier: Int): String = when {
        modifier > 0 -> "$formula + $modifier"
        modifier < 0 -> "$formula - ${kotlin.math.abs(modifier)}"
        else -> formula
    }

    internal fun weaponUseCase(template: StandardWeaponTemplate): String = buildList {
        val properties = template.properties.lowercase()
        if ("reach" in properties) add(t("Controls space from 10 feet away.", "Kontrolliert Raum aus 10 Fuß Entfernung."))
        if ("finesse" in properties) add(t("Can use Dexterity for attack and damage.", "Kann Geschicklichkeit für Angriff und Schaden verwenden."))
        if ("light" in properties) add(t("Works well for two-weapon fighting.", "Eignet sich gut für den Kampf mit zwei Waffen."))
        if ("thrown" in properties) add(t("Can be used in melee or thrown.", "Kann im Nahkampf oder geworfen verwendet werden."))
        if ("ammunition" in properties) add(t("Ranged weapon; track its ammunition.", "Fernkampfwaffe; Munition mitführen."))
        if (template.mastery.isNotBlank()) add(t("Mastery: ${template.mastery}.", "Meisterschaft: ${template.mastery}."))
    }.joinToString(" ").ifBlank { t("A straightforward weapon for weapon attacks.", "Eine unkomplizierte Waffe für Waffenangriffe.") }

    private fun slug(value: String): String = value.lowercase().map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("").replace(Regex("-+"), "-").trim('-').ifBlank { "entry" }

    private fun uniqueId(base: String, existing: List<String>): String {
        if (base !in existing) return base
        var suffix = 2
        while ("$base-$suffix" in existing) suffix++
        return "$base-$suffix"
    }

    fun toggleInspiration(): Boolean {
        val character = selectedCharacter ?: return false
        if (character.ruleset == Ruleset.Pf2eRemaster) return false
        val active = conditions.firstOrNull { it.characterId == character.id && it.isInspiration() }
        if (active != null) removeCondition(active) else addCondition("Inspiration")
        return true
    }

    private fun consumeInspiration(): Boolean {
        val characterId = selectedCharacterId ?: return false
        val active = conditions.firstOrNull { it.characterId == characterId && it.isInspiration() } ?: return false
        removeCondition(active)
        return true
    }

    fun addCondition(name: String) {
        val inspiration = name.equals("Inspiration", ignoreCase = true)
        if (inspiration && (selectedCharacter == null || selectedCharacter?.ruleset == Ruleset.Pf2eRemaster)) return
        if (name == "Exhaustion") {
            setExhaustion((selectedCharacter?.exhaustionLevel ?: 0).coerceAtLeast(1))
            conditionsOpen = false
            return
        }
        val characterId = selectedCharacterId.orEmpty()
        if (conditions.any { it.characterId == characterId && (it.name == name || inspiration && it.isInspiration()) }) return
        val explanation = conditionExplanation(name)
        val condition = ConditionUi(
            name = name,
            source = t("DM / table", "SL / Spieltisch"),
            duration = if (inspiration) {
                t("Until used or removed", "Bis verwendet oder entfernt")
            } else {
                t("Until removed", "Bis entfernt")
            },
            explanation = explanation,
            characterId = characterId,
            id = "condition-$characterId-${slug(name)}",
        )
        conditions += condition
        updateSelectedCharacter { it }
        recordEvent(TurnEvent.ConditionApplied(condition.id), condition.name)
        conditionsOpen = false
    }

    fun removeCondition(condition: ConditionUi) {
        if (!condition.removable) return
        if (condition.name == "Exhaustion") {
            setExhaustion(0)
            return
        }
        conditions.remove(condition)
        updateSelectedCharacter { it }
        recordEvent(TurnEvent.ConditionRemoved(condition.id), condition.name)
    }

    fun setExhaustion(level: Int) {
        val character = selectedCharacter ?: return
        val maximum = if (character.ruleset == Ruleset.Pf2eRemaster) 0 else 6
        val safeLevel = level.coerceIn(0, maximum)
        val previousLevel = character.exhaustionLevel
        conditions.removeAll { it.characterId == character.id && it.name == "Exhaustion" }
        if (safeLevel > 0) {
            conditions += ConditionUi(
                name = "Exhaustion",
                source = t("DM / table", "SL / Spieltisch"),
                duration = t("Level $safeLevel", "Stufe $safeLevel"),
                explanation = exhaustionExplanation(character.ruleset, safeLevel),
                characterId = character.id,
                id = "condition-${character.id}-exhaustion",
                level = safeLevel,
            )
        }
        updateSelectedCharacter { current ->
            val exhaustionDeath = safeLevel >= 6
            val reducedMaximum = (current.maxHp - current.maxHpReduction).coerceAtLeast(0)
            val effectiveMaximum = if (current.ruleset == Ruleset.Fifth2014 && safeLevel >= 4) {
                reducedMaximum / 2
            } else {
                reducedMaximum
            }
            val maximumDeath = current.ruleset != Ruleset.Pf2eRemaster && effectiveMaximum == 0
            current.copy(
                exhaustionLevel = safeLevel,
                hp = current.hp.coerceAtMost(effectiveMaximum),
                isDead = when {
                    exhaustionDeath || maximumDeath -> true
                    current.deathReason == "Exhaustion" -> false
                    else -> current.isDead
                },
                deathReason = when {
                    exhaustionDeath -> "Exhaustion"
                    maximumDeath -> "Maximum Hit Points are zero"
                    else -> current.deathReason.takeUnless { it == "Exhaustion" }
                },
            )
        }
        if (previousLevel == 0 && safeLevel > 0) {
            recordEvent(TurnEvent.ConditionApplied("condition-${character.id}-exhaustion"), "Exhaustion")
        } else if (previousLevel > 0 && safeLevel == 0) {
            recordEvent(TurnEvent.ConditionRemoved("condition-${character.id}-exhaustion"), "Exhaustion")
        }
        persist()
    }

    private fun exhaustionExplanation(ruleset: Ruleset, level: Int): String = when (ruleset) {
        Ruleset.Fifth2024 -> t(
            "Level $level: ${-2 * level} to D20 Tests and ${-5 * level} ft Speed. Level 6 is death.",
            "Stufe $level: ${-2 * level} auf W20-Proben und ${-5 * level} ft Bewegungsrate. Stufe 6 bedeutet Tod.",
        )
        Ruleset.Fifth2014 -> t(
            "Level $level applies the cumulative 2014 Exhaustion effects.",
            "Stufe $level wendet die kumulativen Erschöpfungseffekte von 2014 an.",
        )
        Ruleset.Pf2eRemaster -> t("PF2e uses its own valued conditions.", "PF2e verwendet eigene Zustände mit Stufenwerten.")
    }

    internal fun showInfo(title: String, body: String, costs: List<CostTokenUi> = emptyList()) {
        infoTitle = title
        infoBody = body
        infoCosts = costs
    }

    fun convert(target: Ruleset) {
        val source = selectedCharacter ?: return
        val convertedCurrency = if (target == Ruleset.Pf2eRemaster && source.ruleset != Ruleset.Pf2eRemaster) {
            val convertedSilver = source.currency.silver.toLong() + source.currency.electrum.toLong() * 5L
            if (convertedSilver > Int.MAX_VALUE) {
                showInfo(
                    t("Currency is too large to convert", "Währungsbetrag ist zu groß zum Konvertieren"),
                    t("Reduce the electrum or silver balance before converting this character.", "Verringere vor der Konvertierung den Elektrum- oder Silberbetrag."),
                )
                return
            }
            source.currency.copy(electrum = 0, silver = convertedSilver.toInt())
        } else source.currency
        val convertedMaximum = if (target == Ruleset.Pf2eRemaster) source.level * 10 + 8 else source.maxHp
        val convertedReduction = source.maxHpReduction.coerceAtMost(convertedMaximum)
        val convertedHealth = source.copy(
            ruleset = target,
            maxHp = convertedMaximum,
            maxHpReduction = convertedReduction,
        )
        val convertedEffectiveMaximum = convertedHealth.effectiveMaxHp
        val maximumDeath = target != Ruleset.Pf2eRemaster && convertedEffectiveMaximum == 0
        val copy = source.copy(
            id = "conversion-${characters.size + 1}-${Random.nextInt(10_000)}",
            name = "${source.name} · ${target.shortLabel}",
            ruleset = target,
            sourceCharacterId = source.id,
            hp = if (target == Ruleset.Pf2eRemaster) convertedEffectiveMaximum else source.hp.coerceAtMost(convertedEffectiveMaximum),
            maxHp = convertedMaximum,
            maxHpReduction = convertedReduction,
            isDead = maximumDeath || source.isDead,
            deathReason = if (maximumDeath) "Maximum Hit Points are zero" else source.deathReason,
            armorClass = if (target == Ruleset.Pf2eRemaster) 10 + source.level + 4 else source.armorClass,
            currency = convertedCurrency,
            activePlaySession = null,
            savedPlaySessions = emptyList(),
            notes = source.notes + CharacterNote(
                id = uniqueId("ruleset-conversion", source.notes.map { it.id }),
                title = t("Ruleset conversion", "Regelwerk-Konvertierung"),
                body = t(
                    "Guided conversion from ${source.ruleset.shortLabel}; review required choices.",
                    "Geführte Konvertierung von ${source.ruleset.shortLabel}; erforderliche Wahlen prüfen.",
                ),
            ),
        )
        characters += withCompleteSkills(copy)
        persist()
        selectedCharacterId = copy.id
        conversionOpen = false
    }

    fun search(query: String): List<SearchResultUi> {
        val character = selectedCharacter ?: return emptyList()
        val entries = buildList {
            if (character.level < 20) {
                add(
                    SearchResultUi(
                        "level-up",
                        t("Level up", "Stufenaufstieg"),
                        t("Advance this character to level ${character.level + 1}", "Diesen Charakter auf Stufe ${character.level + 1} steigern"),
                        SearchResultKind.Action,
                        t("Open", "Öffnen"),
                    ),
                )
            }
            add(SearchResultUi("initiative", t("Roll initiative", "Initiative würfeln"), t("Dexterity ${signed(character.initiative)} · one tap", "Geschicklichkeit ${signed(character.initiative)} · ein Tipp"), SearchResultKind.Roll, t("Roll", "Würfeln"), character.initiative))
            character.saves.forEach { (name, modifier) ->
                add(SearchResultUi("save-$name", t("$name saving throw", "$name-Rettungswurf"), t("Saving throw ${signed(modifier)}", "Rettungswurf ${signed(modifier)}"), SearchResultKind.Roll, t("Roll", "Würfeln"), modifier))
            }
            character.skills.forEach { (name, modifier) ->
                add(SearchResultUi("skill-$name", localizedSkillName(name), t("Skill check ${signed(modifier)}", "Fertigkeitswurf ${signed(modifier)}"), SearchResultKind.Roll, t("Roll", "Würfeln"), modifier))
            }
            character.weapons.forEach { weapon ->
                add(SearchResultUi("weapon-${weapon.id}", weapon.name, t("Attack ${signed(weapon.attackBonus)} · ${weapon.damage}", "Angriff ${signed(weapon.attackBonus)} · ${weapon.damage}"), SearchResultKind.Action, t("Attack", "Angriff"), cost = ActionCost(actions = 1, attacks = 1)))
            }
            character.availableSpells.forEach { spell ->
                add(SearchResultUi("spell-${spell.id}", spell.name, spell.summary, SearchResultKind.Action, t("Use", "Nutzen"), cost = spell.activationCost))
            }
            character.features.forEach { feature ->
                add(SearchResultUi("feature-${feature.id}", feature.name, feature.summary, SearchResultKind.Rule, t("Info", "Info"), cost = feature.actionCost, resourceLabel = feature.sharedResourceCostLabel()))
            }
            character.subclassIdsByClass.forEach { (className, subclassId) ->
                val option = subclassOptions(character.ruleset, className).firstOrNull { it.id == subclassId }
                val name = character.subclassNamesByClass[className] ?: option?.name ?: return@forEach
                add(
                    SearchResultUi(
                        "subclass-$subclassId",
                        name,
                        option?.summary(language) ?: t("Subclass for $className", "Unterklasse f\u00fcr $className"),
                        SearchResultKind.Rule,
                        t("Info", "Info"),
                    )
                )
            }
            character.abilities.forEach { (ability, score) ->
                add(SearchResultUi("ability-$ability", "$ability $score", t("Ability score · tap to roll", "Attributswert · tippen zum Würfeln"), SearchResultKind.Roll, t("Roll", "Würfeln"), abilityModifier(score)))
            }
            add(SearchResultUi("stat-ac", t("Armor Class", "Rüstungsklasse"), character.armorClass.toString(), SearchResultKind.Navigate, t("Info", "Info")))
            add(SearchResultUi("stat-hp", t("Hit points", "Trefferpunkte"), "${character.hp}/${character.effectiveMaxHp}", SearchResultKind.Navigate, t("Info", "Info")))
            coinDenominations(character.ruleset).forEach { denomination ->
                add(
                    SearchResultUi(
                        id = "currency-${denomination.name.lowercase()}",
                        title = "${denomination.localizedName(language)} (${denomination.shortCode})",
                        subtitle = t(
                            "Currency: ${character.currency.balance(denomination)} ${denomination.shortCode}",
                            "Währung: ${character.currency.balance(denomination)} ${denomination.shortCode}",
                        ),
                        kind = SearchResultKind.Currency,
                        actionLabel = t("Edit", "Bearbeiten"),
                        searchTerms = denomination.searchTerms,
                        denomination = denomination,
                    ),
                )
            }
            addAll(knowledgeEntries())
            addAll(srdWikiEntries(character.ruleset))
            addAll(srdSpellWikiEntries(character.ruleset))
            addAll(builtInWikiItemEntries(character.ruleset))
            addAll(localWikiEntries(character.ruleset))
            character.notes.forEach { note ->
                add(SearchResultUi("note-${note.id}", note.title, note.body, SearchResultKind.Note, t("Open", "Öffnen")))
            }
        }
        val needle = query.trim()
        if (needle.isEmpty()) {
            val suggestedRuleIds = when {
                character.subclass.contains("Battle Master", true) -> listOf("rule-maneuvers", "rule-superiority-dice", "rule-conditions")
                character.isSorcerer -> listOf("rule-spell-slots", "rule-sorcery-points", "rule-metamagic")
                character.className.equals("Wizard", true) -> listOf("rule-spell-slots", "rule-concentration", "rule-conditions")
                else -> listOf("rule-conditions", "rule-advantage", "rule-rests")
            }
            val suggestedRules = suggestedRuleIds.mapNotNull { id -> entries.firstOrNull { it.id == id } }
            val quickActions = entries.filter { it.kind != SearchResultKind.Rule }.take(3)
            return (suggestedRules + quickActions).distinctBy { it.id }.take(6)
        }
        val normalizedNeedle = normalizeWikiSearch(needle)
        return entries.mapNotNull { result -> wikiSearchRank(result, normalizedNeedle)?.let { rank -> rank to result } }
            .sortedWith(compareBy<Pair<Int, SearchResultUi>> { it.first }.thenBy { normalizeWikiSearch(it.second.title) }.thenBy { it.second.id })
            .map { it.second }
    }

    private fun srdWikiEntries(ruleset: Ruleset): List<SearchResultUi> {
        val revision = when (ruleset) {
            Ruleset.Fifth2014 -> SrdWikiRevision.SRD_5_1
            Ruleset.Fifth2024 -> SrdWikiRevision.SRD_5_2_1
            Ruleset.Pf2eRemaster -> return emptyList()
        }
        return SrdWikiCatalog.forRevision(revision).map { entry ->
            val text = if (language == UiLanguage.German) entry.de else entry.en
            val category = when (entry.kind) {
                SrdWikiKind.ACTION -> t("Action", "Aktion")
                SrdWikiKind.ANCESTRY -> if (ruleset == Ruleset.Fifth2014) t("Race", "Volk") else t("Species", "Spezies")
                SrdWikiKind.BACKGROUND -> t("Background", "Hintergrund")
                SrdWikiKind.CLASS -> t("Class", "Klasse")
                SrdWikiKind.CONDITION -> t("Condition", "Zustand")
                SrdWikiKind.CREATURE -> t("Creature", "Kreatur")
                SrdWikiKind.FEAT -> t("Feat", "Talent")
                SrdWikiKind.KNOWLEDGE -> t("Magic school", "Magieschule")
                SrdWikiKind.SUBCLASS -> t("Subclass", "Unterklasse")
            }
            val metadata = if (entry.kind == SrdWikiKind.CREATURE) {
                val parts = entry.metadata.split('|')
                if (parts.size == 2) t("${parts[0]} ${parts[1]}", "${localizedCreatureSize(parts[0])} · ${localizedCreatureType(parts[1])}") else entry.metadata
            } else entry.metadata
            val details = buildList {
                add(text.summary)
                add(text.beginnerTip)
                if (metadata.isNotBlank()) add(metadata)
                add(t("Source: ${entry.source} · ${entry.locator}", "Quelle: ${entry.source} · ${entry.locator}"))
            }.joinToString("\n\n")
            SearchResultUi(
                id = "wiki-${revision.name.lowercase()}-${entry.id}",
                title = text.name,
                subtitle = "$category · ${text.summary}",
                kind = SearchResultKind.Rule,
                actionLabel = t("Info", "Info"),
                searchTerms = listOf(entry.en.name, entry.de.name, entry.en.summary, entry.de.summary, entry.metadata) + entry.aliases + entry.keywords,
                detailBody = details,
            )
        }
    }

    private fun srdSpellWikiEntries(ruleset: Ruleset): List<SearchResultUi> {
        val revision = when (ruleset) {
            Ruleset.Fifth2014 -> SrdSpellRevision.SRD_5_1
            Ruleset.Fifth2024 -> SrdSpellRevision.SRD_5_2_1
            Ruleset.Pf2eRemaster -> return emptyList()
        }
        val source = if (revision == SrdSpellRevision.SRD_5_1) "SRD 5.1" else "SRD 5.2.1"
        return SrdSpellCatalog.entries.filter { it.revision == revision }.map { entry ->
            val text = if (language == UiLanguage.German) entry.de else entry.en
            val classNames = entry.classes.sortedBy { it.name }.joinToString(", ") { spellClassName(it) }
            val flags = buildList {
                if (entry.concentration) add(t("Concentration", "Konzentration"))
                if (entry.ritual) add("Ritual")
                if (entry.specificMaterial) add(t("Specific material", "Besondere Materialkomponente"))
            }
            val spellMetadata = buildList {
                add(if (entry.level == 0) t("Cantrip", "Zaubertrick") else t("Level ${entry.level}", "Grad ${entry.level}"))
                add(localizedSpellSchool(entry.school))
                add(classNames)
                addAll(flags)
            }.joinToString(" · ")
            val details = listOf(
                text.summary,
                spellMetadata,
                t("Source: $source · $classNames spell lists; Spell Descriptions: ${entry.en.name}", "Quelle: $source · Zauberlisten $classNames; Zauberbeschreibungen: ${entry.en.name}"),
            ).joinToString("\n\n")
            SearchResultUi(
                id = "wiki-spell-${revision.name.lowercase()}-${entry.id}",
                title = text.name,
                subtitle = t("Spell · $spellMetadata", "Zauber · $spellMetadata"),
                kind = SearchResultKind.Rule,
                actionLabel = t("Info", "Info"),
                searchTerms = listOf(entry.en.name, entry.de.name, entry.en.summary, entry.de.summary, entry.school, localizedSpellSchool(entry.school), classNames) + entry.aliases,
                detailBody = details,
            )
        }
    }

    private fun builtInWikiItemEntries(ruleset: Ruleset): List<SearchResultUi> = builtInKnownItemCatalog()
        .filter { it.compatibleWith(ruleset) && it.type != KnownItemType.Currency }
        .map { item ->
            val summary = item.details.ifBlank {
                t("A ${item.type.name.lowercase()} in the built-in equipment catalog.", "Ein Eintrag der Kategorie ${item.type.name} im integrierten Ausrüstungskatalog.")
            }
            SearchResultUi(
                id = "wiki-item-${item.id.replace(':', '-')}",
                title = item.name,
                subtitle = t("Item · ${item.type.name} · $summary", "Gegenstand · ${item.type.name} · $summary"),
                kind = SearchResultKind.Rule,
                actionLabel = t("Info", "Info"),
                searchTerms = listOf(item.name, item.type.name, item.rarity.name, item.details, "equipment", "gear", "Ausrüstung", "Gegenstand"),
                detailBody = "$summary\n\n" + t("Source: built-in SRD equipment catalog", "Quelle: integrierter SRD-Ausrüstungskatalog"),
            )
        }

    private fun localWikiEntries(ruleset: Ruleset): List<SearchResultUi> = privateEntries
        .filter { it.appliesTo(ruleset) }
        .map { entry ->
            val category = entry.normalizedKind().replaceFirstChar { it.titlecase() }
            val summary = if (
                entry.normalizedKind() in setOf("feature", "feat", "resource")
            ) entry.privateContentSummary() else entry.summary.ifBlank {
                t("Private local Wiki entry.", "Privater lokaler Wiki-Eintrag.")
            }
            val details = buildList {
                add(summary)
                if (entry.formula.isNotBlank()) add("Details: ${entry.formula}")
                add(t("Source: ${entry.sourceNote} · stored only on this device", "Quelle: ${entry.sourceNote} · nur auf diesem Gerät gespeichert"))
            }.joinToString("\n\n")
            SearchResultUi(
                id = "local-wiki-${entry.id}",
                title = entry.name,
                subtitle = t("Local · $category · $summary", "Lokal · $category · $summary"),
                kind = SearchResultKind.Rule,
                actionLabel = t("Info", "Info"),
                searchTerms = listOf(entry.name, entry.kind, category, summary, entry.summary, entry.formula, entry.sourceNote, "local", "private", "lokal", "privat"),
                detailBody = details,
            )
        }

    private fun wikiSearchRank(result: SearchResultUi, needle: String): Int? {
        val title = normalizeWikiSearch(result.title)
        val fields = (listOf(result.title, result.subtitle, result.detailBody) + result.searchTerms)
            .map(::normalizeWikiSearch)
            .filter(String::isNotBlank)
        val combined = fields.joinToString(" ")
        val words = needle.split(' ').filter(String::isNotBlank)
        if (!words.all(combined::contains)) return null
        return when {
            title == needle && result.id.startsWith("rule-") -> 0
            title == needle -> 1
            fields.any { it == needle } -> 2
            title.startsWith(needle) -> 3
            fields.any { field -> field.split(' ').any { it.startsWith(needle) } } -> 4
            combined.contains(needle) -> 5
            else -> 6
        }
    }

    private fun normalizeWikiSearch(value: String): String = value.lowercase()
        .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
        .replace("é", "e").replace("è", "e").replace("á", "a").replace("à", "a")
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun spellClassName(spellClass: SrdSpellClass): String = when (spellClass) {
        SrdSpellClass.BARD -> t("Bard", "Barde")
        SrdSpellClass.CLERIC -> t("Cleric", "Kleriker")
        SrdSpellClass.DRUID -> t("Druid", "Druide")
        SrdSpellClass.PALADIN -> "Paladin"
        SrdSpellClass.RANGER -> t("Ranger", "Waldläufer")
        SrdSpellClass.SORCERER -> t("Sorcerer", "Zauberer")
        SrdSpellClass.WARLOCK -> t("Warlock", "Hexenmeister")
        SrdSpellClass.WIZARD -> t("Wizard", "Magier")
    }

    private fun localizedSpellSchool(school: String): String = if (language == UiLanguage.English) school else when (school) {
        "Abjuration" -> "Bannmagie"
        "Conjuration" -> "Beschwörung"
        "Divination" -> "Erkenntnismagie"
        "Enchantment" -> "Verzauberung"
        "Evocation" -> "Hervorrufung"
        "Illusion" -> "Illusion"
        "Necromancy" -> "Nekromantie"
        "Transmutation" -> "Verwandlung"
        else -> school
    }

    private fun localizedCreatureSize(size: String): String = when (size) {
        "Tiny" -> "Winzig"
        "Small" -> "Klein"
        "Medium" -> "Mittelgroß"
        "Large" -> "Groß"
        "Huge" -> "Riesig"
        "Gargantuan" -> "Gigantisch"
        else -> size
    }

    private fun localizedCreatureType(type: String): String = when (type) {
        "Aberration" -> "Aberration"
        "Beast" -> "Tier"
        "Celestial" -> "Celestisches Wesen"
        "Construct" -> "Konstrukt"
        "Dragon" -> "Drache"
        "Elemental" -> "Elementarwesen"
        "Fey" -> "Fee"
        "Fiend" -> "Unhold"
        "Giant" -> "Riese"
        "Humanoid" -> "Humanoider"
        "Monstrosity" -> "Monstrosität"
        "Ooze" -> "Schlick"
        "Plant" -> "Pflanze"
        "Undead" -> "Untoter"
        else -> type
    }

    fun handleSearchResult(result: SearchResultUi) {
        when (result.kind) {
            SearchResultKind.Roll -> if (result.id == "initiative") {
                executeQuickRoll(QuickRollUi(QuickRollKind.INITIATIVE, "initiative", result.title))
            } else {
                roll(result.title, result.modifier, modifierLabel = result.title)
            }
            SearchResultKind.Action -> when {
                result.id == "level-up" -> beginLevelUp()
                result.id.startsWith("weapon-") -> openSheetAttack(result.id.removePrefix("weapon-"))
                else -> openTurn(TurnSection.Spell)
            }
            SearchResultKind.Rule -> showInfo(
                result.title,
                result.detailBody.ifBlank { result.subtitle } + "\n\n" + t("Your current ruleset: ${selectedCharacter?.ruleset?.longLabel}.", "Dein aktuelles Regelwerk: ${selectedCharacter?.ruleset?.longLabel}."),
                result.cost.toCostTokens() + result.resourceLabel?.let { listOf(CostTokenUi(CostTokenKind.Resource, labelOverride = it)) }.orEmpty(),
            )
            SearchResultKind.Note, SearchResultKind.Navigate -> showInfo(result.title, result.subtitle)
            SearchResultKind.Currency -> result.denomination?.let(::openCurrencyAdjustment)
        }
        searchOpen = false
    }

    private fun conditionExplanation(name: String): String = conditionInfo(name) ?: t(
        "This effect can change what your character can do.",
        "Dieser Effekt kann die Möglichkeiten deines Charakters ändern.",
    )

    internal fun conditionInfo(name: String, level: Int = 1): String? {
        if (name.equals("Custom effect", ignoreCase = true)) return null
        return when (selectedCharacter?.ruleset ?: Ruleset.Fifth2024) {
            Ruleset.Pf2eRemaster -> pf2eConditionInfo(name)
            Ruleset.Fifth2014, Ruleset.Fifth2024 -> fiveEConditionInfo(name, level)
        }
    }

    private fun fiveEConditionInfo(name: String, level: Int): String? = when (name) {
        "Inspiration" -> t(
            "After an eligible d20 roll, use Inspiration to roll it again. The new result replaces the previous result.",
            "Verwende Inspiration nach einem passenden W20-Wurf, um ihn zu wiederholen. Das neue Ergebnis ersetzt das vorherige.",
        )
        "Blinded" -> t("You cannot see. Your attacks have disadvantage, and attacks against you have advantage.", "Du kannst nicht sehen. Deine Angriffe haben Nachteil; Angriffe gegen dich haben Vorteil.")
        "Charmed" -> t("You cannot attack the charmer, and the charmer has an edge in social interaction with you.", "Du kannst den Bezaubernden nicht angreifen; er hat Vorteile bei sozialen Interaktionen mit dir.")
        "Deafened" -> t("You cannot hear and automatically fail checks that require hearing.", "Du kannst nicht hören und scheiterst automatisch an Würfen, die Gehör erfordern.")
        "Exhaustion" -> selectedCharacter?.let { exhaustionExplanation(it.ruleset, level.coerceAtLeast(1)) }
            ?: t("Exhaustion has cumulative levels.", "Erschöpfung hat kumulative Stufen.")
        "Frightened" -> t("You have disadvantage on checks and attacks while the source is visible, and cannot willingly move closer.", "Du hast Nachteil bei Würfen, solange die Quelle sichtbar ist, und kannst dich ihr nicht freiwillig nähern.")
        "Grappled" -> t("Your speed becomes 0 until the grapple ends.", "Deine Bewegungsrate wird 0, bis der Griff endet.")
        "Incapacitated" -> t("You cannot take actions, Bonus Actions, or Reactions.", "Du kannst keine Aktionen, Bonusaktionen oder Reaktionen ausführen.")
        "Invisible" -> t("You cannot be seen without special senses; your attacks gain advantage and attacks against you have disadvantage when your position is known.", "Ohne besondere Sinne bist du unsichtbar; deine Angriffe haben Vorteil und Angriffe gegen dich Nachteil, wenn deine Position bekannt ist.")
        "Paralyzed" -> t("You are incapacitated and cannot move or speak. Nearby hits can become critical hits.", "Du bist handlungsunfähig und kannst dich weder bewegen noch sprechen. Nahe Treffer können kritische Treffer werden.")
        "Petrified" -> t("You are transformed and incapacitated, with strong defenses but no movement or speech.", "Du bist verwandelt und handlungsunfähig, mit starken Abwehrvorteilen, aber ohne Bewegung oder Sprache.")
        "Poisoned" -> t("Disadvantage on attack rolls and ability checks.", "Nachteil bei Angriffswürfen und Attributswürfen.")
        "Prone" -> t("Crawling costs extra movement. Standing normally costs half your speed.", "Kriechen kostet zusätzliche Bewegung. Aufstehen kostet normalerweise die Hälfte deiner Bewegungsrate.")
        "Restrained" -> t("Your speed is 0; your attacks and Dexterity saves suffer while attacks against you gain advantage.", "Deine Bewegungsrate ist 0; Angriffe und Geschicklichkeitsrettungswürfe sind erschwert, Angriffe gegen dich erleichtert.")
        "Stunned" -> t("You are incapacitated, cannot move, and nearby attackers have advantage; relevant saves can fail automatically.", "Du bist handlungsunfähig, kannst dich nicht bewegen, und Angreifer haben Vorteil; bestimmte Rettungswürfe scheitern automatisch.")
        "Unconscious" -> t("You are incapacitated, cannot move or speak, drop held items, and fall Prone.", "Du bist handlungsunfähig, kannst dich weder bewegen noch sprechen, lässt Gehaltenes fallen und liegst am Boden.")
        "Concentrating" -> t("Taking damage may require a Constitution saving throw.", "Schaden kann einen Konstitutionsrettungswurf erfordern.")
        else -> null
    }

    private fun pf2eConditionInfo(name: String): String? = when (name) {
        "Blinded" -> t("Sight is unavailable, so visual tasks and locating creatures are impaired.", "Du kannst nicht sehen; sichtabhängige Aufgaben und das Aufspüren von Kreaturen sind beeinträchtigt.")
        "Clumsy" -> t("Reduced coordination hinders Dexterity-based defenses and actions.", "Verringerte Koordination erschwert geschicklichkeitsbasierte Verteidigung und Handlungen.")
        "Confused" -> t("You act unpredictably and cannot reliably distinguish allies from enemies.", "Du handelst unberechenbar und kannst Verbündete nicht zuverlässig von Gegnern unterscheiden.")
        "Controlled" -> t("Another creature directs your actions, even against your wishes.", "Eine andere Kreatur bestimmt deine Handlungen, auch gegen deinen Willen.")
        "Deafened" -> t("Hearing is unavailable, so sound-based awareness and activities are impaired.", "Du kannst nicht hören; geräuschbasierte Wahrnehmung und Handlungen sind beeinträchtigt.")
        "Drained" -> t("Lost vitality weakens Constitution-based endurance and reduces how much harm you can withstand.", "Verlorene Lebenskraft schwächt deine konstitutionsbasierte Ausdauer und verringert, wie viel Schaden du verkraftest.")
        "Enfeebled" -> t("Reduced strength hinders Strength-based attacks, damage, and physical tasks.", "Verringerte Stärke erschwert stärkebasierte Angriffe, Schaden und körperliche Aufgaben.")
        "Frightened" -> t("Fear imposes broad temporary penalties that normally ease over time.", "Furcht verursacht vorübergehende allgemeine Abzüge, die normalerweise mit der Zeit nachlassen.")
        "Grabbed" -> t("You are held in place, easier to exploit, and less able to perform precise actions.", "Du wirst festgehalten, bist leichter auszunutzen und kannst präzise Handlungen schlechter ausführen.")
        "Immobilized" -> t("You cannot move freely until the effect holding you in place ends.", "Du kannst dich nicht frei bewegen, bis der festhaltende Effekt endet.")
        "Invisible" -> t("Creatures relying on sight cannot see you and must locate you by other means.", "Kreaturen, die sich auf Sicht verlassen, können dich nicht sehen und müssen dich anders aufspüren.")
        "Off-Guard" -> t("Your defenses are lowered because you cannot protect yourself as effectively.", "Deine Verteidigung ist verringert, weil du dich nicht so wirksam schützen kannst.")
        "Paralyzed" -> t("Your body is frozen, preventing normal movement and physical actions.", "Dein Körper ist erstarrt, wodurch normale Bewegung und körperliche Handlungen verhindert werden.")
        "Persistent Damage" -> t("The listed damage repeats over time until the ongoing effect ends.", "Der angegebene Schaden wiederholt sich, bis der anhaltende Effekt endet.")
        "Prone" -> t("You are on the ground, easier to attack, and must reposition before moving normally.", "Du liegst am Boden, bist leichter anzugreifen und musst dich neu positionieren, bevor du dich normal bewegst.")
        "Quickened" -> t("You gain an extra action each turn, limited to the activities named by the source.", "Du erhältst in jedem Zug eine zusätzliche Aktion, begrenzt auf die vom Ursprung genannten Handlungen.")
        "Restrained" -> t("You are pinned in place and can do little besides trying to get free.", "Du bist festgesetzt und kannst kaum etwas tun, außer dich zu befreien.")
        "Sickened" -> t("Illness makes many tasks harder until you recover or spend effort overcoming it.", "Übelkeit erschwert viele Aufgaben, bis du dich erholst oder sie aktiv überwindest.")
        "Slowed" -> t("You regain fewer actions at the start of your turns.", "Du erhältst zu Beginn deiner Züge weniger Aktionen zurück.")
        "Stunned" -> t("You temporarily lose the ability to use some or all of your actions.", "Du verlierst vorübergehend die Möglichkeit, einige oder alle Aktionen einzusetzen.")
        "Unconscious" -> t("You are unaware and cannot act; you fall down and release held items.", "Du bist ohne Bewusstsein und kannst nicht handeln; du gehst zu Boden und lässt Gehaltenes fallen.")
        else -> null
    }

    private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()

    private fun knowledgeEntries(): List<SearchResultUi> {
        val entries = listOf(
            "maneuvers" to (t("Maneuvers", "Manöver") to t("Battle Master maneuvers are tactical effects. Using one spends a Superiority Die from the shared pool shown beside the Maneuvers heading.", "Kampfmeister-Manöver sind taktische Effekte. Ihre Nutzung verbraucht einen Überlegenheitswürfel aus dem gemeinsamen Vorrat neben der Manöver-Überschrift.")),
            "superiority-dice" to (t("Superiority Dice", "Überlegenheitswürfel") to t("A shared Battle Master resource used by maneuvers. The die size and remaining pool apply across every maneuver.", "Eine gemeinsame Kampfmeister-Ressource für Manöver. Würfelgröße und verbleibender Vorrat gelten für alle Manöver.")),
            "spell-slots" to (t("Spell slots", "Zauberplätze") to t("Casting a leveled spell spends one slot of that level or higher. Cantrips do not spend spell slots, and a Long Rest restores them.", "Ein Zauber mit Grad verbraucht einen Zauberplatz dieses oder eines höheren Grades. Zaubertricks verbrauchen keine Plätze; eine Lange Rast stellt sie wieder her.")),
            "sorcery-points" to (t("Sorcery Points", "Zaubereipunkte") to t("Sorcerers spend this shared resource on class features and can convert points into spell slots through Flexible Casting.", "Zauberer verbrauchen diese gemeinsame Ressource für Klassenmerkmale und können Punkte über Flexible Zauberei in Zauberplätze umwandeln.")),
            "metamagic" to (t("Metamagic", "Metamagie") to t("Metamagic changes how a spell works and normally spends Sorcery Points. Its exact cost is listed by the chosen option.", "Metamagie verändert die Wirkung eines Zaubers und verbraucht normalerweise Zaubereipunkte. Die genauen Kosten stehen bei der gewählten Option.")),
            "conditions" to (t("Conditions", "Zustände") to t("Conditions change what a creature can do. Open a specific condition for its current rules and effects.", "Zustände verändern die Möglichkeiten einer Kreatur. Öffne einen bestimmten Zustand für seine aktuellen Regeln und Effekte.")),
            "rests" to (t("Short & Long Rests", "Kurze & Lange Rast") to t("Rests recover only resources that list the matching recovery timing. A Long Rest also restores spell slots.", "Rasten stellen nur Ressourcen mit dem passenden Erholungszeitpunkt wieder her. Eine Lange Rast stellt außerdem Zauberplätze wieder her.")),
            "advantage" to (t("Advantage & disadvantage", "Vorteil & Nachteil") to t("Roll two d20s. Advantage keeps the higher; disadvantage keeps the lower. They cancel each other.", "Würfle zwei W20. Vorteil behält den höheren, Nachteil den niedrigeren. Beides hebt sich gegenseitig auf.")),
            "concentration" to (t("Concentration", "Konzentration") to t("You can maintain one concentration effect. Damage can trigger a Constitution save; starting another ends the first.", "Du kannst einen Konzentrationseffekt halten. Schaden kann einen Konstitutionsrettungswurf auslösen; ein neuer Effekt beendet den ersten.")),
            "cover" to (t("Cover", "Deckung") to t("Cover can raise defenses when an obstacle blocks part of a target. The DM decides which degree applies.", "Deckung kann die Verteidigung erhöhen, wenn ein Hindernis Teile des Ziels verdeckt. Die Spielleitung bestimmt den Grad.")),
            "difficult-terrain" to (t("Difficult terrain", "Schwieriges Gelände") to t("Each foot moved usually costs one extra foot of movement.", "Jeder bewegte Fuß kostet normalerweise einen zusätzlichen Fuß Bewegung.")),
            "opportunity" to (t("Opportunity attack", "Gelegenheitsangriff") to t("Leaving an enemy's reach can let it use its reaction for a melee attack; Disengage normally prevents this.", "Wenn du die Reichweite eines Gegners verlässt, kann er seine Reaktion für einen Nahkampfangriff nutzen; Lösen verhindert dies normalerweise.")),
            "dash" to (t("Dash", "Sprinten") to t("Spend the relevant action to gain extra movement equal to your current speed for the turn.", "Verbrauche die passende Aktion, um für diesen Zug zusätzliche Bewegung in Höhe deiner aktuellen Bewegungsrate zu erhalten.")),
            "dodge" to (t("Dodge", "Ausweichen") to t("Until your next turn, visible attackers have disadvantage and you gain an edge on Dexterity saves while able to act.", "Bis zu deinem nächsten Zug haben sichtbare Angreifer Nachteil und du erhältst einen Vorteil bei Geschicklichkeitsrettungswürfen, solange du handeln kannst.")),
            "ready" to (t("Ready", "Bereithalten") to t("Choose a perceivable trigger and an action; when it happens, use your reaction to respond. Readied spells can require concentration.", "Wähle einen wahrnehmbaren Auslöser und eine Aktion; tritt er ein, reagierst du mit deiner Reaktion. Bereitgehaltene Zauber können Konzentration verlangen.")),
            "bonus-action" to (t("Bonus action", "Bonusaktion") to t("You only have one when a feature, spell, or rule says it uses a bonus action.", "Du hast sie nur, wenn ein Merkmal, Zauber oder eine Regel ausdrücklich eine Bonusaktion verwendet.")),
            "reaction" to (t("Reaction", "Reaktion") to t("A response to a defined trigger, normally limited until it refreshes at the start of your next turn.", "Eine Antwort auf einen bestimmten Auslöser, normalerweise begrenzt, bis sie zu Beginn deines nächsten Zuges erneuert wird.")),
            "prone" to (t("Prone", "Liegend") to conditionExplanation("Prone")),
            "grappled" to (t("Grappled", "Gepackt") to conditionExplanation("Grappled")),
            "restrained" to (t("Restrained", "Festgesetzt") to conditionExplanation("Restrained")),
            "poisoned" to (t("Poisoned", "Vergiftet") to conditionExplanation("Poisoned")),
            "frightened" to (t("Frightened", "Verängstigt") to conditionExplanation("Frightened")),
            "stunned" to (t("Stunned", "Betäubt") to conditionExplanation("Stunned")),
            "invisible" to (t("Invisible", "Unsichtbar") to conditionExplanation("Invisible")),
        )
        return entries.map { (id, text) ->
            SearchResultUi("rule-$id", text.first, text.second, SearchResultKind.Rule, t("Info", "Info"))
        }
    }

    private fun creationAbilityBreakdown(ability: String): CreationAbilityBreakdownUi =
        creationAbilityBreakdowns().firstOrNull { it.ability == ability }
            ?: error("Unknown creation ability $ability")

    private fun creationAbilityName(ability: String): String = when (ability) {
        "STR" -> t("Strength", "Stärke")
        "DEX" -> t("Dexterity", "Geschicklichkeit")
        "CON" -> t("Constitution", "Konstitution")
        "INT" -> t("Intelligence", "Intelligenz")
        "WIS" -> t("Wisdom", "Weisheit")
        "CHA" -> t("Charisma", "Charisma")
        else -> ability
    }

    private fun creationAbilityDescription(ability: String): String = when (ability) {
        "STR" -> t(
            "Physical power used for melee force, Athletics, lifting, and carrying.",
            "Körperkraft für Nahkampfkraft, Athletik, Heben und Tragen.",
        )
        "DEX" -> if (creation.ruleset == Ruleset.Pf2eRemaster) {
            t(
                "Agility used for ranged attacks, Armor Class, Reflex defenses, and Stealth.",
                "Beweglichkeit für Fernkampfangriffe, Rüstungsklasse, Reflexverteidigung und Heimlichkeit.",
            )
        } else {
            t(
                "Agility used for finesse and ranged attacks, Armor Class, initiative, and Stealth.",
                "Beweglichkeit für Finesse- und Fernkampfangriffe, Rüstungsklasse, Initiative und Heimlichkeit.",
            )
        }
        "CON" -> if (creation.ruleset == Ruleset.Pf2eRemaster) {
            t(
                "Endurance that increases Hit Points and helps with Fortitude defenses.",
                "Ausdauer, die Trefferpunkte erhöht und bei Zähigkeitsverteidigungen hilft.",
            )
        } else {
            t(
                "Endurance that increases Hit Points and helps Constitution saves, including concentration checks.",
                "Ausdauer, die Trefferpunkte erhöht und bei Konstitutionsrettungswürfen einschließlich Konzentration hilft.",
            )
        }
        "INT" -> t(
            "Reasoning and learned knowledge used for Arcana and Intelligence-based class abilities.",
            "Verstand und erlerntes Wissen für Arkane Kunde und intelligenzbasierte Klassenfähigkeiten.",
        )
        "WIS" -> if (creation.ruleset == Ruleset.Pf2eRemaster) {
            t(
                "Awareness and intuition used for Perception, Will defenses, and this app's PF2e initiative.",
                "Wahrnehmung und Intuition für Wahrnehmung, Willensverteidigung und die PF2e-Initiative dieser App.",
            )
        } else {
            t(
                "Awareness and intuition used for Perception, Insight, Survival, and Wisdom-based class abilities.",
                "Wahrnehmung und Intuition für Wahrnehmung, Motiv erkennen, Überleben und weisheitsbasierte Klassenfähigkeiten.",
            )
        }
        "CHA" -> t(
            "Presence and force of personality used for social interaction and Charisma-based class abilities.",
            "Ausstrahlung und Willenskraft für soziale Interaktion und charismabasierte Klassenfähigkeiten.",
        )
        else -> t("A character ability score.", "Ein Attributswert des Charakters.")
    }

    private fun creationAbilityRecommendation(ability: String): String? {
        if (!creation.useRecommendations) return null
        val primary = primaryAbilityFor(creation.className)
        val rank = abilityPriorityForClass(creation.className).indexOf(ability)
        return when {
            ability == primary -> t(
                "Recommended for ${creation.className}: this is the class's main ability, so making it one of your highest scores is a strong default.",
                "Empfohlen für ${creation.className}: Dies ist das Hauptattribut der Klasse und daher meist einer der höchsten Werte.",
            )
            rank in 1..2 -> t(
                "Useful for ${creation.className}: this is a strong secondary ability in the app's class-aware assignment.",
                "Nützlich für ${creation.className}: Dies ist ein starkes Nebenattribut in der klassenbewussten Verteilung der App.",
            )
            else -> t(
                "Usually a lower priority for ${creation.className}, but your concept and table rules can make it important.",
                "Für ${creation.className} meist weniger wichtig, kann aber durch dein Konzept und die Spieltischregeln bedeutsam werden.",
            )
        }
    }

    private fun creationAncestryBonuses(): CreationAncestryBonuses {
        val privateEntry = privateEntryForName("ancestry", creation.ancestry)
        if (privateEntry != null) {
            val formula = privateEntry.formula
            val hasAbilityDeclaration = Regex(
                "\\bability\\s+(?:STR|DEX|CON|INT|WIS|CHA)\\b",
                RegexOption.IGNORE_CASE,
            ).containsMatchIn(formula)
            if (!hasAbilityDeclaration) return CreationAncestryBonuses(detail = privateEntry.sourceNote)

            val matches = Regex(
                "\\b(STR|DEX|CON|INT|WIS|CHA)\\s*=\\s*([+-]?\\d+)\\b",
                RegexOption.IGNORE_CASE,
            ).findAll(formula).toList()
            val keys = matches.map { it.groupValues[1].uppercase() }
            val valuesAreValid = matches.isNotEmpty() && matches.all { match ->
                val raw = match.groupValues[2]
                raw.startsWith('+') && (raw.drop(1).toIntOrNull() ?: 0) in 1..10
            }
            val keysAreUnique = keys.distinct().size == keys.size
            if (!valuesAreValid || !keysAreUnique) {
                return CreationAncestryBonuses(detail = privateEntry.sourceNote, invalidPrivateFormula = true)
            }
            return CreationAncestryBonuses(
                amounts = matches.associate { match ->
                    match.groupValues[1].uppercase() to match.groupValues[2].drop(1).toInt()
                },
                detail = privateEntry.sourceNote,
            )
        }

        if (creation.ruleset != Ruleset.Fifth2014) return CreationAncestryBonuses()
        return CreationAncestryBonuses(
            amounts = fifth2014AncestryBonuses[creation.ancestry].orEmpty(),
            detail = "SRD 5.1 · Races: ${creation.ancestry}",
        )
    }

    private fun abilityScoresForDraft(): LinkedHashMap<String, Int> = linkedMapOf<String, Int>().apply {
        creationAbilityBreakdowns().forEach { breakdown -> put(breakdown.ability, breakdown.finalScore) }
    }

    private fun baseAbilityScoresForDraft(): LinkedHashMap<String, Int> {
        if (creation.statMethod == StatMethod.Manual) return LinkedHashMap(creation.manualAbilities)
        if (creation.statMethod == StatMethod.Rolled) {
            ensureRolledAbilityAssignments()
            return linkedMapOf<String, Int>().apply {
                creationAbilityOrder.forEach { ability ->
                    put(ability, creation.rolledAbilityAssignments.getValue(ability))
                }
            }
        }
        val scores = when (creation.statMethod) {
            StatMethod.StandardArray -> if (creation.ruleset == Ruleset.Pf2eRemaster) listOf(18, 16, 14, 12, 10, 8) else listOf(15, 14, 13, 12, 10, 8)
            StatMethod.PointBuy -> listOf(15, 15, 14, 10, 8, 8)
            StatMethod.Rolled -> error("handled above")
            StatMethod.Manual -> error("handled above")
        }.sortedDescending()
        val priority = abilityPriorityForClass(creation.className)
        return linkedMapOf<String, Int>().apply {
            priority.forEachIndexed { index, ability -> put(ability, scores[index]) }
        }
    }

    private fun ensureRolledAbilityAssignments() {
        if (creation.rolledScores.size != creationAbilityOrder.size) rollCreationAbilityScores()
        val assignmentsAreComplete =
            creation.rolledAbilityAssignments.keys.toSet() == creationAbilityOrder.toSet() &&
                creation.rolledAbilityAssignments.values.toList().sorted() == creation.rolledScores.sorted()
        if (!assignmentsAreComplete) assignRolledScoresForCurrentClass()
    }

    private fun assignRolledScoresForCurrentClass() {
        creation.rolledAbilityAssignments.clear()
        if (creation.rolledScores.size == creationAbilityOrder.size) {
            val scores = creation.rolledScores.sortedDescending()
            abilityPriorityForClass(creation.className).forEachIndexed { index, ability ->
                creation.rolledAbilityAssignments[ability] = scores[index]
            }
        }
        creation.rolledAssignmentEdited = false
    }

    private fun abilityPriorityForClass(className: String): List<String> = when (className) {
            "Wizard" -> listOf("INT", "CON", "DEX", "WIS", "CHA", "STR")
            "Cleric", "Druid" -> listOf("WIS", "CON", "DEX", "CHA", "INT", "STR")
            "Rogue", "Monk" -> listOf("DEX", "WIS", "CON", "CHA", "INT", "STR")
            "Bard", "Sorcerer", "Warlock" -> listOf("CHA", "DEX", "CON", "WIS", "INT", "STR")
            "Paladin", "Champion" -> listOf("STR", "CHA", "CON", "WIS", "DEX", "INT")
            "Ranger" -> listOf("DEX", "WIS", "CON", "STR", "INT", "CHA")
            else -> listOf("STR", "CON", "DEX", "WIS", "CHA", "INT")
        }

    private fun DerivedModifierFormulaUi.resolve(
        abilities: Map<String, Int>,
        proficiency: Int,
        ruleset: Ruleset,
        level: Int,
        ranks: Map<String, ProficiencyRank>,
        legacyIds: Set<String>,
    ): Int {
        val rank = proficiencyId?.let { id -> proficiencyRank(id, ranks, legacyIds) }
        val proficiencyValue = if (proficiencyId != null) proficiencyModifier(ruleset, level, rank) else proficiencyMultiplier * proficiency
        return base + abilityModifier(abilities[ability] ?: 10) + proficiencyValue
    }

    private fun proficiencyForLevel(level: Int): Int = 2 + (level.coerceIn(1, 20) - 1) / 4

    private fun recalculateWeapon(
        weapon: WeaponUi,
        abilities: Map<String, Int>,
        proficiency: Int,
        ruleset: Ruleset,
        level: Int,
        ranks: Map<String, ProficiencyRank>,
        legacyIds: Set<String>,
    ): WeaponUi {
        val ability = weapon.abilityModifierOverride ?: abilityModifier(abilities[weapon.ability] ?: 10)
        val rank = weapon.proficiencyId?.let { id -> proficiencyRank(id, ranks, legacyIds) }
        val proficiencyValue = if (weapon.proficiencyId != null) proficiencyModifier(ruleset, level, rank)
        else if (weapon.proficient) proficiency else 0
        val attackBonus = if (weapon.attackBonusOverride == null) {
            ability + proficiencyValue + weapon.itemBonus
        } else weapon.attackBonus
        val damage = weapon.damageAbility?.let { damageAbility ->
            replaceFormulaModifier(weapon.damage, abilityModifier(abilities[damageAbility] ?: 10))
        } ?: weapon.damage
        return weapon.copy(attackBonus = attackBonus, damage = damage)
    }

    private fun replaceFormulaModifier(formula: String, modifier: Int): String {
        val match = Regex("^\\s*(\\d+d\\d+)(?:\\s*[+-]\\s*\\d+)?(.*)$", RegexOption.IGNORE_CASE).matchEntire(formula)
            ?: return formula
        return buildString {
            append(match.groupValues[1])
            when {
                modifier > 0 -> append(" + $modifier")
                modifier < 0 -> append(" - ${kotlin.math.abs(modifier)}")
            }
            append(match.groupValues[2])
        }.trim()
    }

    private fun abilityModifier(score: Int): Int = kotlin.math.floor((score - 10) / 2.0).toInt()

    private fun ProficiencyRank.rankIndex(): Int = when (this) {
        ProficiencyRank.TRAINED -> 1
        ProficiencyRank.EXPERT -> 2
        ProficiencyRank.MASTER -> 3
        ProficiencyRank.LEGENDARY -> 4
    }

    private fun proficiencyRank(
        id: String,
        ranks: Map<String, ProficiencyRank>,
        legacyIds: Set<String>,
    ): ProficiencyRank? = ranks[id] ?: if (id in legacyIds) ProficiencyRank.TRAINED else null

    private fun proficiencyModifier(ruleset: Ruleset, level: Int, rank: ProficiencyRank?): Int = when {
        rank == null -> 0
        ruleset == Ruleset.Pf2eRemaster -> level.coerceIn(1, 20) + rank.rankBonus
        else -> proficiencyForLevel(level)
    }

    private fun saveProficiencyId(ability: String): String = when (ability.uppercase().take(3)) {
        "STR" -> "save:strength"
        "DEX" -> "save:dexterity"
        "CON" -> "save:constitution"
        "INT" -> "save:intelligence"
        "WIS" -> "save:wisdom"
        else -> "save:charisma"
    }

    private fun persist(): Boolean {
        turnSession?.let { savedTurnDrafts[it.characterId] = it.snapshot() }
        return runCatching {
            val documents = characters.map { character ->
                val characterConditions = conditions.filter { it.characterId == character.id }
                val document = character.toDocument(characterConditions)
                val turn = savedTurnDrafts[character.id]?.toDomainTurnDraft(document, characterConditions)
                document.copy(state = document.state.copy(activeTurn = turn)).also {
                    require(CharacterDocumentValidator.validate(it).isEmpty())
                }
            }
            storage.writeState(
                json.encodeToString(
                    PersistedAppState(
                        language = language,
                        characters = documents,
                        conditions = conditions.filter { it.characterId.isBlank() },
                        privateEntries = privateEntries.toList(),
                        pendingImports = pendingImports.toList(),
                        installedPrivatePacks = installedPrivatePacks.toList(),
                    )
                )
            )
        }.isSuccess
    }
}

private fun seedCharacters(): List<CharacterUi> = listOf(
    CharacterUi(
        id = "seed-fighter-10",
        name = "Mara Stoneguard",
        ruleset = Ruleset.Fifth2024,
        level = 10,
        ancestry = "Human",
        className = "Fighter",
        subclass = "Battle Master",
        hp = 84,
        maxHp = 84,
        armorClass = 18,
        unarmoredArmorClass = 12,
        speedFeet = 30,
        initiative = 2,
        proficiency = 4,
        portraitSeed = 10,
        abilities = linkedMapOf("STR" to 18, "DEX" to 14, "CON" to 16, "INT" to 10, "WIS" to 12, "CHA" to 10),
        skills = linkedMapOf("Athletics" to 8, "Perception" to 5, "Intimidation" to 4, "Survival" to 5),
        saves = standardSaves(mapOf("STR" to 4, "DEX" to 2, "CON" to 3, "INT" to 0, "WIS" to 1, "CHA" to 0), setOf("STR", "CON"), 4),
        languages = listOf("Common", "Dwarvish"),
        lockedLanguages = listOf("Common"),
        proficiencyIds = setOf("weapon:simple", "weapon:martial"),
        proficiencyRanks = mapOf("weapon:simple" to ProficiencyRank.TRAINED, "weapon:martial" to ProficiencyRank.TRAINED),
        weapons = listOf(
            WeaponUi("longsword", "Longsword", 8, "1d8 + 4", "Slashing", "Versatile (1d10)", mastery = "Sap", damageAbility = "STR"),
            WeaponUi("longbow", "Longbow", 6, "1d8 + 2", "Piercing", "Ammunition, heavy, two-handed", ability = "DEX", range = "150/600 ft", mastery = "Slow", damageAbility = "DEX"),
        ),
        spells = emptyList(),
        features = listOf(
            FeatureUi("second-wind", "Second Wind", "Regain 1d10 + Fighter level (10) HP.", 4, 4, Recovery.SHORT_REST, FeatureEffect.SECOND_WIND, ActionCost(bonusActions = 1)),
            FeatureUi("action-surge", "Action Surge", "Take one additional action this turn.", 1, 1, Recovery.SHORT_REST, FeatureEffect.EXTRA_ACTION),
            FeatureUi("extra-attack", "Extra Attack", "Attack twice when you take the Attack action."),
            FeatureUi("indomitable", "Indomitable", "Reroll a failed saving throw.", 1, 1, Recovery.LONG_REST, FeatureEffect.REROLL_SAVE),
            FeatureUi("superiority-dice", "Superiority Dice", "Five d10 dice fuel your Battle Master maneuvers.", 5, 5, Recovery.SHORT_REST, resourceDieSides = 10),
            FeatureUi("maneuver-precision-attack", "Precision Attack", "Spend a Superiority Die after an attack roll to add it to the roll.", 5, 5, Recovery.SHORT_REST, resourceId = "superiority-dice"),
            FeatureUi("maneuver-trip-attack", "Trip Attack", "After a weapon hit, spend a Superiority Die to add damage and possibly knock the target Prone.", 5, 5, Recovery.SHORT_REST, resourceId = "superiority-dice"),
            FeatureUi("maneuver-riposte", "Riposte", "When a creature misses you in melee, spend a Superiority Die and your Reaction to attack it.", 5, 5, Recovery.SHORT_REST, actionCost = ActionCost(reactions = 1), resourceId = "superiority-dice"),
        ),
        equipmentItems = listOf(
            EquipmentUi("chain-mail", "Chain Mail", EquipmentKind.ARMOR, worn = true, armorClass = 16),
            EquipmentUi("shield", "Shield", EquipmentKind.ARMOR, worn = true, shieldBonus = 2),
            EquipmentUi("explorers-pack", "Explorer's Pack"),
        ),
        progression = progression("Fighter", 10, first = 13, later = 9),
        derivation = standardDerivation(setOf("STR", "CON")),
        notes = listOf(CharacterNote("general", "General", "Battle Master test character for level-scaled features, shared Superiority Dice, armor, and multiple attacks.")),
    ),
    CharacterUi(
        id = "seed-wizard-5",
        name = "Ilyra Quill",
        ruleset = Ruleset.Fifth2024,
        level = 5,
        ancestry = "Elf",
        className = "Wizard",
        subclass = "Evoker",
        hp = 32,
        maxHp = 32,
        armorClass = 13,
        unarmoredArmorClass = 13,
        speedFeet = 30,
        initiative = 3,
        proficiency = 3,
        portraitSeed = 20,
        abilities = linkedMapOf("STR" to 8, "DEX" to 16, "CON" to 14, "INT" to 18, "WIS" to 12, "CHA" to 10),
        skills = linkedMapOf("Arcana" to 7, "History" to 7, "Investigation" to 7, "Perception" to 4),
        saves = standardSaves(mapOf("STR" to -1, "DEX" to 3, "CON" to 2, "INT" to 4, "WIS" to 1, "CHA" to 0), setOf("INT", "WIS"), 3),
        languages = listOf("Common", "Elvish", "Draconic"),
        lockedLanguages = listOf("Common", "Elvish"),
        weapons = listOf(WeaponUi("quarterstaff", "Quarterstaff", 2, "1d6 - 1", "Bludgeoning", "Versatile (1d8)", damageAbility = "STR")),
        spells = listOf(
            SpellUi("fire-bolt", "Fire Bolt", 0, "Ranged spell attack · 120 ft", spellAttack = true),
            SpellUi("magic-missile", "Magic Missile", 1, "Automatic force darts"),
            SpellUi("shield", "Shield", 1, "+5 AC until your next turn", activationCost = ActionCost(reactions = 1)),
            SpellUi(
                "fireball",
                "Fireball",
                3,
                "Dexterity save · area fire damage",
                castPreviews = (3..9).associateWith { level -> "${8 + level - 3}d6 fire" },
                savingThrows = listOf(SavingThrowPrompt(Ability.DEXTERITY, DifficultyClass(useSpellcasting = true))),
            ),
        ),
        features = listOf(FeatureUi("arcane-recovery", "Arcane Recovery", "Recover expended spell slots after a Short Rest.", 1, 1, Recovery.LONG_REST)),
        equipmentItems = listOf(EquipmentUi("spellbook", "Spellbook"), EquipmentUi("component-pouch", "Component Pouch")),
        hasSpellcastingCapability = true,
        progression = progression("Wizard", 5, first = 8, later = 6),
        derivation = standardDerivation(setOf("INT", "WIS")),
        notes = listOf(CharacterNote("general", "General", "Wizard test character for prepared spells and spell-first turns.")),
    ),
    CharacterUi(
        id = "seed-sorcerer-5",
        name = "Rook Ember",
        ruleset = Ruleset.Fifth2014,
        level = 5,
        ancestry = "Human",
        className = "Sorcerer",
        subclass = "Draconic Bloodline",
        hp = 37,
        maxHp = 37,
        armorClass = 15,
        unarmoredArmorClass = 15,
        speedFeet = 30,
        initiative = 2,
        proficiency = 3,
        portraitSeed = 30,
        abilities = linkedMapOf("STR" to 8, "DEX" to 14, "CON" to 16, "INT" to 10, "WIS" to 12, "CHA" to 18),
        skills = linkedMapOf("Arcana" to 3, "Deception" to 7, "Insight" to 4, "Persuasion" to 7),
        saves = standardSaves(mapOf("STR" to -1, "DEX" to 2, "CON" to 3, "INT" to 0, "WIS" to 1, "CHA" to 4), setOf("CON", "CHA"), 3),
        languages = listOf("Common", "Draconic"),
        lockedLanguages = listOf("Common"),
        weapons = listOf(WeaponUi("light-crossbow", "Light Crossbow", 5, "1d8 + 2", "Piercing", "Ammunition, loading, two-handed", ability = "DEX", range = "80/320 ft", damageAbility = "DEX")),
        spells = listOf(
            SpellUi("ray-of-frost", "Ray of Frost", 0, "Ranged spell attack · cold damage", spellAttack = true),
            SpellUi(
                "burning-hands",
                "Burning Hands",
                1,
                "Dexterity save · cone fire damage",
                castPreviews = (1..9).associateWith { level -> "${level + 2}d6 fire" },
                savingThrows = listOf(SavingThrowPrompt(Ability.DEXTERITY, DifficultyClass(useSpellcasting = true))),
            ),
            SpellUi("misty-step", "Misty Step", 2, "Bonus-action teleport", activationCost = ActionCost(bonusActions = 1)),
        ),
        features = listOf(
            FeatureUi("sorcery-points", "Sorcery Points", "Fuel Metamagic options.", 5, 5, Recovery.LONG_REST),
            FeatureUi("metamagic", "Metamagic", "Choose how a known spell is altered."),
        ),
        equipmentItems = listOf(EquipmentUi("arcane-focus", "Arcane Focus")),
        hasSpellcastingCapability = true,
        progression = progression("Sorcerer", 5, first = 9, later = 7),
        derivation = standardDerivation(setOf("CON", "CHA")),
        notes = listOf(CharacterNote("general", "General", "2014 Sorcerer test character for Metamagic and revision-specific rules.")),
    ),
    CharacterUi(
        id = "seed-monk-5",
        name = "Tarin Reed",
        ruleset = Ruleset.Fifth2024,
        level = 5,
        ancestry = "Dwarf",
        className = "Monk",
        subclass = "Warrior of the Open Hand",
        hp = 38,
        maxHp = 38,
        armorClass = 16,
        unarmoredArmorClass = 16,
        speedFeet = 40,
        initiative = 4,
        proficiency = 3,
        portraitSeed = 40,
        abilities = linkedMapOf("STR" to 10, "DEX" to 18, "CON" to 14, "INT" to 8, "WIS" to 16, "CHA" to 10),
        skills = linkedMapOf("Acrobatics" to 7, "Insight" to 6, "Perception" to 6, "Stealth" to 7),
        saves = standardSaves(mapOf("STR" to 0, "DEX" to 4, "CON" to 2, "INT" to -1, "WIS" to 3, "CHA" to 0), setOf("STR", "DEX"), 3),
        languages = listOf("Common", "Dwarvish"),
        lockedLanguages = listOf("Common", "Dwarvish"),
        weapons = listOf(
            WeaponUi("quarterstaff", "Quarterstaff", 7, "1d8 + 4", "Bludgeoning", "Versatile", ability = "DEX", damageAbility = "DEX"),
            WeaponUi("unarmed", "Unarmed Strike", 7, "1d8 + 4", "Bludgeoning", "Monk weapon", ability = "DEX", damageAbility = "DEX"),
        ),
        spells = emptyList(),
        features = listOf(
            FeatureUi("focus-points", "Focus Points", "Fuel Monk techniques.", 5, 5, Recovery.SHORT_REST),
            FeatureUi("extra-attack", "Extra Attack", "Attack twice when you take the Attack action."),
            FeatureUi("stunning-strike", "Stunning Strike", "Spend Focus after hitting with a Monk weapon or Unarmed Strike."),
        ),
        equipmentItems = listOf(EquipmentUi("explorers-pack", "Explorer's Pack")),
        progression = progression("Monk", 5, first = 10, later = 7),
        derivation = standardDerivation(setOf("STR", "DEX")),
        notes = listOf(CharacterNote("general", "General", "Monk test character for Focus resources and fast movement.")),
    ),
)

private fun progression(className: String, level: Int, first: Int, later: Int): List<LevelProgressionUi> =
    (1..level).map { current -> LevelProgressionUi(current, className, if (current == 1) first else later, HpMethod.Fixed) }

private fun standardSaves(base: Map<String, Int>, proficient: Set<String>, proficiency: Int): Map<String, Int> {
    val labels = linkedMapOf("Strength" to "STR", "Dexterity" to "DEX", "Constitution" to "CON", "Intelligence" to "INT", "Wisdom" to "WIS", "Charisma" to "CHA")
    return labels.mapValues { (_, ability) -> (base[ability] ?: 0) + if (ability in proficient) proficiency else 0 }
}

private fun standardDerivation(proficientSaves: Set<String>): CharacterDerivationUi {
    val labels = mapOf("Strength" to "STR", "Dexterity" to "DEX", "Constitution" to "CON", "Intelligence" to "INT", "Wisdom" to "WIS", "Charisma" to "CHA")
    return CharacterDerivationUi(
        proficiencyFromLevel = true,
        initiative = DerivedModifierFormulaUi("DEX"),
        saves = labels.mapValues { (_, ability) -> DerivedModifierFormulaUi(ability, if (ability in proficientSaves) 1 else 0) },
    )
}

private fun String.isConcreteSubclass(): Boolean =
    isNotBlank() && this != "—" && !contains("Adventurer's path", ignoreCase = true)
