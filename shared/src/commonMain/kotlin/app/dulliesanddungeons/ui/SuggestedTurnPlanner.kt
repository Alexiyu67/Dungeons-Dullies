package app.dulliesanddungeons.ui

/**
 * A short, character-aware guide. It never predicts a target number or spends a resource; each
 * line routes to the real part of the turn where the player makes that choice.
 */
object SuggestedTurnPlanner {
    fun build(character: CharacterUi, session: TurnSession?): List<SuggestedTurnStepUi> {
        val availableFeatures = character.features.filter {
            it.turnGuideEligible && (it.remaining == null || it.remaining > 0)
        }
        val bonusFeature = availableFeatures.firstOrNull { it.actionCost.bonusActions > 0 }
        val resourceFeature = availableFeatures.firstOrNull {
            (it.remaining != null || it.actionCost.actions > 0 || it.actionCost.reactions > 0) &&
                it.actionCost.bonusActions == 0 &&
                it.effect != FeatureEffect.EXTRA_ACTION
        }
        val preparedSpell = character.availableSpells.firstOrNull { it.prepared }
        val weapon = character.weapons.firstOrNull()
        val preferSpell = preparedSpell != null && (weapon == null || character.className in fullCasters)

        return buildList {
            if ((session?.remainingMovement ?: character.speedFeet) > 0) {
                add(
                    SuggestedTurnStepUi(
                        id = "move",
                        title = if (character.flySpeedFeet != null) "Move or fly into position" else "Move into position",
                        subtitle = "Use only the movement you need; keep a safe route out",
                        section = TurnSection.Move,
                    )
                )
            }

            if (preferSpell) {
                val spell = checkNotNull(preparedSpell)
                add(
                    SuggestedTurnStepUi(
                        id = "spell",
                        title = "Cast ${spell.name}",
                        subtitle = spell.sourceName.takeIf { it.isNotBlank() }?.let { "${spell.summary} · from $it" }
                            ?: spell.summary,
                        section = TurnSection.Spell,
                        cost = spell.activationCost,
                    )
                )
            } else if (weapon != null) {
                add(
                    SuggestedTurnStepUi(
                        id = "attack",
                        title = "Attack with ${weapon.name}",
                        subtitle = "${signed(weapon.attackBonus)} to hit · ${weapon.damage} ${weapon.damageType.lowercase()}",
                        section = TurnSection.Attack,
                        weaponId = weapon.id,
                        cost = app.dulliesanddungeons.domain.ActionCost(actions = 1, attacks = 1),
                    )
                )
                if (character.className == "Fighter" && character.level >= 5) {
                    add(
                        SuggestedTurnStepUi(
                            id = "extra-attack",
                            title = "Make your second attack",
                            subtitle = "Extra Attack is part of the same action",
                            section = TurnSection.Attack,
                            weaponId = weapon.id,
                            cost = app.dulliesanddungeons.domain.ActionCost(attacks = 1),
                        )
                    )
                }
            } else if (preparedSpell != null) {
                add(SuggestedTurnStepUi("spell", "Cast ${preparedSpell.name}", preparedSpell.summary, TurnSection.Spell, cost = preparedSpell.activationCost))
            } else {
                add(SuggestedTurnStepUi("other-action", "Choose a useful action", "Dash, Dodge, Help, or interact with the scene", TurnSection.Other, cost = app.dulliesanddungeons.domain.ActionCost(actions = 1)))
            }

            resourceFeature?.let { feature ->
                add(
                    SuggestedTurnStepUi(
                        id = "feature",
                        title = "Consider ${feature.name}",
                        subtitle = feature.summary,
                        section = TurnSection.Other,
                        featureId = feature.id,
                        cost = feature.actionCost,
                    )
                )
            }

            bonusFeature?.let { feature ->
                add(
                    SuggestedTurnStepUi(
                        id = "bonus",
                        title = "Use ${feature.name} if needed",
                        subtitle = "Bonus action · ${feature.summary}",
                        section = TurnSection.Other,
                        featureId = feature.id,
                        cost = feature.actionCost,
                    )
                )
            }

            val onHitText = when {
                character.features.any { it.effect == FeatureEffect.OPEN_HAND } -> "On a qualifying hit, choose an Open Hand effect"
                weapon?.mastery?.isNotBlank() == true -> "On a hit, apply ${weapon.mastery} if its condition is met"
                weapon?.properties?.contains("grapple", ignoreCase = true) == true -> "On a hit, decide whether to grapple or shove"
                else -> null
            }
            onHitText?.let {
                add(SuggestedTurnStepUi("on-hit", it, "Resolve the effect after the attack result", TurnSection.Attack, weaponId = weapon?.id, cost = app.dulliesanddungeons.domain.ActionCost(attacks = 1)))
            }
        }.take(5)
    }

    private val fullCasters = setOf("Wizard", "Cleric", "Druid", "Bard", "Sorcerer", "Warlock")
    private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
}
