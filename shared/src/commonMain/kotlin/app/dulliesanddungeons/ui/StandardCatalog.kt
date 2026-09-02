package app.dulliesanddungeons.ui

enum class KnownItemType { Weapon, Armor, Gear, Tool, Consumable, Rations }

enum class ItemRarity {
    Mundane,
    Common,
    Uncommon,
    Rare,
    VeryRare,
    Legendary,
    Artifact,
    Unique,
    Unspecified,
}

enum class KnownItemSource { BuiltIn, Local }

enum class KnownItemSort { Name, Type, Rarity }

enum class ItemBrowserTarget { Inventory, StartingArmor }

data class KnownItemUi(
    val id: String,
    val name: String,
    val type: KnownItemType,
    val rarity: ItemRarity,
    val details: String,
    val source: KnownItemSource,
    val supportedRulesets: Set<Ruleset>,
    val equipment: EquipmentUi? = null,
    val weapon: StandardWeaponTemplate? = null,
    val privateEntry: PrivateEntryUi? = null,
    val complete: Boolean = true,
) {
    fun compatibleWith(ruleset: Ruleset): Boolean = ruleset in supportedRulesets
}

/** Compact SRD 5.2.1 standard catalog used by the local add flow. */
data class StandardWeaponTemplate(
    val id: String,
    val name: String,
    val damage: String,
    val damageType: String,
    val ability: String,
    val properties: String,
    val range: String = "",
    val mastery: String = "",
    val itemBonus: Int = 0,
    val needsAttunement: Boolean = false,
    val custom: Boolean = false,
)

val standardWeaponCatalog = listOf(
    StandardWeaponTemplate("club", "Club", "1d4", "Bludgeoning", "STR", "Light", mastery = "Slow"),
    StandardWeaponTemplate("dagger", "Dagger", "1d4", "Piercing", "DEX", "Finesse, light, thrown", "20/60 ft", "Nick"),
    StandardWeaponTemplate("greatclub", "Greatclub", "1d8", "Bludgeoning", "STR", "Two-handed", mastery = "Push"),
    StandardWeaponTemplate("handaxe", "Handaxe", "1d6", "Slashing", "STR", "Light, thrown", "20/60 ft", "Vex"),
    StandardWeaponTemplate("javelin", "Javelin", "1d6", "Piercing", "STR", "Thrown", "30/120 ft", "Slow"),
    StandardWeaponTemplate("light-hammer", "Light Hammer", "1d4", "Bludgeoning", "STR", "Light, thrown", "20/60 ft", "Nick"),
    StandardWeaponTemplate("mace", "Mace", "1d6", "Bludgeoning", "STR", "", mastery = "Sap"),
    StandardWeaponTemplate("quarterstaff", "Quarterstaff", "1d6", "Bludgeoning", "STR", "Versatile (1d8)", mastery = "Topple"),
    StandardWeaponTemplate("sickle", "Sickle", "1d4", "Slashing", "STR", "Light", mastery = "Nick"),
    StandardWeaponTemplate("spear", "Spear", "1d6", "Piercing", "STR", "Thrown, versatile (1d8)", "20/60 ft", "Sap"),
    StandardWeaponTemplate("light-crossbow", "Light Crossbow", "1d8", "Piercing", "DEX", "Ammunition, loading, two-handed", "80/320 ft", "Slow"),
    StandardWeaponTemplate("dart", "Dart", "1d4", "Piercing", "DEX", "Finesse, thrown", "20/60 ft", "Vex"),
    StandardWeaponTemplate("shortbow", "Shortbow", "1d6", "Piercing", "DEX", "Ammunition, two-handed", "80/320 ft", "Vex"),
    StandardWeaponTemplate("sling", "Sling", "1d4", "Bludgeoning", "DEX", "Ammunition", "30/120 ft", "Slow"),
    StandardWeaponTemplate("battleaxe", "Battleaxe", "1d8", "Slashing", "STR", "Versatile (1d10)", mastery = "Topple"),
    StandardWeaponTemplate("flail", "Flail", "1d8", "Bludgeoning", "STR", "", mastery = "Sap"),
    StandardWeaponTemplate("glaive", "Glaive", "1d10", "Slashing", "STR", "Heavy, reach, two-handed", mastery = "Graze"),
    StandardWeaponTemplate("greataxe", "Greataxe", "1d12", "Slashing", "STR", "Heavy, two-handed", mastery = "Cleave"),
    StandardWeaponTemplate("greatsword", "Greatsword", "2d6", "Slashing", "STR", "Heavy, two-handed", mastery = "Graze"),
    StandardWeaponTemplate("halberd", "Halberd", "1d10", "Slashing", "STR", "Heavy, reach, two-handed", mastery = "Cleave"),
    StandardWeaponTemplate("lance", "Lance", "1d10", "Piercing", "STR", "Heavy, reach, two-handed unless mounted", mastery = "Topple"),
    StandardWeaponTemplate("longsword", "Longsword", "1d8", "Slashing", "STR", "Versatile (1d10)", mastery = "Sap"),
    StandardWeaponTemplate("maul", "Maul", "2d6", "Bludgeoning", "STR", "Heavy, two-handed", mastery = "Topple"),
    StandardWeaponTemplate("morningstar", "Morningstar", "1d8", "Piercing", "STR", "", mastery = "Sap"),
    StandardWeaponTemplate("pike", "Pike", "1d10", "Piercing", "STR", "Heavy, reach, two-handed", mastery = "Push"),
    StandardWeaponTemplate("rapier", "Rapier", "1d8", "Piercing", "DEX", "Finesse", mastery = "Vex"),
    StandardWeaponTemplate("scimitar", "Scimitar", "1d6", "Slashing", "DEX", "Finesse, light", mastery = "Nick"),
    StandardWeaponTemplate("shortsword", "Shortsword", "1d6", "Piercing", "DEX", "Finesse, light", mastery = "Vex"),
    StandardWeaponTemplate("trident", "Trident", "1d8", "Piercing", "STR", "Thrown, versatile (1d10)", "20/60 ft", "Topple"),
    StandardWeaponTemplate("war-pick", "War Pick", "1d8", "Piercing", "STR", "Versatile (1d10)", mastery = "Sap"),
    StandardWeaponTemplate("warhammer", "Warhammer", "1d8", "Bludgeoning", "STR", "Versatile (1d10)", mastery = "Push"),
    StandardWeaponTemplate("whip", "Whip", "1d4", "Slashing", "DEX", "Finesse, reach", mastery = "Slow"),
    StandardWeaponTemplate("blowgun", "Blowgun", "1", "Piercing", "DEX", "Ammunition, loading", "25/100 ft", "Vex"),
    StandardWeaponTemplate("hand-crossbow", "Hand Crossbow", "1d6", "Piercing", "DEX", "Ammunition, light, loading", "30/120 ft", "Vex"),
    StandardWeaponTemplate("heavy-crossbow", "Heavy Crossbow", "1d10", "Piercing", "DEX", "Ammunition, heavy, loading, two-handed", "100/400 ft", "Push"),
    StandardWeaponTemplate("longbow", "Longbow", "1d8", "Piercing", "DEX", "Ammunition, heavy, two-handed", "150/600 ft", "Slow"),
    StandardWeaponTemplate("musket", "Musket", "1d12", "Piercing", "DEX", "Ammunition, loading, two-handed", "40/120 ft", "Slow"),
    StandardWeaponTemplate("pistol", "Pistol", "1d10", "Piercing", "DEX", "Ammunition, loading", "30/90 ft", "Vex"),
)

val standardEquipmentCatalog = listOf(
    EquipmentUi("backpack", "Backpack", details = "Carries gear"),
    EquipmentUi("bedroll", "Bedroll"),
    EquipmentUi("bell", "Bell"),
    EquipmentUi("blanket", "Blanket"),
    EquipmentUi("block-and-tackle", "Block and Tackle"),
    EquipmentUi("book", "Book"),
    EquipmentUi("caltrops", "Caltrops", EquipmentKind.CONSUMABLE),
    EquipmentUi("candle", "Candle", EquipmentKind.CONSUMABLE),
    EquipmentUi("case-map", "Map or Scroll Case"),
    EquipmentUi("chain", "Chain (10 ft)"),
    EquipmentUi("climbers-kit", "Climber's Kit"),
    EquipmentUi("component-pouch", "Component Pouch"),
    EquipmentUi("crowbar", "Crowbar"),
    EquipmentUi("explorers-pack", "Explorer's Pack"),
    EquipmentUi("grappling-hook", "Grappling Hook"),
    EquipmentUi("healers-kit", "Healer's Kit", EquipmentKind.CONSUMABLE, quantity = 10),
    EquipmentUi("holy-symbol", "Holy Symbol"),
    EquipmentUi("hourglass", "Hourglass"),
    EquipmentUi("hunting-trap", "Hunting Trap"),
    EquipmentUi("ink", "Ink"),
    EquipmentUi("ladder", "Ladder (10 ft)"),
    EquipmentUi("lamp", "Lamp"),
    EquipmentUi("lantern-bullseye", "Bullseye Lantern"),
    EquipmentUi("lantern-hooded", "Hooded Lantern"),
    EquipmentUi("lock", "Lock"),
    EquipmentUi("magnifying-glass", "Magnifying Glass"),
    EquipmentUi("manacles", "Manacles"),
    EquipmentUi("mess-kit", "Mess Kit"),
    EquipmentUi("oil", "Oil Flask", EquipmentKind.CONSUMABLE),
    EquipmentUi("paper", "Paper", EquipmentKind.CONSUMABLE),
    EquipmentUi("parchment", "Parchment", EquipmentKind.CONSUMABLE),
    EquipmentUi("piton", "Piton", EquipmentKind.CONSUMABLE),
    EquipmentUi("pole", "Pole (10 ft)"),
    EquipmentUi("potion-healing", "Potion of Healing", EquipmentKind.CONSUMABLE),
    EquipmentUi("rations", "Rations (1 day)", EquipmentKind.RATIONS),
    EquipmentUi("rope-hempen", "Hempen Rope (50 ft)"),
    EquipmentUi("rope-silk", "Silk Rope (50 ft)"),
    EquipmentUi("shovel", "Shovel"),
    EquipmentUi("signal-whistle", "Signal Whistle"),
    EquipmentUi("spellbook", "Spellbook"),
    EquipmentUi("spikes-iron", "Iron Spikes", EquipmentKind.CONSUMABLE, quantity = 10),
    EquipmentUi("spyglass", "Spyglass"),
    EquipmentUi("tent", "Tent"),
    EquipmentUi("tinderbox", "Tinderbox"),
    EquipmentUi("torch", "Torch", EquipmentKind.CONSUMABLE),
    EquipmentUi("waterskin", "Waterskin"),
    EquipmentUi("thieves-tools", "Thieves' Tools", EquipmentKind.TOOL),
    EquipmentUi("disguise-kit", "Disguise Kit", EquipmentKind.TOOL),
    EquipmentUi("herbalism-kit", "Herbalism Kit", EquipmentKind.TOOL),
    EquipmentUi("shield", "Shield", EquipmentKind.ARMOR, details = "+2 AC while worn", shieldBonus = 2),
    EquipmentUi("leather-armor", "Leather Armor", EquipmentKind.ARMOR, details = "AC 11 + DEX"),
    EquipmentUi("studded-leather", "Studded Leather Armor", EquipmentKind.ARMOR, details = "AC 12 + DEX"),
    EquipmentUi("chain-shirt", "Chain Shirt", EquipmentKind.ARMOR, details = "AC 13 + DEX (max 2)"),
    EquipmentUi("scale-mail", "Scale Mail", EquipmentKind.ARMOR, details = "AC 14 + DEX (max 2)"),
    EquipmentUi("breastplate", "Breastplate", EquipmentKind.ARMOR, details = "AC 14 + DEX (max 2)"),
    EquipmentUi("half-plate", "Half Plate Armor", EquipmentKind.ARMOR, details = "AC 15 + DEX (max 2)"),
    EquipmentUi("ring-mail", "Ring Mail", EquipmentKind.ARMOR, details = "AC 14"),
    EquipmentUi("chain-mail", "Chain Mail", EquipmentKind.ARMOR, details = "AC 16"),
    EquipmentUi("splint-armor", "Splint Armor", EquipmentKind.ARMOR, details = "AC 17"),
    EquipmentUi("plate-armor", "Plate Armor", EquipmentKind.ARMOR, details = "AC 18"),
)

private val fifthEditionRulesets = setOf(Ruleset.Fifth2014, Ruleset.Fifth2024)
private val everyRuleset = Ruleset.entries.toSet()

internal fun builtInKnownItemCatalog(): List<KnownItemUi> = buildList {
    standardWeaponCatalog.forEach { weapon ->
        add(
            KnownItemUi(
                id = "weapon:${weapon.id}",
                name = weapon.name,
                type = KnownItemType.Weapon,
                rarity = ItemRarity.Mundane,
                details = listOf(weapon.damage, weapon.damageType, weapon.properties).filter(String::isNotBlank).joinToString(" · "),
                source = KnownItemSource.BuiltIn,
                supportedRulesets = fifthEditionRulesets,
                weapon = weapon,
            ),
        )
    }
    standardEquipmentCatalog.forEach { equipment ->
        add(
            KnownItemUi(
                id = "equipment:${equipment.id}",
                name = equipment.name,
                type = equipment.kind.toKnownItemType(),
                rarity = if (equipment.id == "potion-healing") ItemRarity.Common else ItemRarity.Mundane,
                details = equipment.details,
                source = KnownItemSource.BuiltIn,
                supportedRulesets = fifthEditionRulesets,
                equipment = equipment,
            ),
        )
    }

    // These are the PF2e armor definitions the creation flow already knows how to grant.
    listOf(
        EquipmentUi("pf2e-leather-armor", "Leather Armor", EquipmentKind.ARMOR, details = "Item bonus +1 · Dexterity cap +4"),
        EquipmentUi("pf2e-chain-shirt", "Chain Shirt", EquipmentKind.ARMOR, details = "Item bonus +2 · Dexterity cap +3"),
        EquipmentUi("pf2e-scale-mail", "Scale Mail", EquipmentKind.ARMOR, details = "Item bonus +3 · Dexterity cap +2"),
        EquipmentUi("pf2e-half-plate", "Half Plate", EquipmentKind.ARMOR, details = "Item bonus +5 · Dexterity cap +1"),
    ).forEach { equipment ->
        add(
            KnownItemUi(
                id = "equipment:${equipment.id}",
                name = equipment.name,
                type = KnownItemType.Armor,
                rarity = ItemRarity.Common,
                details = equipment.details,
                source = KnownItemSource.BuiltIn,
                supportedRulesets = setOf(Ruleset.Pf2eRemaster),
                equipment = equipment,
            ),
        )
    }
}

internal fun privateKnownItem(entry: PrivateEntryUi): KnownItemUi? {
    val normalizedKind = entry.normalizedPrivateKind()
    if (normalizedKind !in setOf("item", "weapon")) return null
    val rulesets = entry.privateRulesets()
    val rarity = entry.formulaValue("rarity")?.toItemRarity() ?: ItemRarity.Unspecified

    if (normalizedKind == "weapon") {
        val damage = entry.formulaValue("damage")
            ?: Regex("\\b\\d+d\\d+(?:\\s*[+-]\\s*\\d+)?\\b", RegexOption.IGNORE_CASE).find(entry.formula)?.value
        val damageType = entry.formulaValue("damage[ _-]?type")
        val ability = entry.formulaValue("ability")?.uppercase()?.takeIf { it in setOf("STR", "DEX") } ?: "STR"
        val template = damage?.let {
            StandardWeaponTemplate(
                id = "private-${entry.id}",
                name = entry.name,
                damage = it,
                damageType = damageType.orEmpty(),
                ability = ability,
                properties = entry.formulaValue("properties").orEmpty(),
                range = entry.formulaValue("range").orEmpty(),
                mastery = entry.formulaValue("mastery").orEmpty(),
                itemBonus = entry.formulaInt("(?:item[ _-]?)?bonus")?.coerceIn(-5, 5) ?: 0,
                needsAttunement = entry.formulaFlag("attunement", "attuned", "requires attunement"),
                custom = true,
            )
        }
        return KnownItemUi(
            id = "local-weapon:${entry.id}",
            name = entry.name,
            type = KnownItemType.Weapon,
            rarity = rarity,
            details = entry.summary,
            source = KnownItemSource.Local,
            supportedRulesets = rulesets,
            weapon = template,
            privateEntry = entry,
            complete = template != null && damageType?.isNotBlank() == true,
        )
    }

    val declaredType = entry.formulaValue("(?:type|category)")?.lowercase()?.replace('-', ' ')?.replace('_', ' ')
        ?: entry.kind.trim().lowercase()
    val itemType = when (declaredType) {
        "armor", "armour", "shield" -> KnownItemType.Armor
        "tool" -> KnownItemType.Tool
        "consumable" -> KnownItemType.Consumable
        "ration", "rations" -> KnownItemType.Rations
        else -> KnownItemType.Gear
    }
    val kind = itemType.toEquipmentKind()
    val armorClass = entry.formulaInt("(?:armor[ _-]?class|ac)")?.coerceIn(1, 30)
    val shieldBonus = entry.formulaInt("shield(?:[ _-]?bonus)?")?.coerceIn(0, 9) ?: 0
    val equipment = EquipmentUi(
        id = "private-${entry.id}",
        name = entry.name,
        kind = kind,
        details = entry.summary,
        needsAttunement = entry.formulaFlag("attunement", "attuned", "requires attunement"),
        armorClass = armorClass,
        shieldBonus = shieldBonus,
    )
    return KnownItemUi(
        id = "local-item:${entry.id}",
        name = entry.name,
        type = itemType,
        rarity = rarity,
        details = entry.summary,
        source = KnownItemSource.Local,
        supportedRulesets = rulesets,
        equipment = equipment,
        privateEntry = entry,
        complete = itemType != KnownItemType.Armor || armorClass != null || shieldBonus > 0,
    )
}

internal fun PrivateEntryUi.normalizedPrivateKind(): String = when (kind.trim().lowercase()) {
    "ancestry", "species", "race" -> "ancestry"
    "class" -> "class"
    "subclass", "sub-class", "archetype" -> "subclass"
    "feat" -> "feat"
    "feature", "class feature", "species feature", "racial feature", "trait" -> "feature"
    "spell" -> "spell"
    "creature", "monster", "enemy", "gegner", "kreatur" -> "creature"
    "item", "equipment", "gear", "armor", "armour" -> "item"
    "weapon" -> "weapon"
    "language" -> "language"
    "condition" -> "condition"
    "action" -> "action"
    "resource" -> "resource"
    "rule", "knowledge" -> "rule"
    else -> "informational"
}

internal fun PrivateEntryUi.privateRulesets(): Set<Ruleset> {
    val marker = formulaValue("ruleset")?.lowercase() ?: return everyRuleset
    return when (marker) {
        "5e" -> fifthEditionRulesets
        "5.5e", "2024", "fifth2024" -> setOf(Ruleset.Fifth2024)
        "2014", "fifth2014" -> setOf(Ruleset.Fifth2014)
        "pf2e", "pf2e remaster", "pf2e-remaster", "remaster" -> setOf(Ruleset.Pf2eRemaster)
        else -> emptySet()
    }
}

internal fun PrivateEntryUi.formulaValue(keyPattern: String): String? = Regex(
    "(?:^|[,;]|\\s)(?:$keyPattern)\\s*[:=]\\s*(.+?)(?=\\s+[a-z][a-z _-]*\\s*[:=]|[,;]|$)",
    setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
).find(formula)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)

private fun PrivateEntryUi.formulaInt(keyPattern: String): Int? = formulaValue(keyPattern)
    ?.let { Regex("-?\\d+").find(it)?.value }
    ?.toIntOrNull()

private fun PrivateEntryUi.formulaFlag(vararg markers: String): Boolean = markers.any { marker ->
    Regex("(?:^|[,;\\s])${Regex.escape(marker)}(?:\\s*[:=]\\s*(?:true|required|yes))?(?:$|[,;\\s])", RegexOption.IGNORE_CASE)
        .containsMatchIn(formula)
}

private fun String.toItemRarity(): ItemRarity = when (trim().lowercase().replace('-', ' ').replace('_', ' ')) {
    "mundane" -> ItemRarity.Mundane
    "common" -> ItemRarity.Common
    "uncommon" -> ItemRarity.Uncommon
    "rare" -> ItemRarity.Rare
    "very rare" -> ItemRarity.VeryRare
    "legendary" -> ItemRarity.Legendary
    "artifact" -> ItemRarity.Artifact
    "unique" -> ItemRarity.Unique
    else -> ItemRarity.Unspecified
}

internal fun EquipmentKind.toKnownItemType(): KnownItemType = when (this) {
    EquipmentKind.GEAR -> KnownItemType.Gear
    EquipmentKind.ARMOR -> KnownItemType.Armor
    EquipmentKind.TOOL -> KnownItemType.Tool
    EquipmentKind.CONSUMABLE -> KnownItemType.Consumable
    EquipmentKind.RATIONS -> KnownItemType.Rations
}

internal fun KnownItemType.toEquipmentKind(): EquipmentKind = when (this) {
    KnownItemType.Weapon, KnownItemType.Gear -> EquipmentKind.GEAR
    KnownItemType.Armor -> EquipmentKind.ARMOR
    KnownItemType.Tool -> EquipmentKind.TOOL
    KnownItemType.Consumable -> EquipmentKind.CONSUMABLE
    KnownItemType.Rations -> EquipmentKind.RATIONS
}
