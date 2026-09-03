package app.dulliesanddungeons.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.Font
import app.dulliesanddungeons.resources.Res
import app.dulliesanddungeons.resources.alegreya_sans_bold
import app.dulliesanddungeons.resources.alegreya_sans_medium
import app.dulliesanddungeons.resources.alegreya_sans_regular
import app.dulliesanddungeons.resources.alegreya_variable

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6048),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE8DD),
    onPrimaryContainer = Color(0xFF17271F),
    secondary = Color(0xFF7A3940),
    secondaryContainer = Color(0xFFF0DDDA),
    onSecondaryContainer = Color(0xFF341717),
    tertiary = Color(0xFF765A2E),
    tertiaryContainer = Color(0xFFEFE2BD),
    onTertiaryContainer = Color(0xFF2C210B),
    background = Color(0xFFF4ECDD),
    onBackground = Color(0xFF2A261F),
    surface = Color(0xFFFFF9EE),
    onSurface = Color(0xFF2A261F),
    surfaceVariant = Color(0xFFE8DDC8),
    onSurfaceVariant = Color(0xFF554C3F),
    outline = Color(0xFF86745D),
    outlineVariant = Color(0xFFC9B99E),
    error = Color(0xFF9E2F2F),
)

@Composable
private fun appTypography(): Typography {
    val heading = FontFamily(
        Font(Res.font.alegreya_variable, FontWeight.Normal),
        Font(Res.font.alegreya_variable, FontWeight.SemiBold),
        Font(Res.font.alegreya_variable, FontWeight.Bold),
    )
    val body = FontFamily(
        Font(Res.font.alegreya_sans_regular, FontWeight.Normal),
        Font(Res.font.alegreya_sans_medium, FontWeight.Medium),
        Font(Res.font.alegreya_sans_bold, FontWeight.Bold),
    )
    val base = Typography()
    return Typography(
        displaySmall = base.displaySmall.copy(fontFamily = heading, fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontFamily = heading, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        headlineMedium = base.headlineMedium.copy(fontFamily = heading, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = heading, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = heading, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = heading, fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontFamily = body, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontFamily = body),
        bodyMedium = base.bodyMedium.copy(fontFamily = body),
        bodySmall = base.bodySmall.copy(fontFamily = body),
        labelLarge = base.labelLarge.copy(fontFamily = body, fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontFamily = body, fontWeight = FontWeight.Medium),
        labelSmall = base.labelSmall.copy(fontFamily = body, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun DulliesAndDungeonsApp(
    state: DndAppState = remember { DndAppState() },
    onPickPortrait: (PortraitPickTarget) -> Unit = {},
    onImportPrivateContent: () -> Unit = {},
    onEditPortrait: (PortraitPickTarget) -> Unit = {},
    portraitEditor: @Composable () -> Unit = {},
) {
    DulliesTheme {
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(state.inventoryFeedback) {
            val message = state.inventoryFeedback ?: return@LaunchedEffect
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (state.inventoryFeedbackCanUndo) state.t("Undo", "Rückgängig") else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) state.undoInventoryRemoval()
            else state.clearInventoryFeedback()
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Box(Modifier.fillMaxSize().then(if (state.dicePresentation != null) Modifier.blur(8.dp) else Modifier)) {
                    when (state.screen) {
                        AppScreen.Characters -> CharacterListScreen(state)
                        AppScreen.CreateCharacter -> CharacterCreationScreen(state, onPickPortrait, onEditPortrait)
                        AppScreen.CharacterSheet -> CharacterSheetScreen(state, onPickPortrait, onEditPortrait)
                    }
                }
                if (state.dicePresentation != null) {
                    Surface(
                        onClick = { state.dicePresentation = null },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = .38f),
                    ) {}
                }
                DicePresentationOverlay(
                    state,
                    Modifier.align(Alignment.Center),
                )
            }
        }

        if (state.searchOpen) SearchDialog(state)
        if (state.conditionsOpen) ConditionsDialog(state)
        if (state.hpAdjustOpen) HpAdjustDialog(state)
        if (state.quickRollEditorOpen) QuickRollEditorDialog(state)
        if (state.equipmentAddOpen) ItemBrowserDialog(state)
        if (state.privateContentOpen) PrivateContentDialog(state, onImportPrivateContent)
        if (state.turnOpen) TurnAssistantDialog(state)
        if (state.sessionHistoryOpen) SessionHistoryDialog(state)
        if (state.sessionSaveOpen) SaveSessionDialog(state)
        if (state.conversionOpen) ConversionDialog(state)
        if (state.editorOpen) CharacterEditorDialog(state, onPickPortrait, onEditPortrait)
        if (state.levelUpOpen) LevelUpDialog(state)

        if (state.revivalConfirmationOpen) {
            AlertDialog(
                onDismissRequest = state::cancelRevival,
                title = { Text(state.t("Record revival?", "Wiederbelebung eintragen?")) },
                text = { Text(state.t("This is a manual table correction, not ordinary healing. It clears the recorded death state.", "Dies ist eine manuelle Spieltischkorrektur, keine gewöhnliche Heilung. Der eingetragene Todeszustand wird entfernt.")) },
                confirmButton = {
                    TextButton(onClick = state::confirmRevival) { Text(state.t("Record revival", "Wiederbelebung eintragen")) }
                },
                dismissButton = {
                    TextButton(onClick = state::cancelRevival) { Text(state.t("Cancel", "Abbrechen")) }
                },
            )
        }

        val infoTitle = state.infoTitle
        if (infoTitle != null) {
            AlertDialog(
                onDismissRequest = { state.infoTitle = null; state.infoCosts = emptyList() },
                icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                title = { Text(infoTitle) },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        if (state.infoCosts.isNotEmpty()) CostChipRow(state, state.infoCosts)
                        Text(state.infoBody, style = MaterialTheme.typography.bodyLarge)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { state.infoTitle = null; state.infoCosts = emptyList() }) {
                        Text(state.t("Got it", "Verstanden"))
                    }
                },
            )
        }
        portraitEditor()
    }
}

@Composable
private fun DulliesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = appTypography(),
        content = content,
    )
}

@Composable
internal fun LanguageButton(state: DndAppState) {
    val label = if (state.language == UiLanguage.English) "DE" else "EN"
    IconButton(
        onClick = {
            state.toggleLanguage()
        },
        modifier = Modifier.size(48.dp).semantics { role = Role.Button },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Language, contentDescription = state.t("Switch to German", "Zu Englisch wechseln"), modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun RulesetBadge(ruleset: Ruleset) {
    Surface(
        color = when (ruleset) {
            Ruleset.Fifth2024 -> MaterialTheme.colorScheme.primaryContainer
            Ruleset.Fifth2014 -> MaterialTheme.colorScheme.secondaryContainer
            Ruleset.Pf2eRemaster -> MaterialTheme.colorScheme.tertiaryContainer
        },
        contentColor = when (ruleset) {
            Ruleset.Fifth2024 -> MaterialTheme.colorScheme.onPrimaryContainer
            Ruleset.Fifth2014 -> MaterialTheme.colorScheme.onSecondaryContainer
            Ruleset.Pf2eRemaster -> MaterialTheme.colorScheme.onTertiaryContainer
        },
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            ruleset.shortLabel,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun CharacterPortrait(
    name: String,
    seed: Int,
    modifier: Modifier = Modifier,
    portraitData: ByteArray? = null,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
) {
    val palette = listOf(Color(0xFF4D5F54), Color(0xFF795548), Color(0xFF4C5F7C), Color(0xFF76546D))
    val initials = name.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifEmpty { "?" }
    val portrait = remember(portraitData) {
        runCatching { portraitData?.decodeToImageBitmap() }.getOrNull()
    }
    val interactionModifier = if (onClick == null) {
        Modifier.semantics { role = Role.Image }
    } else {
        Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                clickLabel?.let { contentDescription = it }
            }
    }
    Box(
        modifier = modifier.clip(CircleShape).then(interactionModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (portrait != null) {
            Image(
                bitmap = portrait,
                contentDescription = if (onClick == null) name else null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(color = palette[kotlin.math.abs(seed) % palette.size], modifier = Modifier.fillMaxSize()) {}
            Text(initials, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        action?.invoke()
    }
}

@Composable
internal fun ExplanationCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
