package app.dulliesanddungeons.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.dulliesanddungeons.domain.ActionCost

internal enum class CostTokenKind { Movement, Action, BonusAction, Reaction, Attack, ObjectInteraction, Resource }

@Immutable
internal data class CostTokenUi(
    val kind: CostTokenKind,
    val amount: Int = 1,
    val labelOverride: String? = null,
)

internal val ActionCost.hasCost: Boolean
    get() = actions > 0 || bonusActions > 0 || reactions > 0 || attacks > 0 ||
        objectInteractions > 0 || pf2eActions > 0 || resources.values.any { it > 0 }

internal fun ActionCost.toCostTokens(): List<CostTokenUi> = buildList {
    if (actions > 0) add(CostTokenUi(CostTokenKind.Action, actions))
    if (bonusActions > 0) add(CostTokenUi(CostTokenKind.BonusAction, bonusActions))
    if (reactions > 0) add(CostTokenUi(CostTokenKind.Reaction, reactions))
    if (attacks > 0) add(CostTokenUi(CostTokenKind.Attack, attacks))
    if (objectInteractions > 0) add(CostTokenUi(CostTokenKind.ObjectInteraction, objectInteractions))
    if (pf2eActions > 0) add(CostTokenUi(CostTokenKind.Action, pf2eActions))
    resources.filterValues { it > 0 }.forEach { (id, amount) ->
        add(CostTokenUi(CostTokenKind.Resource, amount, id.toReadableCostName()))
    }
}

internal fun FeatureUi.toCostTokens(): List<CostTokenUi> = buildList {
    addAll(actionCost.toCostTokens())
    val resourceLabel = sharedResourceCostLabel()
    if (resourceLabel != null && none { it.kind == CostTokenKind.Resource && it.labelOverride == resourceLabel }) {
        add(CostTokenUi(CostTokenKind.Resource, labelOverride = resourceLabel))
    }
}

internal fun FeatureUi.sharedResourceCostLabel(): String? = resourceId?.toReadableCostName()

@Composable
internal fun CostChip(
    state: DndAppState,
    token: CostTokenUi,
    modifier: Modifier = Modifier,
    available: Boolean = true,
    fillWidth: Boolean = false,
) {
    val label = token.displayLabel(state)
    val activeColor = costTokenColor(token.kind)
    val color = if (available) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        color = color.copy(alpha = if (available) 0.10f else 0.06f),
        contentColor = color,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = if (available) 0.55f else 0.25f)),
    ) {
        Row(
            modifier = Modifier.then(if (fillWidth) Modifier.fillMaxWidth() else Modifier).padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalArrangement = if (fillWidth) Arrangement.Center else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(costTokenIcon(token.kind), contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun CostChipRow(
    state: DndAppState,
    tokens: List<CostTokenUi>,
    modifier: Modifier = Modifier,
    available: Boolean = true,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        tokens.forEach { CostChip(state, it, available = available) }
    }
}

@Composable
private fun costTokenColor(kind: CostTokenKind): Color = when (kind) {
    CostTokenKind.Movement -> Color(0xFF4C5F7C)
    CostTokenKind.Action -> Color(0xFF2F6048)
    CostTokenKind.BonusAction -> Color(0xFF765A2E)
    CostTokenKind.Reaction -> Color(0xFF7A3940)
    CostTokenKind.Attack -> Color(0xFF955230)
    CostTokenKind.ObjectInteraction -> Color(0xFF3D6D6A)
    CostTokenKind.Resource -> Color(0xFF76546D)
}

private fun costTokenIcon(kind: CostTokenKind): ImageVector = when (kind) {
    CostTokenKind.Movement -> Icons.AutoMirrored.Rounded.DirectionsRun
    CostTokenKind.Action -> Icons.Rounded.Schedule
    CostTokenKind.BonusAction -> Icons.Rounded.AutoAwesome
    CostTokenKind.Reaction -> Icons.AutoMirrored.Rounded.Reply
    CostTokenKind.Attack -> Icons.Rounded.SportsMma
    CostTokenKind.ObjectInteraction -> Icons.Rounded.TouchApp
    CostTokenKind.Resource -> Icons.Rounded.Diamond
}

private fun CostTokenUi.displayLabel(state: DndAppState): String {
    val base = labelOverride ?: when (kind) {
        CostTokenKind.Movement -> state.t("Move", "Bewegung")
        CostTokenKind.Action -> state.t("Action", "Aktion")
        CostTokenKind.BonusAction -> state.t("Bonus", "Bonus")
        CostTokenKind.Reaction -> state.t("Reaction", "Reaktion")
        CostTokenKind.Attack -> state.t("Attack", "Angriff")
        CostTokenKind.ObjectInteraction -> state.t("Object", "Objekt")
        CostTokenKind.Resource -> state.t("Resource", "Ressource")
    }
    return if (amount > 1 && labelOverride == null) "$amount $base" else if (amount > 1) "$amount $base" else base
}

private fun String.toReadableCostName(): String = split('-', '_')
    .filter(String::isNotBlank)
    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
