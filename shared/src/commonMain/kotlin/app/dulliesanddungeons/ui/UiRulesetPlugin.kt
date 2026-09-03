package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.AvailableAction
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.ChoiceSelection
import app.dulliesanddungeons.domain.ClassLevel
import app.dulliesanddungeons.domain.DerivedStats
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.LocaleId
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.Pf2eBuildData
import app.dulliesanddungeons.domain.ResourcePool
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.TurnDraft
import app.dulliesanddungeons.domain.ValidationIssue
import app.dulliesanddungeons.rules.FiveETurnReducer
import app.dulliesanddungeons.rules.Pf2eTurnReducer
import app.dulliesanddungeons.rules.RulesetPlugin
import app.dulliesanddungeons.rules.SeededSampleCatalog
import app.dulliesanddungeons.rules.TurnReducer
import app.dulliesanddungeons.rules.matches

/** UI adapter that makes suggestion generation pass through the same ruleset-plugin boundary. */
internal class UiRulesetPlugin private constructor(override val id: RulesetId) : RulesetPlugin {
    private val catalog = SeededSampleCatalog.create()
    override val turnReducer: TurnReducer = when (id) {
        RulesetId.FIFTH_EDITION_2014, RulesetId.FIFTH_EDITION_2024 -> FiveETurnReducer(id)
        RulesetId.PF2E_REMASTER -> Pf2eTurnReducer()
    }

    override fun validate(build: CharacterBuild, state: CharacterState?): List<ValidationIssue> = emptyList()

    override fun derive(build: CharacterBuild, state: CharacterState): DerivedStats {
        val dexterity = modifier(build.abilities[Ability.DEXTERITY] ?: 10)
        val proficiency = 2 + (build.level - 1) / 4
        return DerivedStats(
            armorClass = 10 + dexterity,
            proficiencyBonus = proficiency,
            initiative = dexterity,
            savingThrows = build.abilities.mapValues { modifier(it.value) },
            speedsFeet = state.activeTurn?.speedsFeet.orEmpty(),
        )
    }

    override fun availableActions(build: CharacterBuild, state: CharacterState): List<AvailableAction> =
        catalog.rules(id).filter { it.predicate.matches(build) }.mapNotNull { it.action }

    override fun startTurn(build: CharacterBuild, state: CharacterState): TurnDraft {
        val speeds = mapOf(MovementMode.WALK to 30)
        val resources = state.resources.associate { it.id to it.current }
        return when (val reducer = turnReducer) {
            is FiveETurnReducer -> reducer.newTurn(
                speedsFeet = speeds,
                attacksPerAction = if (build.classes.any { it.classId == "fighter" && it.levels >= 5 }) 2 else 1,
                resources = resources,
            )
            is Pf2eTurnReducer -> reducer.newTurn(speeds, resources, characterId = build.id)
            else -> error("Unsupported reducer")
        }
    }

    fun build(character: CharacterUi): CharacterBuild {
        val abilities = character.abilities.mapNotNull { (key, value) -> ability(key)?.let { it to value } }.toMap()
        val featureSelections = character.features.map { ChoiceSelection(it.id.replace('-', '_')) }
        val rules = when (id) {
            RulesetId.FIFTH_EDITION_2014, RulesetId.FIFTH_EDITION_2024 -> FiveEBuildData(
                ancestryId = character.ancestry.lowercase(),
                backgroundId = "local",
                classes = listOf(
                    ClassLevel(
                        character.className.lowercase(),
                        character.level,
                        character.subclass.takeUnless { it == "—" }?.lowercase(),
                    ),
                ),
                abilities = abilities,
                features = featureSelections,
            )
            RulesetId.PF2E_REMASTER -> Pf2eBuildData(
                ancestryId = character.ancestry.lowercase(),
                heritageId = "local",
                backgroundId = "local",
                classId = character.className.lowercase(),
                level = character.level,
                abilities = abilities,
                features = featureSelections,
            )
        }
        return CharacterBuild(
            id = character.id,
            name = character.name,
            ruleset = id,
            locale = LocaleId.EN,
            rules = rules,
        )
    }

    fun state(character: CharacterUi): CharacterState = CharacterState(
        characterId = character.id,
        currentHitPoints = character.hp,
        maximumHitPoints = character.maxHp,
        temporaryHitPoints = character.temporaryHp,
        maximumHitPointReduction = character.maxHpReduction,
        activeConcentration = character.activeConcentration,
        resources = character.features.mapNotNull { feature ->
            val current = feature.remaining ?: return@mapNotNull null
            val maximum = feature.maximum ?: return@mapNotNull null
            ResourcePool(feature.id.replace('-', '_'), feature.name, current, maximum)
        },
    )

    companion object {
        fun forCharacter(character: CharacterUi) = UiRulesetPlugin(when (character.ruleset) {
            Ruleset.Fifth2024 -> RulesetId.FIFTH_EDITION_2024
            Ruleset.Fifth2014 -> RulesetId.FIFTH_EDITION_2014
            Ruleset.Pf2eRemaster -> RulesetId.PF2E_REMASTER
        })
    }
}

private fun ability(key: String): Ability? = when (key.uppercase()) {
    "STR" -> Ability.STRENGTH
    "DEX" -> Ability.DEXTERITY
    "CON" -> Ability.CONSTITUTION
    "INT" -> Ability.INTELLIGENCE
    "WIS" -> Ability.WISDOM
    "CHA" -> Ability.CHARISMA
    else -> null
}

private fun modifier(score: Int): Int = kotlin.math.floor((score - 10) / 2.0).toInt()
