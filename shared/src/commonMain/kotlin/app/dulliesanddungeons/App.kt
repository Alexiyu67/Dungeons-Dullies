package app.dulliesanddungeons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.dulliesanddungeons.ui.DndAppState
import app.dulliesanddungeons.ui.DulliesAndDungeonsApp
import app.dulliesanddungeons.ui.PortraitPickTarget

@Composable
fun App(
    state: DndAppState = remember { DndAppState() },
    onPickPortrait: (PortraitPickTarget) -> Unit = {},
    onImportPrivateContent: () -> Unit = {},
    onDownloadPrivateContentSchema: () -> Unit = {},
    onEditPortrait: (PortraitPickTarget) -> Unit = {},
    portraitEditor: @Composable () -> Unit = {},
) {
    DulliesAndDungeonsApp(
        state = state,
        onPickPortrait = onPickPortrait,
        onEditPortrait = onEditPortrait,
        onImportPrivateContent = onImportPrivateContent,
        onDownloadPrivateContentSchema = onDownloadPrivateContentSchema,
        portraitEditor = portraitEditor,
    )
}
