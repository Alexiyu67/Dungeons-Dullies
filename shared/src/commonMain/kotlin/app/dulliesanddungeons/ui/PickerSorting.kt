package app.dulliesanddungeons.ui

/**
 * Consistent ordering for user-facing picker entries. Group ordering remains the caller's
 * responsibility; this only sorts entries inside an existing group.
 */
internal fun <T> Iterable<T>.sortedForPicker(
    language: UiLanguage,
    displayName: (T) -> String,
    stableId: (T) -> String = displayName,
): List<T> = sortedWith { left, right ->
    val leftName = displayName(left)
    val rightName = displayName(right)
    pickerSortKey(leftName, language).compareTo(pickerSortKey(rightName, language))
        .takeIf { it != 0 }
        ?: leftName.compareTo(rightName, ignoreCase = true).takeIf { it != 0 }
        ?: stableId(left).compareTo(stableId(right), ignoreCase = true).takeIf { it != 0 }
        ?: stableId(left).compareTo(stableId(right))
}

private fun pickerSortKey(value: String, language: UiLanguage): String {
    val normalized = value.trim().lowercase()
    if (language != UiLanguage.German) return normalized
    return buildString(normalized.length) {
        normalized.forEach { character ->
            when (character) {
                'ä' -> append('a')
                'ö' -> append('o')
                'ü' -> append('u')
                'ß' -> append("ss")
                else -> append(character)
            }
        }
    }
}
