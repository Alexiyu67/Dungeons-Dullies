package app.dulliesanddungeons.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.dulliesanddungeons.ui.decodePrivateContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class PrivateImportWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val staged = inputData.getString(KEY_STAGED_PATH)?.let(::File)
            ?: return@withContext failure("missing-staged-path")
        if (!isSafeStagedFile(staged)) return@withContext failure("unsafe-staged-path")

        try {
            setProgress(workDataOf(KEY_PROGRESS_STAGE to "validating"))
            require(staged.length() in 1..MAX_SOURCE_BYTES) { "invalid-source-size" }
            val displayName = inputData.getString(KEY_DISPLAY_NAME).orEmpty().ifBlank { staged.name }
            val mediaType = inputData.getString(KEY_MEDIA_TYPE).orEmpty()
            val kind = detectKind(staged, displayName, mediaType)
            val completed = File(applicationContext.filesDir, "imports/completed").apply { mkdirs() }

            val result = when (kind) {
                SourceKind.JSON -> {
                    val content = staged.inputStream().use { it.readCapped(MAX_CONTENT_BYTES) }
                    val document = decodePrivateContent(content.toString(Charsets.UTF_8))
                    val output = File(completed, "${document.id}-${UUID.randomUUID().toString().take(8)}.dndpack")
                    writePack(output, content)
                    ImportResult(output, document.id, document.version)
                }
                SourceKind.DNDPACK -> {
                    val validated = validatePack(staged)
                    val output = File(completed, "${validated.id}-${sha256(staged).take(12)}.dndpack")
                    copyAtomically(staged, output)
                    ImportResult(output, validated.id, validated.version)
                }
            }
            Result.success(workDataOf(KEY_RESULT_PATH to result.file.absolutePath, KEY_PACK_ID to result.packId, KEY_PACK_VERSION to result.version))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            failure(stableErrorCode(failure))
        } finally {
            staged.delete()
        }
    }

    private fun isSafeStagedFile(file: File): Boolean {
        val staging = File(applicationContext.filesDir, "imports/staging").canonicalFile
        val candidate = runCatching { file.canonicalFile }.getOrNull() ?: return false
        return candidate.isFile && candidate.parentFile == staging && !candidate.name.endsWith(".part")
    }

    private fun detectKind(file: File, displayName: String, mediaType: String): SourceKind {
        val prefix = FileInputStream(file).use { input -> ByteArray(4).also { input.read(it) } }
        val suffix = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when {
            prefix[0] == 0x50.toByte() && prefix[1] == 0x4b.toByte() && suffix == "dndpack" -> SourceKind.DNDPACK
            suffix == "json" || mediaType.contains("json", ignoreCase = true) -> SourceKind.JSON
            else -> throw IllegalArgumentException("unsupported-source-type")
        }
    }

    private fun writePack(output: File, content: ByteArray) {
        val manifest = manifest(content).toString(2).toByteArray(Charsets.UTF_8)
        output.parentFile?.mkdirs()
        val partial = File(output.parentFile, "${output.name}.part")
        try {
            FileOutputStream(partial).use { fileOutput ->
                ZipOutputStream(fileOutput).use { zip ->
                    zip.writeEntry("manifest.json", manifest)
                    zip.writeEntry("content.json", content)
                }
            }
            check(partial.renameTo(output)) { "pack-rename-failed" }
        } finally {
            partial.delete()
        }
    }

    private fun validatePack(file: File): ValidatedPack = ZipFile(file).use { zip ->
        val entries = zip.entries().asSequence().toList()
        require(entries.size == 2 && entries.none { it.isDirectory }) { "pack-entry-count" }
        require(entries.map { it.name }.toSet() == setOf("manifest.json", "content.json")) { "pack-file-list" }
        val manifestBytes = zip.getInputStream(zip.getEntry("manifest.json")).use { it.readCapped(MAX_MANIFEST_BYTES) }
        val contentBytes = zip.getInputStream(zip.getEntry("content.json")).use { it.readCapped(MAX_CONTENT_BYTES) }
        val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        require(manifest.length() == 2 && manifest.optInt("schemaVersion") == 1) { "pack-manifest" }
        val metadata = manifest.optJSONObject("content") ?: error("pack-content-metadata")
        require(metadata.length() == 3 && metadata.optString("path") == "content.json") { "pack-content-metadata" }
        require(metadata.optLong("size", -1) == contentBytes.size.toLong()) { "pack-content-size" }
        require(metadata.optString("sha256") == sha256(contentBytes)) { "pack-content-digest" }
        val document = decodePrivateContent(contentBytes.toString(Charsets.UTF_8))
        ValidatedPack(document.id, document.version)
    }

    private fun manifest(content: ByteArray) = JSONObject()
        .put("schemaVersion", 1)
        .put("content", JSONObject().put("path", "content.json").put("size", content.size).put("sha256", sha256(content)))

    private fun copyAtomically(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        val partial = File(destination.parentFile, "${destination.name}.part")
        try {
            source.inputStream().use { input -> FileOutputStream(partial).use { output -> input.copyTo(output); output.fd.sync() } }
            check(partial.renameTo(destination)) { "pack-copy-rename-failed" }
        } finally {
            partial.delete()
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name)); write(bytes); closeEntry()
    }

    private fun InputStream.readCapped(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = read(buffer)
            if (count < 0) return output.toByteArray()
            require(output.size() + count <= limit) { "pack-entry-size" }
            output.write(buffer, 0, count)
        }
    }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private fun failure(code: String): Result = Result.failure(workDataOf(KEY_ERROR_CODE to code))
    private fun stableErrorCode(failure: Throwable): String = failure.message.orEmpty().takeIf(SAFE_ERROR_CODE::matches)
        ?: when (failure) {
            is SecurityException -> "source-access-denied"
            is OutOfMemoryError -> "import-memory-limit"
            else -> "import-failed"
        }

    private enum class SourceKind { JSON, DNDPACK }
    private data class ValidatedPack(val id: String, val version: String)
    private data class ImportResult(val file: File, val packId: String, val version: String)

    companion object {
        const val WORK_TAG = "private-content-import"
        const val KEY_STAGED_PATH = "stagedPath"
        const val KEY_DISPLAY_NAME = "displayName"
        const val KEY_MEDIA_TYPE = "mediaType"
        const val KEY_PROGRESS_STAGE = "progressStage"
        const val KEY_RESULT_PATH = "resultPath"
        const val KEY_PACK_ID = "packId"
        const val KEY_PACK_VERSION = "packVersion"
        const val KEY_ERROR_CODE = "errorCode"
        private const val MAX_SOURCE_BYTES = 16L * 1024L * 1024L
        private const val MAX_MANIFEST_BYTES = 16 * 1024
        private const val MAX_CONTENT_BYTES = 16 * 1024 * 1024
        private val SAFE_ERROR_CODE = Regex("^[a-z][a-z0-9-]{2,63}$")
    }
}
