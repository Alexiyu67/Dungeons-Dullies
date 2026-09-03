package app.dulliesanddungeons.android

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.DamageAbilityRule
import app.dulliesanddungeons.domain.DerivedAttackGrant
import app.dulliesanddungeons.domain.DerivedAttackParent
import app.dulliesanddungeons.domain.DerivedAttackTrigger
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.WeaponCombatType
import app.dulliesanddungeons.domain.WeaponMatch
import app.dulliesanddungeons.domain.WeaponTrainingCategory
import app.dulliesanddungeons.ui.PendingImportUi
import app.dulliesanddungeons.ui.PrivateEntryUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import org.json.JSONObject

internal class PrivateImportCoordinator(private val context: Context) {
    suspend fun stageAndEnqueue(uri: Uri, locale: String = "en"): Result<UUID> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val displayName = resolver.queryDisplayName(uri) ?: "private-content"
            val safeSuffix = displayName.substringAfterLast('.', "bin")
                .lowercase()
                .takeIf { it in ALLOWED_SUFFIXES }
                ?: "bin"
            val stagingDirectory = File(context.filesDir, "imports/staging").apply { mkdirs() }
            val id = UUID.randomUUID()
            val partial = File(stagingDirectory, "$id.$safeSuffix.part")
            val staged = File(stagingDirectory, "$id.$safeSuffix")

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
                partial.delete()
                staged.delete()
                throw failure
            }

            val input = Data.Builder()
                .putString(PrivateImportWorker.KEY_STAGED_PATH, staged.absolutePath)
                .putString(PrivateImportWorker.KEY_DISPLAY_NAME, displayName.sanitizedDisplayName())
                .putString(PrivateImportWorker.KEY_MEDIA_TYPE, resolver.getType(uri).orEmpty().take(100))
                .putString(PrivateImportWorker.KEY_LOCALE, if (locale == "de") "de" else "en")
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

    suspend fun loadPendingImport(resultPath: String, expectedPackId: String, expectedKind: String): PendingImportUi =
        withContext(Dispatchers.IO) {
            val completedDirectory = File(context.filesDir, "imports/completed").canonicalFile
            val pack = File(resultPath).canonicalFile
            require(pack.isFile && pack.parentFile == completedDirectory && pack.extension == "dndpack") { "unsafe-result-path" }
            ZipFile(pack).use { zip ->
                val manifestEntry = zip.getEntry("manifest.json") ?: error("missing-pack-manifest")
                val manifestBytes = zip.getInputStream(manifestEntry).use { it.readCapped(MAX_MANIFEST_BYTES) }
                val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
                require(manifest.optInt("schemaVersion") == 1) { "pack-schema-version" }
                require(manifest.optString("id") == expectedPackId) { "pack-id-mismatch" }
                require(manifest.optString("containerKind") == expectedKind) { "pack-kind-mismatch" }
                val privacy = manifest.optJSONObject("privacy") ?: error("pack-privacy")
                require(privacy.optBoolean("containsPrivateContent") && !privacy.optBoolean("distributionReady") && !privacy.optBoolean("networkAccess")) { "pack-privacy" }
                val review = manifest.optJSONObject("review") ?: error("pack-review")
                val primary = manifest.optJSONObject("payload")?.optString("primary").orEmpty()
                if (expectedKind == "review-candidates") {
                    require(primary == "candidates.json" && review.optString("status") == "needs-review" && review.optInt("automationEligibleCount", -1) == 0) { "pack-review-gate" }
                } else {
                    require(expectedKind == "installable-content" && primary == "content-manifest.json" && review.optString("status") == "reviewed") { "pack-review-gate" }
                }

                val metadata = manifest.getJSONArray("files")
                val candidates = if (expectedKind == "review-candidates") {
                    val candidateMetadata = (0 until metadata.length())
                        .map { metadata.getJSONObject(it) }
                        .single { it.getString("path") == "candidates.json" }
                    val candidateEntry = zip.getEntry("candidates.json") ?: error("missing-candidates")
                    val bytes = zip.getInputStream(candidateEntry).use { it.readCapped(MAX_CANDIDATE_BYTES) }
                    require(candidateMetadata.getLong("size") == bytes.size.toLong()) { "candidate-size" }
                    require(candidateMetadata.getString("sha256") == sha256(bytes)) { "candidate-digest" }
                    val array = org.json.JSONArray(bytes.toString(Charsets.UTF_8))
                    require(array.length() <= MAX_CANDIDATES) { "candidate-count" }
                    (0 until array.length()).map { index ->
                        val item = array.getJSONObject(index)
                        val formulas = item.optJSONArray("formulas")
                        val formula = if (formulas == null) "" else (0 until formulas.length())
                            .map { formulas.optString(it) }
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                            .take(80)
                        PrivateEntryUi(
                            id = item.optString("id").take(100),
                            kind = item.optString("kind", "rule").take(40),
                            name = item.getString("name").take(100),
                            summary = item.optString("summaryCandidate").take(500),
                            formula = formula,
                            sourceNote = "Local import · page ${item.optInt("sourcePage", 1)}",
                        )
                    }
                } else {
                    require(expectedKind == "installable-content") { "unsupported-pack-kind" }
                    val contentMetadata = (0 until metadata.length())
                        .map { metadata.getJSONObject(it) }
                        .single { it.getString("path") == "content-manifest.json" }
                    val contentEntry = zip.getEntry("content-manifest.json") ?: error("missing-content-manifest")
                    val bytes = zip.getInputStream(contentEntry).use { it.readCapped(MAX_CANDIDATE_BYTES) }
                    require(contentMetadata.getLong("size") == bytes.size.toLong()) { "content-size" }
                    require(contentMetadata.getString("sha256") == sha256(bytes)) { "content-digest" }
                    val content = JSONObject(bytes.toString(Charsets.UTF_8))
                    require(
                        content.optInt("schemaVersion") == 1 &&
                            content.optString("contentStatus") == "private-local" &&
                            !content.optBoolean("distributionReady", true),
                    ) { "installable-content-manifest" }
                    val entries = content.optJSONArray("entries")
                    if (entries != null) {
                        require(entries.length() <= MAX_CANDIDATES) { "content-entry-count" }
                        val mapped = (0 until entries.length()).map { index ->
                            val item = entries.getJSONObject(index)
                            PrivateEntryUi(
                                id = item.getString("id").take(100),
                                kind = item.getString("kind").take(40),
                                name = item.getString("name").take(100),
                                summary = item.optString("summary").take(500),
                                formula = item.optString("formula").take(80),
                                sourceNote = "Local .dndpack / ${expectedPackId.take(80)}",
                                attackGrants = item.optJSONArray("attackGrants").toAttackGrants(),
                            )
                        }
                        val automatedEntries = mapped.count { it.attackGrants.isNotEmpty() }
                        require(automatedEntries <= review.optInt("automationEligibleCount", 0)) { "content-automation-count" }
                        mapped
                    } else
                    listOf(
                        PrivateEntryUi(
                            id = "${expectedPackId.take(80)}-review",
                            kind = "content-pack",
                            name = content.optString("name").take(100).ifBlank { expectedPackId.take(100) },
                            summary = "Reviewed local content pack. Its mechanics remain informational until the app explicitly maps them.",
                            sourceNote = "Local .dndpack · installable content",
                        ),
                    )
                }
                PendingImportUi(
                    packId = expectedPackId,
                    containerKind = expectedKind,
                    sourcePath = pack.absolutePath,
                    candidates = candidates,
                )
            }
        }

    suspend fun isHandled(workId: UUID): Boolean = withContext(Dispatchers.IO) {
        handledMarker(workId).isFile
    }

    suspend fun markHandled(workId: UUID, resultPath: String?) = withContext(Dispatchers.IO) {
        val marker = AtomicFile(handledMarker(workId))
        var stream: FileOutputStream? = null
        try {
            stream = marker.startWrite()
            stream.write(resultPath.orEmpty().toByteArray(Charsets.UTF_8))
            stream.fd.sync()
            marker.finishWrite(stream)
        } catch (failure: Throwable) {
            stream?.let(marker::failWrite)
            throw failure
        }
    }

    suspend fun cleanupHandledImports(activePaths: Set<String>) = withContext(Dispatchers.IO) {
        val completedDirectory = File(context.filesDir, "imports/completed").canonicalFile
        val active = activePaths.mapNotNull { path ->
            runCatching { File(path).canonicalFile }
                .getOrNull()
                ?.takeIf { it.parentFile == completedDirectory }
                ?.path
        }.toSet()
        handledDirectory().listFiles().orEmpty().filter(File::isFile).forEach { marker ->
            val path = runCatching { marker.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
            if (path.isBlank()) return@forEach
            val pack = runCatching { File(path).canonicalFile }.getOrNull()
            if (pack != null && pack.parentFile == completedDirectory && pack.path !in active) {
                pack.delete()
                marker.delete()
            }
        }
    }

    private fun android.content.ContentResolver.queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) cursor.getString(0) else null
        } finally {
            cursor?.close()
        }
    }

    private fun handledDirectory(): File = File(context.filesDir, "imports/handled").apply { mkdirs() }

    private fun handledMarker(workId: UUID): File = File(handledDirectory(), "$workId.txt")

    private fun String.sanitizedDisplayName(): String =
        replace(Regex("[^A-Za-z0-9._ -]"), "_").take(255).ifBlank { "private-content" }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

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

    private companion object {
        const val MAX_SOURCE_BYTES = 50L * 1024L * 1024L
        val ALLOWED_SUFFIXES = setOf("pdf", "txt", "md", "json", "dndpack")
        const val MAX_MANIFEST_BYTES = 128 * 1024
        const val MAX_CANDIDATE_BYTES = 16 * 1024 * 1024
        const val MAX_CANDIDATES = 5_000
    }
}

private fun org.json.JSONArray?.toAttackGrants(): List<DerivedAttackGrant> {
    if (this == null) return emptyList()
    require(length() <= 20) { "attack-grant-count" }
    return (0 until length()).map { index -> getJSONObject(index).toAttackGrant() }
}

private fun JSONObject.toAttackGrant(): DerivedAttackGrant {
    val id = getString("id")
    require(id.matches(Regex("[a-z0-9][a-z0-9._-]{0,79}"))) { "attack-grant-id" }
    val weaponMatch = optJSONObject("weaponMatch").toWeaponMatch()
    val triggerWeaponMatch = optJSONObject("triggerWeaponMatch").toWeaponMatch().takeUnless { it == WeaponMatch() }
        ?: weaponMatch
    val dice = optJSONObject("damageDice")?.let { value ->
        val sides = value.getInt("sides")
        require(sides in setOf(1, 2, 4, 6, 8, 10, 12, 20, 100)) { "attack-grant-die" }
        DiceExpression(
            count = value.optInt("count", 1).coerceIn(1, 20),
            sides = sides,
            modifier = value.optInt("modifier", 0).coerceIn(-100, 100),
        )
    }
    return DerivedAttackGrant(
        id = id,
        name = getString("name").take(100),
        supportedRulesets = stringSet("supportedRulesets").map { value ->
            when (value) {
                "2014", "fifth_edition_2014", "fifth-edition-2014" -> RulesetId.FIFTH_EDITION_2014
                "2024", "fifth_edition_2024", "fifth-edition-2024" -> RulesetId.FIFTH_EDITION_2024
                "pf2e", "pf2e_remaster", "pf2e-remaster" -> RulesetId.PF2E_REMASTER
                else -> error("attack-grant-ruleset")
            }
        }.toSet(),
        parent = enumValue(optString("parent", "same_weapon"), DerivedAttackParent.SAME_WEAPON),
        weaponMatch = weaponMatch,
        triggerWeaponMatch = triggerWeaponMatch,
        trigger = enumValue(optString("trigger", "always"), DerivedAttackTrigger.ALWAYS),
        cost = optJSONObject("cost").toActionCost(),
        damageDice = dice,
        damageType = optString("damageType").take(40).ifBlank { null },
        damageAbilityRule = enumValue(optString("damageAbilityRule", "inherit"), DamageAbilityRule.INHERIT),
        attackCount = optInt("attackCount", 1).coerceIn(1, 10),
        maxUsesPerTurn = if (has("maxUsesPerTurn")) optInt("maxUsesPerTurn").coerceIn(1, 20) else null,
        requiresDifferentTriggerWeapon = optBoolean("requiresDifferentTriggerWeapon", false),
        inheritItemBonus = optBoolean("inheritItemBonus", true),
        inheritReach = optBoolean("inheritReach", true),
        timingHint = optString("timingHint").take(240),
        details = optString("details").take(500),
    )
}

private inline fun <reified T : Enum<T>> enumValue(value: String, default: T): T {
    if (value.isBlank()) return default
    return enumValues<T>().firstOrNull { it.name.equals(value.replace('-', '_'), true) }
        ?: error("attack-grant-enum")
}

private fun JSONObject?.toWeaponMatch(): WeaponMatch {
    if (this == null) return WeaponMatch()
    return WeaponMatch(
        baseWeaponIds = stringSet("baseWeaponIds"),
        excludedBaseWeaponIds = stringSet("excludedBaseWeaponIds"),
        training = stringSet("training").mapNotNull { value ->
            enumValues<WeaponTrainingCategory>().firstOrNull { it.name.equals(value, true) }
        }.toSet(),
        combatTypes = stringSet("combatTypes").mapNotNull { value ->
            enumValues<WeaponCombatType>().firstOrNull { it.name.equals(value, true) }
        }.toSet(),
        allPropertyIds = stringSet("allPropertyIds"),
        anyPropertyIds = stringSet("anyPropertyIds"),
        excludedPropertyIds = stringSet("excludedPropertyIds"),
    )
}

private fun JSONObject.stringSet(name: String): Set<String> {
    val values = optJSONArray(name) ?: return emptySet()
    require(values.length() <= 50) { "attack-grant-match-count" }
    return (0 until values.length()).map { values.getString(it).lowercase(Locale.ROOT).take(80) }.toSet()
}

private fun JSONObject?.toActionCost(): ActionCost {
    if (this == null) return ActionCost()
    val resourcesObject = optJSONObject("resources")
    val resources = if (resourcesObject == null) emptyMap() else resourcesObject.keys().asSequence().associateWith { key ->
        require(key.matches(Regex("[A-Za-z0-9._-]{1,80}"))) { "attack-grant-resource-id" }
        resourcesObject.getInt(key).coerceIn(1, 20)
    }
    return ActionCost(
        actions = optInt("actions", 0).coerceIn(0, 10),
        bonusActions = optInt("bonusActions", 0).coerceIn(0, 1),
        reactions = optInt("reactions", 0).coerceIn(0, 1),
        attacks = optInt("attacks", 0).coerceIn(0, 10),
        objectInteractions = optInt("objectInteractions", 0).coerceIn(0, 10),
        pf2eActions = optInt("pf2eActions", 0).coerceIn(0, 3),
        resources = resources,
    )
}
