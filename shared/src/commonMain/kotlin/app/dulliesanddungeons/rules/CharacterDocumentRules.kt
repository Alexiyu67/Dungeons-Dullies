package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.CharacterDocument
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.ValidationIssue

/** Cross-record invariants for the canonical persistence document. */
object CharacterDocumentValidator {
    fun validate(document: CharacterDocument): List<ValidationIssue> = buildList {
        addAll(CharacterValidator.validate(document.build, document.state))
        if (document.schemaVersion < 1) {
            add(ValidationIssue("schema.invalid", "Schema version must be positive", "schemaVersion"))
        }
        document.state.activeTurn?.let { turn ->
            if (turn.characterId != document.build.id) {
                add(ValidationIssue("turn.character", "Turn draft belongs to another character", "state.activeTurn.characterId"))
            }
            if (turn.ruleset != document.build.ruleset) {
                add(ValidationIssue("turn.ruleset", "Turn draft uses another ruleset", "state.activeTurn.ruleset"))
            }
        }
        document.state.activeLevelUp?.let { draft ->
            if (draft.characterId != document.build.id) {
                add(ValidationIssue("level_up.character", "Level-up draft belongs to another character", "state.activeLevelUp.characterId"))
            }
            if (draft.ruleset != document.build.ruleset) {
                add(ValidationIssue("level_up.ruleset", "Level-up draft uses another ruleset", "state.activeLevelUp.ruleset"))
            }
        }
        duplicateIds(document.state.resources.map { it.id }).forEach {
            add(ValidationIssue("resource.duplicate", "Resource ID $it is duplicated", "state.resources"))
        }
        duplicateIds(document.state.equipment.map { it.id }).forEach {
            add(ValidationIssue("equipment.duplicate", "Equipment instance ID $it is duplicated", "state.equipment"))
        }
        duplicateIds(document.state.conditions.map { it.instanceId }).forEach {
            add(ValidationIssue("condition.duplicate", "Condition instance ID $it is duplicated", "state.conditions"))
        }
        val sessions = listOfNotNull(document.state.activePlaySession) + document.state.savedPlaySessions
        duplicateIds(sessions.map { it.id }).forEach {
            add(ValidationIssue("session.duplicate", "Play session ID $it is duplicated", "state.savedPlaySessions"))
        }
        sessions.forEach { session ->
            duplicateIds(session.activities.map { it.id }).forEach {
                add(ValidationIssue("activity.duplicate", "Activity ID $it is duplicated", "state.savedPlaySessions.activities"))
            }
            if (session.currentTurnNumber < 1 || session.activities.any { (it.turnNumber ?: 1) < 1 }) {
                add(ValidationIssue("session.turn", "Session turn numbers must be positive", "state.savedPlaySessions"))
            }
        }
        duplicateIds(document.sheet.weapons.map { it.id }).forEach {
            add(ValidationIssue("weapon.duplicate", "Weapon record ID $it is duplicated", "sheet.weapons"))
        }
        duplicateIds(document.sheet.spells.map { it.id }).forEach {
            add(ValidationIssue("spell.duplicate", "Spell record ID $it is duplicated", "sheet.spells"))
        }
        duplicateIds(document.sheet.features.map { it.id }).forEach {
            add(ValidationIssue("feature.duplicate", "Feature record ID $it is duplicated", "sheet.features"))
        }
        duplicateIds(document.sheet.noteEntries.map { it.id }).forEach {
            add(ValidationIssue("note.duplicate", "Note ID $it is duplicated", "sheet.noteEntries"))
        }
        validateProgression(document).forEach { add(it) }
    }

    private fun validateProgression(document: CharacterDocument): List<ValidationIssue> {
        val entries = document.progression.entries
        val issues = mutableListOf<ValidationIssue>()
        val baseline = document.progression.baselineLevel
        if (baseline !in 0..document.build.level) {
            issues += ValidationIssue(
                "progression.baseline",
                "Progression baseline must be within the character's level range",
                "progression.baselineLevel",
            )
        }
        val levels = entries.map { it.characterLevel }
        val expectedLevels = if (baseline < document.build.level) {
            (baseline + 1..document.build.level).toList()
        } else {
            emptyList()
        }
        if (levels != expectedLevels) {
            issues += ValidationIssue(
                "progression.sequence",
                "Progression entries must contain each character level exactly once and in order",
                "progression.entries",
            )
        }
        if (entries.any { it.classLevel < 1 || it.characterLevel < 1 }) {
            issues += ValidationIssue(
                "progression.level",
                "Progression levels must be positive",
                "progression.entries",
            )
        }
        val rules = document.build.rules
        if (rules is FiveEBuildData) {
            val classIds = rules.classes.mapTo(mutableSetOf()) { it.classId }
            rules.classHitDieOverrides.forEach { (classId, sides) ->
                if (classId !in classIds || sides !in setOf(6, 8, 10, 12)) {
                    issues += ValidationIssue(
                        "progression.hit_die_override",
                        "Hit Die overrides must reference a current class and use a supported die",
                        "build.rules.classHitDieOverrides",
                    )
                }
            }
            val recorded = document.progression.baselineClassLevels.toMutableMap()
            entries.groupingBy { it.classId }.eachCount().forEach { (id, levels) ->
                recorded[id] = recorded.getOrDefault(id, 0) + levels
            }
            recorded.entries.removeAll { it.value == 0 }
            val expected = rules.classes.associate { it.classId to it.levels }
            if (recorded != expected) {
                issues += ValidationIssue(
                    "progression.classes",
                    "Progression class choices do not match the final class levels",
                    "progression.entries",
                )
            }
        }
        return issues
    }

    private fun duplicateIds(ids: List<String>): Set<String> =
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
}
