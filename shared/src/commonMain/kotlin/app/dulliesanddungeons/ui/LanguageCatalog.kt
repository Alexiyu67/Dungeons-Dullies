package app.dulliesanddungeons.ui

internal data class LanguageOptionUi(
    val id: String,
    val english: String,
    val german: String,
    val rulesets: Set<Ruleset>,
) {
    fun label(language: UiLanguage): String = if (language == UiLanguage.German) german else english
}

private val fifth2014 = setOf(Ruleset.Fifth2014)
private val fifth2024 = setOf(Ruleset.Fifth2024)
private val bothFifth = fifth2014 + fifth2024
private val pf2e = setOf(Ruleset.Pf2eRemaster)

internal val standardLanguageCatalog = listOf(
    LanguageOptionUi("common", "Common", "Gemeinsprache", Ruleset.entries.toSet()),
    LanguageOptionUi("common-sign-language", "Common Sign Language", "Gemeinsame Gebärdensprache", fifth2024),
    LanguageOptionUi("draconic", "Draconic", "Drakonisch", Ruleset.entries.toSet()),
    LanguageOptionUi("dwarvish", "Dwarvish", "Zwergisch", bothFifth),
    LanguageOptionUi("dwarven", "Dwarven", "Zwergisch", pf2e),
    LanguageOptionUi("elvish", "Elvish", "Elfisch", bothFifth),
    LanguageOptionUi("elven", "Elven", "Elfisch", pf2e),
    LanguageOptionUi("giant", "Giant", "Riesisch", bothFifth),
    LanguageOptionUi("gnomish", "Gnomish", "Gnomisch", Ruleset.entries.toSet()),
    LanguageOptionUi("goblin", "Goblin", "Goblinisch", Ruleset.entries.toSet()),
    LanguageOptionUi("halfling", "Halfling", "Halblingisch", Ruleset.entries.toSet()),
    LanguageOptionUi("orc", "Orc", "Orkisch", bothFifth),
    LanguageOptionUi("orcish", "Orcish", "Orkisch", pf2e),
    LanguageOptionUi("abyssal", "Abyssal", "Abyssisch", bothFifth),
    LanguageOptionUi("celestial", "Celestial", "Himmlisch", bothFifth),
    LanguageOptionUi("deep-speech", "Deep Speech", "Tiefensprache", bothFifth),
    LanguageOptionUi("druidic", "Druidic", "Druidisch", bothFifth),
    LanguageOptionUi("infernal", "Infernal", "Infernalisch", bothFifth),
    LanguageOptionUi("primordial", "Primordial", "Primordial", bothFifth),
    LanguageOptionUi("sylvan", "Sylvan", "Sylvanisch", bothFifth),
    LanguageOptionUi("thieves-cant", "Thieves' Cant", "Diebesjargon", bothFifth),
    LanguageOptionUi("undercommon", "Undercommon", "Tiefengemeinsprache", bothFifth),
    LanguageOptionUi("fey", "Fey", "Fey", pf2e),
    LanguageOptionUi("jotun", "Jotun", "Jotun", pf2e),
    LanguageOptionUi("sakvroth", "Sakvroth", "Sakvroth", pf2e),
    LanguageOptionUi("aklo", "Aklo", "Aklo", pf2e),
    LanguageOptionUi("chthonian", "Chthonian", "Chthonisch", pf2e),
    LanguageOptionUi("diabolic", "Diabolic", "Diabolisch", pf2e),
    LanguageOptionUi("empyrean", "Empyrean", "Empyreisch", pf2e),
    LanguageOptionUi("necril", "Necril", "Necril", pf2e),
    LanguageOptionUi("petran", "Petran", "Petran", pf2e),
    LanguageOptionUi("pyric", "Pyric", "Pyrisch", pf2e),
    LanguageOptionUi("shadowtongue", "Shadowtongue", "Schattenzunge", pf2e),
    LanguageOptionUi("sussuran", "Sussuran", "Sussuran", pf2e),
    LanguageOptionUi("thalassic", "Thalassic", "Thalassisch", pf2e),
    LanguageOptionUi("wildsong", "Wildsong", "Wildgesang", pf2e),
)

internal fun languageSuggestions(
    input: String,
    options: Iterable<String>,
    limit: Int = 6,
): List<String> {
    val segments = input.split(',')
    val needle = segments.lastOrNull().orEmpty().trim()
    if (needle.isEmpty()) return emptyList()
    val alreadySelected = segments.dropLast(1).map(String::trim).filter(String::isNotBlank).map(String::lowercase).toSet()
    return options
        .asSequence()
        .filter { it.lowercase() !in alreadySelected }
        .filterNot { it.equals(needle, ignoreCase = true) }
        .filter { it.contains(needle, ignoreCase = true) }
        .distinctBy { it.lowercase() }
        .sortedWith(compareBy<String>({ !it.startsWith(needle, ignoreCase = true) }, { it.lowercase() }))
        .take(limit)
        .toList()
}

internal fun replaceActiveLanguageSegment(input: String, suggestion: String): String {
    val prefix = input.substringBeforeLast(',', missingDelimiterValue = "")
    return if (prefix.isEmpty()) suggestion else "$prefix, $suggestion"
}
