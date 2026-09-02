package app.dulliesanddungeons.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AlignmentPickerTest {
    @Test
    fun pickerOffersUnsetThenTheNineStandardAlignments() {
        assertEquals(
            listOf(
                "",
                "Lawful Good",
                "Neutral Good",
                "Chaotic Good",
                "Lawful Neutral",
                "Neutral",
                "Chaotic Neutral",
                "Lawful Evil",
                "Neutral Evil",
                "Chaotic Evil",
            ),
            alignmentChoicesFor("").map(AlignmentChoice::persistedValue),
        )
    }

    @Test
    fun knownValuesAreLocalizedAndComparedWithoutCaseSensitivity() {
        assertEquals("Chaotisch Gut", alignmentDisplayName("Chaotic good", UiLanguage.German))
        assertEquals("Chaotic Good", alignmentDisplayName("Chaotisch Gut", UiLanguage.English))
        assertTrue(alignmentsEquivalent("Chaotic good", "Chaotic Good"))
    }

    @Test
    fun legacyCustomValueRemainsAvailableWithoutTranslation() {
        val choices = alignmentChoicesFor("Heroically uncertain")

        assertEquals("Heroically uncertain", choices.last().persistedValue)
        assertEquals(
            "Heroically uncertain",
            alignmentDisplayName("Heroically uncertain", UiLanguage.German),
        )
    }
}
