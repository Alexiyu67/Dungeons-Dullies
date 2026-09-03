package app.dulliesanddungeons.android

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.dulliesanddungeons.ui.PendingImportUi
import app.dulliesanddungeons.ui.decodePrivateContent
import app.dulliesanddungeons.ui.toPrivateEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

internal class PrivateImportCoordinator(private val context: Context) {
    suspend fun stageAndEnqueue(uri: Uri): Result<UUID> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val displayName = resolver.queryDisplayName(uri) ?: "private-content"
            val suffix = displayName.substringAfterLast('.', "").lowercase()
            require(suffix in ALLOWED_SUFFIXES) { "unsupported-source-type" }
            val stagingDirectory = File(context.filesDir, "imports/staging").apply { mkdirs() }
            val id = UUID.randomUUID()
            val partial = File(stagingDirectory, "$id.$suffix.part")
            val staged = File(stagingDirectory, "$id.$suffix")
            try {
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(partial).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= MAX_SOURCE_BYTES) { "source-too-large" }
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                } ?: error("source-unreadable")
                check(partial.renameTo(staged)) { "staging-rename-failed" }
            } catch (failure: Throwable) {
                partial.delete(); staged.delete(); throw failure
            }
            val input = Data.Builder()
                .putString(PrivateImportWorker.KEY_STAGED_PATH, staged.absolutePath)
                .putString(PrivateImportWorker.KEY_DISPLAY_NAME, displayName.sanitizedDisplayName())
                .putString(PrivateImportWorker.KEY_MEDIA_TYPE, resolver.getType(uri).orEmpty().take(100))
                .build()
            val request = OneTimeWorkRequestBuilder<PrivateImportWorker>()
                .setInputData(input)
                .addTag(PrivateImportWorker.WORK_TAG)
                .keepResultsForAtLeast(30, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            Result.success(request.id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    suspend fun loadPendingImport(resultPath: String, expectedPackId: String, expectedVersion: String): PendingImportUi =
        withContext(Dispatchers.IO) {
            val completedDirectory = File(context.filesDir, "imports/completed").canonicalFile
            val pack = File(resultPath).canonicalFile
            require(pack.isFile && pack.parentFile == completedDirectory && pack.extension == "dndpack") { "unsafe-result-path" }
            ZipFile(pack).use { zip ->
                val entries = zip.entries().asSequence().toList()
                require(entries.map { it.name }.toSet() == setOf("manifest.json", "content.json") && entries.size == 2) { "pack-file-list" }
                val manifest = JSONObject(zip.getInputStream(zip.getEntry("manifest.json")).use { it.readCapped(MAX_MANIFEST_BYTES) }.toString(Charsets.UTF_8))
                val content = zip.getInputStream(zip.getEntry("content.json")).use { it.readCapped(MAX_CONTENT_BYTES) }
                val metadata = manifest.optJSONObject("content") ?: error("pack-content-metadata")
                require(manifest.length() == 2 && manifest.optInt("schemaVersion") == 1 && metadata.optString("path") == "content.json") { "pack-manifest" }
                require(metadata.length() == 3 && metadata.optLong("size", -1) == content.size.toLong() && metadata.optString("sha256") == sha256(content)) { "pack-content-integrity" }
                val document = decodePrivateContent(content.toString(Charsets.UTF_8))
                require(document.id == expectedPackId && document.version == expectedVersion) { "pack-identity-mismatch" }
                PendingImportUi(
                    packId = document.id,
                    version = document.version,
                    requires = document.requires,
                    sourcePath = pack.absolutePath,
                    candidates = document.entries.map { it.toPrivateEntry(document.id, document.version) },
                )
            }
        }

    suspend fun isHandled(workId: UUID): Boolean = withContext(Dispatchers.IO) { handledMarker(workId).isFile }

    suspend fun markHandled(workId: UUID, resultPath: String?) = withContext(Dispatchers.IO) {
        val marker = AtomicFile(handledMarker(workId))
        var stream: FileOutputStream? = null
        try {
            stream = marker.startWrite()
            stream.write(resultPath.orEmpty().toByteArray(Charsets.UTF_8)); stream.fd.sync(); marker.finishWrite(stream)
        } catch (failure: Throwable) {
            stream?.let(marker::failWrite); throw failure
        }
    }

    suspend fun cleanupHandledImports(activePaths: Set<String>) = withContext(Dispatchers.IO) {
        val completedDirectory = File(context.filesDir, "imports/completed").canonicalFile
        val active = activePaths.mapNotNull { path -> runCatching { File(path).canonicalFile }.getOrNull()?.takeIf { it.parentFile == completedDirectory }?.path }.toSet()
        handledDirectory().listFiles().orEmpty().filter(File::isFile).forEach { marker ->
            val path = runCatching { marker.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
            val pack = path.takeIf(String::isNotBlank)?.let { runCatching { File(it).canonicalFile }.getOrNull() }
            if (pack != null && pack.parentFile == completedDirectory && pack.path !in active) { pack.delete(); marker.delete() }
        }
    }

    private fun android.content.ContentResolver.queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) cursor.getString(0) else null
        } finally { cursor?.close() }
    }

    private fun handledDirectory(): File = File(context.filesDir, "imports/handled").apply { mkdirs() }
    private fun handledMarker(workId: UUID): File = File(handledDirectory(), "$workId.txt")
    private fun String.sanitizedDisplayName(): String = replace(Regex("[^A-Za-z0-9._ -]"), "_").take(255).ifBlank { "private-content" }
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private fun InputStream.readCapped(limit: Int): ByteArray {
        val output = ByteArrayOutputStream(); val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) { val count = read(buffer); if (count < 0) return output.toByteArray(); require(output.size() + count <= limit) { "pack-entry-size" }; output.write(buffer, 0, count) }
    }

    private companion object {
        const val MAX_SOURCE_BYTES = 16L * 1024L * 1024L
        val ALLOWED_SUFFIXES = setOf("json", "dndpack")
        const val MAX_MANIFEST_BYTES = 16 * 1024
        const val MAX_CONTENT_BYTES = 16 * 1024 * 1024
    }
}
