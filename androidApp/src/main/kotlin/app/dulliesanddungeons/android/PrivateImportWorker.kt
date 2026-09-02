package app.dulliesanddungeons.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.Writer
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
            val locale = inputData.getString(KEY_LOCALE).takeIf { it == "de" } ?: "en"
            val kind = detectKind(staged, displayName, mediaType)

            val completed = File(applicationContext.filesDir, "imports/completed").apply { mkdirs() }
            val result = if (kind == SourceKind.DNDPACK) {
                setProgress(workDataOf(KEY_PROGRESS_STAGE to "checking-pack"))
                val validated = validatePack(staged)
                val output = File(completed, "${validated.id}-${sha256(staged).take(12)}.dndpack")
                copyAtomically(staged, output)
                ImportResult(output, validated.id, validated.containerKind)
            } else {
                setProgress(workDataOf(KEY_PROGRESS_STAGE to "extracting"))
                val pages = when (kind) {
                    SourceKind.PDF -> extractPdf(staged)
                    SourceKind.JSON -> listOf(SourcePage(1, stringsFromJson(staged)))
                    SourceKind.TEXT -> listOf(SourcePage(1, readUtf8(staged)))
                    SourceKind.DNDPACK -> error("unreachable")
                }
                val candidates = candidatesFromPages(pages)
                require(candidates.length() > 0) { "no-review-candidates" }
                setProgress(workDataOf(KEY_PROGRESS_STAGE to "packing"))
                val sourceHash = sha256(staged)
                val packId = "local-${sourceHash.take(16)}"
                val output = File(completed, "$packId-${UUID.randomUUID().toString().take(8)}.dndpack")
                writeReviewPack(
                    output = output,
                    packId = packId,
                    locale = locale,
                    displayName = displayName,
                    mediaType = kind.mediaType,
                    sourceHash = sourceHash,
                    candidates = candidates,
                )
                ImportResult(output, packId, "review-candidates")
            }
            Result.success(
                workDataOf(
                    KEY_RESULT_PATH to result.file.absolutePath,
                    KEY_PACK_ID to result.packId,
                    KEY_CONTAINER_KIND to result.containerKind,
                ),
            )
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
        val prefix = FileInputStream(file).use { input -> ByteArray(8).also { input.read(it) } }
        val suffix = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when {
            prefix.copyOfRange(0, 5).contentEquals("%PDF-".toByteArray()) -> SourceKind.PDF
            prefix[0] == 0x50.toByte() && prefix[1] == 0x4b.toByte() -> SourceKind.DNDPACK
            suffix == "json" || mediaType.contains("json", ignoreCase = true) -> SourceKind.JSON
            suffix in setOf("txt", "md") || mediaType.startsWith("text/") -> SourceKind.TEXT
            else -> throw IllegalArgumentException("unsupported-source-type")
        }
    }

    private suspend fun extractPdf(file: File): List<SourcePage> {
        PDFBoxResourceLoader.init(applicationContext)
        return PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()).use { document ->
            require(document.numberOfPages in 1..MAX_PDF_PAGES) { "pdf-page-limit" }
            val pages = ArrayList<SourcePage>(document.numberOfPages)
            var totalCharacters = 0
            for (page in 1..document.numberOfPages) {
                currentCoroutineContext().ensureActive()
                val writer = CappedStringWriter(MAX_EXTRACTED_CHARACTERS - totalCharacters)
                PDFTextStripper().apply {
                    startPage = page
                    endPage = page
                    sortByPosition = true
                }.writeText(document, writer)
                val text = writer.toString()
                totalCharacters += text.length
                pages += SourcePage(page, text)
            }
            require(pages.any { it.text.isNotBlank() }) { "ocr-required" }
            pages
        }
    }

    private fun readUtf8(file: File): String {
        val bytes = file.readBytes()
        require(bytes.size <= MAX_TEXT_BYTES) { "text-size-limit" }
        val text = bytes.toString(Charsets.UTF_8)
        require(!text.contains('\uFFFD')) { "text-not-utf8" }
        return text.take(MAX_EXTRACTED_CHARACTERS)
    }

    private fun stringsFromJson(file: File): String {
        val root = JSONTokener(readUtf8(file)).nextValue()
        val fragments = ArrayList<String>()
        fun visit(value: Any?, depth: Int) {
            if (depth > MAX_JSON_DEPTH || fragments.size >= MAX_JSON_FRAGMENTS) return
            when (value) {
                is JSONObject -> value.keys().forEach { key ->
                    if (key.isNotBlank()) fragments += key.take(MAX_HEADING_LENGTH)
                    visit(value.opt(key), depth + 1)
                }
                is JSONArray -> for (index in 0 until value.length()) visit(value.opt(index), depth + 1)
                is String -> if (value.isNotBlank()) fragments += value.trim().take(MAX_EXTRACTED_CHARACTERS)
            }
        }
        visit(root, 0)
        return fragments.joinToString("\n").take(MAX_EXTRACTED_CHARACTERS)
    }

    private fun candidatesFromPages(pages: List<SourcePage>): JSONArray {
        val output = JSONArray()
        for (page in pages) {
            val lines = page.text.lineSequence().map { it.replace(Regex("\\s+"), " ").trim() }.toList()
            val headings = lines.indices.filter { looksLikeHeading(lines[it]) }
            for ((localIndex, lineIndex) in headings.withIndex()) {
                if (output.length() >= MAX_CANDIDATES) return output
                val title = lines[lineIndex].trim().trim('#', '*', ':', '—', '-', ' ')
                val next = headings.getOrNull(localIndex + 1) ?: lines.size
                val summary = lines.subList(lineIndex + 1, next).filter { it.isNotBlank() }.take(8)
                    .joinToString(" ").take(600)
                if (summary.isBlank()) continue
                val formulas = FORMULA.findAll(summary).map { it.value.lowercase(Locale.ROOT) }.distinct().take(8).toList()
                output.put(
                    JSONObject()
                        .put("id", stableCandidateId(title, page.number, localIndex))
                        .put("kind", guessKind(title, summary))
                        .put("name", title)
                        .put("summaryCandidate", summary)
                        .put("formulas", JSONArray(formulas))
                        .put("sourcePage", page.number)
                        .put("review", JSONObject().put("status", "needs_review").put("approvedBy", JSONObject.NULL).put("reviewedAt", JSONObject.NULL))
                        .put("automation", JSONObject().put("level", "informational").put("eligible", false).put("reason", "human review required")),
                )
            }
        }
        return output
    }

    private fun looksLikeHeading(raw: String): Boolean {
        val markdown = raw.trimStart().startsWith('#')
        val line = raw.trim().trim('#', '*', ':', '—', '-', ' ')
        if (line.length !in 2..MAX_HEADING_LENGTH || line.split(Regex("\\s+")).size > 12) return false
        if (FORMULA.matches(line)) return false
        val hasLetters = line.any(Char::isLetter)
        val titleCase = line.split(Regex("\\s+")).all { word -> word.firstOrNull()?.let { !it.isLetter() || it.isUpperCase() } ?: true }
        return hasLetters && (markdown || line == line.uppercase(Locale.ROOT) || titleCase || HEADING_SUFFIXES.any(line::endsWith))
    }

    private fun stableCandidateId(title: String, page: Int, index: Int): String {
        val slug = title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-').take(60).ifBlank { "candidate" }
        val digest = sha256("$page:$index:$title".toByteArray()).take(8)
        return "$slug-$digest"
    }

    private fun guessKind(title: String, summary: String): String {
        val text = "$title $summary".lowercase(Locale.ROOT)
        val routes = linkedMapOf(
            "creature" to listOf("creature", "monster", "enemy", "kreatur", "gegner", "golem"),
            "spell" to listOf("spell", "cantrip", "zauber"),
            "weapon" to listOf("weapon", "attack", "waffe", "angriff"),
            "condition" to listOf("condition", "zustand"),
            "feat" to listOf("feat", "talent"),
            "feature" to listOf("feature", "trait", "merkmal", "fähigkeit"),
            "species" to listOf("species", "ancestry", "race", "spezies", "abstammung", "volk"),
            "background" to listOf("background", "hintergrund"),
            "subclass" to listOf("subclass", "subklasse"),
            "class" to listOf("class", "klasse"),
            "language" to listOf("language", "speech", "sprache"),
            "item" to listOf("item", "equipment", "gear", "gegenstand", "ausrüstung"),
            "resource" to listOf("uses", "charges", "rest", "verwendungen", "rast"),
        )
        return routes.entries.firstOrNull { (_, words) -> words.any(text::contains) }?.key ?: "rule"
    }

    private fun writeReviewPack(
        output: File,
        packId: String,
        locale: String,
        displayName: String,
        mediaType: String,
        sourceHash: String,
        candidates: JSONArray,
    ) {
        val candidateBytes = candidates.toString(2).toByteArray(Charsets.UTF_8)
        val reviewBytes = REVIEW_NOTE.toByteArray(Charsets.UTF_8)
        val files = JSONArray()
            .put(fileManifest("candidates.json", "application/json", candidateBytes))
            .put(fileManifest("REVIEW.md", "text/markdown", reviewBytes))
        val manifest = JSONObject()
            .put("schemaVersion", 1)
            .put("containerKind", "review-candidates")
            .put("id", packId)
            .put("version", "0.0.1-local")
            .put("locale", locale)
            .put("createdAt", isoTimestamp())
            .put("source", JSONObject().put("fileName", displayName.take(255)).put("sha256", sourceHash).put("mediaType", mediaType))
            .put("payload", JSONObject().put("primary", "candidates.json"))
            .put("files", files)
            .put("privacy", JSONObject().put("containsPrivateContent", true).put("distributionReady", false).put("networkAccess", false))
            .put("review", JSONObject().put("status", "needs-review").put("automationEligibleCount", 0))
        output.parentFile?.mkdirs()
        val partial = File(output.parentFile, "${output.name}.part")
        try {
            FileOutputStream(partial).use { fileOutput ->
                val zip = ZipOutputStream(fileOutput)
                try {
                    zip.writeEntry("manifest.json", manifest.toString(2).toByteArray(Charsets.UTF_8))
                    zip.writeEntry("candidates.json", candidateBytes)
                    zip.writeEntry("REVIEW.md", reviewBytes)
                    zip.finish()
                    fileOutput.fd.sync()
                } finally {
                    zip.close()
                }
            }
            check(partial.renameTo(output)) { "pack-rename-failed" }
        } finally {
            partial.delete()
        }
    }

    private fun validatePack(file: File): ValidatedPack = ZipFile(file).use { zip ->
        val entries = ArrayList<ZipEntry>(MAX_ZIP_ENTRIES)
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            require(entries.size < MAX_ZIP_ENTRIES) { "pack-entry-count" }
            entries += enumeration.nextElement()
        }
        require(entries.size in 2..MAX_ZIP_ENTRIES) { "pack-entry-count" }
        require(entries.none { it.isDirectory || !SAFE_PACK_PATH.matches(it.name) }) { "unsafe-pack-entry" }
        require(entries.map { it.name }.distinct().size == entries.size) { "duplicate-pack-entry" }
        val manifestEntry = entries.singleOrNull { it.name == "manifest.json" } ?: error("missing-pack-manifest")
        val manifestBytes = zip.getInputStream(manifestEntry).use { it.readCapped(MAX_MANIFEST_BYTES) }
        val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
        require(manifest.optInt("schemaVersion") == 1) { "pack-schema-version" }
        val kind = manifest.optString("containerKind")
        require(kind in setOf("review-candidates", "installable-content")) { "pack-container-kind" }
        val id = manifest.optString("id")
        require(PACK_ID.matches(id)) { "pack-id" }
        require(manifest.optString("locale") in setOf("en", "de")) { "pack-locale" }
        val privacy = manifest.optJSONObject("privacy") ?: error("pack-privacy")
        require(privacy.optBoolean("containsPrivateContent") && !privacy.optBoolean("distributionReady") && !privacy.optBoolean("networkAccess")) { "pack-privacy" }
        val review = manifest.optJSONObject("review") ?: error("pack-review")
        val primary = manifest.optJSONObject("payload")?.optString("primary").orEmpty()
        if (kind == "review-candidates") {
            require(primary == "candidates.json" && review.optString("status") == "needs-review" && review.optInt("automationEligibleCount", -1) == 0) { "pack-review-gate" }
        } else {
            require(primary == "content-manifest.json" && review.optString("status") == "reviewed") { "pack-installable-gate" }
        }

        val metadataArray = manifest.optJSONArray("files") ?: error("pack-files")
        require(metadataArray.length() in 1 until MAX_ZIP_ENTRIES) { "pack-file-count" }
        val metadata = LinkedHashMap<String, JSONObject>()
        for (index in 0 until metadataArray.length()) {
            val item = metadataArray.optJSONObject(index) ?: error("pack-file-metadata")
            val path = item.optString("path")
            require(SAFE_PACK_PATH.matches(path) && metadata.put(path, item) == null) { "pack-file-metadata" }
        }
        require(primary in metadata) { "pack-primary-missing" }
        require(entries.map { it.name }.toSet() == metadata.keys.toSet() + setOf("manifest.json")) { "pack-file-list-mismatch" }

        var total = 0L
        for (entry in entries.filterNot { it.name == "manifest.json" }) {
            if (entry.compressedSize > 0 && entry.size > 0) require(entry.size / entry.compressedSize <= MAX_COMPRESSION_RATIO) { "pack-compression-ratio" }
            val bytes = zip.getInputStream(entry).use { it.readCapped(MAX_PACK_ENTRY_BYTES) }
            total += bytes.size
            require(total <= MAX_PACK_TOTAL_BYTES) { "pack-expanded-size" }
            val expected = metadata.getValue(entry.name)
            require(expected.optLong("size", -1) == bytes.size.toLong()) { "pack-file-size" }
            require(expected.optString("sha256") == sha256(bytes)) { "pack-file-digest" }
        }
        if (kind == "installable-content") {
            val contentBytes = zip.getInputStream(zip.getEntry(primary)).use { it.readCapped(MAX_PACK_ENTRY_BYTES) }
            val content = JSONObject(contentBytes.toString(Charsets.UTF_8))
            require(content.optInt("schemaVersion") == 1 && content.optString("contentStatus") == "private-local" && !content.optBoolean("distributionReady", true)) { "installable-content-manifest" }
        }
        ValidatedPack(id, kind)
    }

    private fun copyAtomically(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        val partial = File(destination.parentFile, "${destination.name}.part")
        try {
            source.inputStream().use { input ->
                FileOutputStream(partial).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            check(partial.renameTo(destination)) { "pack-copy-rename-failed" }
        } finally {
            partial.delete()
        }
    }

    private fun ZipOutputStream.writeEntry(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private fun fileManifest(path: String, mediaType: String, bytes: ByteArray): JSONObject = JSONObject()
        .put("path", path)
        .put("mediaType", mediaType)
        .put("size", bytes.size)
        .put("sha256", sha256(bytes))

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

    private fun isoTimestamp(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private fun failure(code: String): Result = Result.failure(workDataOf(KEY_ERROR_CODE to code))

    private fun stableErrorCode(failure: Throwable): String {
        val message = failure.message.orEmpty()
        return if (SAFE_ERROR_CODE.matches(message)) message else when (failure) {
            is SecurityException -> "source-access-denied"
            is OutOfMemoryError -> "import-memory-limit"
            else -> "import-failed"
        }
    }

    private enum class SourceKind(val mediaType: String) {
        PDF("application/pdf"),
        JSON("application/json"),
        TEXT("text/plain"),
        DNDPACK("application/vnd.dulliesanddungeons.pack+zip"),
    }

    private data class SourcePage(val number: Int, val text: String)
    private data class ValidatedPack(val id: String, val containerKind: String)
    private data class ImportResult(val file: File, val packId: String, val containerKind: String)

    private class CappedStringWriter(private val limit: Int) : Writer() {
        private val value = StringBuilder()

        override fun write(characters: CharArray, offset: Int, length: Int) {
            require(length <= limit - value.length) { "extracted-text-limit" }
            value.append(characters, offset, length)
        }

        override fun flush() = Unit

        override fun close() = Unit

        override fun toString(): String = value.toString()
    }

    companion object {
        const val WORK_TAG = "private-content-import"
        const val KEY_STAGED_PATH = "stagedPath"
        const val KEY_DISPLAY_NAME = "displayName"
        const val KEY_MEDIA_TYPE = "mediaType"
        const val KEY_LOCALE = "locale"
        const val KEY_PROGRESS_STAGE = "progressStage"
        const val KEY_RESULT_PATH = "resultPath"
        const val KEY_PACK_ID = "packId"
        const val KEY_CONTAINER_KIND = "containerKind"
        const val KEY_ERROR_CODE = "errorCode"

        private const val MAX_SOURCE_BYTES = 50L * 1024L * 1024L
        private const val MAX_TEXT_BYTES = 16 * 1024 * 1024
        private const val MAX_EXTRACTED_CHARACTERS = 5_000_000
        private const val MAX_PDF_PAGES = 1_000
        private const val MAX_JSON_DEPTH = 12
        private const val MAX_JSON_FRAGMENTS = 10_000
        private const val MAX_CANDIDATES = 5_000
        private const val MAX_HEADING_LENGTH = 100
        private const val MAX_ZIP_ENTRIES = 64
        private const val MAX_MANIFEST_BYTES = 128 * 1024
        private const val MAX_PACK_ENTRY_BYTES = 16 * 1024 * 1024
        private const val MAX_PACK_TOTAL_BYTES = 24L * 1024L * 1024L
        private const val MAX_COMPRESSION_RATIO = 100L
        private val SAFE_PACK_PATH = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
        private val PACK_ID = Regex("^[a-z0-9][a-z0-9._-]{1,79}$")
        private val SAFE_ERROR_CODE = Regex("^[a-z][a-z0-9-]{2,63}$")
        private val FORMULA = Regex("\\b\\d+d\\d+(?:\\s*[+-]\\s*\\d+)?\\b", RegexOption.IGNORE_CASE)
        private val HEADING_SUFFIXES = listOf("Feature", "Spell", "Weapon", "Action")
        private const val REVIEW_NOTE = """# Local pack review

Every candidate is review-only and informational. Check the named source page, correct the
mechanical fields, and approve it explicitly before enabling automation. Never move this pack
into the public content directory. Missing or ambiguous fields must remain informational.
"""
    }
}
