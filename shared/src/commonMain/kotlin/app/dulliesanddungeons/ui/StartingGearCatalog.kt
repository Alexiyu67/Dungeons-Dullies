package app.dulliesanddungeons.ui

data class StartingGearPackageUi(
    val id: String,
    val name: String,
    val weaponIds: List<String> = emptyList(),
    val equipmentIds: List<String> = emptyList(),
    val armorId: String? = null,
    val goldPieces: Int = 0,
    val goldOnly: Boolean = false,
) {
    fun summary(): String = if (goldOnly) "$goldPieces GP" else buildList {
        addAll(weaponIds.map(::gearDisplayName))
        armorId?.let { add(gearDisplayName(it)) }
        addAll(equipmentIds.map(::gearDisplayName))
        if (goldPieces > 0) add("$goldPieces GP")
    }.joinToString(", ")
}

internal fun startingGearPackages(ruleset: Ruleset, className: String): List<StartingGearPackageUi> {
    if (ruleset == Ruleset.Pf2eRemaster) {
        return listOf(
            StartingGearPackageUi("pf2e-adventurer", "Adventurer's kit", listOf("dagger"), listOf("backpack", "bedroll", "rope-hempen", "rations", "torch")),
            StartingGearPackageUi("pf2e-gold", "Start with currency", goldPieces = 15, goldOnly = true),
        )
    }
    val recommended = when (className.lowercase()) {
        "barbarian" -> StartingGearPackageUi("barbarian-a", "Package A", listOf("greataxe", "handaxe", "handaxe", "handaxe", "handaxe"), listOf("explorers-pack"), goldPieces = 15)
        "bard" -> StartingGearPackageUi("bard-a", "Package A", listOf("dagger", "dagger"), listOf("musical-instrument", "entertainers-pack"), "leather-armor", 19)
        "cleric" -> StartingGearPackageUi("cleric-a", "Package A", listOf("mace"), listOf("shield", "holy-symbol", "priests-pack"), "chain-shirt", 7)
        "druid" -> StartingGearPackageUi("druid-a", "Package A", listOf("sickle"), listOf("shield", "druidic-focus", "explorers-pack", "herbalism-kit"), "leather-armor", 9)
        "fighter" -> StartingGearPackageUi("fighter-a", "Package A", listOf("greatsword", "flail", "javelin", "javelin", "javelin", "javelin", "javelin", "javelin", "javelin", "javelin"), listOf("dungeoneers-pack"), "chain-mail", 4)
        "monk" -> StartingGearPackageUi("monk-a", "Package A", listOf("spear", "dagger", "dagger", "dagger", "dagger", "dagger"), listOf("explorers-pack", "artisans-tools"), goldPieces = 11)
        "paladin" -> StartingGearPackageUi("paladin-a", "Package A", listOf("longsword", "javelin", "javelin", "javelin", "javelin", "javelin", "javelin"), listOf("shield", "holy-symbol", "priests-pack"), "chain-mail", 9)
        "ranger" -> StartingGearPackageUi("ranger-a", "Package A", listOf("scimitar", "shortsword", "longbow"), listOf("quiver", "ammunition", "druidic-focus", "explorers-pack"), "studded-leather", 7)
        "rogue" -> StartingGearPackageUi("rogue-a", "Package A", listOf("dagger", "dagger", "shortsword", "shortbow"), listOf("quiver", "ammunition", "thieves-tools", "burglars-pack"), "leather-armor", 8)
        "sorcerer" -> StartingGearPackageUi("sorcerer-a", "Package A", listOf("spear", "dagger", "dagger"), listOf("arcane-focus", "dungeoneers-pack"), goldPieces = 28)
        "warlock" -> StartingGearPackageUi("warlock-a", "Package A", listOf("sickle", "dagger", "dagger"), listOf("arcane-focus", "book", "scholars-pack"), "leather-armor", 15)
        "wizard" -> StartingGearPackageUi("wizard-a", "Package A", listOf("dagger", "dagger"), listOf("arcane-focus", "robe", "spellbook", "scholars-pack"), goldPieces = 5)
        else -> StartingGearPackageUi("adventurer-a", "Recommended package", listOf("dagger"), listOf("explorers-pack"), goldPieces = 10)
    }
    val gold = when (className.lowercase()) {
        "barbarian" -> 75
        "bard" -> 90
        "cleric" -> 110
        "druid", "monk", "sorcerer" -> 50
        "fighter" -> 155
        "paladin", "ranger" -> 150
        "rogue", "warlock" -> 100
        "wizard" -> 55
        else -> 75
    }
    val classOptions = if (ruleset == Ruleset.Fifth2024 && className.equals("Fighter", true)) {
        listOf(
            recommended,
            StartingGearPackageUi(
                "fighter-b", "Package B",
                listOf("scimitar", "shortsword", "longbow"),
                listOf("ammunition", "quiver", "dungeoneers-pack"),
                "studded-leather", 11,
            ),
        )
    } else listOf(recommended)
    return classOptions + StartingGearPackageUi("${className.lowercase()}-gold", "Starting gold", goldPieces = gold, goldOnly = true)
}

private fun gearDisplayName(id: String): String = id.split('-').joinToString(" ") { part ->
    part.replaceFirstChar { it.uppercase() }
}
