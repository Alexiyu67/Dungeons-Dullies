package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ProficiencyRank

internal data class SkillDefinitionUi(
    val id: String,
    val englishName: String,
    val germanName: String,
    val ability: String,
    val lore: Boolean = false,
) {
    fun name(language: UiLanguage): String = if (language == UiLanguage.German) germanName else englishName
}

internal data class BackgroundDefinitionUi(
    val id: String,
    val englishName: String,
    val germanName: String,
    val grantedSkillIds: Set<String> = emptySet(),
    val customSkillCount: Int = 0,
    val loreSkill: SkillDefinitionUi? = null,
) {
    fun name(language: UiLanguage): String = if (language == UiLanguage.German) germanName else englishName
    val allGrantedSkillIds: Set<String> get() = grantedSkillIds + listOfNotNull(loreSkill?.id)
}

internal data class ClassProficiencyDefinitionUi(
    val classSkillIds: Set<String>,
    val classSkillCount: Int,
    val fixedSkillIds: Set<String> = emptySet(),
    val requiredOneOfSkillGroups: List<Set<String>> = emptyList(),
    val pf2AdditionalSkills: Int = 0,
    val automaticRanks: Map<String, ProficiencyRank> = emptyMap(),
)

internal data class CreationSkillOptionUi(
    val id: String,
    val name: String,
    val ability: String,
    val modifier: Int,
    val rank: ProficiencyRank?,
    val granted: Boolean = false,
)

internal object ProficiencyCatalog {
    private fun skill(id: String, en: String, de: String, ability: String) =
        SkillDefinitionUi("skill:$id", en, de, ability)

    val fiveESkills = listOf(
        skill("acrobatics", "Acrobatics", "Akrobatik", "DEX"),
        skill("animal-handling", "Animal Handling", "Mit Tieren umgehen", "WIS"),
        skill("arcana", "Arcana", "Arkane Kunde", "INT"),
        skill("athletics", "Athletics", "Athletik", "STR"),
        skill("deception", "Deception", "Täuschen", "CHA"),
        skill("history", "History", "Geschichte", "INT"),
        skill("insight", "Insight", "Motiv erkennen", "WIS"),
        skill("intimidation", "Intimidation", "Einschüchtern", "CHA"),
        skill("investigation", "Investigation", "Nachforschungen", "INT"),
        skill("medicine", "Medicine", "Heilkunde", "WIS"),
        skill("nature", "Nature", "Naturkunde", "INT"),
        skill("perception", "Perception", "Wahrnehmung", "WIS"),
        skill("performance", "Performance", "Auftreten", "CHA"),
        skill("persuasion", "Persuasion", "Überzeugen", "CHA"),
        skill("religion", "Religion", "Religion", "INT"),
        skill("sleight-of-hand", "Sleight of Hand", "Fingerfertigkeit", "DEX"),
        skill("stealth", "Stealth", "Heimlichkeit", "DEX"),
        skill("survival", "Survival", "Überleben", "WIS"),
    )

    val pf2eSkills = listOf(
        skill("acrobatics", "Acrobatics", "Akrobatik", "DEX"),
        skill("arcana", "Arcana", "Arkane Kunde", "INT"),
        skill("athletics", "Athletics", "Athletik", "STR"),
        skill("crafting", "Crafting", "Handwerk", "INT"),
        skill("deception", "Deception", "Täuschung", "CHA"),
        skill("diplomacy", "Diplomacy", "Diplomatie", "CHA"),
        skill("intimidation", "Intimidation", "Einschüchtern", "CHA"),
        skill("medicine", "Medicine", "Heilkunde", "WIS"),
        skill("nature", "Nature", "Naturkunde", "WIS"),
        skill("occultism", "Occultism", "Okkultismus", "INT"),
        skill("perception", "Perception", "Wahrnehmung", "WIS"),
        skill("performance", "Performance", "Darbietung", "CHA"),
        skill("religion", "Religion", "Religion", "WIS"),
        skill("society", "Society", "Gesellschaftskunde", "INT"),
        skill("stealth", "Stealth", "Heimlichkeit", "DEX"),
        skill("survival", "Survival", "Überleben", "WIS"),
        skill("thievery", "Thievery", "Diebeskunst", "DEX"),
    )

    private fun lore(id: String, en: String, de: String) =
        SkillDefinitionUi("skill:lore:$id", en, de, "INT", lore = true)

    private val fifth2024Backgrounds = listOf(
        BackgroundDefinitionUi("background:acolyte", "Acolyte", "Akolyth:in", setOf("skill:insight", "skill:religion")),
        BackgroundDefinitionUi("background:criminal", "Criminal", "Kriminelle:r", setOf("skill:sleight-of-hand", "skill:stealth")),
        BackgroundDefinitionUi("background:sage", "Sage", "Gelehrte:r", setOf("skill:arcana", "skill:history")),
        BackgroundDefinitionUi("background:soldier", "Soldier", "Soldat:in", setOf("skill:athletics", "skill:intimidation")),
    )

    private val fifth2014Backgrounds = listOf(
        BackgroundDefinitionUi("background:acolyte", "Acolyte", "Akolyth:in", setOf("skill:insight", "skill:religion")),
        BackgroundDefinitionUi("background:custom", "Custom background", "Eigener Hintergrund", customSkillCount = 2),
    )

    /* Project-original names and terse mechanics; no PF2 rules prose is bundled here. */
    private val pf2eBackgrounds = listOf(
        BackgroundDefinitionUi("background:artisan", "Artisan", "Handwerker:in", setOf("skill:crafting"), loreSkill = lore("guild", "Guild Lore", "Gildenkunde")),
        BackgroundDefinitionUi("background:criminal", "Criminal", "Kriminelle:r", setOf("skill:stealth"), loreSkill = lore("underworld", "Underworld Lore", "Unterweltkunde")),
        BackgroundDefinitionUi("background:scholar", "Scholar", "Gelehrte:r", setOf("skill:society"), loreSkill = lore("academia", "Academia Lore", "Akademiekunde")),
        BackgroundDefinitionUi("background:warrior", "Warrior", "Krieger:in", setOf("skill:athletics"), loreSkill = lore("warfare", "Warfare Lore", "Kriegskunde")),
        BackgroundDefinitionUi("background:acolyte", "Acolyte", "Akolyth:in", setOf("skill:religion"), loreSkill = lore("scribing", "Scribing Lore", "Schreibkunstkunde")),
    )

    fun backgrounds(ruleset: Ruleset): List<BackgroundDefinitionUi> = when (ruleset) {
        Ruleset.Fifth2024 -> fifth2024Backgrounds
        Ruleset.Fifth2014 -> fifth2014Backgrounds
        Ruleset.Pf2eRemaster -> pf2eBackgrounds
    }

    fun background(ruleset: Ruleset, id: String?): BackgroundDefinitionUi? =
        backgrounds(ruleset).firstOrNull { it.id == id }

    fun skills(ruleset: Ruleset, backgroundId: String? = null): List<SkillDefinitionUi> =
        ((if (ruleset == Ruleset.Pf2eRemaster) pf2eSkills else fiveESkills) +
            listOfNotNull(background(ruleset, backgroundId)?.loreSkill)).distinctBy { it.id }

    fun skill(ruleset: Ruleset, id: String, backgroundId: String? = null): SkillDefinitionUi? =
        skills(ruleset, backgroundId).firstOrNull { it.id == normalizeSkillId(id) }

    fun skillByDisplayName(ruleset: Ruleset, name: String): SkillDefinitionUi? =
        (skills(ruleset) + backgrounds(ruleset).mapNotNull { it.loreSkill }).distinctBy { it.id }.firstOrNull {
            it.englishName.equals(name, true) || it.germanName.equals(name, true) || it.id == normalizeSkillId(name)
        }

    fun normalizeSkillId(value: String): String {
        if (value.startsWith("skill:")) return value.lowercase()
        val slug = value.lowercase().map { if (it.isLetterOrDigit()) it else '-' }
            .joinToString("").replace(Regex("-+"), "-").trim('-')
        return "skill:$slug"
    }

    fun classDefinition(ruleset: Ruleset, className: String): ClassProficiencyDefinitionUi =
        if (ruleset == Ruleset.Pf2eRemaster) pf2eClass(className) else fiveEClass(ruleset, className)

    private fun fiveEClass(ruleset: Ruleset, className: String): ClassProficiencyDefinitionUi {
        val any = fiveESkills.mapTo(linkedSetOf()) { it.id }
        val data = when (className) {
            "Barbarian" -> Triple(2, ids("animal-handling", "athletics", "intimidation", "nature", "perception", "survival"), setOf("STR", "CON"))
            "Bard" -> Triple(3, any, setOf("DEX", "CHA"))
            "Cleric" -> Triple(2, ids("history", "insight", "medicine", "persuasion", "religion"), setOf("WIS", "CHA"))
            "Druid" -> Triple(2, ids("arcana", "animal-handling", "insight", "medicine", "nature", "perception", "religion", "survival"), setOf("INT", "WIS"))
            "Fighter" -> Triple(2, ids("acrobatics", "animal-handling", "athletics", "history", "insight", "intimidation", "perception", "survival") + if (ruleset == Ruleset.Fifth2024) ids("persuasion") else emptySet(), setOf("STR", "CON"))
            "Monk" -> Triple(2, ids("acrobatics", "athletics", "history", "insight", "religion", "stealth"), setOf("STR", "DEX"))
            "Paladin" -> Triple(2, ids("athletics", "insight", "intimidation", "medicine", "persuasion", "religion"), setOf("WIS", "CHA"))
            "Ranger" -> Triple(3, ids("animal-handling", "athletics", "insight", "investigation", "nature", "perception", "stealth", "survival"), setOf("STR", "DEX"))
            "Rogue" -> Triple(4, ids("acrobatics", "athletics", "deception", "insight", "intimidation", "investigation", "perception", "persuasion", "sleight-of-hand", "stealth") + if (ruleset == Ruleset.Fifth2014) ids("performance") else emptySet(), setOf("DEX", "INT"))
            "Sorcerer" -> Triple(2, ids("arcana", "deception", "insight", "intimidation", "persuasion", "religion"), setOf("CON", "CHA"))
            "Warlock" -> Triple(2, ids("arcana", "deception", "history", "intimidation", "investigation", "nature", "religion"), setOf("WIS", "CHA"))
            "Wizard" -> Triple(2, ids("arcana", "history", "insight", "investigation", "medicine", "religion") + if (ruleset == Ruleset.Fifth2024) ids("nature") else emptySet(), setOf("INT", "WIS"))
            else -> Triple(2, any, emptySet())
        }
        val automatic = buildMap {
            data.third.forEach { ability ->
                val id = when (ability) {
                    "STR" -> "strength"
                    "DEX" -> "dexterity"
                    "CON" -> "constitution"
                    "INT" -> "intelligence"
                    "WIS" -> "wisdom"
                    else -> "charisma"
                }
                put("save:$id", ProficiencyRank.TRAINED)
            }
            fiveEEquipmentRanks(className).forEach { (id, rank) -> put(id, rank) }
        }
        return ClassProficiencyDefinitionUi(data.second, data.first, automaticRanks = automatic)
    }

    private fun fiveEEquipmentRanks(className: String): Map<String, ProficiencyRank> {
        val ids = when (className) {
            "Barbarian" -> setOf("weapon:simple", "weapon:martial", "armor:light", "armor:medium", "armor:shield")
            "Bard" -> setOf("weapon:simple", "weapon:shortsword", "weapon:longsword", "weapon:rapier", "armor:light")
            "Cleric" -> setOf("weapon:simple", "armor:light", "armor:medium", "armor:shield")
            "Druid" -> setOf("weapon:simple", "weapon:scimitar", "armor:light", "armor:medium", "armor:shield")
            "Fighter", "Paladin" -> setOf("weapon:simple", "weapon:martial", "armor:light", "armor:medium", "armor:heavy", "armor:shield")
            "Monk" -> setOf("weapon:simple", "weapon:shortsword", "armor:unarmored")
            "Ranger" -> setOf("weapon:simple", "weapon:martial", "armor:light", "armor:medium", "armor:shield")
            "Rogue" -> setOf("weapon:simple", "weapon:shortsword", "weapon:longsword", "weapon:rapier", "weapon:hand-crossbow", "armor:light")
            "Sorcerer", "Wizard" -> setOf("weapon:dagger", "weapon:dart", "weapon:sling", "weapon:quarterstaff", "weapon:light-crossbow")
            "Warlock" -> setOf("weapon:simple", "armor:light")
            else -> emptySet()
        }
        return ids.associateWith { ProficiencyRank.TRAINED }
    }

    private fun pf2eClass(className: String): ClassProficiencyDefinitionUi {
        val fixed: Set<String>
        var requiredOneOf: List<Set<String>> = emptyList()
        val additional: Int
        val saveRanks: Map<String, ProficiencyRank>
        val perception: ProficiencyRank
        val weaponRanks: Map<String, ProficiencyRank>
        val armorRanks: Map<String, ProficiencyRank>
        when (className) {
            "Alchemist" -> { fixed = ids("crafting"); additional = 3; saveRanks = saves(ProficiencyRank.EXPERT, ProficiencyRank.TRAINED, ProficiencyRank.TRAINED); perception = ProficiencyRank.TRAINED; weaponRanks = weapons(simple = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Barbarian" -> { fixed = ids("athletics"); additional = 3; saveRanks = saves(ProficiencyRank.EXPERT, ProficiencyRank.TRAINED, ProficiencyRank.TRAINED); perception = ProficiencyRank.EXPERT; weaponRanks = weapons(simple = ProficiencyRank.TRAINED, martial = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, medium = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Bard" -> { fixed = ids("occultism", "performance"); additional = 4; saveRanks = saves(ProficiencyRank.TRAINED, ProficiencyRank.TRAINED, ProficiencyRank.EXPERT); perception = ProficiencyRank.EXPERT; weaponRanks = weapons(simple = ProficiencyRank.TRAINED, martial = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Champion" -> { fixed = ids("religion"); additional = 3; saveRanks = saves(ProficiencyRank.EXPERT, ProficiencyRank.TRAINED, ProficiencyRank.EXPERT); perception = ProficiencyRank.TRAINED; weaponRanks = weapons(simple = ProficiencyRank.TRAINED, martial = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, medium = ProficiencyRank.TRAINED, heavy = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Cleric" -> { fixed = ids("religion"); additional = 3; saveRanks = saves(ProficiencyRank.TRAINED, ProficiencyRank.TRAINED, ProficiencyRank.EXPERT); perception = ProficiencyRank.TRAINED; weaponRanks = weapons(simple = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, medium = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Druid" -> { fixed = ids("nature"); additional = 3; saveRanks = saves(ProficiencyRank.TRAINED, ProficiencyRank.TRAINED, ProficiencyRank.EXPERT); perception = ProficiencyRank.TRAINED; weaponRanks = weapons(simple = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, medium = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Fighter" -> { fixed = emptySet(); requiredOneOf = listOf(ids("acrobatics", "athletics")); additional = 4; saveRanks = saves(ProficiencyRank.EXPERT, ProficiencyRank.EXPERT, ProficiencyRank.TRAINED); perception = ProficiencyRank.EXPERT; weaponRanks = weapons(simple = ProficiencyRank.EXPERT, martial = ProficiencyRank.EXPERT, advanced = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, medium = ProficiencyRank.TRAINED, heavy = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Monk" -> { fixed = emptySet(); additional = 4; saveRanks = saves(ProficiencyRank.EXPERT, ProficiencyRank.EXPERT, ProficiencyRank.EXPERT); perception = ProficiencyRank.TRAINED; weaponRanks = mapOf("weapon:unarmed" to ProficiencyRank.EXPERT, "weapon:simple" to ProficiencyRank.TRAINED); armorRanks = armor(unarmored = ProficiencyRank.EXPERT) }
            "Ranger" -> { fixed = ids("nature", "survival"); additional = 4; saveRanks = saves(ProficiencyRank.EXPERT, ProficiencyRank.EXPERT, ProficiencyRank.TRAINED); perception = ProficiencyRank.EXPERT; weaponRanks = weapons(simple = ProficiencyRank.TRAINED, martial = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, medium = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Rogue" -> { fixed = ids("stealth"); additional = 8; saveRanks = saves(ProficiencyRank.TRAINED, ProficiencyRank.EXPERT, ProficiencyRank.EXPERT); perception = ProficiencyRank.EXPERT; weaponRanks = weapons(simple = ProficiencyRank.TRAINED, martial = ProficiencyRank.TRAINED); armorRanks = armor(light = ProficiencyRank.TRAINED, unarmored = ProficiencyRank.TRAINED) }
            "Sorcerer" -> { fixed = emptySet(); additional = 4; saveRanks = saves(ProficiencyRank.TRAINED, ProficiencyRank.TRAINED, ProficiencyRank.EXPERT); perception = ProficiencyRank.TRAINED; weaponRanks = weapons(simple = ProficiencyRank.TRAINED); armorRanks = armor(unarmored = ProficiencyRank.TRAINED) }
            "Wizard" -> { fixed = ids("arcana"); additional = 2; saveRanks = saves(ProficiencyRank.TRAINED, ProficiencyRank.TRAINED, ProficiencyRank.EXPERT); perception = ProficiencyRank.TRAINED; weaponRanks = weapons(simple = ProficiencyRank.TRAINED); armorRanks = armor(unarmored = ProficiencyRank.TRAINED) }
            else -> { fixed = emptySet(); additional = 3; saveRanks = saves(ProficiencyRank.TRAINED, ProficiencyRank.TRAINED, ProficiencyRank.TRAINED); perception = ProficiencyRank.TRAINED; weaponRanks = weapons(simple = ProficiencyRank.TRAINED); armorRanks = armor(unarmored = ProficiencyRank.TRAINED) }
        }
        return ClassProficiencyDefinitionUi(
            classSkillIds = pf2eSkills.filterNot { it.id == "skill:perception" }.mapTo(linkedSetOf()) { it.id },
            classSkillCount = 0,
            fixedSkillIds = fixed,
            requiredOneOfSkillGroups = requiredOneOf,
            pf2AdditionalSkills = additional,
            automaticRanks = buildMap {
                putAll(saveRanks)
                put("skill:perception", perception)
                putAll(weaponRanks)
                putAll(armorRanks)
            },
        )
    }

    private fun ids(vararg values: String): Set<String> = values.mapTo(linkedSetOf()) { "skill:$it" }

    private fun saves(fortitude: ProficiencyRank, reflex: ProficiencyRank, will: ProficiencyRank) = mapOf(
        "save:constitution" to fortitude,
        "save:dexterity" to reflex,
        "save:wisdom" to will,
    )

    private fun weapons(
        simple: ProficiencyRank? = null,
        martial: ProficiencyRank? = null,
        advanced: ProficiencyRank? = null,
    ) = buildMap {
        simple?.let { put("weapon:simple", it) }
        martial?.let { put("weapon:martial", it) }
        advanced?.let { put("weapon:advanced", it) }
    }

    private fun armor(
        unarmored: ProficiencyRank? = null,
        light: ProficiencyRank? = null,
        medium: ProficiencyRank? = null,
        heavy: ProficiencyRank? = null,
    ) = buildMap {
        unarmored?.let { put("armor:unarmored", it) }
        light?.let { put("armor:light", it) }
        medium?.let { put("armor:medium", it) }
        heavy?.let { put("armor:heavy", it) }
    }
}

internal fun ProficiencyRank.displayName(language: UiLanguage): String = when (this) {
    ProficiencyRank.TRAINED -> if (language == UiLanguage.German) "Geübt" else "Trained"
    ProficiencyRank.EXPERT -> if (language == UiLanguage.German) "Experte" else "Expert"
    ProficiencyRank.MASTER -> if (language == UiLanguage.German) "Meister" else "Master"
    ProficiencyRank.LEGENDARY -> if (language == UiLanguage.German) "Legendär" else "Legendary"
}
