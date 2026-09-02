package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.CharacterBuild
import app.dulliesanddungeons.domain.CharacterDocument
import app.dulliesanddungeons.domain.ClassDefinition
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.HitPointGainMethod
import app.dulliesanddungeons.domain.LevelProgressionEntry
import app.dulliesanddungeons.domain.ValidationIssue

object LevelUpRules {
    fun validateTransition(
        current: CharacterDocument,
        targetBuild: CharacterBuild,
        entry: LevelProgressionEntry,
        classes: Map<String, ClassDefinition>,
    ): List<ValidationIssue> = buildList {
        if (targetBuild.id != current.build.id || targetBuild.ruleset != current.build.ruleset) {
            add(ValidationIssue("level_up.identity", "Level up must preserve character ID and ruleset"))
            return@buildList
        }
        if (targetBuild.level != current.build.level + 1 || entry.characterLevel != targetBuild.level) {
            add(ValidationIssue("level_up.level", "A level-up transition must add exactly one character level"))
        }
        val before = current.build.classes.associate { it.classId to it.levels }
        val after = targetBuild.classes.associate { it.classId to it.levels }
        val changed = (before.keys + after.keys).filter { after.getOrDefault(it, 0) - before.getOrDefault(it, 0) != 0 }
        if (changed.size != 1 || after.getOrDefault(entry.classId, 0) - before.getOrDefault(entry.classId, 0) != 1) {
            add(ValidationIssue("level_up.class_delta", "Exactly one class must gain one level", "progression.classId"))
        }
        if (entry.classLevel != after.getOrDefault(entry.classId, 0)) {
            add(ValidationIssue("level_up.class_level", "Recorded class level does not match the target build"))
        }
        val classDefinition = classes[entry.classId]
        if (classDefinition == null || classDefinition.ruleset != targetBuild.ruleset) {
            add(ValidationIssue("level_up.class_definition", "The selected class is unavailable for this ruleset"))
        } else {
            if (entry.classId !in before && current.build.rules is FiveEBuildData) {
                val rules = current.build.rules as FiveEBuildData
                if (!rules.optionalRules.multiclassingEnabled) {
                    add(ValidationIssue("multiclass.disabled", "Multiclassing is disabled for this character"))
                } else if (!classDefinition.multiclassPrerequisite.matches(current.build, current.state)) {
                    add(ValidationIssue("multiclass.prerequisite", "Multiclass prerequisites are not met"))
                }
            }
            val expectedHitDie = (targetBuild.rules as? FiveEBuildData)
                ?.classHitDieOverrides
                ?.get(entry.classId)
                ?: classDefinition.hitDieSides
            validateHitPoints(entry, expectedHitDie).forEach { add(it) }
        }
    }

    fun apply(
        current: CharacterDocument,
        targetBuild: CharacterBuild,
        entry: LevelProgressionEntry,
        classes: Map<String, ClassDefinition>,
    ): CharacterDocument {
        val issues = validateTransition(current, targetBuild, entry, classes)
        require(issues.isEmpty()) { issues.joinToString { it.code } }
        val gain = entry.hitPoints.totalGain
        val state = current.state.copy(
            maximumHitPoints = current.state.maximumHitPoints + gain,
            currentHitPoints = if (entry.applyHitPointGainToCurrent) {
                (current.state.currentHitPoints + gain).coerceAtMost(current.state.maximumHitPoints + gain)
            } else {
                current.state.currentHitPoints
            },
            activeLevelUp = null,
        )
        return current.copy(
            build = targetBuild,
            state = state,
            progression = current.progression.copy(entries = current.progression.entries + entry),
        )
    }

    private fun validateHitPoints(
        entry: LevelProgressionEntry,
        expectedHitDie: Int,
    ): List<ValidationIssue> = buildList {
        val hitPoints = entry.hitPoints
        if (expectedHitDie !in setOf(6, 8, 10, 12) || hitPoints.dieSides != expectedHitDie) {
            add(ValidationIssue("level_up.hit_die", "Hit Die does not match the selected class"))
        }
        when (hitPoints.method) {
            HitPointGainMethod.FIRST_LEVEL_MAXIMUM ->
                add(ValidationIssue("level_up.first_level_hp", "First-level maximum is not a level-up option"))
            HitPointGainMethod.ROLLED -> {
                val face = hitPoints.dieFace
                if (face == null || face !in 1..hitPoints.dieSides) {
                    add(ValidationIssue("level_up.hp_roll", "Rolled HP needs a valid die face"))
                } else if (hitPoints.totalGain != (face + hitPoints.constitutionModifier).coerceAtLeast(1)) {
                    add(ValidationIssue("level_up.hp_total", "Rolled HP total does not match the roll and Constitution modifier"))
                }
            }
            HitPointGainMethod.FIXED -> {
                val expected = (hitPoints.dieSides / 2 + 1 + hitPoints.constitutionModifier).coerceAtLeast(1)
                if (hitPoints.totalGain != expected) {
                    add(ValidationIssue("level_up.hp_total", "Fixed HP total is incorrect"))
                }
            }
            HitPointGainMethod.MANUAL_OVERRIDE -> {
                if (hitPoints.totalGain < 1) add(ValidationIssue("level_up.hp_total", "HP gain must be positive"))
            }
        }
    }
}
