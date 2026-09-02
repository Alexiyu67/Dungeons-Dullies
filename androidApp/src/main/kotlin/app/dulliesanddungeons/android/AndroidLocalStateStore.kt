package app.dulliesanddungeons.android

import android.content.Context
import android.util.AtomicFile
import app.dulliesanddungeons.data.LocalStateStore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal class AndroidLocalStateStore(context: Context) : LocalStateStore {
    private val stateFile = AtomicFile(File(context.filesDir, "character-state-v1.json"))
    private val portraitDirectory = File(context.filesDir, "portraits")
    private val stateLock = Any()

    @Volatile
    private var preloaded = false

    @Volatile
    private var cachedState: String? = null

    /** Reads storage on the caller's thread. MainActivity invokes this from Dispatchers.IO. */
    fun preload() {
        synchronized(stateLock) {
            cachedState = readStateFile()
            preloaded = true
        }
    }

    override fun readState(): String? = synchronized(stateLock) {
        if (!preloaded) {
            cachedState = readStateFile()
            preloaded = true
        }
        cachedState
    }

    override fun writeState(value: String) {
        synchronized(stateLock) {
            var stream: FileOutputStream? = null
            try {
                stream = stateFile.startWrite()
                stream.write(value.toByteArray(Charsets.UTF_8))
                stream.fd.sync()
                stateFile.finishWrite(stream)
                cachedState = value
                preloaded = true
            } catch (failure: Throwable) {
                stream?.let(stateFile::failWrite)
                throw failure
            }
        }
    }

    override fun writePortrait(characterId: String, bytes: ByteArray): String? = runCatching {
        writePortraitFile(characterId, "display", bytes)
    }.getOrNull()

    override fun writePortraitSource(characterId: String, bytes: ByteArray): String? = runCatching {
        writePortraitFile(characterId, "source", bytes)
    }.getOrNull()

    private fun writePortraitFile(characterId: String, kind: String, bytes: ByteArray): String {
        if (!portraitDirectory.exists()) portraitDirectory.mkdirs()
        val safeId = characterId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "$safeId-$kind-${UUID.randomUUID()}.jpg"
        File(portraitDirectory, fileName).writeBytes(bytes)
        return fileName
    }

    override fun readPortrait(fileName: String): ByteArray? {
        if (fileName != File(fileName).name) return null
        return runCatching { File(portraitDirectory, fileName).takeIf(File::isFile)?.readBytes() }.getOrNull()
    }

    override fun deletePortrait(fileName: String): Boolean {
        if (fileName != File(fileName).name) return false
        return runCatching {
            val file = File(portraitDirectory, fileName)
            !file.exists() || file.delete()
        }.getOrDefault(false)
    }

    private fun readStateFile(): String? = if (stateFile.baseFile.isFile) {
        stateFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
    } else null
}
