package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.CombatContribution
import app.dulliesanddungeons.domain.CombatContributionTiming
import app.dulliesanddungeons.domain.CombatContributionType
import app.dulliesanddungeons.domain.ActionCost

internal data class PotentialSourceUi(
    val label: String,
    val count: Int,
    val timing: CombatContributionTiming,
    val requiresAttackAction: Boolean = false,
    val requiresActionCantripForAnotherCast: Boolean = false,
    val requiresSetup: Boolean = false,
    val setupUsesConcentration: Boolean = false,
    val castsSpellThisTurn: Boolean = false,
    val requiresHit: Boolean = false,
    val requiresAdditionalTarget: Boolean = false,
    val variable: Boolean = false,
    val resourceName: String = "",
    val resourceCost: Int = 0,
    val note: String = "",
)

internal data class TurnPotentialUi(
    val baseCount: Int,
    val extraCount: Int,
    val baseLabel: String,
    val bestSources: List<PotentialSourceUi>,
    val alternatives: List<PotentialSourceUi>,
    val variableSources: List<PotentialSourceUi>,
) {
    val compactLabel: String get() = "$baseCount + $extraCount"
}

/**
 * Computes a rules-aware maximum, assuming full resources and satisfiable stated conditions.
 * Reactions, save-only effects, summons, and attacks made by controlled creatures are excluded.
 */
internal object CombatPotentialEngine {
    fun standardSpellActivationCost(id: String): ActionCost = when (normalizeSpellId(id)) {
        in reactionSpellIds -> ActionCost(reactions = 1)
        in bonusActionSpellIds -> ActionCost(bonusActions = 1)
        else -> ActionCost(actions = 1)
    }

    fun attacks(character: CharacterUi, conditions: List<ConditionUi> = emptyList()): TurnPotentialUi {
        if (character.ruleset == Ruleset.Pf2eRemaster) return emptyPotential("Attack action")

        val base = attacksPerAction(character)
        val fixed = mutableListOf<PotentialSourceUi>()
        val bonus = mutableListOf<PotentialSourceUi>()
        val alternatives = mutableListOf<PotentialSourceUi>()
        val variable = mutableListOf<PotentialSourceUi>()

        fun add(source: PotentialSourceUi) {
            when {
                source.variable -> variable += source
                source.timing == CombatContributionTiming.BONUS_ACTION -> bonus += source
                source.timing == CombatContributionTiming.ACTION -> alternatives += source
                else -> fixed += source
            }
        }

        val actionSurgeAvailable = classLevel(character, "Fighter") >= 2 || character.hasFeature("action-surge", "Action Surge")

        val monkLevel = classLevel(character, "Monk")
        if (monkLevel >= 1) {
            add(PotentialSourceUi("Martial Arts", 1, CombatContributionTiming.BONUS_ACTION, requiresAttackAction = character.ruleset == Ruleset.Fifth2014, note = "Unarmed Strike"))
        }
        if (monkLevel >= 2) {
            val flurryCount = if (character.ruleset == Ruleset.Fifth2024 && monkLevel >= 10) 3 else 2
            add(
                PotentialSourceUi(
                    "Flurry of Blows",
                    flurryCount,
                    CombatContributionTiming.BONUS_ACTION,
                    requiresAttackAction = character.ruleset == Ruleset.Fifth2014,
                    resourceName = if (character.ruleset == Ruleset.Fifth2014) "Ki Point" else "Focus Point",
                    resourceCost = 1,
                    note = "Unarmed Strikes",
                )
            )
        }

        val usableWeapons = character.weapons.filter(::isUsable)
        val lightWeapons = usableWeapons.count { weapon -> weapon.properties.containsWord("light") }
        if (lightWeapons >= 2) {
            val nick = character.ruleset == Ruleset.Fifth2024 && canUseWeaponMastery(character) &&
                usableWeapons.any { it.mastery.equals("Nick", true) }
            add(
                PotentialSourceUi(
                    if (nick) "Light property · Nick" else "Light weapon attack",
                    1,
                    if (nick) CombatContributionTiming.ATTACK_ACTION else CombatContributionTiming.BONUS_ACTION,
                    requiresAttackAction = true,
                    note = if (nick) "Nick leaves the Bonus Action free" else "Requires two eligible Light weapons",
                )
            )
        }
        if (
            character.ruleset == Ruleset.Fifth2024 && canUseWeaponMastery(character) &&
            usableWeapons.any { it.mastery.equals("Cleave", true) }
        ) {
            add(PotentialSourceUi("Cleave mastery", 1, CombatContributionTiming.TRIGGERED, requiresHit = true, requiresAdditionalTarget = true))
        }

        if (character.ruleset == Ruleset.Fifth2014 && character.hasFeature("frenzy", "Frenzy")) {
            add(PotentialSourceUi("Frenzy", 1, CombatContributionTiming.BONUS_ACTION, requiresSetup = true, note = "While raging"))
        }
        if (character.hasFeature("horde-breaker", "Horde Breaker")) {
            add(PotentialSourceUi("Horde Breaker", 1, CombatContributionTiming.TRIGGERED, requiresAdditionalTarget = true))
        }
        if (character.features.any { it.name.equals("Multiattack", true) }) {
            add(PotentialSourceUi("Multiattack", 0, CombatContributionTiming.ACTION, variable = true, requiresAdditionalTarget = true, note = "Depends on the selected option and number of valid targets"))
        }

        val hasHaste = conditions.any { it.name.equals("Haste", true) || it.id.contains("haste", true) } ||
            character.availableSpells.any { it.isNamedSpell("haste") && spellUsable(character, it) }
        if (hasHaste) {
            add(PotentialSourceUi("Haste", 1, CombatContributionTiming.EXTRA_ACTION, requiresSetup = true, setupUsesConcentration = true, note = "Hasted action is limited to one weapon attack · Concentration"))
        }

        character.resolvedEquipment.filter(::isUsable).forEach { item ->
            when {
                item.name.equals("Scimitar of Speed", true) -> add(PotentialSourceUi(item.name, 1, CombatContributionTiming.BONUS_ACTION))
                item.name.equals("Dancing Sword", true) -> add(PotentialSourceUi(item.name, 1, CombatContributionTiming.BONUS_ACTION, requiresSetup = true))
            }
        }

        if (character.hasFeature("metamagic-quickened", "Quickened Spell")) {
            character.availableSpells.filter { spellUsable(character, it) && it.isTurnActionSpell(character.ruleset) }.maxByOrNull { attackRollCount(character, it) }
                ?.takeIf { attackRollCount(character, it) > 0 }
                ?.let { spell ->
                    add(
                        PotentialSourceUi(
                            "Quickened Spell · ${spell.name}",
                            attackRollCount(character, spell),
                            CombatContributionTiming.BONUS_ACTION,
                            castsSpellThisTurn = true,
                            resourceName = "Sorcery Points",
                            resourceCost = 2,
                        )
                    )
                }
            if (character.ruleset == Ruleset.Fifth2024 && character.availableSpells.any { spellUsable(character, it) && it.isNamedSpell("chromatic-orb") }) {
                add(
                    PotentialSourceUi(
                        "Quickened Spell · Chromatic Orb chain",
                        0,
                        CombatContributionTiming.BONUS_ACTION,
                        variable = true,
                        requiresAdditionalTarget = true,
                        resourceName = "Sorcery Points",
                        resourceCost = 2,
                        note = "Additional attack rolls depend on matching damage dice, slot level, and valid targets",
                    )
                )
            }
        }

        character.availableSpells.filter { spellUsable(character, it) && it.isBonusActionSpell() }.forEach { spell ->
            val attacks = attackRollCount(character, spell)
            if (attacks > 0) add(PotentialSourceUi(spell.name, attacks, CombatContributionTiming.BONUS_ACTION, castsSpellThisTurn = true))
        }
        character.availableSpells.filter { spellUsable(character, it) && it.isNamedSpell("arcane-sword") }.forEach { spell ->
            add(PotentialSourceUi(spell.name, 1, CombatContributionTiming.BONUS_ACTION, requiresSetup = true, setupUsesConcentration = true, note = "Ongoing spell · Concentration"))
        }

        structuredSources(character, CombatContributionType.EXTRA_ATTACKS).forEach(::add)

        val fixedUsesConcentration = fixed.any { it.setupUsesConcentration }
        val compatibleBonus = bonus.filterNot { fixedUsesConcentration && it.setupUsesConcentration }
        val bestBonus = compatibleBonus.maxWithOrNull(compareBy<PotentialSourceUi> { it.count }.thenBy { it.label })
        bestBonus?.let(fixed::add)
        alternatives += bonus.filterNot { it === bestBonus }
        if (actionSurgeAvailable) {
            val actionSpell = if (character.ruleset == Ruleset.Fifth2014) {
                character.availableSpells
                    .filter { spellUsable(character, it) && !it.isBonusActionSpell() && !it.isReactionSpell() }
                    .filter { it.isTurnActionSpell(character.ruleset) }
                    .filter { bestBonus?.castsSpellThisTurn != true || it.level == 0 }
                    .maxByOrNull { attackRollCount(character, it) }
            } else null
            val actionSpellAttacks = actionSpell?.let { attackRollCount(character, it) } ?: 0
            val surgeCount = maxOf(base, actionSpellAttacks)
            fixed += PotentialSourceUi(
                label = if (actionSpellAttacks > base) "Action Surge · ${actionSpell?.name}" else "Action Surge",
                count = surgeCount,
                timing = CombatContributionTiming.EXTRA_ACTION,
                resourceName = "Action Surge",
                resourceCost = 1,
                castsSpellThisTurn = actionSpellAttacks > base,
            )
        }
        return TurnPotentialUi(
            baseCount = base,
            extraCount = fixed.sumOf(PotentialSourceUi::count),
            baseLabel = attackActionLabel(character, base),
            bestSources = fixed,
            alternatives = alternatives.sortedByDescending(PotentialSourceUi::count),
            variableSources = variable,
        )
    }

    fun casts(character: CharacterUi): TurnPotentialUi {
        if (character.ruleset == Ruleset.Pf2eRemaster) return emptyPotential("Magic action")
        val available = character.availableSpells.filter { spellUsable(character, it) && !it.isReactionSpell() }
        val actionSpells = available.filter { !it.isBonusActionSpell() && it.isTurnActionSpell(character.ruleset) }
        val cantrips = actionSpells.filter { it.level == 0 }
        val base = if (actionSpells.isNotEmpty()) 1 else 0
        val best = mutableListOf<PotentialSourceUi>()
        val alternatives = mutableListOf<PotentialSourceUi>()

        val bonusOptions = available.filter { it.isBonusActionSpell() }.map { spell ->
            PotentialSourceUi(
                spell.name,
                1,
                CombatContributionTiming.BONUS_ACTION,
                requiresActionCantripForAnotherCast = character.ruleset == Ruleset.Fifth2014 || spell.level > 0,
            )
        }.toMutableList()
        if (character.hasFeature("metamagic-quickened", "Quickened Spell") && actionSpells.isNotEmpty()) {
            bonusOptions += PotentialSourceUi(
                "Quickened Spell",
                1,
                CombatContributionTiming.BONUS_ACTION,
                requiresActionCantripForAnotherCast = true,
                resourceName = "Sorcery Points",
                resourceCost = 2,
            )
        }
        structuredSources(character, CombatContributionType.EXTRA_CASTS).forEach { source ->
            if (source.variable) return@forEach
            if (source.timing == CombatContributionTiming.BONUS_ACTION) bonusOptions += source else best += source
        }

        // Both supported fifth-edition revisions permit the two-cast branch when the other cast is
        // an action cantrip. This also honors 2024's one-spell-slot-per-turn limit.
        val eligibleBonusOptions = bonusOptions.filter {
            base == 0 || !it.requiresActionCantripForAnotherCast || cantrips.isNotEmpty()
        }
        val bestBonus = eligibleBonusOptions.maxWithOrNull(compareBy<PotentialSourceUi> { it.count }.thenBy { it.label })
        bestBonus?.let(best::add)
        alternatives += bonusOptions.filterNot { it === bestBonus }

        if (character.ruleset == Ruleset.Fifth2014 && base > 0 &&
            (classLevel(character, "Fighter") >= 2 || character.hasFeature("action-surge", "Action Surge"))
        ) {
            best += PotentialSourceUi("Action Surge", 1, CombatContributionTiming.EXTRA_ACTION, resourceName = "Action Surge", resourceCost = 1)
        }

        return TurnPotentialUi(
            baseCount = base,
            extraCount = best.sumOf(PotentialSourceUi::count),
            baseLabel = if (character.ruleset == Ruleset.Fifth2014) "Cast a Spell action" else "Magic action",
            bestSources = best,
            alternatives = alternatives.sortedBy { it.label },
            variableSources = emptyList(),
        )
    }

    fun attacksPerAction(character: CharacterUi): Int {
        if (character.ruleset == Ruleset.Pf2eRemaster) return 1
        val fighter = classLevel(character, "Fighter")
        var result = when {
            fighter >= 20 -> 4
            fighter >= 11 -> 3
            fighter >= 5 -> 2
            listOf("Barbarian", "Monk", "Paladin", "Ranger").any { classLevel(character, it) >= 5 } -> 2
            else -> 1
        }
        if (character.hasFeature("devouring-blade", "Devouring Blade")) result = maxOf(result, 3)
        if (character.hasFeature("thirsting-blade", "Thirsting Blade")) result = maxOf(result, 2)
        structuredContributions(character).filter { it.type == CombatContributionType.ATTACK_ACTION_COUNT }
            .forEach { result = maxOf(result, it.count.coerceAtLeast(1)) }
        return result
    }

    private fun structuredSources(character: CharacterUi, type: CombatContributionType): List<PotentialSourceUi> =
        labeledStructuredContributions(character).filter { it.second.type == type }.map { (label, contribution) ->
            PotentialSourceUi(
                label = label,
                count = contribution.count.coerceAtLeast(0),
                timing = contribution.timing,
                requiresAttackAction = contribution.requiresAttackAction,
                requiresActionCantripForAnotherCast = contribution.requiresActionCantripForAnotherCast,
                requiresSetup = contribution.requiresSetup,
                setupUsesConcentration = contribution.setupUsesConcentration,
                castsSpellThisTurn = contribution.castsSpellThisTurn,
                requiresHit = contribution.requiresHit,
                requiresAdditionalTarget = contribution.requiresAdditionalTarget,
                variable = contribution.variable,
                resourceName = contribution.resourceName,
                resourceCost = contribution.resourceCost,
                note = contribution.note,
            )
        }

    private fun labeledStructuredContributions(character: CharacterUi): List<Pair<String, CombatContribution>> {
        val usableWeapons = character.weapons.filter(::isUsable)
        fun applicable(contribution: CombatContribution): Boolean = contribution.requiredWeaponProperties.isEmpty() ||
            usableWeapons.any { weapon ->
                contribution.requiredWeaponProperties.all { required -> weapon.properties.containsWord(required) }
            }
        return buildList {
            character.features.forEach { owner -> owner.combatContributions.forEach { add(owner.name to it) } }
            character.availableSpells.forEach { owner -> owner.combatContributions.forEach { add(owner.name to it) } }
            usableWeapons.forEach { owner -> owner.combatContributions.forEach { add(owner.name to it) } }
            character.resolvedEquipment.filter(::isUsable).forEach { owner -> owner.combatContributions.forEach { add(owner.name to it) } }
        }.filter { applicable(it.second) }
    }

    private fun structuredContributions(character: CharacterUi): List<CombatContribution> =
        labeledStructuredContributions(character).map { it.second }

    private fun classLevel(character: CharacterUi, name: String): Int =
        character.progression.count { it.className.equals(name, true) }.takeIf { it > 0 }
            ?: character.level.takeIf { character.className.equals(name, true) }
            ?: 0

    private fun attackActionLabel(character: CharacterUi, count: Int): String {
        val local = labeledStructuredContributions(character)
            .filter { it.second.type == CombatContributionType.ATTACK_ACTION_COUNT && it.second.count == count }
            .firstOrNull()?.first
        if (local != null) return "Attack action · $local"
        return when {
            classLevel(character, "Fighter") >= 5 -> "Attack action · Fighter Extra Attack"
            listOf("Barbarian", "Monk", "Paladin", "Ranger").firstOrNull { classLevel(character, it) >= 5 } != null -> {
                val source = listOf("Barbarian", "Monk", "Paladin", "Ranger").first { classLevel(character, it) >= 5 }
                "Attack action · $source Extra Attack"
            }
            character.hasFeature("devouring-blade", "Devouring Blade") -> "Attack action · Devouring Blade"
            character.hasFeature("thirsting-blade", "Thirsting Blade") -> "Attack action · Thirsting Blade"
            else -> "Attack action"
        }
    }

    private fun CharacterUi.hasFeature(idPart: String, name: String): Boolean = features.any {
        it.id.contains(idPart, true) || it.name.equals(name, true)
    }

    private fun canUseWeaponMastery(character: CharacterUi): Boolean =
        listOf("Barbarian", "Fighter", "Paladin", "Ranger", "Rogue").any { classLevel(character, it) > 0 } ||
            character.features.any { it.name.contains("Weapon Mastery", true) }

    private fun isUsable(weapon: WeaponUi): Boolean = !weapon.needsAttunement || weapon.attuned
    private fun isUsable(item: EquipmentUi): Boolean = !item.needsAttunement || item.attuned
    private fun String.containsWord(value: String): Boolean = split(',', ';').any { it.trim().equals(value, true) }

    private fun spellUsable(character: CharacterUi, spell: SpellUi): Boolean = spell.level == 0 ||
        spell.sourceKind != SpellSourceKind.CLASS || character.resolvedSpellSlots.any { it.level >= spell.level && it.maximum > 0 } ||
        character.hasSpellcastingCapability

    private fun SpellUi.normalizedId(): String = normalizeSpellId(id).ifBlank { normalizedSpellName() }
    private fun SpellUi.normalizedSpellName(): String = name.lowercase().replace("'", "").replace(' ', '-')
    private fun SpellUi.isNamedSpell(id: String): Boolean = normalizedId() == id || normalizedSpellName() == id

    private fun SpellUi.isBonusActionSpell(): Boolean = activationCost.bonusActions > 0 || bonusActionSpellIds.any { isNamedSpell(it) }
    private fun SpellUi.isReactionSpell(): Boolean = activationCost.reactions > 0 || reactionSpellIds.any { isNamedSpell(it) }
    private fun SpellUi.isTurnActionSpell(ruleset: Ruleset): Boolean = activationCost.actions > 0 &&
        longCastingSpellIds.none { isNamedSpell(it) } &&
        (ruleset != Ruleset.Fifth2014 || !isNamedSpell("conjure-elemental"))

    private fun attackRollCount(character: CharacterUi, spell: SpellUi): Int {
        val level = character.level
        return when {
            spell.isNamedSpell("eldritch-blast") -> when {
                level >= 17 -> 4
                level >= 11 -> 3
                level >= 5 -> 2
                else -> 1
            }
            spell.isNamedSpell("scorching-ray") -> 3 + ((character.resolvedSpellSlots.maxOfOrNull { it.level } ?: spell.level) - 2).coerceAtLeast(0)
            listOf("spiritual-weapon", "arcane-sword", "flame-blade").any { spell.isNamedSpell(it) } -> 1
            else -> if (spell.spellAttack) 1 else 0
        }
    }

    private fun emptyPotential(label: String) = TurnPotentialUi(0, 0, label, emptyList(), emptyList(), emptyList())

    private fun normalizeSpellId(id: String): String = id.substringAfterLast(':').removePrefix("spell.").removePrefix("spell-")
        .lowercase().replace('_', '-')

    private val reactionSpellIds = setOf("counterspell", "feather-fall", "hellish-rebuke", "shield")
    private val longCastingSpellIds = setOf(
        "alarm", "animate-dead", "awaken", "clone", "commune", "commune-with-nature",
        "contact-other-plane", "create-undead", "fabricate", "find-familiar", "find-steed", "forbiddance",
        "geas", "glyph-of-warding", "guards-and-wards", "hallow", "heroes-feast", "identify", "legend-lore",
        "magic-circle", "magnificent-mansion", "planar-ally", "planar-binding", "prayer-of-healing",
        "private-sanctum", "raise-dead", "reincarnate", "resurrection", "scrying", "simulacrum", "symbol",
        "teleportation-circle", "tiny-hut", "true-resurrection",
    )
    private val bonusActionSpellIds = setOf(
        "banishing-smite", "blinding-smite", "branding-smite", "compelled-duel", "divine-favor", "divine-smite",
        "divine-word", "ensnaring-strike", "expeditious-retreat", "grasping-vine", "hail-of-thorns",
        "healing-word", "hex", "hunter-s-mark", "hunters-mark", "lightning-arrow", "magic-weapon", "mass-healing-word",
        "misty-step", "sanctuary", "searing-smite", "shield-of-faith", "shillelagh", "shining-smite",
        "spiritual-weapon", "staggering-smite", "swift-quiver", "thunderous-smite", "wrathful-smite",
    )
}
