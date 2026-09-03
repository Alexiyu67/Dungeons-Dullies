package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CalculationPart
import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.CoinDenomination
import app.dulliesanddungeons.domain.DerivedStats
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.FiveEHealthState
import app.dulliesanddungeons.domain.HealthStatus
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.Pf2eBuildData
import app.dulliesanddungeons.domain.Pf2eHealthState
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.Severity
import app.dulliesanddungeons.domain.ValidationIssue

enum class Pf2eProficiencyRank(val rankBonus: Int) {
    UNTRAINED(0),
    TRAINED(2),
    EXPERT(4),
    MASTER(6),
    LEGENDARY(8),
}

object DerivedStatRules {
    fun abilityModifier(score: Int): Int {
        require(score >= 0) { "ability score cannot be negative" }
        return (score / 2) - 5
    }

    fun fiveEProficiencyBonus(level: Int): Int {
        require(level in 1..20) { "5e level must be between 1 and 20" }
        return 2 + (level - 1) / 4
    }

    fun fiveESkillModifier(
        abilityScore: Int,
        level: Int,
        proficient: Boolean,
        expertise: Boolean = false,
    ): Int {
        val multiplier = when {
            expertise -> 2
            proficient -> 1
            else -> 0
        }
        return abilityModifier(abilityScore) + fiveEProficiencyBonus(level) * multiplier
    }

    fun pf2eProficiencyModifier(level: Int, rank: Pf2eProficiencyRank): Int {
        require(level in 1..20) { "PF2e level must be between 1 and 20" }
        return if (rank == Pf2eProficiencyRank.UNTRAINED) 0 else level + rank.rankBonus
    }

    fun pf2eMultipleAttackPenalty(attackNumber: Int, agile: Boolean): Int {
        require(attackNumber >= 1) { "attack number starts at one" }
        if (attackNumber == 1) return 0
        val step = if (agile) 4 else 5
        return -step * (attackNumber - 1).coerceAtMost(2)
    }

    fun combinedFiveECasterLevel(contributions: List<FiveECasterContribution>): Int =
        contributions.sumOf { contribution ->
            require(contribution.classLevels >= 0) { "class levels cannot be negative" }
            require(contribution.divisor >= 1) { "caster divisor must be positive" }
            if (contribution.roundUp) {
                (contribution.classLevels + contribution.divisor - 1) / contribution.divisor
            } else {
                contribution.classLevels / contribution.divisor
            }
        }.coerceIn(0, 20)

    fun fiveESpellSlots(casterLevel: Int): List<Int> {
        require(casterLevel in 0..20) { "combined caster level must be between 0 and 20" }
        return FIVE_E_SPELL_SLOTS[casterLevel].toList()
    }

    /** PF2e typed bonuses do not stack with bonuses of the same type; penalties do. */
    fun stackPf2eModifiers(modifiers: List<TypedModifier>): Int =
        modifiers.filter { it.type == ModifierStackType.UNTYPED }.sumOf { it.value } +
            ModifierStackType.entries
                .filter { it != ModifierStackType.UNTYPED }
                .sumOf { type ->
                    val values = modifiers.filter { it.type == type }.map { it.value }
                    (values.filter { it > 0 }.maxOrNull() ?: 0) + (values.filter { it < 0 }.minOrNull() ?: 0)
                }

    fun basicFiveE(build: CharacterBuild, baseArmorClass: Int = 10, state: CharacterState? = null): DerivedStats {
        require(build.rules is FiveEBuildData) { "basicFiveE requires a fifth-edition build payload" }
        val dexterity = abilityModifier(build.abilities.getValue(Ability.DEXTERITY))
        val proficiency = fiveEProficiencyBonus(build.level)
        val effectiveMaximum = state?.let { FiveEHealthRules.effectiveMaximumHitPoints(it, build.ruleset) }
        val healthStatus = state?.let { FiveEHealthRules.status(it, build.ruleset) } ?: HealthStatus.ALIVE
        return DerivedStats(
            armorClass = baseArmorClass + dexterity,
            proficiencyBonus = proficiency,
            initiative = dexterity,
            savingThrows = Ability.entries.associateWith { ability ->
                val proficient = build.proficiencyIds.contains("save:${ability.name.lowercase()}")
                abilityModifier(build.abilities.getValue(ability)) + if (proficient) proficiency else 0
            },
            speedsFeet = mapOf(MovementMode.WALK to 30),
            effectiveMaximumHitPoints = effectiveMaximum,
            healthStatus = healthStatus,
            explanations = mapOf(
                "armorClass" to listOf(
                    CalculationPart("base", baseArmorClass),
                    CalculationPart("Dexterity", dexterity),
                ),
                "initiative" to listOf(CalculationPart("Dexterity", dexterity)),
            ),
        )
    }
}

data class FiveECasterContribution(
    val classLevels: Int,
    val divisor: Int = 1,
    val roundUp: Boolean = false,
)

private val FIVE_E_SPELL_SLOTS = listOf(
    intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(3, 0, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(4, 2, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(4, 3, 0, 0, 0, 0, 0, 0, 0),
    intArrayOf(4, 3, 2, 0, 0, 0, 0, 0, 0),
    intArrayOf(4, 3, 3, 0, 0, 0, 0, 0, 0),
    intArrayOf(4, 3, 3, 1, 0, 0, 0, 0, 0),
    intArrayOf(4, 3, 3, 2, 0, 0, 0, 0, 0),
    intArrayOf(4, 3, 3, 3, 1, 0, 0, 0, 0),
    intArrayOf(4, 3, 3, 3, 2, 0, 0, 0, 0),
    intArrayOf(4, 3, 3, 3, 2, 1, 0, 0, 0),
    intArrayOf(4, 3, 3, 3, 2, 1, 0, 0, 0),
    intArrayOf(4, 3, 3, 3, 2, 1, 1, 0, 0),
    intArrayOf(4, 3, 3, 3, 2, 1, 1, 0, 0),
    intArrayOf(4, 3, 3, 3, 2, 1, 1, 1, 0),
    intArrayOf(4, 3, 3, 3, 2, 1, 1, 1, 0),
    intArrayOf(4, 3, 3, 3, 2, 1, 1, 1, 1),
    intArrayOf(4, 3, 3, 3, 3, 1, 1, 1, 1),
    intArrayOf(4, 3, 3, 3, 3, 2, 1, 1, 1),
    intArrayOf(4, 3, 3, 3, 3, 2, 2, 1, 1),
)

enum class ModifierStackType { UNTYPED, CIRCUMSTANCE, ITEM, STATUS }

data class TypedModifier(val value: Int, val type: ModifierStackType)

object CharacterValidator {
    fun validate(build: CharacterBuild, state: CharacterState? = null): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (build.name.isBlank()) issues += ValidationIssue("name.blank", "Character name is required", "name")
        if (build.level !in 1..20) {
            issues += ValidationIssue("level.range", "Level must be between 1 and 20", "level")
        }
        when (val rules = build.rules) {
            is FiveEBuildData -> {
                if (rules.classes.isEmpty()) {
                    issues += ValidationIssue("classes.empty", "A fifth-edition character needs a class", "rules.classes")
                }
                if (rules.classes.any { it.levels <= 0 }) {
                    issues += ValidationIssue("classes.non_positive", "Every class entry needs at least one level", "rules.classes")
                }
                if (!rules.optionalRules.multiclassingEnabled && rules.classes.size > 1) {
                    issues += ValidationIssue("multiclass.disabled", "This character has multiclassing disabled", "rules.classes")
                }
            }
            is Pf2eBuildData -> {
                if (rules.classId.isBlank()) {
                    issues += ValidationIssue("class.missing", "A PF2e character needs a class", "rules.classId")
                }
                if (rules.heritageId.isBlank()) {
                    issues += ValidationIssue("heritage.missing", "A PF2e character needs a heritage", "rules.heritageId")
                }
            }
        }
        Ability.entries.forEach { ability ->
            val score = build.abilities[ability]
            if (score == null) {
                issues += ValidationIssue("ability.missing", "$ability is missing", "abilities.$ability")
            } else if (score !in 1..30) {
                issues += ValidationIssue("ability.range", "$ability must be between 1 and 30", "abilities.$ability")
            }
        }
        if (state != null) {
            if (state.characterId != build.id) {
                issues += ValidationIssue("state.character", "State belongs to another character", severity = Severity.ERROR)
            }
            if (state.maximumHitPoints < 1) {
                issues += ValidationIssue("hp.maximum", "Maximum hit points must be positive", "maximumHitPoints")
            }
            if (state.maximumHitPointReduction !in 0..state.maximumHitPoints.coerceAtLeast(0)) {
                issues += ValidationIssue("hp.maximum_reduction", "Maximum hit point reduction must be within the base maximum", "maximumHitPointReduction")
            }
            if (state.temporaryHitPoints < 0) {
                issues += ValidationIssue("hp.temporary", "Temporary hit points cannot be negative", "temporaryHitPoints")
            }
            val effectiveMaximum = state.effectiveMaximumHitPoints(build.ruleset)
            if (state.currentHitPoints !in 0..effectiveMaximum) {
                issues += ValidationIssue("hp.current", "Current hit points must be within the valid range", "currentHitPoints")
            }
            when (val health = state.health) {
                is FiveEHealthState -> {
                    if (!build.ruleset.isFiveEdition) {
                        issues += ValidationIssue("health.ruleset", "Fifth-edition health is attached to a non-fifth-edition build", "health")
                    }
                    if (health.deathSaveSuccesses !in 0..2 || health.deathSaveFailures !in 0..3) {
                        issues += ValidationIssue("death_saves.range", "Death-save counters are outside their valid range", "health")
                    }
                    if (health.exhaustionLevel !in 0..6) {
                        issues += ValidationIssue("exhaustion.range", "Exhaustion must be between zero and six", "health.exhaustionLevel")
                    }
                }
                is Pf2eHealthState -> {
                    if (build.ruleset != RulesetId.PF2E_REMASTER) {
                        issues += ValidationIssue("health.ruleset", "PF2e health is attached to a fifth-edition build", "health")
                    }
                    if (health.dying < 0 || health.wounded < 0 || health.doomed < 0 || health.heroPoints < 0) {
                        issues += ValidationIssue("dying.range", "PF2e health values cannot be negative", "health")
                    }
                }
            }
            state.resources.filter { it.maximum < 0 || it.current !in 0..it.maximum }.forEach {
                issues += ValidationIssue("resource.range", "Resource ${it.label} is outside its range", "resources.${it.id}")
            }
            CoinDenomination.entries.filter { state.currency.balance(it) < 0 }.forEach { denomination ->
                issues += ValidationIssue(
                    "currency.negative",
                    "$denomination currency cannot be negative",
                    "currency.${denomination.name.lowercase()}",
                )
            }
        }
        return issues
    }
}
