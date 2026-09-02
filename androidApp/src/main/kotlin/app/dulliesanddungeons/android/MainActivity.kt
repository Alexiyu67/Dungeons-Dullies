package app.dulliesanddungeons.android

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.dulliesanddungeons.App
import app.dulliesanddungeons.ui.DndAppState
import app.dulliesanddungeons.ui.PendingImportUi
import app.dulliesanddungeons.ui.PortraitPickTarget
import app.dulliesanddungeons.ui.UiLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val storage by lazy { AndroidLocalStateStore(applicationContext) }
    private val privateImportCoordinator by lazy { PrivateImportCoordinator(applicationContext) }
    private var bootstrapState by mutableStateOf<BootstrapState>(BootstrapState.Loading)
    private var bootstrapJob: Job? = null
    private val importCompletionMutex = Mutex()
    private val privateImportPicker = registerForActivityResult(OpenDocument()) { uri ->
        if (uri != null) enqueuePrivateImport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            when (val current = bootstrapState) {
                BootstrapState.Loading -> BootstrapLoadingScreen()
                is BootstrapState.Error -> BootstrapErrorScreen(current.failure, ::beginBootstrap)
                is BootstrapState.Ready -> {
                    var portraitTarget by remember(current.appState) { mutableStateOf<PortraitPickTarget?>(null) }
                    val portraitPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
                        val target = portraitTarget
                        portraitTarget = null
                        if (target != null) {
                            uri?.let(::normalizedPortrait)?.let { bytes ->
                                current.appState.selectPortrait(target, bytes)
                            }
                        }
                    }
                    App(
                        state = current.appState,
                        onPickPortrait = { target ->
                            portraitTarget = target
                            portraitPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                        onImportPrivateContent = ::launchPrivateImport,
                    )
                }
            }
        }
        beginBootstrap()
    }

    override fun onStop() {
        (bootstrapState as? BootstrapState.Ready)?.appState?.let { appState ->
            appState.saveTurnDraft()
            lifecycleScope.launch {
                privateImportCoordinator.cleanupHandledImports(appState.pendingImports.map { it.sourcePath }.toSet())
            }
        }
        super.onStop()
    }

    private fun beginBootstrap() {
        bootstrapJob?.cancel()
        bootstrapState = BootstrapState.Loading
        bootstrapJob = lifecycleScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    storage.preload()
                    storage.readState()
                }
            }
            if (systemMotionEnabled()) {
                val remaining = 1_600L - (SystemClock.elapsedRealtime() - startedAt)
                if (remaining > 0) delay(remaining)
            }
            bootstrapState = loaded.fold(
                onSuccess = { BootstrapState.Ready(DndAppState(storage, it)) },
                onFailure = { BootstrapState.Error(it) },
            )
            (bootstrapState as? BootstrapState.Ready)?.appState?.let { appState ->
                lifecycleScope.launch { recoverPrivateImports(appState) }
            }
        }
    }

    private fun systemMotionEnabled(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }

    private fun launchPrivateImport() {
        privateImportPicker.launch(arrayOf("*/*"))
    }

    private fun enqueuePrivateImport(uri: Uri) {
        lifecycleScope.launch {
            val locale = (bootstrapState as? BootstrapState.Ready)?.appState?.language
                .let { if (it == UiLanguage.German) "de" else "en" }
            privateImportCoordinator.stageAndEnqueue(uri, locale).fold(
                onSuccess = { id ->
                    Toast.makeText(this@MainActivity, "Private import started", Toast.LENGTH_SHORT).show()
                    val info = awaitImport(id)
                    val appState = (bootstrapState as? BootstrapState.Ready)?.appState
                    val message = if (appState == null) {
                        null
                    } else {
                        handleCompletedImport(id, info, appState)
                    }
                    message?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
                },
                onFailure = {
                    Toast.makeText(this@MainActivity, "Private import could not be started", Toast.LENGTH_LONG).show()
                },
            )
        }
    }

    private suspend fun recoverPrivateImports(appState: DndAppState) {
        privateImportCoordinator.cleanupHandledImports(appState.pendingImports.map { it.sourcePath }.toSet())
        val completed = withContext(Dispatchers.IO) {
            WorkManager.getInstance(applicationContext)
                .getWorkInfosByTag(PrivateImportWorker.WORK_TAG)
                .get()
                .orEmpty()
                .filter { it.state.isFinished }
        }
        completed.forEach { handleCompletedImport(it.id, it, appState) }
    }

    private suspend fun handleCompletedImport(id: UUID, info: WorkInfo, appState: DndAppState): String? =
        importCompletionMutex.withLock {
            if (privateImportCoordinator.isHandled(id)) return@withLock null
            val path = info.outputData.getString(PrivateImportWorker.KEY_RESULT_PATH).orEmpty()
            val message = if (info.state == WorkInfo.State.SUCCEEDED) {
                val packId = info.outputData.getString(PrivateImportWorker.KEY_PACK_ID).orEmpty()
                val kind = info.outputData.getString(PrivateImportWorker.KEY_CONTAINER_KIND).orEmpty()
                runCatching { privateImportCoordinator.loadPendingImport(path, packId, kind) }.fold(
                    onSuccess = { pending ->
                        appState.registerPendingImport(pending)
                        "Private import is ready for review"
                    },
                    onFailure = {
                        appState.registerPendingImport(
                            PendingImportUi(
                                packId = packId.ifBlank { "failed-${id.toString().take(8)}" },
                                containerKind = kind.ifBlank { "error" },
                                sourcePath = path,
                                error = "pack-read-failed",
                            ),
                        )
                        "Private import could not be opened for review"
                    },
                )
            } else {
                val errorCode = info.outputData.getString(PrivateImportWorker.KEY_ERROR_CODE) ?: "import-failed"
                appState.registerPendingImport(
                    PendingImportUi(
                        packId = "failed-${id.toString().take(8)}",
                        containerKind = "error",
                        sourcePath = "",
                        error = errorCode,
                    ),
                )
                "Private import failed ($errorCode)"
            }
            privateImportCoordinator.markHandled(id, path.takeIf { it.isNotBlank() })
            privateImportCoordinator.cleanupHandledImports(appState.pendingImports.map { it.sourcePath }.toSet())
            message
        }

    private suspend fun awaitImport(id: UUID): WorkInfo {
        val workManager = WorkManager.getInstance(applicationContext)
        while (true) {
            val info = withContext(Dispatchers.IO) { workManager.getWorkInfoById(id).get() }
            if (info != null && info.state.isFinished) return info
            delay(250)
        }
    }

    private fun normalizedPortrait(uri: Uri): ByteArray? = runCatching {
        val source = contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) ?: return null
        val longest = maxOf(source.width, source.height)
        val scaled = if (longest > 512) {
            val scale = 512f / longest
            source.scale((source.width * scale).toInt(), (source.height * scale).toInt())
        } else source
        ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 86, output)
            output.toByteArray()
        }.also {
            if (scaled !== source) scaled.recycle()
            source.recycle()
        }
    }.getOrNull()

    private sealed interface BootstrapState {
        data object Loading : BootstrapState
        data class Ready(val appState: DndAppState) : BootstrapState
        data class Error(val failure: Throwable) : BootstrapState
    }
}
