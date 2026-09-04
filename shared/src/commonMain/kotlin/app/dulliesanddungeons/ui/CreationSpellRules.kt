package app.dulliesanddungeons.ui

import app.dulliesanddungeons.rules.SrdSpellClass
import kotlin.math.floor

internal data class CreationSpellLimits(
    val spellClass: SrdSpellClass,
    val cantripLimit: Int,
    val leveledSpellLimit: Int,
    val preparedLimit: Int? = null,
    val maxSpellLevel: Int,
    val leveledLabel: String,
)

/** Character-creation spell choices derived from the selected edition, class, and level. */
internal object CreationSpellRules {
    fun limits(
        ruleset: Ruleset,
        className: String,
        level: Int,
        abilities: Map<String, Int>,
    ): CreationSpellLimits? {
        val safeLevel = level.coerceIn(1, 20)
        val spellClass = spellClass(className) ?: return null
        return when (ruleset) {
            Ruleset.Fifth2014 -> fifth2014(spellClass, safeLevel, abilities)
            Ruleset.Fifth2024 -> fifth2024(spellClass, safeLevel)
            Ruleset.Pf2eRemaster -> null
        }
    }

    fun spellClass(className: String): SrdSpellClass? = when (className.trim().lowercase()) {
        "bard" -> SrdSpellClass.BARD
        "cleric" -> SrdSpellClass.CLERIC
        "druid" -> SrdSpellClass.DRUID
        "paladin" -> SrdSpellClass.PALADIN
        "ranger" -> SrdSpellClass.RANGER
        "sorcerer" -> SrdSpellClass.SORCERER
        "warlock" -> SrdSpellClass.WARLOCK
        "wizard" -> SrdSpellClass.WIZARD
        else -> null
    }

    private fun fifth2014(
        spellClass: SrdSpellClass,
        level: Int,
        abilities: Map<String, Int>,
    ): CreationSpellLimits? {
        val fullMax = ((level + 1) / 2).coerceAtMost(9)
        val halfMax = ((level + 3) / 4).coerceAtMost(5)
        val tieredCantrips = when {
            level >= 10 -> 5
            level >= 4 -> 4
            else -> 3
        }
        return when (spellClass) {
            SrdSpellClass.BARD -> limits(spellClass, if (level >= 10) 4 else if (level >= 4) 3 else 2, BARD_2014[level], fullMax, "Known spells")
            SrdSpellClass.CLERIC -> limits(spellClass, tieredCantrips, prepared(level, abilities["WIS"]), fullMax, "Prepared spells")
            SrdSpellClass.DRUID -> limits(spellClass, if (level >= 10) 4 else if (level >= 4) 3 else 2, prepared(level, abilities["WIS"]), fullMax, "Prepared spells")
            SrdSpellClass.PALADIN -> if (level < 2) null else limits(spellClass, 0, prepared(level / 2, abilities["CHA"]), halfMax, "Prepared spells")
            SrdSpellClass.RANGER -> if (level < 2) null else limits(spellClass, 0, RANGER_2014[level], halfMax, "Known spells")
            SrdSpellClass.SORCERER -> limits(spellClass, if (level >= 10) 6 else if (level >= 4) 5 else 4, SORCERER_2014[level], fullMax, "Known spells")
            SrdSpellClass.WARLOCK -> limits(spellClass, if (level >= 10) 4 else if (level >= 4) 3 else 2, WARLOCK_2014[level], fullMax.coerceAtMost(5), "Known spells")
            SrdSpellClass.WIZARD -> limits(spellClass, tieredCantrips, 6 + (level - 1) * 2, fullMax, "Spellbook", prepared(level, abilities["INT"]))
        }
    }

    private fun fifth2024(spellClass: SrdSpellClass, level: Int): CreationSpellLimits {
        val fullMax = ((level + 1) / 2).coerceAtMost(9)
        val halfMax = ((level + 3) / 4).coerceAtMost(5)
        val standardCantrips = when {
            level >= 10 -> 5
            level >= 4 -> 4
            else -> 3
        }
        return when (spellClass) {
            SrdSpellClass.BARD -> limits(spellClass, if (level >= 10) 4 else if (level >= 4) 3 else 2, FULL_PREPARED_2024[level], fullMax, "Prepared spells")
            SrdSpellClass.CLERIC -> limits(spellClass, standardCantrips, FULL_PREPARED_2024[level], fullMax, "Prepared spells")
            SrdSpellClass.DRUID -> limits(spellClass, if (level >= 10) 4 else if (level >= 4) 3 else 2, FULL_PREPARED_2024[level], fullMax, "Prepared spells")
            SrdSpellClass.PALADIN -> limits(spellClass, 0, PALADIN_2024[level], halfMax, "Prepared spells")
            SrdSpellClass.RANGER -> limits(spellClass, 0, RANGER_2024[level], halfMax, "Prepared spells")
            SrdSpellClass.SORCERER -> limits(spellClass, if (level >= 10) 6 else if (level >= 4) 5 else 4, SORCERER_2024[level], fullMax, "Prepared spells")
            SrdSpellClass.WARLOCK -> limits(spellClass, if (level >= 10) 4 else if (level >= 4) 3 else 2, WARLOCK_2014[level], fullMax.coerceAtMost(5), "Prepared spells")
            SrdSpellClass.WIZARD -> limits(spellClass, standardCantrips, 6 + (level - 1) * 2, fullMax, "Spellbook", WIZARD_PREPARED_2024[level])
        }
    }

    private fun limits(
        spellClass: SrdSpellClass,
        cantrips: Int,
        leveled: Int,
        maxLevel: Int,
        label: String,
        prepared: Int? = null,
    ) = CreationSpellLimits(spellClass, cantrips, leveled, prepared, maxLevel, label)

    private fun prepared(classLevel: Int, score: Int?): Int =
        (classLevel + floor(((score ?: 10) - 10) / 2.0).toInt()).coerceAtLeast(1)

    private val BARD_2014 = counts(4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 15, 16, 18, 19, 19, 20, 22, 22, 22)
    private val SORCERER_2014 = counts(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 12, 13, 13, 14, 14, 15, 15, 15, 15)
    private val WARLOCK_2014 = counts(2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15)
    private val RANGER_2014 = counts(0, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11)
    private val FULL_PREPARED_2024 = counts(4, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22)
    private val SORCERER_2024 = counts(2, 4, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22)
    private val PALADIN_2024 = counts(2, 3, 4, 5, 6, 6, 7, 7, 9, 9, 10, 10, 11, 11, 12, 12, 14, 14, 15, 15)
    private val RANGER_2024 = counts(2, 3, 4, 5, 6, 6, 7, 7, 9, 9, 10, 10, 11, 11, 12, 12, 14, 14, 15, 15)
    private val WIZARD_PREPARED_2024 = counts(4, 5, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 18, 19, 21, 22, 23, 24, 25)

    private fun counts(vararg values: Int): IntArray = intArrayOf(0, *values)
}
