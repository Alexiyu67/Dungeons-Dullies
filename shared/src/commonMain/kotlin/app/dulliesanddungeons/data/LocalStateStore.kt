package app.dulliesanddungeons.data

import app.dulliesanddungeons.domain.CharacterDocument
import app.dulliesanddungeons.ui.ConditionUi
import app.dulliesanddungeons.ui.PrivateEntryUi
import app.dulliesanddungeons.ui.PendingImportUi
import app.dulliesanddungeons.ui.UiLanguage
import kotlinx.serialization.Serializable

/**
 * Small synchronous storage boundary for the offline vertical slice. Android stores the JSON and
 * portrait files in app-private storage; other platforms can provide their own implementation.
 */
interface LocalStateStore {
    fun readState(): String?
    fun writeState(value: String)
    fun writePortrait(characterId: String, bytes: ByteArray): String? = null
    fun readPortrait(fileName: String): ByteArray? = null
    fun deletePortrait(fileName: String): Boolean = false

    object None : LocalStateStore {
        override fun readState(): String? = null
        override fun writeState(value: String) = Unit
    }
}

@Serializable
data class PersistedAppState(
    val schemaVersion: Int = 2,
    val language: UiLanguage = UiLanguage.English,
    val characters: List<CharacterDocument> = emptyList(),
    /** App-wide condition notices only; character effects live inside CharacterDocument.state. */
    val conditions: List<ConditionUi> = emptyList(),
    val privateEntries: List<PrivateEntryUi> = emptyList(),
    val pendingImports: List<PendingImportUi> = emptyList(),
)
