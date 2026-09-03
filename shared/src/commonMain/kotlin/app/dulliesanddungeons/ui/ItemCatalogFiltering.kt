package app.dulliesanddungeons.ui

internal fun filterKnownItems(
    items: Iterable<KnownItemUi>,
    ruleset: Ruleset,
    language: UiLanguage,
    query: String = "",
    types: Set<KnownItemType> = emptySet(),
    rarities: Set<ItemRarity> = emptySet(),
    sources: Set<KnownItemSource> = emptySet(),
    includeIncompatible: Boolean = false,
    sort: KnownItemSort = KnownItemSort.Name,
    ascending: Boolean = true,
): List<KnownItemUi> {
    val needle = query.trim()
    val filtered = items.filter { item ->
        (includeIncompatible || item.compatibleWith(ruleset)) &&
            (types.isEmpty() || item.type in types) &&
            (rarities.isEmpty() || item.rarity in rarities) &&
            (sources.isEmpty() || item.source in sources) &&
            (
                needle.isEmpty() || listOf(
                    item.name,
                    item.details,
                    item.type.name,
                    item.rarity.name,
                    item.source.name,
                ).plus(item.searchTerms).any { it.contains(needle, ignoreCase = true) }
            )
    }
    val comparator = when (sort) {
        KnownItemSort.Name -> Comparator<KnownItemUi> { left, right ->
            comparePickerNames(left.name, right.name, left.id, right.id, language)
        }
        KnownItemSort.Type -> compareBy<KnownItemUi>({ it.type.ordinal }, { pickerSortValue(it.name, language) }, { it.id })
        KnownItemSort.Rarity -> compareBy<KnownItemUi>({ it.rarity.sortRank(ruleset) }, { pickerSortValue(it.name, language) }, { it.id })
    }
    return filtered.sortedWith(if (ascending) comparator else comparator.reversed())
}

private fun comparePickerNames(
    leftName: String,
    rightName: String,
    leftId: String,
    rightId: String,
    language: UiLanguage,
): Int = pickerSortValue(leftName, language).compareTo(pickerSortValue(rightName, language))
    .takeIf { it != 0 }
    ?: leftName.compareTo(rightName, ignoreCase = true).takeIf { it != 0 }
    ?: leftId.compareTo(rightId, ignoreCase = true).takeIf { it != 0 }
    ?: leftId.compareTo(rightId)

private fun pickerSortValue(value: String, language: UiLanguage): String {
    val normalized = value.trim().lowercase()
    if (language != UiLanguage.German) return normalized
    return normalized
        .replace("ä", "a")
        .replace("ö", "o")
        .replace("ü", "u")
        .replace("ß", "ss")
}

private fun ItemRarity.sortRank(ruleset: Ruleset): Int = when (ruleset) {
    Ruleset.Pf2eRemaster -> when (this) {
        ItemRarity.Common -> 0
        ItemRarity.Uncommon -> 1
        ItemRarity.Rare -> 2
        ItemRarity.Unique -> 3
        ItemRarity.Mundane -> 4
        ItemRarity.VeryRare -> 5
        ItemRarity.Legendary -> 6
        ItemRarity.Artifact -> 7
        ItemRarity.Unspecified -> 8
    }
    else -> when (this) {
        ItemRarity.Mundane -> 0
        ItemRarity.Common -> 1
        ItemRarity.Uncommon -> 2
        ItemRarity.Rare -> 3
        ItemRarity.VeryRare -> 4
        ItemRarity.Legendary -> 5
        ItemRarity.Artifact -> 6
        ItemRarity.Unique -> 7
        ItemRarity.Unspecified -> 8
    }
}
