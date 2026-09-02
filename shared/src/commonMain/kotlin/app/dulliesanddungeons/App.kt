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
) {
    DulliesAndDungeonsApp(state, onPickPortrait, onImportPrivateContent)
}
