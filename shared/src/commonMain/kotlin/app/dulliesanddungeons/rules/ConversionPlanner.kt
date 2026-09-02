package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.ConversionMapping
import app.dulliesanddungeons.domain.ConversionPlan
import app.dulliesanddungeons.domain.ChoiceSelection
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.MappingKind
import app.dulliesanddungeons.domain.Pf2eBuildData
import app.dulliesanddungeons.domain.RulesetId

data class ConversionDraft(
    val character: CharacterBuild,
    val plan: ConversionPlan,
)

object ConversionPlanner {
    private val fifthEditionRulesets = setOf(
        RulesetId.FIFTH_EDITION_2014,
        RulesetId.FIFTH_EDITION_2024,
    )

    fun copyForRuleset(
        source: CharacterBuild,
        targetCharacterId: String,
        targetRuleset: RulesetId,
        explicitMappings: Map<String, String> = emptyMap(),
    ): ConversionDraft {
        require(targetCharacterId.isNotBlank() && targetCharacterId != source.id) {
            "Conversion must create a separately identified character"
        }
        val relatedFifthEditions = source.ruleset in fifthEditionRulesets && targetRuleset in fifthEditionRulesets
        val preserved = setOf("name", "portraitFileName", "locale", "abilities")
        val choiceIds = buildSet {
            add(source.ancestryId)
            add(source.backgroundId)
            source.classes.forEach { add(it.classId) }
            addAll(source.featIds)
            addAll(source.knownSpellIds)
        }
        val mappings = choiceIds.map { sourceId ->
            val targetId = explicitMappings[sourceId]
            ConversionMapping(
                sourceId = sourceId,
                targetId = targetId,
                kind = if (targetId != null) MappingKind.EXACT else MappingKind.RESELECT_REQUIRED,
                explanation = if (targetId != null) {
                    "Mapped by an explicit content-pack conversion entry."
                } else {
                    "Select the target-ruleset equivalent during review."
                },
            )
        }
        val unresolved = mappings.filter { it.kind == MappingKind.RESELECT_REQUIRED }.mapTo(mutableSetOf()) { it.sourceId }
        val targetPayload = if (targetRuleset.isFiveEdition) {
            FiveEBuildData(
                ancestryId = explicitMappings[source.ancestryId].orEmpty(),
                heritageId = null,
                backgroundId = explicitMappings[source.backgroundId].orEmpty(),
                classes = source.classes.mapNotNull { classLevel ->
                    explicitMappings[classLevel.classId]?.let { mapped ->
                        classLevel.copy(classId = mapped, subclassId = null)
                    }
                },
                abilities = source.abilities,
                feats = source.featIds.mapNotNull { explicitMappings[it] }.map(::ChoiceSelection),
                proficiencyIds = emptySet(),
                languages = emptyList(),
                knownSpells = source.knownSpellIds.mapNotNull { explicitMappings[it] }.map(::ChoiceSelection),
                preparedSpellIds = emptySet(),
                features = emptyList(),
            )
        } else {
            Pf2eBuildData(
                ancestryId = explicitMappings[source.ancestryId].orEmpty(),
                heritageId = "",
                backgroundId = explicitMappings[source.backgroundId].orEmpty(),
                classId = "",
                level = source.level,
                abilities = source.abilities,
            )
        }
        val draft = CharacterBuild(
            id = targetCharacterId,
            name = source.name,
            ruleset = targetRuleset,
            sourceCharacterId = source.id,
            locale = source.locale,
            contentVersions = emptyList(),
            portraitFileName = source.portraitFileName,
            rules = targetPayload,
        )
        return ConversionDraft(
            character = draft,
            plan = ConversionPlan(
                sourceCharacterId = source.id,
                targetCharacterId = targetCharacterId,
                targetRuleset = targetRuleset,
                preservedFields = preserved,
                mappings = mappings,
                warnings = if (relatedFifthEditions) {
                    listOf("Review changed class, feat, spell, and equipment rules before saving.")
                } else {
                    listOf("Cross-system conversion is a guided rebuild; derived numbers are not transferred.")
                },
                unresolvedChoiceIds = unresolved,
            ),
        )
    }
}
