package app.dulliesanddungeons.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.dulliesanddungeons.domain.CoinDenomination
import app.dulliesanddungeons.domain.CurrencyPurse

internal val CoinDenomination.shortCode: String
    get() = when (this) {
        CoinDenomination.PLATINUM -> "PP"
        CoinDenomination.ELECTRUM -> "EP"
        CoinDenomination.GOLD -> "GP"
        CoinDenomination.SILVER -> "SP"
        CoinDenomination.COPPER -> "CP"
    }

internal fun CoinDenomination.localizedName(language: UiLanguage): String = when (this) {
    CoinDenomination.PLATINUM -> if (language == UiLanguage.German) "Platin" else "Platinum"
    CoinDenomination.ELECTRUM -> if (language == UiLanguage.German) "Elektrum" else "Electrum"
    CoinDenomination.GOLD -> "Gold"
    CoinDenomination.SILVER -> if (language == UiLanguage.German) "Silber" else "Silver"
    CoinDenomination.COPPER -> if (language == UiLanguage.German) "Kupfer" else "Copper"
}

internal val CoinDenomination.searchTerms: List<String>
    get() = when (this) {
        CoinDenomination.PLATINUM -> listOf("PP", "platinum", "platinum piece", "platinum pieces", "Platin", "Platinmünze", "Platinmünzen")
        CoinDenomination.ELECTRUM -> listOf("EP", "electrum", "electrum piece", "electrum pieces", "Elektrum", "Elektrummünze", "Elektrummünzen")
        CoinDenomination.GOLD -> listOf("GP", "gold", "gold piece", "gold pieces", "Goldmünze", "Goldmünzen")
        CoinDenomination.SILVER -> listOf("SP", "silver", "silver piece", "silver pieces", "Silber", "Silbermünze", "Silbermünzen")
        CoinDenomination.COPPER -> listOf("CP", "copper", "copper piece", "copper pieces", "Kupfer", "Kupfermünze", "Kupfermünzen")
    } + listOf("currency", "coins", "money", "Währung", "Münzen", "Geld")

internal fun coinDenominations(ruleset: Ruleset): List<CoinDenomination> = when (ruleset) {
    Ruleset.Fifth2014, Ruleset.Fifth2024 -> listOf(
        CoinDenomination.PLATINUM,
        CoinDenomination.ELECTRUM,
        CoinDenomination.GOLD,
        CoinDenomination.SILVER,
        CoinDenomination.COPPER,
    )
    Ruleset.Pf2eRemaster -> listOf(
        CoinDenomination.PLATINUM,
        CoinDenomination.GOLD,
        CoinDenomination.SILVER,
        CoinDenomination.COPPER,
    )
}

internal fun currencyKnownItems(): List<KnownItemUi> = CoinDenomination.entries.map { denomination ->
    KnownItemUi(
        id = "currency:${denomination.name.lowercase()}",
        name = "${denomination.localizedName(UiLanguage.English)} Pieces",
        type = KnownItemType.Currency,
        rarity = ItemRarity.Mundane,
        details = "",
        source = KnownItemSource.BuiltIn,
        supportedRulesets = if (denomination == CoinDenomination.ELECTRUM) {
            setOf(Ruleset.Fifth2014, Ruleset.Fifth2024)
        } else {
            Ruleset.entries.toSet()
        },
        currencyDenomination = denomination,
        searchTerms = denomination.searchTerms,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CurrencyOverview(state: DndAppState, character: CharacterUi) {
    CurrencyOverview(
        state = state,
        ruleset = character.ruleset,
        currency = character.currency,
        onAdjust = state::openCurrencyAdjustment,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CurrencyOverview(
    state: DndAppState,
    ruleset: Ruleset,
    currency: CurrencyPurse,
    onAdjust: ((CoinDenomination) -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        coinDenominations(ruleset).forEach { denomination ->
            val name = denomination.localizedName(state.language)
            val balance = currency.balance(denomination)
            val adjustLabel = state.t("Adjust $name", "$name anpassen")
            val interactionModifier = if (onAdjust == null) {
                Modifier.semantics { contentDescription = "$name, $balance ${denomination.shortCode}" }
            } else {
                Modifier
                    .combinedClickable(
                        onClick = { onAdjust(denomination) },
                        onLongClick = { onAdjust(denomination) },
                    )
                    .semantics {
                        contentDescription = "$name, $balance ${denomination.shortCode}"
                        customActions = listOf(
                            CustomAccessibilityAction(adjustLabel) {
                                onAdjust(denomination)
                                true
                            },
                        )
                    }
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .then(interactionModifier),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        denomination.shortCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        balance.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CurrencyAdjustmentDialog(state: DndAppState, denomination: CoinDenomination) {
    val character = state.selectedCharacter ?: return
    var spending by remember(denomination) { mutableStateOf(false) }
    var amountText by remember(denomination) { mutableStateOf("") }
    val current = character.currency.balance(denomination)
    val amount = amountText.toLongOrNull() ?: 0L
    val resulting = current.toLong() + if (spending) -amount else amount
    val valid = amount in 1..Int.MAX_VALUE.toLong() && resulting in 0..Int.MAX_VALUE.toLong()
    val error = when {
        amountText.isBlank() || amount == 0L -> null
        amount > Int.MAX_VALUE.toLong() || resulting > Int.MAX_VALUE.toLong() -> state.t("Amount is too large.", "Betrag ist zu groß.")
        resulting < 0L -> state.t("Not enough ${denomination.shortCode} available.", "Nicht genug ${denomination.shortCode} vorhanden.")
        else -> null
    }
    val name = denomination.localizedName(state.language)

    AlertDialog(
        onDismissRequest = state::closeCurrencyAdjustment,
        title = { Text(state.t("Adjust $name", "$name anpassen")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !spending,
                        onClick = { spending = false },
                        label = { Text(state.t("Add", "Hinzufügen")) },
                    )
                    FilterChip(
                        selected = spending,
                        onClick = { spending = true },
                        label = { Text(state.t("Spend", "Ausgeben")) },
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value -> amountText = value.filter(Char::isDigit).take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(state.t("Amount", "Betrag")) },
                    suffix = { Text(denomination.shortCode) },
                    supportingText = if (error != null) {{
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }} else null,
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    "$current ${denomination.shortCode} ${if (spending) "-" else "+"} $amount = $resulting ${denomination.shortCode}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val signedAmount = if (spending) -amount.toInt() else amount.toInt()
                    if (state.adjustCurrency(denomination, signedAmount)) state.closeCurrencyAdjustment()
                },
                enabled = valid,
            ) {
                Text(
                    if (spending) {
                        state.t("Spend $amount ${denomination.shortCode}", "$amount ${denomination.shortCode} ausgeben")
                    } else {
                        state.t("Add $amount ${denomination.shortCode}", "$amount ${denomination.shortCode} hinzufügen")
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = state::closeCurrencyAdjustment) { Text(state.t("Cancel", "Abbrechen")) }
        },
    )
}
